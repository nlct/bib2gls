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
import java.nio.charset.Charset;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.aux.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.CsvList;
import com.dickimawbooks.texparserlib.html.L2HConverter;

public class GlsResource
{
   public GlsResource(TeXParser parser, AuxData data)
    throws IOException,InterruptedException
   {
      sources = new Vector<TeXPath>();

      init(parser, data.getArg(0), data.getArg(1));
   }

   private void init(TeXParser parser, TeXObject opts, TeXObject arg)
      throws IOException,InterruptedException
   {
      bib2gls = (Bib2Gls)parser.getListener().getTeXApp();

      TeXPath texPath = new TeXPath(parser, 
        arg.toString(parser), "glstex");

      texFile = bib2gls.resolveFile(texPath.getFile());

      bib2gls.registerTeXFile(texFile);

      String filename = texPath.getTeXPath(true);

      KeyValList list = KeyValList.getList(parser, opts);

      String[] srcList = null;

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
         else if (opt.equals("external"))
         {// TODO
         }
         else if (opt.equals("match-op"))
         {
            String val = getChoice(parser, list, opt, "and", "or");

            fieldPatternsAnd = val.equals("and");
         }
         else if (opt.equals("match"))
         {
            TeXObject[] array = getTeXObjectArray(parser, list, opt);

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

                  if (split.size() != 2)
                  {
                     throw new IllegalArgumentException(
                       bib2gls.getMessage("error.invalid.opt.keylist.value", 
                        field, array[i].toString(parser), opt));
                  }

                  String val = split.get(1).toString(parser);

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
         else if (opt.equals("flatten"))
         {
            flatten = getBoolean(parser, list, opt);
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

            dualAbbrvMap = getDualMap(parser, list, opt, keys);

            dualAbbrvFirstMap = keys[0];
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
               backLinkDualAbbrv = true;
               backLinkDualSymbol = true;
            }
            else
            {
               backLinkDualEntry = false;
               backLinkDualAbbrv = false;
               backLinkDualSymbol = false;
            }
         }
         else if (opt.equals("dual-entry-backlink"))
         {
            backLinkDualEntry = getBoolean(parser, list, opt);
         }
         else if (opt.equals("dual-abbrv-backlink"))
         {
            backLinkDualAbbrv = getBoolean(parser, list, opt);
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
         else if (opt.equals("label-prefix"))
         {
            labelPrefix = getOptional(parser, list, opt);
         }
         else if (opt.equals("dual-prefix"))
         {
            dualPrefix = getOptional(parser, list, opt);
         }
         else if (opt.equals("sort"))
         {
            sort = getOptional(parser, "locale", list, opt);

            if (sort.equals("none") || sort.equals("unsrt"))
            {
               sort = null;
            }
         }
         else if (opt.equals("dual-sort"))
         {
            dualSort = getOptional(parser, "locale", list, opt);

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
         else if (opt.equals("loc-prefix"))
         {
            String[] values = getStringArray(parser, "true", list, opt);

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
                  locationPrefix = new String[]{bib2gls.getMessage("tag.page"),
                    bib2gls.getMessage("tag.pages")};
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

      if (dualAbbrvMap == null)
      {
         dualAbbrvMap = new HashMap<String,String>();
         dualAbbrvMap.put("short", "symbol");
         dualAbbrvMap.put("shortplural", "symbolplural");
         dualAbbrvMap.put("long", "description");
         dualAbbrvMap.put("longplural", "descriptionplural");
         dualAbbrvMap.put("symbol", "short");
         dualAbbrvMap.put("symbolplural", "shortplural");
         dualAbbrvMap.put("description", "long");
         dualAbbrvMap.put("descriptionplural", "longplural");

         dualAbbrvFirstMap = "short";
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

         for (Iterator<String> it = dualAbbrvMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualAbbrvMap.get(key)));
         }

         bib2gls.logMessage();
      }

      if (srcList == null)
      {
         sources.add(bib2gls.getBibFilePath(parser, filename));
      }
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
      CsvList csvList = CsvList.getList(parser, list.getValue(opt));

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

         map.put(key, obj2.toString(parser));

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

   private TeXObjectList trimList(TeXObjectList list)
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

         if (!bib2gls.isKnownField(field))
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

         BibParser bibParserListener = new BibParser(bib2gls, srcCharset)
         {
            protected void addPredefined()
            {
               parser.putActiveChar(new Bib2GlsAt());
            }

         };

         TeXParser texParser = bibParserListener.parseBibFile(bibFile);

         Vector<BibData> list = bibParserListener.getBibData();

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

               // does this entry have any records?

               boolean hasRecords = false;
               boolean dualHasRecords = false;

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

                     bibData.add(dual);
                  }
                  else
                  {
                     dualData.add(dual);
                  }
               }

               if (selectionMode == SELECTION_RECORDED_AND_DEPS)
               {
                  if (hasRecords)
                  {
                     // does this entry have a "see" field?

                     entry.initCrossRefs(parser);

                     for (Iterator<String> it = entry.getDependencyIterator();
                          it.hasNext(); )
                     {
                        String dep = it.next();

                        bib2gls.addDependent(dep);
                     }

                     if (dual != null)
                     {
                        bib2gls.addDependent(dual.getId());
                     }
                  }

                  if (dualHasRecords)
                  {
                     // Does the "see" field need setting?

                     if (dual.getFieldValue("see") == null)
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
            }
            else if (data instanceof BibPreamble)
            {
                preamble = ((BibPreamble)data).getPreamble()
                              .expand(texParser).toString(texParser);
            }
         }
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

   private Bib2GlsEntry getEntry(String label, Vector<Bib2GlsEntry> data)
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

   private void processData(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries,
      String entrySort, String entrySortField)
      throws Bib2GlsException
   {
      Vector<String> fields = bib2gls.getFields();
      Vector<GlsRecord> records = bib2gls.getRecords();

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
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
               }

               entries.add(entry);
            }
         }
      }

      processDepsAndSort(data, entries, entrySort, entrySortField);
   }

   private void processDepsAndSort(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries,
      String entrySort, String entrySortField)
      throws Bib2GlsException
   {
      // add any dependencies

      if (selectionMode == SELECTION_RECORDED_AND_DEPS)
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

      // sort if required

      int entryCount = entries.size();

      if (entrySort != null && !entrySort.equals("use") && entryCount > 0)
      {
         if (entrySort.equals("letter-case"))
         {
            Bib2GlsEntryLetterComparator comparator = 
               new Bib2GlsEntryLetterComparator(bib2gls, entries, 
                 entrySort, entrySortField, false);

            comparator.sortEntries();
         }
         else if (entrySort.equals("letter-nocase"))
         {
            Bib2GlsEntryLetterComparator comparator = 
               new Bib2GlsEntryLetterComparator(bib2gls, entries, 
                 entrySort, entrySortField, true);

            comparator.sortEntries();
         }
         else
         {
            Bib2GlsEntryComparator comparator = 
               new Bib2GlsEntryComparator(bib2gls, entries, 
                  entrySort, entrySortField, 
                  collatorStrength, collatorDecomposition);

            comparator.sortEntries();
         }
      }
   }

   public int processData()
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
      checkFieldMaps(dualAbbrvMap, "dual-abbrv-map");
      checkFieldMaps(dualSymbolMap, "dual-symbol-map");

      Vector<Bib2GlsEntry> entries = new Vector<Bib2GlsEntry>();

      processData(bibData, entries, sort, sortField);

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

               dualEntries.add(dual);
            }
         }

         processDepsAndSort(dualData, dualEntries, dualSort, dualSortField);

         entryCount += dualEntries.size();
      }

      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));

      // Already checked openout_any in init method

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(texFile, bib2gls.getTeXCharset().name());

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

         if (bib2gls.useGroupField())
         {
            writer.println("\\providecommand{\\bibglslettergroup}[5]{#1}");
            writer.println("\\providecommand{\\bibglsothergroup}[2]{\\glssymbolsgroupname}");
            writer.println();
         }

         if (locationPrefix != null)
         {
              writer.println("\\providecommand{\\bibglspostprefix}{\\ }");

              if (type == null)
              {
                 writer.println("\\appto\\glossarypreamble{%");
              }
              else
              {
                 writer.format("\\apptoglossarypreamble[%s]{%%%n", type);
              }

              writer.println(" \\providecommand{\\bibglsprefix}[1]{%");
              if (type == null)
              {
                 writer.println("  \\ifcase#1");
              }
              else
              {
                 writer.println("  \\ifcase##1");
              }

              for (int i = 0; i < locationPrefix.length; i++)
              {
                 writer.format("  \\%s %s\\bibglspostprefix%n",
                   (i == locationPrefix.length-1 ? "else" : "or"), 
                   locationPrefix[i]);
              }

              writer.println("  \\fi");
              writer.println(" }");

              writer.println("}");
         }

         // syntax: {label}{opts}{name}{description}

         writer.println("\\providecommand{\\bibglsnewentry}[4]{%");
         writer.print(" \\longnewglossaryentry*{#1}");
         writer.println("{name={#3},#2}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewdualentry}[4]{%");
         writer.print(" \\longnewglossaryentry*{#1}");
         writer.println("{name={#3},#2}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewsymbol}[4]{%");
         writer.print(" \\longnewglossaryentry*{#1}");
         writer.println("{name={#3},sort={#1},category={symbol},#2}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewnumber}[4]{%");
         writer.print(" \\longnewglossaryentry*{#1}");
         writer.println("{name={#3},sort={#1},category={number},#2}{#4}%");
         writer.println("}");

         // syntax: {label}{opts}
         writer.println("\\providecommand*{\\bibglsnewterm}[2]{%");
         writer.println(" \\newglossaryentry{#1}{name={#1},description={},#2}%");
         writer.println("}");

         // syntax: {label}{opts}{short}{long}
         writer.println("\\providecommand{\\bibglsnewacronym}[4]{%");
         writer.println("  \\newacronym[#2]{#1}{#3}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewabbreviation}[4]{%");
         writer.println("  \\newabbreviation[#2]{#1}{#3}{#4}%");
         writer.println("}");

         if (preamble != null)
         {
            writer.println();
            writer.println(preamble);
         }

         writer.println();

         Vector<Bib2GlsEntry> secondaryList = null;

         if (secondaryType != null)
         {
            secondaryList = new Vector<Bib2GlsEntry>(entryCount);
         }

         Vector<String> widestNames = null;
         Vector<Double> widest = null;
         Font font = null;
         FontRenderContext frc = null;

         TeXParser parser = null;

         if (setWidest)
         {
            L2HConverter listener = new L2HConverter(bib2gls);
            listener.setIsInDocEnv(true);

            parser = new TeXParser(listener);

            widestNames = new Vector<String>();
            widest = new Vector<Double>();

            // Just using the JVM's default serif font as a rough
            // guide to guess the width.
            font = new Font("Serif", 0, 12);
            frc = new FontRenderContext(null, false, false);
         }

         for (int i = 0, n = entries.size(); i < n; i++)
         {
            Bib2GlsEntry entry = entries.get(i);

            bib2gls.verbose(entry.getId());

            entry.updateLocationList(minLocationRange,
              suffixF, suffixFF, seeLocation, locationPrefix != null, locGap);

            checkParent(entry, i, entries);

            entry.writeBibEntry(writer);
            entry.writeLocList(writer);

            writer.println();

            if (secondaryList != null)
            {
               secondaryList.add(entry);
            }

            if (widestNames != null)
            {
               updateWidestName(parser, entry, widestNames, 
                 widest, font, frc);
            }
         }

         if (dualEntries != null)
         {
            for (int i = 0, n = dualEntries.size(); i < n; i++)
            {
               Bib2GlsEntry entry = dualEntries.get(i);

               bib2gls.verbose(entry.getId());

               entry.updateLocationList(minLocationRange,
                 suffixF, suffixFF, seeLocation, locationPrefix != null, locGap);
               checkParent(entry, i, dualEntries);

               entry.writeBibEntry(writer);
               entry.writeLocList(writer);

               writer.println();

               if (secondaryList != null)
               {
                  secondaryList.add(entry);
               }

               if (widestNames != null)
               {
                  updateWidestName(parser, entry, widestNames, 
                    widest, font, frc);
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
               if (secondarySort.equals("letter-case"))
               {
                  Bib2GlsEntryLetterComparator comparator = 
                     new Bib2GlsEntryLetterComparator(bib2gls, secondaryList, 
                       secondarySort,
                       secondaryField == null ? sortField : secondaryField,
                       false);

                  comparator.sortEntries();
               }
               else if (secondarySort.equals("letter-nocase"))
               {
                  Bib2GlsEntryLetterComparator comparator = 
                     new Bib2GlsEntryLetterComparator(bib2gls, secondaryList, 
                       secondarySort,
                       secondaryField == null ? sortField : secondaryField,
                       true);

                  comparator.sortEntries();
               }
               else
               {
                  Bib2GlsEntryComparator comparator = 
                     new Bib2GlsEntryComparator(bib2gls, secondaryList, 
                        secondarySort,
                        secondaryField == null ? sortField : secondaryField,
                        collatorStrength, collatorDecomposition);

                  comparator.sortEntries();
               }

               for (Bib2GlsEntry entry : secondaryList)
               {
                  writer.format("\\glsxtrcopytoglossary{%s}{%s}",
                       entry.getId(), secondaryType);
                  writer.println();
               }
            }
         }

         if (widestNames != null)
         {// TODO check dualType
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

   private void updateWidestName(TeXParser parser,
     Bib2GlsEntry entry, Vector<String> widestNames, 
     Vector<Double> widest, Font font, FontRenderContext frc)
   {
      // This is just approximate as fonts, commands etc
      // will affect the width.

      String name = entry.getFieldValue("name");

      if (name == null || name.isEmpty()) return;

      String orgName = name;

      // In the event that the name field contains any TeX code,
      // strip out anything that's not 'letter' or 'other'

      try
      {
         BibValueList bibValList = entry.getField("name");

         if (bibValList != null)
         {
            TeXObjectList contents = bibValList.expand(parser);

            if (contents != null)
            {
               StringBuilder builder = new StringBuilder();

               TeXObjectList list = contents.expandfully(parser);

               if (list != null)
               {
                  contents = list;
               }

               for (TeXObject obj : contents)
               {
                  if (obj instanceof CharObject)
                  {
                     builder.appendCodePoint(((CharObject)obj).getCharCode());
                  }
               }

               name = builder.toString();
            }
         }
      }
      catch (IOException e)
      {// too complicated, just use the name
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

      TextLayout layout = new TextLayout(
        name, font, frc);

      double w = layout.getBounds().getWidth();

      bib2gls.debug(bib2gls.getMessage("message.calc.text.width",
        name, w));

      if (w > maxWidth)
      {
          if (level < widestNames.size())
          {
             widestNames.set(level, orgName);
             widest.set(level, new Double(w));
          }
          else
          {
             for (int j = widestNames.size(); j < level; j++)
             {
                widestNames.add("");
                widest.add(new Double(0.0));
             }

             widestNames.add(orgName);
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

      boolean found = false;

      for (int j = i-1; j >= 0; j--)
      {
         /*
           Search backwards.
           (If entry has a parent it's more likely to be nearby
            with the default sort.)
         */

         if (list.get(j).getId().equals(parentId))
         {
            return;
         }
      }

      bib2gls.warning(bib2gls.getMessage(
         "warning.parent.missing", parentId, entry.getId()));
      entry.removeFieldValue("parent");
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
      if (category != null)
      {
         if (category.equals("same as entry"))
         {
            entry.putField("category", entry.getEntryType());
         }
         else if (category.equals("same as type"))
         {
            String val = entry.getFieldValue("type");

            if (val != null)
            {
               entry.putField("category", val);
            }
         }
         else
         {
            entry.putField("category", category);
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

   public String getDualDescPluralSuffix()
   {
      return dualDescPluralSuffix;
   }

   public String getDualSymbolPluralSuffix()
   {
      return dualSymbolPluralSuffix;
   }

   public String getPluralSuffix()
   {
      return pluralSuffix;
   }

   public String getShortPluralSuffix()
   {
      return shortPluralSuffix;
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

   public HashMap<String,String> getDualAbbrvMap()
   {
      return dualAbbrvMap;
   }

   public String getFirstDualAbbrvMap()
   {
      return dualAbbrvFirstMap;
   }

   public boolean backLinkFirstDualAbbrvMap()
   {
      return backLinkDualAbbrv;
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

   private File texFile;

   private Vector<TeXPath> sources;

   private String[] skipFields = null;

   private String[] externalPrefixes = null;

   private String type=null, category=null, sort = "locale", sortField = "sort";

   private String dualType=null, dualCategory=null, 
      dualSort = null, dualSortField = "sort";

   private String pluralSuffix="\\glspluralsuffix ";

   private String shortPluralSuffix="\\abbrvpluralsuffix ";

   private String dualSymbolPluralSuffix="\\glspluralsuffix ", 
     dualDescPluralSuffix="\\glspluralsuffix ";

   private Charset bibCharset = null;

   private boolean flatten = false;

   private boolean setWidest = false;

   private String secondaryType=null, secondarySort=null, secondaryField=null;

   private int minLocationRange = 3, locGap = 1;

   private String suffixF, suffixFF;

   private String preamble = null;

   private HashMap<String,Pattern> fieldPatterns = null;

   private boolean fieldPatternsAnd=true;

   private static final String PATTERN_FIELD_ID = "id";
   private static final String PATTERN_FIELD_ENTRY_TYPE = "entrytype";

   private Vector<Bib2GlsEntry> bibData;

   private Vector<Bib2GlsEntry> dualData;

   private Bib2Gls bib2gls;

   private int collatorStrength=Collator.PRIMARY;

   private int collatorDecomposition=Collator.CANONICAL_DECOMPOSITION;

   private int seeLocation=Bib2GlsEntry.POST_SEE;

   private String[] locationPrefix = null;

   private String labelPrefix = null, dualPrefix="dual.";

   private String dualField = null;

   private HashMap<String,String> dualEntryMap, dualAbbrvMap,
      dualSymbolMap;

   // HashMap doesn't retain order, so keep track of the first
   // mapping separately.

   private String dualEntryFirstMap, dualAbbrvFirstMap, dualSymbolFirstMap;

   private boolean backLinkDualEntry=false;
   private boolean backLinkDualAbbrv=false;
   private boolean backLinkDualSymbol=false;

   public static final int SELECTION_RECORDED_AND_DEPS=0;
   public static final int SELECTION_RECORDED_NO_DEPS=1;
   public static final int SELECTION_RECORDED_AND_PARENTS=2;
   public static final int SELECTION_ALL=3;

   private int selectionMode = SELECTION_RECORDED_AND_DEPS;

   private static final String[] SELECTION_OPTIONS = new String[]
    {"recorded and deps", "recorded no deps", "recorded and ancestors", "all"};
}

