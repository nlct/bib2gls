/*
    Copyright (C) 2017-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.gls2bib;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.TeXApp;

public class GlsData
{
   public GlsData(String label)
   {
      this(label, "entry");
   }

   public GlsData(String label, String type)
   {
      this.id = label;
      this.type = type;
      fields = new HashMap<String,String>();
   }

   public String getId()
   {
      return id;
   }

   public void setEntryType(String type)
   {
      this.type = type;
   }

   public void setGlossaryType(String glosType)
   {
      this.glosType = glosType;
   }

   public String getGlossaryType()
   {
      return glosType;
   }

   public void setCategory(String category)
   {
      this.category = category;
   }

   public String getCategory()
   {
      return category;
   }

   // top-level braces should already be added to the fieldValue
   // if required
   public void putField(String fieldName, String fieldValue)
   {
      fields.put(fieldName, fieldValue);
   }

   public String removeField(String fieldName)
   {
      return fields.remove(fieldName);
   }

   public void writeBibEntry(PrintWriter writer)
     throws IOException
   {
      writer.format("@%s{%s", type, id);

      Iterator<String> it = fields.keySet().iterator();

      boolean hasFields = false;

      while (it.hasNext())
      {
         String key = it.next();

         // grouping should already have been added
         writer.format(",%n  %s = %s", key, fields.get(key));

         hasFields = true;
      }

      if (hasFields)
      {
         writer.println();
      }

      writer.println("}");
      writer.println();
   }

   public void absorbSee(TeXParser parser, String original, String key,
      String xrList, String optArg)
   {
      Gls2Bib gls2bib = (Gls2Bib)parser.getListener().getTeXApp();

      String existingValue = fields.get(key);

      if (existingValue == null)
      {
         if (key.equals("see"))
         {
            fields.put(key, String.format("{%s%s}", optArg, xrList));
         }
         else
         {
            fields.put(key, String.format("{%s}", xrList));
         }

         gls2bib.message(gls2bib.getMessage("gls2bib.absorbsee", original));
         return;
      }

      // split and reconstruct 

      Matcher m = SEE_PATTERN.matcher(existingValue);

      if (m.matches())
      {
         String originalXrList = m.group(1); 

         String[] originals = originalXrList.split(" *, *");
         String[] newLabels = xrList.split(",");

         String append = "";

         for (String label : newLabels)
         {
            if (Arrays.binarySearch(originals, label) < 0)
            {
               append += ","+label;
            }
         }

         fields.put(key, existingValue.substring(0, existingValue.length()-2)
           + append + "}");

         gls2bib.message(gls2bib.getMessage("gls2bib.absorbsee", original));
      }
      else
      {
         gls2bib.warning(parser, gls2bib.getMessage("gls2bib.absorbsee.failed",
           original, String.format("%s=%s", key, existingValue)));
      }
   }

   private static final Pattern SEE_PATTERN = 
      Pattern.compile("\\{(?:\\[.*\\])?(.*)\\}");

   private String id, type, glosType, category;

   private HashMap<String,String> fields;
}
