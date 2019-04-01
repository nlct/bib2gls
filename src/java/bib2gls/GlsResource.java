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
import com.dickimawbooks.texparserlib.generic.Nbsp;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.MissingValue;
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

      sortSettings = new SortSettings("doc", bib2gls);
      dualSortSettings = new SortSettings(bib2gls);
      secondarySortSettings = new SortSettings(bib2gls);

      TeXPath texPath = new TeXPath(parser, 
        arg.toString(parser), "glstex", false);

      texFile = bib2gls.resolveFile(texPath.getFile());

      bib2gls.registerTeXFile(texFile);

      bib2gls.verboseMessage("message.initialising.resource",
        texFile.getName());

      String filename = texPath.getTeXPath(true);

      dependencies = new Vector<String>();
      KeyValList list = KeyValList.getList(parser, opts);

      String[] srcList = null;

      String master = null;
      String[] supplemental = null;

      String writeActionSetting = "define";

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
         {// Fetch supplemental locations from another document.
          // As from v1.7, the value may now be a list of
          // document base names. 
            supplemental = getStringArray(parser, list, opt);
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
         else if (opt.equals("entry-type-aliases"))
         {
            entryTypeAliases = getHashMap(parser, list, opt);

            if (entryTypeAliases == null)
            {
               if (bib2gls.getVerboseLevel() > 0)
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                     "message.clearing.entry.aliases"));
               }
            }
            else
            {
               if (bib2gls.getVerboseLevel() > 0)
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                     "message.setting.entry.aliases"));
               }

               for (Iterator<String> aliasIt 
                      = entryTypeAliases.keySet().iterator(); 
                    aliasIt.hasNext();)
               {
                  String key = aliasIt.next();
                  String val = entryTypeAliases.get(key);

                  if (key.matches(".*\\W.*"))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.entry.type",key,opt));
                  }

                  if (val.isEmpty())
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.missing.value", key));
                  }

                  if (val.matches(".*\\W.*"))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.entry.type",val,opt));
                  }

                  if (bib2gls.getVerboseLevel() > 0)
                  {
                     bib2gls.logMessage(String.format("@%s=>@%s.", key, val));
                  }
               }
            }
         }
         else if (opt.equals("unknown-entry-alias"))
         {
            unknownEntryMap = getOptional(parser, "", list, opt);

            if ("".equals(unknownEntryMap))
            {
               unknownEntryMap = null;
            }
         }
         else if (opt.equals("field-aliases"))
         {
            fieldAliases = getHashMap(parser, list, opt);

            if (fieldAliases != null)
            {
               Set<String> keys = fieldAliases.keySet();

               for (Iterator<String> it1 = keys.iterator(); it1.hasNext();)
               {
                  String key = it1.next();

                  Set<String> keys2 = fieldAliases.keySet();

                  for (Iterator<String> it2=keys2.iterator(); it2.hasNext();)
                  {
                     String key2 = it2.next();

                     String value = fieldAliases.get(key2);

                     if (value.equals(key))
                     {
                        if (key.equals(key2))
                        {
                           throw new IllegalArgumentException(
                             bib2gls.getMessage("error.field.alias.identity",
                               key));
                        }
                        else
                        {
                           throw new IllegalArgumentException(
                             bib2gls.getMessage("error.field.alias.trail",
                               key, fieldAliases.get(key), key2));
                        }
                     }
                  }
               }
            }
         }
         else if (opt.equals("replicate-fields"))
         {
            fieldCopies = getHashMapVector(parser, list, opt, true);
         }
         else if (opt.equals("replicate-override"))
         {
            replicateOverride = getBoolean(parser, list, opt);
         }
         else if (opt.equals("replicate-missing-field-action"))
         {
            String val = getChoice(parser, list, opt, "skip", "fallback", "empty");

            if (val.equals("skip"))
            {
               missingFieldReplicateAction = MISSING_FIELD_REPLICANT_SKIP;
            }
            else if (val.equals("fallback"))
            {
               missingFieldReplicateAction = MISSING_FIELD_REPLICANT_FALLBACK;
            }
            else // if (val.equals("empty"))
            {
               missingFieldReplicateAction = MISSING_FIELD_REPLICANT_EMPTY;
            }
         }
         else if (opt.equals("primary-dual-dependency"))
         {
            dualPrimaryDependency = getBoolean(parser, list, opt);
         }
         else if (opt.equals("strip-trailing-nopost"))
         {
            stripTrailingNoPost = getBoolean(parser, list, opt);
         }
         else if (opt.equals("copy-alias-to-see"))
         {
            copyAliasToSee = getBoolean(parser, list, opt);
         }
         else if (opt.equals("save-index-counter"))
         {
            indexCounter = getOptional(parser, "true", list, opt);

            if (indexCounter.equals("false"))
            {
               indexCounter = null;
            }
         }
         else if (opt.equals("post-description-dot"))
         {
            String val = getChoice(parser, list, opt, "none", "all", "check");

            if (val.equals("none"))
            {
               postDescDot = POST_DESC_DOT_NONE;
            }
            else if (val.equals("all"))
            {
               postDescDot = POST_DESC_DOT_ALL;
            }
            else //if (val.equals("check"))
            {
               postDescDot = POST_DESC_DOT_CHECK;
            }
         }
         else if (opt.equals("word-boundaries"))
         {
            String[] array = getStringArray(parser, list, opt);

            if (array == null || array.length == 0)
            {
               throw new IllegalArgumentException(
                bib2gls.getMessage("error.missing.value", opt));
            }

            wordBoundarySpace=false;
            wordBoundaryCsSpace=false;
            wordBoundaryNbsp=false;
            wordBoundaryDash=false;

            for (String element : array)
            {
               if (element.equals("white space"))
               {
                  wordBoundarySpace=true;
               }
               else if (element.equals("cs space"))
               {
                  wordBoundaryCsSpace=true;
               }
               else if (element.equals("dash"))
               {
                  wordBoundaryDash=true;
               }
               else if (element.equals("nbsp"))
               {
                  wordBoundaryNbsp=true;
               }
               else
               {
                  throw new IllegalArgumentException(
                     bib2gls.getMessage("error.invalid.choice.value", opt, 
                      element, "white space, cs space, dash, nbsp"));
               }
            }
         }
         else if (opt.equals("no-case-change-cs"))
         {
            noCaseChangeCs = getStringArray(parser, list, opt);
         }
         else if (opt.equals("name-case-change"))
         {
            nameCaseChange = getChoice(parser, list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("description-case-change"))
         {
            descCaseChange = getChoice(parser, list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("short-case-change"))
         {
            shortCaseChange = getChoice(parser, list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("dual-short-case-change"))
         {
            dualShortCaseChange = getChoice(parser, list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("short-plural-suffix"))
         {
            shortPluralSuffix = getOptional(parser, "", list, opt);

            if (shortPluralSuffix.equals("use-default"))
            {
               shortPluralSuffix = null;
            }
         }
         else if (opt.equals("long-case-change"))
         {
            longCaseChange = getChoice(parser, list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("dual-long-case-change"))
         {
            dualLongCaseChange = getChoice(parser, list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("field-case-change"))
         {
            TeXObject[] array = getTeXObjectArray(parser, list, opt, true);

            if (array == null)
            {
               fieldCaseChange = null;
            }
            else
            {
               fieldCaseChange = new HashMap<String,String>();

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

                  if (!bib2gls.isKnownField(field))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.field", field, opt));
                  }

                  if (split.size() > 2)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.value", 
                        field, array[i].toString(parser), opt));
                  }

                  String val = split.size() == 1 ? "" 
                               : split.get(1).toString(parser).trim();

                  String caseChangeOpt = null;

                  for (String caseOpt : CASE_OPTIONS)
                  {
                     if (caseOpt.equals(val))
                     {
                        caseChangeOpt = caseOpt;
                        break;
                     }
                  }

                  if (caseChangeOpt == null)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.value", 
                        field, val, opt));
                  }
                  else
                  {
                     fieldCaseChange.put(field, caseChangeOpt);
                  }
               }
            }
         }
         else if (opt.equals("encapsulate-fields"))
         {
            TeXObject[] array = getTeXObjectArray(parser, list, opt, true);

            if (array == null)
            {
               encapFields = null;
            }
            else
            {
               encapFields = new HashMap<String,String>();

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

                  if (!bib2gls.isKnownField(field))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.field", field, opt));
                  }

                  if (split.size() > 2)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.value", 
                        field, array[i].toString(parser), opt));
                  }

                  String val = split.size() == 1 ? "" 
                               : split.get(1).toString(parser).trim();

                  encapFields.put(field, val);
               }
            }
         }
         else if (opt.equals("encapsulate-fields*"))
         {
            TeXObject[] array = getTeXObjectArray(parser, list, opt, true);

            if (array == null)
            {
               encapFieldsIncLabel = null;
            }
            else
            {
               encapFieldsIncLabel = new HashMap<String,String>();

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

                  if (!bib2gls.isKnownField(field))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.field", field, opt));
                  }

                  if (split.size() > 2)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.value", 
                        field, array[i].toString(parser), opt));
                  }

                  String val = split.size() == 1 ? "" 
                               : split.get(1).toString(parser).trim();

                  encapFieldsIncLabel.put(field, val);
               }
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
         else if (opt.equals("action"))
         {
            writeActionSetting = getChoice(parser, list, opt,
              "define", "define or copy", "copy");

            if (writeActionSetting.equals("define"))
            {
               writeAction = WRITE_ACTION_DEFINE;
            }
            else if (writeActionSetting.equals("define or copy"))
            {
               writeAction = WRITE_ACTION_DEFINE_OR_COPY;
            }
            else
            {
               writeAction = WRITE_ACTION_COPY;
            }
         }
         else if (opt.equals("copy-action-group-field"))
         {
            if (bib2gls.useGroupField())
            {
               copyActionGroupField = getRequired(parser, list, opt);
            }
            else
            {
               bib2gls.warning(bib2gls.getMessage(
                 "warning.group.option.required", opt, "--group"));
               copyActionGroupField = null;
            }
         }
         else if (opt.equals("match-action"))
         {
            String val = getChoice(parser, list, opt, "filter", "add");

            if (val.equals("filter"))
            {
               matchAction = MATCH_ACTION_FILTER;
            }
            else
            {
               matchAction = MATCH_ACTION_ADD;
            }
         }
         else if (opt.equals("match-op"))
         {
            String val = getChoice(parser, list, opt, "and", "or");

            fieldPatternsAnd = val.equals("and");
         }
         else if (opt.equals("match"))
         {
            TeXObject[] array = getTeXObjectArray(parser, list, opt, true);
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
            TeXObject[] array = getTeXObjectArray(parser, list, opt, true);
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
         else if (opt.equals("limit"))
         {
            limit = getRequiredIntGe(parser, 0, list, opt);
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

            String method = split.get(0).toString(parser);

            try
            {
               secondarySortSettings.setMethod(method);
            }
            catch (IllegalArgumentException e)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.sort.value", method, opt),
                 e);
            }
         }
         else if (opt.equals("sort-label-list"))
         {
            TeXObject[] array = getTeXObjectArray(parser, list, opt, true);

            if (array == null || array.length == 0)
            {
               sortLabelList = null;
            }
            else
            {
               sortLabelList = new LabelListSortMethod[array.length];

               for (int j = 0; j < array.length; j++)
               {
                  if (!(array[j] instanceof TeXObjectList))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.value", opt, 
                       array[j].toString(parser)));
                  }

                  sortLabelList[j] = getLabelListSortMethod(parser, 
                    (TeXObjectList)array[j], opt); 
               }
            }
         }
         else if (opt.equals("ext-prefixes"))
         {
            externalPrefixes = getStringArray(parser, list, opt);
         }
         else if (opt.equals("labelify"))
         {
            labelifyFields = getFieldArray(parser, list, opt, true);
         }
         else if (opt.equals("labelify-list"))
         {
            labelifyListFields = getFieldArray(parser, list, opt, true);
         }
         else if (opt.equals("labelify-replace"))
         {
            labelifyReplaceMap = getSubstitutionList(parser, list, opt, true);
         }
         else if (opt.equals("dependency-fields"))
         {
            dependencyListFields = getFieldArray(parser, list, opt, true);
         }
         else if (opt.equals("sort-replace"))
         {
            sortSettings.setRegexList(
              getSubstitutionList(parser, list, opt, true));
         }
         else if (opt.equals("dual-sort-replace"))
         {
            dualSortSettings.setRegexList(
              getSubstitutionList(parser, list, opt, true));
         }
         else if (opt.equals("secondary-sort-replace"))
         {
            secondarySortSettings.setRegexList(
              getSubstitutionList(parser, list, opt, true));
         }
         else if (opt.equals("interpret-preamble"))
         {
            interpretPreamble = getBoolean(parser, list, opt);
         }
         else if (opt.equals("interpret-label-fields"))
         {
            interpretLabelFields = getBoolean(parser, list, opt);
         }
         else if (opt.equals("strip-missing-parents"))
         {
            stripMissingParents = getBoolean(parser, list, opt);

            if (stripMissingParents)
            {
               createMissingParents = false;
            }
         }
         else if (opt.equals("missing-parents"))
         {
            String val = getChoice(parser, list, opt, "strip", "create",
             "warn");

            if (val.equals("strip"))
            {
               stripMissingParents = true;
               createMissingParents = false;
            }
            else if (val.equals("create"))
            {
               stripMissingParents = false;
               createMissingParents = true;
            }
            else if (val.equals("warn"))
            {
               stripMissingParents = false;
               createMissingParents = false;
            }
         }
         else if (opt.equals("write-preamble"))
         {
            savePreamble = getBoolean(parser, list, opt);
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
         else if (opt.equals("save-loclist"))
         {
            saveLocList = getBoolean(parser, list, opt);
         }
         else if (opt.equals("save-primary-locations"))
         {
            String val = getChoice(parser, list, opt,
              "false", "remove", "retain", "start", "default format");

            if (val.equals("false"))
            {
               savePrimaryLocations = SAVE_PRIMARY_LOCATION_OFF;
            }
            else if (val.equals("remove"))
            {
               savePrimaryLocations = SAVE_PRIMARY_LOCATION_REMOVE;
            }
            else if (val.equals("retain"))
            {
               savePrimaryLocations = SAVE_PRIMARY_LOCATION_RETAIN;
            }
            else if (val.equals("start"))
            {
               savePrimaryLocations = SAVE_PRIMARY_LOCATION_START;
            }
            else if (val.equals("default format"))
            {
               savePrimaryLocations = SAVE_PRIMARY_LOCATION_DEFAULT_FORMAT;
            }
         }
         else if (opt.equals("primary-location-formats"))
         {
            primaryLocationFormats = getStringArray(parser, list, opt);

            if (savePrimaryLocations == SAVE_PRIMARY_LOCATION_OFF)
            {
               savePrimaryLocations = SAVE_PRIMARY_LOCATION_RETAIN;
            }
         }
         else if (opt.equals("combine-dual-locations"))
         {
            String val = getChoice(parser, list, opt,
              "false", "both", "dual", "primary");

            if (val.equals("false"))
            {
               combineDualLocations = COMBINE_DUAL_LOCATIONS_OFF;
            }
            else if (val.equals("both"))
            {
               combineDualLocations = COMBINE_DUAL_LOCATIONS_BOTH;
            }
            else if (val.equals("dual"))
            {
               combineDualLocations = COMBINE_DUAL_LOCATIONS_DUAL;
            }
            else if (val.equals("primary"))
            {
               combineDualLocations = COMBINE_DUAL_LOCATIONS_PRIMARY;
            }
         }
         else if (opt.equals("save-child-count"))
         {
            saveChildCount = getBoolean(parser, list, opt);
         }
         else if (opt.equals("save-original-entrytype"))
         {
            saveOriginalEntryType = getBoolean(parser, list, opt);
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
            bib2gls.warning(bib2gls.getMessage(
              "warning.deprecated.option", opt, "dual-abbrventry-map"));

            String[] keys = new String[1];

            dualAbbrevEntryMap = getDualMap(parser, list, opt, keys);

            dualAbbrevEntryFirstMap = keys[0];
         }
         else if (opt.equals("dual-abbrventry-map"))
         {
            String[] keys = new String[1];

            dualAbbrevEntryMap = getDualMap(parser, list, opt, keys);

            dualAbbrevEntryFirstMap = keys[0];
         }
         else if (opt.equals("dual-indexentry-map"))
         {
            String[] keys = new String[1];

            dualIndexEntryMap = getDualMap(parser, list, opt, keys);

            dualIndexEntryFirstMap = keys[0];
         }
         else if (opt.equals("dual-indexsymbol-map"))
         {
            String[] keys = new String[1];

            dualIndexSymbolMap = getDualMap(parser, list, opt, keys);

            dualIndexSymbolFirstMap = keys[0];
         }
         else if (opt.equals("dual-indexabbrv-map"))
         {
            String[] keys = new String[1];

            dualIndexAbbrevMap = getDualMap(parser, list, opt, keys);

            dualIndexAbbrevFirstMap = keys[0];
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
               backLinkDualAbbrevEntry = true;
               backLinkDualIndexEntry = true;
               backLinkDualIndexSymbol = true;
               backLinkDualIndexAbbrev = true;
            }
            else
            {
               backLinkDualEntry = false;
               backLinkDualAbbrev = false;
               backLinkDualSymbol = false;
               backLinkDualAbbrevEntry = false;
               backLinkDualIndexEntry = false;
               backLinkDualIndexSymbol = false;
               backLinkDualIndexAbbrev = false;
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
            bib2gls.warning(bib2gls.getMessage("warning.deprecated.option", opt,
              "dual-abbrventry-backlink"));
            backLinkDualAbbrevEntry = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-abbrventry-backlink"))
         {
            backLinkDualAbbrevEntry = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-indexentry-backlink"))
         {
            backLinkDualIndexEntry = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-indexsymbol-backlink"))
         {
            backLinkDualIndexSymbol = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-indexabbrv-backlink"))
         {
            backLinkDualIndexAbbrev = getBoolean(parser, list, opt);
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
         else if (opt.equals("trigger-type"))
         {
            triggerType = getRequired(parser, list, opt);
         }
         else if (opt.equals("progenitor-type"))
         {
            progenitorType = getRequired(parser, list, opt);
         }
         else if (opt.equals("progeny-type"))
         {
            progenyType = getRequired(parser, list, opt);
         }
         else if (opt.equals("adopted-parent-field"))
         {
            adoptedParentField = getRequired(parser, list, opt);
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
         else if (opt.equals("missing-parent-category"))
         {
            missingParentCategory = getRequired(parser, list, opt);

            if ("no value".equals(missingParentCategory))
            {
               missingParentCategory = null;
            }
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
         else if (opt.equals("tertiary-prefix"))
         {
            tertiaryPrefix = getOptional(parser, list, opt);
         }
         else if (opt.equals("tertiary-category"))
         {
            tertiaryCategory = getOptional(parser, list, opt);
         }
         else if (opt.equals("tertiary-type"))
         {
            tertiaryType = getOptional(parser, list, opt);
         }
         else if (opt.equals("cs-label-prefix"))
         {
            csLabelPrefix = getOptional(parser, list, opt);

            if (csLabelPrefix == null)
            {
               csLabelPrefix = "";
            }
         }
         else if (opt.equals("record-label-prefix"))
         {
            recordLabelPrefix = getOptional(parser, list, opt);
         }
         else if (opt.equals("duplicate-label-suffix"))
         {
            dupLabelSuffix = getOptional(parser, list, opt);
         }
         else if (opt.equals("save-original-id"))
         {
            saveOriginalId = getOptional(parser, list, opt);

            if (saveOriginalId == null || saveOriginalId.isEmpty())
            {
               saveOriginalId = "originalid";
            }
            else if (saveOriginalId.equals("false"))
            {
               saveOriginalId = null;
            }
         }
         else if (opt.equals("sort-suffix"))
         {
            String val = getRequired(parser, list, opt);

            if (val.equals("none"))
            {
               sortSettings.setSuffixOption(SortSettings.SORT_SUFFIX_NONE);
            }
            else if (val.equals("non-unique"))
            {
               sortSettings.setSuffixOption(SortSettings.SORT_SUFFIX_NON_UNIQUE);
            }
            else if (bib2gls.isKnownField(val)
                     || bib2gls.isKnownSpecialField(val))
            {
               sortSettings.setSuffixOption(SortSettings.SORT_SUFFIX_FIELD);
               sortSettings.setSuffixField(val);
            }
            else
            {
               throw new IllegalArgumentException(
                  bib2gls.getMessage("error.invalid.opt.value", opt, val));
            }

            dualSortSettings.setSuffixOption(sortSettings.getSuffixOption());
            dualSortSettings.setSuffixField(sortSettings.getSuffixField());

            secondarySortSettings.setSuffixOption(sortSettings.getSuffixOption());
            secondarySortSettings.setSuffixField(sortSettings.getSuffixField());
         }
         else if (opt.equals("dual-sort-suffix"))
         {
            String val = getRequired(parser, list, opt);

            if (val.equals("none"))
            {
               dualSortSettings.setSuffixOption(SortSettings.SORT_SUFFIX_NONE);
            }
            else if (val.equals("non-unique"))
            {
               dualSortSettings.setSuffixOption(
                 SortSettings.SORT_SUFFIX_NON_UNIQUE);
            }
            else if (bib2gls.isKnownField(val)
                     || bib2gls.isKnownSpecialField(val))
            {
               dualSortSettings.setSuffixOption(SortSettings.SORT_SUFFIX_FIELD);
               dualSortSettings.setSuffixField(val);
            }
            else
            {
               throw new IllegalArgumentException(
                  bib2gls.getMessage("error.invalid.opt.value", opt, val));
            }
         }
         else if (opt.equals("secondary-sort-suffix"))
         {
            String val = getRequired(parser, list, opt);

            if (val.equals("none"))
            {
               secondarySortSettings.setSuffixOption(
                 SortSettings.SORT_SUFFIX_NONE);
            }
            else if (val.equals("non-unique"))
            {
               secondarySortSettings.setSuffixOption(
                 SortSettings.SORT_SUFFIX_NON_UNIQUE);
            }
            else if (bib2gls.isKnownField(val)
                    || bib2gls.isKnownSpecialField(val))
            {
               secondarySortSettings.setSuffixOption(
                 SortSettings.SORT_SUFFIX_FIELD);
               secondarySortSettings.setSuffixField(val);
            }
            else
            {
               throw new IllegalArgumentException(
                  bib2gls.getMessage("error.invalid.opt.value", opt, val));
            }
         }
         else if (opt.equals("sort-suffix-marker"))
         {
            sortSettings.setSuffixMarker(
               replaceHex(getOptional(parser, "", list, opt)));
            dualSortSettings.setSuffixMarker(sortSettings.getSuffixMarker());
            secondarySortSettings.setSuffixMarker(sortSettings.getSuffixMarker());
         }
         else if (opt.equals("dual-sort-suffix-marker"))
         {
            dualSortSettings.setSuffixMarker(
               replaceHex(getOptional(parser, "", list, opt)));
         }
         else if (opt.equals("secondary-sort-suffix-marker"))
         {
            secondarySortSettings.setSuffixMarker(
               replaceHex(getOptional(parser, "", list, opt)));
         }
         else if (opt.equals("group-formation"))
         {
             String val = getChoice(parser, list, opt, "default", 
               "codepoint", "unicode category", "unicode script",
               "unicode category and script");

             if (val.equals("codepoint"))
             {
                sortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CODEPOINT);
             }
             else if (val.equals("unicode category"))
             {
                sortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CATEGORY);
             }
             else if (val.equals("unicode script"))
             {
                sortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_SCRIPT);
             }
             else if (val.equals("unicode category and script"))
             {
                sortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CATEGORY_SCRIPT);
             }
             else // if (val.equals("default"))
             {
                sortSettings.setGroupFormation(SortSettings.GROUP_DEFAULT);
             }
         }
         else if (opt.equals("secondary-group-formation"))
         {
             String val = getChoice(parser, list, opt, "default", 
               "codepoint", "unicode category", "unicode script",
               "unicode category and script");

             if (val.equals("codepoint"))
             {
                secondarySortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CODEPOINT);
             }
             else if (val.equals("unicode category"))
             {
                secondarySortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CATEGORY);
             }
             else if (val.equals("unicode script"))
             {
                secondarySortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_SCRIPT);
             }
             else if (val.equals("unicode category and script"))
             {
                secondarySortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CATEGORY_SCRIPT);
             }
             else // if (val.equals("default"))
             {
                secondarySortSettings.setGroupFormation(SortSettings.GROUP_DEFAULT);
             }
         }
         else if (opt.equals("dual-group-formation"))
         {
             String val = getChoice(parser, list, opt, "default", 
               "codepoint", "unicode category", "unicode script",
               "unicode category and script");

             if (val.equals("codepoint"))
             {
                dualSortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CODEPOINT);
             }
             else if (val.equals("unicode category"))
             {
                dualSortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CATEGORY);
             }
             else if (val.equals("unicode script"))
             {
                dualSortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_SCRIPT);
             }
             else if (val.equals("unicode category and script"))
             {
                dualSortSettings.setGroupFormation(
                   SortSettings.GROUP_UNICODE_CATEGORY_SCRIPT);
             }
             else // if (val.equals("default"))
             {
                dualSortSettings.setGroupFormation(SortSettings.GROUP_DEFAULT);
             }
         }
         else if (opt.equals("identical-sort-action"))
         {
            String val = getRequired(parser, list, opt);

            if (val.equals("none"))
            {
               sortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_NO_ACTION);
            }
            else if (val.equals("id"))
            {
               sortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_ID);
            }
            else if (val.equals("original id"))
            {
               sortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_ORIGINAL_ID);
            }
            else if (bib2gls.isKnownField(val)
                     || bib2gls.isKnownSpecialField(val))
            {
               sortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_FIELD);
               sortSettings.setIdenticalSortField(val);
            }
            else
            {
               throw new IllegalArgumentException(
                  bib2gls.getMessage("error.invalid.opt.value", opt, val));
            }

            dualSortSettings.setIdenticalSortAction(
               sortSettings.getIdenticalSortAction());
            dualSortSettings.setIdenticalSortField(
               sortSettings.getIdenticalSortField());

            secondarySortSettings.setIdenticalSortAction(
               sortSettings.getIdenticalSortAction());
            secondarySortSettings.setIdenticalSortField(
               sortSettings.getIdenticalSortField());
         }
         else if (opt.equals("dual-identical-sort-action"))
         {
            String val = getRequired(parser, list, opt);

            if (val.equals("none"))
            {
               dualSortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_NO_ACTION);
            }
            else if (val.equals("id"))
            {
               dualSortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_ID);
            }
            else if (val.equals("original id"))
            {
               dualSortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_ORIGINAL_ID);
            }
            else if (bib2gls.isKnownField(val)
                   || bib2gls.isKnownSpecialField(val))
            {
               dualSortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_FIELD);
               dualSortSettings.setIdenticalSortField(val);
            }
            else
            {
               throw new IllegalArgumentException(
                  bib2gls.getMessage("error.invalid.opt.value", opt, val));
            }
         }
         else if (opt.equals("secondary-identical-sort-action"))
         {
            String val = getRequired(parser, list, opt);

            if (val.equals("none"))
            {
               secondarySortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_NO_ACTION);
            }
            else if (val.equals("id"))
            {
               secondarySortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_ID);
            }
            else if (val.equals("original id"))
            {
               secondarySortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_ORIGINAL_ID);
            }
            else if (bib2gls.isKnownField(val)
                  || bib2gls.isKnownSpecialField(val))
            {
               secondarySortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_FIELD);
               secondarySortSettings.setIdenticalSortField(val);
            }
            else
            {
               throw new IllegalArgumentException(
                  bib2gls.getMessage("error.invalid.opt.value", opt, val));
            }
         }
         else if (opt.equals("sort"))
         {
            String method = getOptional(parser, "doc", list, opt);

            try
            {
               sortSettings.setMethod(method);
            }
            catch (IllegalArgumentException e)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.sort.value", method, opt),
                 e);
            }
         }
         else if (opt.equals("sort-rule"))
         {
            sortSettings.setCollationRule(
              replaceHexAndSpecial(getRequired(parser, list, opt)));
         }
         else if (opt.equals("dual-sort-rule"))
         {
            dualSortSettings.setCollationRule(
              replaceHexAndSpecial(getRequired(parser, list, opt)));
         }
         else if (opt.equals("secondary-sort-rule"))
         {
            secondarySortSettings.setCollationRule(
              replaceHexAndSpecial(getRequired(parser, list, opt)));
         }
         else if (opt.equals("sort-number-pad"))
         {
            sortSettings.setNumberPad(
               getRequiredInt(parser, list, opt));
            dualSortSettings.setNumberPad(sortSettings.getNumberPad());
            secondarySortSettings.setNumberPad(sortSettings.getNumberPad());
         }
         else if (opt.equals("dual-sort-number-pad"))
         {
            dualSortSettings.setNumberPad(
               getRequiredInt(parser, list, opt));
         }
         else if (opt.equals("secondary-sort-number-pad"))
         {
            secondarySortSettings.setNumberPad(
               getRequiredInt(parser, list, opt));
         }
         else if (opt.equals("sort-pad-plus"))
         {
            sortSettings.setPadPlus(
               replaceHex(getOptional(parser, "", list, opt)));
            dualSortSettings.setPadPlus(sortSettings.getPadPlus());
            secondarySortSettings.setPadPlus(sortSettings.getPadPlus());
         }
         else if (opt.equals("dual-sort-pad-plus"))
         {
            dualSortSettings.setPadPlus(
               replaceHex(getOptional(parser, "", list, opt)));
         }
         else if (opt.equals("secondary-sort-pad-plus"))
         {
            secondarySortSettings.setPadPlus(
               replaceHex(getOptional(parser, "", list, opt)));
         }
         else if (opt.equals("sort-pad-minus"))
         {
            sortSettings.setPadMinus(
               replaceHex(getOptional(parser, "", list, opt)));
            dualSortSettings.setPadMinus(sortSettings.getPadMinus());
            secondarySortSettings.setPadMinus(sortSettings.getPadMinus());
         }
         else if (opt.equals("dual-sort-pad-minus"))
         {
            dualSortSettings.setPadMinus(
               replaceHex(getOptional(parser, "", list, opt)));
         }
         else if (opt.equals("secondary-sort-pad-minus"))
         {
            secondarySortSettings.setPadMinus(
               replaceHex(getOptional(parser, "", list, opt)));
         }
         else if (opt.equals("numeric-locale"))
         {
            sortSettings.setNumberLocale(
              getRequired(parser, list, opt));
         }
         else if (opt.equals("dual-numeric-locale"))
         {
            dualSortSettings.setNumberLocale(
              getRequired(parser, list, opt));
         }
         else if (opt.equals("secondary-numeric-locale"))
         {
            secondarySortSettings.setNumberLocale(
              getRequired(parser, list, opt));
         }
         else if (opt.equals("numeric-sort-pattern"))
         {
            sortSettings.setNumberFormat(
              replaceHexAndSpecial(getRequired(parser, list, opt)));
         }
         else if (opt.equals("dual-numeric-sort-pattern"))
         {
            dualSortSettings.setNumberFormat(
              replaceHexAndSpecial(getRequired(parser, list, opt)));
         }
         else if (opt.equals("secondary-numeric-sort-pattern"))
         {
            secondarySortSettings.setNumberFormat(
              replaceHexAndSpecial(getRequired(parser, list, opt)));
         }
         else if (opt.equals("trim-sort"))
         {
            sortSettings.setTrim(getBoolean(parser, list, opt));
            dualSortSettings.setTrim(sortSettings.isTrimOn());
            secondarySortSettings.setTrim(sortSettings.isTrimOn());
         }
         else if (opt.equals("dual-trim-sort"))
         {
            dualSortSettings.setTrim(getBoolean(parser, list, opt));
         }
         else if (opt.equals("secondary-trim-sort"))
         {
            secondarySortSettings.setTrim(getBoolean(parser, list, opt));
         }
         else if (opt.equals("letter-number-rule"))
         {
            sortSettings.setLetterNumberRule(
              getLetterNumberRule(parser, list, opt));

            dualSortSettings.setLetterNumberRule(
              sortSettings.getLetterNumberRule());
            secondarySortSettings.setLetterNumberRule(
              sortSettings.getLetterNumberRule());
         }
         else if (opt.equals("dual-letter-number-rule"))
         {
            dualSortSettings.setLetterNumberRule(
              getLetterNumberRule(parser, list, opt));
         }
         else if (opt.equals("secondary-letter-number-rule"))
         {
            secondarySortSettings.setLetterNumberRule(
              getLetterNumberRule(parser, list, opt));
         }
         else if (opt.equals("letter-number-punc-rule"))
         {
            sortSettings.setLetterNumberPuncRule(
              getLetterNumberPuncRule(parser, list, opt));

            dualSortSettings.setLetterNumberPuncRule(
              sortSettings.getLetterNumberPuncRule());
            secondarySortSettings.setLetterNumberPuncRule(
              sortSettings.getLetterNumberPuncRule());
         }
         else if (opt.equals("dual-letter-number-punc-rule"))
         {
            dualSortSettings.setLetterNumberPuncRule(
              getLetterNumberPuncRule(parser, list, opt));
         }
         else if (opt.equals("secondary-letter-number-punc-rule"))
         {
            secondarySortSettings.setLetterNumberPuncRule(
              getLetterNumberPuncRule(parser, list, opt));
         }
         else if (opt.equals("date-sort-format"))
         {
            sortSettings.setDateFormat(replaceHexAndSpecial(
               getRequired(parser, list, opt)));
         }
         else if (opt.equals("dual-date-sort-format"))
         {
            dualSortSettings.setDateFormat(replaceHexAndSpecial(
               getRequired(parser, list, opt)));
         }
         else if (opt.equals("secondary-date-sort-format"))
         {
            secondarySortSettings.setDateFormat(replaceHexAndSpecial(
               getRequired(parser, list, opt)));
         }
         else if (opt.equals("date-sort-locale"))
         {
            sortSettings.setDateLocale(getRequired(parser, list, opt));
         }
         else if (opt.equals("dual-date-sort-locale"))
         {
            dualSortSettings.setDateLocale(getRequired(parser, list, opt));
         }
         else if (opt.equals("secondary-date-sort-locale"))
         {
            secondarySortSettings.setDateLocale(getRequired(parser, list, opt));
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

            sortSettings.setMethod("random");

            flatten = true;
         }
         else if (opt.equals("dual-sort"))
         {
            String method = getOptional(parser, "doc", list, opt);

            try
            {
               dualSortSettings.setMethod(method);
            }
            catch (IllegalArgumentException e)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.sort.value", method, opt),
                 e);
            }
         }
         else if (opt.equals("sort-field"))
         {
            String field = getRequired(parser, list, opt);

            if (!field.equals("id") && !bib2gls.isKnownField(field)
                && !bib2gls.isKnownSpecialField(field))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, field));
            }

            sortSettings.setSortField(field);
         }
         else if (opt.equals("dual-sort-field"))
         {
            String field = getRequired(parser, list, opt);

            if (!field.equals("id") && !bib2gls.isKnownField(field)
                && !bib2gls.isKnownSpecialField(field))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", 
                    opt, field));
            }

            dualSortSettings.setSortField(field);
         }
         else if (opt.equals("missing-sort-fallback"))
         {
            sortSettings.setMissingFieldFallback(
              getOptional(parser, list, opt));
         }
         else if (opt.equals("dual-missing-sort-fallback"))
         {
            dualSortSettings.setMissingFieldFallback(
              getOptional(parser, list, opt));
         }
         else if (opt.equals("secondary-missing-sort-fallback"))
         {
            secondarySortSettings.setMissingFieldFallback(
              getOptional(parser, list, opt));
         }
         else if (opt.equals("entry-sort-fallback"))
         {
            entryDefaultSortField = getRequired(parser, list, opt);

            // Don't allow unknown fields (to guard against typos).
            // The fallback field can't be "sort" as it will cause
            // infinite recursion.

            if ((!bib2gls.isKnownField(entryDefaultSortField)
             && !bib2gls.isKnownSpecialField(entryDefaultSortField))
             || entryDefaultSortField.equals("sort"))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   entryDefaultSortField));
            }
         }
         else if (opt.equals("abbreviation-sort-fallback"))
         {
            abbrevDefaultSortField = getRequired(parser, list, opt);

            if ((!bib2gls.isKnownField(abbrevDefaultSortField)
             && !bib2gls.isKnownSpecialField(abbrevDefaultSortField))
             || abbrevDefaultSortField.equals("sort"))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   abbrevDefaultSortField));
            }
         }
         else if (opt.equals("symbol-sort-fallback"))
         {
            symbolDefaultSortField = getRequired(parser, list, opt);

            if ((!bib2gls.isKnownField(symbolDefaultSortField)
             && !bib2gls.isKnownSpecialField(symbolDefaultSortField))
             || symbolDefaultSortField.equals("sort"))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   symbolDefaultSortField));
            }
         }
         else if (opt.equals("bibtexentry-sort-fallback"))
         {
            bibTeXEntryDefaultSortField = getRequired(parser, list, opt);

            if ((!bib2gls.isKnownField(bibTeXEntryDefaultSortField)
             && !bib2gls.isKnownSpecialField(bibTeXEntryDefaultSortField))
             || bibTeXEntryDefaultSortField.equals("sort"))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   bibTeXEntryDefaultSortField));
            }
         }
         else if (opt.equals("abbreviation-name-fallback"))
         {
            abbrevDefaultNameField = getRequired(parser, list, opt);

            if ((!bib2gls.isKnownField(abbrevDefaultNameField)
             && !bib2gls.isKnownSpecialField(abbrevDefaultNameField))
             || abbrevDefaultNameField.equals("name"))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   abbrevDefaultNameField));
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
         else if (opt.equals("compact-ranges"))
         {
            String val = getOptional(parser, "true", list, opt);

            if (val.equals("false"))
            {
               compactRanges = 0;
            }
            else if (val.equals("true"))
            {
               compactRanges = 3;
            }
            else
            {
               try
               {
                  compactRanges = Integer.parseInt(val);
               }
               catch (NumberFormatException e)
               {
                  throw new IllegalArgumentException(
                    bib2gls.getMessage("error.invalid.opt.intorbool.value",
                      opt, val), e);
               }
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
         else if (opt.equals("alias"))
         {
            String val = getChoice(parser, list, opt, "omit", "before", "after");

            if (val.equals("omit"))
            {
               aliasLocation = Bib2GlsEntry.NO_SEE;
            }
            else if (val.equals("before"))
            {
               aliasLocation = Bib2GlsEntry.PRE_SEE;
            }
            else if (val.equals("after"))
            {
               aliasLocation = Bib2GlsEntry.POST_SEE;
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
               else if (values[0].equals("comma"))
               {
                  locationPrefix = new String[]{", "};
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
         else if (opt.equals("check-end-punctuation"))
         {
            checkEndPunc = getStringArray(parser, list, opt);
         }
         else if (opt.equals("date-time-fields"))
         {
            dateTimeList = getStringArray(parser, list, opt);
         }
         else if (opt.equals("date-fields"))
         {
            dateList = getStringArray(parser, list, opt);
         }
         else if (opt.equals("time-fields"))
         {
            timeList = getStringArray(parser, list, opt);
         }
         else if (opt.equals("date-time-field-format"))
         {
            dateTimeListFormat = replaceHexAndSpecial(getRequired(parser, list, opt));
            dualDateTimeListFormat = dateTimeListFormat;
         }
         else if (opt.equals("dual-date-time-field-format"))
         {
            dualDateTimeListFormat = replaceHexAndSpecial(getRequired(parser, list, opt));
         }
         else if (opt.equals("date-field-format"))
         {
            dateListFormat = replaceHexAndSpecial(getRequired(parser, list, opt));
            dualDateListFormat = dateListFormat;
         }
         else if (opt.equals("dual-date-field-format"))
         {
            dualDateListFormat = replaceHexAndSpecial(getRequired(parser, list, opt));
         }
         else if (opt.equals("time-field-format"))
         {
            timeListFormat = replaceHexAndSpecial(getRequired(parser, list, opt));
            dualTimeListFormat = timeListFormat;
         }
         else if (opt.equals("dual-time-field-format"))
         {
            dualTimeListFormat = replaceHexAndSpecial(getRequired(parser, list, opt));
         }
         else if (opt.equals("date-time-field-locale"))
         {
            dateTimeListLocale = getLocale(parser, list, opt);
            dualDateTimeListLocale = dateTimeListLocale;
         }
         else if (opt.equals("dual-date-time-field-locale"))
         {
            dualDateTimeListLocale = getLocale(parser, list, opt);
         }
         else if (opt.equals("date-field-locale"))
         {
            dateListLocale = getLocale(parser, list, opt);
            dualDateListLocale = dateListLocale;
         }
         else if (opt.equals("dual-date-field-locale"))
         {
            dualDateListLocale = getLocale(parser, list, opt);
         }
         else if (opt.equals("time-field-locale"))
         {
            timeListLocale = getLocale(parser, list, opt);
            dualTimeListLocale = timeListLocale;
         }
         else if (opt.equals("dual-time-field-locale"))
         {
            dualTimeListLocale = getLocale(parser, list, opt);
         }
         else if (opt.equals("interpret-fields"))
         {
            interpretFields = getFieldArray(parser, list, opt, true);

            if (interpretFields != null && !bib2gls.useInterpreter())
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.option.requires.interpreter", opt));
            }
         }
         else if (opt.equals("bibtex-contributor-fields"))
         {
            bibtexAuthorList = getFieldArray(parser, list, opt, true);
         }
         else if (opt.equals("contributor-order"))
         {
            String val = getChoice(parser, list, opt, 
              "surname", "von", "forenames");

            if (val.equals("surname"))
            {
               contributorOrder = CONTRIBUTOR_ORDER_SURNAME;
            }
            else if (val.equals("von"))
            {
               contributorOrder = CONTRIBUTOR_ORDER_VON;
            }
            else if (val.equals("forenames"))
            {
               contributorOrder = CONTRIBUTOR_ORDER_FORENAMES;
            }
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
            sortSettings.setBreakPoint(getBreakAt(parser, list, opt));
            dualSortSettings.setBreakPoint(sortSettings.getBreakPoint());
            secondarySortSettings.setBreakPoint(sortSettings.getBreakPoint());
         }
         else if (opt.equals("dual-break-at"))
         {
            dualSortSettings.setBreakPoint(getBreakAt(parser, list, opt));
         }
         else if (opt.equals("secondary-break-at"))
         {
            secondarySortSettings.setBreakPoint(getBreakAt(parser, list, opt));
         }
         else if (opt.equals("break-marker"))
         {
            sortSettings.setBreakPointMarker(getOptional(parser, "", list, opt));
            dualSortSettings.setBreakPointMarker(sortSettings.getBreakPointMarker());
            secondarySortSettings.setBreakPointMarker(sortSettings.getBreakPointMarker());
         }
         else if (opt.equals("dual-break-marker"))
         {
            dualSortSettings.setBreakPointMarker(getOptional(parser, "", list, opt));
         }
         else if (opt.equals("secondary-break-marker"))
         {
            secondarySortSettings.setBreakPointMarker(getOptional(parser, "", list, opt));
         }
         else if (opt.equals("strength"))
         { // collator strength

            String val = getChoice(parser, list, opt, "primary", "secondary",
               "tertiary", "identical");

            if (val.equals("primary"))
            {
               sortSettings.setCollatorStrength(Collator.PRIMARY);
            }
            else if (val.equals("secondary"))
            {
               sortSettings.setCollatorStrength(Collator.SECONDARY);
            }
            else if (val.equals("tertiary"))
            {
               sortSettings.setCollatorStrength(Collator.TERTIARY);
            }
            else if (val.equals("identical"))
            {
               sortSettings.setCollatorStrength(Collator.IDENTICAL);
            }

            dualSortSettings.setCollatorStrength(sortSettings.getCollatorStrength());
            secondarySortSettings.setCollatorStrength(sortSettings.getCollatorStrength());
         }
         else if (opt.equals("dual-strength"))
         { // dual collator strength

            String val = getChoice(parser, list, opt, "primary", "secondary",
               "tertiary", "identical");

            if (val.equals("primary"))
            {
               dualSortSettings.setCollatorStrength(Collator.PRIMARY);
            }
            else if (val.equals("secondary"))
            {
               dualSortSettings.setCollatorStrength(Collator.SECONDARY);
            }
            else if (val.equals("tertiary"))
            {
               dualSortSettings.setCollatorStrength(Collator.TERTIARY);
            }
            else if (val.equals("identical"))
            {
               dualSortSettings.setCollatorStrength(Collator.IDENTICAL);
            }
         }
         else if (opt.equals("secondary-strength"))
         { // secondary collator strength

            String val = getChoice(parser, list, opt, "primary", "secondary",
               "tertiary", "identical");

            if (val.equals("primary"))
            {
               secondarySortSettings.setCollatorStrength(Collator.PRIMARY);
            }
            else if (val.equals("secondary"))
            {
               secondarySortSettings.setCollatorStrength(Collator.SECONDARY);
            }
            else if (val.equals("tertiary"))
            {
               secondarySortSettings.setCollatorStrength(Collator.TERTIARY);
            }
            else if (val.equals("identical"))
            {
               secondarySortSettings.setCollatorStrength(Collator.IDENTICAL);
            }
         }
         else if (opt.equals("decomposition"))
         { // collator decomposition

            String val = getChoice(parser, list, opt, "none", "canonical",
              "full");

            if (val.equals("none"))
            {
               sortSettings.setCollatorDecomposition(Collator.NO_DECOMPOSITION);
            }
            else if (val.equals("canonical"))
            {
               sortSettings.setCollatorDecomposition(Collator.CANONICAL_DECOMPOSITION);
            }
            else if (val.equals("full"))
            {
               sortSettings.setCollatorDecomposition(Collator.FULL_DECOMPOSITION);
            }

            dualSortSettings.setCollatorDecomposition(
              sortSettings.getCollatorDecomposition());
            secondarySortSettings.setCollatorDecomposition(
              sortSettings.getCollatorDecomposition());
         }
         else if (opt.equals("dual-decomposition"))
         { // dual collator decomposition

            String val = getChoice(parser, list, opt, "none", "canonical",
              "full");

            if (val.equals("none"))
            {
               dualSortSettings.setCollatorDecomposition(Collator.NO_DECOMPOSITION);
            }
            else if (val.equals("canonical"))
            {
               dualSortSettings.setCollatorDecomposition(Collator.CANONICAL_DECOMPOSITION);
            }
            else if (val.equals("full"))
            {
               dualSortSettings.setCollatorDecomposition(Collator.FULL_DECOMPOSITION);
            }
         }
         else if (opt.equals("secondary-decomposition"))
         { // secondary collator decomposition

            String val = getChoice(parser, list, opt, "none", "canonical",
              "full");

            if (val.equals("none"))
            {
               secondarySortSettings.setCollatorDecomposition(Collator.NO_DECOMPOSITION);
            }
            else if (val.equals("canonical"))
            {
               secondarySortSettings.setCollatorDecomposition(Collator.CANONICAL_DECOMPOSITION);
            }
            else if (val.equals("full"))
            {
               secondarySortSettings.setCollatorDecomposition(Collator.FULL_DECOMPOSITION);
            }
         }
         else
         {
            throw new IllegalArgumentException(
             bib2gls.getMessage("error.syntax.unknown_option", opt));
         }
      }

      if (csLabelPrefix == null)
      {
         csLabelPrefix = labelPrefix;
      }

      if (supplemental != null)
      {
         if (supplemental.length > 1 &&
               !bib2gls.isMultipleSupplementarySupported())
         {
            bib2gls.warningMessage("warning.multi_supp_unsupported",
             supplemental[0], 
             bib2gls.getGlossariesExtraVersion(),
             String.format("%s %4d/%02d/%02d", 
               bib2gls.MIN_MULTI_SUPP_VERSION,
               bib2gls.MIN_MULTI_SUPP_YEAR,
               bib2gls.MIN_MULTI_SUPP_MONTH,
               bib2gls.MIN_MULTI_SUPP_DAY));

            parseSupplemental(parser, supplemental[0]);
         }
         else
         {
            for (String suppRef : supplemental)
            {
               parseSupplemental(parser, suppRef);
            }
         }
      }

      if (master != null)
      {
         parseMaster(parser, master);
      }

      if (!dualPrimaryDependency && !dualSortSettings.requiresSorting())
      {
         bib2gls.warningMessage("warning.option.clash", 
          "dual-sort="+dualSortSettings.getMethodName(), 
          "primary-dual-dependency=false");
         dualPrimaryDependency=true;
      }

      String docLocale = bib2gls.getDocDefaultLocale();

      if (docLocale == null)
      {
         Locale locale = bib2gls.getDefaultLocale();

         if (locale == null)
         {
            locale = Locale.getDefault();
         }

         docLocale = locale.toLanguageTag();
      }

      sortSettings.setDocLocale(docLocale);
      dualSortSettings.setDocLocale(docLocale);

      if (sortSettings.isCustom() && !sortSettings.hasCustomRule())
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", "sort=custom", "sort-rules"));
      }

      if (sortSettings.isCustomNumeric() 
            && !sortSettings.hasCustomNumericRule())
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", 
           "sort="+sortSettings.getMethod(), "sort-number-pattern"));
      }

      if (dualSortSettings.isCustom() && !dualSortSettings.hasCustomRule())
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", "dual-sort=custom", 
          "dual-sort-rules"));
      }

      if (dualSortSettings.isCustomNumeric() 
            && !dualSortSettings.hasCustomNumericRule())
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", 
           "dual-sort="+dualSortSettings.getMethod(),
           "dual-sort-number-pattern"));
      }

      if (secondarySortSettings.isCustom()
           && !secondarySortSettings.hasCustomRule())
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", 
          secondaryField == null ?
          String.format("secondary={%s:%s}",
            secondarySortSettings.getMethod(), secondaryType) :
          String.format("secondary={%s:%s:%s}", 
            secondarySortSettings.getMethod(),
            secondarySortSettings.getSortField(), secondaryType), 
           "secondary-sort-rules"));
      }

      if (secondarySortSettings.isCustomNumeric()
           && !secondarySortSettings.hasCustomNumericRule())
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "warning.option.pair.required", 
          secondaryField == null ?
          String.format("secondary={%s:%s}",
            secondarySortSettings.getMethod(), secondaryType) :
          String.format("secondary={%s:%s:%s}", 
            secondarySortSettings.getMethod(),
            secondarySortSettings.getSortField(), secondaryType), 
           "secondary-sort-number-pattern"));
      }

      if (savePrimaryLocations != SAVE_PRIMARY_LOCATION_OFF
           && primaryLocationFormats == null)
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
           "warning.option.pair.required",
           "save-primary-locations",
           "primary-location-formats"));
      }

      if (limit > 0 && master != null)
      {
         throw new IllegalArgumentException(bib2gls.getMessage(
          "error.option.clash", 
          "limit="+limit, "master"));
      }

      if (writeAction != WRITE_ACTION_DEFINE)
      {
         if (type == null)
         {
            throw new IllegalArgumentException(bib2gls.getMessage(
             "warning.option.pair.required", 
             "action="+writeActionSetting, "type"));
         }

         if (secondaryType != null)
         {
            throw new IllegalArgumentException(bib2gls.getMessage(
             "error.option.clash", 
             "action="+writeActionSetting, "secondary"));
         }

         if (master != null)
         {
            throw new IllegalArgumentException(bib2gls.getMessage(
             "error.option.clash", 
             "action="+writeActionSetting, "master"));
         }
      }

      if (selectionMode == SELECTION_ALL && sortSettings.isOrderOfRecords())
      {
         bib2gls.warning(
            bib2gls.getMessage("warning.option.clash", "selection=all",
            "sort="+sortSettings.getMethod()));

         sortSettings.setMethod(null);
      }
      else if (matchAction == MATCH_ACTION_ADD
                && sortSettings.isOrderOfRecords())
      {
         bib2gls.warning(
            bib2gls.getMessage("warning.option.clash", 
            "match-action=add",
            "sort="+sortSettings.getMethod()));

         sortSettings.setMethod(null);
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

      if ((labelPrefix == null && tertiaryPrefix == null)
        ||(labelPrefix != null && tertiaryPrefix != null
           && labelPrefix.equals(tertiaryPrefix)))
      {
         throw new IllegalArgumentException(
            bib2gls.getMessage("error.option.clash",
            String.format("label-prefix={%s}", 
               labelPrefix == null ? "" : labelPrefix),
            String.format("tertiary-prefix={%s}", 
               tertiaryPrefix == null ? "" : tertiaryPrefix)));
      }

      if ((dualPrefix == null && tertiaryPrefix == null)
        ||(dualPrefix != null && tertiaryPrefix != null
           && dualPrefix.equals(tertiaryPrefix)))
      {
         throw new IllegalArgumentException(
            bib2gls.getMessage("error.option.clash",
            String.format("dual-prefix={%s}", 
               dualPrefix == null ? "" : dualPrefix),
            String.format("tertiary-prefix={%s}", 
               tertiaryPrefix == null ? "" : tertiaryPrefix)));
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

      if (dualAbbrevEntryMap == null)
      {
         dualAbbrevEntryMap = new HashMap<String,String>();
         dualAbbrevEntryMap.put("long", "name");
         dualAbbrevEntryMap.put("longplural", "plural");
         dualAbbrevEntryMap.put("short", "text");

         dualAbbrevEntryFirstMap = "long";
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

      if (dualIndexEntryMap == null)
      {
         dualIndexEntryMap = new HashMap<String,String>();
         dualIndexEntryMap.put("name", "name");

         dualIndexEntryFirstMap = "name";
      }

      if (dualIndexSymbolMap == null)
      {
         dualIndexSymbolMap = new HashMap<String,String>();
         dualIndexSymbolMap.put("symbol", "name");
         dualIndexSymbolMap.put("name", "symbol");
         dualIndexSymbolMap.put("symbolplural", "plural");
         dualIndexSymbolMap.put("plural", "symbolplural");

         dualIndexSymbolFirstMap = "symbol";
      }

      if (dualIndexAbbrevMap == null)
      {
         dualIndexAbbrevMap = new HashMap<String,String>();
         dualIndexAbbrevMap.put("name", "name");

         dualIndexAbbrevFirstMap = "name";
      }

      if (dualSortSettings.getMethod() == null)
      {
         dualSortSettings.setMethod("combine");
      }
      else if ("none".equals(dualSortSettings.getMethod()))
      {
         dualSortSettings.setMethod(null);
      }

      if (dualSortSettings.getSortField() == null)
      {
         dualSortSettings.setSortField(sortSettings.getSortField());
      }

      if (bib2gls.getVerboseLevel() > 0)
      {
         bib2gls.logMessage();
         bib2gls.verboseMessage("message.selection.mode", 
          SELECTION_OPTIONS[selectionMode]);

         if (skipFields != null)
         {
            bib2gls.verboseMessage("message.ignore.fields"); 

            for (int i = 0; i < skipFields.length; i++)
            {
               bib2gls.verbose(skipFields[i]);
            }

            bib2gls.logMessage();
         }

         sortSettings.verboseMessages(bib2gls);
         dualSortSettings.verboseMessages(bib2gls, "dual.sort");

         bib2gls.verboseMessage("message.label.prefix", 
            labelPrefix == null ? "" : labelPrefix);

         bib2gls.verboseMessage("message.dual.label.prefix", 
            dualPrefix == null ? "" : dualPrefix);

         bib2gls.verboseMessage("message.tertiary.label.prefix", 
            tertiaryPrefix == null ? "" : tertiaryPrefix);

         bib2gls.logMessage();
         bib2gls.verboseMessage("message.dual.entry.mappings"); 

         for (Iterator<String> it = dualEntryMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualEntryMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verboseMessage("message.dual.symbol.mappings"); 

         for (Iterator<String> it = dualSymbolMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualSymbolMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verboseMessage("message.dual.abbreviation.mappings"); 

         for (Iterator<String> it = dualAbbrevMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualAbbrevMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verboseMessage(
            "message.dual.abbreviationentry.mappings"); 

         for (Iterator<String> it = dualAbbrevEntryMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualAbbrevEntryMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verboseMessage("message.dual.indexentry.mappings"); 

         for (Iterator<String> it = dualIndexEntryMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualIndexEntryMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verboseMessage("message.dual.indexsymbol.mappings"); 

         for (Iterator<String> it = dualIndexSymbolMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualIndexSymbolMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verboseMessage("message.dual.indexabbrv.mappings"); 

         for (Iterator<String> it = dualIndexAbbrevMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualIndexAbbrevMap.get(key)));
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

      if (longCaseChange != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "long-case-change"));
      }

      if (dualLongCaseChange != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-long-case-change"));
      }

      if (fieldCaseChange != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "field-case-change"));
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

      if (interpretFields != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "interpret-fields"));
      }

      if (bibtexAuthorList != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "bibtex-contributor-fields"));
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

      if (!"tertiary.".equals(tertiaryPrefix))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "tertiary-prefix"));
      }

      if (dualSortSettings.getMethod() != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-sort"));
      }

      if (!dualSortSettings.getSortField().equals("sort"))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "dual-sort"));
      }

      if (!"locale".equals(sortSettings.getMethod()))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "sort"));
      }

      if (!sortSettings.getSortField().equals("sort"))
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

      if (aliasLocation != Bib2GlsEntry.POST_SEE)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "alias"));
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

      if (sortSettings.getCollatorStrength() != Collator.PRIMARY)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "strength"));
      }

      if (sortSettings.getCollatorDecomposition() != Collator.CANONICAL_DECOMPOSITION)
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

      AuxParser auxParser = new AuxParser(bib2gls, bib2gls.getTeXCharset())
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

      AuxParser auxParser = new AuxParser(bib2gls, bib2gls.getTeXCharset())
      {
         protected void addPredefined()
         {
            super.addPredefined();

            addAuxCommand("glsxtr@record", 5);
            addAuxCommand("glsxtr@record@nameref", 8);
         }
      };

      supplementalPdfPath = new TeXPath(parser, basename+".pdf", false);

      if (supplementalPdfPaths == null)
      {
         supplementalPdfPaths = new Vector<TeXPath>();
      }

      supplementalPdfPaths.add(supplementalPdfPath);

      TeXParser auxTeXParser = auxParser.parseAuxFile(auxFile);

      Vector<AuxData> auxData = auxParser.getAuxData();

      if (supplementalRecords == null)
      {
         supplementalRecords = new Vector<SupplementalRecord>();
      }

      for (AuxData data : auxData)
      {
         String name = data.getName();

         if (name.equals("glsxtr@record")
              || (bib2gls.useCiteAsRecord() && name.equals("citation"))
              || name.equals("glsxtr@record@nameref"))
         {
            String recordLabel = data.getArg(0).toString(auxTeXParser);
            String recordPrefix;
            String recordCounter;
            String recordFormat;
            String recordLocation;
            String recordTitle = null;
            String recordHref = null;
            String recordHcounter = null;

            if (data.getNumArgs() >= 5)
            {
               recordPrefix = data.getArg(1).toString(auxTeXParser);
               recordCounter = data.getArg(2).toString(auxTeXParser);
               recordFormat = data.getArg(3).toString(auxTeXParser);
               recordLocation = data.getArg(4).toString(auxTeXParser);

               if (data.getNumArgs() == 8)
               {
                  recordTitle = data.getArg(5).toString(auxTeXParser);
                  recordHref = data.getArg(6).toString(auxTeXParser);
                  recordHcounter = data.getArg(7).toString(auxTeXParser);
               }

               // No support for wrglossary counter in supplemental
               // records. Convert to page location if found.

               if (recordCounter.equals("wrglossary"))
               {
                  TeXObject pageRef = AuxData.getPageReference(
                    auxData, auxTeXParser, "wrglossary."+recordLocation);

                  if (pageRef != null)
                  {
                     recordCounter = "page";
                     recordLocation = pageRef.toString(auxTeXParser);
                  }
               }
            }
            else
            {
               if (recordLabel.equals("*"))
               {
                  bib2gls.verboseMessage("message.ignored.record", 
                   "\\citation{*}");

                  continue;
               }

               recordPrefix = "";
               recordCounter = "page";
               recordFormat = "glsignore";
               recordLocation = "";
            }

            if (recordTitle == null)
            {
               supplementalRecords.add(new GlsSuppRecord(
                 bib2gls, recordLabel, recordPrefix, recordCounter,
                 recordFormat, recordLocation, supplementalPdfPath));
            }
            else
            {
               supplementalRecords.add(new GlsSuppRecordNameRef(
                 bib2gls, recordLabel, recordPrefix, recordCounter,
                 recordFormat, recordLocation, recordTitle,
                 recordHref, recordHcounter, supplementalPdfPath));
            }
         }
      }

      if (supplementalCategory == null)
      {
         supplementalCategory = category;
      }
   }

   private String replaceHexAndSpecial(String original)
   {
      return replaceHex(replaceEscapeSpecialChar(original));
   }

   private String replaceEscapeSpecialChar(String original)
   {
      // Replace all \#, \%, \_, \&, \{, \}

      Pattern p = Pattern.compile("\\\\([#%_\\&\\{\\}])");

      Matcher m = p.matcher(original);

      StringBuilder builder = new StringBuilder();
      int idx = 0;

      while (m.find())
      {
         String grp = m.group(1);

         builder.append(original.substring(idx, m.start()));

         builder.append(grp);

         idx = m.end();
      }

      builder.append(original.substring(idx));

      return builder.toString();
   }

   private String replaceHex(String original)
   {
      // Replace \\u<hex> sequences with the appropriate Unicode
      // characters.

      Pattern p = Pattern.compile("\\\\u\\s?([0-9A-Fa-f]+)");

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

      if (obj == null || obj instanceof MissingValue)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.missing.value", opt));
      }

      if (obj instanceof TeXObjectList)
      {
         obj = trimList((TeXObjectList)obj);
      }

      return obj;
   }

   private String getRequired(TeXParser parser, KeyValList list, String opt)
    throws IOException
   {
      TeXObject obj = list.getValue(opt);

      if (obj == null || obj instanceof MissingValue)
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

      if (obj == null || obj instanceof MissingValue) return defValue;

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

   private Locale getLocale(TeXParser parser, KeyValList list, String opt)
    throws IOException
   {
      String value = getOptional(parser, list, opt);

      if (value == null || "locale".equals(value))
      {
         return null;
      }
      else if ("doc".equals(value))
      {
         // if null, this becomes equivalent to the above
         return bib2gls.getDefaultLocale();
      }
      else
      {
         return bib2gls.getLocale(value);
      }
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
      return getStringArray(parser, list, opt, false);
   }

   private String[] getStringArray(TeXParser parser, KeyValList list, 
     String opt, boolean isRequired)
    throws IOException
   {
      TeXObject object;

      if (isRequired)
      {
         object = getRequiredObject(parser, list, opt);
      }
      else
      {
         object = list.getValue(opt);
      }

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

   private String[] getFieldArray(TeXParser parser, KeyValList list, 
     String opt, boolean isRequired)
    throws IOException
   {
      String[] array = getStringArray(parser, list, opt, isRequired);

      if (array == null)
      {
         return null;
      }

      for (String field : array)
      {
         if (!bib2gls.isKnownField(field)
              && !bib2gls.isKnownSpecialField(field))
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.field", field, opt));
         }
      }

      return array;
   }

   private TeXObject[] getTeXObjectArray(TeXParser parser, KeyValList list, 
     String opt, boolean isRequired)
    throws IOException
   {
      TeXObject obj;

      if (isRequired)
      {
         obj = getRequiredObject(parser, list, opt);
      }
      else
      {
         obj = list.getValue(opt);
      }

      if (obj instanceof TeXObjectList)
      {
         obj = trimList((TeXObjectList)obj);
      }

      CsvList csvList = CsvList.getList(parser, obj);

      int n = csvList.size();

      if (n == 0)
      {
         return null;
      }

      TeXObject[] array = new TeXObject[n];

      for (int i = 0; i < n; i++)
      {
         obj = csvList.getValue(i);

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

   private HashMap<String,String> getHashMap(TeXParser parser, 
      KeyValList list, String opt)
    throws IOException
   {
      TeXObject object = list.getValue(opt);

      if (object instanceof TeXObjectList)
      {
         object = trimList((TeXObjectList)object);
      }

      KeyValList keyList = KeyValList.getList(parser, object);

      int n = keyList.size();

      if (n == 0)
      {
         return null;
      }

      HashMap<String,String> map = new HashMap<String,String>(n);

      for (Iterator<String> it = keyList.keySet().iterator(); it.hasNext(); )
      {
         String field = it.next();

         object = keyList.get(field);

         String value = (object == null ? "" : object.toString(parser).trim());

         field = field.trim();

         map.put(field, value);
      }

      return map;
   }

   private HashMap<String,Vector<String>> getHashMapVector(TeXParser parser, 
      KeyValList list, String opt, boolean isRequired)
    throws IOException
   {
      TeXObject[] array = getTeXObjectArray(parser, list, opt, isRequired);

      if (array == null)
      {
         return null;
      }

      HashMap<String,Vector<String>> map 
        = new HashMap<String,Vector<String>>();

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

         if (split.size() != 2)
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.opt.keylist.value", 
               field, array[i].toString(parser), opt));
         }

         CsvList csvList = CsvList.getList(parser, split.get(1));

         int n = csvList.size();

         Vector<String> valList = new Vector<String>(n);

         for (int j = 0; j < n; j++)
         {
            TeXObject obj = csvList.getValue(j);

            if (obj instanceof TeXObjectList)
            {
               obj = trimList((TeXObjectList)obj);
            }

            valList.add(obj.toString(parser).trim());
         }

         map.put(field, valList);
      }

      return map;
   }

   private Vector<PatternReplace> getSubstitutionList(TeXParser parser, 
      KeyValList list, String opt, boolean isRequired)
    throws IOException
   {
      TeXObject[] array = getTeXObjectArray(parser, list, opt, isRequired);

      if (array == null)
      {
         return null;
      }

      Vector<PatternReplace> regexList = new Vector<PatternReplace>();

      for (TeXObject obj : array)
      {
         if (!(obj instanceof TeXObjectList 
               || ((TeXObjectList)obj).size() != 2))
         {
            throw new IllegalArgumentException(
               bib2gls.getMessage("error.invalid.substitution", 
                obj.toString(parser), opt));
         }

         TeXObject regexArg = ((TeXObjectList)obj).get(0);
         TeXObject replacementArg = ((TeXObjectList)obj).get(1);

         if (regexArg instanceof Group && !(regexArg instanceof MathGroup))
         {
            regexArg = ((Group)regexArg).toList();
         }

         if (replacementArg instanceof Group
              && !(replacementArg instanceof MathGroup))
         {
            replacementArg = ((Group)replacementArg).toList();
         }

         // replace \$ with $ and backslash u<hex> with given character

         if (replacementArg instanceof TeXObjectList)
         {
            TeXObjectList newList = new TeXObjectList();
            TeXParserListener listener = parser.getListener();

            while (((TeXObjectList)replacementArg).size() > 0)
            {
               TeXObject token = ((TeXObjectList)replacementArg).popToken();

               if (token instanceof ControlSequence)
               {
                  String name = ((ControlSequence)token).getName();

                  if (name.equals("$"))
                  {
                     token = listener.getOther('$');
                  }
                  else if (name.equals("u"))
                  {
                     ((TeXObjectList)replacementArg).push(
                       listener.getOther('"'));
                     TeXNumber num
                       = ((TeXObjectList)replacementArg).popNumber(parser);
                     token = listener.getOther(num.number(parser));
                     newList.add(listener.getOther('\\'));
                  }
               }

               newList.add(token);
            }

            replacementArg = newList;
         }

         String regex = regexArg.toString(parser);
         String replacement = replacementArg.toString(parser);

         regexList.add(new PatternReplace(regex, replacement));
      }

      return regexList;
   }

   private int getLetterNumberRule(TeXParser parser, KeyValList list,
     String opt)
   throws IOException
   {
      String val = getChoice(parser, list, opt, "before letter",
        "after letter", "between", "first", "last");

      if (val.equals("before letter"))
      {
         return Bib2GlsEntryLetterNumberComparator.NUMBER_BEFORE_LETTER;
      }
      else if (val.equals("after letter"))
      {
         return Bib2GlsEntryLetterNumberComparator.NUMBER_AFTER_LETTER;
      }
      else if (val.equals("between"))
      {
         return Bib2GlsEntryLetterNumberComparator.NUMBER_BETWEEN;
      }
      else if (val.equals("first"))
      {
         return Bib2GlsEntryLetterNumberComparator.NUMBER_FIRST;
      }
      else if (val.equals("last"))
      {
         return Bib2GlsEntryLetterNumberComparator.NUMBER_LAST;
      }

      // shouldn't happen
      throw new IllegalArgumentException("Invalid letter number rule");
   }

   private int getLetterNumberPuncRule(TeXParser parser, KeyValList list,
     String opt)
   throws IOException
   {
      String val = getChoice(parser, list, opt, "punc-space-first", 
        "punc-space-last", "space-punc-first", "space-punc-last",
        "space-first-punc-last", "punc-first-space-last",
        "punc-first-space-zero", "punc-last-space-zero",
        "punc-first-space-zero-match-next", "punc-last-space-zero-match-next");

      if (val.equals("punc-space-first"))
      {
         return Bib2GlsEntryLetterNumberComparator.PUNCTUATION_SPACE_FIRST;
      }
      else if (val.equals("punc-space-last"))
      {
         return Bib2GlsEntryLetterNumberComparator.PUNCTUATION_SPACE_LAST;
      }
      else if (val.equals("space-punc-first"))
      {
         return Bib2GlsEntryLetterNumberComparator.SPACE_PUNCTUATION_FIRST;
      }
      else if (val.equals("space-punc-last"))
      {
         return Bib2GlsEntryLetterNumberComparator.SPACE_PUNCTUATION_LAST;
      }
      else if (val.equals("space-first-punc-last"))
      {
         return Bib2GlsEntryLetterNumberComparator.SPACE_FIRST_PUNCTUATION_LAST;
      }
      else if (val.equals("punc-first-space-last"))
      {
         return Bib2GlsEntryLetterNumberComparator.PUNCTUATION_FIRST_SPACE_LAST;
      }
      else if (val.equals("punc-first-space-zero"))
      {
         return Bib2GlsEntryLetterNumberComparator.PUNCTUATION_FIRST_SPACE_ZERO;
      }
      else if (val.equals("punc-last-space-zero"))
      {
         return Bib2GlsEntryLetterNumberComparator.PUNCTUATION_LAST_SPACE_ZERO;
      }
      else if (val.equals("punc-first-space-zero-match-next"))
      {
         return Bib2GlsEntryLetterNumberComparator.PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT;
      }
      else if (val.equals("punc-last-space-zero-match-next"))
      {
         return Bib2GlsEntryLetterNumberComparator.PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT;
      }

      // shouldn't happen
      throw new IllegalArgumentException("Invalid letter number punc rule");
   }

   private int getBreakAt(TeXParser parser, KeyValList list,
     String opt)
   throws IOException
   {
      String val = getChoice(parser, list, opt, "none", "word",
        "character", "sentence", "upper-notlower", "upper-upper", 
        "upper-notlower-word", "upper-upper-word");

      if (val.equals("none"))
      {
         return Bib2GlsEntryComparator.BREAK_NONE;
      }
      else if (val.equals("word"))
      {
         return Bib2GlsEntryComparator.BREAK_WORD;
      }
      else if (val.equals("character"))
      {
         return Bib2GlsEntryComparator.BREAK_CHAR;
      }
      else if (val.equals("sentence"))
      {
         return Bib2GlsEntryComparator.BREAK_SENTENCE;
      }
      else if (val.equals("upper-notlower"))
      {
         return Bib2GlsEntryComparator.BREAK_UPPER_NOTLOWER;
      }
      else if (val.equals("upper-upper"))
      {
         return Bib2GlsEntryComparator.BREAK_UPPER_UPPER;
      }
      else if (val.equals("upper-notlower-word"))
      {
         return Bib2GlsEntryComparator.BREAK_UPPER_NOTLOWER_WORD;
      }
      else if (val.equals("upper-upper-word"))
      {
         return Bib2GlsEntryComparator.BREAK_UPPER_UPPER_WORD;
      }

      // shouldn't happen
      throw new IllegalArgumentException("Invalid break at setting: "+val);
   }

   private LabelListSortMethod getLabelListSortMethod(TeXParser parser, 
     TeXObjectList list, String opt)
   throws IOException
   {
      Vector<TeXObject> split = splitList(parser, ':', list);

      int n = split.size();

      if (n < 2 || n > 3)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.value", opt, 
              list.toString(parser)));
      }

      TeXObject fieldListArg = split.get(0);
      String[] fields;

      if (fieldListArg instanceof TeXObjectList)
      {
         if (fieldListArg instanceof Group)
         {
            fieldListArg = ((Group)fieldListArg).toList();
         }

         CsvList csvList = CsvList.getList(parser, fieldListArg);

         fields = new String[csvList.size()];

         for (int i = 0; i < fields.length; i++)
         {
            fields[i] = csvList.getValue(i).toString(parser).trim();

            if (!bib2gls.isKnownField(fields[i])
             && !bib2gls.isKnownSpecialField(fields[i]))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.field", fields[i], opt));
            }
         }
      }
      else
      {
         fields = new String[1];
         fields[0] = fieldListArg.toString(parser).trim();

         if (!bib2gls.isKnownField(fields[0])
          && !bib2gls.isKnownSpecialField(fields[0]))
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.field", fields[0], opt));
         }
      }

      String method = split.get(1).toString(parser).trim();
      String csname;

      if (n == 3)
      {
         csname = split.get(2).toString(parser).trim();
      }
      else
      {
         csname = null;
      }

      if (method.equals("none") || method.equals("unsrt")
          || method.equals("combine")
          || !SortSettings.isValidSortMethod(method))
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.sort.value", method, opt));
      }

      return new LabelListSortMethod(fields, method, csname);
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
             && !bib2gls.isKnownSpecialField(field)
             && !field.equals(PATTERN_FIELD_ID)
             && !field.equals(PATTERN_FIELD_ENTRY_TYPE)
             && !field.equals(PATTERN_FIELD_ORIGINAL_ENTRY_TYPE)
         )
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

   public void parseBibFiles(TeXParser parser)
   throws IOException
   {
      bib2gls.verboseMessage("message.parsing.resource.bib", texFile.getName());
      bib2gls.logDefaultEncoding(bibCharset);

      stripUnknownFieldPatterns();

      bibParserListener = new Bib2GlsBibParser(bib2gls, this, bibCharset);

      for (TeXPath src : sources)
      {
         File bibFile = src.getFile();

         String base = bibFile.getName();

         base = base.substring(0, base.length()-4);

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

                        bib2gls.logEncodingDetected(srcCharset);
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

         bibParserListener.setCharSet(srcCharset);
         bibParserListener.parse(bibFile);
      }
   }

   public String toString()
   {
      return texFile == null ? super.toString() : texFile.getName();
   }

   public void processBibList(TeXParser parser)
   throws IOException,Bib2GlsException
   {
      bib2gls.verboseMessage("message.processing.resource",
        texFile.getName());

      processPreamble();

      TeXParser bibParser = bibParserListener.getParser();
      Vector<BibData> list = bibParserListener.getBibData();

      bibData = new Vector<Bib2GlsEntry>();
      dualData = new Vector<Bib2GlsEntry>();

      Vector<GlsRecord> records = bib2gls.getRecords();
      Vector<GlsSeeRecord> seeRecords = bib2gls.getSeeRecords();

      Vector<Bib2GlsEntry> seeList = null;
      Vector<Bib2GlsEntry> aliasList = null;

      if (selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
      {
         seeList = new Vector<Bib2GlsEntry>();
      }

      boolean combine = "combine".equals(dualSortSettings.getMethod());

      for (int i = 0; i < list.size(); i++)
      {
         BibData data = list.get(i);

         if (data instanceof Bib2GlsEntry)
         {
            Bib2GlsEntry entry = (Bib2GlsEntry)data;
            entry.parseFields(bibParser);

            Bib2GlsEntry dual = null;

            String primaryId = entry.getId();
            String dualId = null;
            String tertiaryId = null;

            if (entry instanceof Bib2GlsDualEntry)
            {
               dual = ((Bib2GlsDualEntry)entry).createDual();
               entry.setDual(dual);
               dual.setDual(entry);
               dualId = dual.getId();
               hasDuals = true;

               if (((Bib2GlsDualEntry)entry).hasTertiary())
               {
                  tertiaryId = (tertiaryPrefix == null ?
                     entry.getOriginalId():
                     tertiaryPrefix+entry.getOriginalId());
                  hasTertiaries = true;
               }

               // is there a cross-reference list?
               // dual entries can't have an alias

               if (dual.getField("see") != null 
                   || dual.getField("seealso") != null)
               {
                  dual.initCrossRefs(parser);
               }

            }

            // does this entry have any records?

            boolean hasRecords = entry.hasRecords();
            boolean dualHasRecords = (dual != null && dual.hasRecords());

            for (GlsRecord record : records)
            {
               String recordLabel = getRecordLabel(record);

               if (recordLabel.equals(primaryId))
               {
                  entry.addRecord(record);
                  hasRecords = true;

                  if (dual != null &&
                    combineDualLocations != COMBINE_DUAL_LOCATIONS_OFF)
                  {
                     dual.addRecord(record.copy(dualId));
                     dualHasRecords = true;
                  }
               }

               if (dual != null)
               {
                  if (recordLabel.equals(dualId))
                  {
                     dual.addRecord(record);
                     dualHasRecords = true;


                     if (combineDualLocations != COMBINE_DUAL_LOCATIONS_OFF)
                     {
                        entry.addRecord(record.copy(primaryId));
                        hasRecords = true;
                     }
                  }

                  if (tertiaryId != null)
                  {
                     if (record.getLabel().equals(tertiaryId))
                     {
                        dual.addRecord(record.copy(dualId));
                        dualHasRecords = true;

                        if (combineDualLocations != COMBINE_DUAL_LOCATIONS_OFF)
                        {
                           entry.addRecord(record.copy(primaryId));
                           hasRecords = true;
                        }
                     }
                  }
               }
            }

            // any 'see' records?

            for (GlsSeeRecord record : seeRecords)
            {
               String recordLabel = getRecordLabel(record);

               if (recordLabel.equals(primaryId))
               {
                  entry.addRecord(record);
                  hasRecords = true;
               }

               if (dual != null)
               {
                  if (recordLabel.equals(dualId))
                  {
                     dual.addRecord(record);
                     dualHasRecords = true;
                  }
               }
            }

            setType(entry);
            setCategory(entry);
            setCounter(entry);

            if (discard(entry))
            {
               bib2gls.verboseMessage("message.discarding.entry",
                  entry.getId());

               continue;
            }

            // Only check required fields after filtering
            // (no need to worry about missing fields if the entry
            // is discarded).

            entry.checkRequiredFields();

            bibData.add(entry);

            if (dual != null)
            {
               if (discard(dual))
               {
                  bib2gls.verboseMessage("message.discarding.entry", dualId);

                  continue;
               }

               if (combine)
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

               if (dualPrimaryDependency)
               {
                  entry.addDependency(dualId);
                  dual.addDependency(primaryId);
               }

               if (bib2gls.getVerboseLevel() > 0)
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                    "message.dual.dep", dualId, primaryId));
               }
            }

            if (aliasLocations == ALIAS_LOC_TRANS 
                  && entry.getField("alias") != null)
            {
               if (aliasList == null)
               {
                  aliasList = new Vector<Bib2GlsEntry>();
               }

               aliasList.add(entry);
            }

            if (selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE ||
                 selectionMode == SELECTION_RECORDED_AND_DEPS)
            {
               // does this entry have a "see" or "seealso" field?

               entry.initCrossRefs(parser);

               if (dual != null)
               {
                  dual.initCrossRefs(parser);
               }

               if (seeList != null)
               {
                  if (entry.hasCrossRefs() 
                      || entry.getField("alias") != null)
                  {
                     seeList.add(entry);
                  }

                  if (dual != null && (dual.hasCrossRefs() 
                      || dual.getField("alias") != null))
                  {
                     seeList.add(dual);
                  }
               }

               // if entry has records, register dependencies with the
               // cross-resource list.

               if (hasRecords || dualHasRecords)
               {
                  bib2gls.registerDependencies(entry);

                  if (dual != null)
                  {
                     bib2gls.registerDependencies(dual);
                  }
               }
            }

            if (dateTimeList != null)
            {
               for (String field : dateTimeList)
               {
                  entry.convertFieldToDateTime(bibParser, field,
                    dateTimeListFormat, dateTimeListLocale,
                    true, true);

                  if (dual != null)
                  {
                     entry.convertFieldToDateTime(bibParser, field,
                       dualDateTimeListFormat,
                       dualDateTimeListLocale,
                       true, true);
                  }
               }
            }

            if (dateList != null)
            {
               for (String field : dateList)
               {
                  entry.convertFieldToDateTime(bibParser, field,
                    dateListFormat, dateListLocale,
                    true, false);

                  if (dual != null)
                  {
                     entry.convertFieldToDateTime(bibParser, field,
                       dualDateListFormat,
                       dualDateListLocale,
                       true, false);
                  }
               }
            }

            if (timeList != null)
            {
               for (String field : timeList)
               {
                  entry.convertFieldToDateTime(bibParser, field,
                    timeListFormat, timeListLocale,
                    false, true);

                  if (dual != null)
                  {
                     entry.convertFieldToDateTime(bibParser, field,
                       dualTimeListFormat,
                       dualTimeListLocale,
                       false, true);
                  }
               }
            }

         }
      }

      // Now that all entries have been defined and had their fields
      // initialised, it's possible to search for aliases.

      if (aliasList != null)
      {
         // Need to transfer records for aliased entries

         for (Bib2GlsEntry entry : aliasList)
         {
            String alias = entry.getFieldValue("alias");

            Bib2GlsEntry target = getEntry(alias, bibData);

            if (target == null && dualData != null)
            {
               target = getEntry(alias, dualData);
            }

            if (target == null)
            {
               bib2gls.warningMessage("warning.alias.not.found",
                 alias, entry.getId(), "alias-loc", "transfer");
            }
            else
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
               {
                  bib2gls.debugMessage("message.added.alias.dep", entry.getId(),
                    target.getId());
                  target.addDependency(entry.getId());
               }

               target.copyRecordsFrom(entry);
            }
         }
      }

      if (seeList != null)
      {
         for (Bib2GlsEntry entry : seeList)
         {
            addCrossRefs(entry, entry.getCrossRefs());

            addCrossRefs(entry, entry.getAlsoCrossRefs());

            String alias = entry.getFieldValue("alias");

            if (alias != null)
            {
               addCrossRefs(entry, alias);
            }
         }
      }

      addSupplementalRecords();
   }

   private void addCrossRefs(Bib2GlsEntry entry, String... xrList)
   {
      if (xrList == null) return;

      for (String xr : xrList)
      {
         Bib2GlsEntry xrEntry = null;

         if (dualData.isEmpty())
         {
            xrEntry = getEntry(xr, bibData);
         }
         else if (entry instanceof Bib2GlsDualEntry
                   && !((Bib2GlsDualEntry)entry).isPrimary())
         {
            xrEntry = getEntry(xr, dualData);

            if (xrEntry == null)
            {
               xrEntry = getEntry(xr, bibData);
            }
         }
         else
         {
            xrEntry = getEntry(xr, bibData);

            if (xrEntry == null)
            {
               xrEntry = getEntry(xr, dualData);
            }
         }

         if (xrEntry != null)
         {
            if (bib2gls.getVerboseLevel() > 0)
            {
               bib2gls.logMessage(bib2gls.getMessage(
                 "message.crossref.by", xrEntry.getId(), entry.getId()));
            }

            xrEntry.addDependency(entry.getId());
            xrEntry.addCrossRefdBy(entry);
         }
      }
   }

   private void addSupplementalRecords()
   {
      if (supplementalRecords != null)
      {
         for (SupplementalRecord suppRecord : supplementalRecords)
         {
            GlsRecord record = (GlsRecord)suppRecord;

            String label = getRecordLabel(record);

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

   private void addDependencies(Bib2GlsEntry entry, 
     Vector<Bib2GlsEntry> entries)
   {
      for (Iterator<String> it = entry.getDependencyIterator();
           it.hasNext(); )
      {
         String dep = it.next();

         // Has the dependency already been added or does it have
         // any records?  (Don't want to get stuck in an infinite loop!)

         if (isDependent(dep))
         {
            continue;
         }

         addDependent(dep);

         if (bib2gls.hasRecord(dep))
         {
            continue;
         }

         Bib2GlsEntry depEntry = getEntry(dep, entries);

         if (depEntry != null)
         {
            // add any dependencies

            addDependencies(depEntry, entries);
         }
      }
   }

   public boolean isDependent(String id)
   {
      return dependencies.contains(id);
   }

   public void addDependent(String id)
   {
      if (!dependencies.contains(id))
      {
         bib2gls.verboseMessage("message.added.dep", id);
         dependencies.add(id);
      }
   }

   public Vector<String> getDependencies()
   {
      return dependencies;
   }

   public int processData()
      throws IOException,Bib2GlsException
   {
      if (masterGlsTeXPath == null)
      {
         bib2gls.verboseMessage("message.selecting.entries", texFile.getName());

         return processBibData();
      }
      else
      {
         bib2gls.verboseMessage("message.processing.master", masterGlsTeXPath);

         processMaster();
         return -1;
      }
   }

   private void processMaster()
      throws IOException,Bib2GlsException
   {
      Charset charSet = bib2gls.getTeXCharset();

      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));
      bib2gls.logEncoding(charSet);

      // Already checked openout_any in init method

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(texFile, charSet.name());

         if (bib2gls.suppressFieldExpansion())
         {
            writer.println("\\glsnoexpandfields");
         }

         bib2gls.writeCommonCommands(writer);

         writeLabelPrefixHooks(writer);

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
         if (preambleList == null)
         {
            preambleList = list;
         }
         else
         {
            preambleList.addAll(list);
         }
      }
   }

   public void processPreamble()
    throws IOException
   {
      if (preambleList != null)
      {
         bib2gls.processPreamble(preambleList);
      }
   }

   private Bib2GlsEntry addMissingParent(Bib2GlsEntry childEntry, 
      Vector<Bib2GlsEntry> data)
   {
      String parentId = childEntry.getParent();

      if (parentId == null) return null;

      // has parent already been defined?

      Bib2GlsEntry parent = getEntry(parentId, data);

      if (parent != null)
      {
         return null;
      }

      TeXParser bibParser = bibParserListener.getParser();

      parent = childEntry.createParent(bibParser);

      if (parent != null)
      {
         bib2gls.verboseMessage("message.created.missing.parent",
           parent.getId(), childEntry.getId());

         data.add(parent);

         if (missingParentCategory != null)
         {
            if (missingParentCategory.equals("same as child"))
            {
               String childCat = childEntry.getFieldValue("category");

               if (childCat != null)
               {
                  parent.putField("category", childCat);
               }
            }
            else if (missingParentCategory.equals("same as base"))
            {
               parent.putField("category", parent.getBase());
            }
            else
            {
               parent.putField("category", missingParentCategory);
            }
         }
      }

      return parent;
   }

   private void addHierarchy(Bib2GlsEntry childEntry, 
      Vector<Bib2GlsEntry> entries, Vector<Bib2GlsEntry> data)
   throws Bib2GlsException
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

      if (parentId.equals(childEntry.getId()))
      {
         throw new Bib2GlsException(bib2gls.getMessage("error.child.parent",
           parentId));
      }

      Bib2GlsEntry parent = getEntry(parentId, data);

      if (parent != null)
      {
         bib2gls.verboseMessage("message.added.parent", parentId);
         addHierarchy(parent, entries, data);
         addEntry(entries, parent);
      }
   }

   public String flipLabel(String label)
   {
      if (labelPrefix == null)
      {
         if (label.startsWith(dualPrefix))
         {
            return label.substring(dualPrefix.length());
         }
         else if (hasTertiaries && label.startsWith(tertiaryPrefix))
         {
            return label.substring(tertiaryPrefix.length());
         }
         else
         {
            return dualPrefix+label;
         }
      }

      if (dualPrefix == null)
      {
         if (label.startsWith(labelPrefix))
         {
            return label.substring(labelPrefix.length());
         }
         else if (hasTertiaries && label.startsWith(tertiaryPrefix))
         {
            return label.substring(tertiaryPrefix.length());
         }
         else
         {
            return labelPrefix+label;
         }
      }

      if (label.startsWith(labelPrefix))
      {
         String substr = label.substring(labelPrefix.length());

         if (substr.startsWith(dualPrefix))
         {
            return substr;
         }

         return dualPrefix+substr;
      }

      if (label.startsWith(dualPrefix))
      {
         String substr = label.substring(dualPrefix.length());

         if (substr.startsWith(labelPrefix))
         {
            return substr;
         }

         return labelPrefix+substr;
      }

      if (hasTertiaries)
      {
         if (tertiaryPrefix != null && label.startsWith(tertiaryPrefix))
         {
            String substr = label.substring(tertiaryPrefix.length());

            if (substr.startsWith(dualPrefix)||substr.startsWith(labelPrefix))
            {
               return substr;
            }

            return dualPrefix+substr;
         }
      }

      return null;
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

   public Bib2GlsEntry getBib2GlsEntry(String label, Vector<BibData> data)
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

   public String getRecordLabel(GlsRecord record)
   {
      String label = record.getLabel();

      if (recordLabelPrefix == null || label.startsWith(recordLabelPrefix))
      {
         return label;
      }

      return recordLabelPrefix+label;
   }

   public String getRecordLabel(GlsSeeRecord record)
   {
      if (recordLabelPrefix == null)
      {
         return record.getLabel();
      }

      return recordLabelPrefix+record.getLabel();
   }

   private void addEntry(Vector<Bib2GlsEntry> entries, Bib2GlsEntry entry)
   {
      String id = entry.getId();

      for (Bib2GlsEntry e : entries)
      {
         if (e.getId().equals(id))
         {
            bib2gls.debugMessage("message.entry.already.added", id,
             e.getOriginalEntryType(), e.getOriginalId(),
             entry.getOriginalEntryType(), entry.getOriginalId());
            return;
         }
      }

      entries.add(entry);
      entry.setSelected(true);
   }

   private void processData(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries, String entrySort)
      throws Bib2GlsException
   {
      Vector<String> fields = bib2gls.getFields();
      Vector<GlsRecord> records = bib2gls.getRecords();
      Vector<GlsSeeRecord> seeRecords = bib2gls.getSeeRecords();

      if (selectionMode == SELECTION_ALL)
      {
         // select all entries

         bib2gls.debugMessage("message.selecting.all");

         for (Bib2GlsEntry entry : data)
         {
            addEntry(entries, entry);
         }
      }
      else if (entrySort == null || entrySort.equals("none")
         || matchAction == MATCH_ACTION_ADD)
      {
         // add all entries that have been recorded in the order of
         // definition

         for (Bib2GlsEntry entry : data)
         {
            boolean hasRecords = entry.hasRecords();
            Bib2GlsEntry dual = entry.getDual();
            boolean dualHasRecords = (dual != null && dual.hasRecords()
              && dualPrimaryDependency);
            boolean recordedOrDependent = hasRecords
              || bib2gls.isDependent(entry.getId());

            if (recordedOrDependent ||
                (matchAction == MATCH_ACTION_ADD && fieldPatterns != null
                 && !notMatch(entry))
               || (dual != null && 
                    (dualHasRecords ||
                       matchAction == MATCH_ACTION_ADD && fieldPatterns != null
                       && !notMatch(dual))))
            {
               if (bib2gls.getDebugLevel() > 0)
               {
                  if (hasRecords)
                  {
                     bib2gls.debugMessage("message.selecting.entry.records",
                      entry);
                  }
                  else if (recordedOrDependent)
                  {
                     bib2gls.debugMessage("message.selecting.entry.dep",
                      entry);
                  }
                  else if (dualHasRecords)
                  {
                     bib2gls.debugMessage("message.selecting.entry.dualrecords",
                      entry, dual);
                  }
                  else
                  {
                     bib2gls.debugMessage("message.selecting.entry", entry);
                  }
               }

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
               }

               addEntry(entries, entry);

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
               {
                  addDependencies(entry, data);
               }
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

            String recordLabel = getRecordLabel(record);

            Bib2GlsEntry entry = getEntry(recordLabel, data);

            if (entry == null && hasDuals)
            {
               String label = flipLabel(recordLabel);

               if (label != null)
               {
                  entry = getEntry(label, data);
               }
            }

            if (entry != null && !entries.contains(entry))
            {
                bib2gls.debugMessage("message.selecting.entry.record.match",
                  entry.getId(), recordLabel);

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
               }

               addEntry(entries, entry);

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
               {
                  addDependencies(entry, data);
               }
            }
         }

         for (int i = 0; i < seeRecords.size(); i++)
         {
            GlsSeeRecord record = seeRecords.get(i);

            String recordLabel = getRecordLabel(record);

            Bib2GlsEntry entry = getEntry(recordLabel, data);

            if (entry == null && hasDuals)
            {
               String label = flipLabel(recordLabel);

               if (label != null)
               {
                  entry = getEntry(label, data);
               }
            }

            if (entry != null && !entries.contains(entry))
            {
                bib2gls.debugMessage("message.selecting.entry.seerecord.match",
                  entry.getId(), recordLabel);

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
               }

               addEntry(entries, entry);

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
               {
                  addDependencies(entry, data);
               }
            }
         }

         if (supplementalRecords != null && supplementalSelection != null)
         {
            for (SupplementalRecord suppRecord : supplementalRecords)
            {
               GlsRecord record = (GlsRecord) suppRecord;

               String label = getRecordLabel(record);

               Bib2GlsEntry entry = getEntry(label, data);

               if (entry != null && !entries.contains(entry))
               {
                  if (supplementalSelection.length == 1
                  && supplementalSelection[0].equals("all"))
                  {
                      bib2gls.debugMessage(
                        "message.selecting.entry.suprecord.match",
                        entry.getId(), label);

                     if (selectionMode == SELECTION_RECORDED_AND_DEPS
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                       ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
                     {
                        addHierarchy(entry, entries, data);
                     }

                     addEntry(entries, entry);

                     if (selectionMode == SELECTION_RECORDED_AND_DEPS
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
                     {
                        addDependencies(entry, data);
                     }
                  }
                  else
                  {
                     for (String selLabel : supplementalSelection)
                     {
                        if (selLabel.equals(label))
                        {
                           bib2gls.debugMessage(
                              "message.selecting.entry.suprecord.match",
                              entry.getId(), label);

                           if (selectionMode == SELECTION_RECORDED_AND_DEPS
                             ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                             ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
                           {
                              addHierarchy(entry, entries, data);
                           }

                           addEntry(entries, entry);

                           if (selectionMode == SELECTION_RECORDED_AND_DEPS
                             ||selectionMode
                                  == SELECTION_RECORDED_AND_DEPS_AND_SEE)
                           {
                              addDependencies(entry, data);
                           }
                           break;
                        }
                     }
                  }
               }
            }
         }

         if (bib2gls.hasCrossResourceDependencies())
         {
            for (Iterator<String> it = bib2gls.getDependencyIterator();
                 it.hasNext(); )
            {
               String dep = it.next();

               Bib2GlsEntry entry = getEntry(dep, data);

               if (entry != null)
               {
                  if (!entries.contains(entry))
                  {
                     bib2gls.debugMessage(
                       "message.selecting.entry.crossresource.dep", entry);

                     if (selectionMode == SELECTION_RECORDED_AND_DEPS
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                       ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
                     {
                        addHierarchy(entry, entries, data);
                     }

                     addEntry(entries, entry);

                     if (selectionMode == SELECTION_RECORDED_AND_DEPS
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
                     {
                        addDependencies(entry, data);
                     }
                  }
               }
            }
         }
      }

      if (createMissingParents)
      {
         for (int i = 0; i < entries.size(); i++)
         {
            Bib2GlsEntry entry = entries.get(i);

            Bib2GlsEntry parentEntry = addMissingParent(entry, data);

            if (parentEntry != null 
                 && selectionMode != SELECTION_RECORDED_NO_DEPS)
            {
               addEntry(entries, parentEntry);
            }
         }
      }

      processDeps(data, entries);
   }

   private void processDeps(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries)
      throws Bib2GlsException
   {
      // add any dependencies

      if (selectionMode == SELECTION_RECORDED_AND_DEPS
        ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE)
      {
         for (String id : dependencies)
         {
            Bib2GlsEntry dep = getEntry(id, data);

            if (dep == null && hasDuals)
            {
               String label = flipLabel(id);

               if (label != null)
               {
                  dep = getEntry(label, data);
               }
            }

            if (dep != null && !entries.contains(dep))
            {
               addHierarchy(dep, entries, data);
               addEntry(entries, dep);
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

   }

   private void sortDataIfRequired(Vector<Bib2GlsEntry> entries,
     SortSettings settings, String entryGroupField)
    throws Bib2GlsException
   {
      if (settings.requiresSorting() && entries.size() > 0)
      {
         sortData(entries, settings, entryGroupField, null);
      }
      else
      {
         bib2gls.verboseMessage("message.no.sort.required");
      }

      if (sortLabelList != null)
      {
         for (LabelListSortMethod method : sortLabelList)
         {
            try
            {
               method.sort(entries, settings);
            }
            catch (IOException e)
            {
               throw new Bib2GlsException(e.getMessage(), e);
            }
         }
      }
   }

   private void sortData(Vector<Bib2GlsEntry> entries, SortSettings settings,
     String entryGroupField, String entryType)
    throws Bib2GlsException
   {
      sortData(entries, settings, settings.getSortField(), entryGroupField,
       entryType);
   }

   private void sortData(Vector<Bib2GlsEntry> entries, SortSettings settings,
     String sortField, String entryGroupField, String entryType)
    throws Bib2GlsException
   {
      if (settings.isRandom())
      {
         if (random == null)
         {
            random = new Random();
         }

         Collections.shuffle(entries, random);
      }
      else if (settings.isLetter())
      {
         Bib2GlsEntryLetterComparator comparator = 
            new Bib2GlsEntryLetterComparator(bib2gls, entries, 
              settings, sortField, entryGroupField, entryType);

         comparator.sortEntries();
      }
      else if (settings.isLetterNumber())
      {
         Bib2GlsEntryLetterNumberComparator comparator = 
            new Bib2GlsEntryLetterNumberComparator(bib2gls, entries, 
              settings, sortField, entryGroupField, entryType);

         comparator.sortEntries();
      }
      else if (settings.isNumeric())
      {
         Bib2GlsEntryNumericComparator comparator = 
            new Bib2GlsEntryNumericComparator(bib2gls, entries, 
              settings, sortField, entryGroupField, entryType);

         comparator.sortEntries();
      }
      else if (settings.isDateOrTimeSort())
      {
         Bib2GlsEntryDateTimeComparator comparator = 
            new Bib2GlsEntryDateTimeComparator(bib2gls, entries, 
              settings, sortField, entryGroupField, entryType);

         comparator.sortEntries();
      }
      else
      {
         try
         {
            Bib2GlsEntryComparator comparator = 
               new Bib2GlsEntryComparator(bib2gls, entries, settings,
                  sortField, entryGroupField, entryType);

            comparator.sortEntries();
         }
         catch (ParseException e)
         {
            throw new Bib2GlsException(bib2gls.getMessage(
             "error.invalid.sort.rule", e.getMessage()), e);
         }
      }
   }

   private int processBibData()
      throws IOException,Bib2GlsException
   {
      if (bibData == null)
      {
         throw new NullPointerException(
            "No data (processBibList must come before processData)");
      }

      if (dualField != null)
      {
         bib2gls.addField(dualField);
      }

      // check field mapping keys

      checkFieldMaps(dualEntryMap, "dual-entry-map");
      checkFieldMaps(dualAbbrevMap, "dual-abbrv-map");
      checkFieldMaps(dualSymbolMap, "dual-symbol-map");
      checkFieldMaps(dualAbbrevEntryMap, "dual-abbrventry-map");
      checkFieldMaps(dualIndexEntryMap, "dual-indexentry-map");
      checkFieldMaps(dualIndexSymbolMap, "dual-indexsymbol-map");
      checkFieldMaps(dualIndexAbbrevMap, "dual-indexabbrv-map");

      Vector<Bib2GlsEntry> entries = new Vector<Bib2GlsEntry>();

      processData(bibData, entries, sortSettings.getMethod());

      if ("use-reverse".equals(sortSettings.getMethod()))
      {
         // reverse the list (won't work for hierarchical entries)

         for (int i = 0, j = entries.size(), n = j/2; i < n; i++)
         {
            Bib2GlsEntry entry1 = entries.get(i);
            Bib2GlsEntry entry2 = entries.get(--j);

            entries.set(i, entry2);
            entries.set(j, entry1);
         }
      }

      if (limit > 0 && entries.size() > limit)
      {
         for (int i = limit; i < entries.size(); i++)
         {
            entries.get(i).setSelected(false);
         }

         bib2gls.verboseMessage("message.truncated", limit);
         entries.setSize(limit);
      }

      if (bib2gls.getVerboseLevel() > 0)
      {
         bib2gls.logMessage(bib2gls.getChoiceMessage("message.selected", 0,
            "entry", 3, entries.size()));
      }

      Vector<Bib2GlsEntry> dualEntries = null;

      int entryCount = entries.size();

      if (dualData.size() > 0)
      {
         dualEntries = new Vector<Bib2GlsEntry>();

         // If dual-sort=use or none, use the same order as entries
         // (unless reverse sort). 

         if (!dualSortSettings.requiresSorting())
         {
            if (!"use-reverse".equals(sortSettings.getMethod()) 
                 && "use-reverse".equals(dualSortSettings.getMethod()))
            {
               for (int i = entries.size()-1; i >= 0; i--)
               {
                  Bib2GlsEntry entry = entries.get(i);

                  if (entry instanceof Bib2GlsDualEntry)
                  {
                     Bib2GlsEntry dual = entry.getDual();

                     addEntry(dualEntries, dual);
                  }
               }
            }
            else
            {
               for (Bib2GlsEntry entry : entries)
               {
                  if (entry instanceof Bib2GlsDualEntry)
                  {
                     Bib2GlsEntry dual = entry.getDual();

                     addEntry(dualEntries, dual);
                  }
               }
            }
         }
         else
         {
            processData(dualData, dualEntries, dualSortSettings.getMethod());
         }

         processDeps(dualData, dualEntries);

         for (Bib2GlsEntry dual : dualEntries)
         {
            setDualType(dual);
            setDualCategory(dual);
            setDualCounter(dual);
         }

         entryCount += dualEntries.size();

         if (limit > 0 && dualEntries.size() > limit)
         {
            bib2gls.verboseMessage("message.truncated", limit);
            dualEntries.setSize(limit);
         }

         if (bib2gls.getVerboseLevel() > 0)
         {
            bib2gls.logMessage(bib2gls.getChoiceMessage(
               "message.dual.selected", 0, "entry", 3, dualEntries.size()));
         }
      }

      sortDataIfRequired(entries, sortSettings, "group");

      if (dualEntries != null)
      {
         sortDataIfRequired(dualEntries, dualSortSettings, "group");
      }

      String charSetName = bib2gls.getTeXCharset().name();

      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));
      bib2gls.logEncoding(charSetName);

      // Already checked openout_any in init method

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(texFile, charSetName);

         if (bib2gls.suppressFieldExpansion())
         {
            writer.println("\\glsnoexpandfields");
         }

         bib2gls.writeCommonCommands(writer);

         writeLabelPrefixHooks(writer);

         writeAllCapsCsDefs(writer);

         if (triggerType != null)
         {
            writer.format("\\provideignoredglossary*{%s}%n", triggerType);
         }

         if (bibtexAuthorList != null)
         {
            writer.println("\\ifdef\\DTLformatlist");
            writer.println("{% datatool v2.28+");
            writer.print(" \\providecommand*{\\bibglscontributorlist}[2]");
            writer.println("{\\DTLformatlist{#1}}");
            writer.println("}");
            writer.println("{% datatool v2.27 or earlier");
            writer.println(" \\providecommand*{\\bibglscontributorlist}[2]{%");
            writer.println("  \\def\\bibgls@sep{}%");
            writer.print("  \\@for\\bibgls@item:=#1\\do{");
            writer.println("\\bibgls@sep\\bibgls@item\\def\\bibgls@sep{, }}%");
            writer.println(" }");
            writer.println("}");
            writeBibGlsContributorDef(writer);
         }

         if (dateTimeList != null)
         {
            writer.print("\\providecommand{\\bibglsdatetime}[9]");
            writer.println("{\\bibglsdatetimeremainder}");
            writer.format("\\providecommand{\\bibglsdatetimeremainder}[4]{#4}");
            writer.println();
         }

         if (dateList != null)
         {
            writer.println("\\providecommand{\\bibglsdate}[7]{#7}");
            writer.println();
         }

         if (timeList != null)
         {
            writer.println("\\providecommand{\\bibglstime}[7]{#7}");
            writer.println();
         }

         if (counters != null)
         {
            writer.println("\\providecommand{\\bibglslocationgroup}[3]{#3}");
            writer.println("\\providecommand{\\bibglslocationgroupsep}{\\bibglsdelimN}");
            writer.println();
         }

         if (compactRanges > 1)
         {
            writer.println("\\providecommand{\\bibglscompact}[3]{#3}");
         }

         if (supplementalRecords != null)
         {
            writer.println("\\providecommand{\\bibglssupplementalsep}{\\bibglsdelimN}");
            writer.println("\\providecommand{\\bibglssupplemental}[2]{#2}");

            if (bib2gls.isMultipleSupplementarySupported())
            {
               writer.println("\\providecommand{\\bibglssupplementalsubsep}{\\bibglsdelimN}");

               writer.println("\\providecommand{\\bibglssupplementalsublist}[3]{#3}");
            }

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

         if (aliasLocation != Bib2GlsEntry.NO_SEE)
         {
            writer.println("\\providecommand{\\bibglsaliassep}{, }");
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

            // unicode groups:

            writer.println("  \\providecommand{\\bibglsunicodegroup}[4]{#4#3}");
            writer.println("  \\providecommand{\\bibglsunicodegrouptitle}[4]{\\unexpanded{#1}}");
            writer.println("  \\providecommand{\\bibglssetunicodegrouptitle}[1]{%");
            writer.println("    \\glsxtrsetgrouptitle{\\bibglsunicodegroup#1}{\\bibglsunicodegrouptitle#1}}");

            // other groups:

            writer.println("  \\providecommand{\\bibglsothergroup}[3]{glssymbols}");
            writer.println("  \\providecommand{\\bibglsothergrouptitle}[3]{\\protect\\glssymbolsgroupname}");
            writer.println("  \\providecommand{\\bibglssetothergrouptitle}[1]{%");
            writer.println("    \\glsxtrsetgrouptitle{\\bibglsothergroup#1}{\\bibglsothergrouptitle#1}}");

            // empty groups:

            writer.println("  \\providecommand{\\bibglsemptygroup}[1]{glssymbols}");
            writer.println("  \\providecommand{\\bibglsemptygrouptitle}[1]{\\protect\\glssymbolsgroupname}");
            writer.println("  \\providecommand{\\bibglssetemptygrouptitle}[1]{%");
            writer.println("    \\glsxtrsetgrouptitle{\\bibglsemptygroup#1}{\\bibglsemptygrouptitle#1}}");

            // number groups

            writer.println("  \\providecommand{\\bibglsnumbergroup}[3]{glsnumbers}");
            writer.println("  \\providecommand{\\bibglsnumbergrouptitle}[3]{\\protect\\glsnumbersgroupname}");
            writer.println("  \\providecommand{\\bibglssetnumbergrouptitle}[1]{%");
            writer.println("    \\glsxtrsetgrouptitle{\\bibglsnumbergroup#1}{\\bibglsnumbergrouptitle#1}}");

            boolean requiresDateTime = sortSettings.isDateTimeSort()
                                    || dualSortSettings.isDateTimeSort()
                                    || secondarySortSettings.isDateTimeSort();

            boolean requiresDate = sortSettings.isDateSort()
                                || dualSortSettings.isDateSort()
                                || secondarySortSettings.isDateSort();


            boolean requiresTime = sortSettings.isTimeSort()
                                 || dualSortSettings.isTimeSort()
                                 || secondarySortSettings.isTimeSort();

            // date-time groups

            if (requiresDateTime)
            {
               writer.println("  \\providecommand{\\bibglsdatetimegroup}[9]{#1#2#3\\@firstofone}");
               writer.println("  \\providecommand{\\bibglsdatetimegrouptitle}[9]{#1-#2-#3\\@gobble}");
               writer.println("  \\providecommand{\\bibglssetdatetimegrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsdatetimegroup#1}{\\bibglsdatetimegrouptitle#1}}");
            }

            // date groups

            if (requiresDate)
            {
               writer.println("  \\providecommand{\\bibglsdategroup}[7]{#1#2#4#7}");
               writer.println("  \\providecommand{\\bibglsdategrouptitle}[7]{#1-#2}");
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
               writer.println("  \\providecommand{\\bibglsdatetimegroup}[9]{#1-#2-#3\\@gobble}");
            }

            if (requiresDate)
            {
               writer.println("  \\providecommand{\\bibglsdategroup}[7]{#1-#2}");
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

            if (bib2gls.isLastResource(this))
            {
               writer.println("\\providecommand{\\bibglssetlastgrouptitle}[2]{}");

               writer.println();
            }

            createHyperGroups = writeGroupDefs(writer);

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
               writer.println("    \\ifcsdef{@gls@hypergrouplist@#1}%");
               writer.println("    {\\@glstarget{\\glsnavhyperlinkname{#1}{#2}}{#3}}%");
               writer.println("    {\\bibglsorgnavhypertarget{#1}{#2}{#3}}%");
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

         if (getSavePrimaryLocationSetting() != SAVE_PRIMARY_LOCATION_OFF)
         {
            writer.println("\\providecommand{\\bibglsprimary}[2]{#2}");
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

         if (preamble != null && savePreamble)
         {
            writer.println(preamble);
            writer.println();
         }

         Vector<Bib2GlsEntry> secondaryList = null;

         if (secondaryType != null)
         {
            secondaryList = new Vector<Bib2GlsEntry>(entryCount);
         }

         Font font = null;
         FontRenderContext frc = null;

         if (setWidest)
         {
            // Just using the JVM's default serif font as a rough
            // guide to guess the width.
            font = new Font("Serif", 0, 12);
            frc = new FontRenderContext(null, false, false);
         }

         if (!bib2gls.isMultipleSupplementarySupported()
             && supplementalPdfPath != null
             && supplementalCategory != null)
         {
            writer.format(
              "\\glssetcategoryattribute{%s}{externallocation}{%s}%n%n", 
              supplementalCategory, bib2gls.getTeXPathHref(supplementalPdfPath));
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

            if (setWidest)
            {
               updateWidestName(entry, font, frc);
            }

            if (saveLocations)
            {
               if (entry instanceof Bib2GlsDualEntry
                 && !(combineDualLocations == COMBINE_DUAL_LOCATIONS_OFF
                   ||combineDualLocations == COMBINE_DUAL_LOCATIONS_BOTH))
               {
                  boolean isPrimary = ((Bib2GlsDualEntry)entry).isPrimary();

                  if ((combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY 
                        && isPrimary)
                    ||(combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL 
                        && !isPrimary))
                  {
                     entry.updateLocationList();
                  }
               }
               else
               {
                  entry.updateLocationList();
               }
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

            writeBibEntry(writer, entry);

            if (!bib2gls.isMultipleSupplementarySupported()
                && supplementalPdfPath != null 
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
         }

         if (dualEntries != null)
         {
            if (flattenLonely == FLATTEN_LONELY_POST_SORT
                || (saveChildCount && flattenLonely != FLATTEN_LONELY_PRE_SORT))
            {// need to check parents before writing definitions

               flattenLonelyChildren(dualEntries);
            }

            for (int i = 0, n = dualEntries.size(); i < n; i++)
            {
               Bib2GlsEntry entry = dualEntries.get(i);

               bib2gls.verbose(entry.getId());

               if (setWidest)
               {
                  updateWidestName(entry, dualType, font, frc);

                  if (entry instanceof Bib2GlsDualEntry
                       && ((Bib2GlsDualEntry)entry).hasTertiary()
                       && tertiaryType != null)
                  {
                     updateWidestName(entry, tertiaryType, font, frc);
                  }
               }

               if (saveLocations
                && combineDualLocations != COMBINE_DUAL_LOCATIONS_PRIMARY)
               {
                  entry.updateLocationList();
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

               writeBibEntry(writer, entry);

               if (secondaryList != null)
               {
                  secondaryList.add(entry);
               }
            }
         }

         if (secondaryList != null)
         {
            writer.format("\\provideignoredglossary*{%s}%n", secondaryType);

            writer.println("\\ifdef\\glsxtrgroupfield{%");
            writer.format("  \\apptoglossarypreamble[%s]{",
               secondaryType);
            writer.println("\\renewcommand{\\glsxtrgroupfield}{secondarygroup}}%");
            writer.println("}{}");

            if (secondarySortSettings.isUnsrt())
            {
               for (Bib2GlsEntry entry : secondaryList)
               {
                  writeCopyToGlossary(writer, entry);

                  if (setWidest)
                  {
                     updateWidestName(entry, secondaryType, font, frc);
                  }
               }
            }
            else if (secondarySortSettings.isOrderOfRecords())
            {
               Vector<GlsRecord> records = bib2gls.getRecords();

               if (secondarySortSettings.isReverse())
               {
                  for (int i = records.size()-1; i >= 0; i--)
                  {
                     GlsRecord record = records.get(i);

                     Bib2GlsEntry entry = getEntry(getRecordLabel(record), 
                        secondaryList);

                     if (entry != null)
                     {
                        writeCopyToGlossary(writer, entry);

                        if (setWidest)
                        {
                           updateWidestName(entry, secondaryType, font, frc);
                        }
                     }
                  }
               }
               else
               {
                  for (GlsRecord record : records)
                  {
                     Bib2GlsEntry entry = getEntry(getRecordLabel(record), 
                        secondaryList);

                     if (entry != null)
                     {
                        writeCopyToGlossary(writer, entry);

                        if (setWidest)
                        {
                           updateWidestName(entry, secondaryType, font, frc);
                        }
                     }
                  }
               }
            }
            else
            {
               if (bib2gls.useGroupField())
               {
                  groupTitleMap = new HashMap<String,GroupTitle>();
               }

               sortData(secondaryList, secondarySortSettings, 
                       secondaryField == null ? 
                         secondarySortSettings.getSortField() : secondaryField,
                       "secondarygroup", secondaryType);

               if (bib2gls.useGroupField())
               {
                  writeGroupDefs(writer);
               }

               for (Bib2GlsEntry entry : secondaryList)
               {
                  writeCopyToGlossary(writer, entry);

                  if (setWidest)
                  {
                     updateWidestName(entry, secondaryType, font, frc);
                  }
               }
            }
         }

         if (setWidest)
         {
            writer.println("\\ifdef\\glsxtrSetWidest");
            writer.println("{");
            writer.println("  \\providecommand*{\\bibglssetwidest}[2]{%");
            writer.println("    \\glsxtrSetWidest{}{#1}{#2}%");
            writer.println("  }");
            writer.println("  \\providecommand*{\\bibglssetwidestfortype}[3]{%");
            writer.println("    \\glsxtrSetWidest{#1}{#2}{#3}%");
            writer.println("  }");
            writer.println("}");
            writer.println("{");
            writer.println("  \\ifdef\\glsupdatewidest");
            writer.println("  {");
            writer.println("    \\providecommand*{\\bibglssetwidest}[2]{%");
            writer.println("      \\glsupdatewidest[#1]{#2}%");
            writer.println("    }");
            writer.println("    \\providecommand*{\\bibglssetwidestfortype}[3]{%");
            writer.print("      \\apptoglossarypreamble[#1]");
            writer.println("{\\glsupdatewidest[#2]{#3}}%");
            writer.println("    }");
            writer.println("  }");
            writer.println("  {");
            writer.println("    \\providecommand*{\\bibglssetwidest}[2]{%");
            writer.println("      \\glssetwidest[#1]{#2}%");
            writer.println("    }");
            writer.println("    \\providecommand*{\\bibglssetwidestfortype}[3]{%");
            writer.print("      \\apptoglossarypreamble[#1]");
            writer.println("{\\glssetwidest[#2]{#3}}%");
            writer.println("    }");
            writer.println("  }");
            writer.println("}");

            writer.println("\\ifdef\\glsxtrSetWidestFallback");
            writer.println("{");
            writer.println("  \\providecommand*{\\bibglssetwidestfallback}[1]{%");
            writer.println("    \\glsxtrSetWidestFallback{2}{#1}%");
            writer.println("  }");
            writer.println("  \\providecommand*{\\bibglssetwidesttoplevelfallback}[1]{%");
            writer.println("    \\glsxtrSetWidestFallback{0}{#1}%");
            writer.println("  }");
            writer.println("}");
            writer.println("{");
            writer.println("  \\providecommand*{\\bibglssetwidestfallback}[1]{%");
            writer.println("    \\glsFindWidestLevelTwo[#1]%");
            writer.println("  }");

            writer.print("  \\providecommand*");
            writer.println("{\\bibglssetwidesttoplevelfallback}[1]{%");
            writer.println("    \\glsFindWidestTopLevelName[#1]%");
            writer.println("  }");
            writer.println("}");

            writer.print("\\providecommand*");
            writer.println("{\\bibglssetwidestfortypefallback}[1]{%");
            writer.print("  \\apptoglossarypreamble[#1]");
            writer.println("{\\bibglssetwidestfallback{#1}}%");
            writer.println("}");

            writer.print("\\providecommand*");
            writer.println("{\\bibglssetwidesttoplevelfortypefallback}[1]{%");
            writer.print("  \\apptoglossarypreamble[#1]");
            writer.println("{\\bibglssetwidesttoplevelfallback{#1}}%");
            writer.println("}");
         }

         if (setWidest)
         {
            writeWidestNames(writer, font, frc);
            writer.println();
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

   public void writeBibGlsContributorDef(PrintWriter writer)
     throws IOException
   {
      writer.println("\\providecommand*{\\bibglscontributor}[4]{%");

      switch (contributorOrder)
      {
         case CONTRIBUTOR_ORDER_SURNAME:
           writer.print("  #3\\ifstrempty{#4}{}{, #4}");
           writer.print("\\ifstrempty{#1}{}{, #1}");
           writer.println("\\ifstrempty{#2}{}{, #2}%");
         break;
         case CONTRIBUTOR_ORDER_VON:
           writer.print("  \\ifstrempty{#2}{}{#2 }#3");
           writer.print("\\ifstrempty{#4}{}{, #4}");
           writer.println("\\ifstrempty{#1}{}{, #1}%");
         break;
         case CONTRIBUTOR_ORDER_FORENAMES:
           writer.print("  #1\\ifstrempty{#2}{}{ #2} #3");
           writer.println("\\ifstrempty{#4}{}{, #4}%");
         break;
      }

      writer.println("}");
   }

   private void writeBibEntryDef(PrintWriter writer, Bib2GlsEntry entry)
     throws IOException,Bib2GlsException
   {
      String id = entry.getId();

      if (bib2gls.isEntrySelected(id))
      {
         if (dupLabelSuffix == null)
         {
            bib2gls.warningMessage("warning.entry.already.defined",
             id, toString());
         }
         else
         {
            int i = 1;

            String suffix = dupLabelSuffix+i;

            while (bib2gls.isEntrySelected(id+suffix))
            {
               i++;
               suffix = dupLabelSuffix+i;
            }

            entry.setSuffix(suffix);
            id = entry.getId();
            bib2gls.selectedEntry(id);
         }
      }
      else
      {
         bib2gls.selectedEntry(id);
      }

      entry.writeBibEntry(writer);

      if (saveOriginalId != null && !bib2gls.isKnownField(saveOriginalId)
          && !bib2gls.isKnownSpecialField(saveOriginalId))
      {
         writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
           id, saveOriginalId, entry.getOriginalId());
      }

      if (saveLocList)
      {
         entry.writeLocList(writer);
      }

      if (getSavePrimaryLocationSetting() != SAVE_PRIMARY_LOCATION_OFF)
      {
         String val = entry.getPrimaryRecordList();

         if (val != null)
         {
            writer.format("\\GlsXtrSetField{%s}{primarylocations}{%s}%n",
              id, val);
         }
      }

      if (saveChildCount)
      {
         writer.format("\\GlsXtrSetField{%s}{childcount}{%d}%n",
           id, entry.getChildCount());

         String parentId = entry.getParent();

         if (parentId != null)
         {
            writer.format("\\glsxtrfieldlistadd{%s}{childlist}{%s}%n",
              parentId, id);
         }
      }

      if (saveOriginalEntryType)
      {
         writer.format("\\GlsXtrSetField{%s}{originalentrytype}{%s}%n",
           id, entry.getOriginalEntryType());
      }

      if (checkEndPunc != null)
      {
         for (String f : checkEndPunc)
         {
            String field  = f+"endpunc";

            String val = entry.getFieldValue(field);

            if (val != null)
            {
               writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
                 id, field, val);
            }
         }
      }

      if (bib2gls.isRecordCountSet())
      {
         bib2gls.writeRecordCount(id, writer);
      }

      if (indexCounter != null)
      {
         entry.writeIndexCounterField(writer);
      }

      entry.writeExtraFields(writer);
   }

   private void writeBibEntryCopy(PrintWriter writer, Bib2GlsEntry entry)
     throws IOException
   {
      String entryType = getType(entry);

      if (entryType == null)
      {
         // dual entry without dual-type set

         entryType = type;
      }

      writer.format("\\glsxtrcopytoglossary{%s}{%s}%n",
        entry.getId(), entryType);

      if (copyActionGroupField != null)
      {
         String value = entry.getFieldValue("group");

         if (value != null)
         {
            writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
             entry.getId(), copyActionGroupField, value);
         }
      }

      if (entry instanceof Bib2GlsDualEntry && 
           ((Bib2GlsDualEntry)entry).hasTertiary())
      {
         if (tertiaryType != null)
         {
            entryType = tertiaryType;
         }

         String id = entry.getOriginalId();

         if (tertiaryPrefix != null)
         {
            id = tertiaryPrefix+id;
         }

         writer.format("\\glsxtrcopytoglossary{%s}{%s}%n",
           id, entryType);

         if (copyActionGroupField != null)
         {
            String value = entry.getFieldValue("group");

            if (value != null)
            {
               writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
                id, copyActionGroupField, value);
            }
         }

      }
   }

   private void writeBibEntry(PrintWriter writer, Bib2GlsEntry entry)
     throws IOException,Bib2GlsException
   {
      switch (writeAction)
      {
         case WRITE_ACTION_DEFINE:
           writeBibEntryDef(writer, entry);
         break;
         case WRITE_ACTION_COPY:
           writeBibEntryCopy(writer, entry);
         break;
         case WRITE_ACTION_DEFINE_OR_COPY:

           writer.format("\\ifglsentryexists{%s}{%n", entry.getId());
           writeBibEntryCopy(writer, entry);
           writer.println("}");
           writer.println("{");

           writeBibEntryDef(writer, entry);

           if (copyActionGroupField != null)
           {
              String value = entry.getFieldValue("group");

              if (value != null)
              {
                 writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
                  entry.getId(), copyActionGroupField, value);
              }
           }

           writer.println("}");
      }

      writer.println();
   }

   private boolean writeGroupDefs(PrintWriter writer)
     throws IOException
   {
      boolean allowHyper = bib2gls.hyperrefLoaded()
                && bib2gls.createHyperGroupsOn();

      GroupTitle lastTitle = null;
      boolean isLastResource = bib2gls.isLastResource(this);

      for (Iterator<String> it = groupTitleMap.keySet().iterator();
          it.hasNext(); )
      {
         String key = it.next();

         GroupTitle groupTitle = groupTitleMap.get(key);

         if (isLastResource && 
              (lastTitle == null || lastTitle.getId() < groupTitle.getId()))
         {
            lastTitle = groupTitle;
         }

         if (groupTitle.getType() == null)
         {
            allowHyper = false;
         }

         writer.format("\\%s{%s}%n",
            groupTitle.getCsSetName(), groupTitle);
      }

      writer.println();

      if (lastTitle != null)
      {
         writer.format("\\bibglssetlastgrouptitle{\\%s}{%s}%n",
           lastTitle.getCsLabelName(), lastTitle);
      }

      return allowHyper;
   }

   private void writeCopyToGlossary(PrintWriter writer, Bib2GlsEntry entry)
     throws IOException
   {
      writer.format("\\glsxtrcopytoglossary{%s}{%s}%n",
                       entry.getId(), secondaryType);

      String secondaryGroup = entry.getFieldValue("secondarygroup");

      if (secondaryGroup != null)
      {
         writer.format("\\GlsXtrSetField{%s}{secondarygroup}{%s}%n",
            entry.getId(), secondaryGroup);
      }

      String secondarySort = entry.getFieldValue("sort");

      if (secondarySort != null)
      {
         writer.format("\\GlsXtrSetField{%s}{secondarysort}{%s}%n",
            entry.getId(), secondarySort);
      }

      writer.println();
   }

   private void writeWidestNames(PrintWriter writer, 
     Font font, FontRenderContext frc)
    throws IOException
   {
      if (widestNameNoType != null)
      {
         int maxDepth = 0;
         boolean fallbackRequired=false;

         StringBuilder builder = new StringBuilder();

         for (Iterator<Integer> levelIt = widestNameNoType.getKeyIterator(); 
                 levelIt.hasNext(); )
         {
            Integer levelKey = levelIt.next();

            if (levelKey > maxDepth)
            {
               maxDepth = levelKey;
            }
   
            WidestName value = widestNameNoType.get(levelKey);

            if (value.getName().isEmpty())
            {
               bib2gls.verboseMessage("warning.unknown.widest");

               fallbackRequired = true;
            }
            else
            {
               builder.append("\\bibglssetwidest");
               builder.append(String.format("{%d}{\\glsentryname{%s}}%n",
                  levelKey, value.getLabel()));
            }
         }

         if (fallbackRequired)
         {
            if (maxDepth == 0)
            {
               writer.println("\\bibglssetwidesttoplevelfallback{\\@glo@types}");
            }
            else
            {
               writer.format("\\bibglssetwidestfallback{\\@glo@types}");
            }
         }

         writer.println(builder);
      }

      if (widestNames != null)
      {
         for (Iterator<String> typeIt = widestNames.keySet().iterator();
              typeIt.hasNext(); )
         {
            String entryType = typeIt.next();

            WidestNameHierarchy hierarchy = widestNames.get(entryType);
   
            int maxDepth = 0;
            boolean fallbackRequired=false;

            StringBuilder builder = new StringBuilder();

            for (Iterator<Integer> levelIt = hierarchy.getKeyIterator(); 
                 levelIt.hasNext(); )
            {
               Integer levelKey = levelIt.next();

               if (levelKey > maxDepth)
               {
                  maxDepth = levelKey;
               }
   
               WidestName value = hierarchy.get(levelKey);
   
               if (value.getName().isEmpty())
               {
                  bib2gls.verboseMessage(
                    "warning.unknown.widest.fortype", levelKey, entryType);
   
                  fallbackRequired = true;
               }
               else
               {
                  builder.append("\\bibglssetwidestfortype");
                  builder.append(String.format("{%s}{%d}{\\glsentryname{%s}}%n",
                     entryType, levelKey, value.getLabel()));
               }
            }

            if (fallbackRequired)
            {
               if (maxDepth == 0)
               {
                  writer.format("\\bibglssetwidesttoplevelfortypefallback{%s}%n",
                    entryType);
               }
               else
               {
                  writer.format("\\bibglssetwidestfortypefallback{%s}%n",
                    entryType);
               }
            }

            writer.println(builder);
         }
      }

   }

   private void writeAllCapsCsDefs(PrintWriter writer)
      throws IOException
   {
      if (allCapsEntryField != null)
      {
         for (String field : allCapsEntryField)
         {
            writer.format("\\providecommand{\\GLSentry%s}[1]{%%%n", field);
            writer.format("  \\bibglsuppercase{\\glsentry%s{#1}}%%%n", field);
            writer.println("}");
         }
      }

      if (allCapsAccessField != null)
      {
         for (String field : allCapsAccessField)
         {
            writer.format("\\providecommand{\\GLSaccess%s}[1]{%%%n", field);
            writer.format("  \\bibglsuppercase{\\glsaccess%s{#1}}%%%n", field);
            writer.println("}");
         }
      }
   }

   private void writeLabelPrefixHooks(PrintWriter writer)
      throws IOException
   {
      // Identify label prefixes if required.
      writer.format("\\bibglstertiaryprefixlabel{%s}%n",
        tertiaryPrefix == null ? "" : tertiaryPrefix);
      writer.format("\\bibglsdualprefixlabel{%s}%n",
        dualPrefix == null ? "" : dualPrefix);
      writer.format("\\bibglsprimaryprefixlabel{%s}%n",
        labelPrefix == null ? "" : labelPrefix);

      if (externalPrefixes != null)
      {
         for (int i = 0; i < externalPrefixes.length; i++)
         {
            writer.format("\\bibglsexternalprefixlabel{%d}{%s}%n",
              i+1, externalPrefixes[i]);
         }
      }
   }

   private void updateWidestName(Bib2GlsEntry entry, 
     Font font, FontRenderContext frc)
   {
      updateWidestName(entry, null, font, frc);
   }

   private void updateWidestName(Bib2GlsEntry entry, String entryType,
     Font font, FontRenderContext frc)
   {
      if (entryType == null)
      {
         entryType = getType(entry);
      }
      else
      {
         entryType = getType(entry, entryType, false);
      }

      // This is just approximate as fonts, commands etc
      // will affect the width.

      String name = entry.getFieldValue("name");

      if (name == null || name.isEmpty()) return;

      bib2gls.logMessage(bib2gls.getMessage("message.calc.text.width",
        entry.getId()));

      if (name.matches("(?s).*[\\\\\\$\\{\\}].*"))
      {
         // Try to interpret any LaTeX code that may be in the name.
         // This assumes custom user commands are provided in the
         // preamble. Won't work on anything complicated and doesn't
         // take font changes into account.

         name = bib2gls.interpret(name, entry.getField("name"), true);
      }

      double w = 0.0;

      if (!name.isEmpty())
      {
         TextLayout layout = new TextLayout(name, font, frc);

         w = layout.getBounds().getWidth();
      }

      bib2gls.logMessage(bib2gls.getMessage("message.calc.text.width.result",
        name, w));

      if (entryType == null)
      {
         if (widestNameNoType == null)
         {
            widestNameNoType = new WidestNameHierarchy();
         }

         widestNameNoType.update(entry, name, w);
      }
      else
      {
         if (widestNames == null)
         {
            widestNames = new HashMap<String,WidestNameHierarchy>();
         }

         WidestNameHierarchy hierarchy = widestNames.get(entryType);

         if (hierarchy == null)
         {
            hierarchy = new WidestNameHierarchy();
            widestNames.put(entryType, hierarchy);
         }

         hierarchy.update(entry, name, w);
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
      // This has to be done first to ensure the parent-child
      // information is all correct.

      for (int i = 0, n = entries.size(); i < n; i++)
      {
         Bib2GlsEntry entry = entries.get(i);
         checkParent(entry, i, entries);
      }

      // This method will be called if saveChildCount == true
      // && flattenLonely != FLATTEN_LONELY_PRE_SORT
      // Don't need to go any further if flattenLonely == FLATTEN_LONELY_FALSE

      if (flattenLonely == FLATTEN_LONELY_FALSE)
      {
         return;
      }

      // The entries need to be traversed down the hierarchical
      // system otherwise moving a grandchild up the hierarchy
      // before the parent entry has been checked will confuse the
      // test.

      Hashtable<Integer,Vector<Bib2GlsEntry>> flattenMap 
      = new Hashtable<Integer,Vector<Bib2GlsEntry>>();

      // Key set needs to be ordered
      TreeSet<Integer> keys = new TreeSet<Integer>();

      Vector<Bib2GlsEntry> discardList = new Vector<Bib2GlsEntry>();

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
      if (triggerType != null && entry.hasTriggerRecord())
      {
         entry.putField("type", triggerType);
      }
      else if (progenitorType != null && entry.getFieldValue("progeny") != null)
      {
         String entryType = getType(entry, progenitorType, false);

         if (entryType != null)
         {
            entry.putField("type", entryType);
         }
      }
      else if (progenyType != null && entry.getFieldValue("progenitor") != null)
      {
         String entryType = getType(entry, progenyType, false);

         if (entryType != null)
         {
            entry.putField("type", entryType);
         }
      }
      else if (type != null)
      {
         String entryType = getType(entry, type, false);

         if (entryType != null)
         {
            entry.putField("type", entryType);
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
         String entryCategory = getCategory(entry, catLabel, false);

         if (entryCategory != null)
         {
            entry.putField("category", entryCategory);
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
      if (triggerType != null && dual.hasTriggerRecord())
      {
         dual.putField("type", triggerType);
      }
      else if (dualType != null)
      {
         String entryType = getType(dual, dualType, false);

         if (entryType != null)
         {
            dual.putField("type", entryType);
         }
      }
   }

   private void setDualCategory(Bib2GlsEntry dual)
   {
      if (dualCategory != null)
      {
         String entryCategory = getCategory(dual, dualCategory, false);

         if (entryCategory != null)
         {
            dual.putField("category", entryCategory);
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

   public byte getContributorOrder()
   {
      return contributorOrder;
   }

   public boolean isBibTeXAuthorField(String field)
   {
      if (bibtexAuthorList == null)
      {
         return false;
      }

      for (String f : bibtexAuthorList)
      {
         if (f.equals(field))
         {
            return true;
         }
      }

      return false;
   }

   public boolean isInterpretField(String field)
   {
      if (interpretFields == null)
      {
         return false;
      }

      for (String f : interpretFields)
      {
         if (f.equals(field))
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

   public String getTertiaryType()
   {
      return tertiaryType;
   }

   public String getTertiaryPrefix()
   {
      return tertiaryPrefix;
   }

   public String getTertiaryCategory()
   {
      return tertiaryCategory;
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

   public String getCsLabelPrefix()
   {
      return csLabelPrefix;
   }

   public String getDualSortField()
   {
      return dualSortSettings.getSortField();
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

   public HashMap<String,String> getDualAbbrevEntryMap()
   {
      return dualAbbrevEntryMap;
   }

   public HashMap<String,String> getDualIndexEntryMap()
   {
      return dualIndexEntryMap;
   }

   public HashMap<String,String> getDualIndexSymbolMap()
   {
      return dualIndexSymbolMap;
   }

   public HashMap<String,String> getDualIndexAbbrevMap()
   {
      return dualIndexAbbrevMap;
   }

   public String getFirstDualAbbrevMap()
   {
      return dualAbbrevFirstMap;
   }

   public String getFirstDualAbbrevEntryMap()
   {
      return dualAbbrevEntryFirstMap;
   }

   public String getFirstDualIndexEntryMap()
   {
      return dualIndexEntryFirstMap;
   }

   public String getFirstDualIndexSymbolMap()
   {
      return dualIndexSymbolFirstMap;
   }

   public String getFirstDualIndexAbbrevMap()
   {
      return dualIndexAbbrevFirstMap;
   }

   public boolean backLinkFirstDualAbbrevMap()
   {
      return backLinkDualAbbrev;
   }

   public boolean backLinkFirstDualAbbrevEntryMap()
   {
      return backLinkDualAbbrevEntry;
   }

   public boolean backLinkFirstDualIndexEntryMap()
   {
      return backLinkDualIndexEntry;
   }

   public boolean backLinkFirstDualIndexSymbolMap()
   {
      return backLinkDualIndexSymbol;
   }

   public boolean backLinkFirstDualIndexAbbrevMap()
   {
      return backLinkDualIndexAbbrev;
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

         if (!bib2gls.isKnownField(key) && !bib2gls.isKnownSpecialField(key))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.field", key, optName));
         }

         key = mapping.get(key);

         if (!bib2gls.isKnownField(key) && !bib2gls.isKnownSpecialField(key))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.field", key, optName));
         }
      }
   }

   // Allow for entries to be filtered out
   public boolean discard(Bib2GlsEntry entry)
   {
      if (fieldPatterns == null || matchAction != MATCH_ACTION_FILTER)
      {
         return false;
      }

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
         else if (field.equals(PATTERN_FIELD_ORIGINAL_ENTRY_TYPE))
         {
            value = entry.getOriginalEntryType();
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

   public byte getPostDescDot()
   {
      return postDescDot;
   }

   public boolean isStripTrailingNoPostOn()
   {
      return stripTrailingNoPost;
   }

   public boolean isLabelifyField(String field)
   {
      if (labelifyFields == null) return false;

      for (String f : labelifyFields)
      {
         if (f.equals(field)) return true;
      }

      return false;
   }

   public boolean isLabelifyListField(String field)
   {
      if (labelifyListFields == null) return false;

      for (String f : labelifyListFields)
      {
         if (f.equals(field)) return true;
      }

      return false;
   }

   public Vector<PatternReplace> getLabelifySubstitutions()
   {
      return labelifyReplaceMap;
   }

   public boolean isDependencyListField(String field)
   {
      if (dependencyListFields == null) return false;

      for (String f : dependencyListFields)
      {
         if (f.equals(field)) return true;
      }

      return false;
   }

   public boolean isCheckEndPuncOn()
   {
      return checkEndPunc != null;
   }

   public boolean isCheckEndPuncOn(String field)
   {
      if (checkEndPunc == null) return false;

      for (String f : checkEndPunc)
      {
         if (f.equals(field)) return true;
      }

      return false;
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

   public boolean changeLongCase()
   {
      return longCaseChange != null;
   }

   public boolean changeDualLongCase()
   {
      return dualLongCaseChange != null;
   }

   public HashMap<String,String> getFieldCaseOptions()
   {
      return fieldCaseChange;
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

   public BibValueList applyLongCaseChange(TeXParser parser, 
      BibValueList value)
    throws IOException
   {
      return applyCaseChange(parser, value, longCaseChange);
   }

   public BibValueList applyDualLongCaseChange(TeXParser parser,
      BibValueList value)
    throws IOException
   {
      return applyCaseChange(parser, value, dualLongCaseChange);
   }

   public static boolean isUcLcCommand(String csname)
   {
      return (csname.equals("O") || csname.equals("o")
             || csname.equals("L") || csname.equals("l")
             || csname.equals("AE") || csname.equals("ae") 
             || csname.equals("OE") || csname.equals("oe")
             || csname.equals("AA") || csname.equals("aa") 
             || csname.equals("SS") || csname.equals("ss")
             || csname.equals("NG") || csname.equals("ng") 
             || csname.equals("TH") || csname.equals("th")
             || csname.equals("DH") || csname.equals("dh")
             || csname.equals("DJ") || csname.equals("dj")
       );
   }

   public void toLowerCase(TeXObjectList list, TeXParserListener listener)
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
         else if (object instanceof MathGroup)
         {// skip
            continue;
         }
         else if (object instanceof TeXObjectList)
         {
            toLowerCase((TeXObjectList)object, listener);
         }
         else if (object instanceof ControlSequence)
         {
            String csname = ((ControlSequence)object).getName();

            if (csname.equals("protect")) continue;

            if (isUcLcCommand(csname))
            {
               list.set(i, new TeXCsRef(csname.toLowerCase()));
               continue;
            }

            int csIdx = i;

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
                 || csname.equals("si") || isNoCaseChangeCs(csname))
            {
               // skip argument
            }
            else if (csname.endsWith("ref"))
            {
               // Skip argument but check for star or optional
               // argument.

               if (object instanceof CharObject)
               {
                  int cp = ((CharObject)object).getCharCode();

                  if (cp == '*')
                  {
                     // is there an optional argument after '*'?

                     while (i < n)
                     {
                        object = list.get(i);

                        if (!(object instanceof Ignoreable))
                        {
                           break;
                        }

                        i++;
                     }

                     if (object instanceof CharObject)
                     {
                        cp = ((CharObject)object).getCharCode();
                     }
                  }

                  if (cp == '[')
                  {
                     while (i < n)
                     {
                        object = list.get(i);

                        if (!(object instanceof CharObject
                            && ((CharObject)object).getCharCode() == ']'))
                        {
                           break;
                        }

                        i++;
                     }
                  }
                  else
                  {
                     continue;
                  }
               }
               else
               {
                  continue;
               }

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
            else if (csname.equals("glsentrytitlecase"))
            {
               list.set(csIdx, new TeXCsRef("glsxtrusefield"));

               // skip next argument as well
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
            else if (csname.equals("glshyperlink"))
            {
               TeXObjectList subList = new TeXObjectList();

               // Convert contents of optional argument to lower
               // case, if present.

               if (object instanceof CharObject 
                    && ((CharObject)object).getCharCode() == '[')
               {
                  TeXObject obj = list.get(i);

                  while (!(obj instanceof CharObject 
                    && ((CharObject)object).getCharCode() == ']'))
                  {
                     subList.add(list.remove(i));

                     if (i >= list.size()) break;

                     obj = list.get(i);
                  }

                  toLowerCase(subList, listener);

                  list.addAll(i, subList);
               }
            }
            else
            {
               String lowerCsname = null;

               if (csname.matches("d?gls(disp|link)"))
               {
                  if (object instanceof CharObject 
                       && ((CharObject)object).getCharCode() == '[')
                  {
                     // skip optional argument
   
                     while (i < n)
                     {
                        object = list.get(i);
   
                        if (object instanceof CharObject 
                               && ((CharObject)object).getCharCode() == ']')
                        {
                           break;
                        }
   
                        i++;
                     }
   
                     // skip ignoreables following optional argument
   
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
   
                  // 'object' should now be label argument. Skip
                  // ignoreables to get text argument.
   
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
               else
               {
                  lowerCsname = getLowerCsName(csname);

                  if (lowerCsname == null)
                  {
                     // ignore this command

                     if (i > csIdx)
                     {
                        i--;
                     }
                  }
                  else
                  {
                     list.set(csIdx, new TeXCsRef(lowerCsname));
                  }
               }
            }
         }
      }
   }

   public void toUpperCase(TeXObjectList list, TeXParserListener listener)
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
            toUpperCase((TeXObjectList)object, listener);
         }
         else if (object instanceof ControlSequence)
         {
            String csname = ((ControlSequence)object).getName();

            if (csname.equals("protect")) continue;

            if (isUcLcCommand(csname))
            {
               list.set(i, new TeXCsRef(csname.toUpperCase()));
               continue;
            }

            int csIdx = i;

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
                 || csname.equals("si") || isNoCaseChangeCs(csname))
            {
               // skip argument

            }
            else if (csname.endsWith("ref"))
            {
               // Skip argument but check for star or optional
               // argument.

               if (object instanceof CharObject)
               {
                  int cp = ((CharObject)object).getCharCode();

                  if (cp == '*')
                  {
                     // is there an optional argument after '*'?

                     while (i < n)
                     {
                        object = list.get(i);

                        if (!(object instanceof Ignoreable))
                        {
                           break;
                        }

                        i++;
                     }

                     if (object instanceof CharObject)
                     {
                        cp = ((CharObject)object).getCharCode();
                     }
                  }

                  if (cp == '[')
                  {
                     while (i < n)
                     {
                        object = list.get(i);

                        if (!(object instanceof CharObject
                            && ((CharObject)object).getCharCode() == ']'))
                        {
                           break;
                        }

                        i++;
                     }
                  }
                  else
                  {
                     continue;
                  }
               }
               else
               {
                  continue;
               }

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
            else if (csname.equals("glsentrytitlecase"))
            { // skip next argument as well

               list.set(csIdx, new TeXCsRef("GLSxtrusefield"));

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
            else if (csname.equals("glshyperlink"))
            {
               TeXObjectList subList = new TeXObjectList();

               if (object instanceof CharObject 
                    && ((CharObject)object).getCharCode() == '[')
               {
                  TeXObject obj = list.get(i);

                  while (!(obj instanceof CharObject 
                    && ((CharObject)object).getCharCode() == ']'))
                  {
                     subList.add(list.remove(i));

                     if (i >= list.size()) break;

                     obj = list.get(i);
                  }

                  toUpperCase(subList, listener);

                  list.addAll(i, subList);
               }
               else
               {// no optional argument, need to add one
                  subList.add(listener.getOther('['));

                  subList.add(new TeXCsRef("bibglsuppercase"));

                  Group arg = listener.createGroup();
                  subList.add(arg);

                  arg.add(new TeXCsRef("glsentrytext"));
                  arg.add(new TeXCsRef("glslabel"));

                  subList.add(listener.getOther(']'));

                  list.addAll(i, subList);

               }

               i += subList.size();
               n = list.size();
            }
            else
            {
               String allcapsCSname = null;

               if (csname.matches("d?gls(disp|link)"))
               {
                  if (object instanceof CharObject 
                       && ((CharObject)object).getCharCode() == '[')
                  {
                     // skip optional argument
   
                     while (i < n)
                     {
                        object = list.get(i);
   
                        if (object instanceof CharObject 
                               && ((CharObject)object).getCharCode() == ']')
                        {
                           break;
                        }
   
                        i++;
                     }
   
                     // skip ignoreables following optional argument
   
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
   
                  // 'object' should now be label argument. Skip
                  // ignoreables to get text argument.
   
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
               else if (csname.matches("glsp[st]"))
               {
                  // need to replace \glsps{label} with \GLSxtrp{short}{label}
                  // and \glspt{label} with \GLSxtrp{text}{label}
   
                  list.set(csIdx, new TeXCsRef("GLSxtrp"));
   
                  list.add(i, listener.createGroup(
                   csname.endsWith("s") ? "short" : "text"));

                  n = list.size();

                  // skip next argument
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
               else
               {
                  allcapsCSname = getAllCapsCsName(csname);

                  if (allcapsCSname == null)
                  {
                     // ignore this command

                     if (i > csIdx)
                     {
                        i--;
                     }
                  }
                  else
                  {
                     list.set(csIdx, new TeXCsRef(allcapsCSname));
                  }
               }
            }
         }
      }
   }

   public void toSentenceCase(TeXObjectList list, TeXParserListener listener)
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

            toUpperCase((TeXObjectList)object, listener);

            return;
         }
         else if (object instanceof ControlSequence)
         {
            String csname = ((ControlSequence)object).getName();

            if (csname.equals("protect")) continue;

            if (isUcLcCommand(csname))
            {
               list.set(i, new TeXCsRef(csname.toUpperCase()));
               return;
            }

            int csIdx = i;

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

            if (csname.equals("ensuremath") || csname.equals("si") 
                 || csname.endsWith("ref") || isNoCaseChangeCs(csname))
            {
               return;
            }

            if (csname.equals("NoCaseChange"))
            {
               // object should now be the argument, which should be
               // skipped.

               continue;
            }

            if (csname.equals("glsentrytitlecase"))
            {
               list.set(csIdx, new TeXCsRef("Glsxtrusefield"));

               return;
            }

            if (csname.equals("glshyperlink"))
            {
               TeXObjectList subList = new TeXObjectList();

               if (object instanceof CharObject 
                    && ((CharObject)object).getCharCode() == '[')
               {
                  TeXObject obj = list.get(i);

                  while (!(obj instanceof CharObject 
                    && ((CharObject)object).getCharCode() == ']'))
                  {
                     subList.add(list.remove(i));

                     if (i >= list.size()) break;

                     obj = list.get(i);
                  }

                  toSentenceCase(subList, listener);
               }
               else
               {// no optional argument, need to add one
                  subList.add(listener.getOther('['));
                  subList.add(new TeXCsRef("Glsentrytext"));
                  subList.add(new TeXCsRef("glslabel"));
                  subList.add(listener.getOther(']'));
               }

               list.addAll(i, subList);

               return;
            }

            String sentenceCsname = null;

            if (csname.matches("d?gls(disp|link)"))
            {
               if (object instanceof CharObject 
                    && ((CharObject)object).getCharCode() == '[')
               {
                  // skip optional argument

                  while (i < n)
                  {
                     object = list.get(i);

                     if (object instanceof CharObject 
                            && ((CharObject)object).getCharCode() == ']')
                     {
                        break;
                     }

                     i++;
                  }

                  // skip ignoreables following optional argument

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

               // 'object' should now be label argument. Skip
               // ignoreables to get text argument.

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
            else if (csname.matches("glsp[st]"))
            {
               // need to replace \glsps{label} with \Glsxtrp{short}{label}
               // and \glspt{label} with \Glsxtrp{text}{label}

               list.set(csIdx, new TeXCsRef("Glsxtrp"));

               list.add(i, listener.createGroup(
                csname.endsWith("s") ? "short" : "text"));

               return;
            }
            else
            {
               sentenceCsname = getSentenceCsName(csname);
            }

            if (sentenceCsname == null)
            {
               // if a group follows the command, title case the group
               // otherwise finish.

               if (object instanceof Group && !(object instanceof MathGroup))
               {
                  // title case argument

                  toSentenceCase((TeXObjectList)object, listener);
               }
            }
            else
            {
               list.set(csIdx, new TeXCsRef(sentenceCsname));
            }

            return;
         }
      }
   }

   public boolean isNoCaseChangeCs(String csname)
   {
      if (noCaseChangeCs == null) return false;

      for (String cs : noCaseChangeCs)
      {
         if (cs.equals(csname))
         {
            return true;
         }
      }

      return false;
   }

   public boolean isWordBoundary(TeXObject object)
   {
      int codePoint = -1;
      String csname = null;

      if (object instanceof CharObject)
      {
         codePoint = ((CharObject)object).getCharCode();
      }

      if (object instanceof ControlSequence)
      {
         csname = ((ControlSequence)object).getName();
      }

      if (wordBoundarySpace)
      {
         if (object instanceof WhiteSpace 
           || (codePoint != -1 && Character.isWhitespace(codePoint)))
         {
            return true;
         }
      }

      if (wordBoundaryNbsp)
      {
         if (object instanceof Nbsp || codePoint == 0x00A0
            || codePoint == 0x2007 || codePoint == 0x202F)
         {
            return true;
         }
      }

      if (wordBoundaryCsSpace)
      {
         if (csname != null)
         {
            if (csname.equals("space") || csname.equals(" "))
            {
               return true;
            }
         }
      }

      if (wordBoundaryDash)
      {
         if (codePoint != -1 && 
              Character.getType(codePoint) == Character.DASH_PUNCTUATION)
         {
            return true;
         }

         if (csname != null)
         {
            if (csname.equals("textemdash") || csname.equals("textendash"))
            {
               return true;
            }
         }
      }

      return false;
   }

   protected boolean isWordException(TeXObjectList list, int index,
     TeXObjectList wordExceptions)
   {
      if (wordExceptions == null || wordExceptions.size() == 0)
      {
         return false;
      }

      int endIndex = list.size();

      // find the word boundary

      for (int i = index; i < endIndex; i++)
      {
         TeXObject object = list.get(i);

         if (isWordBoundary(object))
         {
            endIndex = i;
         }
      }

      int wordLength = endIndex-index;

      for (TeXObject wordExc : wordExceptions)
      {
         TeXObjectList tokenList;

         if (wordExc instanceof TeXObjectList)
         {
            tokenList = (TeXObjectList)wordExc;
         }
         else
         {
            tokenList = new TeXObjectList();
            tokenList.add(wordExc);
         }

         if (wordLength != tokenList.size())
         {
            continue;
         }

         boolean match = true;

         for (int i = 0; i < wordLength; i++)
         {
            TeXObject obj1 = list.get(index+i);
            TeXObject obj2 = tokenList.get(i);

            if (!(
                   (obj1 instanceof Ignoreable && obj2 instanceof Ignoreable)
                || (   obj1 instanceof ControlSequence 
                    && obj2 instanceof ControlSequence
                    && ((ControlSequence)obj1).getName().equals(
                        ((ControlSequence)obj2).getName())
                   )
                || obj1.equals(obj2)
               ))
            {
               match = false;
               break;
            }
         }

         if (match) return true;
      }

      return false;
   }

   public void toTitleCase(TeXObjectList list, TeXParserListener listener)
   {
      toTitleCase(list, listener, 0, bib2gls.getWordExceptionList());
   }

   private int toTitleCase(TeXObjectList list, TeXParserListener listener,
     int wordCount, TeXObjectList wordExceptions)
   {
      boolean wordBoundary = true;

      for (int i = 0, n = list.size(); i < n; i++)
      {
         TeXObject object = list.get(i);

         boolean prevWordBoundary = wordBoundary;

         wordBoundary = isWordBoundary(object);

         if (wordBoundary || !prevWordBoundary)
         {
            continue;
         }

         if (object instanceof CharObject)
         {
            int codePoint = ((CharObject)object).getCharCode();

            if (Character.isAlphabetic(codePoint))
            {
               wordCount++;

               if (wordCount == 1 || !isWordException(list, i, wordExceptions))
               {
                  codePoint = Character.toTitleCase(codePoint);
                  ((CharObject)object).setCharCode(codePoint);
               }
            }
            else
            {
               int charType = Character.getType(codePoint);

               if (codePoint == '`' || codePoint == '\''
                   || charType == Character.INITIAL_QUOTE_PUNCTUATION
                   || charType == Character.START_PUNCTUATION)
               {
                  wordBoundary = prevWordBoundary;
               }
            }
         }
         else if (object instanceof MathGroup)
         {
            // don't try converting $...$ or \[...\]
            wordCount++;
            continue;
         }
         else if (object instanceof Group)
         {
            // upper case entire group contents

            wordCount++;
            toUpperCase((TeXObjectList)object, listener);
         }
         else if (object instanceof ControlSequence)
         {
            String csname = ((ControlSequence)object).getName();

            if (csname.equals("protect"))
            {
               // skip \protect
               wordBoundary = prevWordBoundary;
               continue;
            }

            if (isUcLcCommand(csname))
            {
               wordCount++;

               if (wordCount > 1 && !isWordException(list, i, wordExceptions))
               {
                  list.set(i, new TeXCsRef(csname.toUpperCase()));
               }

               continue;
            }

            int csIdx = i;

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

            if (csname.equals("NoCaseChange"))
            {
               // skip \NoCaseChange and its argument
               wordBoundary = prevWordBoundary;
               continue;
            }

            if (csname.equals("ensuremath") || csname.equals("si") 
                || isNoCaseChangeCs(csname))
            {// no case-change
               wordCount++;
               continue;
            }
            else if (csname.endsWith("ref"))
            {// No case-change, but check for star or optional argument.
             // This won't work for some cases, but will work for
             // the standard \ref{label}, \pageref{label}, and for
             // hyperref's \ref*{label}, \pageref*{label}, \autoref{label} and
             // \autopageref{label}

               wordCount++;

               if (object instanceof CharObject)
               {
                  int cp = ((CharObject)object).getCharCode();

                  if (cp == '*')
                  {
                     // is there an optional argument after '*'?

                     while (i < n)
                     {
                        object = list.get(i);

                        if (!(object instanceof Ignoreable))
                        {
                           break;
                        }

                        i++;
                     }

                     if (object instanceof CharObject)
                     {
                        cp = ((CharObject)object).getCharCode();
                     }
                  }

                  if (cp == '[')
                  {
                     while (i < n)
                     {
                        object = list.get(i);

                        if (!(object instanceof CharObject
                            && ((CharObject)object).getCharCode() == ']'))
                        {
                           break;
                        }

                        i++;
                     }
                  }
                  else
                  {
                     continue;
                  }
               }
               else
               {
                  continue;
               }

               while (i < n)
               {
                  object = list.get(i);

                  if (!(object instanceof Ignoreable))
                  {
                     break;
                  }

                  i++;
               }

               continue;
            }
            else if (csname.equals("glsentrytitlecase"))
            { // skip next argument as well

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

               wordCount++;
               continue;
            }

            if (csname.equals("glshyperlink"))
            {
               TeXObjectList subList = new TeXObjectList();

               if (object instanceof CharObject 
                    && ((CharObject)object).getCharCode() == '[')
               {
                  TeXObject obj = list.get(i);

                  while (!(obj instanceof CharObject 
                    && ((CharObject)object).getCharCode() == ']'))
                  {
                     subList.add(list.remove(i));

                     if (i >= list.size()) break;

                     obj = list.get(i);
                  }

                  wordCount = toTitleCase(subList, listener, wordCount,
                    wordExceptions);
               }
               else
               {// no optional argument, need to add one
                  subList.add(listener.getOther('['));
                  subList.add(new TeXCsRef("glsentrytitlecase"));
                  subList.add(new TeXCsRef("glslabel"));
                  subList.add(listener.createGroup("text"));
                  subList.add(listener.getOther(']'));

                  wordCount++;
               }

               list.addAll(i, subList);

               i += subList.size();
               n = list.size();

               continue;
            }

            if (csname.matches("d?gls(disp|link)"))
            {
               if (object instanceof CharObject 
                    && ((CharObject)object).getCharCode() == '[')
               {
                  // skip optional argument

                  i++;
                  while (i < n)
                  {
                     object = list.get(i);

                     i++;

                     if (object instanceof CharObject 
                            && ((CharObject)object).getCharCode() == ']')
                     {
                        break;
                     }
                  }

                  // skip ignoreables following optional argument

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

               // 'object' should now be label argument. Skip
               // ignoreables to get text argument.

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

               if (object instanceof Group)
               {
                  // case-change argument
                  wordCount = toTitleCase((TeXObjectList)object, listener, 
                     wordCount, wordExceptions);
               }
               else
               {
                  // leave for the next iteration
                  wordBoundary = prevWordBoundary;
                  i--;
               }

               continue;
            }
            else if (csname.matches("glsp[st]"))
            {
               // need to replace \glsps{label} with \Glsxtrp{short}{label}
               // and \glspt{label} with \Glsxtrp{text}{label}

               list.set(csIdx, new TeXCsRef("Glsxtrp"));

               list.add(i, listener.createGroup(
                csname.endsWith("s") ? "short" : "text"));

               i++;
               n = list.size();

               // skip label
               while (i < n)
               {
                  object = list.get(i);

                  if (!(object instanceof Ignoreable))
                  {
                     break;
                  }

                  i++;
               }

               wordCount++;
               continue;
            }

            wordCount++;
            Matcher m = PATTERN_FIELD_CS.matcher(csname);

            if (m.matches())
            {
               String tail = m.group(1);

               if (bib2gls.isKnownField(tail))
               {
                  if (csname.startsWith("glsentry"))
                  {
                     // convert \glsentry<field>{label} 
                     // to \glsentrytitlecase{label}{field}

                     list.set(csIdx, new TeXCsRef("glsentrytitlecase"));

                     i++;
                     list.add(i, listener.createGroup(tail));

                     n = list.size();

                     continue;
                  }
                  else if (csname.equals("gls"+tail))
                  {
                     // convert \gls<field>[...]{label} 
                     // to
                     // \glslink>[...]{label}{\glsentrytitlecase{label}{field}}

                     list.set(csIdx, new TeXCsRef("glslink"));

                     if (object instanceof CharObject 
                          && ((CharObject)object).getCharCode() == '[')
                     {
                        // skip optional argument

                        i++;
                        while (i < n)
                        {
                           object = list.get(i);
                           i++;

                           if (object instanceof CharObject 
                                  && ((CharObject)object).getCharCode() == ']')
                           {
                              object = list.get(i);
                              break;
                           }
                        }
                     }

                     // object should now be label

                     TeXObject labelObj = (TeXObject)object.clone();
                     i++;

                     // skip label
                     while (i < n)
                     {
                        object = list.get(i);

                        if (!(object instanceof Ignoreable))
                        {
                           break;
                        }

                        i++;
                     }

                     Group grp = listener.createGroup();
                     list.add(i, grp);

                     grp.add(new TeXCsRef("glsentrytitlecase"));
                     grp.add(labelObj);
                     grp.add(listener.createGroup(tail));

                     i++;
                     n = list.size();

                     continue;
                  }
               }
            }

            String titleCsname = getTitleCsName(csname);

            if (titleCsname == null)
            {
               // if a group follows the command, title case the group
               // otherwise no case-change.

               if (object instanceof Group && !(object instanceof MathGroup))
               {
                  // title case argument

                  wordCount = toTitleCase((TeXObjectList)object, listener, 
                    wordCount-1, wordExceptions);
               }
            }
            else
            {
               list.set(csIdx, new TeXCsRef(titleCsname));
            }
         }
      }

      return wordCount;
   }

   public String getSentenceCsName(String csname)
   {
      if (csname.matches("gls(pl|xtrshort|xtrlong|xtrfull|xtrp)?"))
      {
         return "G"+csname.substring(1);
      }

      if (csname.matches("acr(short|full|long)(pl)?")
        || (bib2gls.checkAcroShortcuts() 
           && csname.matches("ac(sp?|lp?|fp?)?"))
        || (bib2gls.checkAbbrvShortcuts() 
           && csname.matches("a[bslf]p?")))
      {
         return "A"+csname.substring(1);
      }

      if (csname.matches("[rdpc]gls(pl)?"))
      {
         return String.format("%cG%s", csname.charAt(0), csname.substring(2));
      }

      Matcher m = PATTERN_FIELD_CS.matcher(csname);

      if (m.matches())
      {
         String tail = m.group(1);

         if (bib2gls.isKnownField(tail) || tail.matches("full(pl)?"))
         {
            return "G"+csname.substring(1);
         }
      }

      if (bib2gls.isGlsLike(csname))
      {
         int cp = csname.codePointAt(0);

         int i = Character.charCount(cp);

         String str = new String(Character.toChars(Character.toUpperCase(cp)));

         if (i < csname.length())
         {
            str = String.format("%s%s", str, csname.substring(i));
         }

         if (bib2gls.isGlsLike(str))
         {
            return str;
         }
      }

      return null;
   }

   // This is harder than getSentenceCsName and this method will have to
   // fallback on that method in most cases.
   public String getTitleCsName(String csname)
   {
      if (csname.toLowerCase().equals("glsxtrusefield"))
      {
         return "glsentrytitlecase";
      }

      return getSentenceCsName(csname);
   }

   public String getLowerCsName(String csname)
   {
      if (csname.matches("(Gls|GLS)(pl|xtrshort|xtrlong|xtrfull|xtrp)?")
        || csname.matches("[rdpc](Gls|GLS)(pl)?")
        || csname.matches("(Acr|ACR)(short|full|long)(pl)?")
        || (bib2gls.checkAcroShortcuts() 
           && (csname.matches("Ac(sp?|lp?|fp?)?")
               || csname.matches("AC(SP?|LP?|FP?)?")))
        || (bib2gls.checkAbbrvShortcuts() 
           && (csname.matches("A[bslf]p?") || csname.matches("A[BSLF]P?"))))
      {
         return csname.toLowerCase();
      }

      Matcher m = PATTERN_FIELD_CAP_CS.matcher(csname);

      if (m.matches())
      {
         String tail = m.group(1);

         if (bib2gls.isKnownField(tail) || tail.matches("full(pl)?"))
         {
            return csname.toLowerCase();
         }
      }

      if (bib2gls.isGlsLike(csname))
      {
         String str = csname.toLowerCase();

         if (bib2gls.isGlsLike(str))
         {
            return str;
         }
      }

      return null;
   }

   public String getAllCapsCsName(String csname)
   {
      csname = csname.toLowerCase();

      if (csname.matches("gls(pl|xtrshort|xtrlong|xtrfull|xtrp)?"))
      {
         return "GLS"+csname.substring(3);
      }

      if (csname.matches("acr(short|full|long)(pl)?"))
      {
         return "ACR"+csname.substring(3);
      }

      if ((bib2gls.checkAcroShortcuts() 
           && csname.matches("ac(sp?|lp?|fp?)?"))
        || (bib2gls.checkAbbrvShortcuts() 
           && csname.matches("a[bslf]p?")))
      {
         return csname.toUpperCase();
      }

      if (csname.matches("[rdpc]gls(pl)?"))
      {
         return String.format("%cGLS%s", csname.charAt(0), csname.substring(4));
      }

      Matcher m = PATTERN_FIELD_CS.matcher(csname);

      if (m.matches())
      {
         String tail = m.group(1);

         boolean isKnown = bib2gls.isKnownField(tail);

         if (isKnown || tail.matches("full(pl)?"))
         {
            // This can result in commands like \GLSentrytext, which
            // aren't provided. The simplest method is to just
            // provide these commands in the .glstex file.

            if (isKnown)
            {
               if (csname.startsWith("glsentry"))
               {
                  provideAllCapsEntryCs(tail);
               }
               else if (csname.startsWith("glsaccess"))
               {
                  provideAllCapsAccessCs(tail);
               }
            }

            return "GLS"+csname.substring(3);
         }
      }

      if (bib2gls.isGlsLike(csname))
      {
         String str = csname.toUpperCase();

         if (bib2gls.isGlsLike(str))
         {
            return str;
         }

         if (csname.endsWith("pl"))
         {
            str = csname.substring(0, csname.length()-2).toUpperCase()+"pl";

            if (bib2gls.isGlsLike(str))
            {
               return str;
            }
         }
      }

      return null;
   }

   private void provideAllCapsEntryCs(String field)
   {
      if (allCapsEntryField == null)
      {
         allCapsEntryField = new Vector<String>();
      }

      if (!allCapsEntryField.contains(field))
      {
         allCapsEntryField.add(field);
      }
   }

   private void provideAllCapsAccessCs(String field)
   {
      if (allCapsAccessField == null)
      {
         allCapsAccessField = new Vector<String>();
      }

      if (!allCapsAccessField.contains(field))
      {
         allCapsAccessField.add(field);
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
         list.add(new TeXCsRef("bibglslowercase"));
         list.add(grp);
      }
      else if (change.equals("uc-cs"))
      {
         Group grp = parser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("bibglsuppercase"));
         list.add(grp);
      }
      else if (change.equals("firstuc-cs"))
      {
         Group grp = parser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("bibglsfirstuc"));
         list.add(grp);
      }
      else if (change.equals("title-cs"))
      {
         Group grp = parser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("bibglstitlecase"));
         list.add(grp);
      }
      else if (change.equals("lc"))
      {
         toLowerCase(list, parser.getListener());
      }
      else if (change.equals("uc"))
      {
         toUpperCase(list, parser.getListener());
      }
      else if (change.equals("firstuc"))
      {
         toSentenceCase(list, parser.getListener());
      }
      else if (change.equals("title"))
      {
         toTitleCase(list, parser.getListener());
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
      String value = entry.getFieldValue("type");

      if (value != null)
      {
         return value;
      }

      value = type;

      if (entry instanceof Bib2GlsDualEntry
          && !(((Bib2GlsDualEntry)entry).isPrimary()))
      {
         value = dualType;
      }
      else if (entry.getFieldValue("progeny") != null)
      {
         value = progenitorType;
      }
      else if (entry.getFieldValue("progenitor") != null)
      {
         value = progenyType;
      }

      return getType(entry, value, false);
   }

   public String getType(Bib2GlsEntry entry, String fallback)
   {
      return getType(entry, fallback, true);
   }

   public String getType(Bib2GlsEntry entry, String fallback, 
      boolean checkField)
   {
      String value = null;

      if (checkField)
      {
         value = entry.getFieldValue("type");

         if (value != null)
         {
            return value;
         }
      }

      value = fallback;

      if (value == null)
      {
         return null;
      }

      if (value.equals("same as category"))
      {
         if (entry instanceof Bib2GlsDualEntry  
              && !((Bib2GlsDualEntry)entry).isPrimary())
         {
            value = dualCategory;
         }
         else
         {
            value = category;
         }

         if ("same as type".equals(value))
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.cyclic.sameas.type.category"));
         }

         return getCategory(entry, value);
      }

      if (value.equals("same as entry"))
      {
         return entry.getEntryType();
      }

      if (value.equals("same as original entry"))
      {
         return entry.getOriginalEntryType();
      }

      if (value.equals("same as base"))
      {
         return entry.getBase();
      }

      if (value.equals("same as parent"))
      {
         String parentId = entry.getParent();

         if (parentId == null) return null;

         Bib2GlsEntry parentEntry = getEntry(parentId);

         if (parentEntry == null) return null;

         return getType(parentEntry);
      }

      if (value.equals("same as primary") 
          && (entry instanceof Bib2GlsDualEntry)
          && !((Bib2GlsDualEntry)entry).isPrimary())
      {
         Bib2GlsEntry dual = entry.getDual();

         if (dual != null)
         {
            return getType(dual, fallback);
         }
      }

      return value;
   }

   public String getCategory(Bib2GlsEntry entry)
   {
      String value = entry.getFieldValue("category");

      if (value != null)
      {
         return value;
      }

      value = category;

      if (entry instanceof Bib2GlsDualEntry
          && !(((Bib2GlsDualEntry)entry).isPrimary()))
      {
         value = dualCategory;
      }

      return getCategory(entry, value, false);
   }

   public String getCategory(Bib2GlsEntry entry, String fallback)
   {
      return getCategory(entry, fallback, true);
   }

   public String getCategory(Bib2GlsEntry entry, String fallback,
     boolean checkField)
   {
      String value = null;

      if (checkField)
      {
         value = entry.getFieldValue("category");

         if (value != null)
         {
            return value;
         }
      }

      value = fallback;

      if (value == null)
      {
         return null;
      }

      if (value.equals("same as type"))
      {
         if (entry instanceof Bib2GlsDualEntry  
              && !((Bib2GlsDualEntry)entry).isPrimary())
         {
            value = dualType;
         }
         else
         {
            value = type;
         }

         if ("same as category".equals(value))
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.cyclic.sameas.type.category"));
         }

         return getType(entry, value);
      }

      if (value.equals("same as entry"))
      {
         return entry.getEntryType();
      }

      if (value.equals("same as original entry"))
      {
         return entry.getOriginalEntryType();
      }

      if (value.equals("same as base"))
      {
         return entry.getBase();
      }

      if (value.equals("same as primary") 
          && (entry instanceof Bib2GlsDualEntry)
          && !((Bib2GlsDualEntry)entry).isPrimary())
      {
         Bib2GlsEntry dual = entry.getDual();

         if (dual != null)
         {
            return getCategory(dual, fallback);
         }
      }

      return value;
   }

   private void writeHyperGroupDef(Bib2GlsEntry entry, PrintWriter writer)
     throws IOException
   {
      String key = entry.getGroupId();

      if (key == null)
      {
         bib2gls.debug("writeHyperGroupDef: No group ID for entry "
            +entry.getId());
         return;
      }

      GroupTitle groupTitle = groupTitleMap.get(key);

      if (groupTitle == null)
      {
         bib2gls.debug("writeHyperGroupDef: No group found for "+key);
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

   public GroupTitle getGroupTitle(String entryType, long id)
   {
      if (groupTitleMap != null)
      {
         return groupTitleMap.get(GroupTitle.getKey(entryType, id));
      }

      return null;
   }

   public String mapEntryType(String entryType)
   {
      if (entryTypeAliases == null)
      {
         return entryType;
      }

      String val = entryTypeAliases.get(entryType);

      return val == null ? entryType : val;
   }

   public String getUnknownEntryMap()
   {
      return unknownEntryMap;
   }

   public String getSaveOriginalIdField()
   {
      return saveOriginalId;
   }

   public boolean hasFieldAliases()
   {
      return fieldAliases != null;
   }

   public Iterator<String> getFieldAliasesIterator()
   {
      return fieldAliases.keySet().iterator();
   }

   public String getFieldAlias(String fieldName)
   {
      return fieldAliases.get(fieldName);
   }

   public String getOriginalField(String mappedName)
   {
      if (hasFieldAliases())
      {
         for (Iterator<String> it = getFieldAliasesIterator(); it.hasNext(); )
         {
            String key = it.next();

            if (mappedName.equals(fieldAliases.get(key)))
            {
               return key;
            }
         }
      }

      if (hasFieldCopies())
      {
         for (Iterator<String> it = getFieldCopiesIterator();
              it.hasNext(); )
         {
            String key = it.next();

            Vector<String> list = getFieldCopy(key);

            if (list != null && list.contains(mappedName))
            {
               return key;
            }
         }
      }

      return null;
   }

   public boolean isReplicateOverrideOn()
   {
      return replicateOverride;
   }

   public byte getFallbackOnMissingReplicateAction()
   {
      return missingFieldReplicateAction;
   }

   public boolean hasFieldCopies()
   {
      return fieldCopies != null;
   }

   public Iterator<String> getFieldCopiesIterator()
   {
      return fieldCopies.keySet().iterator();
   }

   public Vector<String> getFieldCopy(String fieldName)
   {
      return fieldCopies.get(fieldName);
   }

   public boolean isCombineDualLocationsOn()
   {
      return combineDualLocations != COMBINE_DUAL_LOCATIONS_OFF;
   }

   public int getCombineDualLocations()
   {
      return combineDualLocations;
   }

   public String getCategory()
   {
      return category;
   }

   public String getDualCategory()
   {
      return dualCategory;
   }

   public String getAbbrevDefaultNameField()
   {
      return abbrevDefaultNameField;
   }

   public String getAbbrevDefaultSortField()
   {
      return abbrevDefaultSortField;
   }

   public String getEntryDefaultSortField()
   {
      return entryDefaultSortField;
   }

   public String getSymbolDefaultSortField()
   {
      return symbolDefaultSortField;
   }

   public String getBibTeXEntryDefaultSortField()
   {
      return bibTeXEntryDefaultSortField;
   }

   public boolean useNonBreakSpace()
   {
      return bib2gls.useNonBreakSpace();
   }

   public boolean useInterpreter()
   {
      return bib2gls.useInterpreter();
   }

   public String interpret(String texCode, BibValueList bibVal, boolean trim)
   {
      return bib2gls.interpret(texCode, bibVal, trim);
   }

   public boolean isInterpretLabelFieldsEnabled()
   {
      return interpretLabelFields && bib2gls.useInterpreter();
   }

   // Cross-resource references aren't permitted if the resource set
   // has a preamble that's interpreted
   //     (preamble != null && interpretPreamble)

   public boolean allowsCrossResourceRefs()
   {
      return preamble == null || !interpretPreamble;
   }

   public boolean isLabelifyEnabled()
   {
      if (labelifyFields == null || labelifyFields.length == 0)
      {
         return true;
      }

      if (labelifyListFields == null || labelifyListFields.length == 0)
      {
         return true;
      }

      return false;
   }

   public boolean isStripMissingParentsEnabled()
   {
      return stripMissingParents;
   }

   public boolean isCreateMissingParentsEnabled()
   {
      return createMissingParents;
   }

   public boolean isCopyAliasToSeeEnabled()
   {
      return copyAliasToSee;
   }

   public int getMinLocationRange()
   {
      return minLocationRange;
   }

   public String getSuffixF()
   {
      return suffixF;
   }

   public String getSuffixFF()
   {
      return suffixFF;
   }

   public int getSeeLocation()
   {
      return seeLocation;
   }

   public int getSeeAlsoLocation()
   {
      return seeAlsoLocation;
   }

   public int getAliasLocation()
   {
      return aliasLocation;
   }

   public boolean showLocationPrefix()
   {
      return locationPrefix != null;
   }

   public boolean showLocationSuffix()
   {
      return locationSuffix != null;
   }

   public int getLocationGap()
   {
      return locGap;
   }

   public String getSaveIndexCounter()
   {
      return indexCounter;
   }

   public Vector<TeXPath> getSupplementalPaths()
   {
      return supplementalPdfPaths;
   }

   public boolean isPrimaryLocation(String format)
   {
      if (primaryLocationFormats == null 
           || savePrimaryLocations == SAVE_PRIMARY_LOCATION_OFF)
      {
         return false;
      }

      if (format.startsWith("(") || format.startsWith(")"))
      {
         format = format.substring(1);
      }

      for (String primaryFmt : primaryLocationFormats)
      {
         if (primaryFmt.equals(format))
         {
            return true;
         }
      }

      return false;
   }

   public int getSavePrimaryLocationSetting()
   {
      return savePrimaryLocations;
   }

   class LabelListSortMethod
   {
      public LabelListSortMethod(String[] fields, String sortMethod, 
        String csname)
      {
         this.fields = fields;
         this.method = sortMethod;
         this.csname = csname;
      }

      public void sort(Vector<Bib2GlsEntry> entries, SortSettings baseSettings)
       throws Bib2GlsException,IOException
      {
         if (bib2gls.getVerboseLevel() > 0)
         {
            if (fields.length == 1)
            {
               bib2gls.verboseMessage("message.sort.labels", method,
                 fields.length, fields[0]);
            }
            else
            {
               StringBuilder builder = new StringBuilder(fields[0]);

               for (int i = 1; i < fields.length; i++)
               {
                  builder.append(", ");
                  builder.append(fields[i]);
               }

               bib2gls.verboseMessage("message.sort.labels", method,
                 fields.length, builder);
            }
         }

         SortSettings settings = baseSettings.copy(method, "name");

         TeXParser bibParser = bibParserListener.getParser();

         for (Bib2GlsEntry entry : entries)
         {
            if (hasField(entry))
            {
               for (String field : fields)
               {
                  BibValueList val = entry.getField(field);

                  if (val != null)
                  {
                     TeXObjectList labelList = val.expand(bibParser);
                     CsvList csvList = CsvList.getList(bibParser, labelList);

                     if (csvList.size() > 1)
                     {
                        if (settings.isOrderOfRecords())
                        {
                           orderByUse(bibParser, entry, csvList, field,
                            settings.isReverse());
                        }
                        else
                        {
                           sort(bibParser, entry, csvList, settings, field);
                        }
                     }
                  }
               }
            }
         }
      }

      private void insertByUse(String element, TreeSet<GlsRecord> set)
      {
         Vector<GlsRecord> allRecords = bib2gls.getRecords();

         for (GlsRecord record : allRecords)
         {
            if (element.equals(record.getLabel()))
            {
               set.add(record);
               return;
            }
         }

         set.add(new GlsRecord(bib2gls, element, "", "page",
           "glsignore", ""));
      }

      private void orderByUse(TeXParser bibParser, Bib2GlsEntry entry, 
        CsvList csvList, String field, boolean reverse)
      throws IOException
      {
         TeXParserListener listener = bibParser.getListener();

         TeXObject opt = null;

         TreeSet<GlsRecord> set = new TreeSet<GlsRecord>();

         for (int i = 0, n = csvList.size(); i < n; i++)
         {
            TeXObject element = csvList.getValue(i);

            if (i == 0 && element instanceof TeXObjectList)
            {
               TeXObjectList elemList = (TeXObjectList)element;

               opt = elemList.popArg(bibParser, '[', ']');
            }

            insertByUse(element.toString(bibParser), set);
         }

         StringBuilder builder = new StringBuilder();
         TeXObjectList elementList = listener.createGroup();

         if (opt != null)
         {
            elementList.add(listener.getOther('['));
            elementList.add(opt);
            elementList.add(listener.getOther(']'));

            builder.append('[');
            builder.append(opt.toString(bibParser));
            builder.append(']');
         }

         boolean addSep = false;

         for (Iterator<GlsRecord> iter = reverse ? set.descendingIterator()
           : set.iterator() ; iter.hasNext(); )
         {
            GlsRecord record = iter.next();

            if (addSep)
            {
               elementList.add(listener.getOther(','));
               builder.append(',');
            }
            else
            {
               addSep = true;
            }

            String id = record.getLabel();
            elementList.add(listener.createString(id));
            builder.append(id);
         }

         BibValueList val = new BibValueList();
         val.add(new BibUserString(elementList));

         entry.putField(field, val);
         entry.putField(field, builder.toString());
      }

      private void sort(TeXParser bibParser, Bib2GlsEntry entry, 
        CsvList csvList, SortSettings settings, String field)
        throws IOException,Bib2GlsException
      {
         TeXParserListener listener = bibParser.getListener();

         Vector<Bib2GlsEntry> list = new Vector<Bib2GlsEntry>();
         TeXObject opt = null;

         for (int i = 0, n = csvList.size(); i < n; i++)
         {
            TeXObject element = (TeXObject)csvList.getValue(i).clone();

            if (i == 0 && element instanceof TeXObjectList
                 && ((TeXObjectList)element).size() > 2)
            {
               TeXObjectList elemList = (TeXObjectList)element;

               opt = elemList.popArg(bibParser, '[', ']');
            }

            TeXObjectList elementList = listener.createGroup();

            if (csname == null)
            {
               elementList.add(element);
            }
            else
            {
               elementList.add(new TeXCsRef(csname));
               Group group = listener.createGroup();
               elementList.add(group);
               group.add(element);
            }

            BibValueList bibValList = new BibValueList();
            bibValList.add(new BibUserString(elementList));

            Bib2GlsEntry copy = entry.getMinimalCopy();
            String id = element.toString(bibParser);

            String prefix = entry.getPrefix();

            if (prefix != null && id.startsWith(prefix) 
                 && id.length() > prefix.length())
            {
               id = id.substring(prefix.length());
            }

            copy.setId(prefix, id);
            copy.putField("name", bibValList);
            copy.putField("name", elementList.toString(bibParser));

            list.add(copy);
         }

         sortData(list, settings, "group", null);

         StringBuilder builder = new StringBuilder();
         TeXObjectList elementList = listener.createGroup();

         if (opt != null)
         {
            elementList.add(listener.getOther('['));
            elementList.add(opt);
            elementList.add(listener.getOther(']'));

            builder.append('[');
            builder.append(opt.toString(bibParser));
            builder.append(']');
         }

         for (int i = 0, n = list.size(); i < n; i++)
         {
            if (i > 0)
            {
               elementList.add(listener.getOther(','));
               builder.append(',');
            }

            Bib2GlsEntry index = list.get(i);

            String id = index.getId();

            elementList.add(listener.createString(id));
            builder.append(id);
         }

         BibValueList val = new BibValueList();
         val.add(new BibUserString(elementList));

         entry.putField(field, val);
         entry.putField(field, builder.toString());
      }

      private boolean hasField(Bib2GlsEntry entry)
      {
         for (String field : fields)
         {
            if (entry.getField(field) != null)
            {
               return true;
            }
         }

         return false;
      }

      private String[] fields;
      private String method;
      private String csname;
   }

   public int getCompactRanges()
   {
      return compactRanges;
   }

   public String getAdoptedParentField()
   {
      return adoptedParentField;
   }

   public boolean hasDualPrimaryDepencendies()
   {
      return dualPrimaryDependency;
   }

   public String getFieldEncap(String field)
   {
      if (encapFields == null) return null;

      String csname = encapFields.get(field);

      if ("".equals(csname)) return null;

      return csname;
   }

   public String getFieldEncapIncLabel(String field)
   {
      if (encapFieldsIncLabel == null) return null;

      String csname = encapFieldsIncLabel.get(field);

      if ("".equals(csname)) return null;

      return csname;
   }

   private File texFile;

   private Vector<TeXPath> sources;

   private boolean interpretLabelFields = false;

   private boolean stripMissingParents = false;

   private HashMap<String,String> entryTypeAliases = null;

   private String unknownEntryMap = null;

   private HashMap<String,String> fieldAliases = null;

   private HashMap<String,Vector<String>> fieldCopies = null;

   private boolean replicateOverride=false;

   public static final byte MISSING_FIELD_REPLICANT_SKIP=(byte)0;
   public static final byte MISSING_FIELD_REPLICANT_FALLBACK=(byte)1;
   public static final byte MISSING_FIELD_REPLICANT_EMPTY=(byte)2;

   private byte missingFieldReplicateAction = MISSING_FIELD_REPLICANT_SKIP;

   private String[] skipFields = null;

   private String[] bibtexAuthorList = null;

   private String[] interpretFields = null;

   private String[] dateTimeList = null;
   private String[] dateList = null;
   private String[] timeList = null;

   private String dateTimeListFormat = "default";
   private String dateListFormat = "default";
   private String timeListFormat = "default";

   private String dualDateTimeListFormat = "default";
   private String dualDateListFormat = "default";
   private String dualTimeListFormat = "default";

   private Locale dateTimeListLocale = null;
   private Locale dateListLocale = null;
   private Locale timeListLocale = null;

   private Locale dualDateTimeListLocale = null;
   private Locale dualDateListLocale = null;
   private Locale dualTimeListLocale = null;

   private String[] externalPrefixes = null;

   private String[] checkEndPunc = null;

   private String[] labelifyFields=null;
   private String[] labelifyListFields=null;
   private Vector<PatternReplace> labelifyReplaceMap;

   private String[] dependencyListFields=null;

   private LabelListSortMethod[] sortLabelList = null;

   private String type=null, category=null, counter=null;

   private String missingParentCategory=null;

   private SortSettings sortSettings;
   private SortSettings dualSortSettings;
   private SortSettings secondarySortSettings;

   private String symbolDefaultSortField = "id";
   private String entryDefaultSortField = "name";

   private String abbrevDefaultSortField = "short";
   private String abbrevDefaultNameField = "short";

   private String bibTeXEntryDefaultSortField = "name";

   private String dualType=null, dualCategory=null, dualCounter=null;

   private String triggerType=null;

   private String progenitorType=null, progenyType=null;

   private String adoptedParentField = "parent";

   private String pluralSuffix="\\glspluralsuffix ";
   private String dualPluralSuffix="\\glspluralsuffix ";

   private String shortPluralSuffix=null;
   private String dualShortPluralSuffix=null;

   private Charset bibCharset = null;

   private boolean flatten = false;

   private boolean interpretPreamble = true;

   private boolean setWidest = false;

   private WidestNameHierarchy widestNameNoType = null;

   private HashMap<String,WidestNameHierarchy> widestNames = null;

   private String secondaryType=null, secondaryField=null;

   private int minLocationRange = 3, locGap = 1;

   private String suffixF=null, suffixFF=null;

   private String preamble = null;
   private BibValueList preambleList = null;

   private boolean savePreamble = true;

   private HashMap<String,Pattern> fieldPatterns = null;

   private boolean notMatch=false;

   private boolean fieldPatternsAnd=true;

   private final byte MATCH_ACTION_FILTER = 0; 
   private final byte MATCH_ACTION_ADD = 1; 

   private byte matchAction = MATCH_ACTION_FILTER;

   private final byte WRITE_ACTION_DEFINE=0;
   private final byte WRITE_ACTION_COPY=1;
   private final byte WRITE_ACTION_DEFINE_OR_COPY=2;

   private byte writeAction = WRITE_ACTION_DEFINE;

   private String copyActionGroupField = null;

   private static final String PATTERN_FIELD_ID = "id";
   private static final String PATTERN_FIELD_ENTRY_TYPE = "entrytype";
   private static final String PATTERN_FIELD_ORIGINAL_ENTRY_TYPE = "original entrytype";

   private static final Pattern PATTERN_FIELD_CS = 
       Pattern.compile("gls(?:entry|access|xtr|fmt)?(.+)");

   private static final Pattern PATTERN_FIELD_CAP_CS = 
       Pattern.compile("(Gls|GLS)(?:entry|access|xtr|fmt)?(.+)");

   private Vector<String> allCapsEntryField=null;
   private Vector<String> allCapsAccessField=null;

   private Vector<Bib2GlsEntry> bibData;

   private Vector<Bib2GlsEntry> dualData;

   private boolean hasDuals = false, hasTertiaries = false;

   private Bib2Gls bib2gls;

   private int seeLocation=Bib2GlsEntry.POST_SEE;
   private int seeAlsoLocation=Bib2GlsEntry.POST_SEE;
   private int aliasLocation=Bib2GlsEntry.POST_SEE;

   private String[] locationPrefix = null;

   private String[] locationSuffix = null;

   private boolean saveLocations = true;
   private boolean saveLocList = true;

   private int savePrimaryLocations = SAVE_PRIMARY_LOCATION_OFF;

   private String[] primaryLocationFormats = null;

   public static final int SAVE_PRIMARY_LOCATION_OFF=0;
   public static final int SAVE_PRIMARY_LOCATION_REMOVE=1;
   public static final int SAVE_PRIMARY_LOCATION_RETAIN=2;
   public static final int SAVE_PRIMARY_LOCATION_START=3;
   public static final int SAVE_PRIMARY_LOCATION_DEFAULT_FORMAT=4;

   public static final int COMBINE_DUAL_LOCATIONS_OFF=0;
   public static final int COMBINE_DUAL_LOCATIONS_BOTH=1;
   public static final int COMBINE_DUAL_LOCATIONS_PRIMARY=2;
   public static final int COMBINE_DUAL_LOCATIONS_DUAL=3;

   private int combineDualLocations = COMBINE_DUAL_LOCATIONS_OFF;

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

   private boolean saveOriginalEntryType = false;

   private boolean defpagesname = false;

   public static final int ALIAS_LOC_OMIT=0;
   public static final int ALIAS_LOC_TRANS=1;
   public static final int ALIAS_LOC_KEEP=2;

   private int aliasLocations = ALIAS_LOC_TRANS;

   private boolean aliases = false;

   private String labelPrefix = null, dualPrefix="dual.";

   private String tertiaryType=null, tertiaryCategory=null,
     tertiaryPrefix="tertiary.";

   private String csLabelPrefix = null, recordLabelPrefix = null;

   private String dupLabelSuffix = null;

   private String dualField = null;

   private HashMap<String,String> dualEntryMap, dualAbbrevMap,
      dualSymbolMap, dualAbbrevEntryMap, dualIndexEntryMap,
      dualIndexSymbolMap, dualIndexAbbrevMap;

   // HashMap doesn't retain order, so keep track of the first
   // mapping separately.

   private String dualEntryFirstMap, dualAbbrevFirstMap, dualSymbolFirstMap,
     dualAbbrevEntryFirstMap, dualIndexEntryFirstMap, dualIndexSymbolFirstMap,
     dualIndexAbbrevFirstMap;

   private boolean backLinkDualEntry=false;
   private boolean backLinkDualAbbrev=false;
   private boolean backLinkDualSymbol=false;
   private boolean backLinkDualAbbrevEntry=false;
   private boolean backLinkDualIndexEntry=false;
   private boolean backLinkDualIndexSymbol=false;
   private boolean backLinkDualIndexAbbrev=false;

   private String shortCaseChange=null;
   private String dualShortCaseChange=null;
   private String nameCaseChange=null;
   private String descCaseChange=null;
   private String longCaseChange=null;
   private String dualLongCaseChange=null;

   private HashMap<String,String> fieldCaseChange = null;

   private static final String[] CASE_OPTIONS = new String[]
   {"none", "lc", "uc", "lc-cs", "uc-cs", "firstuc", "firstuc-cs", 
    "title", "title-cs"};

   private boolean wordBoundarySpace=true;
   private boolean wordBoundaryCsSpace=true;
   private boolean wordBoundaryNbsp=false;
   private boolean wordBoundaryDash=false;

   private String[] noCaseChangeCs = null;

   private HashMap<String,String> encapFields, encapFieldsIncLabel;

   public static final byte POST_DESC_DOT_NONE=0;
   public static final byte POST_DESC_DOT_ALL=1;
   public static final byte POST_DESC_DOT_CHECK=2;

   private byte postDescDot = POST_DESC_DOT_NONE; 

   private boolean stripTrailingNoPost = false;

   private String masterLinkPrefix=null;
   private Vector<TeXPath> masterGlsTeXPath = null;
   private TeXPath masterPdfPath = null;
   private String[] masterSelection = null;

   private String[] counters=null;

   private Random random=null;

   private HashMap<String,GroupTitle> groupTitleMap=null;

   private Vector<SupplementalRecord> supplementalRecords=null;
   private TeXPath supplementalPdfPath=null;
   private Vector<TeXPath> supplementalPdfPaths=null;
   private String[] supplementalSelection=null;
   private String supplementalCategory=null;

   private String groupField = null;

   private String saveOriginalId = null;

   private String indexCounter=null;

   public static final int SELECTION_RECORDED_AND_DEPS=0;
   public static final int SELECTION_RECORDED_AND_DEPS_AND_SEE=1;
   public static final int SELECTION_RECORDED_NO_DEPS=2;
   public static final int SELECTION_RECORDED_AND_PARENTS=3;
   public static final int SELECTION_ALL=4;

   private int selectionMode = SELECTION_RECORDED_AND_DEPS;

   private static final String[] SELECTION_OPTIONS = new String[]
    {"recorded and deps", "recorded and deps and see",
     "recorded no deps", "recorded and ancestors", "all"};

   public static final byte CONTRIBUTOR_ORDER_SURNAME=0;
   public static final byte CONTRIBUTOR_ORDER_VON=1;
   public static final byte CONTRIBUTOR_ORDER_FORENAMES=2;

   private byte contributorOrder=CONTRIBUTOR_ORDER_VON;

   private Vector<String> dependencies;

   private boolean dualPrimaryDependency=true;

   private int limit=0;

   private boolean copyAliasToSee = false;

   private boolean createMissingParents = false;

   private int compactRanges = 0;

   private Bib2GlsBibParser bibParserListener = null;
}

