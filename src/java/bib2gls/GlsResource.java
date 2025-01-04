/*
    Copyright (C) 2017-2025 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.bib2gls;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.text.Collator;
import java.text.CollationKey;
import java.text.ParseException;
import java.text.BreakIterator;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.auxfile.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.generic.Nbsp;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.MissingValue;
import com.dickimawbooks.texparserlib.latex.CsvList;
import com.dickimawbooks.texparserlib.latex.LaTeXGenericCommand;
import com.dickimawbooks.texparserlib.html.L2HStringConverter;

import com.dickimawbooks.bibgls.common.Bib2GlsException;

/**
 * Class representing a resource set. Each resource set is
 * identified by {@code \glsxtr@resource} in the aux file (which is
 * written to the aux file by {@code \glsxtrresourcefile}).
 * @author Nicola L C Talbot
 */

public class GlsResource
{
   /**
    * Creates a new instance corresponding to a resource set.
    * @param parser the aux file parser
    * @param data the aux data corresponding to {@code \glsxtr@resource}
    * which has two arguments: the settings and the basename of the
    * glstex file
    * @param pluralSuffix the default plural suffix obtained from
    * the first argument of {@code \glsxtr@pluralsuffixes}
    * @param abbrvPluralSuffix the default abbreviation plural suffix obtained from
    * the second argument of {@code \glsxtr@pluralsuffixes}
    * @throws Bib2GlsException invalid resource setting syntax
    * @throws IllegalArgumentException invalid value supplied to a setting
    * @throws IOException may be thrown by the aux file parser
    * @throws InterruptedException may be thrown by an interrupted
    * system call to kpsewhich (when trying to find a bib file on
    * TeX's path)
    */
   public GlsResource(TeXParser parser, AuxData data, 
     String pluralSuffix, String abbrvPluralSuffix)
    throws IOException,InterruptedException,Bib2GlsException
   {
      this.parser = parser;
      sources = new Vector<TeXPath>();

      this.pluralSuffix = pluralSuffix;
      this.dualPluralSuffix = pluralSuffix;

      init(data.getArg(0), data.getArg(1));
   }

   /**
    * Gets the aux parser.
    * @return the parser
    */ 
   public TeXParser getParser()
   {
      return parser;
   }

   /**
    * Gets the string representation of this resource set.
    * @return the string representation of this object
    */ 
   @Override
   public String toString()
   {
      return texFile == null ? super.toString() : texFile.getName();
   }

   /*
    * METHODS: initialisation (stage 1).
    * Establishing settings and parsing supplemental or 
    * master aux files.
    */

   /**
    * Initialises the settings for this resource set (stage 1).
    * @param opts the resource settings (which should be a key=value comma-separated list)
    * @param glstexBasename the basename of the glstex file
    * @throws Bib2GlsException invalid resource setting syntax
    * @throws IllegalArgumentException invalid value supplied to a setting
    * @throws IOException may be thrown by the aux file parser
    * @throws InterruptedException may be thrown by an interrupted
    * system call to kpsewhich (when trying to find a bib file on
    * TeX's path)
    */
   private void init(TeXObject opts, TeXObject glstexBasename)
      throws IOException,InterruptedException,
             Bib2GlsException,IllegalArgumentException
   {
      bib2gls = (Bib2Gls)parser.getListener().getTeXApp();

      sortSettings = new SortSettings("resource", this);
      dualSortSettings = new SortSettings(this);
      secondarySortSettings = new SortSettings(this);

      TeXPath texPath = new TeXPath(parser, 
        glstexBasename.toString(parser), "glstex", false);

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
            srcList = getStringArray(list, opt);

            if (srcList == null)
            {
               sources.add(bib2gls.getBibFilePath(parser, filename));
            }
            else
            {
               for (String src : srcList)
               {
                  if (!src.isEmpty())
                  {// skip empty elements
                     sources.add(bib2gls.getBibFilePath(parser, src));
                  }
               }
            }
         }
         else if (opt.equals("master"))
         {// link all entries to glossary in external pdf file
            master = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("master-resources"))
         {
            masterSelection = getStringArray(list, opt);
         }
         else if (opt.equals("supplemental-locations"))
         {// Fetch supplemental locations from another document.
          // As from v1.7, the value may now be a list of
          // document base names. 
            supplemental = getStringArray(list, opt);
         }
         else if (opt.equals("supplemental-category"))
         {
            supplementalCategory = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("supplemental-selection"))
         {
            supplementalSelection = getStringArray(list, opt);

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
            entryTypeAliases = getHashMap(list, opt);

            if (entryTypeAliases == null)
            {
               if (bib2gls.isVerbose())
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                     "message.clearing.entry.aliases"));
               }
            }
            else
            {
               if (bib2gls.isVerbose())
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

                  if (bib2gls.isVerbose())
                  {
                     bib2gls.logMessage(String.format("@%s=>@%s.", key, val));
                  }
               }
            }
         }
         else if (opt.equals("unknown-entry-alias"))
         {
            unknownEntryMap = getOptional("", list, opt);

            if ("".equals(unknownEntryMap))
            {
               unknownEntryMap = null;
            }
         }
         else if (opt.equals("field-aliases"))
         {
            fieldAliases = getHashMap(list, opt);

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
         else if (opt.equals("assign-fields"))
         {
            assignFieldsData = parseAssignFieldsData(list, opt);
         }
         else if (opt.equals("assign-override"))
         {
            assignFieldsOverride = getBoolean(list, opt);
         }
         else if (opt.equals("assign-missing-field-action"))
         {
            missingFieldAssignAction = getMissingFieldAction(list, opt);
         }
         else if (opt.equals("copy-to-glossary"))
         {
            copyToGlossary = getFieldEvaluationList(list, opt);
         }
         else if (opt.equals("copy-to-glossary-missing-field-action"))
         {
            missingFieldCopyToGlossary = getMissingFieldAction(list, opt);
         }
         else if (opt.equals("omit-fields"))
         {
            omitFields = getFieldEvaluationList(list, opt);
         }
         else if (opt.equals("omit-fields-missing-field-action"))
         {
            missingFieldOmitFields = getMissingFieldAction(list, opt);
         }
         else if (opt.equals("replicate-fields"))
         {
            fieldCopies = getHashMapVector(list, opt, true);

            if (fieldCopies != null)
            {
               for (Iterator<String> mapIt = fieldCopies.keySet().iterator();
                    mapIt.hasNext(); )
               {
                  String key = mapIt.next();

                  if (bib2gls.isPrivateNonBibField(key))
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.field", key, opt));
                  }

                  Vector<String> values = fieldCopies.get(key);

                  for (String f : values)
                  {
                     if (bib2gls.isPrivateNonBibField(f))
                     {
                        throw new IllegalArgumentException(
                          bib2gls.getMessage("error.invalid.field", f, opt));
                     }
                     else if (!(bib2gls.isKnownField(f)
                                || bib2gls.isKnownSpecialField(f)
                                || bib2gls.isNonBibField(f)
                                ))
                     {
                        addUserField(f);
                     }
                  }
               }
            }
         }
         else if (opt.equals("replicate-override"))
         {
            replicateOverride = getBoolean(list, opt);
         }
         else if (opt.equals("replicate-missing-field-action"))
         {
            missingFieldReplicateAction = getMissingFieldAction(list, opt);
         }
         else if (opt.equals("primary-dual-dependency"))
         {
            dualPrimaryDependency = getBoolean(list, opt);
         }
         else if (opt.equals("strip-trailing-nopost"))
         {
            stripTrailingNoPost = getBoolean(list, opt);
         }
         else if (opt.equals("copy-alias-to-see"))
         {
            copyAliasToSee = getBoolean(list, opt);
         }
         else if (opt.equals("save-index-counter"))
         {
            indexCounter = getOptionalOrFalse("true", list, opt);
         }
         else if (opt.equals("save-from-alias"))
         {
            saveFromAlias = getOptionalOrFalse("from-alias", list, opt);
         }
         else if (opt.equals("save-from-seealso"))
         {
            saveFromSeeAlso = getOptionalOrFalse("from-seealso", list, opt);
         }
         else if (opt.equals("save-from-see"))
         {
            saveFromSee = getOptionalOrFalse("from-see", list, opt);
         }
         else if (opt.equals("save-crossref-tail"))
         {
            saveCrossRefTail = getOptionalOrFalse("crossref-tail", list, opt);
         }
         else if (opt.equals("save-definition-index"))
         {
            saveDefinitionIndex = getBoolean(list, opt);
         }
         else if (opt.equals("save-use-index"))
         {
            saveUseIndex = getBoolean(list, opt);
         }
         else if (opt.equals("post-description-dot"))
         {
            String val = getChoice(list, opt, "none", "all", "check");

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
            String[] array = getStringArray(list, opt);

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
            noCaseChangeCs = getStringArray(list, opt);
         }
         else if (opt.equals("name-case-change"))
         {
            nameCaseChange = getChoice(list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("description-case-change"))
         {
            descCaseChange = getChoice(list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("short-case-change"))
         {
            shortCaseChange = getChoice(list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("dual-short-case-change"))
         {
            dualShortCaseChange = getChoice(list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("short-plural-suffix"))
         {
            shortPluralSuffix = getOptional("", list, opt);

            if (shortPluralSuffix.equals("use-default"))
            {
               shortPluralSuffix = null;
            }
         }
         else if (opt.equals("long-case-change"))
         {
            longCaseChange = getChoice(list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("dual-long-case-change"))
         {
            dualLongCaseChange = getChoice(list, opt, CASE_OPTIONS);
         }
         else if (opt.equals("field-case-change"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);

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

                  Vector<TeXObject> split = splitList('=', 
                     (TeXObjectList)array[i]);

                  if (split == null || split.size() == 0) continue;

                  String field = split.get(0).toString(parser);

                  if (!isReferencableField(field))
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
            TeXObject[] array = getTeXObjectArray(list, opt, true);

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

                  Vector<TeXObject> split = splitList('=', 
                     (TeXObjectList)array[i]);

                  if (split == null || split.size() == 0) continue;

                  String field = split.get(0).toString(parser);

                  if (!isReferencableField(field))
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
            TeXObject[] array = getTeXObjectArray(list, opt, true);

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

                  Vector<TeXObject> split = splitList('=', 
                     (TeXObjectList)array[i]);

                  if (split == null || split.size() == 0) continue;

                  String field = split.get(0).toString(parser);

                  if (!isReferencableField(field))
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
         else if (opt.equals("encapsulate-sort"))
         {
            encapSort = getOptional("", list, opt);

            if (encapSort.isEmpty())
            {
               encapSort = null;
            }
         }
         else if (opt.equals("format-integer-fields"))
         {
            formatIntegerFields = getFieldFormatPattern(list, opt);
         }
         else if (opt.equals("format-decimal-fields"))
         {
            formatDecimalFields = getFieldFormatPattern(list, opt);
         }
         else if (opt.equals("append-prefix-field"))
         {
            String val = getChoice(list, opt, PREFIX_FIELD_OPTIONS);

            appendPrefixField = -1;

            for (int i = 0; i < PREFIX_FIELD_OPTIONS.length; i++)
            {
               if (val.equals(PREFIX_FIELD_OPTIONS[i]))
               {
                  appendPrefixField = i;
                  break;
               }
            }
         }
         else if (opt.equals("append-prefix-field-cs"))
         {
            TeXObject obj = getRequiredObject(list, opt);

            if (obj instanceof ControlSequence)
            {
               prefixControlSequence = (ControlSequence)obj;
            }
            else if (obj instanceof TeXObjectList)
            {
               TeXObject o = ((TeXObjectList)obj).popStack(parser,
                  TeXObjectList.POP_IGNORE_LEADING_SPACE);

               if (o == null)
               {
                  throw new IllegalArgumentException(
                        bib2gls.getMessage("error.append.prefix.field.spacecs", 
                         "", opt));
               }

               if (!(o instanceof ControlSequence))
               {
                  throw new IllegalArgumentException(
                        bib2gls.getMessage("error.append.prefix.field.spacecs", 
                         o.toString(parser), opt));
               }

               prefixControlSequence = (ControlSequence)o;

               o = ((TeXObjectList)obj).peekStack(
                  TeXObjectList.POP_IGNORE_LEADING_SPACE);

               if (o != null)
               {
                  throw new IllegalArgumentException(
                        bib2gls.getMessage("error.append.prefix.field.spacecs", 
                         o.toString(parser), opt));
               }
            }
            else
            {
               throw new IllegalArgumentException(
                     bib2gls.getMessage("error.append.prefix.field.spacecs", 
                      obj.toString(parser), opt));
            }
         }
         else if (opt.equals("append-prefix-field-exceptions"))
         {
            TeXObject obj = getRequiredObject(list, opt);

            prefixFieldExceptions = new Vector<Integer>();

            if (obj instanceof CharObject)
            {
               prefixFieldExceptions.add(((CharObject)obj).getCharCode());
            }
            else if (obj instanceof ActiveChar)
            {
               prefixFieldExceptions.add(((ActiveChar)obj).getCharCode());
            }
            else if (obj instanceof TeXObjectList)
            {
               while (((TeXObjectList)obj).size() > 0)
               {
                  TeXObject o = ((TeXObjectList)obj).pop();

                  if (o instanceof CharObject)
                  {
                     prefixFieldExceptions.add(((CharObject)o).getCharCode());
                  }
                  else if (o instanceof ActiveChar)
                  {
                     prefixFieldExceptions.add(((ActiveChar)o).getCharCode());
                  }
                  else if (o instanceof ControlSequence && 
                     ((ControlSequence)o).getName().equals("u"))
                  {
                     TeXNumber num = ((TeXObjectList)obj).popNumber(parser, 16);

                     prefixFieldExceptions.add(num.getValue());
                  }
                  else if (!(o instanceof WhiteSpace || o instanceof Ignoreable))
                  {
                     throw new IllegalArgumentException(
                        bib2gls.getMessage("error.append.prefix.field", 
                         o.toString(parser), opt));
                  }
               }
            }
            else if (!(obj instanceof WhiteSpace || obj instanceof Ignoreable))
            {
               throw new IllegalArgumentException(
                       bib2gls.getMessage("error.append.prefix.field", 
                       obj.toString(parser), opt));
            }
         }
         else if (opt.equals("append-prefix-field-cs-exceptions"))
         {
            TeXObject obj = getRequiredObject(list, opt);

            prefixFieldCsExceptions = new Vector<String>();

            if (obj instanceof ControlSequence)
            {
               prefixFieldCsExceptions.add(((ControlSequence)obj).getName());
            }
            else if (obj instanceof TeXObjectList)
            {
               while (((TeXObjectList)obj).size() > 0)
               {
                  TeXObject o = ((TeXObjectList)obj).pop();

                  if (o instanceof ControlSequence)
                  {
                     prefixFieldCsExceptions.add(((ControlSequence)o).getName());
                  }
                  else if (!(o instanceof WhiteSpace || o instanceof Ignoreable))
                  {
                     throw new IllegalArgumentException(
                        bib2gls.getMessage("error.append.prefix.field.cs", 
                         o.toString(parser), opt));
                  }
               }
            }
            else if (!(obj instanceof WhiteSpace || obj instanceof Ignoreable))
            {
               throw new IllegalArgumentException(
                       bib2gls.getMessage("error.append.prefix.field", 
                       obj.toString(parser), opt));
            }
         }
         else if (opt.equals("prefix-fields"))
         {
            prefixFields = getStringArray(list, opt);
         }
         else if (opt.equals("append-prefix-field-nbsp-match"))
         {
            String val = getRequired(list, opt);

            try
            {
               prefixFieldNbspPattern = Pattern.compile(val);
            }
            catch (PatternSyntaxException e)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.pattern", val, opt), e);
            }
         }
         else if (opt.equals("dual-short-plural-suffix"))
         {
            dualShortPluralSuffix = getOptional("", list, opt);

            if (dualShortPluralSuffix.equals("use-default"))
            {
               dualShortPluralSuffix = null;
            }
         }
         else if (opt.equals("action"))
         {
            writeActionSetting = getChoice(list, opt,
              "define", "provide", "define or copy", "copy");

            if (writeActionSetting.equals("define"))
            {
               writeAction = WRITE_ACTION_DEFINE;
            }
            else if (writeActionSetting.equals("provide"))
            {
               writeAction = WRITE_ACTION_PROVIDE;
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
               copyActionGroupField = getRequired(list, opt);
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
            String val = getChoice(list, opt, "filter", "add");

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
            String val = getChoice(list, opt, "and", "or");

            fieldPatternsAnd = val.equals("and");
         }
         else if (opt.equals("match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            notMatch = false;

            if (array == null)
            {
               fieldPatterns = null;
            }
            else
            {
               fieldPatterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(fieldPatterns, array, opt, list);
            }
         }
         else if (opt.equals("not-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            notMatch = true;

            if (array == null)
            {
               fieldPatterns = null;
            }
            else
            {
               fieldPatterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(fieldPatterns, array, opt, list);
            }
         }
         else if (opt.equals("secondary-match-action"))
         {
            String val = getChoice(list, opt, "filter", "add");

            if (val.equals("filter"))
            {
               secondaryMatchAction = MATCH_ACTION_FILTER;
            }
            else
            {
               secondaryMatchAction = MATCH_ACTION_ADD;
            }
         }
         else if (opt.equals("secondary-match-op"))
         {
            String val = getChoice(list, opt, "and", "or");

            secondaryFieldPatternsAnd = val.equals("and");
         }
         else if (opt.equals("secondary-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            secondaryNotMatch = false;

            if (array == null)
            {
               secondaryFieldPatterns = null;
            }
            else
            {
               secondaryFieldPatterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(secondaryFieldPatterns, array, opt, list);
            }
         }
         else if (opt.equals("secondary-not-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            secondaryNotMatch = true;

            if (array == null)
            {
               secondaryFieldPatterns = null;
            }
            else
            {
               secondaryFieldPatterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(secondaryFieldPatterns, array, opt, list);
            }
         }
         else if (opt.equals("limit"))
         {
            limit = getRequiredIntGe(0, list, opt);
         }
         else if (opt.equals("secondary"))
         {
            TeXObject obj = getRequiredObject(list, opt);

            if (!(obj instanceof TeXObjectList))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                    obj.toString(parser)));
            }

            Vector<TeXObject> split = splitList(':', 
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
            TeXObject[] array = getTeXObjectArray(list, opt, true);

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

                  sortLabelList[j] = getLabelListSortMethod(
                    (TeXObjectList)array[j], opt); 
               }
            }
         }
         else if (opt.equals("ext-prefixes"))
         {
            externalPrefixes = getStringArray(list, opt);
         }
         else if (opt.equals("labelify"))
         {
            labelifyFields = getFieldArray(list, opt, true);
         }
         else if (opt.equals("labelify-list"))
         {
            labelifyListFields = getFieldArray(list, opt, true);
         }
         else if (opt.equals("labelify-replace"))
         {
            labelifyReplaceMap = getSubstitutionList(list, opt, true);
         }
         else if (opt.equals("dependency-fields"))
         {
            dependencyListFields = getFieldArray(list, opt, true);
         }
         else if (opt.equals("gather-parsed-dependencies"))
         {
            gatherParsedDeps = getOptionalOrFalse("seealso", list, opt);
         }
         else if (opt.equals("sort-replace"))
         {
            sortSettings.setRegexList(
              getSubstitutionList(list, opt, true));
         }
         else if (opt.equals("dual-sort-replace"))
         {
            dualSortSettings.setRegexList(
              getSubstitutionList(list, opt, true));
         }
         else if (opt.equals("secondary-sort-replace"))
         {
            secondarySortSettings.setRegexList(
              getSubstitutionList(list, opt, true));
         }
         else if (opt.equals("interpret-preamble"))
         {
            interpretPreamble = getBoolean(list, opt);
         }
         else if (opt.equals("interpret-label-fields"))
         {
            interpretLabelFields = getBoolean(list, opt);
         }
         else if (opt.equals("strip-missing-parents"))
         {
            stripMissingParents = getBoolean(list, opt);

            if (stripMissingParents)
            {
               createMissingParents = false;
            }
         }
         else if (opt.equals("missing-parents"))
         {
            String val = getChoice(list, opt, "strip", "create",
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
            savePreamble = getBoolean(list, opt);
         }
         else if (opt.equals("flatten"))
         {
            flatten = getBoolean(list, opt);
         }
         else if (opt.equals("flatten-lonely"))
         {
            String val = getChoice(list, opt, "false", "presort",
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
            String val = getChoice(list, opt, "only unrecorded parents",
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
         else if (opt.equals("flatten-lonely-condition"))
         {
            flattenLonelyConditional = getCondition(list, opt);
         }
         else if (opt.equals("flatten-lonely-missing-field-action"))
         {
            missingFieldFlattenLonely = getMissingFieldAction(list, opt);
         }
         else if (opt.equals("save-locations"))
         {
            String val = getChoice("true", list, opt,
               "false", "true", "see", "see not also", "alias only");

            if (val.equals("false"))
            {
               saveLocations = SAVE_LOCATIONS_OFF;
            }
            else if (val.equals("true"))
            {
               saveLocations = SAVE_LOCATIONS_ON;
            }
            else if (val.equals("see"))
            {
               saveLocations = SAVE_LOCATIONS_SEE;
            }
            else if (val.equals("see not also"))
            {
               saveLocations = SAVE_LOCATIONS_SEE_NOT_ALSO;
            }
            else if (val.equals("alias only"))
            {
               saveLocations = SAVE_LOCATIONS_ALIAS_ONLY;
            }
         }
         else if (opt.equals("save-loclist"))
         {
            saveLocList = getBoolean(list, opt);
         }
         else if (opt.equals("merge-ranges"))
         {
            mergeRanges = getBoolean(list, opt);
         }
         else if (opt.equals("save-primary-locations")
                 || opt.equals("save-principal-locations"))
         {
            String val = getChoice(list, opt,
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
         else if (opt.equals("primary-location-formats")
               || opt.equals("principal-location-formats"))
         {
            primaryLocationFormats = getStringArray(list, opt);

            if (savePrimaryLocations == SAVE_PRIMARY_LOCATION_OFF)
            {
               savePrimaryLocations = SAVE_PRIMARY_LOCATION_RETAIN;
            }
         }
         else if (opt.equals("combine-dual-locations"))
         {
            String val = getChoice(list, opt,
              "false", "both", "dual", "primary",
              "dual retain principal", "primary retain principal");

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
            else if (val.equals("dual retain principal"))
            {
               combineDualLocations = COMBINE_DUAL_LOCATIONS_DUAL_RETAIN_PRINCIPAL;
            }
            else if (val.equals("primary retain principal"))
            {
               combineDualLocations = COMBINE_DUAL_LOCATIONS_PRIMARY_RETAIN_PRINCIPAL;
            }
         }
         else if (opt.equals("save-child-count"))
         {
            saveChildCount = getBoolean(list, opt);
         }
         else if (opt.equals("save-sibling-count"))
         {
            saveSiblingCount = getBoolean(list, opt);
         }
         else if (opt.equals("save-root-ancestor"))
         {
            saveRootAncestor = getBoolean(list, opt);
         }
         else if (opt.equals("save-original-entrytype"))
         {
            saveOriginalEntryType = getOptional(list, opt);

            if (saveOriginalEntryType == null || saveOriginalEntryType.isEmpty()
                || saveOriginalEntryType.equals("true"))
            {
               saveOriginalEntryType = "originalentrytype";
            }
            else if (saveOriginalEntryType.equals("false"))
            {
               saveOriginalEntryType = null;
            }
            else if (bib2gls.isKnownSpecialField(saveOriginalEntryType))
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                 "error.invalid.field", opt, saveOriginalEntryType));
            }
         }
         else if (opt.equals("save-original-entrytype-action"))
         {
            String val = getChoice(list, opt, 
              "always", "no override", "changed override", "changed no override",
              "changed", "diff");

            if (val.equals("always"))
            {
               saveOriginalEntryTypeAction = SAVE_ORIGINAL_ALWAYS;
            }
            else if (val.equals("no override"))
            {
               saveOriginalEntryTypeAction = SAVE_ORIGINAL_NO_OVERRIDE;
            }
            else if (val.equals("changed override") || val.equals("changed")
                      || val.equals("diff"))
            {
               saveOriginalEntryTypeAction = SAVE_ORIGINAL_CHANGED_OVERRIDE;
            }
            else if (val.equals("changed no override"))
            {
               saveOriginalEntryTypeAction = SAVE_ORIGINAL_CHANGED_NO_OVERRIDE;
            }
         }
         else if (opt.equals("alias-loc"))
         {
            String val = getChoice(list, opt, 
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
            setWidest = getBoolean(list, opt);
         }
         else if (opt.equals("dual-entry-map"))
         {
            String[] keys = new String[1];

            dualEntryMap = getDualMap(list, opt, keys);

            dualEntryFirstMap = keys[0];
         }
         else if (opt.equals("dual-abbrv-map"))
         {
            String[] keys = new String[1];

            dualAbbrevMap = getDualMap(list, opt, keys);

            dualAbbrevFirstMap = keys[0];
         }
         else if (opt.equals("dual-entryabbrv-map"))
         {
            bib2gls.warning(bib2gls.getMessage(
              "warning.deprecated.option", opt, "dual-abbrventry-map"));

            String[] keys = new String[1];

            dualAbbrevEntryMap = getDualMap(list, opt, keys);

            dualAbbrevEntryFirstMap = keys[0];
         }
         else if (opt.equals("dual-abbrventry-map"))
         {
            String[] keys = new String[1];

            dualAbbrevEntryMap = getDualMap(list, opt, keys);

            dualAbbrevEntryFirstMap = keys[0];
         }
         else if (opt.equals("dual-indexentry-map"))
         {
            String[] keys = new String[1];

            dualIndexEntryMap = getDualMap(list, opt, keys);

            dualIndexEntryFirstMap = keys[0];
         }
         else if (opt.equals("dual-indexsymbol-map"))
         {
            String[] keys = new String[1];

            dualIndexSymbolMap = getDualMap(list, opt, keys);

            dualIndexSymbolFirstMap = keys[0];
         }
         else if (opt.equals("dual-indexabbrv-map"))
         {
            String[] keys = new String[1];

            dualIndexAbbrevMap = getDualMap(list, opt, keys);

            dualIndexAbbrevFirstMap = keys[0];
         }
         else if (opt.equals("dual-symbol-map"))
         {
            String[] keys = new String[1];

            dualSymbolMap = getDualMap(list, opt, keys);

            dualSymbolFirstMap = keys[0];
         }
         else if (opt.equals("dual-backlink"))
         {
            if (getBoolean(list, opt))
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
            backLinkDualEntry = getBoolean(list, opt);
         }
         else if (opt.equals("dual-abbrv-backlink"))
         {
            backLinkDualAbbrev = getBoolean(list, opt);
         }
         else if (opt.equals("dual-entryabbrv-backlink"))
         {
            bib2gls.warning(bib2gls.getMessage("warning.deprecated.option", opt,
              "dual-abbrventry-backlink"));
            backLinkDualAbbrevEntry = getBoolean(list, opt);
         }
         else if (opt.equals("dual-abbrventry-backlink"))
         {
            backLinkDualAbbrevEntry = getBoolean(list, opt);
         }
         else if (opt.equals("dual-indexentry-backlink"))
         {
            backLinkDualIndexEntry = getBoolean(list, opt);
         }
         else if (opt.equals("dual-indexsymbol-backlink"))
         {
            backLinkDualIndexSymbol = getBoolean(list, opt);
         }
         else if (opt.equals("dual-indexabbrv-backlink"))
         {
            backLinkDualIndexAbbrev = getBoolean(list, opt);
         }
         else if (opt.equals("dual-symbol-backlink"))
         {
            backLinkDualSymbol = getBoolean(list, opt);
         }
         else if (opt.equals("type"))
         {
            type = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("dual-type"))
         {
            dualType = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("trigger-type"))
         {
            triggerType = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("ignored-type"))
         {
            ignoredType = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("progenitor-type"))
         {
            progenitorType = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("progeny-type"))
         {
            progenyType = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("adopted-parent-field"))
         {// default = "parent"
            adoptedParentField = getRequired(list, opt);
         }
         else if (opt.equals("dual-field"))
         {
            dualField = getOptionalOrFalse("dual", list, opt);
         }
         else if (opt.equals("category"))
         {
            category = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("dual-category"))
         {
            dualCategory = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("missing-parent-category"))
         {
            missingParentCategory = getRequiredOrFalse(list, opt);

            if ("no value".equals(missingParentCategory))
            {
               missingParentCategory = null;
            }
         }
         else if (opt.equals("counter"))
         {
            counter = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("dual-counter"))
         {
            dualCounter = getRequiredOrFalse(list, opt);
         }
         else if (opt.equals("label-prefix"))
         {
            labelPrefix = getOptional(list, opt);
         }
         else if (opt.equals("dual-prefix"))
         {
            dualPrefix = getOptional(list, opt);
         }
         else if (opt.equals("tertiary-prefix"))
         {
            tertiaryPrefix = getOptional(list, opt);
         }
         else if (opt.equals("tertiary-category"))
         {
            tertiaryCategory = getOptional(list, opt);
         }
         else if (opt.equals("tertiary-type"))
         {
            tertiaryType = getOptional(list, opt);
         }
         else if (opt.equals("cs-label-prefix"))
         {
            csLabelPrefix = getOptional(list, opt);

            if (csLabelPrefix == null)
            {
               csLabelPrefix = "";
            }
         }
         else if (opt.equals("record-label-prefix"))
         {
            recordLabelPrefix = getOptional(list, opt);
         }
         else if (opt.equals("duplicate-label-suffix"))
         {
            dupLabelSuffix = getOptional(list, opt);
         }
         else if (opt.equals("prefix-only-existing"))
         {
            insertPrefixOnlyExists = getBoolean(list, opt);
         }
         else if (opt.equals("save-original-id"))
         {
            saveOriginalId = getOptional(list, opt);

            if (saveOriginalId == null || saveOriginalId.isEmpty()
                || saveOriginalId.equals("true"))
            {
               saveOriginalId = "originalid";
            }
            else if (saveOriginalId.equals("false"))
            {
               saveOriginalId = null;
            }
            else if (bib2gls.isKnownSpecialField(saveOriginalId))
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                 "error.invalid.field", opt, saveOriginalId));
            }
         }
         else if (opt.equals("save-original-id-action"))
         {
            String val = getChoice(list, opt, 
              "always", "no override", "changed override", "changed no override",
              "changed", "diff");

            if (val.equals("always"))
            {
               saveOriginalIdAction = SAVE_ORIGINAL_ALWAYS;
            }
            else if (val.equals("no override"))
            {
               saveOriginalIdAction = SAVE_ORIGINAL_NO_OVERRIDE;
            }
            else if (val.equals("changed override") || val.equals("changed")
                      || val.equals("diff"))
            {
               saveOriginalIdAction = SAVE_ORIGINAL_CHANGED_OVERRIDE;
            }
            else if (val.equals("changed no override"))
            {
               saveOriginalIdAction = SAVE_ORIGINAL_CHANGED_NO_OVERRIDE;
            }
         }
         else if (opt.equals("sort-suffix"))
         {
            String val = getRequired(list, opt);

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
            String val = getRequired(list, opt);

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
            String val = getRequired(list, opt);

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
               replaceHex(getOptional("", list, opt)));
            dualSortSettings.setSuffixMarker(sortSettings.getSuffixMarker());
            secondarySortSettings.setSuffixMarker(sortSettings.getSuffixMarker());
         }
         else if (opt.equals("dual-sort-suffix-marker"))
         {
            dualSortSettings.setSuffixMarker(
               replaceHex(getOptional("", list, opt)));
         }
         else if (opt.equals("secondary-sort-suffix-marker"))
         {
            secondarySortSettings.setSuffixMarker(
               replaceHex(getOptional("", list, opt)));
         }
         else if (opt.equals("group-formation"))
         {
             String val = getChoice(list, opt, "default", 
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
             String val = getChoice(list, opt, "default", 
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
             String val = getChoice(list, opt, "default", 
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
            String val = getRequired(list, opt);

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
            else if (val.equals("def"))
            {
               sortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_DEF);
            }
            else if (val.equals("use"))
            {
               sortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_RECORD);
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
            String val = getRequired(list, opt);

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
            else if (val.equals("def"))
            {
               dualSortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_DEF);
            }
            else if (val.equals("use"))
            {
               dualSortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_RECORD);
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
            String val = getRequired(list, opt);

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
            else if (val.equals("def"))
            {
               secondarySortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_DEF);
            }
            else if (val.equals("use"))
            {
               secondarySortSettings.setIdenticalSortAction(
                  SortSettings.IDENTICAL_SORT_USE_RECORD);
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
            String method = getOptional("doc", list, opt);

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
              replaceHexAndSpecial(getRequired(list, opt)));
         }
         else if (opt.equals("dual-sort-rule"))
         {
            dualSortSettings.setCollationRule(
              replaceHexAndSpecial(getRequired(list, opt)));
         }
         else if (opt.equals("secondary-sort-rule"))
         {
            secondarySortSettings.setCollationRule(
              replaceHexAndSpecial(getRequired(list, opt)));
         }
         else if (opt.equals("sort-number-pad"))
         {
            sortSettings.setNumberPad(
               getRequiredInt(list, opt));
            dualSortSettings.setNumberPad(sortSettings.getNumberPad());
            secondarySortSettings.setNumberPad(sortSettings.getNumberPad());
         }
         else if (opt.equals("dual-sort-number-pad"))
         {
            dualSortSettings.setNumberPad(
               getRequiredInt(list, opt));
         }
         else if (opt.equals("secondary-sort-number-pad"))
         {
            secondarySortSettings.setNumberPad(
               getRequiredInt(list, opt));
         }
         else if (opt.equals("sort-pad-plus"))
         {
            sortSettings.setPadPlus(
               replaceHex(getOptional("", list, opt)));
            dualSortSettings.setPadPlus(sortSettings.getPadPlus());
            secondarySortSettings.setPadPlus(sortSettings.getPadPlus());
         }
         else if (opt.equals("dual-sort-pad-plus"))
         {
            dualSortSettings.setPadPlus(
               replaceHex(getOptional("", list, opt)));
         }
         else if (opt.equals("secondary-sort-pad-plus"))
         {
            secondarySortSettings.setPadPlus(
               replaceHex(getOptional("", list, opt)));
         }
         else if (opt.equals("sort-pad-minus"))
         {
            sortSettings.setPadMinus(
               replaceHex(getOptional("", list, opt)));
            dualSortSettings.setPadMinus(sortSettings.getPadMinus());
            secondarySortSettings.setPadMinus(sortSettings.getPadMinus());
         }
         else if (opt.equals("dual-sort-pad-minus"))
         {
            dualSortSettings.setPadMinus(
               replaceHex(getOptional("", list, opt)));
         }
         else if (opt.equals("secondary-sort-pad-minus"))
         {
            secondarySortSettings.setPadMinus(
               replaceHex(getOptional("", list, opt)));
         }
         else if (opt.equals("numeric-locale"))
         {
            sortSettings.setNumberLocale(
              getRequired(list, opt));
         }
         else if (opt.equals("dual-numeric-locale"))
         {
            dualSortSettings.setNumberLocale(
              getRequired(list, opt));
         }
         else if (opt.equals("secondary-numeric-locale"))
         {
            secondarySortSettings.setNumberLocale(
              getRequired(list, opt));
         }
         else if (opt.equals("numeric-sort-pattern"))
         {
            sortSettings.setNumberFormat(
              replaceHexAndSpecial(getRequired(list, opt)));
         }
         else if (opt.equals("dual-numeric-sort-pattern"))
         {
            dualSortSettings.setNumberFormat(
              replaceHexAndSpecial(getRequired(list, opt)));
         }
         else if (opt.equals("secondary-numeric-sort-pattern"))
         {
            secondarySortSettings.setNumberFormat(
              replaceHexAndSpecial(getRequired(list, opt)));
         }
         else if (opt.equals("trim-sort"))
         {
            sortSettings.setTrim(getBoolean(list, opt));
            dualSortSettings.setTrim(sortSettings.isTrimOn());
            secondarySortSettings.setTrim(sortSettings.isTrimOn());
         }
         else if (opt.equals("dual-trim-sort"))
         {
            dualSortSettings.setTrim(getBoolean(list, opt));
         }
         else if (opt.equals("secondary-trim-sort"))
         {
            secondarySortSettings.setTrim(getBoolean(list, opt));
         }
         else if (opt.equals("letter-number-rule"))
         {
            sortSettings.setLetterNumberRule(
              getLetterNumberRule(list, opt));

            dualSortSettings.setLetterNumberRule(
              sortSettings.getLetterNumberRule());
            secondarySortSettings.setLetterNumberRule(
              sortSettings.getLetterNumberRule());
         }
         else if (opt.equals("dual-letter-number-rule"))
         {
            dualSortSettings.setLetterNumberRule(
              getLetterNumberRule(list, opt));
         }
         else if (opt.equals("secondary-letter-number-rule"))
         {
            secondarySortSettings.setLetterNumberRule(
              getLetterNumberRule(list, opt));
         }
         else if (opt.equals("letter-number-punc-rule"))
         {
            sortSettings.setLetterNumberPuncRule(
              getLetterNumberPuncRule(list, opt));

            dualSortSettings.setLetterNumberPuncRule(
              sortSettings.getLetterNumberPuncRule());
            secondarySortSettings.setLetterNumberPuncRule(
              sortSettings.getLetterNumberPuncRule());
         }
         else if (opt.equals("dual-letter-number-punc-rule"))
         {
            dualSortSettings.setLetterNumberPuncRule(
              getLetterNumberPuncRule(list, opt));
         }
         else if (opt.equals("secondary-letter-number-punc-rule"))
         {
            secondarySortSettings.setLetterNumberPuncRule(
              getLetterNumberPuncRule(list, opt));
         }
         else if (opt.equals("date-sort-format"))
         {
            sortSettings.setDateFormat(replaceHexAndSpecial(
               getRequired(list, opt)));
         }
         else if (opt.equals("dual-date-sort-format"))
         {
            dualSortSettings.setDateFormat(replaceHexAndSpecial(
               getRequired(list, opt)));
         }
         else if (opt.equals("secondary-date-sort-format"))
         {
            secondarySortSettings.setDateFormat(replaceHexAndSpecial(
               getRequired(list, opt)));
         }
         else if (opt.equals("date-sort-locale"))
         {
            sortSettings.setDateLocale(getRequired(list, opt));
         }
         else if (opt.equals("dual-date-sort-locale"))
         {
            dualSortSettings.setDateLocale(getRequired(list, opt));
         }
         else if (opt.equals("secondary-date-sort-locale"))
         {
            secondarySortSettings.setDateLocale(getRequired(list, opt));
         }
         else if (opt.equals("group"))
         {
            groupField = getOptional("auto", list, opt);

            if (groupField.equals("auto"))
            {
               groupField = null;
            }
         }
         else if (opt.equals("group-level"))
         {
            String val = getOptional("all", list, opt);

            try
            {
               if (val.equals("all"))
               {
                  groupLevelSetting = GROUP_LEVEL_SETTING_GREATER_THAN_EQ;
                  groupLevelSettingValue = 0;
               }
               else if (val.startsWith("<"))
               {
                  val=val.substring(1);
                  groupLevelSetting = GROUP_LEVEL_SETTING_LESS_THAN;

                  if (val.startsWith("="))
                  {
                     val=val.substring(1);
                     groupLevelSetting = GROUP_LEVEL_SETTING_LESS_THAN_EQ;
                  }

                  groupLevelSettingValue = Integer.parseInt(val);
               }
               else if (val.startsWith(">"))
               {
                  val=val.substring(1);
                  groupLevelSetting = GROUP_LEVEL_SETTING_GREATER_THAN;

                  if (val.startsWith("="))
                  {
                     val=val.substring(1);
                     groupLevelSetting = GROUP_LEVEL_SETTING_GREATER_THAN_EQ;
                  }

                  groupLevelSettingValue = Integer.parseInt(val);
               }
               else
               {
                  groupLevelSettingValue = Integer.parseInt(val);
                  groupLevelSetting = GROUP_LEVEL_SETTING_EXACT;
               }
            }
            catch (NumberFormatException e)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.int.value", opt, val), e);
            }
         }
         else if (opt.equals("merge-small-groups"))
         {
            mergeSmallGroupLimit = getOptionalInt(1, list, opt);
         }
         else if (opt.equals("shuffle"))
         {
            long seed = getOptionalLong(0L, list, opt);

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
            String method = getOptional("doc", list, opt);

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
            String field = getRequired(list, opt);

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
            String field = getRequired(list, opt);

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
            String field = getOptional(list, opt);

            if (field == null || field.isEmpty()
                 || isAllowedSortFallbackField(field, false))
            {
               sortSettings.setMissingFieldFallback(field);
            }
            else
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, field));
            }
         }
         else if (opt.equals("dual-missing-sort-fallback"))
         {
            String field = getOptional(list, opt);

            if (field == null || field.isEmpty()
                 || isAllowedSortFallbackField(field, false))
            {
               dualSortSettings.setMissingFieldFallback(field);
            }
            else
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, field));
            }
         }
         else if (opt.equals("secondary-missing-sort-fallback"))
         {
            String field = getOptional(list, opt);

            if (field == null || field.isEmpty()
                  || isAllowedSortFallbackField(field, false))
            {
               secondarySortSettings.setMissingFieldFallback(field);
            }
            else
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, field));
            }
         }
         else if (opt.equals("entry-sort-fallback"))
         {
            entryDefaultSortField = getRequired(list, opt);

            checkAllowedSortFallbackConcatenation(entryDefaultSortField, opt);
         }
         else if (opt.equals("abbreviation-sort-fallback"))
         {
            abbrevDefaultSortField = getRequired(list, opt);

            checkAllowedSortFallbackConcatenation(abbrevDefaultSortField, opt);
         }
         else if (opt.equals("symbol-sort-fallback"))
         {
            symbolDefaultSortField = getRequired(list, opt);

            checkAllowedSortFallbackConcatenation(symbolDefaultSortField, opt);
         }
         else if (opt.equals("bibtexentry-sort-fallback"))
         {
            bibTeXEntryDefaultSortField = getRequired(list, opt);

            checkAllowedSortFallbackConcatenation(bibTeXEntryDefaultSortField, opt);
         }
         else if (opt.equals("custom-sort-fallbacks"))
         {
            customEntryDefaultSortFields = getHashMap(list, opt);

            if (customEntryDefaultSortFields != null)
            {
               Set<String> keys = customEntryDefaultSortFields.keySet();

               for (Iterator<String> it1 = keys.iterator(); it1.hasNext(); )
               {
                  String key = it1.next();
                  String field = customEntryDefaultSortFields.get(key);

                  checkAllowedSortFallbackConcatenation(field, opt);
               }
            }
         }
         else if (opt.equals("field-concat-sep"))
         {
            fieldConcatenationSeparator = getOptional("", list, opt);

            if (fieldConcatenationSeparator != null)
            {
               fieldConcatenationSeparator = replaceHex(fieldConcatenationSeparator);
            }
         }
         else if (opt.equals("abbreviation-name-fallback"))
         {
            abbrevDefaultNameField = getRequired(list, opt);

            if ((!bib2gls.isKnownField(abbrevDefaultNameField)
             && !bib2gls.isKnownSpecialField(abbrevDefaultNameField))
             || abbrevDefaultNameField.equals("name")
             || abbrevDefaultNameField.equals("sort")
             || (
                   abbrevDefaultTextField.equals("name")
                   && abbrevDefaultNameField.equals("text")
                )
            )
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   abbrevDefaultNameField));
            }
         }
         else if (opt.equals("abbreviation-text-fallback"))
         {
            abbrevDefaultTextField = getRequired(list, opt);

            if ((!bib2gls.isKnownField(abbrevDefaultTextField)
             && !bib2gls.isKnownSpecialField(abbrevDefaultTextField))
             || abbrevDefaultTextField.equals("text")
             || abbrevDefaultTextField.equals("sort")
             || (
                   abbrevDefaultTextField.equals("name")
                   && abbrevDefaultNameField.equals("text")
                )
             )
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   abbrevDefaultTextField));
            }
         }
         else if (opt.equals("charset"))
         {
            bibCharset = Charset.forName(getRequired(list, opt));
         }
         else if (opt.equals("min-loc-range"))
         {
            minLocationRange = getRequiredIntGe(2, 
               "none", Integer.MAX_VALUE, list, opt);
         }
         else if (opt.equals("loc-gap"))
         {
            bib2gls.warning(bib2gls.getMessage("warning.deprecated",
              opt, "max-loc-diff"));
            locGap = getRequiredIntGe(1, list, opt);
         }
         else if (opt.equals("max-loc-diff"))
         {
            locGap = getRequiredIntGe(1, list, opt);
         }
         else if (opt.equals("suffixF"))
         {
            suffixF = getOptional("", list, opt);

            if (suffixF.equals("none"))
            {
               suffixF = null;
            }
         }
         else if (opt.equals("suffixFF"))
         {
            suffixFF = getOptional("", list, opt);

            if (suffixFF.equals("none"))
            {
               suffixFF = null;
            }
         }
         else if (opt.equals("compact-ranges"))
         {
            String val = getOptional("true", list, opt);

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
            String val = getChoice(list, opt, "omit", "before", "after");

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
            String val = getChoice(list, opt, "omit", "before", "after");

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
            String val = getChoice(list, opt, "omit", "before", "after");

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
         else if (opt.equals("prune-xr"))
         {// shortcut

            if (getBoolean(list, opt))
            {
               pruneSeePatterns = new HashMap<String,Pattern>();
               pruneSeePatterns.put("entrytype", Pattern.compile("index(plural)?"));
               pruneSeePatterns.put("see", Pattern.compile(""));
               pruneSeePatterns.put("seealso", Pattern.compile(""));
               pruneSeePatterns.put("alias", Pattern.compile(""));
               pruneSeeAlsoPatterns = new HashMap<String,Pattern>();
               pruneSeeAlsoPatterns.put("entrytype", Pattern.compile("index(plural)?"));
               pruneSeeAlsoPatterns.put("see", Pattern.compile(""));
               pruneSeeAlsoPatterns.put("seealso", Pattern.compile(""));
               pruneSeeAlsoPatterns.put("alias", Pattern.compile(""));
            }
            else
            {
               pruneSeePatterns = null;
               pruneSeeAlsoPatterns = null;
            }
         }
         else if (opt.equals("prune-iterations"))
         {
            pruneIterations = getRequiredIntGe(1, list, opt);

            if (pruneIterations > MAX_PRUNE_ITERATIONS)
            {
               bib2gls.warningMessage("warning.max-prune-iteration-cap",
                opt, MAX_PRUNE_ITERATIONS);
            }
         }
         else if (opt.equals("prune-see-op"))
         {
            String val = getChoice(list, opt, "and", "or");

            pruneSeePatternsAnd = val.equals("and");
         }
         else if (opt.equals("prune-see-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);

            if (array == null)
            {
               pruneSeePatterns = null;
            }
            else
            {
               pruneSeePatterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(pruneSeePatterns, array, opt, list);
            }
         }
         else if (opt.equals("prune-seealso-op"))
         {
            String val = getChoice(list, opt, "and", "or");

            pruneSeeAlsoPatternsAnd = val.equals("and");
         }
         else if (opt.equals("prune-seealso-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);

            if (array == null)
            {
               pruneSeeAlsoPatterns = null;
            }
            else
            {
               pruneSeeAlsoPatterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(pruneSeeAlsoPatterns, array, opt, list);
            }
         }
         else if (opt.equals("primary-loc-counters") 
               || opt.equals("principal-loc-counters"))
         {
            String val = getChoice(list, opt, "combine", "match", "split");

            if (val.equals("combine"))
            {
               primaryLocCounters = PRIMARY_LOCATION_COUNTERS_COMBINE;
            }
            else if (val.equals("match"))
            {
               primaryLocCounters = PRIMARY_LOCATION_COUNTERS_MATCH;
            }
            else// if (val.equals("split"))
            {
               primaryLocCounters = PRIMARY_LOCATION_COUNTERS_SPLIT;
            }
         }
         else if (opt.equals("loc-counters"))
         {
            String[] values = getStringArray(list, opt);

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
            String[] values = getStringArray("true", list, opt);

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
         else if (opt.equals("loc-prefix-def"))
         {
            String val = getChoice(list, opt,
              "global", "local", "individual");

            if (val.equals("global"))
            {
               locationPrefixDef = PROVIDE_DEF_GLOBAL;
            }
            else if (val.equals("local"))
            {
               locationPrefixDef = PROVIDE_DEF_LOCAL_ALL;
            }
            else if (val.equals("individual"))
            {
               locationPrefixDef = PROVIDE_DEF_LOCAL_INDIVIDUAL;
            }
         }
         else if (opt.equals("loc-suffix"))
         {
            String[] values = getStringArray("\\@.", list, opt);

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
         else if (opt.equals("loc-suffix-def"))
         {
            String val = getChoice(list, opt,
              "global", "local", "individual");

            if (val.equals("global"))
            {
               locationSuffixDef = PROVIDE_DEF_GLOBAL;
            }
            else if (val.equals("local"))
            {
               locationSuffixDef = PROVIDE_DEF_LOCAL_ALL;
            }
            else if (val.equals("individual"))
            {
               locationSuffixDef = PROVIDE_DEF_LOCAL_INDIVIDUAL;
            }
         }
         else if (opt.equals("ignore-fields"))
         {
            skipFields = getStringArray(list, opt);
         }
         else if (opt.equals("check-end-punctuation"))
         {
            checkEndPunc = getStringArray(list, opt);
         }
         else if (opt.equals("date-time-fields"))
         {
            dateTimeList = getStringArray(list, opt);
         }
         else if (opt.equals("date-fields"))
         {
            dateList = getStringArray(list, opt);
         }
         else if (opt.equals("time-fields"))
         {
            timeList = getStringArray(list, opt);
         }
         else if (opt.equals("date-time-field-format"))
         {
            dateTimeListFormat = replaceHexAndSpecial(getRequired(list, opt));
            dualDateTimeListFormat = dateTimeListFormat;
         }
         else if (opt.equals("dual-date-time-field-format"))
         {
            dualDateTimeListFormat = replaceHexAndSpecial(getRequired(list, opt));
         }
         else if (opt.equals("date-field-format"))
         {
            dateListFormat = replaceHexAndSpecial(getRequired(list, opt));
            dualDateListFormat = dateListFormat;
         }
         else if (opt.equals("dual-date-field-format"))
         {
            dualDateListFormat = replaceHexAndSpecial(getRequired(list, opt));
         }
         else if (opt.equals("time-field-format"))
         {
            timeListFormat = replaceHexAndSpecial(getRequired(list, opt));
            dualTimeListFormat = timeListFormat;
         }
         else if (opt.equals("dual-time-field-format"))
         {
            dualTimeListFormat = replaceHexAndSpecial(getRequired(list, opt));
         }
         else if (opt.equals("date-time-field-locale"))
         {
            dateTimeListLocale = getLocale(list, opt);
            dualDateTimeListLocale = dateTimeListLocale;
         }
         else if (opt.equals("dual-date-time-field-locale"))
         {
            dualDateTimeListLocale = getLocale(list, opt);
         }
         else if (opt.equals("date-field-locale"))
         {
            dateListLocale = getLocale(list, opt);
            dualDateListLocale = dateListLocale;
         }
         else if (opt.equals("dual-date-field-locale"))
         {
            dualDateListLocale = getLocale(list, opt);
         }
         else if (opt.equals("time-field-locale"))
         {
            timeListLocale = getLocale(list, opt);
            dualTimeListLocale = timeListLocale;
         }
         else if (opt.equals("dual-time-field-locale"))
         {
            dualTimeListLocale = getLocale(list, opt);
         }
         else if (opt.equals("locale"))
         {
            resourceLocale = null;
            resourceLocale = getLocale(list, opt);

            if (resourceLocale != null)
            {
               bib2gls.verboseMessage("message.resource_locale", resourceLocale);
            }
         }
         else if (opt.equals("interpret-fields"))
         {
            interpretFields = getFieldArray(list, opt, true);

            if (interpretFields != null && !bib2gls.useInterpreter())
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.option.requires.interpreter", opt));
            }
         }
         else if (opt.equals("interpret-fields-action"))
         {
            String val = getChoice(list, opt, "replace", "replace non empty");

            if (val.equals("replace"))
            {
               interpretFieldAction = INTERPRET_FIELD_ACTION_REPLACE;
            }
            else // if (val.equals("replace non empty"))
            {
               interpretFieldAction = INTERPRET_FIELD_ACTION_REPLACE_NON_EMPTY;
            }
         }
         else if (opt.equals("hex-unicode-fields"))
         {
            hexUnicodeFields = getFieldArray(list, opt, true);
         }
         else if (opt.equals("bibtex-contributor-fields"))
         {
            bibtexAuthorList = getFieldArray(list, opt, true);
         }
         else if (opt.equals("contributor-order"))
         {
            String val = getChoice(list, opt, 
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
            String val = getChoice(list, opt, SELECTION_OPTIONS);

            selectionMode = -1;

            for (int i = 0; i < SELECTION_OPTIONS.length; i++)
            {
               if (val.equals(SELECTION_OPTIONS[i]))
               {
                  selectionMode = i;
                  break;
               }
            }

            if (selectionMode == SELECTION_SELECTED_BEFORE 
                 && bib2gls.anyEntriesSelected())
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.selected_before.none_selected", val));
            }
         }
         else if (opt.equals("break-at"))
         {
            sortSettings.setBreakPoint(getBreakAt(list, opt));
            dualSortSettings.setBreakPoint(sortSettings.getBreakPoint());
            secondarySortSettings.setBreakPoint(sortSettings.getBreakPoint());
         }
         else if (opt.equals("dual-break-at"))
         {
            dualSortSettings.setBreakPoint(getBreakAt(list, opt));
         }
         else if (opt.equals("secondary-break-at"))
         {
            secondarySortSettings.setBreakPoint(getBreakAt(list, opt));
         }
         else if (opt.equals("break-marker"))
         {
            sortSettings.setBreakPointMarker(getOptional("", list, opt));
            dualSortSettings.setBreakPointMarker(sortSettings.getBreakPointMarker());
            secondarySortSettings.setBreakPointMarker(sortSettings.getBreakPointMarker());
         }
         else if (opt.equals("dual-break-marker"))
         {
            dualSortSettings.setBreakPointMarker(getOptional("", list, opt));
         }
         else if (opt.equals("secondary-break-marker"))
         {
            secondarySortSettings.setBreakPointMarker(getOptional("", list, opt));
         }
         else if (opt.equals("break-at-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            HashMap<String,Pattern> patterns = null;

            if (array != null)
            {
               patterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(patterns, array, opt, list);
            }

            sortSettings.setBreakAtMatch(patterns);
            dualSortSettings.setBreakAtNotMatch(patterns);
            secondarySortSettings.setBreakAtNotMatch(patterns);
         }
         else if (opt.equals("break-at-not-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            HashMap<String,Pattern> patterns = null;

            if (array != null)
            {
               patterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(patterns, array, opt, list);
            }

            sortSettings.setBreakAtNotMatch(patterns);
            dualSortSettings.setBreakAtNotMatch(patterns);
            secondarySortSettings.setBreakAtNotMatch(patterns);
         }
         else if (opt.equals("break-at-match-op"))
         {
            String val = getChoice(list, opt, "and", "or");

            boolean and = val.equals("and");
            sortSettings.setBreakAtNotMatchAnd(and);
            dualSortSettings.setBreakAtNotMatchAnd(and);
            secondarySortSettings.setBreakAtNotMatchAnd(and);
         }
         else if (opt.equals("dual-break-at-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            HashMap<String,Pattern> patterns = null;

            if (array != null)
            {
               patterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(patterns, array, opt, list);
            }

            dualSortSettings.setBreakAtNotMatch(patterns);
         }
         else if (opt.equals("dual-break-at-not-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            HashMap<String,Pattern> patterns = null;

            if (array != null)
            {
               patterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(patterns, array, opt, list);
            }

            dualSortSettings.setBreakAtNotMatch(patterns);
         }
         else if (opt.equals("dual-break-at-match-op"))
         {
            String val = getChoice(list, opt, "and", "or");

            dualSortSettings.setBreakAtNotMatchAnd(val.equals("and"));
         }
         else if (opt.equals("secondary-break-at-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            HashMap<String,Pattern> patterns = null;

            if (array != null)
            {
               patterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(patterns, array, opt, list);
            }

            secondarySortSettings.setBreakAtNotMatch(patterns);
         }
         else if (opt.equals("secondary-break-at-not-match"))
         {
            TeXObject[] array = getTeXObjectArray(list, opt, true);
            HashMap<String,Pattern> patterns = null;

            if (array != null)
            {
               patterns = new HashMap<String,Pattern>();
               setFieldMatchPatterns(patterns, array, opt, list);
            }

            secondarySortSettings.setBreakAtNotMatch(patterns);
         }
         else if (opt.equals("secondary-break-at-match-op"))
         {
            String val = getChoice(list, opt, "and", "or");

            secondarySortSettings.setBreakAtNotMatchAnd(val.equals("and"));
         }
         else if (opt.equals("strength"))
         { // collator strength

            String val = getChoice(list, opt, "primary", "secondary",
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

            String val = getChoice(list, opt, "primary", "secondary",
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

            String val = getChoice(list, opt, "primary", "secondary",
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

            String val = getChoice(list, opt, "none", "canonical",
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

            String val = getChoice(list, opt, "none", "canonical",
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

            String val = getChoice(list, opt, "none", "canonical",
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
         else if (opt.equals("compound-options-global"))
         {
            compoundEntriesGlobal = getBoolean(list, opt);
         }
         else if (opt.equals("compound-dependent"))
         {
            compoundEntriesDependent = getBoolean(list, opt);
         }
         else if (opt.equals("compound-add-hierarchy"))
         {
            compoundEntriesAddHierarchy = getBoolean(list, opt);
         }
         else if (opt.equals("compound-has-records"))
         {
            String val = getChoice("true", list, opt,
               "false", "true", "default");

            if (val.equals("false"))
            {
               compoundEntriesHasRecords = COMPOUND_MGLS_RECORDS_FALSE;
            }
            else if (val.equals("true"))
            {
               compoundEntriesHasRecords = COMPOUND_MGLS_RECORDS_TRUE;
            }
            else// if (val.equals("default"))
            {
               compoundEntriesHasRecords = COMPOUND_MGLS_RECORDS_DEFAULT;
            }
         }
         else if (opt.equals("compound-adjust-name"))
         {
            String val = getChoice("once", list, opt,
               "false", "once", "unique");

            if (val.equals("once"))
            {
               compoundAdjustName = COMPOUND_ADJUST_NAME_ONCE;
            }
            else if (val.equals("unique"))
            {
               compoundAdjustName = COMPOUND_ADJUST_NAME_UNIQUE;
            }
            else// if (val.equals("false"))
            {
               compoundAdjustName = COMPOUND_ADJUST_NAME_FALSE;
            }
         }
         else if (opt.equals("compound-write-def"))
         {
            String val = getChoice(list, opt, "none", "all", "ref");

            if (val.equals("none"))
            {
               compoundEntriesDef = COMPOUND_DEF_FALSE;
            }
            else if (val.equals("all"))
            {
               compoundEntriesDef = COMPOUND_DEF_ALL;
            }
            else// if (val.equals("ref"))
            {
               compoundEntriesDef = COMPOUND_DEF_REFD;
            }
         }
         else if (opt.equals("compound-main-type"))
         {
            compoundMainType = getRequired(list, opt);
         }
         else if (opt.equals("compound-other-type"))
         {
            compoundOtherType = getRequired(list, opt);
         }
         else if (opt.equals("compound-type-override"))
         {
            compoundTypeOverride = getBoolean(list, opt);
         }
         else if (opt.equals("wordify-math-greek"))
         {
            wordifyMathGreek = getBoolean(list, opt);
         }
         else if (opt.equals("wordify-math-symbol"))
         {
            wordifyMathSymbol = getBoolean(list, opt);
         }
         else
         {
            throw new IllegalArgumentException(
             bib2gls.getMessage("error.syntax.unknown_option", opt));
         }
      }

      if (prefixFieldExceptions == null)
      {
         prefixFieldExceptions = new Vector<Integer>();
         prefixFieldExceptions.add(0x27);
         prefixFieldExceptions.add(0x2D);
         prefixFieldExceptions.add(0x7E);
         prefixFieldExceptions.add(0x2010);
         prefixFieldExceptions.add(0x2011);
         prefixFieldExceptions.add(0x2019);
      }

      if (prefixFieldCsExceptions == null)
      {
         prefixFieldCsExceptions = new Vector<String>();
         prefixFieldCsExceptions.add("space");
         prefixFieldCsExceptions.add("nobreakspace");
         prefixFieldCsExceptions.add(" ");
      }

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.logMessageNoLn("append-prefix-field-exceptions:");

         for (Integer num : prefixFieldExceptions)
         {
            int cp = num.intValue();

            bib2gls.logMessageNoLn(String.format(" %s (0x%X)", 
              new String(Character.toChars(cp)), cp));
         }

         bib2gls.logMessage();

         bib2gls.logMessageNoLn("append-prefix-field-cs-exceptions:");

         for (String csname : prefixFieldCsExceptions)
         {
            bib2gls.logMessageNoLn(String.format(" \\%s", csname));
         }

         bib2gls.logMessage(".");
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

            parseSupplemental(supplemental[0]);
         }
         else
         {
            for (String suppRef : supplemental)
            {
               parseSupplemental(suppRef);
            }
         }
      }

      if (master != null)
      {
         parseMaster(master);
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

      if (writeAction != WRITE_ACTION_DEFINE && writeAction != WRITE_ACTION_PROVIDE)
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

      if ((pruneSeePatterns != null || pruneSeeAlsoPatterns != null)
          && !(selectionMode == SELECTION_RECORDED_AND_DEPS 
            || selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
            || selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO))
      {
         bib2gls.warning(
            bib2gls.getMessage("warning.option.clash",
            "selection="+SELECTION_OPTIONS[selectionMode],
            pruneSeePatterns != null ? "prune-see-match" : "prune-seealso-match"));

         pruneSeePatterns=null;
         pruneSeeAlsoPatterns=null;
      }

      if ((selectionMode == SELECTION_ALL 
             || selectionMode == SELECTION_SELECTED_BEFORE)
           && sortSettings.isOrderOfRecords())
      {
         bib2gls.warning(
            bib2gls.getMessage("warning.option.clash",
            "selection="+SELECTION_OPTIONS[selectionMode],
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

      boolean addDualPrefixes = bib2gls.arePrefixFieldsKnown();

      if (dualEntryMap == null)
      {
         dualEntryMap = new HashMap<String,String>();
         dualEntryMap.put("name", "description");
         dualEntryMap.put("plural", "descriptionplural");
         dualEntryMap.put("description", "name");
         dualEntryMap.put("descriptionplural", "plural");

         if (addDualPrefixes)
         {// add maps for prefix fields
            addPrefixMaps(dualEntryMap);
         }

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

         if (addDualPrefixes)
         {// add maps for prefix fields
            addPrefixMaps(dualAbbrevMap);
         }

         dualAbbrevFirstMap = "short";
      }

      if (dualAbbrevEntryMap == null)
      {
         dualAbbrevEntryMap = new HashMap<String,String>();
         dualAbbrevEntryMap.put("long", "name");
         dualAbbrevEntryMap.put("longplural", "plural");
         dualAbbrevEntryMap.put("short", "text");

         if (addDualPrefixes)
         {// add maps for prefix fields
            addPrefixMaps(dualAbbrevEntryMap);
         }

         dualAbbrevEntryFirstMap = "long";
      }

      if (dualSymbolMap == null)
      {
         dualSymbolMap = new HashMap<String,String>();
         dualSymbolMap.put("name", "symbol");
         dualSymbolMap.put("plural", "symbolplural");
         dualSymbolMap.put("symbol", "name");
         dualSymbolMap.put("symbolplural", "plural");

         if (addDualPrefixes)
         {// add maps for prefix fields
            addPrefixMaps(dualSymbolMap);
         }

         dualSymbolFirstMap = "name";
      }

      if (dualIndexEntryMap == null)
      {
         dualIndexEntryMap = new HashMap<String,String>();
         dualIndexEntryMap.put("name", "name");

         if (addDualPrefixes)
         {// add maps for prefix fields
            addPrefixMaps(dualIndexEntryMap);
         }

         dualIndexEntryFirstMap = "name";
      }

      if (dualIndexSymbolMap == null)
      {
         dualIndexSymbolMap = new HashMap<String,String>();
         dualIndexSymbolMap.put("symbol", "name");
         dualIndexSymbolMap.put("name", "symbol");
         dualIndexSymbolMap.put("symbolplural", "plural");
         dualIndexSymbolMap.put("plural", "symbolplural");

         if (addDualPrefixes)
         {// add maps for prefix fields
            addPrefixMaps(dualIndexSymbolMap);
         }

         dualIndexSymbolFirstMap = "symbol";
      }

      if (dualIndexAbbrevMap == null)
      {
         dualIndexAbbrevMap = new HashMap<String,String>();
         dualIndexAbbrevMap.put("name", "name");

         if (addDualPrefixes)
         {// add maps for prefix fields
            addPrefixMaps(dualIndexAbbrevMap);
         }

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

      if (bib2gls.isVerbose())
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

   /**
    * Parses the aux file of the master document. The master
    * document contains the actual glossary.
    * @param master the basename of the master aux file
    * @throws Bib2GlsException invalid resource setting syntax
    * @throws IOException may be thrown by the aux file parser
    */
   private void parseMaster(String master)
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
           notMatch ? "match" : "not-match"));
      }

      if (pruneSeePatterns != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "prune-see-match"));
      }

      if (pruneSeeAlsoPatterns != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "prune-seealso-match"));
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
           "dual-sort-field"));
      }

      if (!"doc".equals(sortSettings.getMethod()))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "sort"));
      }

      if (!sortSettings.getSortField().equals("sort"))
      {
         bib2gls.warning(bib2gls.getMessage("warning.option.clash", "master", 
           "sort-field"));
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

   /**
    * Parses the aux file of a supplemental document to add
    * supplemental records to entries shared in both documents.
    * @param basename the basename of the supplemental aux file
    * @throws IOException may be thrown by the aux file parser
    */
   private void parseSupplemental(String basename)
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

   /**
    * Indicates whether or not the given field can be referenced.
    * For example, where a field needs to be given as a value to a setting.
    * @param field the field name
    * @return true if the given field can be referenced otherwise false
    */
   private boolean isReferencableField(String field)
   {
      if (bib2gls.isKnownField(field)) return true;

      if (additionalUserFields != null)
      {
         for (String f : additionalUserFields)
         {
            if (f.equals(field)) return true;
         }
      }

      return (saveDefinitionIndex && field.equals(DEFINITION_INDEX_FIELD));
   }

   /**
    * Initialises field match patterns. This method is used by the
    * field match settings (such as {@code match} or {@code not-match}).
    * @param patterns map in which to save the patterns
    * @param array set of patterns supplied to the setting
    * @param opt the name of the setting
    * @param list the key=value setting list
    * @throws IllegalArgumentException invalid value supplied
    * @throws IOException may be thrown by the aux file parser
    */
   private void setFieldMatchPatterns(HashMap<String,Pattern> patterns,
     TeXObject[] array, String opt, KeyValList list)
   throws IllegalArgumentException,IOException
   {
      for (int i = 0; i < array.length; i++)
      {
         if (!(array[i] instanceof TeXObjectList))
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.opt.value", 
               opt, list.get(opt).toString(parser)));
         }

         Vector<TeXObject> split = splitList('=', 
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

         Pattern p = patterns.get(field);

         try
         {
            if (p == null)
            {
               p = Pattern.compile(val);
            }
            else
            {
               p = Pattern.compile(String.format(
                      "(?:%s)|(?:%s)", p.pattern(), val));
            }

            patterns.put(field, p);
         }
         catch (PatternSyntaxException e)
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.opt.keylist.pattern", 
               field, val, opt), e);
         }
      }
   }

   /**
    * Checks if the concatenated fields are allowed as a fallback
    * for the sort field.
    * @param fields list of fields separated by {@code +}
    * @param opt the name of the setting (for error reporting)
    * @throws IllegalArgumentException not permitted
    */
   private void checkAllowedSortFallbackConcatenation(String fields, String opt)
    throws IllegalArgumentException
   {
      String[] split = fields.split("\\s*\\+\\s*");

      for (String field : split)
      {
         if (!isAllowedSortFallbackField(field, true))
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.field", field, opt));
         }
      }
   }

   /**
    * Gets the separator for field concatenation. This is the
    * separator that will be used when forming the concatenated value,
    * if it's required.
    * @return the separator
    */
   public String getFieldConcatenationSeparator()
   {
      return fieldConcatenationSeparator;
   }

   /**
    * Determines whether or not the given field may be used as a
    * fallback for the sort field.
    * @param field the field name
    * @param allowKeywords if true, allow keywords "id" and "original id"
    * as pseudo fields
    * @return true if the field is allowed as a sort fallback
    */
   private boolean isAllowedSortFallbackField(String field, boolean allowKeywords)
   {
      if (allowKeywords && (field.equals("id") || field.equals("original id")))
      {
         return true;
      }

      // Don't allow unknown fields (to guard against typos).
      // The fallback field can't be "sort" as it will cause
      // infinite recursion.

      if (!bib2gls.isKnownField(field)
             && !bib2gls.isKnownSpecialField(field))
      {
         return false;
      }

      return field.equals("sort") ? false : true;
   }

   /**
    * Replaces TeX control-symbol sequences and hex markup with Unicode
    * characters.
    * @param original the original string
    * @return the substituted string
    */ 
   private String replaceHexAndSpecial(String original)
   {
      return replaceHex(replaceEscapeSpecialChar(original));
   }

   /**
    * Replaces standard TeX control-symbol sequences.
    * @param original the original string
    * @return the substituted string
    */ 
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

   /**
    * Replaces hex markup with Unicode characters.
    * @param original the original string
    * @return the substituted string
    */ 
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

   /**
    * Gets the boolean value assigned to the given setting.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is invalid
    * @return true if value is empty or "true" or false if the value
    * is "false"
    */
   private boolean getBoolean(KeyValList list, String opt)
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

   /**
    * Gets the (required TeXObject) value assigned to the given setting.
    * The value is required and will have leading and trailing
    * spaces removed.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is missing
    * @return the value
    */
   private TeXObject getRequiredObject(KeyValList list, String opt)
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
         obj = ((TeXObjectList)obj).trim();
      }

      return obj;
   }

   /**
    * Gets the (required string) value assigned to the given setting.
    * The value is required and will have leading and trailing
    * spaces removed.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is missing or empty
    * @return the value converted to a string
    */
   private String getRequired(KeyValList list, String opt)
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
         obj = ((TeXObjectList)obj).trim();
      }

      String value = obj.toString(parser).trim();

      if (value.isEmpty())
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.missing.value", opt));
      }

      return value;
   }

   /**
    * Gets the (required string) value assigned to the given setting or 
    * null if the keyword "false" provided.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is missing or empty
    * @return the value converted to a string or null if the value equals "false"
    */
   private String getRequiredOrFalse(KeyValList list, String opt)
    throws IOException
   {
      String val = getRequired(list, opt);

      if (val == null || val.equals("false")) return null;

      return val;
   }

   /**
    * Gets the (optional string) value assigned to the given setting.
    * The value is optional and will have leading and trailing
    * spaces removed.
    * @param defValue the default value
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @return the value converted to a string or the default value
    * if not set
    */
   private String getOptional(String defValue, 
      KeyValList list, String opt)
    throws IOException
   {
      TeXObject obj = list.getValue(opt);

      if (obj == null || obj instanceof MissingValue) return defValue;

      if (obj instanceof TeXObjectList)
      {
         obj = ((TeXObjectList)obj).trim();
      }

      String value = obj.toString(parser).trim();

      if (value.isEmpty())
      {
         return defValue;
      }

      return value;
   }

   /**
    * Gets the (optional string) value assigned to the given setting.
    * The value is optional and will have leading and trailing
    * spaces removed.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @return the value converted to a string or null if not set
    */
   private String getOptional(KeyValList list, String opt)
    throws IOException
   {
      return getOptional(null, list, opt);
   }

   /**
    * Gets the (optional string) value assigned to the given setting or 
    * null if the keyword "false" provided.
    * @param defValue the default value
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @return the value converted to a string or the default value
    * if not set or null if the value equals "false"
    */
   private String getOptionalOrFalse(String defValue, 
      KeyValList list, String opt)
    throws IOException
   {
      String val = getOptional(defValue, list, opt);

      if (val == null || val.equals("false")) return null;

      return val;
   }

   /**
    * Gets the (required integer) value assigned to the given setting.
    * The value is required and must be in integer format. Leading
    * and trailing spaces will be removed.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is missing or empty
    * or isn't an integer
    * @return the value converted to an int
    */
   private int getRequiredInt(KeyValList list, String opt)
    throws IOException
   {
      String value = getRequired(list, opt);

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

   /**
    * Gets the (optional integer) value assigned to the given setting.
    * The value, if present, and must be in integer format. Leading
    * and trailing spaces will be removed.
    * @param defValue the default value
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't an integer
    * @return the value converted to an int or the default value if
    * not set
    */
   private int getOptionalInt(int defValue, KeyValList list, 
     String opt)
    throws IOException
   {
      String value = getOptional(list, opt);

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

   /**
    * Gets the (required integer) value assigned to the given setting.
    * The value is required and must be in integer format. Leading
    * and trailing spaces will be removed. The value may be a
    * keyword which has a corresponding int value.
    * @param keyword the keyword that may be used instead of a
    * number
    * @param keywordValue the numeric value associated with the
    * keyword
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is missing or empty
    * or isn't an integer
    * @return the value converted to an int
    */
   private int getRequiredInt(String keyword, 
       int keywordValue, KeyValList list, String opt)
    throws IOException
   {
      String value = getRequired(list, opt);

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

   /**
    * Gets the (required constrained integer) value assigned to the given setting.
    * The value is required and must be in integer format. Leading
    * and trailing spaces will be removed. The value must be greater
    * than or equal to the given minimum value.
    * @param minValue the minimum allowed value
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is missing or empty
    * or isn't an integer or is out of bounds
    * @return the value converted to an int
    */
   private int getRequiredIntGe(int minValue, KeyValList list,
      String opt)
    throws IOException
   {
      int val = getRequiredInt(list, opt);

      if (val < minValue)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.minint.value", opt, val, 
             minValue));
      }

      return val;
   }

   /**
    * Gets the (required constrained integer) value assigned to the given setting.
    * The value is required and must be in integer format. Leading
    * and trailing spaces will be removed. The value may be a
    * keyword which has a corresponding int value. The value must be greater
    * than or equal to the given minimum value.
    * @param minValue the minimum allowed value
    * @param keyword the keyword that may be used instead of a
    * number
    * @param keywordValue the numeric value associated with the
    * keyword
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is missing or empty
    * or isn't an integer or is out of bounds
    * @return the value converted to an int
    */
   private int getRequiredIntGe(int minValue, String keyword, 
      int keywordValue, KeyValList list, String opt)
    throws IOException
   {
      int val = getRequiredInt(keyword, keywordValue, list, opt);

      if (val < minValue)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.minint.value", opt, val, 
             minValue));
      }

      return val;
   }

   /**
    * Gets the (optional constrained integer) value assigned to the given setting.
    * The value is must be in integer format. Leading
    * and trailing spaces will be removed. The value must be greater
    * than or equal to the given minimum value.
    * @param minValue the minimum allowed value
    * @param defValue the default value
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't an integer or is out of bounds
    * @return the value converted to an int or the default value if
    * not set
    */
   private int getOptionalIntGe(int minValue, int defValue, 
     KeyValList list, String opt)
    throws IOException
   {
      int val = getOptionalInt(defValue, list, opt);

      if (val < minValue)
      {
         throw new IllegalArgumentException(
           bib2gls.getMessage("error.invalid.opt.minint.value", opt, val, 
             minValue));
      }

      return val;
   }

   /**
    * Gets the (required long integer) value assigned to the given setting.
    * The value is required and must be in long integer format. Leading
    * and trailing spaces will be removed.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is missing or empty
    * or isn't a long integer
    * @return the value converted to a long
    */
   private long getRequiredLong(KeyValList list, String opt)
    throws IOException
   {
      String value = getRequired(list, opt);

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

   /**
    * Gets the (optional long integer) value assigned to the given setting.
    * The value, if present, and must be in long integer format. Leading
    * and trailing spaces will be removed.
    * @param defValue the default value
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't a long integer
    * @return the value converted to a long or the default value if
    * not set
    */
   private long getOptionalLong(long defValue, 
     KeyValList list, String opt)
    throws IOException
   {
      String value = getOptional(list, opt);

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

   /**
    * Gets the optional value (which must belong to a set) assigned to the
    * given setting.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param allowedValues list of allowed values
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't in the
    * allowed set
    * @return the value as a string or null if not set
    */ 
   private String getChoice(KeyValList list, String opt, 
      String... allowValues)
    throws IOException
   {
      return getChoice(null, list, opt, allowValues);
   }

   /**
    * Gets the optional value (which must belong to a set) assigned to the
    * given setting.
    * @param defVal the default value
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param allowedValues list of allowed values
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't in the
    * allowed set
    * @return the value as a string or the default value if not set
    */ 
   private String getChoice(String defVal, KeyValList list, 
      String opt, String... allowValues)
    throws IOException
   {
      String value = getOptional(list, opt);

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

   /**
    * Gets the (optional locale) value assigned to the given setting.
    * The value is optional and will have leading and trailing
    * spaces removed. The value should be a valid ISO language tag
    * or the keywords "doc" or "locale" or "resource". A missing value corresponds
    * to "resource". A return value of null indicates the default
    * locale, according to the JVM.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @return the value converted to a locale or null
    */
   private Locale getLocale(KeyValList list, String opt)
    throws IOException
   {
      String value = getOptional(list, opt);

      if (value == null || "resource".equals(value))
      {
         return getResourceLocale();
      }
      else if ("locale".equals(value))
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

   /**
    * Gets the default locale for this resource set.
    * A return value of null indicates the default
    * locale, according to the JVM.
    */ 
   public Locale getResourceLocale()
   {
      if (resourceLocale == null)
      {
         resourceLocale = bib2gls.getDefaultLocale();

         if (resourceLocale == null)
         {
            return Locale.getDefault();
         }
      }

      return resourceLocale;
   }

   /**
    * Gets the (optional) array of string values assigned to the given setting.
    * The array of values should be supplied as a comma-separated list.
    * @param defValue the default value
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @return the array of string values
    */
   private String[] getStringArray(String defValue, 
     KeyValList list, String opt)
    throws IOException
   {
      String[] array = getStringArray(list, opt);

      if (array == null)
      {
         return new String[] {defValue};
      }

      return array;
   }

   /**
    * Gets the (optional) array of string values assigned to the given setting.
    * The array of values should be supplied as a comma-separated list.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @return the array of string values or null if not set
    */
   private String[] getStringArray(KeyValList list, 
     String opt)
    throws IOException
   {
      return getStringArray(list, opt, false);
   }

   /**
    * Gets the array of string values assigned to the given setting.
    * The array of values should be supplied as a comma-separated list.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param isRequired true if the value must be supplied
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't supplied
    * but is required
    * @return the array of string values or (if permitted) null if not set or empty
    */
   private String[] getStringArray(KeyValList list, 
     String opt, boolean isRequired)
    throws IOException
   {
      TeXObject object;

      if (isRequired)
      {
         object = getRequiredObject(list, opt);
      }
      else
      {
         object = list.getValue(opt);
      }

      if (object instanceof TeXObjectList)
      {
         object = ((TeXObjectList)object).trim();
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
            obj = ((TeXObjectList)obj).trim();
         }

         array[i] = obj.toString(parser).trim();

      }

      return array;
   }

   /**
    * Gets the array of field names assigned to the given setting.
    * The array of values should be supplied as a comma-separated list
    * and each item must be a valid field name.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param isRequired true if the value must be supplied
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't supplied
    * but is required
    * @return the array of field names or (if permitted) null if not set or empty
    */
   private String[] getFieldArray(KeyValList list, 
     String opt, boolean isRequired)
    throws IOException
   {
      String[] array = getStringArray(list, opt, isRequired);

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

   /**
    * Gets the array of TeX objects assigned to the given setting.
    * The array of values should be supplied as a comma-separated list.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param isRequired true if the value must be supplied
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't supplied
    * but is required
    * @return the array of objects or (if permitted) null if not set or empty
    */
   private TeXObject[] getTeXObjectArray(KeyValList list, 
     String opt, boolean isRequired)
    throws IOException
   {
      TeXObject obj;

      if (isRequired)
      {
         obj = getRequiredObject(list, opt);
      }
      else
      {
         obj = list.getValue(opt);
      }

      if (obj instanceof TeXObjectList)
      {
         obj = ((TeXObjectList)obj).trim();
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
            obj = ((TeXObjectList)obj).trim();
         }

         array[i] = obj;
      }

      return array;
   }

   /**
    * Gets the CSV list of TeX objects assigned to the given setting.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param isRequired true if the value must be supplied
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't supplied
    * but is required
    * @return the CSV list of objects or (if permitted) null if not set or empty
    */
   private CsvList[] getListArray(KeyValList list, 
     String opt)
    throws IOException
   {
      TeXObject object = list.getValue(opt);

      if (object instanceof TeXObjectList)
      {
         object = ((TeXObjectList)object).trim();
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
            obj = ((TeXObjectList)obj).trim();
         }

         array[i] = CsvList.getList(parser, obj);
      }

      return array;
   }

   /**
    * Gets the required dual map assigned to the given setting.
    * The format of a dual map value is two sublists of equal size
    * separated by a comma, where each element of the sublists must
    * be a valid field name ("alias" not permitted). The result is
    * a map from a field in the first sublist to the corresponding
    * field in the second sublist.
    *
    * The map keys will be stored in the supplied array in the order
    * in which they were read in. (The hash map doesn't retain
    * order.) The key array length may be less than the total
    * number of keys, in which case only the initial keys up will be
    * saved. If the array is larger than the number of keys then the
    * tail elements of the array won't be assigned.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param keys the array in which to store the keys
    * @throws IOException may be thrown by the aux file parser
    * @return the value converted to a string or the default value
    * if not set
    */
   private HashMap<String,String> getDualMap(KeyValList list, 
     String opt, String[] keys)
    throws IOException
   {
      CsvList[] array = getListArray(list, opt);

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
            obj1 = ((TeXObjectList)obj1).trim();
         }

         if (obj2 instanceof TeXObjectList)
         {
            obj2 = ((TeXObjectList)obj2).trim();
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

   /**
    * Gets the key=value list assigned to the given setting.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @return the key=value list corresponding to the setting or
    * null if not set or empty
    */
   private KeyValList getKeyValList(KeyValList list, 
     String opt)
    throws IOException
   {
      TeXObject val = list.getValue(opt);

      if (val instanceof TeXObjectList)
      {
         val = ((TeXObjectList)val).trim();
      }

      KeyValList sublist = KeyValList.getList(parser, val);

      if (sublist == null || sublist.size() == 0)
      {
         return null;
      }

      return sublist;
   }

   /**
    * Gets the (string-to-string) mapping assigned to the given setting.
    * The setting value should be a key=value list.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @return the string map corresponding to the setting or
    * null if not set or empty
    */
   private HashMap<String,String> getHashMap(KeyValList list, String opt)
    throws IOException
   {
      TeXObject object = list.getValue(opt);

      if (object instanceof TeXObjectList)
      {
         object = ((TeXObjectList)object).trim();
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

   /**
    * Gets the (string-to-vector) mapping assigned to the given setting.
    * The setting value should be a key=value list, where each
    * element value is a comma-separated list.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param isRequired true if the value must be supplied
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value isn't supplied
    * but is required
    * @return the map corresponding to the setting or (if allowed)
    * null if not set or empty
    */
   private HashMap<String,Vector<String>> getHashMapVector(
      KeyValList list, String opt, boolean isRequired)
    throws IOException
   {
      TeXObject[] array = getTeXObjectArray(list, opt, isRequired);

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

         Vector<TeXObject> split = splitList('=', 
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
               obj = ((TeXObjectList)obj).trim();
            }

            valList.add(obj.toString(parser).trim());
         }

         map.put(field, valList);
      }

      return map;
   }

   /**
    * Gets the vector of substitution patterns assigned to the given setting.
    * The setting value should be supplied as a comma-separated list of 
    * grouped pairs. The first group is the regular expression and the second
    * group is the replacement.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @param isRequired true if the value must be supplied
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is invalid or isn't supplied
    * but is required
    * @return the vector of substitution patterns or (if permitted) 
    * null if not set or empty
    */
   private Vector<PatternReplace> getSubstitutionList( 
      KeyValList list, String opt, boolean isRequired)
    throws IOException
   {
      TeXObject[] array = getTeXObjectArray(list, opt, isRequired);

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
                     TeXNumber num =
                       ((TeXObjectList)replacementArg).popNumber(parser, 16);
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

   /**
    * Gets the (string-to-string) mapping of field format patterns assigned to the given setting.
    * The setting value should be a key=value list, where the key
    * must be a valid field name and the value must be a valid
    * format pattern.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is invalid
    * @return the string map corresponding to the setting or
    * null if not set or empty
    */
   private HashMap<String,String> getFieldFormatPattern(
     KeyValList list, String opt)
   throws IllegalArgumentException,IOException
   {
      TeXObject[] array = getTeXObjectArray(list, opt, true);

      if (array == null)
      {
         return null;
      }

      HashMap<String,String> formatMap = new HashMap<String,String>();

      for (int i = 0; i < array.length; i++)
      {
         if (!(array[i] instanceof TeXObjectList))
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.opt.value", 
               opt, list.get(opt).toString(parser)));
         }

         Vector<TeXObject> split = splitList('=', 
            (TeXObjectList)array[i]);

         if (split == null || split.size() == 0) continue;

         String field = split.get(0).toString(parser);

         if (!isReferencableField(field))
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.field", field, opt));
         }

         if (split.size() != 2)
         {
            throw new IllegalArgumentException(
              bib2gls.getMessage("error.invalid.opt.keylist.value", 
               field, array[i].toString(parser), opt));
         }

         TeXObject format = split.get(1);

         if (format instanceof TeXObjectList)
         {
            replaceStringFormatter((TeXObjectList)format);
         }

         String val = format.toString(parser).trim();

         formatMap.put(field, val);
      }

      return formatMap;
   }

   private Conditional getCondition(KeyValList list, String opt)
    throws Bib2GlsException
   {
      TeXObject arg = list.get(opt);

      if (arg == null || arg.isEmpty())
      {
         return null;
      }

      try
      {
         TeXObjectList stack;

         if (parser.isStack(arg))
         {
            stack = (TeXObjectList)arg;
         }
         else
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.expected", arg.toString(parser)));
         }

         return ConditionalList.popCondition(this, opt, stack, -1);
      }
      catch (TeXSyntaxException e)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.invalid.option_syntax", opt, e.getMessage(bib2gls)), e);
      }
      catch (Exception e)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.invalid.option_syntax", opt, e.getMessage()), e);
      }
   }

   public void replaceuhex(TeXObjectList list)
   {
      for (int i = 0; i < list.size(); i++)
      {
         TeXObject obj = list.get(i);

         if (obj instanceof ControlSequence 
              && ((ControlSequence)obj).getName().equals("u"))
         {
            int j = i+1;

            while (j < list.size() && (list.get(j) instanceof WhiteSpace))
            {
               j++;
            }

            StringBuilder builder = new StringBuilder();

            while (j < list.size())
            {
               obj = list.get(j);

               if (obj instanceof CharObject)
               {
                  int c = ((CharObject)obj).getCharCode();

                  if ( ( c >= '0' && c <= '9' )
                    || ( c >= 'a' && c <= 'f' )
                    || ( c >= 'A' && c <= 'F' )
                     )
                  {
                     builder.append((char)c);
                     j++;
                  }
                  else
                  {
                     break;
                  }
               }
               else
               {
                  break;
               }
            }

            if (builder.length() > 0)
            {
               int cp = Integer.parseInt(builder.toString(), 16);

               j--;

               for ( ; j > i ; j--)
               {
                  list.remove(j);
               }

               if (parser.isLetter(cp))
               {
                  list.set(i, parser.getListener().getLetter(cp));
               }
               else
               {
                  list.set(i, parser.getListener().getOther(cp));
               }
            }
         }
         else if (obj instanceof TeXObjectList)
         {
            replaceuhex((TeXObjectList)obj);
         }
      }
   }

   /**
    * Parses field assignment specification.
    */ 
   private Vector<FieldAssignment> parseAssignFieldsData(
       KeyValList keyValList, String opt)
    throws Bib2GlsException,IOException
   {
      TeXObject object = keyValList.getValue(opt);

      if (object == null || object.isEmpty())
      {
         return null;
      }

      Vector<FieldAssignment> assignmentList = new Vector<FieldAssignment>();

      if (!(object instanceof TeXObjectList))
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.invalid.option_syntax", opt, object.toString(parser)));
      }

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.logAndPrintMessage("Parsing field assignment setting: "
          +object.format());
      }

      TeXObjectList stack = (TeXObjectList)object;

      replaceuhex(stack);

      StringBuilder builder = null;
      String field = null;
      FieldValueList fieldValueList = null;
      ConditionalList condition = null;
      Boolean override = null;

      while (!stack.isEmpty())
      {
         object = stack.pop(); 

         if (object instanceof WhiteSpace)
         {
            if (builder != null && field == null)
            {
               field = builder.toString();
            }
         }
         else if (object instanceof SingleToken)
         {
            int cp = ((SingleToken)object).getCharCode();

            switch (cp)
            {
               case '=':

                  if (fieldValueList == null)
                  {
                     if (field == null)
                     {
                        if (builder == null)
                        {
                           throw new Bib2GlsException(bib2gls.getMessage(
                             "error.invalid.option_syntax.misplaced_before",
                              opt, object.toString(parser),
                              bib2gls.toTruncatedString(parser, stack)));
                        }

                        field = builder.toString();
                     }

                     String overrideOpt = TeXParserUtils.popOptLabelString(parser, stack);

                     if (overrideOpt != null)
                     {
                        if (overrideOpt.equals("o"))
                        {
                           override = Boolean.TRUE;
                        }
                        else if (overrideOpt.equals("n"))
                        {
                           override = Boolean.FALSE;
                        }
                        else
                        {
                           throw new Bib2GlsException(
                             bib2gls.getMessage("error.invalid.option_syntax",
                               opt,  bib2gls.getMessage(
                                "error.invalid.field_override", overrideOpt)));
                        }
                     }

                     try
                     {
                        fieldValueList = FieldValueList.pop(this, opt, stack);
                     }
                     catch (Bib2GlsException e)
                     {
                        throw new Bib2GlsException(bib2gls.getMessage(
                          "error.invalid.option_syntax", opt, e.getMessage()
                            ), e);
                     }
                  }
                  else
                  {
                     throw new Bib2GlsException(bib2gls.getMessage(
                       "error.invalid.option_syntax.misplaced_before",
                        opt, object.toString(parser), 
                        bib2gls.toTruncatedString(parser, stack)));
                  }

               break;
               case ',':

                  if (fieldValueList == null)
                  {
                     throw new Bib2GlsException(bib2gls.getMessage(
                       "error.invalid.option_syntax.misplaced_before",
                        opt, object.toString(parser),
                        bib2gls.toTruncatedString(parser, stack)));
                  }

                  if (condition != null && condition.isEmpty())
                  {
                     condition = null;
                  }

                  FieldAssignment assignSpec
                     = new FieldAssignment(field, fieldValueList, condition, override);

                  if (bib2gls.isDebuggingOn())
                  {
                     bib2gls.logAndPrintMessage("Field assignment: "+assignSpec);
                  }

                  assignmentList.add(assignSpec);

                  builder = null;
                  field = null;
                  fieldValueList = null;
                  condition = null;
                  override = null;

               break;
               case '[':

                  if (fieldValueList == null || condition != null)
                  {
                     throw new Bib2GlsException(bib2gls.getMessage(
                       "error.invalid.option_syntax.misplaced_before",
                        opt, object.toString(parser), 
                        bib2gls.toTruncatedString(parser, stack)));
                  }

                  try
                  {
                     condition = ConditionalList.popCondition(this, opt, stack, ']');
                  }
                  catch (Bib2GlsException e)
                  {
                     throw new Bib2GlsException(bib2gls.getMessage(
                       "error.invalid.option_syntax", opt, e.getMessage()), e);
                  }

               break;
               default :

                 if (builder == null)
                 {
                    builder = new StringBuilder();
                    builder.appendCodePoint(cp);
                 }
                 else if (field == null)
                 {
                    builder.appendCodePoint(cp);
                 }
                 else
                 {
                     throw new Bib2GlsException(bib2gls.getMessage(
                       "error.invalid.option_syntax.misplaced_before",
                        opt, object.toString(parser),
                        bib2gls.toTruncatedString(parser, stack)));
                 }
            }
         }
         else
         {
            throw new Bib2GlsException(bib2gls.getMessage(
               "error.invalid.option_syntax.misplaced_before",
               opt, object.toString(parser), 
               bib2gls.toTruncatedString(parser, stack)));
         }
      }

      if (field != null && fieldValueList != null)
      {
         FieldAssignment assignSpec
           = new FieldAssignment(field, fieldValueList, condition, override);

         if (bib2gls.isDebuggingOn())
         {
            bib2gls.logAndPrintMessage("Field assignment: "+assignSpec);
         }

         assignmentList.add(assignSpec);
      }

      return assignmentList;
   }

   /**
    * Gets a list of assignments (without the destination part).
    */ 
   private Vector<FieldEvaluation> getFieldEvaluationList(
       KeyValList keyValList, String opt)
    throws Bib2GlsException,IOException
   {
      TeXObject object = keyValList.getValue(opt);

      if (object == null || object.isEmpty())
      {
         return null;
      }

      if (!(object instanceof TeXObjectList))
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.invalid.option_syntax", opt, object.toString(parser)));
      }

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.logAndPrintMessage("Parsing field evaluation setting: "
          +object.format());
      }

      TeXObjectList stack = (TeXObjectList)object;

      Vector<FieldEvaluation> evalList = new Vector<FieldEvaluation>();

      FieldValueList fieldValueList = null;
      ConditionalList condition = null;

      try
      {
         while (!stack.isEmpty())
         {
            object = stack.peek(); 

            if (object instanceof SingleToken)
            {
               int cp = ((SingleToken)object).getCharCode();

               if (cp == '[')
               {
                  stack.pop();
                  condition = ConditionalList.popCondition(this, opt, stack, ']');
               }
               else if (cp == ',')
               {
                  stack.pop();

                  if (fieldValueList != null)
                  {
                     evalList.add(new FieldEvaluation(fieldValueList, condition));
                     fieldValueList = null;
                     condition = null;
                  }
               }
               else if (fieldValueList == null)
               {
                  fieldValueList = FieldValueList.pop(this, opt, stack);
               }
               else
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                    "error.expected_before", "[", object.toString(parser)));
               }
            }
            else if (object instanceof WhiteSpace)
            {
               stack.pop();
            }
            else if (fieldValueList == null)
            {
               fieldValueList = FieldValueList.pop(this, opt, stack);
            }
            else
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                 "error.expected_before", "[", object.toString(parser)));
            }
         }

         stack.popLeadingWhiteSpace();
      }
      catch (Bib2GlsException e)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.invalid.option_syntax", opt, e.getMessage()
             ), e);
      }

      if (!stack.isEmpty())
      {
         throw new Bib2GlsException(bib2gls.getMessage(
              "error.unexpected_content_in_arg", stack.toString(parser), opt));
      }

      if (fieldValueList != null)
      {
         evalList.add(new FieldEvaluation(fieldValueList, condition));
      }

      if (evalList.isEmpty())
      {
         return null;
      }
      else
      {
         return evalList;
      }
   }

   /**
    * Gets a single field value list and conditional.
    */ 
   private FieldEvaluation getFieldEvaluation(
       KeyValList keyValList, String opt)
    throws Bib2GlsException,IOException
   {
      TeXObject object = keyValList.getValue(opt);

      if (object == null || object.isEmpty())
      {
         return null;
      }

      if (!(object instanceof TeXObjectList))
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.invalid.option_syntax", opt, object.toString(parser)));
      }

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.logAndPrintMessage("Parsing field evaluation setting: "
          +object.format());
      }

      TeXObjectList stack = (TeXObjectList)object;

      FieldValueList fieldValueList = null;
      ConditionalList condition = null;

      stack.popLeadingWhiteSpace();

      try
      {
         fieldValueList = FieldValueList.pop(this, opt, stack);

         while (!stack.isEmpty())
         {
            object = stack.pop(); 

            if (object instanceof SingleToken)
            {
               if (((SingleToken)object).getCharCode() == '[')
               {
                  condition = ConditionalList.popCondition(this, opt, stack, ']');
                  break;
               }
               else
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                    "error.expected_before", "[", object.toString(parser)));
               }
            }
            else if (!(object instanceof WhiteSpace))
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                 "error.expected_before", "[", object.toString(parser)));
            }
         }

         stack.popLeadingWhiteSpace();
      }
      catch (Bib2GlsException e)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.invalid.option_syntax", opt, e.getMessage()
             ), e);
      }

      if (!stack.isEmpty())
      {
         throw new Bib2GlsException(bib2gls.getMessage(
              "error.unexpected_content_in_arg", stack.toString(parser), opt));
      }

      if (fieldValueList == null)
      {
         return null;
      }
      else
      {
         return new FieldEvaluation(fieldValueList, condition);
      }
   }

   private MissingFieldAction getMissingFieldAction(
       KeyValList keyValList, String opt)
    throws Bib2GlsException,IOException
   {
      String val = getChoice(keyValList, opt, "skip", "fallback", "empty");

      if (val.equals("skip"))
      {
         return MissingFieldAction.SKIP;
      }
      else if (val.equals("fallback"))
      {
         return MissingFieldAction.FALLBACK;
      }
      else // if (val.equals("empty"))
      {
         return MissingFieldAction.EMPTY;
      }
   }

   /**
    * Substitutes TeX's standard control-character commands for literal characters.
    * The supplied TeX code is a format pattern, but since it has to
    * be written to the aux file, the safest method is to escape the
    * literal characters. These need to have the backslash removed
    * before the code can be converted to a string suitable for a
    * formatter. Note that {@code \\protect\\u} will
    * cause the command name to be followed by whitespace, which
    * needs to be stripped if present.
    * @param format the TeX code
    */ 
   private void replaceStringFormatter(TeXObjectList format)
   {
      for (int i = 0; i < format.size(); i++)
      {
         TeXObject obj = format.get(i);

         if (obj instanceof TeXObjectList)
         {
            replaceStringFormatter((TeXObjectList)obj);
         }
         else if (obj instanceof ControlSequence)
         {
            String name = ((ControlSequence)obj).getName();

            if (name.equals("%"))
            {
               format.set(i, parser.getListener().getOther('%'));
            }
            else if (name.equals("#"))
            {
               format.set(i, parser.getListener().getOther('#'));
            }
            else if (name.equals("$"))
            {
               format.set(i, parser.getListener().getOther('$'));
            }
            else if (name.equals("&"))
            {
               format.set(i, parser.getListener().getOther('&'));
            }
            else if (name.equals("{"))
            {
               format.set(i, parser.getListener().getOther('{'));
            }
            else if (name.equals("}"))
            {
               format.set(i, parser.getListener().getOther('}'));
            }
            else if (name.equals("_"))
            {
               format.set(i, parser.getListener().getOther('_'));
            }
            else if (name.equals("\\"))
            {
               format.set(i, parser.getListener().getOther('\\'));
            }
            else if (name.equals("u"))
            {
               if (i != format.size()-1 && (format.get(i+1) instanceof Ignoreable))
               {
                  format.remove(i+1);
               }
            }
         }
      }
   }

   /**
    * Gets the letter number rule for the given setting as an integer.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is invalid
    * @return the rule ID
    */ 
   private int getLetterNumberRule(KeyValList list, String opt)
   throws IOException
   {
      String val = getChoice(list, opt, "before letter",
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

   /**
    * Gets the letter number punctuation rule for the given setting as an integer.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is invalid
    * @return the rule ID
    */ 
   private int getLetterNumberPuncRule(KeyValList list, String opt)
   throws IOException
   {
      String val = getChoice(list, opt, "punc-space-first", 
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

   /**
    * Gets the break-at rule for the given setting as an integer.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is invalid
    * @return the rule ID
    */ 
   private int getBreakAt(KeyValList list, String opt)
   throws IOException
   {
      String val = getChoice(list, opt, "none", "word",
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

   /**
    * Gets the sort label list method for the given setting.
    * The value should be supplied as {@code field-list:sort-method:csname}
    * or as {@code field-list:sort-method}, which indicates which
    * fields should have their list values sorted according to the
    * given method.
    * @param list the key=value setting list
    * @param opt the name of the setting
    * @throws IOException may be thrown by the aux file parser
    * @throws IllegalArgumentException if the value is invalid
    * @return the method
    */ 
   private LabelListSortMethod getLabelListSortMethod( 
     TeXObjectList list, String opt)
   throws IOException
   {
      Vector<TeXObject> split = splitList(':', list);

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

   /**
    * Splits a list on the given character.
    * @param c the split (separator) character
    * @param list the list that needs splitting
    * @throws IOException may be thrown by the aux file parser
    * @return the vector of list elements or null if empty
    */ 
   private Vector<TeXObject> splitList(char c, TeXObjectList list)
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
            element = element.trim();

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

      element = element.trim();

      if (element.size() != 0)
      {
         split.add(element);
      }

      return split;
   }

   /**
    * Strips out all unknown fields from the primary and secondary
    * patterns.
    */ 
   private void stripUnknownFieldPatterns()
   {
      fieldPatterns = stripUnknownFieldPatterns(fieldPatterns);
      secondaryFieldPatterns = stripUnknownFieldPatterns(secondaryFieldPatterns);
   }

   /**
    * Strips out all unknown fields from a field pattern map.
    * @patterns the field pattern map, which may be null
    * @return the field pattern map or null if all mappings removed
    */ 
   private HashMap<String,Pattern> stripUnknownFieldPatterns(
      HashMap<String,Pattern> patterns)
   {
      if (patterns == null) return null;

      Vector<String> fields = new Vector<String>();

      for (Iterator<String> it = patterns.keySet().iterator();
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
         patterns.remove(field);
      }

      if (patterns.size() == 0)
      {
         patterns = null;
      }

      return patterns;
   }

   /**
    * Adds the given field to the list of valid field names.
    * @param field the field name
    */ 
   public void addUserField(String field)
   {
      if (additionalUserFields == null)
      {
         additionalUserFields = new Vector<String>();
      }

      additionalUserFields.add(field);

      bib2gls.verboseMessage("message.added.user.field", field);
   }

   /*
    * METHODS: bib parsing (stage 2).
    * Reading in all bib data associated with this resource set. 
    * Each bib object is created by the Bib2GlsAt class process
    * method (which parses the object's contents) and is added 
    * to the listener's internal list of data, this
    * includes @preamble and @string.
    */

   /**
    * Parses all bib files associated with this resource set (stage 2).
    * @throws IOException may be thrown by the aux file parser
    */ 
   public void parseBibFiles()
   throws IOException
   {
      bib2gls.verboseMessage("message.parsing.resource.bib", texFile.getName());
      bib2gls.logDefaultEncoding(bibCharset);

      stripUnknownFieldPatterns();

      bibParserListener = new Bib2GlsBibParser(bib2gls, this, 
       bibCharset == null ? bib2gls.getDefaultCharset() : bibCharset);

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
               reader = bib2gls.createBufferedReader(bibFile.toPath(),
                  bib2gls.getDefaultCharset());

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
            catch (MalformedInputException e)
            {
               throw new IOException(bib2gls.getMessage(
                 "error.cant.parse.file.malformed.input",
                   bibFile, bib2gls.getDefaultCharset(),
                   "charset", "--default-encoding", e));
            }
            finally
            {
               if (reader != null)
               {
                  reader.close();
               }
            }
         }

         if (srcCharset == null)
         {
            srcCharset = bib2gls.getDefaultCharset();
            bib2gls.logMessage(bib2gls.getMessage("message.assuming.charset", 
              srcCharset));
         }

         bibParserListener.setCharSet(srcCharset);

         try
         {
            bibParserListener.parse(bibFile);
         }
         catch (MalformedInputException e)
         {
            throw new IOException(bib2gls.getMessage(
              "error.cant.parse.file.malformed.input",
                bibFile, srcCharset,
                "charset", "--default-encoding", e));
         }
      }
   }

   public String getSourceFileList()
   {
      StringBuilder builder = new StringBuilder();

      builder.append(bib2gls.getAuxFile().getName());

      for (int i = 0, n = sources.size()-1; i <= n; i++)
      {
         if (i == n)
         {
            builder.append(bib2gls.getMessage("comment.list.and", sources.size()));
         }
         else
         {
            builder.append(", ");
         }

         builder.append(sources.get(i).getFileName().toString());
      }

      return builder.toString();
   }

   /**
    * Gets the listener used by the bib parser.
    */ 
   public Bib2GlsBibParser getBibParserListener()
   {
      return bibParserListener;
   }

   /**
    * Gets the bib parser.
    */ 
   public TeXParser getBibParser()
   {
      return bibParserListener == null ? null : bibParserListener.getParser();
   }

   // Compound entries are added when @compoundset has its contents
   // parsed.

   /**
    * Determines whether or not there are compound entries.
    * @return true if there are compound entries
    */ 
   public boolean hasCompoundEntries()
   {
      if (compoundEntriesGlobal)
      {
         return bib2gls.hasCompoundEntries();
      }

      return compoundEntries != null;
   }

   /**
    * Gets the compound entry key iterator.
    * The iterator is for the set of compound entry labels.
    * @return the iterator or null if no compound entries
    */ 
   public Iterator<String> getCompoundEntryKeyIterator()
   {
      if (compoundEntriesGlobal)
      {
         return bib2gls.getCompoundEntryKeyIterator();
      }
      else if (compoundEntries != null)
      {
         return compoundEntries.keySet().iterator();
      }

      return null;
   }

   /**
    * Gets the compound entry value iterator.
    * The iterator is for the set of compound entries.
    * @return the iterator or null if no compound entries
    */ 
   public Iterator<CompoundEntry> getCompoundEntryValueIterator()
   {
      if (compoundEntriesGlobal)
      {
         return bib2gls.getCompoundEntryValueIterator();
      }
      else if (compoundEntries != null)
      {
         return compoundEntries.values().iterator();
      }

      return null;
   }

   /**
    * Gets the compound entry value identified by the given label.
    * @return the compound entry or null if not found
    */ 
   public CompoundEntry getCompoundEntry(String label)
   {
      if (compoundEntriesGlobal)
      {
         return bib2gls.getCompoundEntry(label);
      }
      else if (compoundEntries != null)
      {
         return compoundEntries.get(label);
      }

      return null;
   }

   /**
    * Adds the compound entry value.
    * @param compoundEntry the compound entry to add
    */ 
   public void addCompoundEntry(CompoundEntry compoundEntry)
   {
      // bib definition needs to override aux information. 
      bib2gls.addCompoundEntry(compoundEntry, true);

      if (compoundEntries == null)
      {
         compoundEntries = new HashMap<String,CompoundEntry>();
      }

      String label = compoundEntry.getLabel();

      bib2gls.debugMessage("message.compoundset.found", label);

      if (compoundEntries.containsKey(label))
      {
         bib2gls.error(bib2gls.getMessage("error.duplicate.compound_set", label));
      }
      else
      {
         compoundEntries.put(label, compoundEntry);
      }

      if (compoundEntriesHasRecords == COMPOUND_MGLS_RECORDS_DEFAULT)
      {
         compoundEntriesHasRecords = COMPOUND_MGLS_RECORDS_TRUE;
      }
   }

   /**
    * Gets the first compound entry that has the given main.
    * This will search either the global or local list depending on
    * the "compound-options-global" setting.
    * @param mainLabel the label of the main entry
    * @return the compound entry
    */ 
   public CompoundEntry getCompoundEntryWithMain(String mainLabel)
   {
      return getCompoundEntryWithMain(mainLabel, compoundEntriesGlobal);
   }

   /**
    * Gets the first compound entry that has the given main.
    * @param mainLabel the label of the main entry
    * @param global if true search the global list
    * @return the compound entry
    */ 
   public CompoundEntry getCompoundEntryWithMain(String mainLabel, boolean global)
   {
      if (global)
      {
         return bib2gls.getCompoundEntryWithMain(mainLabel);
      }
      else if (compoundEntries != null)
      {
         for (Iterator<CompoundEntry> it=compoundEntries.values().iterator();
              it.hasNext(); )
         {
            CompoundEntry compEntry = it.next();

            if (compEntry.getMainLabel().equals(mainLabel))
            {
               return compEntry;
            }
         }
      }

      return null;
   }

   /**
    * Gets the sole compound entry that has the given main.
    * This will search either the global or local list depending on
    * the "compound-options-global" setting.
    * This method will return null if no compound entry has the given main
    * or if multiple compound entries have the given main.
    * @param mainLabel the label of the main entry
    * @return the only compound entry to have the given main entry
    * or null
    */ 
   public CompoundEntry getUniqueCompoundEntryWithMain(String mainLabel)
   {
      return getUniqueCompoundEntryWithMain(mainLabel, compoundEntriesGlobal);
   }

   /**
    * Gets the sole compound entry that has the given main.
    * This will return null if no compound entry has the given main
    * or if multiple compound entries have the given main.
    * @param mainLabel the label of the main entry
    * @param global if true search the global list
    * @return the only compound entry to have the given main entry
    * or null
    */ 
   public CompoundEntry getUniqueCompoundEntryWithMain(String mainLabel, 
      boolean global)
   {
      if (global)
      {
         return bib2gls.getUniqueCompoundEntryWithMain(mainLabel);
      }
      else if (compoundEntries != null)
      {
         CompoundEntry comp = null;

         for (Iterator<CompoundEntry> it=compoundEntries.values().iterator();
              it.hasNext(); )
         {
            CompoundEntry compEntry = it.next();

            if (compEntry.getMainLabel().equals(mainLabel))
            {
               if (comp == null)
               {
                  comp = compEntry;
               }
               else
               {
                  return null;
               }
            }
         }

         return comp;
      }

      return null;
   }

   /**
    * Sets or appends to the preamble obtained from the bib file.
    * Used by the bib file parser.
    * @param string version of preamble (for writing to the glstex)
    * @param list content of preamble for parser if interpreter
    * required
    */ 
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

   /*
    * METHODS: bib object processing and dependencies (stage 3).
    * The data obtained from parsing the bib files is a mixture of
    * objects representing glossary entries (Bib2GlsEntry) and other
    * types of bib objects (such as @preamble). If a preamble has
    * been provided, it now needs to be processed (if the interpreter
    * needs to pick up any command definitions).
    *
    * Actual glossary entries need to be picked out and stored in 
    * a separate list (bibData for the primary entries and dualData 
    * for the dual entries, if they need to be sorted separately).
    * These lists contain all eligible entry data (not discarded
    * entries) that may or may not be required by this resource set. 
    *
    * Separate lists are used to keep track of the actual selected
    * entries in stage 4.
    */

   /**
    * Processes the list of bib entries (stage 3). The list of bib entries
    * should already have been obtained by parsing the bib file(s)
    * associated with this resource set. This stage interprets the
    * preamble, processes entry fields, and establishes
    * dependencies. Note that the bib parser is used for parsing bib
    * syntax (although it inherits from TeXParser). The bib parser 
    * listener contains the data obtained from parsing the bib file.
    * The selected entries are saved in the bibData list (primary
    * entries or all entries if the dual should be combined with the
    * primart) and the dualData list (dual entries if they need to be
    * sorted separately from the primary entries).
    * The aux parser is used for parsing TeX syntax.
    * @param parser the aux file parser
    * @throws IOException may be thrown by the aux file parser
    * @throws Bib2GlsException invalid resource setting syntax
    */ 
   public void processBibList()
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

      if (selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
       || selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO)
      {
         seeList = new Vector<Bib2GlsEntry>();
      }

      Vector<Bib2GlsEntry> savedSeeEntries = null;

      if (isPruneSeeDeadEndsOn() || isPruneSeeAlsoDeadEndsOn())
      {
         savedSeeEntries = new Vector<Bib2GlsEntry>();
      }

      boolean combine = "combine".equals(dualSortSettings.getMethod());

      for (int i = 0; i < list.size(); i++)
      {
         BibData data = list.get(i);

         if (data instanceof Bib2GlsEntry)
         {
            Bib2GlsEntry entry = (Bib2GlsEntry)data;
            entry.parseFields();

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
                  if (savedSeeEntries != null 
                       && (dual.getField("see") != null
                         || dual.getField("seealso") != null))
                  {
                     savedSeeEntries.add(dual);
                  }
                  else
                  {
                     dual.initCrossRefs();
                  }
               }

            }

            // does this entry have any records?

            boolean hasRecords = entry.hasRecords();
            boolean dualHasRecords = (dual != null && dual.hasRecords());

            for (GlsRecord r : records)
            {
               GlsRecord rec = getRecord(primaryId, dualId, tertiaryId, r);

               if (rec == null) continue;

               String recordLabel = rec.getLabel(recordLabelPrefix);

               if (recordLabel.equals(primaryId))
               {
                  if (dual == null)
                  {
                     entry.addRecord(rec);
                     hasRecords = true;
                  }
                  else
                  {
/*
 * This record is for the primary entry.
 */
                     if (combineDualLocations == COMBINE_DUAL_LOCATIONS_OFF
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_BOTH
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY_RETAIN_PRINCIPAL
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL_RETAIN_PRINCIPAL
                         )
                     {
/*
 * Add the record to the primary entry location list.
 * If 'dual retain principal' is on, only add if this is a
 * principal location.
 */
                        entry.addRecord(rec,
                         combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL_RETAIN_PRINCIPAL);
                        hasRecords = true;
                     }

                     if (combineDualLocations == COMBINE_DUAL_LOCATIONS_BOTH
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL_RETAIN_PRINCIPAL
                        )
                     {
/*
 * Copy the record to the dual entry location list.
 */
                        dual.addRecord(rec.copy(dualId));
                        dualHasRecords = true;
                     }
                  }
               }

               if (dual != null)
               {
                  if (recordLabel.equals(dualId))
                  {
/*
 * This record is for the dual entry.
 */
                     if (combineDualLocations == COMBINE_DUAL_LOCATIONS_OFF
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_BOTH
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL_RETAIN_PRINCIPAL
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY_RETAIN_PRINCIPAL
                         )
                     {
/*
 * Add the record to the dual entry location list.
 * If 'primary retain principal' is on, only add if this is a
 * principal location.
 */
                        dual.addRecord(rec,
                           combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY_RETAIN_PRINCIPAL);
                        dualHasRecords = true;
                     }


                     if (combineDualLocations == COMBINE_DUAL_LOCATIONS_BOTH
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY
                      || combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY_RETAIN_PRINCIPAL
                        )
                     {
/*
 * Copy the record to the primary entry location list.
 */
                        entry.addRecord(rec.copy(primaryId));
                        hasRecords = true;
                     }
                  }

                  if (tertiaryId != null)
                  {
                     if (rec.getLabel().equals(tertiaryId))
                     {
                        dual.addRecord(rec.copy(dualId));
                        dualHasRecords = true;

                        if (combineDualLocations != COMBINE_DUAL_LOCATIONS_OFF)
                        {
                           entry.addRecord(rec.copy(primaryId));
                           hasRecords = true;
                        }
                     }
                  }
               }
            }

            // any 'see' records?

            for (GlsSeeRecord rec : seeRecords)
            {
               String recordLabel = getRecordLabel(rec);

               if (recordLabel.equals(primaryId))
               {
                  entry.addRecord(rec);
                  hasRecords = true;
               }

               if (dual != null)
               {
                  if (recordLabel.equals(dualId))
                  {
                     dual.addRecord(rec);
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

               if (bib2gls.isVerbose())
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
                selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO ||
                selectionMode == SELECTION_RECORDED_AND_DEPS ||
                selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED ||
                selectionMode == SELECTION_ALL)
            {
               // does this entry have a "see" or "seealso" field?

               if (savedSeeEntries != null &&
                    (entry.getField("see") != null
                         || entry.getField("seealso") != null))
               {
                  savedSeeEntries.add(entry);
               }
               else
               {
                  entry.initCrossRefs();
               }

               if (dual != null)
               {
                  if (savedSeeEntries != null
                        && (dual.getField("see") != null
                               || dual.getField("seealso") != null))
                  {
                     savedSeeEntries.add(dual);
                  }
                  else
                  {
                     dual.initCrossRefs();
                  }
               }

               if (selectionMode != SELECTION_ALL)
               {
                  if (seeList != null)
                  {
                     if ((entry.hasCrossRefs() 
                           && (selectionMode != SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                               || entry.getField("seealso") == null)
                         )
                         || entry.getField("alias") != null
                        )
                     {
                        seeList.add(entry);
                     }

                     if (dual != null 
                         && 
                         (
                           (dual.hasCrossRefs() 
                             && (selectionMode != SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                                  || dual.getField("seealso") == null)
                            )
                           || dual.getField("alias") != null
                         )
                        )
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
            }

            if (dateTimeList != null)
            {
               for (String field : dateTimeList)
               {
                  entry.convertFieldToDateTime(field,
                    dateTimeListFormat, dateTimeListLocale,
                    true, true);

                  if (dual != null)
                  {
                     entry.convertFieldToDateTime(field,
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
                  entry.convertFieldToDateTime(field,
                    dateListFormat, dateListLocale,
                    true, false);

                  if (dual != null)
                  {
                     entry.convertFieldToDateTime(field,
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
                  entry.convertFieldToDateTime(field,
                    timeListFormat, timeListLocale,
                    false, true);

                  if (dual != null)
                  {
                     entry.convertFieldToDateTime(field,
                       dualTimeListFormat,
                       dualTimeListLocale,
                       false, true);
                  }
               }
            }

         }
      }

      if (savedSeeEntries != null && !savedSeeEntries.isEmpty())
      {
         // initialise cross-reference lists but prune any dead ends

         if (pruneIterations > 1)
         {
            bib2gls.verboseMessage("message.repruning", 1, pruneIterations);
         }

         for (Bib2GlsEntry entry : savedSeeEntries)
         {
            entry.initCrossRefs();

            if (seeList != null && pruneIterations == 1)
            {
               if ((entry.hasCrossRefs() 
                     && (selectionMode != SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                         || entry.getField("seealso") == null)
                   )
                   || entry.getField("alias") != null
                  )
               {
                  seeList.add(entry);
               }
            }
         }

         for (int i = 2; i <= pruneIterations; i++)
         {
            bib2gls.verboseMessage("message.repruning", i, pruneIterations);

            for (Bib2GlsEntry entry : savedSeeEntries)
            {
               entry.reprune();

               if (seeList != null && pruneIterations == i)
               {
                  if ((entry.hasCrossRefs() 
                        && (selectionMode != SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                            || entry.getField("seealso") == null)
                      )
                      || entry.getField("alias") != null
                     )
                  {
                     seeList.add(entry);
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

            Vector<Bib2GlsEntry> otherTargets = null;

            if (target == null)
            {
               // is alias a compound entry?

               CompoundEntry comp = getCompoundEntry(alias);

               if (comp != null)
               {
                  String mainLabel = comp.getMainLabel();

                  target = getEntry(mainLabel, bibData);

                  if (target == null && dualData != null)
                  {
                     target = getEntry(mainLabel, dualData);
                  }

                  if (target != null)
                  {
                     otherTargets = getOtherElements(comp);
                  }
               }
            }

            if (target == null)
            {
               bib2gls.warningMessage("warning.alias.not.found",
                 alias, entry.getId(), "alias-loc", "transfer");
            }
            else if (target.getId().equals(entry.getId()))
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                "error.self_alias.forbidden", entry.getId()));
            }
            else
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE ||
                   selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO)
               {
                  bib2gls.debugMessage("message.added.alias.dep", entry.getId(),
                    target.getId());
                  target.addDependency(entry.getId());

                  if (otherTargets != null)
                  {
                     for (Bib2GlsEntry other : otherTargets)
                     {
                        other.addDependency(entry.getId());
                     }
                  }
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

            if (selectionMode != SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO)
            {
               addCrossRefs(entry, entry.getAlsoCrossRefs());
            }

            String alias = entry.getFieldValue("alias");

            if (alias != null)
            {
               addCrossRefs(entry, alias);
            }
         }
      }

      applyCompoundEntrySettings();

      addSupplementalRecords();
   }

   /**
    * Process the preamble contents provided in the bib file. This
    * allows any command definitions to be picked up by the
    * interpreter.
    * @throws IOException may be thrown by the parser
    */ 
   public void processPreamble()
    throws IOException
   {
      if (preambleList != null)
      {
         bib2gls.processPreamble(preambleList);
      }

      updateWordExceptionList();
   }

   private void updateWordExceptionList()
   throws IOException
   {
      // save textual word exception lists

      TeXObjectList list = bib2gls.getWordExceptionList();

      if (list != null && !list.isEmpty())
      {
         CsvList csvList = CsvList.getList(parser, list);

         if (!csvList.isEmpty())
         {
            int n = csvList.size();

            stringWordExceptions = new String[n];

            for (int i = 0; i < n; i++)
            {
               stringWordExceptions[i] = csvList.getValue(i).toString(parser);
            }
         }
      }
   }

   /**
    * Applies the compound entry settings.
    */ 
   private void applyCompoundEntrySettings()
   {
      if (!hasCompoundEntries() 
           || (!compoundEntriesDependent && !compoundEntriesAddHierarchy
                && compoundMainType == null && compoundOtherType == null))
      {
         return;
      }

      for (Iterator<CompoundEntry> it = getCompoundEntryValueIterator();
           it.hasNext(); )
      {
         CompoundEntry comp = it.next();

         String mainLabel = comp.getMainLabel();
         Bib2GlsEntry mainEntry = getEntry(mainLabel);

         String[] elements = comp.getElements();

         Bib2GlsEntry prevEntry = null;

         boolean deferOtherTypeAssign = false;
         Vector<Bib2GlsEntry> others = null;

         for (int i = 0; i < elements.length; i++)
         {
            boolean isMain = elements[i].equals(mainLabel);

            if (compoundEntriesDependent && !isMain)
            {
               // If compound entry list is global, elements may not
               // be in this resource set.

               if (mainEntry != null)
               {
                  mainEntry.addDependency(elements[i]);
               }
            }

            Bib2GlsEntry elemEntry = null;
            boolean elemEntryAssigned = false;

            if (compoundEntriesAddHierarchy)
            {
               if (i == 0)
               {
                  if (!isMain && "same as main".equals(compoundOtherType))
                  {
                     deferOtherTypeAssign = true;
                     others = new Vector<Bib2GlsEntry>(elements.length-1);

                     elemEntry = getEntry(elements[i]);
                     elemEntryAssigned = true;

                     if (elemEntry != null)
                     {
                        others.add(elemEntry);
                     }
                  }
               }
               else
               {
                  if (isMain)
                  {
                     elemEntry = mainEntry;
                  }
                  else
                  {
                     elemEntry = getEntry(elements[i]);
                  }

                  elemEntryAssigned = true;

                  if (others != null && elemEntry != null)
                  {
                     others.add(elemEntry);
                  }

                  if (elemEntry != null && prevEntry != null
                       && !elemEntry.hasParent() 
                       && !elements[i].equals(elements[i-1])
                       && !isAncestor(elements[i-1], elemEntry)
                       && !isAncestor(elements[i], prevEntry))
                  {
                     elemEntry.setParent(elements[i-1]);
                  }
               }

               prevEntry = elemEntry;
            }

            if (compoundMainType != null
             || (compoundOtherType != null && !deferOtherTypeAssign))
            {
               if (!elemEntryAssigned)
               {
                  if (isMain)
                  {
                     elemEntry = mainEntry;
                  }
                  else
                  {
                     elemEntry = getEntry(elements[i]);
                  }

                  elemEntryAssigned = true;
               }

               if (elemEntry != null)
               {
                  String entryType = null;

                  if (isMain)
                  {
                     if (compoundMainType != null)
                     {
                        entryType = getType(elemEntry, compoundMainType,
                          !compoundTypeOverride);
                     }
                  }
                  else if (compoundOtherType != null)
                  {
                     String type = compoundOtherType;

                     if (compoundOtherType.equals("same as main"))
                     {
                        type = getType(mainEntry, null, !compoundTypeOverride);
                     }

                     entryType = getType(elemEntry, type, !compoundTypeOverride);
                  }

                  if (entryType != null)
                  {
                     elemEntry.putField("type", entryType);
                  }
               }
            }
         }

         if (others != null)
         {
            String type = compoundOtherType;

            if ("same as main".equals(compoundOtherType))
            {
               type = getType(mainEntry, null, !compoundTypeOverride);
            }

            if (type != null)
            {
               for (Bib2GlsEntry other : others)
               {
                  String entryType = getType(other, type, !compoundTypeOverride);

                  if (entryType != null)
                  {
                     other.putField("type", entryType);
                  }
               }
            }
         }
      }
   }

   /**
    * Gets the compound entry according to the "compound-adjust-name" setting.
    * @param id the label of the main entry
    * @return the first compound entry with the main entry matching
    * the given id if compound-adjust-name=once, or the unique
    * compound entry with the main entry matching the given id if
    * compound-adjust-name=unique, or null if match not found or
    * setting off
    */ 
   public CompoundEntry getCompoundAdjustName(String id)
   {
      switch (compoundAdjustName)
      {
         case COMPOUND_ADJUST_NAME_FALSE:
            return null;

         case COMPOUND_ADJUST_NAME_ONCE:
            return getCompoundEntryWithMain(id);

         case COMPOUND_ADJUST_NAME_UNIQUE:
            return getUniqueCompoundEntryWithMain(id);
      }

      return null;
   }

   /**
    * Adds the given cross-references to dependencies. 
    * For each cross-reference label, if the matching entry can be
    * found, it will have its dependencies updated.
    * @param entry the entry with the cross-references
    * @param xrList the list of cross-reference labels
    */ 
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

         if (xrEntry == null)
         {
            CompoundEntry comp = bib2gls.getCompoundEntry(xr);

            if (comp != null)
            {
               if (bib2gls.isVerbose())
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                    "message.compoundcrossref.by", xr, entry.getId()));
               }

               for (String elem : comp.getElements())
               {
                  addCrossRefs(entry, elem);
               }
            }
         }
         else
         {
            if (bib2gls.isVerbose())
            {
               bib2gls.logMessage(bib2gls.getMessage(
                 "message.crossref.by", xrEntry.getId(), entry.getId()));
            }

            xrEntry.addDependency(entry.getId());
            xrEntry.addCrossRefdBy(entry);
         }
      }
   }

   /**
    * Adds any supplemental records. 
    * These are records found in a supplemental aux file that match
    * entries defined in this resource set.
    */ 
   private void addSupplementalRecords()
   {
      if (supplementalRecords != null)
      {
         for (SupplementalRecord suppRecord : supplementalRecords)
         {
            GlsRecord rec = (GlsRecord)suppRecord;

            Bib2GlsEntry entry = getEntryMatchingRecord(rec);

            if (entry != null)
            {
               if (supplementalCategory != null)
               {
                  setCategory(entry, supplementalCategory);
               }

               entry.addSupplementalRecord(rec);
            }
         }
      }
   }

   /**
    * Adds all the dependencies for the given entry.
    * @param entry the entry
    * @param entries the list of entries to search for dependencies
    */ 
   private void addDependencies(Bib2GlsEntry entry, 
     Vector<Bib2GlsEntry> entries)
   {
      for (Iterator<String> it = entry.getDependencyIterator();
           it.hasNext(); )
      {
         String dep = it.next();

         // Has the dependency already been added or does it have
         // any records?  (Don't want to get stuck in an infinite loop!)

         if (isDependent(dep) || dep.equals(entry.getId()))
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

   /**
    * Determines whether or not the given entry ID has been added to
    * the list of dependent entries.
    * @param id the entry label
    * @return true if the given id is in the list of dependent
    * entries
    */ 
   public boolean isDependent(String id)
   {
      return dependencies.contains(id);
   }

   /**
    * Adds the given entry label to the list of dependent entries.
    * Indicates that one or more selected entries depend on the
    * entry identified by the given label.
    * @param id the entry label
    */ 
   public void addDependent(String id)
   {
      if (!dependencies.contains(id))
      {
         bib2gls.verboseMessage("message.added.dep", id);
         dependencies.add(id);
      }
   }

   /** Gets the list of all dependent entries.
    * @return vector of dependent entries
    */
   public Vector<String> getDependencies()
   {
      return dependencies;
   }

   /*
    * METHODS: selection, sorting and writing (stage 4).
    */

   /**
    * Processes the data (stage 4). This selects the required
    * entries, sorts them (if applicable) and writes the glstex
    * file associated with this resource set.
    * @return -1 if this resource set has used the "master" option,
    * otherwise returns the total number of entries for this
    * resource set
    * @throws IOException may be thrown by the aux file parser
    * @throws Bib2GlsException invalid syntax
    */ 
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

   private void printHeaderComments(PrintWriter writer)
   throws IOException
   {
      writer.println(bib2gls.getGlsTeXHeader());

      writer.println(bib2gls.getMessage("comment.no_edit", Bib2Gls.NAME));
      writer.println(bib2gls.getMessage("comment.source_list")); 
      writer.print("% ");
      writer.println(getSourceFileList());
   }

   /**
    * Process the data (stage 4, master).
    * @throws IOException may be thrown by the aux file parser
    * @throws Bib2GlsException invalid syntax
    */ 
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

         printHeaderComments(writer);

         if (bib2gls.suppressFieldExpansion())
         {
            writer.println("\\glsnoexpandfields");
         }

         bib2gls.writeCommonCommands(writer);

         provideGlossary(writer, type);

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

   /**
    * Adds missing parents. Used if the "missing-parents" option is set
    * to "create".
    * @param childEntry entry that has the parent field set
    * @param data list of entries in which to find parent
    */ 
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

      parent = childEntry.createParent();

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

   /**
    * Determines if the given label corresponds to an ancestor of
    * the given entry.
    * @param ancestorLabel the label of the potential ancestor
    * @param entry the entry
    * @return true if entry has an ancestor with given label
    * otherwise false
    */ 
   public boolean isAncestor(String ancestorLabel, Bib2GlsEntry entry)
   {
      String parentId = entry.getParent();

      if (parentId == null)
      {
         return false;
      }

      if (parentId.equals(ancestorLabel))
      {
         return true;
      }

      Bib2GlsEntry parentEntry = getEntry(parentId);

      if (parentEntry == null)
      {
         return false;
      }

      return isAncestor(ancestorLabel, parentEntry);
   }

   /**
    * Adds hierarchical data for the given entry.
    * @param childEntry the entry
    * @param entries the list of selected entries
    * @param data the full list of entries (bibData or dualData)
    * @throws Bib2GlsException if an entry is its own parent
    */ 
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
         childEntry.addDependency(parent.getId());
      }
   }

   /**
    * Processes the data (internal stage 4).
    * Selects entries, creates missing parents, and
    * processes dependencies. If no sorting is required, the
    * required entries with records are added to the selection in order of
    * definition. If all entries must be selected then they are
    * added in order of definition. Otherwise entries are added
    * according to their records, which means they'll be in the
    * correct order for sort=use.
    * @param data the list of entries
    * @param entries the list of selected entries
    * @param entrySort the sort method (determines how to add the
    * data to the list)
    * @throws Bib2GlsException if a parser error occurs
    * @see #processBibData()
    */ 
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
         || matchAction == MATCH_ACTION_ADD
         || selectionMode == SELECTION_SELECTED_BEFORE)
      {
         // add all entries that have been recorded in the order of
         // definition

         if (matchAction == MATCH_ACTION_ADD)
         {
            bib2gls.debugMessage("message.selecting.by-option", 
               "match-action=add, selection=" + SELECTION_OPTIONS[selectionMode]);
         }
         else
         {
            bib2gls.debugMessage("message.selecting.by-option", 
               "selection=" + SELECTION_OPTIONS[selectionMode]);
         }

         for (Bib2GlsEntry entry : data)
         {
            bib2gls.debugMessage("message.selecting.considering", entry);

            boolean hasRecords = entry.hasRecords();
            Bib2GlsEntry dual = entry.getDual();

            boolean dualHasRecords = (dual != null && dual.hasRecords()
              && dualPrimaryDependency);

            boolean recordedOrDependent = hasRecords
              || bib2gls.isDependent(entry.getId());

            boolean selectedBefore = 
              (selectionMode == SELECTION_SELECTED_BEFORE 
                 && bib2gls.isEntrySelected(entry.getId()));

            boolean isMatch = false;
            boolean isDualMatch = false;

            if (!(recordedOrDependent || selectedBefore))
            {
               if (matchAction == MATCH_ACTION_ADD && fieldPatterns != null
                         && !notMatch(entry))
               {
                  isMatch = true;
               }
               else if (dual != null && !dualHasRecords
                    && matchAction == MATCH_ACTION_ADD && fieldPatterns != null
                    && !notMatch(dual))
               {
                  isDualMatch = true;
               }
            }

            if (recordedOrDependent || selectedBefore || isMatch
               || (dual != null && (dualHasRecords || isDualMatch)))
            {
               if (bib2gls.isDebuggingOn())
               {
                  if (selectedBefore)
                  {
                     bib2gls.debugMessage("message.selecting.entry.before",
                      entry);
                  }
                  else if (hasRecords)
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
                  else if (isMatch)
                  {
                     bib2gls.debugMessage("message.selecting.entry.match-add",
                      entry);
                  }
                  else if (isDualMatch)
                  {
                     bib2gls.debugMessage("message.selecting.entry.dual-match-add",
                      entry);
                  }
                  else
                  {
                     bib2gls.debugMessage("message.selecting.entry", entry);
                  }
               }

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS
                 ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED
                 ||selectionMode == SELECTION_PARENTS_BUT_NOT_RECORDED)
               {
                  addHierarchy(entry, entries, data);
               }

               addEntry(entries, entry);

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                 ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED)
               {
                  addDependencies(entry, data);
               }
            }
            else
            {
               bib2gls.debugMessage("message.not_selected", entry);
            }
         }
      }
      else
      {
         // Add all recorded entries in order of records.
         // (This means they'll be in the correct order if sort=use)
         // This still needs to be done for SELECTION_DEPS_BUT_NOT_RECORDED
         // to ensure hierarchy is added.

         bib2gls.debugMessage("message.selecting.by-option", 
            "selection=" + SELECTION_OPTIONS[selectionMode]);

         for (int i = 0; i < records.size(); i++)
         {
            GlsRecord rec = records.get(i);

            Bib2GlsEntry entry = getEntryMatchingRecord(rec, data, hasDuals);

            if (entry != null && !entries.contains(entry))
            {
                bib2gls.debugMessage("message.selecting.entry.record.match",
                  entry.getId(), rec.getLabel(recordLabelPrefix));

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS
                 ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED
                 ||selectionMode == SELECTION_PARENTS_BUT_NOT_RECORDED)
               {
                  addHierarchy(entry, entries, data);
               }

               addEntry(entries, entry);

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                 ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED)
               {
                  addDependencies(entry, data);
               }
            }
         }

         if (compoundEntriesHasRecords == COMPOUND_MGLS_RECORDS_TRUE)
         {
            Vector<String> mrefs = bib2gls.getMglsRefdList();

            if (mrefs == null)
            {
               bib2gls.verboseMessage("message.mgls.nonefound");
            }
            else
            {
               for (String compLabel : mrefs)
               {
                  CompoundEntry comp = bib2gls.getCompoundEntry(compLabel);

                  if (comp != null)
                  {
                     String[] elements = comp.getElements();

                     for (String elem : elements)
                     {
                        Bib2GlsEntry entry = getEntry(elem, data);

                        if (entry != null && !entry.isSelected())
                        {
                           bib2gls.debugMessage("message.selecting.entry.record.mgls",
                            entry.getId(), compLabel);

                           addEntry(entries, entry);

                           if (selectionMode == SELECTION_RECORDED_AND_DEPS
                             ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                             ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                             ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED)
                           {
                              addDependencies(entry, data);
                           }
                        }
                     }
                  }
               }
            }
         }

         for (int i = 0; i < seeRecords.size(); i++)
         {
            GlsSeeRecord rec = seeRecords.get(i);

            String recordLabel = getRecordLabel(rec);

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
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS
                 ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED
                 ||selectionMode == SELECTION_PARENTS_BUT_NOT_RECORDED)
               {
                  addHierarchy(entry, entries, data);
               }

               addEntry(entries, entry);

               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                 ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                 ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED)
               {
                  addDependencies(entry, data);
               }
            }
         }

         if (supplementalRecords != null && supplementalSelection != null)
         {
            for (SupplementalRecord suppRecord : supplementalRecords)
            {
               GlsRecord rec = (GlsRecord) suppRecord;

               Bib2GlsEntry entry = getEntryMatchingRecord(rec, data, false);

               String label = rec.getLabel(recordLabelPrefix);

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
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                       ||selectionMode == SELECTION_RECORDED_AND_PARENTS
                       ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED
                       ||selectionMode == SELECTION_PARENTS_BUT_NOT_RECORDED)
                     {
                        addHierarchy(entry, entries, data);
                     }

                     addEntry(entries, entry);

                     if (selectionMode == SELECTION_RECORDED_AND_DEPS
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                       ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED)
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
                             ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                             ||selectionMode == SELECTION_RECORDED_AND_PARENTS
                             ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED
                             ||selectionMode == SELECTION_PARENTS_BUT_NOT_RECORDED)
                           {
                              addHierarchy(entry, entries, data);
                           }

                           addEntry(entries, entry);

                           if (selectionMode == SELECTION_RECORDED_AND_DEPS
                             ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                             ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                             ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED)
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
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                       ||selectionMode == SELECTION_RECORDED_AND_PARENTS
                       ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED
                       ||selectionMode == SELECTION_PARENTS_BUT_NOT_RECORDED)
                     {
                        addHierarchy(entry, entries, data);
                     }

                     addEntry(entries, entry);

                     if (selectionMode == SELECTION_RECORDED_AND_DEPS
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
                       ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
                       ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED)
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

      // Now it's time to strip entries with records if SELECTION_DEPS_BUT_NOT_RECORDED
      // or SELECTION_PARENTS_BUT_NOT_RECORDED

      if (selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED
       || selectionMode == SELECTION_PARENTS_BUT_NOT_RECORDED)
      {
         for (int i = entries.size()-1; i >= 0; i--)
         {
            Bib2GlsEntry entry = entries.get(i);

            if (entry.hasRecords())
            {
               bib2gls.verboseMessage("message.removing.entry.with.records",
                 entry.getId(), "selection", SELECTION_OPTIONS[selectionMode]);
  
               entries.remove(i);
               setSelected(entry, false);
            }
         }
      }
      else if (prunedEntryMap != null && !prunedEntryMap.isEmpty())
      {
         // restore any pruned cross-references that ended up being
         // selected

         for (Iterator<String> it = prunedEntryMap.keySet().iterator();
              it.hasNext(); )
         {
            String label = it.next();

            if (isEntrySelected(label))
            {
               PrunedEntry pruned = prunedEntryMap.get(label);
               pruned.restore();
            }
         }
      }
   }

   /**
    * Process dependencies. Adds dependencies and (if
    * flatten-lonely=presort) flattens lonely child entries.
    * @param data the list of entries
    * @param entries the list of selected entries
    * @throws Bib2GlsException if a parser error occurs
    */ 
   private void processDeps(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries)
      throws Bib2GlsException
   {
      // add any dependencies

      if (selectionMode == SELECTION_RECORDED_AND_DEPS
        ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE
        ||selectionMode == SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO
        ||selectionMode == SELECTION_DEPS_BUT_NOT_RECORDED)
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
            TeXParser interpreterParser = listener.getParser();

            if (interpreterParser.getControlSequence("bibglsflattenedchildpresort")==null)
            {
               listener.putControlSequence(new FlattenedPreSort());
            }

            if (interpreterParser.getControlSequence("bibglsflattenedhomograph")==null)
            {
               listener.putControlSequence(new FlattenedHomograph());
            }
         }

         flattenLonelyChildren(entries);
      }

   }

   /**
    * Sorts the data if required. Also sorts any identified label
    * lists if set ("sort-label-list" option).
    * @param entries the list of selected entries
    * @param settings the sort settings
    * @param entryGroupField the field to use to store the group
    * label, if required
    * @throws Bib2GlsException if a parser error occurs
    */ 
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

   /**
    * Sorts the data. The sort field is determined by the sort
    * settings.
    * @param entries the list of selected entries
    * @param settings the sort settings
    * @param entryGroupField the field to use to store the group
    * label, if required
    * @param entryType the default entry type to use if the
    * type field not set
    * @throws Bib2GlsException if a parser error occurs
    */ 
   private void sortData(Vector<Bib2GlsEntry> entries, SortSettings settings,
     String entryGroupField, String entryType)
    throws Bib2GlsException
   {
      sortData(entries, settings, settings.getSortField(), entryGroupField,
       entryType);
   }

   /**
    * Sorts the data.
    * @param entries the list of selected entries
    * @param settings the sort settings
    * @param sortField the field to use as the sort value
    * @param entryGroupField the field to use to store the group
    * label, if required
    * @param entryType the default entry type to use if the
    * type field not set
    * @throws Bib2GlsException if a parser error occurs
    */ 
   private void sortData(Vector<Bib2GlsEntry> entries, SortSettings settings,
     String sortField, String entryGroupField, String entryType)
    throws Bib2GlsException
   {
      sortData(entries, settings, sortField, entryGroupField, entryType, false);
   }

   /**
    * Sorts the data.
    * @param entries the list of selected entries
    * @param settings the sort settings
    * @param sortField the field to use as the sort value
    * @param entryGroupField the field to use to store the group
    * label, if required
    * @param entryType the default entry type to use if the
    * type field not set
    * @param overrideType the entryType parameter will override the
    * type field
    * @throws Bib2GlsException if a parser error occurs
    */ 
   private void sortData(Vector<Bib2GlsEntry> entries, SortSettings settings,
     String sortField, String entryGroupField, String entryType,
     boolean overrideType)
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
              settings, sortField, entryGroupField, entryType, overrideType);

         comparator.sortEntries();
      }
      else if (settings.isLetterNumber())
      {
         Bib2GlsEntryLetterNumberComparator comparator = 
            new Bib2GlsEntryLetterNumberComparator(bib2gls, entries, 
              settings, sortField, entryGroupField, entryType, overrideType);

         comparator.sortEntries();
      }
      else if (settings.isRecordCount())
      {
         Bib2GlsEntryRecordCountComparator comparator = 
            new Bib2GlsEntryRecordCountComparator(bib2gls, entries, 
              settings, entryGroupField, entryType, overrideType);

         comparator.sortEntries();
      }
      else if (settings.isNumeric())
      {
         Bib2GlsEntryNumericComparator comparator = 
            new Bib2GlsEntryNumericComparator(bib2gls, entries, 
              settings, sortField, entryGroupField, entryType, overrideType);

         comparator.sortEntries();
      }
      else if (settings.isDateOrTimeSort())
      {
         Bib2GlsEntryDateTimeComparator comparator = 
            new Bib2GlsEntryDateTimeComparator(bib2gls, entries, 
              settings, sortField, entryGroupField, entryType, overrideType);

         comparator.sortEntries();
      }
      else
      {
         try
         {
            Bib2GlsEntryComparator comparator = 
               new Bib2GlsEntryComparator(bib2gls, entries, settings,
                  sortField, entryGroupField, entryType, overrideType);

            comparator.sortEntries();
         }
         catch (ParseException e)
         {
            throw new Bib2GlsException(bib2gls.getMessage(
             "error.invalid.sort.rule", e.getMessage()), e);
         }
      }

      mergeSmallGroups(entries, entryGroupField);
   }

   public boolean isWordifyMathGreekOn()
   {
      return wordifyMathGreek;
   }

   public boolean isWordifyMathSymbolOn()
   {
      return wordifyMathSymbol;
   }

   public String getLocalisationText(String prefix, String suffix)
   {
      return bib2gls.getLocalisationText(prefix, getResourceLocale(), suffix);
   }

   public String getLocalisationTextIfExists(String prefix, String suffix)
   {
      return bib2gls.getLocalisationTextIfExists(prefix, getResourceLocale(), suffix);
   }

   public String getLocalisationText(String prefix, Locale locale, String suffix)
   {
      if (locale == null || locale.equals(getResourceLocale()))
      {
         return getLocalisationText(prefix, suffix);
      }

      String text = bib2gls.getLocalisationText(prefix, locale, suffix, null);

      if (text != null)
      {
         return text;
      }

      return bib2gls.getLocalisationText(prefix, getResourceLocale(), suffix);
   }

   public String getLocalisationTextIfExists(String prefix, Locale locale, String suffix)
   {
      if (locale == null || locale.equals(getResourceLocale()))
      {
         return getLocalisationTextIfExists(prefix, suffix);
      }

      String text = bib2gls.getLocalisationText(prefix, locale, suffix, null);

      if (text != null)
      {
         return text;
      }

      return bib2gls.getLocalisationTextIfExists(prefix, getResourceLocale(), suffix);
   }

   /**
    * Processes the data (internal stage 4).
    * Checks the field map keys, selects required entries, 
    * truncates (if applicable), sorts (if applicable), and writes
    * the glstex file.
    * @throws IOException if I/O error occurs 
    * @throws Bib2GlsException if a syntax error occurs
    * @return the total number of selected entries
    */ 
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
            setSelected(entries.get(i), false);
         }

         bib2gls.verboseMessage("message.truncated", limit);
         entries.setSize(limit);
      }

      if (bib2gls.isVerbose())
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

         if (bib2gls.isVerbose())
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

      // just in case \ifglshaschildren has been used in sort value
      clearChildLists();

      String charSetName = bib2gls.getTeXCharset().name();

      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));
      bib2gls.logEncoding(charSetName);

      // Already checked openout_any in init method

      PrintWriter writer = null;

      Vector<String> allKnownTypes = null;

      try
      {
         writer = new PrintWriter(texFile, charSetName);

         printHeaderComments(writer);

         if (bib2gls.suppressFieldExpansion())
         {
            writer.println("\\glsnoexpandfields");
         }

         bib2gls.writeCommonCommands(writer);

         // provide glossaries early in case they are referenced

         if (triggerType != null)
         {
            provideGlossary(writer, triggerType);
         }

         if (ignoredType != null)
         {
            provideGlossary(writer, ignoredType, false);
         }

         if (secondaryType != null)
         {
            provideGlossary(writer, secondaryType);
         }

         if (copyToGlossary != null)
         {
            writer.println("\\providecommand{\\bibglscopytoglossary}[2]{%");
            writer.println("  \\ifglossaryexists*{#2}%");
            writer.println("  {\\GlsXtrIfInGlossary{#1}{#2}{}{\\glsxtrcopytoglossary{#1}{#2}}}%");
            writer.println("  {}%");
            writer.println("}");
         }

         /*
          \bibglspassimname has been moved here to allow for the
          resource locale but it's only provided so may not have
          an effect.
          TODO provide hook to allow it to be updated?
         */ 

         writer.format("\\providecommand{\\bibglspassimname}{%s}%n",
           getLocalisationText("tag", "passim"));

         boolean provideBibGlsGroupLevel = isGroupLevelsEnabled();

         if (provideBibGlsGroupLevel)
         {
            writer.println("\\ifdef\\glsxtraddgroup");
            writer.println("{\\let\\glsxtraddgroup\\@secondoftwo}");
            writer.println("{\\GlossariesExtraWarning{Hierarchical group levels (resource option `group-level') not supported. Please upgrade glossaries-extra.sty to at least v1.49}}");
         }

         writeAllCapsCsDefs(writer);
         writeTitleCaseCsDefs(writer);

         writeLabelPrefixHooks(writer);

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

            if (provideBibGlsGroupLevel)
            {
               writer.println("\\providecommand{\\bibglshiersubgrouptitle}[3]{\\ifnum#1>0 \\Glsxtrhiername{#2} / \\fi #3}");

               writer.println("  \\providecommand{\\bibglslettergrouphier}[6]{#4#5#3}");
               writer.println("  \\providecommand{\\bibglslettergrouptitlehier}[6]{\\protect\\bibglshiersubgrouptitle{#6}{#5}{\\unexpanded{#1}}}");
               writer.println("  \\providecommand{\\bibglssetlettergrouptitlehier}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglslettergrouphier#1}{\\bibglslettergrouptitlehier#1}}");
            }
            else
            {
               writer.println("  \\providecommand{\\bibglslettergroup}[4]{#4#3}");
               writer.println("  \\providecommand{\\bibglslettergrouptitle}[4]{\\unexpanded{#1}}");
               writer.println("  \\providecommand{\\bibglssetlettergrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglslettergroup#1}{\\bibglslettergrouptitle#1}}");
            }

            // unicode groups:

            if (provideBibGlsGroupLevel)
            {
               writer.println("  \\providecommand{\\bibglsunicodegrouphier}[6]{#4#5#3}");
               writer.println("  \\providecommand{\\bibglsunicodegrouptitlehier}[6]{\\protect\\bibglshiersubgrouptitle{#6}{#5}{\\unexpanded{#1}}}");
               writer.println("  \\providecommand{\\bibglssetunicodegrouptitlehier}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsunicodegrouphier#1}{\\bibglsunicodegrouptitlehier#1}}");
            }
            else
            {
               writer.println("  \\providecommand{\\bibglsunicodegroup}[4]{#4#3}");
               writer.println("  \\providecommand{\\bibglsunicodegrouptitle}[4]{\\unexpanded{#1}}");
               writer.println("  \\providecommand{\\bibglssetunicodegrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsunicodegroup#1}{\\bibglsunicodegrouptitle#1}}");
            }

            // other groups:

            if (provideBibGlsGroupLevel)
            {
               writer.println("  \\providecommand{\\bibglsothergrouphier}[5]{#3#4glssymbols}");
               writer.println("  \\providecommand{\\bibglsothergrouptitlehier}[5]{\\protect\\bibglshiersubgrouptitle{#5}{#4}{\\protect\\glssymbolsgroupname}}");
               writer.println("  \\providecommand{\\bibglssetothergrouptitlehier}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsothergrouphier#1}{\\bibglsothergrouptitlehier#1}}");
            }
            else
            {
               writer.println("  \\providecommand{\\bibglsothergroup}[3]{glssymbols}");
               writer.println("  \\providecommand{\\bibglsothergrouptitle}[3]{\\protect\\glssymbolsgroupname}");
               writer.println("  \\providecommand{\\bibglssetothergrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsothergroup#1}{\\bibglsothergrouptitle#1}}");
            }

            // empty groups:

            if (provideBibGlsGroupLevel)
            {
               writer.println("  \\providecommand{\\bibglsemptygrouphier}[3]{#1#2glssymbols}");
               writer.println("  \\providecommand{\\bibglsemptygrouptitlehier}[3]{\\protect\\bibglshiersubgrouptitle{#3}{#2}{\\protect\\glssymbolsgroupname}}");
               writer.println("  \\providecommand{\\bibglssetemptygrouptitlehier}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsemptygrouphier#1}{\\bibglsemptygrouptitlehier#1}}");
            }
            else
            {
               writer.println("  \\providecommand{\\bibglsemptygroup}[1]{glssymbols}");
               writer.println("  \\providecommand{\\bibglsemptygrouptitle}[1]{\\protect\\glssymbolsgroupname}");
               writer.println("  \\providecommand{\\bibglssetemptygrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsemptygroup#1}{\\bibglsemptygrouptitle#1}}");
            }

            // number groups

            if (provideBibGlsGroupLevel)
            {
               writer.println("  \\providecommand{\\bibglsnumbergrouphier}[5]{#3#4glsnumbers}");
               writer.println("  \\providecommand{\\bibglsnumbergrouptitlehier}[5]{\\protect\\bibglshiersubgrouptitle{#5}{#4}{\\protect\\glsnumbersgroupname}}");
               writer.println("  \\providecommand{\\bibglssetnumbergrouptitlehier}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsnumbergrouphier#1}{\\bibglsnumbergrouptitlehier#1}}");
            }
            else
            {
               writer.println("  \\providecommand{\\bibglsnumbergroup}[3]{glsnumbers}");
               writer.println("  \\providecommand{\\bibglsnumbergrouptitle}[3]{\\protect\\glsnumbersgroupname}");
               writer.println("  \\providecommand{\\bibglssetnumbergrouptitle}[1]{%");
               writer.println("    \\glsxtrsetgrouptitle{\\bibglsnumbergroup#1}{\\bibglsnumbergrouptitle#1}}");
            }

            // merged groups:

            if (mergeSmallGroupLimit > 0)
            {
               writer.println("  \\providecommand{\\bibglsmergedgroupfmt}[4]{#2, \\ifcase#1\\or\\or\\or #3, \\else\\ldots, \\fi #4}");

               if (provideBibGlsGroupLevel)
               {
                  if (bib2gls.hyperrefLoaded())
                  {
                     writer.println("  \\providecommand{\\bibglsmergedgrouphierfmt}[4]{#2, \\texorpdfstring{{\\def\\bibglshiersubgrouptitle##1##2##3{##3}\\ifcase#1\\or\\or\\or #3, \\else\\ldots, \\fi #4}}{\\ifcase#1\\or\\or\\or #3, \\else\\ldots, \\fi #4}}");
                  }
                  else
                  {
                     writer.println("  \\providecommand{\\bibglsmergedgrouphierfmt}[4]{#2, {\\def\\bibglshiersubgrouptitle##1##2##3{##3}\\ifcase#1\\or\\or\\or #3, \\else\\ldots, \\fi #4}}");
                  }

                  writer.println("  \\providecommand{\\bibglsmergedgrouphier}[8]{merged.#1}");
                  writer.println("  \\providecommand{\\bibglshiermergedsubgrouptitle}[3]{#3}");

                  writer.println("  \\providecommand{\\bibglsmergedgrouptitlehier}[8]{%");
                  writer.println("     \\unexpanded{\\ifnum#8=0\\bibglsmergedgroupfmt{#3}{#4}{#5}{#6}\\else\\bibglsmergedgrouphierfmt{#3}{#4}{#5}{#6}\\fi}}");

                  writer.println("  \\providecommand{\\bibglssetmergedgrouptitlehier}[1]{%");
                  writer.println("    \\glsxtrsetgrouptitle{\\bibglsmergedgrouphier#1}{\\bibglsmergedgrouptitlehier#1}}");
               }
               else
               {
                  writer.println("  \\providecommand{\\bibglsmergedgroup}[6]{merged.#1}");
                  writer.println("  \\providecommand{\\bibglsmergedgrouptitle}[6]{%");
                  writer.println("     \\unexpanded{\\bibglsmergedgroupfmt{#3}{#4}{#5}{#6}}}");
                  writer.println("  \\providecommand{\\bibglssetmergedgrouptitle}[1]{%");
                  writer.println("    \\glsxtrsetgrouptitle{\\bibglsmergedgroup#1}{\\bibglsmergedgrouptitle#1}}");
               }
            }

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
               if (provideBibGlsGroupLevel)
               {
                  writer.println("  \\providecommand*{\\bibglsdatetimegrouphierfinalargs}[3]{#1#2}");
                  writer.println("  \\providecommand*{\\bibglsdatetimegrouptitlehierfinalargs}[4]{\\protect\\bibglshiersubgrouptitle{#4}{#3}{#1}}");
                  writer.println("  \\providecommand{\\bibglsdatetimegrouphier}[9]{#1#2#3\\bibglsdatetimegrouphierfinalargs}");
                  writer.println("  \\providecommand{\\bibglsdatetimegrouptitlehier}[9]{\\bibglsdatetimegrouptitlehierfinalargs{#1-#2-#3}}");
                  writer.println("  \\providecommand{\\bibglssetdatetimegrouptitlehier}[1]{%");
                  writer.println("    \\glsxtrsetgrouptitle{\\bibglsdatetimegrouphier#1}{\\bibglsdatetimegrouptitlehier#1}}");
               }
               else
               {
                  writer.println("  \\providecommand{\\bibglsdatetimegroup}[9]{#1#2#3\\@firstofone}");
                  writer.println("  \\providecommand{\\bibglsdatetimegrouptitle}[9]{#1-#2-#3\\@gobble}");
                  writer.println("  \\providecommand{\\bibglssetdatetimegrouptitle}[1]{%");
                  writer.println("    \\glsxtrsetgrouptitle{\\bibglsdatetimegroup#1}{\\bibglsdatetimegrouptitle#1}}");
               }
            }

            // date groups

            if (requiresDate)
            {
               if (provideBibGlsGroupLevel)
               {
                  writer.println("  \\providecommand{\\bibglsdategrouphier}[9]{#1#2#4#7#8}");
                  writer.println("  \\providecommand{\\bibglsdategrouptitlehier}[9]{\\protect\\bibglshiersubgrouptitle{#9}{#8}{#1-#2}}");
                  writer.println("  \\providecommand{\\bibglssetdategrouptitlehier}[1]{%");
                  writer.println("    \\glsxtrsetgrouptitle{\\bibglsdategrouphier#1}{\\bibglsdategrouptitlehier#1}}");
               }
               else
               {
                  writer.println("  \\providecommand{\\bibglsdategroup}[7]{#1#2#4#7}");
                  writer.println("  \\providecommand{\\bibglsdategrouptitle}[7]{#1-#2}");
                  writer.println("  \\providecommand{\\bibglssetdategrouptitle}[1]{%");
                  writer.println("    \\glsxtrsetgrouptitle{\\bibglsdategroup#1}{\\bibglsdategrouptitle#1}}");
               }
            }

            // time groups

            if (requiresTime)
            {
               if (provideBibGlsGroupLevel)
               {
                  writer.println("  \\providecommand{\\bibglstimegrouphier}[9]{#1#2#7#8}");
                  writer.println("  \\providecommand{\\bibglstimegrouptitlehier}[9]{\\protect\\bibglshiersubgrouptitle{#9}{#8}{#1}}");
                  writer.println("  \\providecommand{\\bibglssettimegrouptitlehier}[1]{%");
                  writer.println("    \\glsxtrsetgrouptitle{\\bibglstimegrouphier#1}{\\bibglstimegrouptitlehier#1}}");
               }
               else
               {
                  writer.println("  \\providecommand{\\bibglstimegroup}[7]{#1#2#7}");
                  writer.println("  \\providecommand{\\bibglstimegrouptitle}[7]{#1}");
                  writer.println("  \\providecommand{\\bibglssettimegrouptitle}[1]{%");
                  writer.println("    \\glsxtrsetgrouptitle{\\bibglstimegroup#1}{\\bibglstimegrouptitle#1}}");
               }
            }


            writer.println("}");
            writer.println("{");

            // old version of glossaries-extra.sty:
            // (no support for hierarchical groups)

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
               if (bib2gls.hasNewHyperGroupSupport())
               {
                  writer.println("\\providecommand{\\bibglshypergroup}[2]{\\ifstrempty{#1}{}{\\@gls@hypergroup{#1}{#2}}}");
               }
               else
               {
                  writer.println("\\providecommand{\\bibglshypergroup}[2]{}");
               }
            }
         }

         if (getSavePrimaryLocationSetting() != SAVE_PRIMARY_LOCATION_OFF)
         {
            if (primaryLocCounters == PRIMARY_LOCATION_COUNTERS_COMBINE
                || (primaryLocCounters == PRIMARY_LOCATION_COUNTERS_MATCH 
                      && counters == null))
            {
               writer.println("\\providecommand{\\bibglsprimary}[2]{#2}");
            }
            else if (counters == null)
            {
               writer.println("\\providecommand{\\bibglsprimarylocationgroup}[3]{#3}");
               writer.println("\\providecommand{\\bibglsprimarylocationgroupsep}{\\bibglsdelimN}");
            }
            else
            {
               writer.println("\\providecommand{\\bibglsprimarylocationgroup}{\\bibglslocationgroup}");
               writer.println("\\providecommand{\\bibglsprimarylocationgroupsep}{\\bibglslocationgroupsep}");
            }
         }

         if (locationPrefix != null)
         {
            writer.println("\\providecommand{\\bibglspostlocprefix}{\\ }");

            if (defpagesname)
            {
               writer.format("\\providecommand{\\bibglspagename}{%s}%n",
                 getLocalisationText("tag", "page"));
               writer.format("\\providecommand{\\bibglspagesname}{%s}%n",
                 getLocalisationText("tag", "pages"));
            }

            switch (locationPrefixDef)
            {
               case PROVIDE_DEF_GLOBAL:
                  writeLocPrefixDef(writer);
               break;
               case PROVIDE_DEF_LOCAL_ALL:
                  writer.println("\\appto\\glossarypreamble{%");
                  writeLocPrefixDef(writer);
                  writer.println("}");
               break;
               case PROVIDE_DEF_LOCAL_INDIVIDUAL:

                  if (allKnownTypes == null)
                  {
                     allKnownTypes = getAllKnownTypes();
                  }

                  if (allKnownTypes.isEmpty())
                  {
                     writer.println("\\appto\\glossarypreamble{%");
                     writeLocPrefixDef(writer);
                     writer.println("}");
                  }
                  else
                  {
                     for (String t : allKnownTypes)
                     {
                        writer.format("\\apptoglossarypreamble[%s]{%%%n", t);
                        writeLocPrefixDef(writer);
                        writer.println("}");
                     }
                  }

               break;
            }
         }

         if (locationSuffix != null)
         {
            switch (locationSuffixDef)
            {
               case PROVIDE_DEF_GLOBAL:
                  writeLocSuffixDef(writer);
               break;
               case PROVIDE_DEF_LOCAL_ALL:
                  writer.println("\\appto\\glossarypreamble{%");
                  writeLocSuffixDef(writer);
                  writer.println("}");
               break;
               case PROVIDE_DEF_LOCAL_INDIVIDUAL:

                  if (allKnownTypes == null)
                  {
                     allKnownTypes = getAllKnownTypes();
                  }

                  if (allKnownTypes.isEmpty())
                  {
                     writer.println("\\appto\\glossarypreamble{%");
                     writeLocSuffixDef(writer);
                     writer.println("}");
                  }
                  else
                  {
                     for (String t : allKnownTypes)
                     {
                        writer.format("\\apptoglossarypreamble[%s]{%%%n", t);
                        writeLocSuffixDef(writer);
                        writer.println("}");
                     }
                  }

               break;
            }
         }

         if (saveDefinitionIndex)
         {
            writer.format("\\providecommand{\\bibglsdefinitionindex}[1]{\\glsxtrusefield{#1}{%s}}%n", DEFINITION_INDEX_FIELD);
         }

         if (saveUseIndex)
         {
            writer.format("\\providecommand{\\bibglsuseindex}[1]{\\glsxtrusefield{#1}{%s}}%n", USE_INDEX_FIELD);
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
              || (needsEarlyHierarchy()
                    && flattenLonely != FLATTEN_LONELY_PRE_SORT))
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

            if (!isSaveLocationsOff())
            {
               if (entry instanceof Bib2GlsDualEntry
                 && !(combineDualLocations == COMBINE_DUAL_LOCATIONS_OFF
                   ||combineDualLocations == COMBINE_DUAL_LOCATIONS_BOTH))
               {
                  boolean isPrimary = ((Bib2GlsDualEntry)entry).isPrimary();

                  if (((combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY 
                        || combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY_RETAIN_PRINCIPAL
                        ) && isPrimary)
                    ||((combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL 
                        || combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL_RETAIN_PRINCIPAL
                        ) && !isPrimary))
                  {
                     entry.updateLocationList();
                  }
               }
               else
               {
                  entry.updateLocationList();
               }
            }

            if (flattenLonely == FLATTEN_LONELY_FALSE && !needsEarlyHierarchy())
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
               addToSecondaryList(entry, secondaryList);
            }
         }

         if (dualEntries != null)
         {
            if (flattenLonely == FLATTEN_LONELY_POST_SORT
                || (needsEarlyHierarchy()
                       && flattenLonely != FLATTEN_LONELY_PRE_SORT))
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

               if (!isSaveLocationsOff()
                && !(combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY
                    || combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY_RETAIN_PRINCIPAL
                   ))
               {
                  entry.updateLocationList();
               }

               if (flattenLonely == FLATTEN_LONELY_FALSE 
                     && !needsEarlyHierarchy())
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
                  addToSecondaryList(entry, secondaryList);
               }
            }
         }

         if (secondaryList != null)
         {
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
                     GlsRecord rec = records.get(i);

                     Bib2GlsEntry entry = getEntryMatchingRecord(rec, 
                        secondaryList, false);

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
                  for (GlsRecord rec : records)
                  {
                     Bib2GlsEntry entry = getEntryMatchingRecord(rec, 
                        secondaryList, false);

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
                       "secondarygroup", secondaryType, true);

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

         // Only define compound sets provided with @compoundset in
         // this resource .

         if (compoundEntries != null && compoundEntriesDef != COMPOUND_DEF_FALSE)
         {
            boolean all = (compoundEntriesDef == COMPOUND_DEF_ALL);

            writer.println("\\providecommand*{\\bibglsdefcompoundset}[4]{\\multiglossaryentry[#1]{#2}[#3]{#4}}");

            for (Iterator<CompoundEntry> it=compoundEntries.values().iterator();
                 it.hasNext(); )
            {
               CompoundEntry comp = it.next();

               if (all || bib2gls.isMglsRefd(comp.getLabel()))
               {
                  writer.format("\\bibglsdefcompoundset{%s}{%s}{%s}{%s}%n",
                    comp.getOptions(), comp.getLabel(), comp.getMainLabel(),
                    comp.getElementList());
               }
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

   /**
    * Adds the given entry to the secondary list.
    * @param entry the entry
    * @param secondaryList the secondary list
    */ 
   private void addToSecondaryList(Bib2GlsEntry entry, 
      Vector<Bib2GlsEntry> secondaryList)
   {
      String id = entry.getId();

      if (secondaryFieldPatterns == null)
      {
         bib2gls.verboseMessage("message.add.secondary.entry.no_filter", id);
         secondaryList.add(entry);
      }
      else
      {
         boolean matches = !secondaryNotMatch(entry);

         if (secondaryMatchAction == MATCH_ACTION_ADD)
         {
            bib2gls.debugMessage("message.secondary.filter", id, 
              "add", matches);

            if (matches)
            {
               bib2gls.verboseMessage("message.add.secondary.entry", id);
               secondaryList.add(entry);
            }
         }
         else
         {
            bib2gls.debugMessage("message.secondary.filter", id, 
              "filter", matches);

            if (!matches)
            {
               bib2gls.verboseMessage("message.add.secondary.entry", id);
               secondaryList.add(entry);
            }
         }
      }
   }

   /**
    * Writes the definition of {@code \\bibglslocprefix}.
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
   private void writeLocPrefixDef(PrintWriter writer)
     throws IOException
   {
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
   }

   /**
    * Writes the definition of {@code \\bibglslocsuffix}.
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
   private void writeLocSuffixDef(PrintWriter writer)
     throws IOException
   {
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
   }

   /**
    * Writes the definition of {@code \\bibglscontributor}.
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
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

   /**
    * Writes the code to provide a glossary.
    * @param writer the file writer stream
    * @param type the glossary label
    * @throws IOException if I/O error occurs
    */ 
   private void provideGlossary(PrintWriter writer, String type)
     throws IOException
   {
      provideGlossary(writer, type, true);
   }

   private void provideGlossary(PrintWriter writer, String type, boolean star)
     throws IOException
   {
      if (bib2gls.isProvideGlossariesOn())
      {
         if (!bib2gls.isKnownGlossary(type))
         {
            writer.print("\\provideignoredglossary");

            if (star)
            {
               writer.print('*');
            }

            writer.format("{%s}%n", type);
            bib2gls.addGlossary(type);
         }
      }
      else
      {
         writer.print("\\provideignoredglossary");

         if (star)
         {
            writer.print('*');
         }

         writer.format("{%s}%n", type);
      }
   }

   /**
    * Writes the code that defines the entry.
    * @param writer the file writer stream
    * @param entry the entry
    * @throws IOException if I/O error occurs
    * @throws Bib2GlsException if syntax error occurs
    */ 
   private void writeBibEntryDef(PrintWriter writer, Bib2GlsEntry entry)
     throws IOException,Bib2GlsException
   {
      String id = entry.getId();

      if (bib2gls.isEntrySelected(id))
      {
         if (dupLabelSuffix == null)
         {
            if (writeAction == WRITE_ACTION_DEFINE)
            {
               bib2gls.warningMessage("warning.entry.already.defined",
                id, toString());
            }
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

      if (bib2gls.isProvideGlossariesOn())
      {
         String type = entry.getFieldValue("type");

         if (type != null && !bib2gls.isKnownGlossary(type))
         {
            writer.format("\\provideignoredglossary*{%s}%n", type);

            bib2gls.addGlossary(type);
         }
      }

      entry.writeBibEntry(writer);

      if (saveOriginalId != null && !bib2gls.isKnownField(saveOriginalId)
          && (saveOriginalIdAction == SAVE_ORIGINAL_ALWAYS
              || saveOriginalIdAction == SAVE_ORIGINAL_NO_OVERRIDE
              || !entry.getId().equals(entry.getOriginalId()))
         )
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
         boolean writeLocations = true;

         if (entry instanceof Bib2GlsDualEntry)
         {
            boolean isPrimary = ((Bib2GlsDualEntry)entry).isPrimary();

            if ((combineDualLocations == COMBINE_DUAL_LOCATIONS_PRIMARY 
                  && !isPrimary)
              ||(combineDualLocations == COMBINE_DUAL_LOCATIONS_DUAL 
                  && isPrimary))
            {
               writeLocations = false;
            }
         }

         if (writeLocations)
         {
            String val = entry.getPrimaryRecordList();

            if (val != null)
            {
               writer.format("\\GlsXtrSetField{%s}{primarylocations}{%s}%n",
                 id, val);
            }
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

      if (saveSiblingCount)
      {
         String parentId = entry.getParent();

         if (parentId != null)
         {
            Bib2GlsEntry parentEntry = getEntry(parentId);

            if (parentEntry != null)
            {
               Vector<Bib2GlsEntry> children = parentEntry.getChildren();

               int siblingCount = 0;

               for (Bib2GlsEntry child : children)
               {
                  String childId = child.getId();

                  if (!childId.equals(id))
                  {
                     siblingCount++;

                     writer.format("\\glsxtrfieldlistadd{%s}{siblinglist}{%s}%n",
                        id, childId);
                  }
               }

               writer.format("\\GlsXtrSetField{%s}{siblingcount}{%d}%n", id,
                 siblingCount);
            }
         }
      }

      if (saveRootAncestor)
      {
         Bib2GlsEntry root = entry;

         if (dualData != null && entry instanceof Bib2GlsDualEntry
              && !((Bib2GlsDualEntry)entry).isPrimary())
         {
            root = entry.getHierarchyRoot(dualData);
         }
         else
         {
            root = entry.getHierarchyRoot(bibData);
         }

         if (root != entry)
         {
            writer.format("\\GlsXtrSetField{%s}{rootancestor}{%s}%n", id,
              root.getId());
         }
      }

      if (saveOriginalEntryType != null && !bib2gls.isKnownField(saveOriginalEntryType)
          && (saveOriginalEntryTypeAction == SAVE_ORIGINAL_ALWAYS
              || saveOriginalEntryTypeAction == SAVE_ORIGINAL_NO_OVERRIDE
              || !entry.getEntryType().equals(entry.getOriginalEntryType()))
          )
      {
         writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
           id, saveOriginalEntryType, entry.getOriginalEntryType());
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

      if (additionalUserFields != null)
      {
         for (String field : additionalUserFields)
         {
            String val = entry.getFieldValue(field);

            if (val != null)
            {
               writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
                 id, field, val);
            }
         }
      }

      if (saveFromAlias != null)
      {
         String alias = entry.getAlias();

         if (alias != null)
         {
            writer.format("\\glsxtrapptocsvfield{%s}{%s}{%s}%n",
              alias, saveFromAlias, id);
         }
      }

      if (saveFromSeeAlso != null)
      {
         String[] seealso = entry.getAlsoCrossRefs();

         if (seealso != null)
         {
            for (String xr : seealso)
            {
               writer.format("\\glsxtrapptocsvfield{%s}{%s}{%s}%n",
                 xr, saveFromSeeAlso, id);
            }
         }
      }

      if (saveFromSee != null)
      {
         String[] see = entry.getCrossRefs();

         if (see != null)
         {
            for (String xr : see)
            {
               writer.format("\\glsxtrapptocsvfield{%s}{%s}{%s}%n",
                 xr, saveFromSee, id);
            }
         }
      }

      if (saveCrossRefTail != null)
      {
         String tail = entry.getCrossRefTail();

         if (tail != null)
         {
            writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
              id, saveCrossRefTail, tail);
         }
      }

      if (saveDefinitionIndex)
      {
         String val = entry.getFieldValue(DEFINITION_INDEX_FIELD);

         if (val != null)
         {
            writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
              id, DEFINITION_INDEX_FIELD, val);
         }
      }

      if (saveUseIndex)
      {
         String val = entry.getFieldValue(USE_INDEX_FIELD);

         if (val != null)
         {
            writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
              id, USE_INDEX_FIELD, val);
         }
      }
   }

   /**
    * Writes the code that copies the entry.
    * @param writer the file writer stream
    * @param entry the entry
    * @throws IOException if I/O error occurs
    */ 
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

   /**
    * Writes the code that defines or copies the entry.
    * The "action" setting determines whether to define the entry or
    * copy the entry.
    * @param writer the file writer stream
    * @param entry the entry
    * @throws IOException if I/O error occurs
    */ 
   private void writeBibEntry(PrintWriter writer, Bib2GlsEntry entry)
     throws IOException,Bib2GlsException
   {
      switch (writeAction)
      {
         case WRITE_ACTION_DEFINE:
           writeBibEntryDef(writer, entry);
         break;
         case WRITE_ACTION_PROVIDE:
           writer.format("\\ifglsentryexists{%s}{}{%n", entry.getId());
           writeBibEntryDef(writer, entry);
           writer.println("}");
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

      if (copyToGlossary != null)
      {
         try
         {
            for (FieldEvaluation eval : copyToGlossary)
            {
               String label = eval.getStringValue(entry);

               if (label != null)
               {
                  writer.format("\\bibglscopytoglossary{%s}{%s}%n",
                     entry.getId(), label);
               } 
            }
         }
         catch (Exception e)
         {
            bib2gls.warning(e.getMessage());
            bib2gls.debug(e);
         }
      }
   }

   /**
    * Writes the code that defines the group titles.
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
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

         writer.format("\\%s{%s}%n", groupTitle.getCsSetName(), groupTitle);
      }

      writer.println();

      if (lastTitle != null)
      {
         writer.format("\\bibglssetlastgrouptitle{\\%s}{%s}%n",
           lastTitle.getCsLabelName(), lastTitle);
      }

      return allowHyper;
   }

   /**
    * Writes the code that copies an entry.
    * @param writer the file writer stream
    * @param entry the entry to copy
    * @throws IOException if I/O error occurs
    */ 
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

   /**
    * Writes the code that sets the widest name.
    * @param writer the file writer stream
    * @param font the font to use to determine the text width
    * @param frc the font render context
    * @throws IOException if I/O error occurs
    */ 
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

   /**
    * Writes the code that provides the custom titlecase entry reference commands.
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
   private void writeTitleCaseCsDefs(PrintWriter writer)
      throws IOException
   {
      if (titleCaseCommands != null)
      {
         writer.println("\\providecommand*{\\BibGlsTitleCase}[1]{\\BibGlsLongOrText{#1}}");
         writer.println("\\providecommand*{\\BibGlsTitleCasePlural}[1]{\\BibGlsLongOrTextPlural{#1}}");

         writer.println("\\providecommand*{\\BibGlsLongOrText}[1]{\\ifglshaslong{#1}{{\\glssetabbrvfmt{\\glscategory{#1}}\\glslongfont{\\glsxtrfieldtitlecase{#1}{long}}}}{\\glsxtrfieldtitlecase{#1}{text}}}");
         writer.println("\\providecommand*{\\BibGlsLongOrTextPlural}[1]{\\ifglshaslong{#1}{{\\glssetabbrvfmt{\\glscategory{#1}}\\glslongfont{\\glsxtrfieldtitlecase{#1}{longplural}}}}{\\glsxtrfieldtitlecase{#1}{plural}}}");
         writer.println("\\providecommand*{\\BibGlsShortOrText}[1]{\\ifglshasshort{#1}{{\\glssetabbrvfmt{\\glscategory{#1}}\\glsabbrvfont{\\glsxtrfieldtitlecase{#1}{short}}}}{\\glsxtrfieldtitlecase{#1}{text}}}");
         writer.println("\\providecommand*{\\BibGlsShortOrTextPlural}[1]{\\ifglshasshort{#1}{{\\glssetabbrvfmt{\\glscategory{#1}}\\glsabbrvfont{\\glsxtrfieldtitlecase{#1}{shortplural}}}}{\\glsxtrfieldtitlecase{#1}{plural}}}");

         for (Iterator<String> it=titleCaseCommands.keySet().iterator(); it.hasNext();)
         {
            String titleCs = it.next();
            String defn = titleCaseCommands.get(titleCs);

            writer.println(String.format("\\providecommand*{\\%s}[2][]{%s}",
              titleCs, defn));
         }
      }
   }

   /**
    * Writes the code that provides the all caps entry reference commands.
    * These commands may already be defined by glossaries-extra.sty
    * or may already have been defined in an earlier resource set.
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
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

   /**
    * Writes the code that can be used to pick up the label prefixes.
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
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

      if (customLabelPrefixes != null)
      {
         writer.println("\\providecommand{\\bibglscustomlabelprefixes}[1]{}");

         writer.print("\\bibglscustomlabelprefixes{");

         for (int i = 0; i < customLabelPrefixes.size(); i++)
         {
            if (i > 0)
            {
               writer.print(',');
            }

            writer.print(customLabelPrefixes.get(i));
         }

         writer.println("}");
      }
   }

   /**
    * Writes the code that provides the abbreviation font commands.
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
   public void writeAbbrvFontCommands(PrintWriter writer)
      throws IOException
   {
      if (!abbrvFontCommandsWritten)
      {
         writer.println("\\ifdef\\glsuseabbrvfont");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuseabbrvfont}{\\glsuseabbrvfont}");
         writer.println("}%");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuseabbrvfont}[2]{{\\glssetabbrvfmt{#2}\\glsabbrvfont{#1}}}");
         writer.println("}%");

         writer.println("\\ifdef\\glsuselongfont");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuselongfont}{\\glsuselongfont}");
         writer.println("}%");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuselongfont}[2]{{\\glssetabbrvfmt{#2}\\glslongfont{#1}}}");
         writer.println("}%");

         writer.println("\\providecommand*{\\bibglsuseotherfont}[2]{#1}");

         abbrvFontCommandsWritten = true;
      }
   }

   /**
    * Calculates the width of the entry's name and updates the
    * current widest name, if applicable.
    * @param entry the entry
    * @param font the font to use to determine the text width
    * @param frc the font render context
    */ 
   private void updateWidestName(Bib2GlsEntry entry, 
     Font font, FontRenderContext frc)
   {
      updateWidestName(entry, null, font, frc);
   }

   /**
    * Calculates the width of the entry's name and updates the
    * current widest name, if applicable.
    * @param entry the entry
    * @param entryType the associated glossary label or null if not
    * known
    * @param font the font to use to determine the text width
    * @param frc the font render context
    */ 
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

   /**
    * Writes the hyper group definition for the given entry.
    * @param entry the entry
    * @param writer the file writer stream
    * @throws IOException if I/O error occurs
    */ 
   private void writeHyperGroupDef(Bib2GlsEntry entry, PrintWriter writer)
     throws IOException
   {
      String key = entry.getGroupId();

      if (key == null)
      {
         bib2gls.debugMessage("message.no.group.id", "writeHyperGroupDef",
            entry.getId());
         return;
      }

      GroupTitle groupTitle = groupTitleMap.get(key);

      if (groupTitle == null)
      {
         bib2gls.debugMessage("message.no.group.found", "writeHyperGroupDef", key);
         return;
      }

      if (!groupTitle.isDone())
      {
         writer.format("\\bibglshypergroup{%s}{\\%s%s}%n", groupTitle.getType(),
               groupTitle.getCsLabelName(), groupTitle);

         groupTitle.mark();

         writer.println();
      }
   }

   /**
    * Assigns the group to the given entry.
    * @param grpTitle the group information 
    * @param entry the entry
    */ 
   public void putGroupTitle(GroupTitle grpTitle, Bib2GlsEntry entry)
   {
      if (groupTitleMap != null)
      {
         grpTitle.setSupportsHierarchy(isGroupLevelsEnabled(), 
          Math.max(0, entry.getLevel(null)));

         String key = grpTitle.getKey();

         entry.setGroupId(key);

         groupTitleMap.put(key, grpTitle);
      }
   }

   /**
    * Gets the group data associated with the given glossary, id and
    * parent. The key for the mapping is obtained from a combination
    * of the entry type (which may be null), the id, and the entry's
    * parent (which may be null).
    * @param entryType the glossary type
    * @param id numeric identifier associated with the group
    * @param parent the parent
    * @return the group data or null if not available
    */ 
   public GroupTitle getGroupTitle(String entryType, long id, String parent)
   {
      if (groupTitleMap != null)
      {
         return groupTitleMap.get(GroupTitle.getKey(entryType, id, parent));
      }

      return null;
   }

   /**
    * Determines whether or not hierarchical groups are allowed.
    * @return true if hierarchical groups are allowed
    */ 
   public boolean isGroupLevelsEnabled()
   {
      return !(groupLevelSetting == GROUP_LEVEL_SETTING_EXACT 
             && groupLevelSettingValue == 0);
   }

   /**
    * Assigns the group data to the entry's group field.
    * @param entry the entry
    * @param groupField the field used to store the group label
    * @param groupFieldValue the field value (the LaTeX code that
    * should expand to the group label)
    * @param groupTitle the group data
    */ 
   public void assignGroupField(Bib2GlsEntry entry, 
     String groupField, String groupFieldValue, GroupTitle groupTitle)
   {
      entry.putField(groupField, groupFieldValue);

      if (groupTitle != null)
      {
         entry.setGroupId(groupTitle.getKey());
      }
   }

   /**
    * Merges small groups, if supported.
    * Sorting must be done first.
    * @param entries list of sorted entries
    * @param groupField the field used to store the group label
    */ 
   public void mergeSmallGroups(Vector<Bib2GlsEntry> entries, String groupField)
   {
      if (groupTitleMap == null || groupTitleMap.isEmpty() || mergeSmallGroupLimit < 1)
      {
         return;
      }

      int count = 0;
      String prevKey = null;
      GroupTitle prevTitle = null;

      Vector<GroupTitle> pending = new Vector<GroupTitle>();

      for (int i = 0; i < entries.size(); i++)
      {
         Bib2GlsEntry entry = entries.get(i);
         String key = entry.getGroupId();
         GroupTitle groupTitle = groupTitleMap.get(key);
         boolean doCheck = false;

         if (groupTitle == null)
         {
            bib2gls.debugMessage("message.no.group.id", "mergeSmallGroups",
              entry.getId());

            prevTitle = null;
            count = 0;
            continue;
         }
         else if (key.equals(prevKey))
         {
            count++;
            groupTitle.setEndIndex(i);
         }
         else if (count <= mergeSmallGroupLimit)
         {
            if (prevTitle != null)
            {
               if (groupTitle.getLevel() != prevTitle.getLevel())
               {
                  doCheck = true;
               }

               if (pending.isEmpty() 
                    || pending.lastElement().getLevel() == prevTitle.getLevel())
               {
                  pending.add(prevTitle);
                  prevTitle = null;
               }
            }

            groupTitle.setStartIndex(i);
            groupTitle.setEndIndex(i);
            count = 1;
         }
         else
         {
            groupTitle.setStartIndex(i);
            groupTitle.setEndIndex(i);
            prevTitle = null;
            doCheck = true;
         }

         if (doCheck)
         {
            if (pending.size() > 1)
            {
               MergedGroupTitles mergedTitles = new MergedGroupTitles(bib2gls, pending);
               String groupFieldValue = String.format("\\%s%s",
                 mergedTitles.getCsLabelName(), mergedTitles.format());

               for (GroupTitle grp : pending)
               {
                  for (int j = grp.getStartIndex(); j <= grp.getEndIndex(); j++)
                  {
                     Bib2GlsEntry e = entries.get(j);
                     putGroupTitle(mergedTitles, e);
                     e.putField(groupField, groupFieldValue);
                  }
               }
            }
            else
            {
               for (GroupTitle grp : pending)
               {
                  grp.resetIndexes();
               }
            }

            count = 1;
            pending.clear();

            if (prevTitle != null)
            {
               pending.add(prevTitle);
               //System.out.println("adding previous: "+prevTitle);
            }
         }

         prevKey = key;
         prevTitle = groupTitle;
      }

      if (prevTitle != null && !pending.isEmpty())
      {

         if (count <= mergeSmallGroupLimit && prevTitle != null)
         {
            if (pending.isEmpty() 
                 || pending.lastElement().getLevel() == prevTitle.getLevel())
            {
               pending.add(prevTitle);
            }
         }

         if (pending.size() > 1)
         {
            MergedGroupTitles mergedTitles = new MergedGroupTitles(bib2gls, pending);
            String groupFieldValue = String.format("\\%s%s",
              mergedTitles.getCsLabelName(), mergedTitles.format());

            for (GroupTitle grp : pending)
            {
               for (int j = grp.getStartIndex(); j <= grp.getEndIndex(); j++)
               {
                  Bib2GlsEntry e = entries.get(j);
                  putGroupTitle(mergedTitles, e);
                  e.putField(groupField, groupFieldValue);
               }
            }
         }
         else
         {
            for (GroupTitle grp : pending)
            {
               grp.resetIndexes();
            }
         }
      }
   }

   /**
    * Determines whether or not to use the group field for the given
    * entry.
    * @param entry the entry
    * @param entries the list of entries
    * @return true if the group field should be used
    */ 
   public boolean useGroupField(Bib2GlsEntry entry, Vector<Bib2GlsEntry> entries)
   {
      if (bib2gls.useGroupField())
      {
         switch (groupLevelSetting)
         {
            case GROUP_LEVEL_SETTING_EXACT:

              if (groupLevelSettingValue == 0)
              {
                 return !entry.hasParent();
              }
              else
              {
                 return entry.getLevel(entries) == groupLevelSettingValue;
              }

            case GROUP_LEVEL_SETTING_LESS_THAN:

              return entry.getLevel(entries) < groupLevelSettingValue;

            case GROUP_LEVEL_SETTING_LESS_THAN_EQ:

              return entry.getLevel(entries) <= groupLevelSettingValue;

            case GROUP_LEVEL_SETTING_GREATER_THAN:

              return entry.getLevel(entries) > groupLevelSettingValue;

            case GROUP_LEVEL_SETTING_GREATER_THAN_EQ:

              return entry.getLevel(entries) >= groupLevelSettingValue;

         }
      }

      return false;
   }

   /* METHODS: fetching or setting data */

   /**
    * Gets the flipped label.
    * If the label starts with a known prefix, the flipped label is
    * the label with the opposite prefix (for
    * example, the dual and primary prefixes are swapped).
    * the dual prefix.
    * @return the flipped label or null if can't be determined
    */ 
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

   public void setSelected(Bib2GlsEntry entry, boolean state)
   {
      entry.setSelected(state);

      if (state)
      {
         if (selectedEntries == null)
         {
            selectedEntries = new Vector<String>();
         }

         selectedEntries.add(entry.getId());
      }
      else
      {
         if (selectedEntries != null)
         {
            selectedEntries.remove(entry.getId());
         }
      }
   }

   /**
    * Determines whether or not the given entry has been selected.
    * @param label the entry label
    * @return true if the entry has been selected
    */ 
   public boolean isEntrySelected(String label)
   {
      if (selectedEntries != null && selectedEntries.contains(label))
      {
         return true;
      }

      return bib2gls.isEntrySelected(label);
   }

   /**
    * Gets the entry with the given label. The search is performed
    * on the primary list first and then on the dual list.
    * @param label the search label
    * @return the entry or null if not found
    */ 
   public Bib2GlsEntry getEntry(String label)
   {
      return getEntry(label, false);
   }

   /**
    * Gets the entry with the given label. The search is performed
    * on the primary list first and then on the dual list.
    * @param label the search label
    * @param tryFlipping if true also search on the flipped label
    * @return the entry or null if not found
    */ 
   public Bib2GlsEntry getEntry(String label, boolean tryFlipping)
   {
      if (bibData == null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.get.entry.no.data", label));
         return null;
      }

      Bib2GlsEntry entry = getEntry(label, bibData, tryFlipping);

      if (entry != null)
      {
         return entry;
      }

      return getEntry(label, dualData, tryFlipping);
   }

   /**
    * Gets the entry with the given label in the given list.
    * @param label the search label
    * @param data the list of entries to search
    * @return the entry or null if not found
    */ 
   public Bib2GlsEntry getEntry(String label, Vector<Bib2GlsEntry> data)
   {
      return getEntry(label, data, false);
   }

   /**
    * Gets the entry with the given label in the given list.
    * @param label the search label
    * @param data the list of entries to search
    * @param tryFlipping if true also search on the flipped label
    * @return the entry or null if not found
    */ 
   public Bib2GlsEntry getEntry(String label, Vector<Bib2GlsEntry> data,
     boolean tryFlipping)
   {
      if (data == null)
      {
         throw new NullPointerException();
      }

      String flippedLabel = (tryFlipping ? flipLabel(label) : null);

      for (Bib2GlsEntry entry : data)
      {
         String id = entry.getId();

         if (id.equals(label))
         {
            return entry;
         }

         if (tryFlipping)
         {
            if (id.equals(flippedLabel))
            {
               return entry;
            }

            String flippedId = flipLabel(id);

            if (flippedId.equals(label) || flippedId.equals(flippedLabel))
            {
               return entry;
            }
         }
      }

      return null;
   }

   /**
    * Gets the entry with the given label in the given list.
    * The entry must be a Bib2GlsEntry object for the match to be
    * successful. The provided list may include other BibData objects, such
    * as BibPreamble or BibString.
    * @param label the search label
    * @param data the list of entries to search
    * @return the entry or null if not found
    */ 
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

   /**
    * Gets the list of "other" elements for the given compound
    * entry. This method searches for the entries identified by the
    * compound entry's element list that don't match the main
    * element.
    * @param comp the compound entry
    */ 
   public Vector<Bib2GlsEntry> getOtherElements(CompoundEntry comp)
   {
      Vector<Bib2GlsEntry> list = new Vector<Bib2GlsEntry>();

      String[] elements = comp.getElements();
      String mainLabel = comp.getMainLabel();

      for (String elem : elements)
      {
         if (!elem.equals(mainLabel))
         {
            Bib2GlsEntry other = getEntry(elem, bibData);

            if (other == null && dualData != null)
            {
               other = getEntry(elem, dualData);
            }

            if (other != null)
            {
               list.add(other);
            }
         }
      }

      return list;
   }

   /**
    * Determines if given record matches any of the labels.
    * @param primaryId label of primary entry
    * @param dualId label of dual entry
    * @param tertiaryId tertiary label
    * @param record the record under investigation
    * @return the record if the record label matches 
    * or null if no match
    */ 
   public GlsRecord getRecord(String primaryId, String dualId,
     String tertiaryId, GlsRecord rec)
   {
      return rec.getRecord(this, primaryId, dualId, tertiaryId);
   }

   /**
    * Gets the entry that matches the given record. Searches both
    * the primary and dual data lists.
    * @param record the record
    * @return the entry that matches the record label or null if not
    * found
    */ 
   public Bib2GlsEntry getEntryMatchingRecord(GlsRecord rec)
   {
      return rec.getEntry(this, bibData, dualData);
   }

   /**
    * Gets the entry that matches the given record.
    * @param rec the record
    * @param data the list of data to search for the entry
    * @param tryFlipping if true try matching the flipped label if
    * no match on the label
    * @return the entry that matches the record label or null if not
    * found
    */ 
   public Bib2GlsEntry getEntryMatchingRecord(GlsRecord rec,
      Vector<Bib2GlsEntry> data, boolean tryFlipping)
   {
      return rec.getEntry(this, data, tryFlipping);
   }

   @Deprecated
   public String getRecordLabel(GlsRecord rec)
   {
      String label = rec.getLabel();

      if (recordLabelPrefix == null || label.startsWith(recordLabelPrefix))
      {
         return label;
      }

      return recordLabelPrefix+label;
   }

   /**
    * Gets the prefixed label associated with the record.
    * @param rec the cross-reference record
    * @return the prefixed label
    */ 
   public String getRecordLabel(GlsSeeRecord rec)
   {
      if (recordLabelPrefix == null)
      {
         return rec.getLabel();
      }

      return recordLabelPrefix+rec.getLabel();
   }

   /**
    * Adds the given entry to the list of selected entries. Also
    * sets the entry's selected status to true. Doesn't add the
    * entry if it has already been added.
    * @param entries the list of selected entries
    * @param entry the entry to add to the list
    */ 
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
      setSelected(entry, true);
   }

   /**
    * Clears the child list for each entry.
    */ 
   private void clearChildLists()
   {
      if (childListUpdated)
      {
         for (int i = 0, n = bibData.size(); i < n; i++)
         {
            Bib2GlsEntry entry = bibData.get(i);
            entry.clearChildren();
         }

         childListUpdated = false;
      }
   }

   /**
    * Update the child list for each entry.
    */ 
   public void updateChildLists()
   {
      if (!childListUpdated)
      {
         for (int i = 0; i < bibData.size(); i++)
         {
            Bib2GlsEntry entry = bibData.get(i);

            if (!entry.isSelected()) continue;

            String parentId = entry.getParent();

            if (!(parentId == null || parentId.isEmpty()))
            {
               Bib2GlsEntry parent = getEntry(parentId, bibData);

               if (parent != null && parent.isSelected())
               {
                  parent.addChild(entry);
               }
            }
         }

         childListUpdated = true;
      }
   }

   /**
    * Determines if the hierarchical information needs to be known
    * early.
    */ 
   private boolean needsEarlyHierarchy()
   {
      return saveChildCount || saveSiblingCount || saveRootAncestor;
   }

   /**
    * Checks the parent value for the given entry.
    * If the flatten setting is on, the parent field will be
    * removed. If the entry has a parent, it's searched for and
    * added if found otherwise the parent field is removed.
    * @param entry the entry
    * @param i the entry's index in the list
    * @param list the list of entries
    */ 
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
            if (flattenLonely != FLATTEN_LONELY_FALSE || needsEarlyHierarchy())
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
            if (flattenLonely != FLATTEN_LONELY_FALSE || needsEarlyHierarchy())
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

   /**
    * Gets the control sequence name to use for a flattened lonely
    * child.
    * @return the control sequence name
    */ 
   private String flattenLonelyCsName()
   {
      return flattenLonely == FLATTEN_LONELY_PRE_SORT ?
         "bibglsflattenedchildpresort" : "bibglsflattenedchildpostsort";
   }

   /**
    * Flattens a child entry.
    * @param parent the parent entry
    * @param child the child entry
    * @param entries the list of entries
    */ 
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

   /**
    * Gets the bib value as a TeX parser group.
    * @param bibValue the bib value
    * @param listener the listener used to parse the value
    */ 
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

   /**
    * Flattens all lonely children in the given list.
    * @param entries the list of entries
    */ 
   private void flattenLonelyChildren(Vector<Bib2GlsEntry> entries)
   {
      // This has to be done first to ensure the parent-child
      // information is all correct.

      for (int i = 0, n = entries.size(); i < n; i++)
      {
         Bib2GlsEntry entry = entries.get(i);
         checkParent(entry, i, entries);
      }

      // This method will be called if needsEarlyHierarchy()
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
              || !parentHasRecordsOrCrossRefs )
         {
            boolean condition = false;

            try
            {
               condition = isFlattenConditionTrue(child);
            }
            catch (Exception e)
            {
               bib2gls.warning(e.getMessage());
            }

            if (condition)
            {
               Integer level = Integer.valueOf(child.getLevel(entries));

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

            /*
             Has the parent been identified as a dependency of 
             any entry not marked for removal?
             */

            if (discardList.contains(parent) 
                 && isPendingDiscardDependent(parent, child, entries, discardList))
            {
               discardList.remove(parent);
            }
            else
            {
               flattenChild(parent, child, entries);
            }
         }
      }

      for (Bib2GlsEntry discard : discardList)
      {
         entries.remove(discard);
      }

   }

   private boolean isPendingDiscardDependent(Bib2GlsEntry parent,
     Bib2GlsEntry child, Vector<Bib2GlsEntry> entries,
     Vector<Bib2GlsEntry> discardList)
   {
      String id = parent.getId();

      for (Bib2GlsEntry entry : entries)
      {
         if (entry.equals(parent) || entry.equals(child)
              || discardList.contains(entry))
         {// skip
         }
         else if (entry.hasDependent(id))
         {
            bib2gls.verboseMessage("message.not_removing.dependent_parent",
              child.getId(), entry.getId(), id);

            return true;
         }
      }

      return false;
   }

   private boolean isFlattenConditionTrue(Bib2GlsEntry entry)
     throws IOException,Bib2GlsException
   {
      if (flattenLonelyConditional == null)
      {
         return true;
      }
      else
      {
         return flattenLonelyConditional.booleanValue(entry);
      }
   }

   /**
    * Assigns the glossary type for the given entry, if applicable.
    * If the type has been identified by the relevant setting, the
    * "type" field will be set. If the type can't be determined from
    * the settings, the field won't be set.
    * @param entry the entry
    */ 
   private void setType(Bib2GlsEntry entry)
   {
      if (triggerType != null && entry.hasTriggerRecord())
      {
         entry.putField("type", triggerType);
      }
      else if (ignoredType != null && entry.isIgnoredEntry())
      {
         entry.putField("type", ignoredType);
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

   /**
    * Gets a list of all known glossary types. These are all the
    * types that can be set via various settings. Doesn't include 
    * values of the "type" field that may have been set via other
    * means (such as explicitly within the bib file or by copying
    * another field).
    * @return list of all known glossary types (empty if none known)
    */ 
   public Vector<String> getAllKnownTypes()
   {
      Vector<String> types = new Vector<String>();

      if (type != null)
      {
         types.add(type);
      }

      if (dualType != null && !types.contains(dualType))
      {
         types.add(dualType);
      }

      if (tertiaryType != null && !types.contains(tertiaryType))
      {
         types.add(tertiaryType);
      }

      if (secondaryType != null && !types.contains(secondaryType))
      {
         types.add(secondaryType);
      }

      if (triggerType != null && !types.contains(triggerType))
      {
         types.add(triggerType);
      }

      if (ignoredType != null && !types.contains(ignoredType))
      {
         types.add(ignoredType);
      }

      if (progenitorType != null && !types.contains(progenitorType))
      {
         types.add(progenitorType);
      }

      if (progenyType != null && !types.contains(progenyType))
      {
         types.add(progenyType);
      }

      if (compoundMainType != null && !types.contains(compoundMainType))
      {
         types.add(compoundMainType);
      }

      if (compoundOtherType != null && !types.contains(compoundOtherType))
      {
         types.add(compoundOtherType);
      }

      return types;
   }

   /**
    * Assigns the category for the given entry, if applicable.
    * If the category has been identified by the relevant setting, the
    * "category" field will be set. If the category can't be determined from
    * the settings, the field won't be set.
    * @param entry the entry
    */ 
   private void setCategory(Bib2GlsEntry entry)
   {
      setCategory(entry, category);
   }

   /**
    * Assigns the category for the given entry.
    * The category label is first determined according to the
    * settings. If it can't be determined, the fallback will be used
    * (if not null)
    * @param entry the entry
    * @param catLabel the fallback (may be null)
    */ 
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

   /**
    * Sets the counter field for the given entry.
    * If the "counter" setting has been supplied, this will set the
    * entry's "counter" field. Does nothing if no counter set.
    * @param entry the entry
    */ 
   private void setCounter(Bib2GlsEntry entry)
   {
      if (counter != null)
      {
         entry.putField("counter", counter);
      }
   }

   /**
    * Sets the counter field for the given dual entry.
    * If the "dual-counter" setting has been supplied, this will set the
    * entry's "counter" field. Does nothing if no counter set.
    * @param dual the dual entry
    */ 
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

   /**
    * Sets the type field for the given dual entry, if applicable.
    * @param dual the dual entry
    */ 
   private void setDualType(Bib2GlsEntry dual)
   {
      if (triggerType != null && dual.hasTriggerRecord())
      {
         dual.putField("type", triggerType);
      }
      else if (ignoredType != null && dual.isIgnoredEntry())
      {
         dual.putField("type", ignoredType);
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

   /**
    * Sets the category field for the given dual entry, if applicable.
    * @param dual the dual entry
    */ 
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

   /**
    * Gets the glossary type associated with the given entry.
    * @param entry the entry
    * @return the glossary type or null if unknown
    */ 
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

   /**
    * Gets the glossary type associated with the given entry.
    * @param entry the entry
    * @param fallback the fallback value (may be null)
    * @return the glossary type or null if unknown
    */ 
   public String getType(Bib2GlsEntry entry, String fallback)
   {
      return getType(entry, fallback, true);
   }

   /**
    * Gets the glossary type associated with the given entry.
    * @param entry the entry
    * @param fallback the fallback value (may be null)
    * @param checkField if true check if the "type" field has
    * been set
    * @return the glossary type or null if unknown
    */ 
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

   /**
    * Gets the category for the given entry.
    * @param entry the entry
    * @return the category or null if unknown
    */
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

   /**
    * Gets the category for the given entry.
    * @param entry the entry
    * @param fallback the fallback
    * @return the category or null if unknown
    */
   public String getCategory(Bib2GlsEntry entry, String fallback)
   {
      return getCategory(entry, fallback, true);
   }

   /**
    * Gets the category for the given entry.
    * @param entry the entry
    * @param fallback the fallback
    * @param checkField if true check if the "category" field has
    * been set
    * @return the category or null if unknown
    */
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

   /*
    * METHODS: apply case-changes
    */

   /**
    * Applies the name case change setting to the given value.
    * Uses the "name-case-change" setting to determine how to alter
    * the value.
    * @param value the value to change
    * @return modified value
    */ 
   public BibValueList applyNameCaseChange(BibValueList value)
    throws IOException
   {
      return applyCaseChange(value, nameCaseChange);
   }

   /**
    * Applies the description case change setting to the given value.
    * Uses the "description-case-change" setting to determine how to alter
    * the value.
    * @param value the value to change
    * @return modified value
    */ 
   public BibValueList applyDescriptionCaseChange(BibValueList value)
    throws IOException
   {
      return applyCaseChange(value, descCaseChange);
   }

   /**
    * Applies the short case change setting to the given value.
    * Uses the "short-case-change" setting to determine how to alter
    * the value.
    * @param value the value to change
    * @return modified value
    */ 
   public BibValueList applyShortCaseChange(BibValueList value)
    throws IOException
   {
      return applyCaseChange(value, shortCaseChange);
   }

   /**
    * Applies the dual short case change setting to the given value.
    * Uses the "dual-short-case-change" setting to determine how to alter
    * the value.
    * @param value the value to change
    * @return modified value
    */ 
   public BibValueList applyDualShortCaseChange(BibValueList value)
    throws IOException
   {
      return applyCaseChange(value, dualShortCaseChange);
   }

   /**
    * Applies the long case change setting to the given value.
    * Uses the "long-case-change" setting to determine how to alter
    * the value.
    * @param value the value to change
    * @return modified value
    */ 
   public BibValueList applyLongCaseChange(BibValueList value)
    throws IOException
   {
      return applyCaseChange(value, longCaseChange);
   }

   /**
    * Applies the dual long case change setting to the given value.
    * Uses the "dual-long-case-change" setting to determine how to alter
    * the value.
    * @param value the value to change
    * @return modified value
    */ 
   public BibValueList applyDualLongCaseChange(BibValueList value)
    throws IOException
   {
      return applyCaseChange(value, dualLongCaseChange);
   }

   /**
    * Determines if the given control sequence name represents a
    * case-changing letter. That is, the lowercase/uppercase version
    * can be obtained by simply converting the control sequence name
    * to lowercase/uppercase. For example, {@code \\AE} and {@code \\ae} 
    * represent upper and lowercase versions of the ae-ligature.
    * @param csname the control sequence name
    * @return true if the control sequence name can be changed to
    * the opposite case
    */
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

   /**
    * Converts all the objects in the list to lowercase.
    * Skips maths and arguments of commands such as {@code \\ref}.
    * @param list the list of TeX objects
    * @param listener the TeX parser listener
    */ 
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
               String str = toLowerCase(new String(Character.toChars(codePoint))).toString();

               codePoint = str.codePointAt(0);

               if (str.length() == Character.charCount(codePoint))
               {
                  ((CharObject)object).setCharCode(codePoint);
               }
               else
               {
                  list.set(i, listener.createString(str));
               }
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

            GlsLike gl = bib2gls.getGlsLike(csname);

            if (gl != null)
            {
               GlsLikeFamily fam = gl.getFamily();

               if (fam != null)
               {
                  String caseChangedName = fam.getMember(CaseChange.TO_LOWER, csname);

                  if (!csname.equals(caseChangedName))
                  {
                     list.set(csIdx, new TeXCsRef(caseChangedName));
                  }
               }
            }
            else if (isCaseExclusion(csname))
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
            else if (csname.toLowerCase().equals("glsxtrmultientryadjustedname"))
            {
               list.set(csIdx, new TeXCsRef("glsxtrmultientryadjustedname"));

               // four arguments, second needs adjusting
               i++;
               int count = 0;
               while (i < n)
               {
                  object = list.get(i);

                  if (!(object instanceof Ignoreable))
                  {
                     count++;

                     if (count == 2 && object instanceof TeXObjectList)
                     {
                        toLowerCase((TeXObjectList)object, listener);
                     }

                     if (count == 4) break;
                  }

                  i++;
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

   /**
    * Converts all the objects in the list to uppercase.
    * Skips maths and arguments of commands such as {@code \\ref}.
    * @param list the list of TeX objects
    * @param listener the TeX parser listener
    */ 
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
               String str = toUpperCase(new String(Character.toChars(codePoint))).toString();

               codePoint = str.codePointAt(0);

               if (str.length() == Character.charCount(codePoint))
               {
                  ((CharObject)object).setCharCode(codePoint);
               }
               else
               {
                  list.set(i, listener.createString(str));
               }
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

            GlsLike gl = bib2gls.getGlsLike(csname);

            if (gl != null)
            {
               GlsLikeFamily fam = gl.getFamily();

               if (fam != null)
               {
                  String caseChangedName = fam.getMember(CaseChange.TO_UPPER, csname);

                  if (!csname.equals(caseChangedName))
                  {
                     list.set(csIdx, new TeXCsRef(caseChangedName));
                  }
               }
            }
            else if (isCaseExclusion(csname))
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
            else if (csname.toLowerCase().equals("glsxtrmultientryadjustedname"))
            {
               list.set(csIdx, new TeXCsRef("GLSxtrmultientryadjustedname"));

               // four arguments
               i++;
               int count = 0;
               while (i < n)
               {
                  object = list.get(i);

                  if (!(object instanceof Ignoreable))
                  {
                     count++;

                     if (count == 4) break;
                  }

                  i++;
               }
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

   /**
    * Converts the list to sentence case.
    * @param list the list of TeX objects
    * @param listener the TeX parser listener
    */ 
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
               String str = new String(Character.toChars(codePoint));

               if (i < n-1)
               {
                  TeXObject nextObj = list.get(i+1);

                  if (nextObj instanceof CharObject)
                  {
                     String val = getLocalisationTextIfExists("sentencecase",
                        str+nextObj.format());

                     if (val != null)
                     {
                        list.remove(i+1);
                        list.remove(i);

                        list.add(i, listener.createString(val));

                        return;
                     }
                  }
               }

               str = toSentenceCase(str).toString();

               codePoint = str.codePointAt(0);

               if (str.length() == Character.charCount(codePoint))
               {
                  ((CharObject)object).setCharCode(codePoint);
               }
               else
               {
                  list.set(i, listener.createString(str));
               }
               return;
            }
         }
         else if (object instanceof MathGroup)
         {
            return;
         }
         else if (object instanceof Group)
         {
            // upper case group contents if not empty

            if (!object.isEmpty())
            {
               toUpperCase((TeXObjectList)object, listener);
            }

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

            GlsLike gl = bib2gls.getGlsLike(csname);

            if (gl != null)
            {
               GlsLikeFamily fam = gl.getFamily();

               if (fam != null)
               {
                  String caseChangedName = fam.getMember(CaseChange.SENTENCE, csname);

                  if (!csname.equals(caseChangedName))
                  {
                     list.set(csIdx, new TeXCsRef(caseChangedName));
                  }
               }

               return;
            }

            if (csname.endsWith("ref") || isCaseBlocker(csname))
            {
               return;
            }

            if (isCaseExclusion(csname))
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

            if (csname.toLowerCase().equals("glsxtrmultientryadjustedname"))
            {
               list.set(csIdx, new TeXCsRef("Glsxtrmultientryadjustedname"));

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

   /**
    * Converts the list to non sentence case. That is, it converts
    * the first letter to lowercase.
    * @param list the list of TeX objects
    * @param listener the TeX parser listener
    */ 
   public void toNonSentenceCase(TeXObjectList list, TeXParserListener listener)
   {
      for (int i = 0, n = list.size(); i < n; i++)
      {
         TeXObject object = list.get(i);

         if (object instanceof CharObject)
         {
            int codePoint = ((CharObject)object).getCharCode();

            if (Character.isAlphabetic(codePoint))
            {
               String str = new String(Character.toChars(codePoint));

               if (i < n-1)
               {
                  TeXObject nextObj = list.get(i+1);

                  if (nextObj instanceof CharObject)
                  {
                     String val = getLocalisationTextIfExists("nonsentencecase",
                         str + nextObj.format());

                     if (val != null)
                     {
                        list.remove(i+1);
                        list.remove(i);

                        list.add(i, listener.createString(val));
                        return;
                     }
                  }
               }

               str = toNonSentenceCase(str).toString();

               codePoint = str.codePointAt(0);

               if (str.length() == Character.charCount(codePoint))
               {
                  ((CharObject)object).setCharCode(codePoint);
               }
               else
               {
                  list.set(i, listener.createString(str));
               }

               return;
            }
         }
         else if (object instanceof MathGroup)
         {
            return;
         }
         else if (object instanceof Group)
         {
            // lower case group contents if not empty

            if (!object.isEmpty())
            {
               toLowerCase((TeXObjectList)object, listener);
            }

            return;
         }
         else if (object instanceof ControlSequence)
         {
            String csname = ((ControlSequence)object).getName();

            if (csname.equals("protect")) continue;

            if (isUcLcCommand(csname))
            {
               list.set(i, new TeXCsRef(csname.toLowerCase()));
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

            GlsLike gl = bib2gls.getGlsLike(csname);

            if (gl != null)
            {
               GlsLikeFamily fam = gl.getFamily();

               if (fam != null)
               {
                  String caseChangedName = fam.getMember(CaseChange.TO_LOWER, csname);

                  if (!csname.equals(caseChangedName))
                  {
                     list.set(csIdx, new TeXCsRef(caseChangedName));
                  }
               }

               return;
            }

            if (csname.endsWith("ref") || isCaseBlocker(csname))
            {
               return;
            }

            if (isCaseExclusion(csname))
            {
               // object should now be the argument, which should be
               // skipped.

               continue;
            }

            if (csname.equals("glsentrytitlecase"))
            {
               list.set(csIdx, new TeXCsRef("glsxtrusefield"));

               return;
            }

            if (csname.toLowerCase().equals("glsxtrmultientryadjustedname"))
            {
               list.set(csIdx, new TeXCsRef("glsxtrmultientryadjustedname"));

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

                  toNonSentenceCase(subList, listener);
               }
               else
               {// no optional argument, need to add one
                  subList.add(listener.getOther('['));
                  subList.add(new TeXCsRef("glsentrytext"));
                  subList.add(new TeXCsRef("glslabel"));
                  subList.add(listener.getOther(']'));
               }

               list.addAll(i, subList);

               return;
            }

            String nonSentenceCsname = null;

            if (csname.matches("d?Gls(disp|link)"))
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
               nonSentenceCsname = getLowerCsName(csname);
            }

            if (nonSentenceCsname == null)
            {
               // if a group follows the command, lower case the group
               // otherwise finish.

               if (object instanceof Group && !(object instanceof MathGroup))
               {
                  // lower case argument

                  toNonSentenceCase((TeXObjectList)object, listener);
               }
            }
            else
            {
               list.set(csIdx, new TeXCsRef(nonSentenceCsname));
            }

            return;
         }
      }
   }

   /**
    * Determines whether or not the given control sequence name
    * corresponds to a command that should not have any case-changing applied 
    * to its argument.
    * @param csname the control sequence name
    * @return true if the command's argument should not have its
    * case-changed
    */ 
   @Deprecated
   public boolean isNoCaseChangeCs(String csname)
   {
      if (csname.equals("si") || csname.equals("ensuremath")
       || csname.equals("NoCaseChange") || csname.equals("BibGlsNoCaseChange"))
      {
         return true;
      }

      if (noCaseChangeCs != null)
      {
         for (String cs : noCaseChangeCs)
         {
            if (cs.equals(csname))
            {
               return true;
            }
         }
      }

      return false;
   }

   public boolean isCaseExclusion(String csname)
   {
      if (bib2gls.isCaseExclusion(csname) || isNoCaseChangeCs(csname))
      {
         return true;
      }

      return false;
   }

   public boolean isCaseBlocker(String csname)
   {
      if (bib2gls.isCaseBlocker(csname))
      {
         return true;
      }

      if (noCaseChangeCs != null)
      {
         for (String cs : noCaseChangeCs)
         {
            if (cs.equals(csname))
            {
               return true;
            }
         }
      }

      return false;
   }

   public String getCaseMapping(String csname)
   {
      return bib2gls.getCaseMapping(csname);
   }

   /**
    * Determines whether or not the given object marks a word
    * boundary.
    * @param object the TeX object
    * @return true if the object marks a word boundary
    */ 
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

         if (csname.equals("MFUwordbreak"))
         {
            return true;
         }
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

   /**
    * Determines whether or not the word starting at the given index is a
    * title-case exception.
    * @param list the list being parsed for title case change
    * @param index the current index within the list
    * @param wordExceptions the list of exceptions
    * @return true if there is a word at the current position that
    * matches one of the given list of exceptions
    */ 
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

   private void registerGlsLikeTitleCase(String titlecasename, boolean isPlural,
      GlsLikeFamily fam)
   {
      bib2gls.addGlsLike(fam.getPrefix(), titlecasename);

      if (titleCaseCommands == null)
      {
         titleCaseCommands = new HashMap<String,String>();
      }

      if (!titleCaseCommands.containsKey(titlecasename))
      {
         String titlecs = "BibGlsTitleCase";

         if (isPlural)
         {
            titlecs = "BibGlsTitleCasePlural";
         }

         L2HStringConverter listener = bib2gls.getInterpreterListener();

         String options = fam.getOptions();
         TeXObject optsObj = null;

         if (options == null || options.isEmpty())
         {
            options = "#1";

            if (listener != null)
            {
               optsObj = listener.getParam(1);
            }
         }
         else
         {
            options += ",#1";

            if (listener != null)
            {
               optsObj = listener.createString(options+",");
               ((TeXObjectList)optsObj).add(listener.getParam(1));
            }
         }

         String famPrefix = fam.getPrefix();

         if (famPrefix == null)
         {
            famPrefix = "";
         }

         String def = String.format("\\glsdisp[%s]{%s#2}{\\%s{%s#2}}", 
           options, famPrefix, titlecs, famPrefix);

         titleCaseCommands.put(titlecasename, def);

         if (listener != null)
         {
            TeXObjectList defList = listener.createStack();

            defList.add(new TeXCsRef("glsdisp"));
            defList.add(listener.getOther('['));
            defList.add(optsObj, true);
            defList.add(listener.getOther(']'));

            Group grp = listener.createGroup(famPrefix);
            defList.add(grp);

            grp.add(listener.getParam(2));

            grp = listener.createGroup();
            defList.add(grp);

            grp.add(new TeXCsRef(titlecs));

            Group subgrp = listener.createGroup(famPrefix);
            grp.add(subgrp);

            subgrp.add(listener.getParam(2));

            listener.putControlSequence(new LaTeXGenericCommand(true,
              titlecasename, new char[] {'o', 'm'}, 
              defList, new TeXObject[] { listener.createStack()}));
         }
      }
   }

   /**
    * Converts the list to title case.
    * @param list the list of TeX objects
    * @param listener the TeX parser listener
    */ 
   public void toTitleCase(TeXObjectList list, TeXParserListener listener)
   {
      toTitleCase(list, listener, 0, bib2gls.getWordExceptionList());
   }

   /**
    * Converts the list to title case.
    * @param list the list of TeX objects
    * @param listener the TeX parser listener
    * @param wordCount the current word count
    * @param wordExceptions the list of word exceptions
    * @return the updated current word count
    */ 
   private int toTitleCase(TeXObjectList list, TeXParserListener listener,
     int wordCount, TeXObjectList wordExceptions)
   {
      boolean wordBoundary = true;

      for (int i = 0, n = list.size(); i < n; i++)
      {
         TeXObject object = list.get(i);

         boolean prevWordBoundary = wordBoundary;

         wordBoundary = isWordBoundary(object);

         if ((object instanceof ControlSequence))
         {
            String name = ((ControlSequence)object).getName();

            if (name.equals("MFUwordbreak")
                 || name.equals("MFUskippunc")
                 || name.equals("NoCaseChange")
               )
            { 
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

               continue;
            }
         }

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

            GlsLike gl = bib2gls.getGlsLike(csname);

            if (gl != null)
            {
               GlsLikeFamily fam = gl.getFamily();

               if (fam != null)
               {
                  boolean isPlural = fam.isPlural(csname);
                  String caseChangedName = "bibglsliketitlecase"
                   + (isPlural ? fam.getPlural() : fam.getSingular());

                  registerGlsLikeTitleCase(caseChangedName, isPlural, fam);
                  list.set(csIdx, new TeXCsRef(caseChangedName));
               }

               continue;
            }
            else if (isCaseBlocker(csname) || isCaseExclusion(csname))
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
            else if (csname.toLowerCase().equals("glsxtrmultientryadjustedname"))
            {
               list.set(csIdx, new TeXCsRef("GlsXtrmultientryadjustedname"));

               i++;
               int count = 0;
               while (i < n)
               {
                  object = list.get(i);

                  if (!(object instanceof Ignoreable))
                  {
                     count++;

                     if (count==4) break;
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

   /**
    * Gets the corresponding sentence case control sequence name
    * matching the given name.
    * @param csname the control sequence name
    * @return the sentence case control sequence name or null if
    * unknown
    */ 
   public String getSentenceCsName(String csname)
   {
      String mappedName = getCaseMapping(csname);

      if (mappedName != null)
      {
         return mappedName;
      }

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

      if (csname.matches("pgls(pl)?"))
      {
         return String.format("P%s", csname.substring(1));
      }

      if (csname.matches("[rdc]gls(pl)?"))
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

   /**
    * Gets the corresponding title case control sequence name
    * matching the given name.
    * This is harder than getSentenceCsName and this method will have to
    * fallback on that method in most cases.
    * @param csname the control sequence name
    * @return the title case control sequence name or sentence case
    * control sequence, if available, or null if unknown
   */
   public String getTitleCsName(String csname)
   {
      if (csname.toLowerCase().equals("glsxtrusefield"))
      {
         return "glsentrytitlecase";
      }

      return getSentenceCsName(csname);
   }

   /**
    * Gets the corresponding lowercase control sequence name
    * matching the given name.
    * @param csname the control sequence name
    * @return the lower case control sequence name or null if unknown
    */
   public String getLowerCsName(String csname)
   {
      if (csname.matches("(Gls|GLS)(pl|xtrshort|xtrlong|xtrfull|xtrp)?")
        || csname.matches("[rdc](Gls|GLS)(pl)?")
        || csname.matches("P(gls|GLS)(pl)?")
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

   /**
    * Gets the corresponding all caps control sequence name
    * matching the given name.
    * @param csname the control sequence name
    * @return the all caps control sequence name or null if unknown
    */
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

   /**
    * Indicates that the given field needs to have an all caps entry
    * command provided in the glstex file.
    * @param field the field
    */ 
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

   /**
    * Indicates that the given field needs to have an all caps entry
    * accessibility command provided in the glstex file.
    * @param field the field
    */ 
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

   /**
    * Applies the appropriate case-change to the given value.
    * @param value the value to apply the case-change
    * @param change the change to apply (one of: "lc-cs", "uc-cs",
    * "firstuc-cs", "title-cs", "lc", "uc", "firstuc" or "title")
    * @throws IOException if parser error occurs
    */ 
   public BibValueList applyCaseChange(BibValueList value, String change)
    throws IOException
   {
      TeXParser bibParser = getBibParser();

      if (change == null) return value;

      TeXObjectList list = BibValueList.stripDelim(value.expand(bibParser));

      BibValueList bibList = new BibValueList();

      if (change.equals("lc-cs"))
      {
         Group grp = bibParser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("bibglslowercase"));
         list.add(grp);
      }
      else if (change.equals("uc-cs"))
      {
         Group grp = bibParser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("bibglsuppercase"));
         list.add(grp);
      }
      else if (change.equals("firstuc-cs"))
      {
         Group grp = bibParser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("bibglsfirstuc"));
         list.add(grp);
      }
      else if (change.equals("title-cs"))
      {
         Group grp = bibParser.getListener().createGroup();
         grp.addAll(list);

         list = new TeXObjectList();
         list.add(new TeXCsRef("bibglstitlecase"));
         list.add(grp);
      }
      else if (change.equals("lc"))
      {
         toLowerCase(list, bibParser.getListener());
      }
      else if (change.equals("uc"))
      {
         toUpperCase(list, bibParser.getListener());
      }
      else if (change.equals("firstuc"))
      {
         toSentenceCase(list, bibParser.getListener());
      }
      else if (change.equals("title"))
      {
         toTitleCase(list, bibParser.getListener());
      }
      else
      {
         throw new IllegalArgumentException("Invalid case change option: "
          +change);
      }

      bibList.add(new BibUserString(list));

      return bibList;
   }

   // Case conversion for strings.

   /**
    * Converts a string to upper case using the resource locale.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toUpperCase(CharSequence text)
   {
      return toUpperCase(text.toString());
   }

   /**
    * Converts a string to upper case using the resource locale.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toUpperCase(String text)
   {
      return text.toUpperCase(getResourceLocale());
   }

   /**
    * Converts a string to lower case using the resource locale.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toLowerCase(CharSequence text)
   {
      return toLowerCase(text.toString());
   }

   /**
    * Converts a string to lower case using the resource locale.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toLowerCase(String text)
   {
      return text.toLowerCase(getResourceLocale());
   }

   /**
    * Tests if the given text is a title case exception.
    * @param text the text to test
    * @return true if the text is a word exception
    */ 
   public boolean isWordException(CharSequence text)
   {
      int n1 = text.length();

      for (String word : stringWordExceptions)
      {
         int n2 = word.length();

         if (n1 == n2)
         {
            boolean match = true;

            for (int i = 0; i < n1; i++)
            {
               if (text.charAt(i) != word.charAt(i))
               {
                  match = false;
                  break;
               }
            }

            if (match)
            {
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Converts a string to title case.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toTitleCase(CharSequence text)
   {
      return toTitleCase(text.toString());
   }

   /**
    * Converts a string to title case.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toTitleCase(String text)
   {
      StringBuilder builder = new StringBuilder();
      StringBuilder wordBuilder = new StringBuilder();

      boolean beginning = true;

      BreakIterator boundary = BreakIterator.getLineInstance(getResourceLocale());

      boundary.setText(text);

      int start = boundary.first();

      for (int end = boundary.next();
         end != BreakIterator.DONE;
         start = end, end = boundary.next())
      {
         int len = end-start;

         // This will include leading and trailing punctuation and
         // trailing spaces.
         String word = text.substring(start,end);

         int i = 0;

         while (i < word.length())
         {
            int cp = word.codePointAt(i);
            i += Character.charCount(cp);

            if (Character.isAlphabetic(cp))
            {
               break;
            }

            builder.appendCodePoint(cp);
         }

         while (i < word.length())
         {
            wordBuilder.setLength(0);

            while (i < word.length())
            {
               int cp = word.codePointAt(i);
               i += Character.charCount(cp);

               if (Character.isAlphabetic(cp))
               {
                  wordBuilder.appendCodePoint(cp);
               }
               else
               {
                  break;
               }
            }

            CharSequence seq = wordBuilder;

            if (beginning || (!beginning && !isWordException(seq)))
            {
               seq = toSentenceCase(seq);
            }

            builder.append(seq);
            beginning = false;

            while (i < word.length())
            {
               int cp = word.codePointAt(i);
               i += Character.charCount(cp);

               if (Character.isAlphabetic(cp))
               {
                  break;
               }

               builder.appendCodePoint(cp);
            }
         }
      }

      return builder;
   }

   /**
    * Converts a string to sentence case.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toSentenceCase(CharSequence text)
   {
      return toSentenceCase(text.toString());
   }

   /**
    * Converts a string to sentence case.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toSentenceCase(String text)
   {
      return toSentenceCase(text, getResourceLocale());
   }

   /**
    * Converts a string to sentence case.
    * @param text the text to convert
    * @param locale the locale
    * @return the converted text
    */ 
   public CharSequence toSentenceCase(String text, Locale locale)
   {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < text.length(); )
      {
         int cp = text.codePointAt(i);
         i += Character.charCount(cp);

         if (Character.isAlphabetic(cp))
         {
            int uc = Character.toUpperCase(cp);
            int tc = Character.toTitleCase(cp);

            if (uc == tc)
            {
               String str = new String(Character.toChars(cp));

               // Check for known digraphs.

               if (i < text.length())
               {
                  int nextCp = text.codePointAt(i);

                  String val = getLocalisationTextIfExists("sentencecase",
                     str + new String(Character.toChars(nextCp)));

                  if (val != null)
                  {
                     str = val;
                     i += Character.charCount(nextCp);
                  }
                  else
                  {
                     str = str.toUpperCase(locale);
                  }
               }
               else
               {
                  str = str.toUpperCase(locale);
               }

               builder.append(str);
            }
            else
            {
               builder.appendCodePoint(tc);
            }

            builder.append(text.substring(i));
            break;
         }
         else
         {
            builder.appendCodePoint(cp);
         }
      }

      return builder;
   }

   /**
    * Converts a string to non-sentence case.
    * That is, the first alphabetic character is converted to lower
    * case.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toNonSentenceCase(CharSequence text)
   {
      return toNonSentenceCase(text.toString());
   }

   /**
    * Converts a string to non-sentence case.
    * That is, the first alphabetic character is converted to lower
    * case.
    * @param text the text to convert
    * @return the converted text
    */ 
   public CharSequence toNonSentenceCase(String text)
   {
      Locale locale = getResourceLocale();
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < text.length(); )
      {
         int cp = text.codePointAt(i);
         i += Character.charCount(cp);

         if (Character.isAlphabetic(cp))
         {
            String str = new String(Character.toChars(cp));

            // Check for known digraphs.

            if (i < text.length())
            {
               int nextCp = text.codePointAt(i);

               String val = getLocalisationTextIfExists("nonsentencecase",
                  str + new String(Character.toChars(nextCp)));

               if (val != null)
               {
                  str = val;
                  i += Character.charCount(nextCp);
               }
               else
               {
                  str = str.toLowerCase(locale);
               }
            }
            else
            {
               str = str.toLowerCase(locale);
            }

            builder.append(str);
            builder.append(text.substring(i));

            break;
         }
         else
         {
            builder.appendCodePoint(cp);
         }
      }

      return builder;
   }

   /*
    * METHODS: query settings
    */

   /**
    * Gets the "flatten" setting.
    * @return true if flatten setting on otherwise returns false
    */ 
   public boolean flattenSort()
   {
      return flatten;
   }

   /**
    * Determines whether or not this resource set has skipped
    * fields.
    * @return true if this resource set has skipped fields otherwise
    * false
    */ 
   public boolean hasSkippedFields()
   {
      return skipFields != null && skipFields.length != 0;
   }

   /**
    * Gets the array of skipped fields.
    * @return the array of skipped fields
    */ 
   public String[] getSkipFields()
   {
      return skipFields;
   }

   /**
    * Determines whether or not the given field should be skipped.
    * @param field the field
    * @return true if the field should be skipped otherwise
    * false
    */ 
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

   public Vector<String> getOmitFieldList(Bib2GlsEntry entry)
   {
      if (omitFields == null) return null;

      bib2gls.debugMessage("message.calculating.omitlist", entry);

      Vector<String> fields = new Vector<String>();

      try
      {
         for (FieldEvaluation eval : omitFields)
         {
            String field = eval.getStringValue(entry);

            if (field != null && !field.isEmpty() && !fields.contains(field))
            {
               if (bib2gls.isKnownField(field))
               {
                  bib2gls.debugMessage("message.adding.omitlist", field);
                  fields.add(field);
               }
               else
               {
                  bib2gls.warningMessage("warning.unknown_field_in_omitlist",
                    field, entry);
               }
            } 
         }
      }
      catch (Exception e)
      {
         bib2gls.warning(e.getMessage());
         bib2gls.debug(e);
      }

      return fields;
   }

   /**
    * Gets the contributor order setting.
    * @return numeric identifier corresponding to "contributor-order" setting
    */  
   public byte getContributorOrder()
   {
      return contributorOrder;
   }

   /**
    * Determines whether or not the given field is identified as a
    * bibtex author field.
    * @param field the field
    * @return true if the field is an author field
    */  
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

   /**
    * Gets the interpret fields action setting.
    * @return numeric identifier corresponding to
    * "interpret-fields-action" setting
    */  
   public byte getInterpretFieldAction()
   {
      return interpretFieldAction;
   }

   /**
    * Determines whether or not the given field should be
    * interpreted.
    * @param field the field
    * @return true if the field should be interpreted
    */  
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

   /**
    * Gets the array of fields that should have Unicode characters
    * converted. Corresponds to the "hex-unicode-fields" setting.
    * @return the array of fields
    */  
   public String[] getHexUnicodeFields()
   {
      return hexUnicodeFields;
   }

   /**
    * Gets the record label prefix.
    * Corresponds to the "record-label-prefix" setting.
    * @return the record label prefix or null if not set
    */  
   public String getRecordLabelPrefix()
   {
      return recordLabelPrefix;
   }

   /**
    * Gets the primary label prefix.
    * Corresponds to the "label-prefix" setting.
    * @return the primary label prefix or null if not set
    */  
   public String getLabelPrefix()
   {
      return labelPrefix;
   }

   /**
    * Gets the dual label prefix.
    * Corresponds to the "dual-prefix" setting.
    * @return the dual label prefix or null if not set
    */  
   public String getDualPrefix()
   {
      return dualPrefix;
   }

   /**
    * Gets the tertiary type.
    * Corresponds to the "tertiary-type" setting.
    * @return the tertiary type or null if not set
    */  
   public String getTertiaryType()
   {
      return tertiaryType;
   }

   /**
    * Gets the tertiary label prefix.
    * Corresponds to the "tertiary-prefix" setting.
    * @return the tertiary label prefix or null if not set
    */  
   public String getTertiaryPrefix()
   {
      return tertiaryPrefix;
   }

   /**
    * Gets the tertiary category.
    * Corresponds to the "tertiary-category" setting.
    * @return the tertiary category or null if not set
    */  
   public String getTertiaryCategory()
   {
      return tertiaryCategory;
   }

   /**
    * Gets the external prefix corresponding to the given index.
    * Corresponds to given index within the "ext-prefixes" setting.
    * @param idx the index
    * @return the external prefix or null if not set
    */  
   public String getExternalPrefix(int idx)
   {
      if (externalPrefixes == null) return null;

      if (idx >= 1 && idx <= externalPrefixes.length)
      {
         return externalPrefixes[idx-1];
      }

      return null;
   }

   /**
    * Gets the command label prefix.
    * Corresponds to the "cs-label-prefix" setting.
    * @return the command label prefix or null if not set
    */  
   public String getCsLabelPrefix()
   {
      return csLabelPrefix;
   }

   /**
    * Gets the dual sort field.
    * @return the sort field for the dual list
    */  
   public String getDualSortField()
   {
      return dualSortSettings.getSortField();
   }

   /**
    * Gets the default plural suffix.
    * @return the plural suffix
    */  
   public String getPluralSuffix()
   {
      return pluralSuffix;
   }

   /**
    * Gets the default abbreviation plural suffix.
    * @return the abbreviation plural suffix
    */  
   public String getShortPluralSuffix()
   {
      return shortPluralSuffix;
   }

   /**
    * Gets the default dual plural suffix.
    * @return the plural dual suffix
    */  
   public String getDualPluralSuffix()
   {
      return dualPluralSuffix;
   }

   /**
    * Gets the default dual abbreviation plural suffix.
    * @return the plural dual suffix for abbreviations
    */  
   public String getDualShortPluralSuffix()
   {
      return dualShortPluralSuffix;
   }

   /**
    * Gets the dual entry map.
    * Corresponds to the "dual-entry-map" setting.
    * @return the dual entry map or null if not set
    */ 
   public HashMap<String,String> getDualEntryMap()
   {
      return dualEntryMap;
   }

   /**
    * Gets the first key for the dual entry map.
    * Corresponds to the first field in the "dual-entry-map".
    * @return the key or null if not set
    */ 
   public String getFirstDualEntryMap()
   {
      return dualEntryFirstMap;
   }

   /**
    * Gets the back-link for the dual entry map.
    * @return the back-link entry or null if not set
    */ 
   public boolean backLinkFirstDualEntryMap()
   {
      return backLinkDualEntry;
   }

   /**
    * Gets the dual symbol map.
    * Corresponds to the "dual-symbol-map" setting.
    * @return the dual symbol map or null if not set
    */ 
   public HashMap<String,String> getDualSymbolMap()
   {
      return dualSymbolMap;
   }

   /**
    * Gets the first key for the dual symbol map.
    * Corresponds to the first field in the "dual-symbol-map".
    * @return the key or null if not set
    */ 
   public String getFirstDualSymbolMap()
   {
      return dualSymbolFirstMap;
   }

   /**
    * Gets the back-link for the dual symbol map.
    * @return the back-link entry or null if not set
    */ 
   public boolean backLinkFirstDualSymbolMap()
   {
      return backLinkDualSymbol;
   }

   /**
    * Gets the dual abbreviation map.
    * Corresponds to the "dual-abbrv-map" setting.
    * @return the dual abbreviation map or null if not set
    */ 
   public HashMap<String,String> getDualAbbrevMap()
   {
      return dualAbbrevMap;
   }

   /**
    * Gets the dual abbreviation-entry map.
    * Corresponds to the "dual-entryabbrv-map" setting.
    * @return the dual entry-abbreviation map or null if not set
    */ 
   public HashMap<String,String> getDualAbbrevEntryMap()
   {
      return dualAbbrevEntryMap;
   }

   /**
    * Gets the dual index-entry map.
    * Corresponds to the "dual-indexentry-map" setting.
    * @return the dual index-entry map or null if not set
    */ 
   public HashMap<String,String> getDualIndexEntryMap()
   {
      return dualIndexEntryMap;
   }

   /**
    * Gets the dual index-symbol map.
    * Corresponds to the "dual-indexsymbol-map" setting.
    * @return the dual index-symbol map or null if not set
    */ 
   public HashMap<String,String> getDualIndexSymbolMap()
   {
      return dualIndexSymbolMap;
   }

   /**
    * Gets the dual index-abbreviation map.
    * Corresponds to the "dual-indexabbrv-map" setting.
    * @return the dual index-abbreviation map or null if not set
    */ 
   public HashMap<String,String> getDualIndexAbbrevMap()
   {
      return dualIndexAbbrevMap;
   }

   /**
    * Gets the first key for the dual abbreviation map.
    * Corresponds to the first field in the "dual-abbrv-map".
    * @return the key or null if not set
    */ 
   public String getFirstDualAbbrevMap()
   {
      return dualAbbrevFirstMap;
   }

   /**
    * Gets the first key for the dual abbreviation-entry map.
    * Corresponds to the first field in the "dual-abbrventry-map".
    * @return the key or null if not set
    */ 
   public String getFirstDualAbbrevEntryMap()
   {
      return dualAbbrevEntryFirstMap;
   }

   /**
    * Gets the first key for the dual index-entry map.
    * Corresponds to the first field in the "dual-indexentry-map".
    * @return the key or null if not set
    */ 
   public String getFirstDualIndexEntryMap()
   {
      return dualIndexEntryFirstMap;
   }

   /**
    * Gets the first key for the dual index-symbol map.
    * Corresponds to the first field in the "dual-indexsymbol-map".
    * @return the key or null if not set
    */ 
   public String getFirstDualIndexSymbolMap()
   {
      return dualIndexSymbolFirstMap;
   }

   /**
    * Gets the first key for the dual index-abbrviation map.
    * Corresponds to the first field in the "dual-indexabbrv-map".
    * @return the key or null if not set
    */ 
   public String getFirstDualIndexAbbrevMap()
   {
      return dualIndexAbbrevFirstMap;
   }

   /**
    * Gets the back-link for the dual abbreviation map.
    * @return the back-link entry or null if not set
    */ 
   public boolean backLinkFirstDualAbbrevMap()
   {
      return backLinkDualAbbrev;
   }

   /**
    * Gets the back-link for the dual abbreviation-entry map.
    * @return the back-link entry or null if not set
    */ 
   public boolean backLinkFirstDualAbbrevEntryMap()
   {
      return backLinkDualAbbrevEntry;
   }

   /**
    * Gets the back-link for the dual index-entry map.
    * @return the back-link entry or null if not set
    */ 
   public boolean backLinkFirstDualIndexEntryMap()
   {
      return backLinkDualIndexEntry;
   }

   /**
    * Gets the back-link for the dual index-symbol map.
    * @return the back-link entry or null if not set
    */ 
   public boolean backLinkFirstDualIndexSymbolMap()
   {
      return backLinkDualIndexSymbol;
   }

   /**
    * Gets the back-link for the dual index-abbreviation map.
    * @return the back-link entry or null if not set
    */ 
   public boolean backLinkFirstDualIndexAbbrevMap()
   {
      return backLinkDualIndexAbbrev;
   }

   /**
    * Gets the dual field.
    * Corresponds to the "dual-field" setting.
    * @return the dual field or null if not set
    */ 
   public String getDualField()
   {
      return dualField;
   }

   /**
    * Checks the field maps are valid.
    * @throws Bib2GlsException if any listed fields are invalid
    */ 
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

   /**
    * Determines whether or not the entry should be discarded.
    * @param entry the entry
    * @return true if the entry should be discarded otherwise false
    */ 
   public boolean discard(Bib2GlsEntry entry)
   {
      if (fieldPatterns == null || matchAction != MATCH_ACTION_FILTER)
      {
         return false;
      }

      boolean discard = notMatch(entry);

      return notMatch ? !discard : discard;
   }

   /**
    * Determines whether or not the entry does not match the field
    * pattern filter. Not match means the entry should be discarded
    * unless the match has been negated.
    * @param entry the entry
    * @return true if the entry does not match otherwise false
    */ 
   private boolean notMatch(Bib2GlsEntry entry)
   {
      return notMatch(entry, fieldPatternsAnd, fieldPatterns);
   }

   /**
    * Determines whether or not the entry does not match the
    * secondary field pattern filter. Not match means the entry should be discarded
    * unless the match has been negated.
    * @param entry the entry
    * @return true if the entry does not match otherwise false
    */ 
   private boolean secondaryNotMatch(Bib2GlsEntry entry)
   {
      return notMatch(entry, secondaryFieldPatternsAnd, secondaryFieldPatterns);
   }

   /**
    * Determines whether or not the entry does not match the
    * pattern filter.
    * @param entry the entry
    * @param and if true perform logical AND otherwise use OR
    * @param patterns pattern map
    * @return true if the entry does not match otherwise false
    */ 
   private boolean notMatch(Bib2GlsEntry entry, boolean and, 
       HashMap<String,Pattern> patterns)
   {
      return notMatch(bib2gls, entry, and, patterns);
   }

   /**
    * Determines whether or not the entry does not match the
    * pattern filter.
    * @param bib2gls the application
    * @param entry the entry
    * @param and if true perform logical AND otherwise use OR
    * @param patterns pattern map
    * @return true if the entry does not match otherwise false
    */ 
   public static boolean notMatch(Bib2Gls bib2gls, Bib2GlsEntry entry, 
       boolean and, HashMap<String,Pattern> patterns)
   {
      boolean matches = and;

      for (Iterator<String> it = patterns.keySet().iterator();
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

         Pattern p = patterns.get(field);

         Matcher m = p.matcher(value);

         boolean result = m.matches();

         bib2gls.debugMessage("message.pattern.info", p.pattern(), field, value, result);

         if (and)
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

   public Matcher getLastMatch()
   {
      return lastMatcher;
   }

   public void setLastMatch(Matcher matcher)
   {
      lastMatcher = matcher;
   }

   public String getLastMatchGroup(int i) throws Bib2GlsException
   {
      if (lastMatcher == null)
      {
         throw new Bib2GlsException(bib2gls.getMessage("error.no_matcher", i));
      }

      if (i > lastMatcher.groupCount())
      {
         throw new Bib2GlsException(bib2gls.getMessage("error.matcher_idx_too_big",
           i, lastMatcher.groupCount(), lastMatcher.pattern().pattern()));
      }
      else if (i < 0)
      {
         throw new Bib2GlsException(bib2gls.getMessage("error.matcher_invalid_idx",
           i, lastMatcher.pattern().pattern()));
      }
      else
      {
         try
         {
            return lastMatcher.group(i);
         }
         catch (IllegalStateException e)
         {
            throw new Bib2GlsException(bib2gls.getMessage(
               "error.matcher_failed", i, lastMatcher.pattern().pattern()), e);
         }
      }
   }

   public String getLastMatchGroup(String name) throws Bib2GlsException
   {
      if (lastMatcher == null)
      {
         throw new Bib2GlsException(bib2gls.getMessage("error.no_matcher", name));
      }

      try
      {
         return lastMatcher.group(name);
      }
      catch (IllegalStateException e)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
            "error.matcher_failed", name, lastMatcher.pattern().pattern()), e);
      }
   }

   /**
    * Gets the post-description dot setting.
    * Corresponds to the "post-description-dot" setting.
    * @return the numeric identifier indicating the setting
    */ 
   public byte getPostDescDot()
   {
      return postDescDot;
   }

   /**
    * Gets the "strip-trailing-nopost" setting.
    * @return true if the setting is on
    */ 
   public boolean isStripTrailingNoPostOn()
   {
      return stripTrailingNoPost;
   }

   /**
    * Determines whether or not the given field should be converted
    * into a label.
    * @param field the field
    * @return true if the field should be converted to a label
    */ 
   public boolean isLabelifyField(String field)
   {
      if (labelifyFields == null) return false;

      for (String f : labelifyFields)
      {
         if (f.equals(field)) return true;
      }

      return false;
   }

   /**
    * Determines whether or not the given field should be converted
    * into a label list.
    * @param field the field
    * @return true if the field should be converted to a label list
    */ 
   public boolean isLabelifyListField(String field)
   {
      if (labelifyListFields == null) return false;

      for (String f : labelifyListFields)
      {
         if (f.equals(field)) return true;
      }

      return false;
   }

   /**
    * Gets the labelify substitutions.
    * Corresponds to the "labelify-replace" setting.
    * @return the list of pattern substitutions or null if not set
    */ 
   public Vector<PatternReplace> getLabelifySubstitutions()
   {
      return labelifyReplaceMap;
   }

   /**
    * Determines whether or not the given field should contain a list of
    * dependencies.
    * Corresponds to the "dependency-fields" setting.
    * @param field the field
    * @return true if the field should contain a list of
    * dependencies
    */ 
   public boolean isDependencyListField(String field)
   {
      if (dependencyListFields == null) return false;

      for (String f : dependencyListFields)
      {
         if (f.equals(field)) return true;
      }

      return false;
   }

   /**
    * Determines whether or not the end punctuation check is on for
    * any of the fields.
    * Corresponds to the "check-end-punctuation" setting.
    * @return true if the check is on for one or more fields
    */ 
   public boolean isCheckEndPuncOn()
   {
      return checkEndPunc != null;
   }

   /**
    * Determines whether or not the end punctuation check is on for
    * the given field.
    * Corresponds to the "check-end-punctuation" setting.
    * @param field
    * @return true if the check is on for the field
    */ 
   public boolean isCheckEndPuncOn(String field)
   {
      if (checkEndPunc == null) return false;

      for (String f : checkEndPunc)
      {
         if (f.equals(field)) return true;
      }

      return false;
   }

   /**
    * Determines whether or not the name field should have a
    * case-change applied.
    * Corresponds to the "name-case-change" setting.
    * @return true if the name field should have a case-change
    * applied
    */ 
   public boolean changeNameCase()
   {
      return nameCaseChange != null;
   }

   /**
    * Determines whether or not the description field should have a
    * case-change applied.
    * Corresponds to the "description-case-change" setting.
    * @return true if the description field should have a case-change
    * applied
    */ 
   public boolean changeDescriptionCase()
   {
      return descCaseChange != null;
   }

   /**
    * Determines whether or not the short field should have a
    * case-change applied.
    * Corresponds to the "short-case-change" setting.
    * @return true if the short field should have a case-change
    * applied
    */ 
   public boolean changeShortCase()
   {
      return shortCaseChange != null;
   }

   /**
    * Determines whether or not the short field for dual entries should have a
    * case-change applied.
    * Corresponds to the "dual-short-case-change" setting.
    * @return true if the short field for dual entries should have a case-change
    * applied
    */ 
   public boolean changeDualShortCase()
   {
      return dualShortCaseChange != null;
   }

   /**
    * Determines whether or not the long field should have a
    * case-change applied.
    * Corresponds to the "long-case-change" setting.
    * @return true if the long field should have a case-change
    * applied
    */ 
   public boolean changeLongCase()
   {
      return longCaseChange != null;
   }

   /**
    * Determines whether or not the long field for dual entries should have a
    * case-change applied.
    * Corresponds to the "dual-long-case-change" setting.
    * @return true if the long field for dual entries should have a case-change
    * applied
    */ 
   public boolean changeDualLongCase()
   {
      return dualLongCaseChange != null;
   }

   /**
    * Gets the map of field case change options.
    * Corresponds to the "field-case-change" setting.
    * @return field case change map or null if not set
    */ 
   public HashMap<String,String> getFieldCaseOptions()
   {
      return fieldCaseChange;
   }

   /**
    * Gets the alias location setting.
    * Corresponds to the "alias-loc" setting.
    * @return numeric ID representing the setting
    */ 
   public int aliasLocations()
   {
      return aliasLocations;
   }

   /**
    * Determines whether or not there are any aliases.
    * @return true if there are aliases
    */ 
   public boolean hasAliases()
   {
      return aliases;
   }

   /**
    * Sets whether or not there are any aliases.
    * @param hasAliases true if there are aliases
    */ 
   public void setAliases(boolean hasAliases)
   {
      aliases = hasAliases;
   }

   /**
    * Gets the array of location counters.
    * @return array of location counter names
    */ 
   public String[] getLocationCounters()
   {
      return counters;
   }

   /**
    * Gets the field used to store the group label.
    * @return the field name or null if not set
    */ 
   public String getGroupField()
   {
      return groupField;
   }

   /**
    * Gets the alias for the given entry bib type.
    * Uses the mapping identified by the "entry-type-aliases"
    * setting.
    * @param entryType the bib entry type
    * @return the alias or the original entry type if not set
    */ 
   public String mapEntryType(String entryType)
   {
      if (entryTypeAliases == null)
      {
         return entryType;
      }

      String val = entryTypeAliases.get(entryType);

      return val == null ? entryType : val;
   }

   /**
    * Gets the alias for an unknown bib type.
    * Uses the mapping identified by the "unknown-entry-alias"
    * setting.
    * @return the alias or null if not set
    */ 
   public String getUnknownEntryMap()
   {
      return unknownEntryMap;
   }

   /**
    * Gets the field name in which to store the original entry
    * label. Specified by the "save-original-id" setting.
    * @return the field name or null if not set
    */ 
   public String getSaveOriginalIdField()
   {
      return saveOriginalId;
   }

   /**
    * Gets the "save-original-id-action" setting.
    * @return the numeric ID corresponding to the setting
    */ 
   public int getSaveOriginalIdAction()
   {
      return saveOriginalIdAction;
   }

   /**
    * Gets the field name in which to store the original entry
    * bib type. Specified by the "save-original-entrytype" setting.
    * @return the field name or null if not set
    */ 
   public String getSaveOriginalEntryTypeField()
   {
      return saveOriginalEntryType;
   }

   /**
    * Gets the "save-original-entrytype-action" setting.
    * @return the numeric ID corresponding to the setting
    */ 
   public int getSaveOriginalEntryTypeAction()
   {
      return saveOriginalEntryTypeAction;
   }

   /**
    * Determines whether or not there are any field aliases set.
    * @return true if there are field aliases
    */ 
   public boolean hasFieldAliases()
   {
      return fieldAliases != null;
   }

   /**
    * Gets the field aliases iterator. Ensure that there are field
    * aliases set first.
    * @return the iterator
    */ 
   public Iterator<String> getFieldAliasesIterator()
   {
      return fieldAliases.keySet().iterator();
   }

   /**
    * Gets the alias for the given field. Ensure that there are field
    * aliases set first.
    * @param fieldName the field name
    * @return the alias or null if not found
    */ 
   public String getFieldAlias(String fieldName)
   {
      return fieldAliases.get(fieldName);
   }

   /**
    * Gets the name of the field that was mapped to the given name.
    * @param mappedName the mapped field name
    * @return the original field name or null if no mapping found
    */ 
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

   /**
    * Gets the "replicate-override" setting.
    * @return true if replicate override setting is on
    */ 
   public boolean isReplicateOverrideOn()
   {
      return replicateOverride;
   }

   /**
    * Gets the "replicate-missing-field-action" setting.
    * @return the enum corresponding to the setting
    */ 
   public MissingFieldAction getFallbackOnMissingReplicateAction()
   {
      return missingFieldReplicateAction;
   }

   /**
    * Gets the missing field action for the given option.
    */  
   public MissingFieldAction getMissingFieldAction(String option)
   {
      if (option.equals("assign-fields"))
      {
         return missingFieldAssignAction;
      }
      else if (option.equals("copy-to-glossary"))
      {
         return missingFieldCopyToGlossary;
      }
      else if (option.equals("omit-fields"))
      {
         return missingFieldOmitFields;
      }
      else if (option.equals("flatten-lonely-condition"))
      {
         return missingFieldFlattenLonely;
      }
      else if (option.equals("replicate-fields"))
      {
         return getFallbackOnMissingReplicateAction();
      }
      else
      {
         throw new IllegalArgumentException("Invalid missing field action identifier: '"
           + "'");
      }
   }

   /**
    * Determines whether or not the "replicate-fields" setting has
    * been set.
    * @return true if "replicate-fields" setting has been set
    */ 
   public boolean hasFieldCopies()
   {
      return fieldCopies != null;
   }

   /**
    * Gets the field replication iterator. Ensure that the
    * "replicate-fields" setting has been set first.
    * @return the iterator
    */ 
   public Iterator<String> getFieldCopiesIterator()
   {
      return fieldCopies.keySet().iterator();
   }

   /**
    * Gets the replication list for the given field. Ensure that the
    * "replicate-fields" setting has been set first.
    * @param fieldName the field name
    * @return the list of field names or none if not provided for
    * the given field
    */ 
   public Vector<String> getFieldCopy(String fieldName)
   {
      return fieldCopies.get(fieldName);
   }

   /**
    * Determines whether or not the "assign-fields" setting has
    * been set.
    * @return true if "assign-fields" setting has been set
    */ 
   public boolean hasFieldAssignments()
   {
      return assignFieldsData != null;
   }

   /**
    * Gets the "assign-fields" data.
    * @return the list of field assignment data
    */ 
   public Vector<FieldAssignment> getFieldAssignments()
   {
      return assignFieldsData;
   }

   /**
    * Gets the "assign-override" setting.
    * @return true if assign override setting is on
    */ 
   public boolean isAssignOverrideOn()
   {
      return assignFieldsOverride;
   }

   /**
    * Determines whether or not the primary and dual locations should be
    * combined.
    * @return true if "combine-dual-locations" not "false"
    */ 
   public boolean isCombineDualLocationsOn()
   {
      return combineDualLocations != COMBINE_DUAL_LOCATIONS_OFF;
   }

   /**
    * Gets the "combine-dual-locations" setting.
    * @return the numeric identifier corresponding to the setting
    */ 
   public int getCombineDualLocations()
   {
      return combineDualLocations;
   }

   /**
    * Gets the "category" setting.
    * @return the category or null if not set
    */ 
   public String getCategory()
   {
      return category;
   }

   /**
    * Gets the "dual-category" setting.
    * @return the dual category or null if not set
    */ 
   public String getDualCategory()
   {
      return dualCategory;
   }

   /**
    * Gets the abbreviation name fallback.
    * @return the abbreviation name fallback
    */ 
   public String getAbbrevDefaultNameField()
   {
      return abbrevDefaultNameField;
   }

   /**
    * Gets the abbreviation text fallback.
    * @return the abbreviation text fallback
    */ 
   public String getAbbrevDefaultTextField()
   {
      return abbrevDefaultTextField;
   }

   /**
    * Gets the abbreviation sort fallback.
    * @return the abbreviation sort fallback
    */ 
   public String getAbbrevDefaultSortField()
   {
      return abbrevDefaultSortField;
   }

   /**
    * Gets the entry sort fallback.
    * @return the entry sort fallback
    */ 
   public String getEntryDefaultSortField()
   {
      return entryDefaultSortField;
   }

   /**
    * Gets the symbol sort fallback.
    * @return the symbol sort fallback
    */ 
   public String getSymbolDefaultSortField()
   {
      return symbolDefaultSortField;
   }

   /**
    * Gets the bibtex entry sort fallback.
    * @return the bibtex entry sort fallback
    */ 
   public String getBibTeXEntryDefaultSortField()
   {
      return bibTeXEntryDefaultSortField;
   }

   /**
    * Gets the custom sort fallback for the given original entry bib type.
    * @param originalEntryType the original entry bib type
    * @return the fallback identified by the "custom-sort-fallbacks"
    * setting or null if not provided
    */ 
   public String getCustomEntryDefaultSortField(String originalEntryType)
   {
      return (customEntryDefaultSortFields == null ? null : 
         customEntryDefaultSortFields.get(originalEntryType));
   }

   /**
    * Determines whether or not to use non-breakable space.
    * @return true if {@code --no-break-space} global setting on
    */ 
   public boolean useNonBreakSpace()
   {
      return bib2gls.useNonBreakSpace();
   }

   /**
    * Determines whether or not to use interpreter.
    * @return true if {@code --interpret} global setting on
    */ 
   public boolean useInterpreter()
   {
      return bib2gls.useInterpreter();
   }

   /**
    * Interprets TeX code.
    * @param texCode the TeX code as a string
    * @param bibVal the TeX code obtained from parsing a bib field
    * @param trim if true, trim the result
    * @return the interpreted value or the first argument if no
    * interpreter or the value can't be parsed
    */ 
   public String interpret(String texCode, BibValueList bibVal, boolean trim)
   {
      return bib2gls.interpret(texCode, bibVal, trim);
   }

   /**
    * Determines whether or not label fields should be interpreted.
    * Corresponds to "intepret-label-fields" setting but also
    * requires the interpreter to be enabled.
    * @return true if label fields should be interpreted
    */ 
   public boolean isInterpretLabelFieldsEnabled()
   {
      return interpretLabelFields && bib2gls.useInterpreter();
   }

   /**
   * Determines whether or not cross-resource references are
   * permitted.
   * Cross-resource references aren't permitted if the resource set
   * has a preamble that's interpreted
   *     (preamble != null AND interpretPreamble)
   * @return true if cross-resource references are allowed
   */
   public boolean allowsCrossResourceRefs()
   {
      return preamble == null || !interpretPreamble;
   }

   /**
    * Determines whether or not labelify fields is enabled.
    * Not currently used. May be removed in future.
    * TODO check if this is correct and what it's for.
    * @return true if enabled
    */ 
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

   /**
    * Gets "strip-missing-parents" setting.
    * @return true if setting on
    */ 
   public boolean isStripMissingParentsEnabled()
   {
      return stripMissingParents;
   }

   /**
    * Determines whether or not missing parents should be created.
    * @return true if missing parents should be created
    */ 
   public boolean isCreateMissingParentsEnabled()
   {
      return createMissingParents;
   }

   /**
    * Gets "copy-alias-to-see" setting.
    * @return true if setting on
    */ 
   public boolean isCopyAliasToSeeEnabled()
   {
      return copyAliasToSee;
   }

   /**
    * Gets "min-loc-range" setting.
    * @return the minimum number of locations to form a range
    */ 
   public int getMinLocationRange()
   {
      return minLocationRange;
   }

   /**
    * Gets "suffixF" setting.
    * @return the single page suffix or null if not set
    */ 
   public String getSuffixF()
   {
      return suffixF;
   }

   /**
    * Gets "suffixFF" setting.
    * @return the multi page suffix or null if not set
    */ 
   public String getSuffixFF()
   {
      return suffixFF;
   }

   /**
    * Gets "see" setting.
    * @return the numeric identifier corresponding to the setting
    */ 
   public int getSeeLocation()
   {
      return seeLocation;
   }

   /**
    * Gets "seealso" setting.
    * @return the numeric identifier corresponding to the setting
    */ 
   public int getSeeAlsoLocation()
   {
      return seeAlsoLocation;
   }

   /**
    * Gets "alias" setting.
    * @return the numeric identifier corresponding to the setting
    */ 
   public int getAliasLocation()
   {
      return aliasLocation;
   }

   /**
    * Determines whether or not the location prefix should be shown.
    * @return true if the location prefix should be shown
    */ 
   public boolean showLocationPrefix()
   {
      return locationPrefix != null;
   }

   /**
    * Determines whether or not the location suffix should be shown.
    * @return true if the location suffix should be shown
    */ 
   public boolean showLocationSuffix()
   {
      return locationSuffix != null;
   }

   /**
    * Gets "max-loc-diff" setting.
    * @return the maximum location gap setting
    */ 
   public int getLocationGap()
   {
      return locGap;
   }

   /**
    * Gets "save-index-counter" setting.
    * @return the setting or null if off
    */ 
   public String getSaveIndexCounter()
   {
      return indexCounter;
   }

   /**
    * Gets the paths to the supplemental PDFs.
    * @return list of paths or null if no supplemental files
    * specified
    */
   public Vector<TeXPath> getSupplementalPaths()
   {
      return supplementalPdfPaths;
   }

   /**
    * Determines whether or not the given format is a primary
    * location format.
    * @param format the location format (encap)
    * @return true if the format has been identified as a
    * primary/principal format
    */ 
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

   /**
    * Gets save primary/principal locations setting.
    * @return the numeric identifier corresponding to the setting
    */ 
   public int getSavePrimaryLocationSetting()
   {
      return savePrimaryLocations;
   }

   /**
    * Gets save primary/principal location counters setting.
    * @return the numeric identifier corresponding to the setting
    */ 
   public int getPrimaryLocationCountersSetting()
   {
      return primaryLocCounters;
   }

   /**
    * Determines whether or not counter is considered a primary
    * location counter.
    * @return true if counter identified by "loc-counters" or
    * "loc-counters" not set or "primary-loc-counters" is "combine" or "split"
    */ 
   public boolean isPrimaryLocationCounterAllowed(String counter)
   {
      if (counters == null
       || primaryLocCounters == PRIMARY_LOCATION_COUNTERS_COMBINE
       || primaryLocCounters == PRIMARY_LOCATION_COUNTERS_SPLIT)
      {
         return true;
      }

      for (String c : counters)
      {
         if (c.equals(counter)) return true;
      }

      return false;
   }

   public boolean isSaveLocationsOff()
   {
      return saveLocations == SAVE_LOCATIONS_OFF;
   }

   public boolean isSaveAnyNonIgnoredLocationsOn()
   {
      return saveLocations == SAVE_LOCATIONS_ON;
   }

   public boolean isSaveAliasLocationsOn()
   {
      return saveLocations == SAVE_LOCATIONS_ON
           || saveLocations == SAVE_LOCATIONS_SEE
           || saveLocations == SAVE_LOCATIONS_SEE_NOT_ALSO
           || saveLocations == SAVE_LOCATIONS_ALIAS_ONLY;
   }

   public boolean isSaveSeeLocationsOn()
   {
      return saveLocations == SAVE_LOCATIONS_ON
           || saveLocations == SAVE_LOCATIONS_SEE
           || saveLocations == SAVE_LOCATIONS_SEE_NOT_ALSO;
   }

   public boolean isSaveSeeAlsoLocationsOn()
   {
      return saveLocations == SAVE_LOCATIONS_ON
           || saveLocations == SAVE_LOCATIONS_SEE;
   }

   public int getSaveLocationsSetting()
   {
      return saveLocations;
   }

   public boolean isMergeRangesOn()
   {
      return mergeRanges;
   }

   /**
    * Gets the "compact-ranges" setting.
    * @return the setting as a numeric value (0=false) 
    */
   public int getCompactRanges()
   {
      return compactRanges;
   }

   /**
    * Gets the "adopted-parent-field" setting.
    * @return the field name or null if not set
    */ 
   public String getAdoptedParentField()
   {
      return adoptedParentField;
   }

   /**
    * Gets the "primary-dual-dependency" setting.
    * @return true if the setting is on
    */ 
   public boolean hasDualPrimaryDepencendies()
   {
      return dualPrimaryDependency;
   }

   /**
    * Gets the field encapsulator control sequence name for the
    * "encapsulate-fields" setting.
    * @param field the field name
    * @return control sequence name or null if no encapsulation
    * required
    */ 
   public String getFieldEncap(String field)
   {
      if (encapFields == null) return null;

      String csname = encapFields.get(field);

      if ("".equals(csname)) return null;

      return csname;
   }

   /**
    * Gets the field encapsulator control sequence name for the
    * "encapsulate-fields*" setting.
    * @param field the field name
    * @return control sequence name or null if no encapsulation
    * required
    */ 
   public String getFieldEncapIncLabel(String field)
   {
      if (encapFieldsIncLabel == null) return null;

      String csname = encapFieldsIncLabel.get(field);

      if ("".equals(csname)) return null;

      return csname;
   }

   /**
    * Gets the sort encapsulator control sequence name for the
    * "encapsulate-sort" setting.
    * @return control sequence name or null if no encapsulation
    * required
    */ 
   public String getSortEncapCsName()
   {
      return encapSort;
   }

   /**
    * Gets the integer format pattern for the given field.
    * Corresponds to the "format-integer-fields" setting.
    * @param field the field name
    * @return format pattern or null if not required
    */ 
   public String getIntegerFieldFormat(String field)
   {
      if (formatIntegerFields == null) return null;

      String format = formatIntegerFields.get(field);

      return format;
   }

   /**
    * Gets the decimal format pattern for the given field.
    * Corresponds to the "format-decimal-fields" setting.
    * @param field the field name
    * @return format pattern or null if not required
    */ 
   public String getDecimalFieldFormat(String field)
   {
      if (formatDecimalFields == null) return null;

      String format = formatDecimalFields.get(field);

      return format;
   }

   /**
    * Gets the "prefix-only-existing" setting.
    * @return true if the setting is on
    */ 
   public boolean isInsertPrefixOnlyExists()
   {
      return insertPrefixOnlyExists;
   }

   /**
    * Determines whether or not the "append-prefix-field" setting is
    * on for the given field.
    * @param field the field name
    * @return true if setting is on
    */ 
   public boolean isAppendPrefixFieldEnabled(String field)
   {
      if (appendPrefixField == PREFIX_FIELD_NONE || prefixFields == null)
      {
         return false;
      }

      for (String f : prefixFields)
      {
         if (f.equals(field))
         {
            return true;
         }
      }

      return false;
   }

   /**
    * Gets the TeX code to append to the given prefix field.
    * The current content is supplied in order to check if it ends
    * with any identified exceptions.
    * @param field the field name
    * @param list the field's TeX code content
    * @return the suffix to add to the field or null if no suffix
    * required
    */ 
   public TeXObject getAppendPrefixFieldObject(String field, TeXObjectList list)
   {
      TeXParser bibParser = getBibParser();

      if (!isAppendPrefixFieldEnabled(field))
      {
         return null;
      }

      TeXObject obj = null;
      int endIdx = list.size()-1;

      for (; endIdx >= 0; endIdx--)
      {
         obj = list.get(endIdx);

         if (!(obj instanceof Ignoreable))
         {
            break;
         }
      }

      if (obj == null)
      {
         return null;
      }

      if (obj instanceof ControlSequence && prefixFieldCsExceptions != null)
      {
         String csName = ((ControlSequence)obj).getName();

         if (prefixFieldCsExceptions.contains(csName))
         {
            if (bib2gls.isDebuggingOn())
            {
               bib2gls.logMessage(bib2gls.getMessage("message.append.prefix.cs.nospace",
                  field, csName));
            }

            return null;
         }
      }

      int codePoint;

      if (obj instanceof CharObject)
      {
         codePoint = ((CharObject)obj).getCharCode();
      }
      else if (obj instanceof ActiveChar)
      {
         codePoint = ((ActiveChar)obj).getCharCode();
      }
      else
      {
         if (bib2gls.isDebuggingOn())
         {
            bib2gls.logMessage(bib2gls.getMessage("message.append.prefix.no.excp",
               prefixControlSequence.toString(bibParser), field,
               obj.toString(bibParser)));
         }

         return prefixControlSequence;
      }

      for (Integer num : prefixFieldExceptions)
      {
         if (num.intValue() == codePoint)
         {
            if (bib2gls.isDebuggingOn())
            {
               bib2gls.logMessage(bib2gls.getMessage("message.append.prefix.nospace", 
                 field, String.format("0x%X", codePoint)));
            }

            return null;
         }
      }

      if (appendPrefixField == PREFIX_FIELD_APPEND_SPACE
            || prefixFieldNbspPattern == null)
      {
         if (bib2gls.isDebuggingOn())
         {
            bib2gls.logMessage(bib2gls.getMessage("message.append.prefix.space", 
              prefixControlSequence.toString(bibParser), field));
         }

         return prefixControlSequence;
      }

      for (int i = 0; i <= endIdx; i++)
      {
         obj = list.get(i);

         if (!(obj instanceof Ignoreable))
         {
            String subStr = list.substring(bibParser, i, endIdx+1);

            Matcher m = prefixFieldNbspPattern.matcher(subStr);

            if (m.matches())
            {
               if (bib2gls.isDebuggingOn())
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                    "message.append.prefix.nbsp.match",
                    field, subStr, list.toString(bibParser), prefixFieldNbspPattern));
               }

               return new Nbsp();
            }

            if (bib2gls.isDebuggingOn())
            {
               bib2gls.logMessage(bib2gls.getMessage("message.append.prefix.space",
                  prefixControlSequence.toString(bibParser), field));
            }

            return prefixControlSequence;
         }
      }

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.logMessage(bib2gls.getMessage("message.append.prefix.no.excp", field,
            obj.toString(bibParser)));
      }

      return prefixControlSequence;
   }

   /**
    * Adds the dual mappings for the prefix fields.
    * @param map the dual map that requires updating
    */ 
   private void addPrefixMaps(HashMap<String,String> map)
   {
      map.put("prefix", "dualprefix");
      map.put("dualprefix", "prefix");
      map.put("prefixplural", "dualprefixplural");
      map.put("dualprefixplural", "prefixplural");
      map.put("prefixfirst", "dualprefixfirst");
      map.put("dualprefixfirst", "prefixfirst");
      map.put("prefixfirstplural", "dualprefixfirstplural");
      map.put("dualprefixfirstplural", "prefixfirstplural");
   }

   /**
    * Gets the name of the gather-parsed-dependencies field.
    * @return the field name if "gather-parsed-dependencies" on otherwise
    * null
    */ 
   public String getGatherParsedDependenciesField()
   {
      return gatherParsedDeps;
   }

   /**
    * Gets the name of the definition index field.
    * @return the field name if "save-definition-index" on otherwise
    * null
    */ 
   public String getDefinitionIndexField()
   {
      return saveDefinitionIndex ? DEFINITION_INDEX_FIELD : null;
   }

   /**
    * Gets the name of the use index field.
    * @return the field name if "save-use-index" on otherwise
    * null
    */ 
   public String getUseIndexField()
   {
      return saveUseIndex ? USE_INDEX_FIELD : null;
   }

   /**
    * Determines whether or not there are any mgls records
    * associated with the given label.
    * @param label the label
    * @return true if there are any mgls records
    */ 
   public boolean hasEntryMglsRecords(String label)
   {
      if (compoundEntriesHasRecords != COMPOUND_MGLS_RECORDS_TRUE)
      {
         return false;
      }
   
      for (Iterator<CompoundEntry> it=getCompoundEntryValueIterator();
           it != null && it.hasNext(); )
      {
         CompoundEntry comp = it.next();

         if (bib2gls.isMglsRefd(comp.getLabel()))
         {
            String[] elements = comp.getElements();

            for (String elem : elements)
            {
               if (elem.equals(label))
               {
                  return true;
               }
            }
         }
      }

      return false;
   }

   /**
    * Added pruned "see" label.
    * Used to mark the given label as having been pruned from the
    * "see" list corresponding to the given entry.
    * @param label the pruned cross-reference
    * @param entry the entry with the "see" field
    */ 
   public void prunedSee(String label, Bib2GlsEntry entry)
   {
      PrunedEntry pruned = null;

      if (prunedEntryMap == null)
      {
         prunedEntryMap = new HashMap<String,PrunedEntry>();
      }
      else
      {
         pruned = prunedEntryMap.get(label);
      }

      if (pruned == null)
      {
         pruned = new PrunedEntry(label);
         prunedEntryMap.put(label, pruned);
      }

      pruned.fromSee(entry);
   }

   /**
    * Added pruned "seealso" label.
    * Used to mark the given label as having been pruned from the
    * "seealso" list corresponding to the given entry.
    * @param label the pruned cross-reference
    * @param entry the entry with the "seealso" field
    */ 
   public void prunedSeeAlso(String label, Bib2GlsEntry entry)
   {
      PrunedEntry pruned = null;

      if (prunedEntryMap == null)
      {
         prunedEntryMap = new HashMap<String,PrunedEntry>();
      }
      else
      {
         pruned = prunedEntryMap.get(label);
      }

      if (pruned == null)
      {
         pruned = new PrunedEntry(label);
         prunedEntryMap.put(label, pruned);
      }

      pruned.fromSeeAlso(entry);
   }

   /**
    * Determines whether or not the given entry constitutes a "see" dead end.
    * @return true if setting on
    */ 
   public boolean isSeeDeadEnd(String label)
   {
      if (pruneSeePatterns == null || isDependent(label)
            || bib2gls.isDependent(label)
            || bib2gls.isEntrySelected(label))
      {
         return false;
      }

      Bib2GlsEntry entry = getEntry(label);

      if (entry == null) return true;// can't find entry

      if (entry.hasRecords()
            || notMatch(entry, pruneSeePatternsAnd, pruneSeePatterns))
      {
         return false;
      }

      return true;
   }

   /**
    * Determines whether or not the given entry constitutes a "seealso" dead end.
    * @return true if setting on
    */ 
   public boolean isSeeAlsoDeadEnd(String label)
   {
      if (pruneSeeAlsoPatterns == null || isDependent(label)
           || bib2gls.isDependent(label)
           || bib2gls.isEntrySelected(label))
      {
         return false;
      }

      Bib2GlsEntry entry = getEntry(label);

      if (entry == null) return true;// can't find entry

      if (entry.hasRecords()
           || notMatch(entry, pruneSeeAlsoPatternsAnd, pruneSeeAlsoPatterns))
      {
         return false;
      }

      return true;
   }

   /**
    * Determines whether or not to prune "see" dead ends.
    * @return true if setting on
    */ 
   public boolean isPruneSeeDeadEndsOn()
   {
      return pruneSeePatterns != null;
   }

   /**
    * Determines whether or not to prune "seealso" dead ends.
    * @return true if setting on
    */ 
   public boolean isPruneSeeAlsoDeadEndsOn()
   {
      return pruneSeeAlsoPatterns != null;
   }

   public Bib2Gls getBib2Gls()
   {
      return bib2gls;
   }

   /**
    * Inner class for sorting field values containing a
    * comma-separated list of labels.
    */ 
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
         if (bib2gls.isVerbose())
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

         for (GlsRecord rec : allRecords)
         {
            if (element.equals(rec.getLabel()))
            {
               set.add(rec);
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
            GlsRecord rec = iter.next();

            if (addSep)
            {
               elementList.add(listener.getOther(','));
               builder.append(',');
            }
            else
            {
               addSep = true;
            }

            String id = rec.getLabel();
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

            Bib2GlsEntry copy;

            String id = element.toString(bibParser);
            Bib2GlsEntry elementEntry = getEntry(id);

            if (elementEntry == null)
            {
               copy = entry.getMinimalCopy();

               String prefix = entry.getPrefix();

               if (prefix != null && id.startsWith(prefix) 
                   && id.length() > prefix.length())
               {
                  id = id.substring(prefix.length());
               }

               copy.setId(prefix, id);
            }
            else
            {
               copy = elementEntry.getMinimalCopy();
            }

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

      public String toString()
      {
         StringBuilder builder = new StringBuilder();

         for (int i = 0; i < fields.length; i++)
         {
            if (i > 0) builder.append(',');

            builder.append(fields[i]);
         }

         return String.format("%s[fields=[%s],method=%s,csname=%s]",
            getClass().getSimpleName(), builder, method, csname);
      }

      private String[] fields;
      private String method;
      private String csname;
   }

   private TeXParser parser;// the aux file parser

   private File texFile;

   private Vector<TeXPath> sources;

   private boolean interpretLabelFields = false;

   private boolean stripMissingParents = false;

   private HashMap<String,String> entryTypeAliases = null;

   private String unknownEntryMap = null;

   private HashMap<String,String> fieldAliases = null;

   private HashMap<String,Vector<String>> fieldCopies = null;

   private boolean replicateOverride=false;

   private MissingFieldAction missingFieldReplicateAction = MissingFieldAction.SKIP;

   private Vector<FieldAssignment> assignFieldsData;

   private boolean assignFieldsOverride=false;

   private MissingFieldAction missingFieldAssignAction = MissingFieldAction.FALLBACK;

   private Vector<FieldEvaluation> copyToGlossary = null;

   private MissingFieldAction missingFieldCopyToGlossary = MissingFieldAction.FALLBACK;

   private Vector<FieldEvaluation> omitFields = null;

   private MissingFieldAction missingFieldOmitFields = MissingFieldAction.FALLBACK;

   private String[] skipFields = null;

   private String[] bibtexAuthorList = null;

   private String[] interpretFields = null;

   private String[] hexUnicodeFields = null;

   public static final byte INTERPRET_FIELD_ACTION_REPLACE=(byte)0;
   public static final byte INTERPRET_FIELD_ACTION_REPLACE_NON_EMPTY=(byte)1;
   private byte interpretFieldAction = (byte)0;

   private String[] dateTimeList = null;
   private String[] dateList = null;
   private String[] timeList = null;

   private String dateTimeListFormat = "default";
   private String dateListFormat = "default";
   private String timeListFormat = "default";

   private String dualDateTimeListFormat = "default";
   private String dualDateListFormat = "default";
   private String dualTimeListFormat = "default";

   private Locale resourceLocale = null;

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

   private String symbolDefaultSortField = "original id";
   private String entryDefaultSortField = "name";

   private String abbrevDefaultSortField = "short";
   private String abbrevDefaultNameField = "short";
   private String abbrevDefaultTextField = "short";

   private String bibTeXEntryDefaultSortField = "name";

   private HashMap<String,String> customEntryDefaultSortFields = null;

   private String fieldConcatenationSeparator = " ";

   private String dualType=null, dualCategory=null, dualCounter=null;

   private String triggerType=null, ignoredType=null;

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
   private HashMap<String,Pattern> secondaryFieldPatterns = null;

   private boolean notMatch=false;
   private boolean secondaryNotMatch=false;

   private boolean fieldPatternsAnd=true;
   private boolean secondaryFieldPatternsAnd=true;

   private final byte MATCH_ACTION_FILTER = 0; 
   private final byte MATCH_ACTION_ADD = 1; 

   private byte matchAction = MATCH_ACTION_FILTER;
   private byte secondaryMatchAction = MATCH_ACTION_FILTER;

   private final byte WRITE_ACTION_DEFINE=0;
   private final byte WRITE_ACTION_COPY=1;
   private final byte WRITE_ACTION_DEFINE_OR_COPY=2;
   private final byte WRITE_ACTION_PROVIDE=3;

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

   private Vector<String> selectedEntries;

   private boolean hasDuals = false, hasTertiaries = false;

   private Bib2Gls bib2gls;

   private int seeLocation=Bib2GlsEntry.POST_SEE;
   private int seeAlsoLocation=Bib2GlsEntry.POST_SEE;
   private int aliasLocation=Bib2GlsEntry.POST_SEE;

   private String[] locationPrefix = null;

   private String[] locationSuffix = null;

   public static final int PROVIDE_DEF_GLOBAL=0;
   public static final int PROVIDE_DEF_LOCAL_ALL=1;
   public static final int PROVIDE_DEF_LOCAL_INDIVIDUAL=2;

   private int locationPrefixDef=PROVIDE_DEF_LOCAL_INDIVIDUAL;
   private int locationSuffixDef=PROVIDE_DEF_LOCAL_INDIVIDUAL;

   public static final int SAVE_LOCATIONS_OFF=0;
   public static final int SAVE_LOCATIONS_ON=1;// any non-ignored locations
   public static final int SAVE_LOCATIONS_SEE=2;// see, alias or see also
   public static final int SAVE_LOCATIONS_SEE_NOT_ALSO=3;// see, alias
   public static final int SAVE_LOCATIONS_ALIAS_ONLY=4;// alias only

   private int saveLocations = SAVE_LOCATIONS_ON;
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
   public static final int COMBINE_DUAL_LOCATIONS_PRIMARY_RETAIN_PRINCIPAL=4;
   public static final int COMBINE_DUAL_LOCATIONS_DUAL_RETAIN_PRINCIPAL=5;

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

   private Conditional flattenLonelyConditional = null;

   private MissingFieldAction missingFieldFlattenLonely = MissingFieldAction.FALLBACK;

   private boolean saveChildCount = false;

   private boolean saveSiblingCount = false;

   private boolean saveRootAncestor = false;

   private String saveOriginalEntryType = null;

   private boolean defpagesname = false;

   public static final int ALIAS_LOC_OMIT=0;
   public static final int ALIAS_LOC_TRANS=1;
   public static final int ALIAS_LOC_KEEP=2;

   private int aliasLocations = ALIAS_LOC_TRANS;

   private boolean aliases = false;

   private String labelPrefix = null, dualPrefix="dual.";

   private Vector<String> customLabelPrefixes = null;

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

   private HashMap<String,String> titleCaseCommands;

   private boolean wordBoundarySpace=true;
   private boolean wordBoundaryCsSpace=true;
   private boolean wordBoundaryNbsp=false;
   private boolean wordBoundaryDash=false;

   private String[] noCaseChangeCs = null;

   private HashMap<String,String> encapFields, encapFieldsIncLabel;
   private HashMap<String,String> formatIntegerFields;
   private HashMap<String,String> formatDecimalFields;

   private String encapSort = null;

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

   public static final int PRIMARY_LOCATION_COUNTERS_COMBINE=0;
   public static final int PRIMARY_LOCATION_COUNTERS_MATCH=1;
   public static final int PRIMARY_LOCATION_COUNTERS_SPLIT=2;

   private int primaryLocCounters = PRIMARY_LOCATION_COUNTERS_COMBINE;

   private boolean mergeRanges = false;

   private Random random=null;

   private HashMap<String,GroupTitle> groupTitleMap=null;

   private Vector<SupplementalRecord> supplementalRecords=null;
   private TeXPath supplementalPdfPath=null;
   private Vector<TeXPath> supplementalPdfPaths=null;
   private String[] supplementalSelection=null;
   private String supplementalCategory=null;

   private String groupField = null;

   public static final int GROUP_LEVEL_SETTING_EXACT=0;
   public static final int GROUP_LEVEL_SETTING_LESS_THAN=1;
   public static final int GROUP_LEVEL_SETTING_LESS_THAN_EQ=2;
   public static final int GROUP_LEVEL_SETTING_GREATER_THAN=3;
   public static final int GROUP_LEVEL_SETTING_GREATER_THAN_EQ=4;

   private int groupLevelSettingValue = 0;
   private int groupLevelSetting = GROUP_LEVEL_SETTING_EXACT;

   private int mergeSmallGroupLimit = 0;// 0 => don't merge

   private String saveOriginalId = null;

   public static final int SAVE_ORIGINAL_ALWAYS=0;
   public static final int SAVE_ORIGINAL_NO_OVERRIDE=1;
   public static final int SAVE_ORIGINAL_CHANGED_OVERRIDE=2;
   public static final int SAVE_ORIGINAL_CHANGED_NO_OVERRIDE=3;

   private int saveOriginalIdAction = SAVE_ORIGINAL_ALWAYS;
   private int saveOriginalEntryTypeAction = SAVE_ORIGINAL_ALWAYS;

   private String indexCounter=null;

   private String saveFromAlias = null;
   private String saveFromSeeAlso = null;
   private String saveFromSee = null;

   private String saveCrossRefTail = null;

   private String gatherParsedDeps = null;

   private boolean saveDefinitionIndex = false;
   private boolean saveUseIndex = false;

   public static final String DEFINITION_INDEX_FIELD ="definitionindex";
   public static final String USE_INDEX_FIELD ="useindex";

   public static final int SELECTION_RECORDED_AND_DEPS=0;
   public static final int SELECTION_RECORDED_AND_DEPS_AND_SEE=1;
   public static final int SELECTION_RECORDED_NO_DEPS=2;
   public static final int SELECTION_RECORDED_AND_PARENTS=3;
   public static final int SELECTION_ALL=4;
   public static final int SELECTION_RECORDED_AND_DEPS_AND_SEE_NOT_ALSO=5;
   public static final int SELECTION_DEPS_BUT_NOT_RECORDED=6;
   public static final int SELECTION_PARENTS_BUT_NOT_RECORDED=7;
   public static final int SELECTION_SELECTED_BEFORE=8;

   private int selectionMode = SELECTION_RECORDED_AND_DEPS;

   private static final String[] SELECTION_OPTIONS = new String[]
    {"recorded and deps", "recorded and deps and see",
     "recorded no deps", "recorded and ancestors", "all",
     "recorded and deps and see not also",
     "deps but not recorded", "ancestors but not recorded",
     "selected before"};

   public static final byte CONTRIBUTOR_ORDER_SURNAME=0;
   public static final byte CONTRIBUTOR_ORDER_VON=1;
   public static final byte CONTRIBUTOR_ORDER_FORENAMES=2;

   private byte contributorOrder=CONTRIBUTOR_ORDER_VON;

   public static final int PREFIX_FIELD_NONE=0;
   public static final int PREFIX_FIELD_APPEND_SPACE=1;
   public static final int PREFIX_FIELD_APPEND_SPACE_OR_NBSP=2;

   private int appendPrefixField = PREFIX_FIELD_NONE;

   private static final String[] PREFIX_FIELD_OPTIONS = new String[]
    {"none", "space", "space or nbsp"};

   private Vector<Integer> prefixFieldExceptions = null;

   private Vector<String> prefixFieldCsExceptions = null;

   private String[] prefixFields = new String[] 
     {
       "prefix",
       "prefixplural",
       "prefixfirst",
       "prefixfirstplural",
       "dualprefix",
       "dualprefixplural",
       "dualprefixfirst",
       "dualprefixfirstplural"
    };

   private Pattern prefixFieldNbspPattern = Pattern.compile(".");

   private ControlSequence prefixControlSequence = new TeXCsRef("space");

   private Vector<String> dependencies;

   private boolean dualPrimaryDependency=true;

   private int limit=0;

   private boolean copyAliasToSee = false;

   private boolean createMissingParents = false;

   private boolean childListUpdated = false;

   private boolean insertPrefixOnlyExists=false;

   private int compactRanges = 0;

   private Bib2GlsBibParser bibParserListener = null;

   private Vector<String> additionalUserFields = null;

   private boolean compoundEntriesDependent = false;
   private boolean compoundEntriesAddHierarchy = false;

   public static final int COMPOUND_ADJUST_NAME_FALSE=0;
   public static final int COMPOUND_ADJUST_NAME_ONCE=1;
   public static final int COMPOUND_ADJUST_NAME_UNIQUE=2;

   private int compoundAdjustName = COMPOUND_ADJUST_NAME_FALSE;

   private HashMap<String,CompoundEntry> compoundEntries;

   public static final int COMPOUND_DEF_FALSE=0;
   public static final int COMPOUND_DEF_REFD=1;
   public static final int COMPOUND_DEF_ALL=2;

   private int compoundEntriesDef = COMPOUND_DEF_REFD;

   public static final int COMPOUND_MGLS_RECORDS_FALSE=0;
   public static final int COMPOUND_MGLS_RECORDS_TRUE=1;
   public static final int COMPOUND_MGLS_RECORDS_DEFAULT=2;

   private int compoundEntriesHasRecords = COMPOUND_MGLS_RECORDS_DEFAULT;

   private boolean compoundEntriesGlobal = true;
   private boolean compoundTypeOverride = false;

   private String compoundMainType = null;
   private String compoundOtherType = null;

   private HashMap<String,Pattern> pruneSeePatterns=null, pruneSeeAlsoPatterns=null;
   private boolean pruneSeePatternsAnd=true, pruneSeeAlsoPatternsAnd=true;

   private HashMap<String,PrunedEntry> prunedEntryMap;

   private int pruneIterations=1;

   public static final int MAX_PRUNE_ITERATIONS=20;

   private String[] stringWordExceptions = new String[] {};

   private Matcher lastMatcher = null;

   private boolean wordifyMathGreek = false;
   private boolean wordifyMathSymbol = false;

   private boolean abbrvFontCommandsWritten = false;
}

