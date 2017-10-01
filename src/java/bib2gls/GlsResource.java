/*
    Copyright (C) 2017 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.dickimawbooks.bib2gls;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.text.Collator;
import java.text.CollationKey;
import java.text.ParseException;
import java.nio.charset.Charset;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.aux.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.CsvList;
import com.dickimawbooks.texparserlib.html.L2HStringConverter;

public class GlsResource
{
   public GlsResource(TeXParser parser, AuxData data, 
     String pluralSuffix, String abbrvPluralSuffix)
    throws IOException,InterruptedException,Bib2GlsException
   {
      sources = new Vector<TeXPath>();

      this.pluralSuffix = pluralSuffix;
      this.dualPluralSuffix = pluralSuffix;

      init(parser, data.getArg(0), data.getArg(1));
   }

   private void init(TeXParser parser, TeXObject opts, TeXObject arg)
      throws IOException,InterruptedException,
             Bib2GlsException,IllegalArgumentException
   {

      bib2gls = (Bib2Gls)parser.getListener().getTeXApp();

      TeXPath texPath = new TeXPath(parser, 
        arg.toString(parser), "glstex");

      texFile = bib2gls.resolveFile(texPath.getFile());

      bib2gls.registerTeXFile(texFile);

      String filename = texPath.getTeXPath(true);

      KeyValList list = KeyValList.getList(parser, opts);

      String[] srcList = null;

      String master = null;
      String supplemental = null;

      if (bib2gls.useGroupField())
      {
         groupTitleMap = new HashMap<String,GroupTitle>();
      }

      for (Iterator<String> it = list.keySet().iterator(); it.hasNext(); )
      {
         String opt = it.next();

         if (opt.equals("src"))
         {
            srcList = getStringArray(parser, list, opt);

            if (srcList == null)
            {
               sources.add(bib2gls.getBibFilePath(parser, filename));
            }
            else
            {
               for (String src : srcList)
               {
                  sources.add(bib2gls.getBibFilePath(parser, src));
               }
            }
         }
         else if (opt.equals("master"))
         {// link all entries to glossary in external pdf file
            master = getRequired(parser, list, opt);
         }
         else if (opt.equals("master-resources"))
         {
            masterSelection = getStringArray(parser, list, opt);
         }
         else if (opt.equals("supplemental-locations"))
         {// fetch supplemental locations from another document
            supplemental = getRequired(parser, list, opt);
         }
         else if (opt.equals("supplemental-category"))
         {
            supplementalCategory = getRequired(parser, list, opt);
         }
         else if (opt.equals("supplemental-selection"))
         {
            supplementalSelection = getStringArray(parser, list, opt);

            if (supplementalSelection == null
             || supplementalSelection.length == 0)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.missing.value", opt));
            }

            if (supplementalSelection.length == 1 
             && supplementalSelection[0].equals("selected"))
            {
               supplementalSelection = null;
            }
         }
         else if (opt.equals("name-case-change"))
         {
            nameCaseChange = getChoice(parser, list, opt, "none", "lc", "uc",
              "lc-cs", "uc-cs", "firstuc", "firstuc-cs");
         }
         else if (opt.equals("description-case-change"))
         {
            descCaseChange = getChoice(parser, list, opt, "none", "lc", "uc",
              "lc-cs", "uc-cs", "firstuc", "firstuc-cs");
         }
         else if (opt.equals("short-case-change"))
         {
            shortCaseChange = getChoice(parser, list, opt, "none", "lc", "uc",
              "lc-cs", "uc-cs", "firstuc", "firstuc-cs");
         }
         else if (opt.equals("dual-short-case-change"))
         {
            dualShortCaseChange = getChoice(parser, list, opt,
              "none", "lc", "uc", "lc-cs", "uc-cs", "firstuc", "firstuc-cs");
         }
         else if (opt.equals("short-plural-suffix"))
         {
            shortPluralSuffix = getOptional(parser, "", list, opt);

            if (shortPluralSuffix.equals("use-default"))
            {
               shortPluralSuffix = null;
            }
         }
         else if (opt.equals("dual-short-plural-suffix"))
         {
            dualShortPluralSuffix = getOptional(parser, "", list, opt);

            if (dualShortPluralSuffix.equals("use-default"))
            {
               dualShortPluralSuffix = null;
            }
         }
         else if (opt.equals("match-op"))
         {
            String val = getChoice(parser, list, opt, "and", "or");

            fieldPatternsAnd = val.equals("and");
         }
         else if (opt.equals("match"))
         {
            TeXObject[] array = getTeXObjectArray(parser, list, opt);
            notMatch = false;

            if (array == null)
            {
               fieldPatterns = null;
            }
            else
            {
               fieldPatterns = new HashMap<String,Pattern>();

               for (int i = 0; i < array.length; i++)
               {
                  if (!(array[i] instanceof TeXObjectList))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.value", 
                        opt, list.get(opt).toString(parser)));
                  }

                  Vector<TeXObject> split = splitList(parser, '=', 
                     (TeXObjectList)array[i]);

                  if (split == null || split.size() == 0) continue;

                  String field = split.get(0).toString(parser);

                  if (split.size() > 2)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.value", 
                        field, array[i].toString(parser), opt));
                  }

                  String val = split.size() == 1 ? "" 
                               : split.get(1).toString(parser);

                  // Has this field already been added?

                  Pattern p = fieldPatterns.get(field);

                  if (p == null)
                  {
                     p = Pattern.compile(val);
                  }
                  else
                  {
                     p = Pattern.compile(String.format(
                            "(?:%s)|(?:%s)", p.pattern(), val));
                  }
                  try
                  {
                     fieldPatterns.put(field, p);
                  }
                  catch (PatternSyntaxException e)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.pattern", 
                        field, val, opt), e);
                  }
               }
            }
         }
         else if (opt.equals("not-match"))
         {
            TeXObject[] array = getTeXObjectArray(parser, list, opt);
            notMatch = true;

            if (array == null)
            {
               fieldPatterns = null;
            }
            else
            {
               fieldPatterns = new HashMap<String,Pattern>();

               for (int i = 0; i < array.length; i++)
               {
                  if (!(array[i] instanceof TeXObjectList))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.value", 
                        opt, list.get(opt).toString(parser)));
                  }

                  Vector<TeXObject> split = splitList(parser, '=', 
                     (TeXObjectList)array[i]);

                  if (split == null || split.size() == 0) continue;

                  String field = split.get(0).toString(parser);

                  if (split.size() > 2)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.value", 
                        field, array[i].toString(parser), opt));
                  }

                  String val = split.size() == 1 ? "" 
                               : split.get(1).toString(parser);

                  // Has this field already been added?

                  Pattern p = fieldPatterns.get(field);

                  if (p == null)
                  {
                     p = Pattern.compile(val);
                  }
                  else
                  {
                     p = Pattern.compile(String.format(
                            "(?:%s)|(?:%s)", p.pattern(), val));
                  }
                  try
                  {
                     fieldPatterns.put(field, p);
                  }
                  catch (PatternSyntaxException e)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.pattern", 
                        field, val, opt), e);
                  }
               }
            }
         }
         else if (opt.equals("secondary"))
         {
            TeXObject obj = getRequiredObject(parser, list, opt);

            if (!(obj instanceof TeXObjectList))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                    obj.toString(parser)));
            }

            Vector<TeXObject> split = splitList(parser, ':', 
                     (TeXObjectList)obj);

            int n = split.size();

            if (n == 2)
            {
               secondaryType = split.get(1).toString(parser);
            }
            else if (n == 3)
            {
               secondaryField = split.get(1).toString(parser);
               secondaryType = split.get(2).toString(parser);
            }
            else
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                    obj.toString(parser)));
            }

            secondarySort = split.get(0).toString(parser);
         }
         else if (opt.equals("ext-prefixes"))
         {
            externalPrefixes = getStringArray(parser, list, opt);
         }
         else if (opt.equals("interpret-preamble"))
         {
            interpretPreamble = getBoolean(parser, list, opt);
         }
         else if (opt.equals("flatten"))
         {
            flatten = getBoolean(parser, list, opt);
         }
         else if (opt.equals("flatten-lonely"))
         {
            String val = getChoice(parser, list, opt, "false", "presort",
             "postsort");

            if (val.equals("false"))
            {
               flattenLonely = FLATTEN_LONELY_FALSE;
            }
            else if (val.equals("presort"))
            {
               flattenLonely = FLATTEN_LONELY_PRE_SORT;
            }
            else if (val.equals("postsort"))
            {
               flattenLonely = FLATTEN_LONELY_POST_SORT;
            }
         }
         else if (opt.equals("flatten-lonely-rule"))
         {
            String val = getChoice(parser, list, opt, "only unrecorded parents",
              "no discard", "discard unrecorded");

            if (val.equals("only unrecorded parents"))
            {
               flattenLonelyRule = FLATTEN_LONELY_RULE_ONLY_UNRECORDED_PARENTS;
            }
            else if (val.equals("no discard"))
            {
               flattenLonelyRule = FLATTEN_LONELY_RULE_NO_DISCARD;
            }
            else if (val.equals("discard unrecorded"))
            {
               flattenLonelyRule = FLATTEN_LONELY_RULE_DISCARD_UNRECORDED;
            }
         }
         else if (opt.equals("save-locations"))
         {
            saveLocations = getBoolean(parser, list, opt);
         }
         else if (opt.equals("save-child-count"))
         {
            saveChildCount = getBoolean(parser, list, opt);
         }
         else if (opt.equals("alias-loc"))
         {
            String val = getChoice(parser, list, opt, 
              "omit", "transfer", "keep");

            if (val.equals("omit"))
            {
               aliasLocations = ALIAS_LOC_OMIT;
            }
            else if (val.equals("transfer"))
            {
               aliasLocations = ALIAS_LOC_TRANS;
            }
            else
            {
               aliasLocations = ALIAS_LOC_KEEP;
            }
         }
         else if (opt.equals("set-widest"))
         {
            setWidest = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-entry-map"))
         {
            String[] keys = new String[1];

            dualEntryMap = getDualMap(parser, list, opt, keys);

            dualEntryFirstMap = keys[0];
         }
         else if (opt.equals("dual-abbrv-map"))
         {
            String[] keys = new String[1];

            dualAbbrevMap = getDualMap(parser, list, opt, keys);

            dualAbbrevFirstMap = keys[0];
         }
         else if (opt.equals("dual-entryabbrv-map"))
         {
            String[] keys = new String[1];

            dualEntryAbbrevMap = getDualMap(parser, list, opt, keys);

            dualEntryAbbrevFirstMap = keys[0];
         }
         else if (opt.equals("dual-symbol-map"))
         {
            String[] keys = new String[1];

            dualSymbolMap = getDualMap(parser, list, opt, keys);

            dualSymbolFirstMap = keys[0];
         }
         else if (opt.equals("dual-backlink"))
         {
            if (getBoolean(parser, list, opt))
            {
               backLinkDualEntry = true;
               backLinkDualAbbrev = true;
               backLinkDualSymbol = true;
               backLinkDualEntryAbbrev = true;
            }
            else
            {
               backLinkDualEntry = false;
               backLinkDualAbbrev = false;
               backLinkDualSymbol = false;
               backLinkDualEntryAbbrev = false;
            }
         }
         else if (opt.equals("dual-entry-backlink"))
         {
            backLinkDualEntry = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-abbrv-backlink"))
         {
            backLinkDualAbbrev = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-entryabbrv-backlink"))
         {
            backLinkDualEntryAbbrev = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-symbol-backlink"))
         {
            backLinkDualSymbol = getBoolean(parser, list, opt);
         }
         else if (opt.equals("type"))
         {
            type = getRequired(parser, list, opt);
         }
         else if (opt.equals("dual-type"))
         {
            dualType = getRequired(parser, list, opt);
         }
         else if (opt.equals("dual-field"))
         {
            dualField = getOptional(parser, "dual", list, opt);
         }
         else if (opt.equals("category"))
         {
            category = getRequired(parser, list, opt);
         }
         else if (opt.equals("dual-category"))
         {
            dualCategory = getRequired(parser, list, opt);
         }
         else if (opt.equals("counter"))
         {
            counter = getRequired(parser, list, opt);
         }
         else if (opt.equals("dual-counter"))
         {
            dualCounter = getRequired(parser, list, opt);
         }
         else if (opt.equals("label-prefix"))
         {
            labelPrefix = getOptional(parser, list, opt);
         }
         else if (opt.equals("dual-prefix"))
         {
            dualPrefix = getOptional(parser, list, opt);
         }
         else if (opt.equals("sort-suffix"))
         {
            String val = getChoice(parser, list, opt, "none", "non-unique");

            if (val.equals("none"))
            {
               sortSuffixOption = SORT_SUFFIX_NONE;
            }
            else if (val.equals("non-unique"))
            {
               sortSuffixOption = SORT_SUFFIX_NON_UNIQUE;
            }
         }
         else if (opt.equals("sort-suffix-marker"))
         {
            sortSuffixMarker = replaceHex(getOptional(parser, "", list, opt));
         }
         else if (opt.equals("sort"))
         {
            sort = getOptional(parser, "doc", list, opt);

            if (sort.equals("none") || sort.equals("unsrt"))
            {
               sort = null;
            }
         }
         else if (opt.equals("sort-rule"))
         {
            sortRules = replaceHex(getRequired(parser, list, opt));
         }
         else if (opt.equals("dual-sort-rule"))
         {
            dualSortRules = replaceHex(getRequired(parser, list, opt));
         }
         else if (opt.equals("secondary-sort-rule"))
         {
            secondarySortRules = replaceHex(getRequired(parser, list, opt));
         }
         else if (opt.equals("date-sort-format"))
         {
            dateSortFormat = getRequired(parser, list, opt);
         }
         else if (opt.equals("dual-date-sort-format"))
         {
            dualDateSortFormat = getRequired(parser, list, opt);
         }
         else if (opt.equals("secondary-date-sort-format"))
         {
            secondaryDateSortFormat = getRequired(parser, list, opt);
         }
         else if (opt.equals("date-sort-locale"))
         {
            dateSortLocale = getRequired(parser, list, opt);
         }
         else if (opt.equals("dual-date-sort-locale"))
         {
            dualDateSortLocale = getRequired(parser, list, opt);
         }
         else if (opt.equals("secondary-date-sort-locale"))
         {
            secondaryDateSortLocale = getRequired(parser, list, opt);
         }
         else if (opt.equals("group"))
         {
            if (bib2gls.useGroupField())
            {
               groupField = getOptional(parser, "auto", list, opt);

               if (groupField.equals("auto"))
               {
                  groupField = null;
               }
            }
            else
            {
               bib2gls.warning(bib2gls.getMessage(
                 "warning.group.option.required", opt, "--group"));
               groupField = null;
            }
         }
         else if (opt.equals("shuffle"))
         {
            long seed = getOptionalLong(parser, 0L, list, opt);

            if (seed == 0L)
            {
               random = new Random();
            }
            else
            {
               random = new Random(seed);
            }

            sort = "random";
            flatten = true;
         }
         else if (opt.equals("dual-sort"))
         {
            dualSort = getOptional(parser, "doc", list, opt);

            if (dualSort.equals("unsrt"))
            {
               dualSort = "none";
            }
         }
         else if (opt.equals("sort-field"))
         {
            sortField = getRequired(parser, list, opt);

            if (!sortField.equals("id") && !bib2gls.isKnownField(sortField))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, sortField));
            }
         }
         else if (opt.equals("dual-sort-field"))
         {
            dualSortField = getRequired(parser, list, opt);

            if (!dualSortField.equals("id") 
             && !bib2gls.isKnownField(dualSortField))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", 
                    opt, dualSortField));
            }
         }
         else if (opt.equals("charset"))
         {
            bibCharset = Charset.forName(getRequired(parser, list, opt));
         }
         else if (opt.equals("min-loc-range"))
         {
            minLocationRange = getRequiredIntGe(parser, 2, 
               "none", Integer.MAX_VALUE, list, opt);
         }
         else if (opt.equals("loc-gap"))
         {
            bib2gls.warning(bib2gls.getMessage("warning.deprecated",
              opt, "max-loc-diff"));
            locGap = getRequiredIntGe(parser, 1, list, opt);
         }
         else if (opt.equals("max-loc-diff"))
         {
            locGap = getRequiredIntGe(parser, 1, list, opt);
         }
         else if (opt.equals("suffixF"))
         {
            suffixF = getOptional(parser, "", list, opt);

            if (suffixF.equals("none"))
            {
               suffixF = null;
            }
         }
         else if (opt.equals("suffixFF"))
         {
            suffixFF = getOptional(parser, "", list, opt);

            if (suffixFF.equals("none"))
            {
               suffixFF = null;
            }
         }
         else if (opt.equals("see"))
         {
            String val = getChoice(parser, list, opt, "omit", "before", "after");

            if (val.equals("omit"))
            {
               seeLocation = Bib2GlsEntry.NO_SEE;
            }
            else if (val.equals("before"))
            {
               seeLocation = Bib2GlsEntry.PRE_SEE;
            }
            else if (val.equals("after"))
            {
               seeLocation = Bib2GlsEntry.POST_SEE;
            }
         }
         else if (opt.equals("seealso"))
         {
            String val = getChoice(parser, list, opt, "omit", "before", "after");

            if (val.equals("omit"))
            {
               seeAlsoLocation = Bib2GlsEntry.NO_SEE;
            }
            else if (val.equals("before"))
            {
               seeAlsoLocation = Bib2GlsEntry.PRE_SEE;
            }
            else if (val.equals("after"))
            {
               seeAlsoLocation = Bib2GlsEntry.POST_SEE;
            }
         }
         else if (opt.equals("loc-counters"))
         {
            String[] values = getStringArray(parser, list, opt);

            if (values == null || values.length == 0)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.missing.value", opt));
            }
            else if (values.length == 1)
            {
               if (values[0].equals("as-use"))
               {
                  counters = null;
               }
               else
               {
                  counters = values;
               }
            }
            else
            {
               counters = values;
            }
         }
         else if (opt.equals("loc-prefix"))
         {
            String[] values = getStringArray(parser, "true", list, opt);

            defpagesname=false;

            if (values.length == 1)
            {
               if (values[0].equals("false"))
               {
                  locationPrefix = null;
               }
               else if (values[0].equals("list"))
               {
                  locationPrefix = new String[] {"\\pagelistname "};
               }
               else if (values[0].equals("true"))
               {
                  locationPrefix = new String[]{"\\bibglspagename ",
                    "\\bibglspagesname "};
                  defpagesname=true;
               }
               else
               {
                  locationPrefix = values;
               }
            }
            else
            {
               locationPrefix = values;
            }
         }
         else if (opt.equals("loc-suffix"))
         {
            String[] values = getStringArray(parser, "\\@.", list, opt);

            if (values.length == 1)
            {
               if (values[0].equals("false"))
               {
                  locationSuffix = null;
               }
               else
               {
                  locationSuffix = values;
               }
            }
            else
            {
               locationSuffix = values;
            }
         }
         else if (opt.equals("ignore-fields"))
         {
            skipFields = getStringArray(parser, list, opt);
         }
         else if (opt.equals("selection"))
         {
            String val = getChoice(parser, list, opt, SELECTION_OPTIONS);

            selectionMode = -1;

            for (int i = 0; i < SELECTION_OPTIONS.length; i++)
            {
               if (val.equals(SELECTION_OPTIONS[i]))
               {
                  selectionMode = i;
                  break;
               }
            }
         }
         else if (opt.equals("break-at"))
         {
            String val = getChoice(parser, list, opt, "none", "word",
              "character", "sentence");

            if (val.equals("none"))
            {
               breakPoint = Bib2GlsEntryComparator.BREAK_NONE;
            }
            else if (val.equals("word"))
            {
               breakPoint = Bib2GlsEntryComparator.BREAK_WORD;
            }
            else if (val.equals("character"))
            {
               breakPoint = Bib2GlsEntryComparator.BREAK_CHAR;
            }
            else if (val.equals("sentence"))
            {
               breakPoint = Bib2GlsEntryComparator.BREAK_SENTENCE;
            }
         }
         else if (opt.equals("break-marker"))
         {
            breakPointMarker = getOptional(parser, "", list, opt);
         }
         else if (opt.equals("strength"))
         { // collator strength

            String val = getChoice(parser, list, opt, "primary", "secondary",
               "tertiary", "identical");

            if (val.equals("primary"))
            {
               collatorStrength = Collator.PRIMARY;
            }
            else if (val.equals("secondary"))
            {
               collatorStrength = Collator.SECONDARY;
            }
            else if (val.equals("tertiary"))
            {
               collatorStrength = Collator.TERTIARY;
            }
            else if (val.equals("identical"))
            {
               collatorStrength = Collator.IDENTICAL;
            }
         }
         else if (opt.equals("decomposition"))
         { // collator decomposition

            String val = getChoice(parser, list, opt, "none", "canonical",
              "full");

            if (val.equals("none"))
            {
               collatorDecomposition = Collator.NO_DECOMPOSITION;
            }
            else if (val.equals("canonical"))
            {
               collatorDecomposition = Collator.CANONICAL_DECOMPOSITION;
            }
            else if (val.equals("full"))
            {
               collatorDecomposition = Collator.FULL_DECOMPOSITION;
            }
         }
         else
         {
            throw new IllegalArgumentException(
             bib2gls.getMessage("error.syntax.unknown_option", opt));
         }
      }

      if (supplemental != null)
      {
         parseSupplemental(parser, supplemental);
      }

      if (master != null)
      {
         parseMaster(parser, master);
      }

      if ("doc".equals(sort))
      {
         sort = bib2gls.getDocDefaultLocale();
      }

      if ("doc".equals(dualSort))
      {
         dualSort = bib2gls.getDocDefaultLocale();
      }

      if ("doc".equals(dateSortLocale))
      {
         dateSortLocale = bib2gls.getDocDefaultLocale();
      }

      if ("doc".equals(dualDateSortLocale))
      {
         dualDateSortLocale = bib2gls.getDocDefaultLocale();
      }

      if ("custom".equals(sort) && sortRules == null)
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", "sort=custom", "sort-rules"));
      }

      if ("custom".equals(dualSort) && dualSortRules == null)
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", "dual-sort=custom", 
          "dual-sort-rules"));
      }

      if ("custom".equals(secondarySort) && secondarySortRules == null)
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", 
          secondaryField == null ?
          String.format("secondary={%s:%s}", secondarySort, secondaryType) :
          String.format("secondary={%s:%s:%s}", secondarySort,
            secondaryField, secondaryType), 
           "secondary-sort-rules"));
      }

      if (selectionMode == SELECTION_ALL && "use".equals(sort))
      {
         bib2gls.warning(
            bib2gls.getMessage("warning.option.clash", "selection=all",
            "sort=use"));

         sort = null;
      }

      if ((labelPrefix == null && dualPrefix == null)
        ||(labelPrefix != null && dualPrefix != null
           && labelPrefix.equals(dualPrefix)))
      {
         throw new IllegalArgumentException(
            bib2gls.getMessage("error.option.clash",
            String.format("label-prefix={%s}", 
               labelPrefix == null ? "" : labelPrefix),
            String.format("dual-prefix={%s}", 
               dualPrefix == null ? "" : dualPrefix)));
      }

      if (dualEntryMap == null)
      {
         dualEntryMap = new HashMap<String,String>();
         dualEntryMap.put("name", "description");
         dualEntryMap.put("plural", "descriptionplural");
         dualEntryMap.put("description", "name");
         dualEntryMap.put("descriptionplural", "plural");

         dualEntryFirstMap = "name";
      }

      if (dualAbbrevMap == null)
      {
         dualAbbrevMap = new HashMap<String,String>();
         dualAbbrevMap.put("short", "dualshort");
         dualAbbrevMap.put("shortplural", "dualshortplural");
         dualAbbrevMap.put("long", "duallong");
         dualAbbrevMap.put("longplural", "duallongplural");
         dualAbbrevMap.put("dualshort", "short");
         dualAbbrevMap.put("dualshortplural", "shortplural");
         dualAbbrevMap.put("duallong", "long");
         dualAbbrevMap.put("duallongplural", "longplural");

         dualAbbrevFirstMap = "short";
      }

      if (dualEntryAbbrevMap == null)
      {
         dualEntryAbbrevMap = new HashMap<String,String>();
         dualEntryAbbrevMap.put("long", "name");
         dualEntryAbbrevMap.put("short", "text");

         dualEntryAbbrevFirstMap = "long";
      }

      if (dualSymbolMap == null)
      {
         dualSymbolMap = new HashMap<String,String>();
         dualSymbolMap.put("name", "symbol");
         dualSymbolMap.put("plural", "symbolplural");
         dualSymbolMap.put("symbol", "name");
         dualSymbolMap.put("symbolplural", "plural");

         dualSymbolFirstMap = "name";
      }

      if (dualSort == null)
      {
         dualSort = "combine";
      }
      else if (dualSort.equals("none"))
      {
         dualSort = null;
      }

      if (dualSortField == null)
      {
         dualSortField = sortField;
      }

      if (bib2gls.getVerboseLevel() > 0)
      {
         bib2gls.logMessage();
         bib2gls.verbose(bib2gls.getMessage("message.selection.mode", 
          SELECTION_OPTIONS[selectionMode]));

         if (skipFields != null)
         {
            bib2gls.verbose(bib2gls.getMessage("message.ignore.fields")); 

            for (int i = 0; i < skipFields.length; i++)
            {
               bib2gls.verbose(skipFields[i]);
            }

            bib2gls.logMessage();
         }

         bib2gls.verbose(bib2gls.getMessage("message.sort.mode", 
          sort == null ? "none" : sort));

         bib2gls.verbose(bib2gls.getMessage("message.sort.field", 
          sortField));

         bib2gls.verbose(bib2gls.getMessage("message.label.prefix", 
            labelPrefix == null ? "" : labelPrefix));

         bib2gls.verbose(bib2gls.getMessage("message.dual.label.prefix", 
            dualPrefix == null ? "" : dualPrefix));

         bib2gls.verbose(bib2gls.getMessage("message.dual.sort.mode", 
            dualSort == null ? "none" : dualSort));

         bib2gls.verbose(bib2gls.getMessage("message.dual.sort.field", 
            dualSortField));

         bib2gls.logMessage();
         bib2gls.verbose(bib2gls.getMessage("message.dual.entry.mappings")); 

         for (Iterator<String> it = dualEntryMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualEntryMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verbose(bib2gls.getMessage("message.dual.symbol.mappings")); 

         for (Iterator<String> it = dualSymbolMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualSymbolMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verbose(bib2gls.getMessage(
            "message.dual.abbreviation.mappings")); 

         for (Iterator<String> it = dualAbbrevMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualAbbrevMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verbose(bib2gls.getMessage(
            "message.dual.entryabbreviation.mappings")); 

         for (Iterator<String> it = dualEntryAbbrevMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualEntryAbbrevMap.get(key)));
         }

         bib2gls.logMessage();
      }

      if (srcList == null && master == null)
      {
         try
         {
            sources.add(bib2gls.getBibFilePath(parser, filename));
         }
         catch (FileNotFoundException e)
         {
            throw new Bib2GlsException(
              bib2gls.getMessage("error.missing.src", filename+".bib"), e);
         }
      }
   }

   private void parseMaster(TeXParser parser, String master)
    throws IOException,Bib2GlsException
   {
      // Check for option clashes:

      if (sources.size() > 0)
      {
         bib2gls.warning(
           bib2gls.getMessage("warning.option.clash", "master", "src"));

         sources.clear();
      }

      if (supplementalRecords != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "supplemental-locations"));
      }

      if (nameCaseChange != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "name-case-change"));
      }

      if (descCaseChange != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "description-case-change"));
      }

      if (shortCaseChange != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "short-case-change"));
      }

      if (dualShortCaseChange != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-short-case-change"));
      }

      if (shortPluralSuffix != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "short-plural-suffix"));
      }

      if (dualShortPluralSuffix != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-short-plural-suffix"));
      }

      if (fieldPatterns != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "match"));
      }

      if (secondaryType != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "secondary"));
      }

      if (externalPrefixes != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "ext-prefixes"));
      }

      if (flatten)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "flatten"));
      }

      if (setWidest)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "set-widest"));
      }

      if (dualType != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-type"));
      }

      if (dualField != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-field"));
      }

      if (dualCategory != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-category"));
      }

      if (!"dual.".equals(dualPrefix))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-prefix"));
      }

      if (dualSort != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-sort"));
      }

      if (!dualSortField.equals("sort"))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-sort"));
      }

      if (!"locale".equals(sort))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "sort"));
      }

      if (!sortField.equals("sort"))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "sort"));
      }

      if (bibCharset != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "charset"));
      }

      if (minLocationRange != 3)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "min-loc-range"));
      }

      if (locGap != 1)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "loc-gap"));
      }

      if (suffixF != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "suffixF"));
      }

      if (suffixFF != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "suffixFF"));
      }

      if (seeLocation != Bib2GlsEntry.POST_SEE)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "see"));
      }

      if (seeAlsoLocation != Bib2GlsEntry.POST_SEE)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "seealso"));
      }

      if (locationPrefix != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "loc-prefix"));
      }

      if (locationSuffix != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "loc-suffix"));
      }

      if (skipFields != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "ignore-fields"));
      }

      if (selectionMode != SELECTION_RECORDED_AND_DEPS)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "selection"));
      }

      if (collatorStrength != Collator.PRIMARY)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "strength"));
      }

      if (collatorDecomposition != Collator.CANONICAL_DECOMPOSITION)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "decomposition"));
      }

      // Need to find master aux file which should be relative to
      // the current directory. (Don't use kpsewhich to find it.)

      TeXPath path = new TeXPath(parser, master+".aux", false);

      bib2gls.checkReadAccess(path);

      File auxFile = path.getFile();

      // Has the category been set? If not, set it to 'master'

      if (category == null)
      {
         category = "master";
      }
     
      // Has the type been set? If not, set it to 'master'

      if (type == null)
      {
         type = "master";
      }

      // Need to parse aux file to find .bib sources, the value of
      // \glolinkprefix, the label prefix and the .glstex files.

      AuxParser auxParser = new AuxParser(bib2gls)
      {
         protected void addPredefined()
         {
            super.addPredefined();

            addAuxCommand("glsxtr@resource", 2);
            addAuxCommand("glsxtr@linkprefix", 1);
         }
      };

      masterPdfPath = new TeXPath(parser, master+".pdf", false);

      TeXParser auxTeXParser = auxParser.parseAuxFile(auxFile);

      Vector<AuxData> auxData = auxParser.getAuxData();

      masterGlsTeXPath = new Vector<TeXPath>();

      for (AuxData data : auxData)
      {
         String name = data.getName();

         if (name.equals("glsxtr@resource"))
         {// Only need the .glstex file name

            String basename = data.getArg(1).toString(auxTeXParser);

            if (masterSelection != null)
            {
               boolean found = false;

               for (String str : masterSelection)
               {
                  if (str.equals(basename))
                  {
                     found = true;
                     break;
                  }
               }

               if (!found) continue;
            }

            TeXPath glsPath = new TeXPath(parser, basename+".glstex", false);

            bib2gls.checkReadAccess(glsPath);

            masterGlsTeXPath.add(glsPath);
         }
         else if (name.equals("glsxtr@linkprefix"))
         {
            masterLinkPrefix = data.getArg(0).toString(auxTeXParser);
         }
      }

   }

   private void parseSupplemental(TeXParser parser, String basename)
    throws IOException
   {
      // Need to find supplemental aux file which should be relative to
      // the current directory. (Don't use kpsewhich to find it.)

      TeXPath path = new TeXPath(parser, basename+".aux", false);

      bib2gls.checkReadAccess(path);

      File auxFile = path.getFile();

      // Need to parse aux file to find records.

      AuxParser auxParser = new AuxParser(bib2gls)
      {
         protected void addPredefined()
         {
            super.addPredefined();

            addAuxCommand("glsxtr@record", 5);
         }
      };

      supplementalPdfPath = new TeXPath(parser, basename+".pdf", false);

      TeXParser auxTeXParser = auxParser.parseAuxFile(auxFile);

      Vector<AuxData> auxData = auxParser.getAuxData();

      supplementalRecords = new Vector<GlsRecord>();

      for (AuxData data : auxData)
      {
         if (data.getName().equals("glsxtr@record"))
         {
            supplementalRecords.add(new GlsRecord(
              data.getArg(0).toString(auxTeXParser),
              data.getArg(1).toString(auxTeXParser),
              data.getArg(2).toString(auxTeXParser),
              data.getArg(3).toString(auxTeXParser),
              data.getArg(4).toString(auxTeXParser)));
         }
      }

      if (supplementalCategory == null)
      {
         supplementalCategory = category;
      }
   }

   private String replaceHex(String original)
   {
      // Replace \\u<hex> sequences with the appropriate Unicode
      // characters.

      Pattern p = Pattern.compile("\\\\u ?([0-9A-Fa-f]+)");

      Matcher m = p.matcher(original);

      StringBuilder builder = new StringBuilder();
      int idx = 0;

      while (m.find())
      {
         String hex = m.group(1);

         builder.append(original.substring(idx, m.start()));

         builder.appendCodePoint(Integer.parseInt(hex, 16));

         idx = m.end();
      }

      builder.append(original.substring(idx));

      return builder.toString();
   }

   private boolean getBoolean(TeXParser parser, KeyValList list, String opt)
    throws IOException
   {
      String val = list.getValue(opt).toString(parser).trim();

      if (val.isEmpty() || val.equals("true"))
      {
         return true;
      }
      else if (val.equals("false"))
      {
         return false;
      }

      throw new IllegalArgumentException(
        bib2gls.getMessage("error.invalid.choice.value", 
         opt, val, "true, false"));
   }

   private TeXObject getRequiredObject(TeXParser parser, KeyValList list, 
      String opt)
    throws IOException
   {
      TeXObject obj = list.getValue(opt);

      if (obj == null)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.missing.value", opt));
      }

      if (obj instanceof TeXObjectList)
      {
         obj = trimList((TeXObjectList)obj);

         if (((TeXObjectList)obj).size() == 0)
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.missing.value", opt));
         }
      }

      return obj;
   }

   private String getRequired(TeXParser parser, KeyValList list, String opt)
    throws IOException
   {
      TeXObject obj = list.getValue(opt);

      if (obj == null)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.missing.value", opt));
      }

      if (obj instanceof TeXObjectList)
      {
         obj = trimList((TeXObjectList)obj);
      }

      String value = obj.toString(parser).trim();

      if (value.isEmpty())
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.missing.value", opt));
      }

      return value;
   }

   private String getOptional(TeXParser parser, String defValue, 
      KeyValList list, String opt)
    throws IOException
   {
      TeXObject obj = list.getValue(opt);

      if (obj == null) return defValue;

      if (obj instanceof TeXObjectList)
      {
         obj = trimList((TeXObjectList)obj);
      }

      String value = obj.toString(parser).trim();

      if (value.isEmpty())
      {
         return defValue;
      }

      return value;
   }

   private String getOptional(TeXParser parser, KeyValList list, String opt)
    throws IOException
   {
      return getOptional(parser, null, list, opt);
   }

   private int getRequiredInt(TeXParser parser, KeyValList list, String opt)
    throws IOException
   {
      String value = getRequired(parser, list, opt);

      try
      {
         return Integer.parseInt(value);
      }
      catch (NumberFormatException e)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.int.value", opt, value), e);
      }
   }

   private int getOptionalInt(TeXParser parser, int defValue, KeyValList list, 
     String opt)
    throws IOException
   {
      String value = getOptional(parser, list, opt);

      if (value == null)
      {
         return defValue;
      }

      try
      {
         return Integer.parseInt(value);
      }
      catch (NumberFormatException e)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.int.value", opt, value), e);
      }
   }

   private int getRequiredInt(TeXParser parser, String keyword, 
       int keywordValue, KeyValList list, String opt)
    throws IOException
   {
      String value = getRequired(parser, list, opt);

      if (value.equals(keyword))
      {
         return keywordValue;
      }

      try
      {
         return Integer.parseInt(value);
      }
      catch (NumberFormatException e)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.int.value", opt, value), e);
      }
   }

   private int getRequiredIntGe(TeXParser parser, int minValue, KeyValList list,
      String opt)
    throws IOException
   {
      int val = getRequiredInt(parser, list, opt);

      if (val < minValue)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.minint.value", opt, val, 
             minValue));
      }

      return val;
   }

   private int getRequiredIntGe(TeXParser parser, int minValue, String keyword, 
      int keywordValue, KeyValList list, String opt)
    throws IOException
   {
      int val = getRequiredInt(parser, keyword, keywordValue, list, opt);

      if (val < minValue)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.minint.value", opt, val, 
             minValue));
      }

      return val;
   }

   private int getOptionalIntGe(TeXParser parser, int minValue, int defValue, 
     KeyValList list, String opt)
    throws IOException
   {
      int val = getOptionalInt(parser, defValue, list, opt);

      if (val < minValue)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.minint.value", opt, val, 
             minValue));
      }

      return val;
   }

   private long getRequiredLong(TeXParser parser, KeyValList list, String opt)
    throws IOException
   {
      String value = getRequired(parser, list, opt);

      try
      {
         return Long.parseLong(value);
      }
      catch (NumberFormatException e)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.int.value", opt, value), e);
      }
   }

   private long getOptionalLong(TeXParser parser, long defValue, 
     KeyValList list, String opt)
    throws IOException
   {
      String value = getOptional(parser, list, opt);

      if (value == null)
      {
         return defValue;
      }

      try
      {
         return Long.parseLong(value);
      }
      catch (NumberFormatException e)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.int.value", opt, value), e);
      }
   }

   private String getChoice(TeXParser parser, KeyValList list, String opt, 
      String... allowValues)
    throws IOException
   {
      return getChoice(parser, null, list, opt, allowValues);
   }

   private String getChoice(TeXParser parser, String defVal, KeyValList list, 
      String opt, String... allowValues)
    throws IOException
   {
      String value = getOptional(parser, list, opt);

      if (value == null)
      {
         if (defVal == null)
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.missing.value", opt));
         }
         else
         {
            return defVal;
         }
      }

      StringBuilder builder = null;

      for (String choice : allowValues)
      {
          if (value.equals(choice))
          {
             return value;
          }

          if (builder == null)
          {
             builder = new StringBuilder();
          }
          else
          {
             builder.append(", ");
          }

          builder.append('\'');
          builder.append(choice);
          builder.append('\'');
      }

      throw new IllegalArgumentException(
        bib2gls.getMessage("error.invalid.choice.value", 
         opt, value, builder));
   }

   private String[] getStringArray(TeXParser parser, String defValue, 
     KeyValList list, String opt)
    throws IOException
   {
      String[] array = getStringArray(parser, list, opt);

      if (array == null)
      {
         return new String[] {defValue};
      }

      return array;
   }

   private String[] getStringArray(TeXParser parser, KeyValList list, 
     String opt)
    throws IOException
   {
      CsvList csvList = CsvList.getList(parser, list.getValue(opt));

      int n = csvList.size();

      if (n == 0)
      {
         return null;
      }

      String[] array = new String[n];

      for (int i = 0; i < n; i++)
      {
         TeXObject obj = csvList.getValue(i);

         if (obj instanceof TeXObjectList)
         {
            obj = trimList((TeXObjectList)obj);
         }

         array[i] = obj.toString(parser).trim();
      }

      return array;
   }

   private TeXObject[] getTeXObjectArray(TeXParser parser, KeyValList list, 
     String opt)
    throws IOException
   {
      CsvList csvList = CsvList.getList(parser, list.getValue(opt));

      int n = csvList.size();

      if (n == 0)
      {
         return null;
      }

      TeXObject[] array = new TeXObject[n];

      for (int i = 0; i < n; i++)
      {
         TeXObject obj = csvList.getValue(i);

         if (obj instanceof TeXObjectList)
         {
            obj = trimList((TeXObjectList)obj);
         }

         array[i] = obj;
      }

      return array;
   }

   private CsvList[] getListArray(TeXParser parser, KeyValList list, 
     String opt)
    throws IOException
   {
      TeXObject object = list.getValue(opt);

      if (object instanceof TeXObjectList)
      {
         object = trimList((TeXObjectList)object);
      }

      CsvList csvList = CsvList.getList(parser, object);

      int n = csvList.size();

      if (n == 0)
      {
         return null;
      }

      CsvList[] array = new CsvList[n];

      for (int i = 0; i < n; i++)
      {
         TeXObject obj = csvList.getValue(i);

         if (obj instanceof TeXObjectList)
         {
            obj = trimList((TeXObjectList)obj);
         }

         array[i] = CsvList.getList(parser, obj);
      }

      return array;
   }

   private HashMap<String,String> getDualMap(TeXParser parser, KeyValList list, 
     String opt, String[] keys)
    throws IOException
   {
      CsvList[] array = getListArray(parser, list, opt);

      if (array == null)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.missing.value", opt));
      }

      if (array.length != 2)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.list.size", opt, 2));
      }

      int n = array[0].size();

      if (n != array[1].size())
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.dual.map", opt, 
             list.get(opt).toString(parser), n,
             array[1].size()));
      }

      HashMap<String,String> map = new HashMap<String,String>();

      for (int i = 0; i < n; i++)
      {
         TeXObject obj1 = array[0].getValue(i);
         TeXObject obj2 = array[1].getValue(i);

         if (obj1 instanceof TeXObjectList)
         {
            obj1 = trimList((TeXObjectList)obj1);
         }

         if (obj2 instanceof TeXObjectList)
         {
            obj2 = trimList((TeXObjectList)obj2);
         }

         String key = obj1.toString(parser);
         String value = obj2.toString(parser);

         if (key.equals("alias") || value.equals("alias"))
         {
            throw new IllegalArgumentException(bib2gls.getMessage(
               "error.alias.map.forbidden"));
         }

         map.put(key, value);

         if (i < keys.length)
         {
            keys[i] = key;
         }
      }

      return map;
   }

   private KeyValList getKeyValList(TeXParser parser, KeyValList list, 
     String opt)
    throws IOException
   {
      TeXObject val = list.getValue(opt);

      if (val instanceof TeXObjectList)
      {
         val = trimList((TeXObjectList)val);
      }

      KeyValList sublist = KeyValList.getList(parser, val);

      if (sublist == null || sublist.size() == 0)
      {
         return null;
      }

      return sublist;
   }

   public static TeXObjectList trimList(TeXObjectList list)
   {
      // strip redundant white space and grouping

      while (list.size() > 0 && (list.get(0) instanceof WhiteSpace))
      {
         list.remove(0);
      }

      while (list.size() > 0
        && (list.lastElement() instanceof WhiteSpace))
      {
         list.remove(list.size()-1);
      }

      if (list.size() == 1
        && (list.get(0) instanceof Group))
      {
         list = ((Group)list.get(0)).toList(); 
      }

      return list;
   }

   private Vector<TeXObject> splitList(TeXParser parser, char c, 
      TeXObjectList list)
    throws IOException
   {
      while (list.size() == 1 && list.firstElement() instanceof TeXObjectList)
      {
         list = (TeXObjectList)list.firstElement();
      }

      if (list.size() == 0) return null;

      Vector<TeXObject> split = new Vector<TeXObject>();

      TeXObjectList element = new TeXObjectList();

      for (TeXObject obj : list)
      {
         if (obj instanceof CharObject && ((CharObject)obj).getCharCode() == c)
         {
            element = trimList(element);

            if (element.size() != 0)
            {
               split.add(element);
            }

            element = new TeXObjectList();
         }
         else
         {
            element.add(obj);
         }
      }

      element = trimList(element);

      if (element.size() != 0)
      {
         split.add(element);
      }

      return split;
   }

   private void stripUnknownFieldPatterns()
   {
      if (fieldPatterns == null) return;

      Vector<String> fields = new Vector<String>();

      for (Iterator<String> it = fieldPatterns.keySet().iterator();
           it.hasNext(); )
      {
         String field = it.next();

         if (!bib2gls.isKnownField(field) 
             && !field.equals(PATTERN_FIELD_ID)
             && !field.equals(PATTERN_FIELD_ENTRY_TYPE))
         {
            bib2gls.warning(bib2gls.getMessage("warning.unknown.field.pattern",
              field));

            fields.add(field);
         }
      }

      for (String field : fields)
      {
         fieldPatterns.remove(field);
      }

      if (fieldPatterns.size() == 0)
      {
         fieldPatterns = null;
      }
   }

   public void parse(TeXParser parser)
   throws IOException
   {
      stripUnknownFieldPatterns();

      bibData = new Vector<Bib2GlsEntry>();
      dualData = new Vector<Bib2GlsEntry>();

      Vector<GlsRecord> records = bib2gls.getRecords();
      Vector<GlsSeeRecord> seeRecords = bib2gls.getSeeRecords();

      Vector<Bib2GlsEntry> seeList = null;

      if (selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
      {
         seeList = new Vector<Bib2GlsEntry>();
      }

      for (TeXPath src : sources)
      {
         File bibFile = src.getFile();

         Charset srcCharset = bibCharset;

         if (srcCharset == null)
         {
            // search bib file for "% Encoding: <encoding>"

            BufferedReader reader = null;
            Pattern pattern = Pattern.compile("% Encoding: ([^\\s]+)");

            try
            {
               reader = new BufferedReader(new FileReader(bibFile));

               String line;
               int lineNum=0;

               while ((line = reader.readLine()) != null)
               {
                  lineNum++;
                  Matcher m = pattern.matcher(line);

                  if (m.matches())
                  {
                     String encoding = m.group(1);

                     try
                     {
                        srcCharset = Charset.forName(encoding);
                     }
                     catch (Exception e)
                     {
                        bib2gls.warning(bibFile, lineNum,
                         bib2gls.getMessage("warning.ignoring.unknown.encoding", 
                          encoding),
                         e);
                        srcCharset = bibCharset;
                     }

                     reader.close();
                     reader = null;
                     break;
                  }
               }
            }
            finally
            {
               if (reader != null)
               {
                  reader.close();
               }
            }
         }

         BibParser bibParserListener = new Bib2GlsBibParser(bib2gls,
            this, srcCharset);

         TeXParser texParser = bibParserListener.parseBibFile(bibFile);

         Vector<BibData> list = bibParserListener.getBibData();

         if (hasAliases() && aliasLocations == ALIAS_LOC_TRANS)
         {
            // Need to transfer records for aliased entries

            for (int i = 0; i < records.size(); i++)
            {
               GlsRecord record = records.get(i);

               Bib2GlsEntry entry = getBib2GlsEntry(record.getLabel(), list);

               if (entry == null) continue;

               String alias = entry.getFieldValue("alias");

               if (alias == null)
               {
                  entry.addRecord(record);
               }
               else
               {
                  Bib2GlsEntry target = getBib2GlsEntry(alias, list);

                  if (target == null)
                  {
                     bib2gls.warning(bib2gls.getMessage(
                        "warning.alias.not.found", alias, entry.getOriginalId(),
                          "alias-loc", "transfer"));
                  }
                  else
                  {
                     entry.addRecord(record);

                     GlsRecord targetRecord = (GlsRecord)record.clone();
                     targetRecord.setLabel(alias);

                     if (!records.contains(targetRecord))
                     {
                        bib2gls.debug(bib2gls.getMessage(
                           "message.adding.target.record", targetRecord, 
                             entry.getOriginalId()));
                        target.addRecord(targetRecord);
                        records.add(++i, targetRecord);
                     }
                  }
               }
            }
         }

         for (int i = 0; i < list.size(); i++)
         {
            BibData data = list.get(i);

            if (data instanceof Bib2GlsEntry)
            {
               Bib2GlsEntry entry = (Bib2GlsEntry)data;

               Bib2GlsEntry dual = null;

               if (entry instanceof Bib2GlsDualEntry)
               {
                  dual = ((Bib2GlsDualEntry)entry).createDual();
                  entry.setDual(dual);
                  dual.setDual(entry);
               }

               setType(entry);
               setCategory(entry);
               setCounter(entry);

               // does this entry have any records?

               boolean hasRecords = entry.hasRecords();
               boolean dualHasRecords = (dual != null && dual.hasRecords());

               if (aliasLocations != ALIAS_LOC_TRANS
                    || entry.getField("alias") == null)
               {
                  for (GlsRecord record : records)
                  {
                     if (record.getLabel().equals(entry.getId()))
                     {
                        entry.addRecord(record);
                        hasRecords = true;
                     }

                     if (dual != null)
                     {
                        if (record.getLabel().equals(dual.getId()))
                        {
                           dual.addRecord(record);
                           dualHasRecords = true;
                        }
                     }
                  }

                  // any 'see' records?

                  for (GlsSeeRecord record : seeRecords)
                  {
                     if (record.getLabel().equals(entry.getId()))
                     {
                        entry.addRecord(record);
                        hasRecords = true;
                     }

                     if (dual != null)
                     {
                        if (record.getLabel().equals(dual.getId()))
                        {
                           dual.addRecord(record);
                           dualHasRecords = true;
                        }
                     }
                  }
               }

               if (discard(entry))
               {
                  bib2gls.verbose(bib2gls.getMessage("message.discarding.entry",
                     entry.getId()));

                  continue;
               }

               bibData.add(entry);

               if (dual != null)
               {
                  if (discard(dual))
                  {
                     bib2gls.verbose(bib2gls.getMessage("message.discarding.entry",
                        dual.getId()));

                     continue;
                  }

                  if (dualSort.equals("combine"))
                  {
                     setDualType(dual);
                     setDualCategory(dual);
                     setDualCounter(dual);

                     bibData.add(dual);
                  }
                  else
                  {
                     dualData.add(dual);
                  }
               }

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                || selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
               {
                  if (hasRecords)
                  {
                     addDependencies(parser, entry, list);

                     if (dual != null)
                     {
                        bib2gls.addDependent(dual.getId());
                     }
                  }

                  if (dualHasRecords)
                  {
                     // Do the "see" or "seealso" fields need setting?

                     if (dual.getFieldValue("see") == null
                       && dual.getFieldValue("seealso") == null)
                     {
                        dual.initCrossRefs(parser);
                     }

                     for (Iterator<String> it = dual.getDependencyIterator();
                          it.hasNext(); )
                     {
                        String dep = it.next();

                        bib2gls.addDependent(dep);
                     }

                     bib2gls.addDependent(entry.getId());
                  }
               }

               if (!hasRecords && seeList != null)
               {
                  // Does the entry have a cross reference list?

                  entry.initCrossRefs(parser);

                  addCrossRefs(list, entry, entry.getCrossRefs(), seeList);

                  addCrossRefs(list, entry, entry.getAlsoCrossRefs(), seeList);
               }
            }
         }
      }

      if (seeList != null)
      {
         for (Bib2GlsEntry xrEntry : seeList)
         {
            if (xrEntry.hasRecords())
            {
               // cross-referenced entry has records, so need to 
               // add the referring entries if they haven't already
               // been added.

               for (Iterator<Bib2GlsEntry> it=xrEntry.getCrossRefdByIterator();
                 it.hasNext(); )
               {
                  Bib2GlsEntry entry = it.next();

                  bib2gls.addDependent(entry.getId());
               }
            }
         }
      }

      addSupplementalRecords();
   }


   private void addCrossRefs(Vector<BibData> list,
      Bib2GlsEntry entry, String[] xrList,
      Vector<Bib2GlsEntry> seeList)
   {
      if (xrList == null) return;

      for (String xr : xrList)
      {
         Bib2GlsEntry xrEntry = getBib2GlsEntry(xr, list);

         if (xrEntry != null)
         {
            xrEntry.addCrossRefdBy(entry);

            if (!seeList.contains(xrEntry))
            {
               seeList.add(xrEntry);
            }
         }
      }
   }

   private void addSupplementalRecords()
   {
      if (supplementalRecords != null)
      {
         for (GlsRecord record : supplementalRecords)
         {
            String label = record.getLabel();

            Bib2GlsEntry entry = getEntry(label, bibData);

            if (entry == null && labelPrefix != null
                && !label.startsWith(labelPrefix))
            {
               entry = getEntry(labelPrefix+label, bibData);
            }

            if (entry == null)
            {
               if (dualData == null)
               {
                  entry = getEntry(dualPrefix+label, bibData);
               }
               else
               {
                  entry = getEntry(label, dualData);

                  if (entry == null && !label.startsWith(dualPrefix))
                  {
                     entry = getEntry(dualPrefix+label, dualData);
                  }
               }
            }

            if (entry != null)
            {
               if (supplementalCategory != null)
               {
                  setCategory(entry, supplementalCategory);
               }

               entry.addSupplementalRecord(record);
            }
         }
      }
   }

   private void addDependencies(TeXParser parser, Bib2GlsEntry entry, 
     Vector<BibData> list)
    throws IOException
   {
      // does this entry have a "see" or "seealso" field?

      entry.initCrossRefs(parser);

      for (Iterator<String> it = entry.getDependencyIterator();
           it.hasNext(); )
      {
         String dep = it.next();

         // Has the dependency already been added or does it have
         // any records?  (Don't want to get stuck in an infinite loop!)

         if (bib2gls.isDependent(dep))
         {
            continue;
         }

         bib2gls.addDependent(dep);

         if (bib2gls.hasRecord(dep))
         {
            continue;
         }

         Bib2GlsEntry depEntry = getBib2GlsEntry(dep, list);

         if (depEntry != null)
         {
            // Does the dependant entry have dependencies?

            if (depEntry.hasDependencies())
            {
               addDependencies(parser, depEntry, list);
            }
         }
      }
   }

   public int processData()
      throws IOException,Bib2GlsException
   {
      if (masterGlsTeXPath == null)
      {
         return processBibData();
      }
      else
      {
         processMaster();
         return -1;
      }
   }

   private void processMaster()
      throws IOException,Bib2GlsException
   {
      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));

      // Already checked openout_any in init method

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(texFile, bib2gls.getTeXCharset().name());

         // Save original definition of \@glsxtr@s@longnewglossaryentry
         // and \glsxtr@newabbreviation

         writer.println(
           "\\let\\bibglsorgdefglossaryentry\\@glsxtr@s@longnewglossaryentry");
         writer.println(
           "\\let\\bibglsorgdefabbreviation\\glsxtr@newabbreviation");
         writer.println();

         writer.println(
           "\\renewcommand{\\@glsxtr@s@longnewglossaryentry}[3]{%");
         writer.format(
          " \\bibglsorgdefglossaryentry{%s#1}{#2,type={%s},category={%s}}{#3}%%%n",
          labelPrefix == null ? "" : labelPrefix, type, category);
         writer.println("}");
         writer.println();

         writer.println(
           "\\renewcommand{\\glsxtr@newabbreviation}[4]{%");
         writer.format(
          " \\bibglsorgdefabbreviation{#1,type={%s},category={%s}}{%s#2}{#3}{#4}%%%n",
          type, category, labelPrefix == null ? "" : labelPrefix);
         writer.println("}");
         writer.println();

         writer.format("\\provideignoredglossary*{%s}%n", type);
         writer.format("\\glssetcategoryattribute{%s}{targeturl}{%s}%n", 
           category, masterPdfPath);
         writer.format("\\glssetcategoryattribute{%s}{targetname}", 
           category);

         if (labelPrefix == null)
         {
            writer.format("{%s\\glslabel}%n", masterLinkPrefix);
         }
         else
         {
            writer.format(
              "{%s\\csname bibglsstrip%sprefix\\expandafter\\endcsname\\glslabel}%n", 
               masterLinkPrefix, category);

            writer.format("\\csdef{bibglsstrip%sprefix}%s#1{#1}%n", 
              category, labelPrefix);
         }

         for (TeXPath path : masterGlsTeXPath)
         {
            writer.println();
            writer.format("\\InputIfFileExists{%s}{}{}%n", path);
            writer.println();
         }

         // Restore original definitions
         writer.println(
           "\\let\\@glsxtr@s@longnewglossaryentry\\bibglsorgdefglossaryentry");

         writer.println(
           "\\let\\glsxtr@newabbreviation\\bibglsorgdefabbreviation");
      }
      finally
      {
         if (writer != null)
         {
            writer.close();
         }
      }
   }

   public void setPreamble(String content, BibValueList list)
    throws IOException
   {
      if (preamble == null)
      {
         preamble = content;
      }
      else
      {
         preamble += content;
      }

      if (list != null && interpretPreamble)
      {
         bib2gls.processPreamble(list);
      }
   }

   private void addHierarchy(Bib2GlsEntry childEntry, 
      Vector<Bib2GlsEntry> entries, Vector<Bib2GlsEntry> data)
   {
      String parentId = childEntry.getParent();

      if (parentId == null) return;

      // has parent already been added to entries?

      for (Bib2GlsEntry entry : entries)
      {
         if (entry.getId().equals(parentId))
         {
            // already added

            return;
         }
      }

      Bib2GlsEntry parent = getEntry(parentId, data);

      if (parent != null)
      {
         bib2gls.verbose(bib2gls.getMessage("message.added.parent", parentId));
         addHierarchy(parent, entries, data);
         entries.add(parent);
      }
   }

   public Bib2GlsEntry getEntry(String label)
   {
      Bib2GlsEntry entry = getEntry(label, bibData);

      if (entry != null)
      {
         return entry;
      }

      return getEntry(label, dualData);
   }

   public static Bib2GlsEntry getEntry(String label, Vector<Bib2GlsEntry> data)
   {
      for (Bib2GlsEntry entry : data)
      {
         if (entry.getId().equals(label))
         {
            return entry;
         }
      }

      return null;
   }

   public static Bib2GlsEntry getBib2GlsEntry(String label, Vector<BibData> data)
   {
      for (BibData entry : data)
      {
         if (entry instanceof Bib2GlsEntry 
             && ((Bib2GlsEntry)entry).getId().equals(label))
         {
            return (Bib2GlsEntry)entry;
         }
      }

      return null;
   }

   private void processData(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries,
      String entrySort, String entrySortRules, String entrySortField,
      String entryDateSortLocale, String entryDateSortFormat)
      throws Bib2GlsException
   {
      Vector<String> fields = bib2gls.getFields();
      Vector<GlsRecord> records = bib2gls.getRecords();
      Vector<GlsSeeRecord> seeRecords = bib2gls.getSeeRecords();

      if (selectionMode == SELECTION_ALL)
      {
         // select all entries

         for (Bib2GlsEntry entry : data)
         {
            entries.add(entry);
         }
      }
      else if (entrySort == null)
      {
         // add all entries that have been recorded in the order of
         // definition

         for (Bib2GlsEntry entry : data)
         {
            if (entry.hasRecords())
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
               }

               entries.add(entry);
            }
         }
      }
      else
      {
         // Add all recorded entries in order of records.
         // (This means they'll be in the correct order if sort=use)

         for (int i = 0; i < records.size(); i++)
         {
            GlsRecord record = records.get(i);

            Bib2GlsEntry entry = getEntry(record.getLabel(), data);

            if (entry != null && !entries.contains(entry))
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
               }

               entries.add(entry);
            }
         }

         for (int i = 0; i < seeRecords.size(); i++)
         {
            GlsSeeRecord record = seeRecords.get(i);

            Bib2GlsEntry entry = getEntry(record.getLabel(), data);

            if (entry != null && !entries.contains(entry))
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
               }

               entries.add(entry);
            }
         }

         if (supplementalRecords != null && supplementalSelection != null)
         {
            for (GlsRecord record : supplementalRecords)
            {
               String label = record.getLabel();
               Bib2GlsEntry entry = getEntry(label, data);

               if (entry != null && !entries.contains(entry))
               {
                  if (supplementalSelection.length == 1
                  && supplementalSelection[0].equals("all"))
                  {
                     if (selectionMode == SELECTION_RECORDED_AND_DEPS
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                       ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
                     {
                        addHierarchy(entry, entries, data);
                     }

                     entries.add(entry);
                  }
                  else
                  {
                     for (String selLabel : supplementalSelection)
                     {
                        if (selLabel.equals(label))
                        {
                           if (selectionMode == SELECTION_RECORDED_AND_DEPS
                             ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                             ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
                           {
                              addHierarchy(entry, entries, data);
                           }

                           entries.add(entry);
                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      processDepsAndSort(data, entries, entrySort, entrySortRules, 
        entrySortField, entryDateSortLocale, entryDateSortFormat);
   }

   private void processDepsAndSort(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries,
      String entrySort, String entrySortRules, String entrySortField,
      String entryDateSortLocale, String entryDateSortFormat)
      throws Bib2GlsException
   {
      // add any dependencies

      if (selectionMode == SELECTION_RECORDED_AND_DEPS
        ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
      {
         Vector<String> dependencies = bib2gls.getDependencies();

         for (String id : dependencies)
         {
            Bib2GlsEntry dep = getEntry(id, data);

            if (dep != null && !entries.contains(dep))
            {
               addHierarchy(dep, entries, data);
               entries.add(dep);
            }
         }
      }

      if (flattenLonely == FLATTEN_LONELY_PRE_SORT)
      {
         // Only add if not defined in the preamble

         L2HStringConverter listener = bib2gls.getInterpreterListener();

         if (listener != null)
         {
            TeXParser parser = listener.getParser();

            if (parser.getControlSequence("bibglsflattenedchildpresort")==null)
            {
               listener.putControlSequence(new FlattenedPreSort());
            }

            if (parser.getControlSequence("bibglsflattenedhomograph")==null)
            {
               listener.putControlSequence(new FlattenedHomograph());
            }
         }

         flattenLonelyChildren(entries);
      }

      // sort if required

      int entryCount = entries.size();

      if (entrySort != null && !entrySort.equals("use") && entryCount > 0)
      {
         sortData(entries, entrySort, entrySortField, entrySortRules,
                  entryDateSortLocale, entryDateSortFormat);
      }
   }

   private int processBibData()
      throws IOException,Bib2GlsException
   {
      if (bibData == null)
      {// shouldn't happen
         throw new NullPointerException(
            "No data (parse must come before processData)");
      }

      if (dualField != null)
      {
         bib2gls.addField(dualField);
      }

      // check field mapping keys

      checkFieldMaps(dualEntryMap, "dual-entry-map");
      checkFieldMaps(dualAbbrevMap, "dual-abbrv-map");
      checkFieldMaps(dualSymbolMap, "dual-symbol-map");
      checkFieldMaps(dualEntryAbbrevMap, "dual-entryabbrv-map");

      Vector<Bib2GlsEntry> entries = new Vector<Bib2GlsEntry>();

      processData(bibData, entries, sort, sortRules, sortField,
         dateSortLocale, dateSortFormat);

      Vector<Bib2GlsEntry> dualEntries = null;

      int entryCount = entries.size();

      if (dualData.size() > 0)
      {
         dualEntries = new Vector<Bib2GlsEntry>();

         // If dual-sort=use or none, use the same order as entries. 

         if (dualSort == null || dualSort.equals("use"))
         {
            for (Bib2GlsEntry entry : entries)
            {
               if (entry instanceof Bib2GlsDualEntry)
               {
                  Bib2GlsEntry dual = entry.getDual();

                  setDualType(dual);
                  setDualCategory(dual);
                  setDualCounter(dual);

                  dualEntries.add(dual);
               }
            }
         }
         else
         {
            for (Bib2GlsEntry dual : dualData)
            {
               setDualType(dual);
               setDualCategory(dual);
               setDualCounter(dual);

               dualEntries.add(dual);
            }
         }

         processDepsAndSort(dualData, dualEntries, dualSort,
           dualSortRules, dualSortField,
           dualDateSortLocale, dualDateSortFormat);

         entryCount += dualEntries.size();
      }

      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));

      // Already checked openout_any in init method

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(texFile, bib2gls.getTeXCharset().name());

         writer.println("\\providecommand{\\bibglsrange}[1]{#1}");
         writer.println("\\providecommand{\\bibglsinterloper}[1]{#1\\delimN }");
         writer.format("\\providecommand{\\bibglspassimname}{%s}%n",
             bib2gls.getMessage("tag.passim"));
         writer.println("\\providecommand{\\bibglspassim}{ \\bibglspassimname}");
         writer.println();

         if (counters != null)
         {
            writer.println("\\providecommand{\\bibglslocationgroup}[3]{#3}");
            writer.println("\\providecommand{\\bibglslocationgroupsep}{\\delimN}");
            writer.println();
         }

         if (supplementalRecords != null)
         {
            writer.println("\\providecommand{\\bibglssupplementalsep}{\\delimN}");
            writer.println("\\providecommand{\\bibglssupplemental}[2]{#2}");
            writer.println();
         }

         if (dualField != null)
         {
            writer.format("\\glsxtrprovidestoragekey{%s}{}{}%n%n",
               dualField);
         }

         if (seeLocation != Bib2GlsEntry.NO_SEE)
         {
            writer.println("\\providecommand{\\bibglsseesep}{, }");
            writer.println();
         }

         if (seeAlsoLocation != Bib2GlsEntry.NO_SEE)
         {
            writer.println("\\providecommand{\\bibglsseealsosep}{, }");
            writer.println();
         }

         boolean createHyperGroups = false;

         if (bib2gls.useGroupField())
         {
            writer.println("\\ifdef\\glsxtrsetgrouptitle");
            writer.println("{");

            // letter groups:

            writer.println("  \\providecommand{\\bibglslettergroup}[4]{#4#3}");
            writer.println("  \\providecommand{\\bibglslettergrouptitle}[4]{\\unexpanded{#1}}");
            writer.println("  \\providecommand{\\bibglssetlettergrouptitle}[1]{%");
            writer.println("    \\glsxtrsetgrouptitle{\\bibglslettergroup#1}{\\bibglslettergrouptitle#1}}");

            // other groups:

            writer.println("  \\providecommand{\\bibglsothergroup}[3]{glssymbols}");
            writer.println("  \\providecommand{\\bibglsothergrouptitle}[3]{\\glssymbolsgroupname}");
            writer.println("  \\providecommand{\\bibglssetothergrouptitle}[1]{%");
            writer.println("    \\glsxtrsetgrouptitle{\\bibglsothergroup#1}{\\bibglsothergrouptitle#1}}");

            // number groups

            writer.println("  \\providecommand{\\bibglsnumbergroup}[3]{glsnumbers}");
            writer.println("  \\providecommand{\\bibglsnumbergrouptitle}[3]{\\glsnumbersgroupname}");
            writer.println("  \\providecommand{\\bibglssetnumbergrouptitle}[1]{%");
            writer.println("    \\glsxtrsetgrouptitle{\\bibglsnumbergroup#1}{\\bibglsnumbergrouptitle#1}}");

            boolean requiresDateTime = sort.startsWith("datetime") 
               || (dualSort != null && dualSort.startsWith("datetime"))
               || (secondarySort != null && secondarySort.startsWith("datetime"));

            boolean requiresDate = sort.equals("date") || sort.equals("date-reverse")
               || (dualSort != null 
                    && (dualSort.equals("date") || dualSort.equals("date-reverse")))
               || (secondarySort != null 
                     && (secondarySort.equals("date") 
                         || secondarySort.equals("date-reverse")));

            boolean requiresTime = sort.startsWith("time") 
               || (dualSort != null && dualSort.startsWith("time"))
               || (secondarySort != null && secondarySort.startsWith("time"));

            // date-time groups

            if (requiresDateTime)
            {
               writer.println("  \\providecommand{\\bibglsdatetimegroup}[9]{#1#2\\@firstofone}");
               writer.println("  \\providecommand{\\bibglsdatetimegrouptitle}[9]{#1-#2\\@gobble}");
               writer.println("  \\providecommand{\\bibglssetdatetimegrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsdatetimegroup#1}{\\bibglsdatetimegrouptitle#1}}");
            }

            // date groups

            if (requiresDate)
            {
               writer.println("  \\providecommand{\\bibglsdategroup}[6]{#1#2#6}");
               writer.println("  \\providecommand{\\bibglsdategrouptitle}[6]{#1-#2}");
               writer.println("  \\providecommand{\\bibglssetdategrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsdategroup#1}{\\bibglsdategrouptitle#1}}");
            }

            // time groups

            if (requiresTime)
            {
               writer.println("  \\providecommand{\\bibglstimegroup}[7]{#1#2#7}");
               writer.println("  \\providecommand{\\bibglstimegrouptitle}[7]{#1}");
               writer.println("  \\providecommand{\\bibglssettimegrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglstimegroup#1}{\\bibglstimegrouptitle#1}}");
            }


            writer.println("}");
            writer.println("{");

            // old version of glossaries-extra.sty:

            writer.println("  \\providecommand{\\bibglslettergroup}[4]{#1}");
            writer.println("  \\providecommand{\\bibglsothergroup}[3]{glssymbols}");
            writer.println("  \\providecommand{\\bibglsnumbergroup}[3]{glsnumbers}");
            if (requiresDateTime)
            {
               writer.println("  \\providecommand{\\bibglsdatetimegroup}[9]{#1-#2\\@gobble}");
            }

            if (requiresDate)
            {
               writer.println("  \\providecommand{\\bibglsdategroup}[6]{#1-#2}");
            }

            if (requiresTime)
            {
               writer.println("  \\providecommand{\\bibglstimegroup}[6]{#1-#2}");
            }

            writer.println("  \\providecommand{\\bibglssetlettergrouptitle}[1]{}");
            writer.println("  \\providecommand{\\bibglssetothergrouptitle}[1]{}");
            writer.println("  \\providecommand{\\bibglssetnumbergrouptitle}[1]{}");
            writer.println("  \\providecommand{\\bibglssetdatetimegrouptitle}[1]{}");
            writer.println("  \\providecommand{\\bibglssetdategrouptitle}[1]{}");
            writer.println("  \\providecommand{\\bibglssettimegrouptitle}[1]{}");
            writer.println("}");

            writer.println();

            createHyperGroups = bib2gls.hyperrefLoaded()
                && bib2gls.createHyperGroupsOn();

            for (Iterator<String> it = groupTitleMap.keySet().iterator();
                it.hasNext(); )
            {
               String key = it.next();

               GroupTitle groupTitle = groupTitleMap.get(key);

               if (groupTitle.getType() == null)
               {
                  createHyperGroups = false;
               }

               writer.format("\\%s{%s}%n",
                  groupTitle.getCsSetName(), groupTitle);
            }

            writer.println();

            if (!createHyperGroups)
            {
               bib2gls.setCreateHyperGroups(false);
            }

            if (createHyperGroups)
            {
               writer.println("\\ifdef\\@glsnavhypertarget");
               writer.println("{");
               writer.println("  \\ifdef\\bibglsorgnavhypertarget");
               writer.println("  {}");
               writer.println("  {");
               writer.println("    \\let\\bibglsorgnavhypertarget\\@glsnavhypertarget");
               writer.println("  }");
               writer.println("  \\renewcommand*{\\@glsnavhypertarget}[3]{%");
               writer.println("    \\@glstarget{\\glsnavhyperlinkname{#1}{#2}}{#3}%");
               writer.println("  }");
               writer.println("  \\providecommand{\\bibglshypergroup}{\\@gls@hypergroup}");
               writer.println("}");
               writer.println("{");
               writer.println("  \\providecommand{\\bibglshypergroup}[2]{}");
               writer.println("}");

            }
            else if (bib2gls.hyperrefLoaded())
            { 
               writer.println("\\ifdef\\bibglsorgnavhypertarget");
               writer.println("{");
               writer.println("  \\let\\@glsnavhypertarget\\bibglsorgnavhypertarget");
               writer.println("}");
               writer.println("{}");
            }
         }

         if (locationPrefix != null)
         {
            writer.println("\\providecommand{\\bibglspostlocprefix}{\\ }");

            if (defpagesname)
            {
               writer.format("\\providecommand{\\bibglspagename}{%s}%n",
                 bib2gls.getMessage("tag.page"));
               writer.format("\\providecommand{\\bibglspagesname}{%s}%n",
                 bib2gls.getMessage("tag.pages"));
            }

            if (type == null)
            {
               writer.println("\\appto\\glossarypreamble{%");
            }
            else
            {
               writer.format("\\apptoglossarypreamble[%s]{%%%n", type);
            }

            writer.println(" \\providecommand{\\bibglslocprefix}[1]{%");
            writer.println("  \\ifcase#1");

            for (int i = 0; i < locationPrefix.length; i++)
            {
               writer.format("  \\%s %s\\bibglspostlocprefix%n",
                 (i == locationPrefix.length-1 ? "else" : "or"), 
                 locationPrefix[i]);
            }

            writer.println("  \\fi");
            writer.println(" }%");

            writer.println("}");
         }

         if (locationSuffix != null)
         {
            if (type == null)
            {
               writer.println("\\appto\\glossarypreamble{%");
            }
            else
            {
               writer.format("\\apptoglossarypreamble[%s]{%%%n", type);
            }

            writer.print(" \\providecommand{\\bibglslocsuffix}[1]{");

            if (locationSuffix.length == 1)
            {
               writer.print(locationSuffix[0]);
            }
            else
            {
               writer.format("\\ifcase#1 %s", locationSuffix[0]);

               for (int i = 1; i < locationSuffix.length; i++)
               {
                  writer.format("\\%s %s",
                      (i == locationSuffix.length-1 ? "else" : "or"), 
                      locationSuffix[i]);
               }

               writer.print("\\fi");
            }
            writer.println("}%");

            writer.println("}");
         }

         Vector<String> provided = new Vector<String>();

         if (preamble != null)
         {
            writer.println(preamble);
            writer.println();
         }

         Vector<Bib2GlsEntry> secondaryList = null;

         if (secondaryType != null)
         {
            secondaryList = new Vector<Bib2GlsEntry>(entryCount);
         }

         Vector<String> widestNames = null;
         Vector<Double> widest = null;
         Vector<String> dualWidestNames = null;
         Vector<Double> dualWidest = null;
         Font font = null;
         FontRenderContext frc = null;

         if (setWidest)
         {
            widestNames = new Vector<String>();
            widest = new Vector<Double>();

            // Just using the JVM's default serif font as a rough
            // guide to guess the width.
            font = new Font("Serif", 0, 12);
            frc = new FontRenderContext(null, false, false);
         }

         if (supplementalPdfPath != null && supplementalCategory != null)
         {
            writer.format(
              "\\glssetcategoryattribute{%s}{externallocation}{%s}%n%n", 
              supplementalCategory, supplementalPdfPath);
         }

         if (flattenLonely == FLATTEN_LONELY_POST_SORT
              || (saveChildCount && flattenLonely != FLATTEN_LONELY_PRE_SORT))
         {// Need to check parents before writing definitions.
          // This will already have been done if
          // flatten-lonely=presort

            flattenLonelyChildren(entries);
         }

         writer.println("\\providecommand*{\\bibglsflattenedhomograph}[2]{#1}");

         if (flattenLonely == FLATTEN_LONELY_POST_SORT)
         {
            writer.format("\\providecommand*{\\%s}[2]{#1, #2}%n",
              flattenLonelyCsName());
            writer.println();
         }
         else if (flattenLonely == FLATTEN_LONELY_PRE_SORT)
         {
            writer.format("\\providecommand*{\\%s}[2]{#1}%n",
              flattenLonelyCsName());
            writer.println();
         }

         for (int i = 0, n = entries.size(); i < n; i++)
         {
            Bib2GlsEntry entry = entries.get(i);

            bib2gls.verbose(entry.getId());

            if (saveLocations)
            {
               entry.updateLocationList(minLocationRange,
                 suffixF, suffixFF, seeLocation, seeAlsoLocation,
                 locationPrefix != null, locationSuffix != null,
                 locGap);
            }

            if (flattenLonely == FLATTEN_LONELY_FALSE && !saveChildCount)
            {
               checkParent(entry, i, entries);
            }

            if (createHyperGroups)
            {
               writeHyperGroupDef(entry, writer);
            }

            String csname = entry.getCsName();

            if (!provided.contains(csname))
            {
               entry.writeCsDefinition(writer);
               writer.println();
               provided.add(csname);
            }

            entry.writeBibEntry(writer);
            entry.writeLocList(writer);

            if (saveChildCount)
            {
               writer.format("\\GlsXtrSetField{%s}{childcount}{%d}%n",
                 entry.getId(), entry.getChildCount());
            }

            if (supplementalPdfPath != null 
                && supplementalCategory == null 
                && entry.supplementalRecordCount() > 0)
            {
               writer.format("\\glssetattribute{%s}{externallocation}{%s}%n", 
                 entry.getId(), supplementalPdfPath);
            }

            writer.println();

            if (secondaryList != null)
            {
               secondaryList.add(entry);
            }

            if (widestNames != null)
            {
               updateWidestName(entry, widestNames, widest, font, frc);
            }
         }

         if (dualEntries != null)
         {
            if (widestNames != null)
            {
               if (dualType != null && !dualType.equals(type))
               {
                  dualWidestNames = new Vector<String>();
                  dualWidest = new Vector<Double>();
               }
               else
               {
                  dualWidestNames = widestNames;
                  dualWidest = widest;
               }
            }

            if (flattenLonely == FLATTEN_LONELY_POST_SORT
                || (saveChildCount && flattenLonely != FLATTEN_LONELY_PRE_SORT))
            {// need to check parents before writing definitions

               flattenLonelyChildren(dualEntries);
            }

            for (int i = 0, n = dualEntries.size(); i < n; i++)
            {
               Bib2GlsEntry entry = dualEntries.get(i);

               bib2gls.verbose(entry.getId());

               if (saveLocations)
               {
                  entry.updateLocationList(minLocationRange,
                    suffixF, suffixFF, seeLocation, seeAlsoLocation,
                    locationPrefix != null,
                    locationSuffix != null,
                    locGap);
               }

               if (flattenLonely == FLATTEN_LONELY_FALSE && !saveChildCount)
               {
                  checkParent(entry, i, dualEntries);
               }

               if (createHyperGroups)
               {
                  writeHyperGroupDef(entry, writer);
               }

               String csname = entry.getCsName();

               if (!provided.contains(csname))
               {
                  entry.writeCsDefinition(writer);
                  writer.println();
                  provided.add(csname);
               }

               entry.writeBibEntry(writer);
               entry.writeLocList(writer);

               writer.println();

               if (secondaryList != null)
               {
                  secondaryList.add(entry);
               }

               if (dualWidestNames != null)
               {
                  updateWidestName(entry, dualWidestNames, 
                    dualWidest, font, frc);
               }
            }
         }

         if (secondaryList != null)
         {
            writer.format("\\provideignoredglossary*{%s}%n", secondaryType);

            if (secondarySort.equals("none") || secondarySort.equals("unsrt"))
            {
               for (Bib2GlsEntry entry : secondaryList)
               {
                  writer.format("\\glsxtrcopytoglossary{%s}{%s}",
                       entry.getId(), secondaryType);
                  writer.println();
               }
            }
            else if (secondarySort.equals("use"))
            {
               Vector<GlsRecord> records = bib2gls.getRecords();

               for (GlsRecord record : records)
               {
                  Bib2GlsEntry entry = getEntry(record.getLabel(), 
                     secondaryList);

                  if (entry != null)
                  {
                     writer.format("\\glsxtrcopytoglossary{%s}{%s}",
                       entry.getId(), secondaryType);
                     writer.println();
                  }
               }
            }
            else
            {
               sortData(secondaryList, secondarySort, 
                       secondaryField == null ? sortField : secondaryField,
                       secondarySortRules, secondaryDateSortLocale,
                       secondaryDateSortFormat);

               for (Bib2GlsEntry entry : secondaryList)
               {
                  writer.format("\\glsxtrcopytoglossary{%s}{%s}",
                       entry.getId(), secondaryType);
                  writer.println();
               }
            }
         }

         if (widestNames != null)
         {
            if (type != null)
            {
               writer.format("\\apptoglossarypreamble[%s]{", type);
            }

            StringBuilder builder = new StringBuilder();

            for (int i = 0, n = widestNames.size(); i < n; i++)
            {
               String name = widestNames.get(i);

               if (!name.isEmpty())
               {
                  builder.append(String.format(
                     "\\glssetwidest[%d]{%s}", i, name));
               }
            }

            writer.print(builder);

            if (type != null)
            {
               writer.println("}");
            }

            if (secondaryType != null)
            {
               writer.format("\\apptoglossarypreamble[%s]{%s}%n",
                 secondaryType, builder);
            }

            writer.println();

            if (dualWidestNames != null && dualWidestNames != widestNames)
            {
               writer.format("\\apptoglossarypreamble[%s]{", dualType);

               for (int i = 0, n = dualWidestNames.size(); i < n; i++)
               {
                  String name = dualWidestNames.get(i);

                  if (!name.isEmpty())
                  {
                     builder.append(String.format(
                        "\\glssetwidest[%d]{%s}", i, name));
                  }
               }

               writer.println("}");
            }
         }

         bib2gls.message(bib2gls.getChoiceMessage("message.written", 0,
            "entry", 3, entryCount, texFile.toString()));

      }
      finally
      {
         if (writer != null)
         {
            writer.close();
         }
      }

      return entryCount;
   }

   private void sortData(Vector<Bib2GlsEntry> entries, String entrySort,
     String entrySortField, String entrySortRules,
     String entryDateSortLocale, String entryDateSortFormat)
    throws Bib2GlsException
   {
      if (entrySort.equals("random"))
      {
         if (random == null)
         {
            random = new Random();
         }

         Collections.shuffle(entries, random);
      }
      else if (entrySort.equals("letter-case"))
      {
         Bib2GlsEntryLetterComparator comparator = 
            new Bib2GlsEntryLetterComparator(bib2gls, entries, 
              entrySort, entrySortField, false, false, 
              sortSuffixOption, sortSuffixMarker);

         comparator.sortEntries();
      }
      else if (entrySort.equals("letter-case-reverse"))
      {
         Bib2GlsEntryLetterComparator comparator = 
            new Bib2GlsEntryLetterComparator(bib2gls, entries, 
              entrySort, entrySortField, false, true,
              sortSuffixOption, sortSuffixMarker);

         comparator.sortEntries();
      }
      else if (entrySort.equals("letter-nocase"))
      {
         Bib2GlsEntryLetterComparator comparator = 
            new Bib2GlsEntryLetterComparator(bib2gls, entries, 
              entrySort, entrySortField, true, false,
              sortSuffixOption, sortSuffixMarker);

         comparator.sortEntries();
      }
      else if (entrySort.equals("letter-nocase-reverse"))
      {
         Bib2GlsEntryLetterComparator comparator = 
            new Bib2GlsEntryLetterComparator(bib2gls, entries, 
              entrySort, entrySortField, true, true,
              sortSuffixOption, sortSuffixMarker);

         comparator.sortEntries();
      }
      else if (entrySort.startsWith("integer") 
            || entrySort.startsWith("float")
            || entrySort.startsWith("double")
            || entrySort.startsWith("hex")
            || entrySort.startsWith("octal")
            || entrySort.startsWith("binary"))
      {
         Bib2GlsEntryNumericComparator comparator = 
            new Bib2GlsEntryNumericComparator(bib2gls, entries, 
              entrySort, entrySortField);

         comparator.sortEntries();
      }
      else if (entrySort.equals("date"))
      {
         Locale locale = null;

         if (entryDateSortLocale.equals("locale"))
         {
            locale = Locale.getDefault();
         }
         else
         {
            locale = Locale.forLanguageTag(entryDateSortLocale);
         }

         Bib2GlsEntryDateTimeComparator comparator = 
            new Bib2GlsEntryDateTimeComparator(bib2gls, entries, 
              entrySort, entrySortField, locale, 
               entryDateSortFormat, true, false, false);

         comparator.sortEntries();
      }
      else if (entrySort.equals("date-reverse"))
      {
         Locale locale = null;

         if (entryDateSortLocale.equals("locale"))
         {
            locale = Locale.getDefault();
         }
         else
         {
            locale = Locale.forLanguageTag(entryDateSortLocale);
         }

         Bib2GlsEntryDateTimeComparator comparator = 
            new Bib2GlsEntryDateTimeComparator(bib2gls, entries, 
              entrySort, entrySortField, locale, 
               entryDateSortFormat, true, false, true);

         comparator.sortEntries();
      }
      else if (entrySort.equals("datetime"))
      {
         Locale locale = null;

         if (entryDateSortLocale.equals("locale"))
         {
            locale = Locale.getDefault();
         }
         else
         {
            locale = Locale.forLanguageTag(entryDateSortLocale);
         }

         Bib2GlsEntryDateTimeComparator comparator = 
            new Bib2GlsEntryDateTimeComparator(bib2gls, entries, 
              entrySort, entrySortField, locale, 
               entryDateSortFormat, true, true, false);

         comparator.sortEntries();
      }
      else if (entrySort.equals("datetime-reverse"))
      {
         Locale locale = null;

         if (entryDateSortLocale.equals("locale"))
         {
            locale = Locale.getDefault();
         }
         else
         {
            locale = Locale.forLanguageTag(entryDateSortLocale);
         }

         Bib2GlsEntryDateTimeComparator comparator = 
            new Bib2GlsEntryDateTimeComparator(bib2gls, entries, 
              entrySort, entrySortField, locale, 
               entryDateSortFormat, true, true, true);

         comparator.sortEntries();
      }
      else if (entrySort.equals("custom"))
      {
         try
         {
            Bib2GlsEntryComparator comparator = 
               new Bib2GlsEntryComparator(bib2gls, entries, 
                  entrySortField,
                  collatorStrength, collatorDecomposition,
                  entrySortRules, breakPoint, breakPointMarker,
                  sortSuffixOption, sortSuffixMarker);

            comparator.sortEntries();
         }
         catch (ParseException e)
         {
            throw new Bib2GlsException(bib2gls.getMessage(
             "error.invalid.sort.rule", e.getMessage()), e);
         }
      }
      else
      {
         Locale locale = null;

         if (entrySort.equals("locale"))
         {
            locale = Locale.getDefault();
         }
         else
         {
            locale = Locale.forLanguageTag(entrySort);
         }

         Bib2GlsEntryComparator comparator = 
            new Bib2GlsEntryComparator(bib2gls, entries, 
               locale, entrySortField, 
               collatorStrength, collatorDecomposition,
               breakPoint, breakPointMarker,
               sortSuffixOption, sortSuffixMarker);

         comparator.sortEntries();
      }
   }

   private void updateWidestName(Bib2GlsEntry entry, 
     Vector<String> widestNames, 
     Vector<Double> widest, Font font, FontRenderContext frc)
   {
      // This is just approximate as fonts, commands etc
      // will affect the width.

      String name = entry.getFieldValue("name");

      if (name == null || name.isEmpty()) return;

      bib2gls.logMessage(bib2gls.getMessage("message.calc.text.width",
        entry.getId()));

      if (name.matches(".*[\\\\\\$\\{\\}].*"))
      {
         // Try to interpret any LaTeX code that may be in the name.
         // This assumes custom user commands are provided in the
         // preamble. Won't work on anything complicated and doesn't
         // take font changes into account.

         name = bib2gls.interpret(name, entry.getField("name")).trim();
      }

      int level = entry.getHierarchyCount();
      String maxName = "";
      double maxWidth = 0;

      if (level > 0)
      {
         level--;
      }

      if (level < widestNames.size())
      {
         maxName = widestNames.get(level);
         maxWidth = widest.get(level).doubleValue();
      }

      double w = 0.0;

      if (!name.isEmpty())
      {
         TextLayout layout = new TextLayout(name, font, frc);

         w = layout.getBounds().getWidth();
      }

      bib2gls.logMessage(bib2gls.getMessage("message.calc.text.width.result",
        name, w));

      if (w > maxWidth)
      {
          if (level < widestNames.size())
          {
             widestNames.set(level, 
                String.format("\\glsentryname{%s}", entry.getId()));
             widest.set(level, new Double(w));
          }
          else
          {
             for (int j = widestNames.size(); j < level; j++)
             {
                widestNames.add("");
                widest.add(new Double(0.0));
             }

             widestNames.add(String.format("\\glsentryname{%s}", 
                entry.getId()));
             widest.add(new Double(w));
          }
      }
   }

   private void checkParent(Bib2GlsEntry entry, int i, 
      Vector<Bib2GlsEntry> list)
   {
      if (flatten)
      {
         entry.removeFieldValue("parent");
         return;
      }

      String parentId = entry.getParent();

      if (parentId == null || parentId.isEmpty()) return;

      // has parent been added?

      /*
        Search backwards.
        (If entry has a parent it's more likely to be nearby
         with the default sort.)
      */

      for (int j = i-1; j >= 0; j--)
      {
         Bib2GlsEntry thisEntry = list.get(j);

         if (thisEntry.getId().equals(parentId))
         {
            if (flattenLonely != FLATTEN_LONELY_FALSE || saveChildCount)
            {
               thisEntry.addChild(entry);
            }

            return;
         }
      }

      for (int j = list.size()-1; j > i-1; j--)
      {
         Bib2GlsEntry thisEntry = list.get(j);

         if (thisEntry.getId().equals(parentId))
         {
            if (flattenLonely != FLATTEN_LONELY_FALSE || saveChildCount)
            {
               thisEntry.addChild(entry);
            }

            return;
         }
      }

      bib2gls.warning(bib2gls.getMessage(
         "warning.parent.missing", parentId, entry.getId()));
      entry.removeFieldValue("parent");
   }

   private String flattenLonelyCsName()
   {
      return flattenLonely == FLATTEN_LONELY_PRE_SORT ?
         "bibglsflattenedchildpresort" : "bibglsflattenedchildpostsort";
   }

   private void flattenChild(Bib2GlsEntry parent, Bib2GlsEntry child,
      Vector<Bib2GlsEntry> entries)
   {
      // The bib value fields only need to be set for
      // flatten-lonely=presort as they may be required for the
      // sort value.

      L2HStringConverter listener = null;
      BibValueList bibName = null;
      BibValueList bibParentName = null;

      if (flattenLonely == FLATTEN_LONELY_PRE_SORT)
      {
         listener = bib2gls.getInterpreterListener();
      }

      String name = child.getFieldValue("name");

      if (listener != null)
      {
         bibName = child.getField("name");
      }

      if (name == null)
      {
         name = child.getFallbackValue("name");

         if (listener != null && bibName == null)
         {
            bibName = child.getFallbackContents("name");
         }
      }

      // Does the child have a text field?

      String text = child.getFieldValue("text");

      if (text == null && name != null)
      {
         // Use the name field if present.

         child.putField("text", name);
      }

      String parentName = parent.getFieldValue("name");

      if (listener != null)
      {
         bibParentName = parent.getField("name");
      }

      if (parentName == null)
      {
         parentName = parent.getFallbackValue("name");

         if (listener != null)
         {
            bibParentName = parent.getFallbackContents("name");
         }
      }

      if (name != null)
      {
         boolean homograph = name.equals(parentName);

         String csName = homograph ? "bibglsflattenedhomograph" 
            : flattenLonelyCsName();

         TeXObjectList object = null;
         Group nameGroup = null;
         Group parentNameGroup = null;

         if (listener != null)
         {
            object = new TeXObjectList();
            object.add(new TeXCsRef(csName));

            if (bibName == null)
            {
               nameGroup = listener.createGroup(name);
            }
            else
            {
               nameGroup = getContents(bibName, listener);
            }

            if (bibParentName == null)
            {
               if (parentName == null)
               {
                  parentNameGroup = listener.createGroup();
               }
               else
               {
                  parentNameGroup = listener.createGroup(parentName);
               }
            }
            else
            {
               parentNameGroup = getContents(bibParentName, listener);
            }

            if (flattenLonely == FLATTEN_LONELY_POST_SORT)
            {
               object.add(parentNameGroup);
               object.add(nameGroup);
            }
            else
            {
               object.add(nameGroup);
               object.add(parentNameGroup);
            }
         }

         if (flattenLonely == FLATTEN_LONELY_POST_SORT)
         {
            child.putField("name", 
              String.format("\\%s{%s}{%s}",
               csName, parentName, name));
         }
         else
         {
            child.putField("name", 
              String.format("\\%s{%s}{%s}",
               csName, name, parentName));

            if (object != null)
            {
               BibValueList contents = new BibValueList();
               contents.add(new BibUserString(object));
               child.putField("name", contents);
            }
         }
      }

      // remove parent field

      child.moveUpHierarchy(entries);

      if (flattenLonely == FLATTEN_LONELY_POST_SORT)
      {
         // set the child's group to the parent's group, if
         // provided.

         String group = parent.getFieldValue("group");

         if (group != null)
         {
            child.putField("group", group);
         }
      }
   }


   private Group getContents(BibValueList bibValue, L2HStringConverter listener)
   {
      TeXObject contents = bibValue.getContents(true);

      if (contents instanceof TeXObjectList
       && !(contents instanceof MathGroup)
       && (((TeXObjectList)contents).size() == 1)
       && (((TeXObjectList)contents).firstElement() instanceof Group))
      {
         TeXObjectList list = (TeXObjectList)contents;

         Group grp = (Group)list.firstElement();

         if (!(grp instanceof MathGroup))
         {
            return grp;
         }
         else
         {
            grp = listener.createGroup();
            grp.add(list);
            return grp;
         }
      }
      else
      {
         Group grp = listener.createGroup();
         grp.add(contents);
         return grp;
      }
   }

   private void flattenLonelyChildren(Vector<Bib2GlsEntry> entries)
   {
      // The entries need to be traversed down the hierarchical
      // system otherwise moving a grandchild up the hierarchy
      // before the parent entry has been checked will confuse the
      // test.

      Hashtable<Integer,Vector<Bib2GlsEntry>> flattenMap 
      = new Hashtable<Integer,Vector<Bib2GlsEntry>>();

      // Key set needs to be ordered
      TreeSet<Integer> keys = new TreeSet<Integer>();

      Vector<Bib2GlsEntry> discardList = new Vector<Bib2GlsEntry>();

      // This has to be done first to ensure the parent-child
      // information is all correct.
      for (int i = 0, n = entries.size(); i < n; i++)
      {
         Bib2GlsEntry entry = entries.get(i);
         checkParent(entry, i, entries);
      }

      for (int i = 0, n = entries.size(); i < n; i++)
      {
         Bib2GlsEntry parent = entries.get(i);

         /*
          * Conditions for moving the child up one hierarchical
          * level:
          *
          * - The child should not have any siblings
          * - If flatten-lonely-rule = 'only unrecorded parents'
          *    the parent can't have records or cross-references
          *   Otherwise
          *    the parent may have records or cross-references 
          */  

         int childCount = parent.getChildCount();

         if (childCount != 1) continue;

         Bib2GlsEntry child = parent.getChild(0);

         boolean parentHasRecordsOrCrossRefs =
           parent.recordCount() >0 || parent.hasCrossRefs();

         if ( // flatten-lonely-rule != 'only unrecorded parents':
               flattenLonelyRule != FLATTEN_LONELY_RULE_ONLY_UNRECORDED_PARENTS
              // or parent doesn't have records or cross-references:
            || !parentHasRecordsOrCrossRefs)
         {
            Integer level = new Integer(child.getLevel(entries));

            Vector<Bib2GlsEntry> list = flattenMap.get(level);

            if (list == null)
            {
               list = new Vector<Bib2GlsEntry>();
               flattenMap.put(level, list);
               keys.add(level);
            }

            list.add(child);

            /*
             *  The parent will only be discarded if
             *   - parent has no records or cross-references
             *   - flatten-lonely-rule != 'no discard'
             */

            if (flattenLonelyRule != FLATTEN_LONELY_RULE_NO_DISCARD
                 && !parentHasRecordsOrCrossRefs)
            {
               discardList.add(parent);
            }
         }
      }

      Iterator<Integer> it = keys.iterator();

      while (it.hasNext())
      {
         Integer level = it.next();

         Vector<Bib2GlsEntry> list = flattenMap.get(level);

         for (Bib2GlsEntry child : list)
         {
            String parentId = child.getParent();

            Bib2GlsEntry parent = Bib2GlsEntry.getEntry(parentId, entries);

            flattenChild(parent, child, entries);
         }
      }

      for (Bib2GlsEntry discard : discardList)
      {
         entries.remove(discard);
      }
   }

   public boolean flattenSort()
   {
      return flatten;
   }

   private void setType(Bib2GlsEntry entry)
   {
      if (type != null)
      {
         if (type.equals("same as entry"))
         {
            entry.putField("type", entry.getEntryType());
         }
         else
         {
            entry.putField("type", type);
         }
      }
   }

   private void setCategory(Bib2GlsEntry entry)
   {
      setCategory(entry, category);
   }

   private void setCategory(Bib2GlsEntry entry, String catLabel)
   {
      if (catLabel != null)
      {
         if (catLabel.equals("same as entry"))
         {
            entry.putField("category", entry.getEntryType());
         }
         else if (catLabel.equals("same as type"))
         {
            String val = entry.getFieldValue("type");

            if (val != null)
            {
               entry.putField("category", val);
            }
         }
         else
         {
            entry.putField("category", catLabel);
         }
      }
   }

   private void setCounter(Bib2GlsEntry entry)
   {
      if (counter != null)
      {
         entry.putField("counter", counter);
      }
   }

   private void setDualCounter(Bib2GlsEntry dual)
   {
      if (dualCounter != null)
      {
         if (dualCounter.equals("same as primary"))
         {
            String val = dual.getDual().getFieldValue("counter");

            if (val != null)
            {
               dual.putField("counter", val);
            }
         }
         else
         {
            dual.putField("counter", dualCounter);
         }
      }
   }

   private void setDualType(Bib2GlsEntry dual)
   {
      if (dualType != null)
      {
         if (dualType.equals("same as entry"))
         {
            dual.putField("type", dual.getEntryType());
         }
         else if (dualType.equals("same as primary"))
         {
            String val = dual.getDual().getFieldValue("type");

            if (val != null)
            {
               dual.putField("type", val);
            }
         }
         else
         {
            dual.putField("type", dualType);
         }
      }
   }

   private void setDualCategory(Bib2GlsEntry dual)
   {
      if (dualCategory != null)
      {
         if (dualCategory.equals("same as entry"))
         {
            dual.putField("category", dual.getEntryType());
         }
         else if (dualCategory.equals("same as primary"))
         {
            String val = dual.getDual().getFieldValue("category");

            if (val != null)
            {
               dual.putField("category", val);
            }
         }
         else if (dualCategory.equals("same as type"))
         {
            String val = dual.getFieldValue("type");

            if (val != null)
            {
               dual.putField("category", val);
            }
         }
         else
         {
            dual.putField("category", dualCategory);
         }
      }
   }

   public boolean hasSkippedFields()
   {
      return skipFields != null && skipFields.length != 0;
   }

   public String[] getSkipFields()
   {
      return skipFields;
   }

   public boolean skipField(String field)
   {
      if (skipFields == null)
      {
         return false;
      }

      for (int i = 0; i < skipFields.length; i++)
      {
         if (skipFields[i].equals(field))
         {
            return true;
         }
      }

      return false;
   }

   public String getLabelPrefix()
   {
      return labelPrefix;
   }

   public String getDualPrefix()
   {
      return dualPrefix;
   }

   public String getExternalPrefix(int idx)
   {
      if (externalPrefixes == null) return null;

      if (idx >= 1 && idx <= externalPrefixes.length)
      {
         return externalPrefixes[idx-1];
      }

      return null;
   }

   public String getDualSortField()
   {
      return dualSortField;
   }

   public String getPluralSuffix()
   {
      return pluralSuffix;
   }

   public String getShortPluralSuffix()
   {
      return shortPluralSuffix;
   }

   public String getDualPluralSuffix()
   {
      return dualPluralSuffix;
   }

   public String getDualShortPluralSuffix()
   {
      return dualShortPluralSuffix;
   }

   public HashMap<String,String> getDualEntryMap()
   {
      return dualEntryMap;
   }

   public String getFirstDualEntryMap()
   {
      return dualEntryFirstMap;
   }

   public boolean backLinkFirstDualEntryMap()
   {
      return backLinkDualEntry;
   }

   public HashMap<String,String> getDualSymbolMap()
   {
      return dualSymbolMap;
   }

   public String getFirstDualSymbolMap()
   {
      return dualSymbolFirstMap;
   }

   public boolean backLinkFirstDualSymbolMap()
   {
      return backLinkDualSymbol;
   }

   public HashMap<String,String> getDualAbbrevMap()
   {
      return dualAbbrevMap;
   }

   public HashMap<String,String> getDualEntryAbbrevMap()
   {
      return dualEntryAbbrevMap;
   }

   public String getFirstDualAbbrevMap()
   {
      return dualAbbrevFirstMap;
   }

   public String getFirstDualEntryAbbrevMap()
   {
      return dualEntryAbbrevFirstMap;
   }

   public boolean backLinkFirstDualAbbrevMap()
   {
      return backLinkDualAbbrev;
   }

   public boolean backLinkFirstDualEntryAbbrevMap()
   {
      return backLinkDualEntryAbbrev;
   }

   public String getDualField()
   {
      return dualField;
   }


   private void checkFieldMaps(HashMap<String,String> mapping, String optName)
    throws Bib2GlsException
   {
      for (Iterator<String> it = mapping.keySet().iterator();
              it.hasNext(); )
      {
         String key = it.next();

         if (!bib2gls.isKnownField(key))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.field", key, optName));
         }

         key = mapping.get(key);

         if (!bib2gls.isKnownField(key))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.field", key, optName));
         }
      }
   }

   // Allow for entries to be filtered out
   public boolean discard(Bib2GlsEntry entry)
   {
      if (fieldPatterns == null) return false;

      boolean discard = notMatch(entry);

      return notMatch ? !discard : discard;
   }

   private boolean notMatch(Bib2GlsEntry entry)
   {
      boolean matches = fieldPatternsAnd;

      for (Iterator<String> it = fieldPatterns.keySet().iterator();
           it.hasNext(); )
      {
         String field = it.next();

         String value = null;

         if (field.equals(PATTERN_FIELD_ID))
         {
            value = entry.getId();
         }
         else if (field.equals(PATTERN_FIELD_ENTRY_TYPE))
         {
            value = entry.getEntryType();
         }
         else
         {
            value = entry.getFieldValue(field);
         }

         if (value == null)
         {
            value = "";
         }

         Pattern p = fieldPatterns.get(field);

         Matcher m = p.matcher(value);

         boolean result = m.matches();

         bib2gls.debug(bib2gls.getMessage("message.pattern.info",
            p.pattern(), field, value, result));

         if (fieldPatternsAnd)
         {
            if (!result)
            {
               return true;
            }
         }
         else
         {
            if (result)
            {
               return false;
            }
         }
      }

      return !matches;
   }

   public boolean changeNameCase()
   {
      return nameCaseChange != null;
   }

   public boolean changeDescriptionCase()
   {
      return descCaseChange != null;
   }

   public boolean changeShortCase()
   {
      return shortCaseChange != null;
   }

   public boolean changeDualShortCase()
   {
      return dualShortCaseChange != null;
   }

   public BibValueList applyNameCaseChange(TeXParser parser, 
      BibValueList value)
    throws IOException
   {
      return applyCaseChange(parser, value, nameCaseChange);
   }

   public BibValueList applyDescriptionCaseChange(TeXParser parser, 
      BibValueList value)
    throws IOException
   {
      return applyCaseChange(parser, value, descCaseChange);
   }

   public BibValueList applyShortCaseChange(TeXParser parser, 
      BibValueList value)
    throws IOException
   {
      return applyCaseChange(parser, value, shortCaseChange);
   }

   public BibValueList applyDualShortCaseChange(TeXParser parser,
      BibValueList value)
    throws IOException
   {
      return applyCaseChange(parser, value, dualShortCaseChange);
   }

   private void toLowerCase(TeXObjectList list)
   {
      for (int i = 0, n = list.size(); i < n; i++)
      {
         TeXObject object = list.get(i);

         if (object instanceof CharObject)
         {
            int codePoint = ((CharObject)object).getCharCode();

            if (Character.isAlphabetic(codePoint))
            {
               codePoint = Character.toLowerCase(codePoint);
               ((CharObject)object).setCharCode(codePoint);
            }
         }
         else if (object instanceof TeXObjectList)
         {
            toLowerCase((TeXObjectList)object);
         }
         else if (object instanceof ControlSequence)
         {
            String csname = ((ControlSequence)object).getName();

            if (csname.equals("NoCaseChange") || csname.equals("ensuremath")
                 || csname.equals("si"))
            {
               // skip argument

               i++;

               while (i < n)
               {
                  object = list.get(i);

                  if (!(object instanceof Ignoreable))
                  {
                     break;
                  }

                  i++;
               }
            }
         }
      }
   }

   private void toUpperCase(TeXObjectList list)
   {
      for (int i = 0, n = list.size(); i < n; i++)
      {
         TeXObject object = list.get(i);

         if (object instanceof CharObject)
         {
            int codePoint = ((CharObject)object).getCharCode();

            if (Character.isAlphabetic(codePoint))
            {
               codePoint = Character.toUpperCase(codePoint);
               ((CharObject)object).setCharCode(codePoint);
            }
         }
         else if (object instanceof MathGroup)
         {// skip
            continue;
         }
         else if (object instanceof TeXObjectList)
         {
            toUpperCase((TeXObjectList)object);
         }
         else if (object instanceof ControlSequence)
         {
            String csname = ((ControlSequence)object).getName();

            if (csname.equals("NoCaseChange") || csname.equals("ensuremath")
                 || csname.equals("si"))
            {
               // skip argument

               i++;

               while (i < n)
               {
                  object = list.get(i);

                  if (!(object instanceof Ignoreable))
                  {
                     break;
                  }

                  i++;
               }
            }
         }
      }
   }

   private void toSentenceCase(TeXObjectList list)
   {
      for (int i = 0, n = list.size(); i < n; i++)
      {
         TeXObject object = list.get(i);

         if (object instanceof CharObject)
         {
            int codePoint = ((CharObject)object).getCharCode();

            if (Character.isAlphabetic(codePoint))
            {
               codePoint = Character.toTitleCase(codePoint);
               ((CharObject)object).setCharCode(codePoint);

               return;
            }
         }
         else if (object instanceof MathGroup)
         {
            return;
         }
         else if (object instanceof Group)
         {
            // upper case group contents

            toUpperCase((TeXObjectList)object);

            return;
         }
         else if (object instanceof ControlSequence)
         {
            String csname = ((ControlSequence)object).getName();

            if (csname.equals("protect")) continue;

            i++;

            while (i < n)
            {
               object = list.get(i);

               if (!(object instanceof Ignoreable))
               {
                  break;
               }

               i++;
            }

            if (csname.equals("NoCaseChange") || csname.equals("ensuremath")
                || csname.equals("si") )
            {
               continue;
            }

            // if a group follows the command, title case the group
            // otherwise finish.

            if (object instanceof Group && !(object instanceof MathGroup))
            {
               // title case argument

               toSentenceCase((TeXObjectList)object);
            }

            return;
         }
      }
   }

   public BibValueList applyCaseChange(TeXParser parser,
      BibValueList value, String change)
    throws IOException
   {
      if (change == null) return value;

     TeXObjectList list = BibValueList.stripDelim(value.expand(parser));

      BibValueList bibList = new BibValueList();

      if (change.equals("lc-cs"))
      {
         Group grp = parser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("MakeTextLowercase"));
         list.add(grp);
      }
      else if (change.equals("uc-cs"))
      {
         Group grp = parser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("MakeTextUppercase"));
         list.add(grp);
      }
      else if (change.equals("firstuc-cs"))
      {
         Group grp = parser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("makefirstuc"));
         list.add(grp);
      }
      else if (change.equals("lc"))
      {
         toLowerCase(list);
      }
      else if (change.equals("uc"))
      {
         toUpperCase(list);
      }
      else if (change.equals("firstuc"))
      {
         toSentenceCase(list);
      }
      else
      {
         throw new IllegalArgumentException("Invalid case change option: "
          +change);
      }

      bibList.add(new BibUserString(list));

      return bibList;
   }

   public int aliasLocations()
   {
      return aliasLocations;
   }

   public boolean hasAliases()
   {
      return aliases;
   }

   public void setAliases(boolean hasAliases)
   {
      aliases = hasAliases;
   }

   public String[] getLocationCounters()
   {
      return counters;
   }

   public String getGroupField()
   {
      return groupField;
   }

   public String getType(Bib2GlsEntry entry)
   {
      String entryType = entry.getFieldValue("type");

      if (entryType != null)
      {
         return entryType;
      }

      return type;
   }

   private void writeHyperGroupDef(Bib2GlsEntry entry, PrintWriter writer)
     throws IOException
   {
      String key = entry.getGroupId();

      if (key == null)
      {
         bib2gls.debug("No group ID for entry "+entry.getId());
         return;
      }

      GroupTitle groupTitle = groupTitleMap.get(key);

      if (groupTitle == null)
      {
         bib2gls.debug("No group found for "+key);
         return;
      }

      if (!groupTitle.isDone())
      {
         writer.format("\\bibglshypergroup{%s}{\\%s%s}%n",
            groupTitle.getType(),
            groupTitle.getCsLabelName(),
            groupTitle);

         groupTitle.mark();

         writer.println();
      }
   }

   public void putGroupTitle(GroupTitle grpTitle, Bib2GlsEntry entry)
   {
      if (groupTitleMap != null)
      {
         String key = grpTitle.getKey();

         entry.setGroupId(key);

         groupTitleMap.put(key, grpTitle);
      }
   }

   public GroupTitle getGroupTitle(Bib2GlsEntry entry, long id)
   {
      if (groupTitleMap != null)
      {
         return groupTitleMap.get(GroupTitle.getKey(getType(entry), id));
      }

      return null;
   }

   private File texFile;

   private Vector<TeXPath> sources;

   private String[] skipFields = null;

   private String[] externalPrefixes = null;

   private String type=null, category=null, counter=null;

   public static final int SORT_SUFFIX_NONE=0;
   public static final int SORT_SUFFIX_NON_UNIQUE=1;

   private int sortSuffixOption=SORT_SUFFIX_NON_UNIQUE;

   private String sortSuffixMarker = "";

   private String sort = "locale", sortField = "sort";

   private String sortRules=null, secondarySortRules=null, dualSortRules=null;

   private String dateSortFormat = null, dualDateSortFormat = null, secondaryDateSortFormat = null;

   private String dateSortLocale = "locale", dualDateSortLocale = "locale",
    secondaryDateSortLocale = "locale";

   private String dualType=null, dualCategory=null, dualCounter=null;

   private String dualSort = null, dualSortField = "sort";

   private String pluralSuffix="\\glspluralsuffix ";
   private String dualPluralSuffix="\\glspluralsuffix ";

   private String shortPluralSuffix=null;
   private String dualShortPluralSuffix=null;

   private Charset bibCharset = null;

   private boolean flatten = false;

   private boolean interpretPreamble = true;

   private boolean setWidest = false;

   private String secondaryType=null, secondarySort=null, secondaryField=null;

   private int minLocationRange = 3, locGap = 1;

   private String suffixF=null, suffixFF=null;

   private String preamble = null;
   private BibValueList preambleList = null;

   private HashMap<String,Pattern> fieldPatterns = null;

   private boolean notMatch=false;

   private boolean fieldPatternsAnd=true;

   private static final String PATTERN_FIELD_ID = "id";
   private static final String PATTERN_FIELD_ENTRY_TYPE = "entrytype";

   private Vector<Bib2GlsEntry> bibData;

   private Vector<Bib2GlsEntry> dualData;

   private Bib2Gls bib2gls;

   private int collatorStrength=Collator.PRIMARY;

   private int collatorDecomposition=Collator.CANONICAL_DECOMPOSITION;

   private int breakPoint = Bib2GlsEntryComparator.BREAK_WORD;
   private String breakPointMarker = "|";

   private int seeLocation=Bib2GlsEntry.POST_SEE;
   private int seeAlsoLocation=Bib2GlsEntry.POST_SEE;

   private String[] locationPrefix = null;

   private String[] locationSuffix = null;

   private boolean saveLocations = true;

   public static final int FLATTEN_LONELY_FALSE=0;
   public static final int FLATTEN_LONELY_PRE_SORT=1;
   public static final int FLATTEN_LONELY_POST_SORT=2;

   private int flattenLonely = FLATTEN_LONELY_FALSE;

   public static final int FLATTEN_LONELY_RULE_ONLY_UNRECORDED_PARENTS=0;
   public static final int FLATTEN_LONELY_RULE_NO_DISCARD=1;
   public static final int FLATTEN_LONELY_RULE_DISCARD_UNRECORDED=2;

   private int flattenLonelyRule 
     = FLATTEN_LONELY_RULE_ONLY_UNRECORDED_PARENTS;

   private boolean saveChildCount = false;

   private boolean defpagesname = false;

   public static final int ALIAS_LOC_OMIT=0;
   public static final int ALIAS_LOC_TRANS=1;
   public static final int ALIAS_LOC_KEEP=2;

   private int aliasLocations = ALIAS_LOC_TRANS;

   private boolean aliases = false;

   private String labelPrefix = null, dualPrefix="dual.";

   private String dualField = null;

   private HashMap<String,String> dualEntryMap, dualAbbrevMap,
      dualSymbolMap, dualEntryAbbrevMap;

   // HashMap doesn't retain order, so keep track of the first
   // mapping separately.

   private String dualEntryFirstMap, dualAbbrevFirstMap, dualSymbolFirstMap,
     dualEntryAbbrevFirstMap;

   private boolean backLinkDualEntry=false;
   private boolean backLinkDualAbbrev=false;
   private boolean backLinkDualSymbol=false;
   private boolean backLinkDualEntryAbbrev=false;

   private String shortCaseChange=null;
   private String dualShortCaseChange=null;
   private String nameCaseChange=null;
   private String descCaseChange=null;

   private String masterLinkPrefix=null;
   private Vector<TeXPath> masterGlsTeXPath = null;
   private TeXPath masterPdfPath = null;
   private String[] masterSelection = null;

   private String[] counters=null;

   private Random random=null;

   private HashMap<String,GroupTitle> groupTitleMap=null;

   private Vector<GlsRecord> supplementalRecords=null;
   private TeXPath supplementalPdfPath=null;
   private String[] supplementalSelection=null;
   private String supplementalCategory=null;

   private String groupField = null;

   public static final int SELECTION_RECORDED_AND_DEPS=0;
   public static final int SELECTION_RECORDED_AND_DEPS_AND_SEE=1;
   public static final int SELECTION_RECORDED_NO_DEPS=2;
   public static final int SELECTION_RECORDED_AND_PARENTS=3;
   public static final int SELECTION_ALL=4;

   private int selectionMode = SELECTION_RECORDED_AND_DEPS;

   private static final String[] SELECTION_OPTIONS = new String[]
    {"recorded and deps", "recorded and deps and see",
     "recorded no deps", "recorded and ancestors", "all"};

}

