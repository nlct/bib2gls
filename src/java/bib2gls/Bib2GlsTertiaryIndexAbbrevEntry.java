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
package com.dickimawbooks.bibgls.bib2gls;

import java.io.*;
import java.util.Set;
import java.util.Iterator;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsTertiaryIndexAbbrevEntry extends Bib2GlsDualIndexAbbrev
{
   public Bib2GlsTertiaryIndexAbbrevEntry(Bib2Gls bib2gls)
   {
      this(bib2gls, "tertiaryindexabbreviationentry");
   }

   public Bib2GlsTertiaryIndexAbbrevEntry(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   public boolean hasTertiary()
   {
      return true;
   }

   protected Bib2GlsDualEntry createDualEntry()
   {
      return new Bib2GlsTertiaryIndexAbbrevEntry(bib2gls, 
        getEntryType()+"secondary");
   }

   public void checkRequiredFields()
   {
      super.checkRequiredFields();

      if (getField("description") == null)
      {
         missingFieldWarning("description");
      }
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      GlsResource resource = getResource();

      String tertiaryType = resource.getTertiaryType();
      String tertiaryCategory = resource.getTertiaryCategory();
      String tertiaryPrefix = resource.getTertiaryPrefix();

      if (tertiaryPrefix == null)
      {
         tertiaryPrefix = "";
      }

      writer.format("\\%s", getCsName());

      StringBuilder tertiaryFields = null;

      writer.format("{%s}", getId());

      if (isPrimary())
      {
         writer.format("{%s}", getDual().getId());
      }
      else
      {
         writer.format("{%s}", tertiaryPrefix+getOriginalId());

         tertiaryFields = new StringBuilder();
      }

      writer.format("%%%n{");

      String sep = "";
      String descStr = "";
      String nameStr = null;
      String parentid = null;
      String plural = null;
      String shortStr = "";
      String longStr = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         if (field.equals("description"))
         {
            descStr = getFieldValue(field);
         }
         else if (field.equals("name"))
         {
            nameStr = getFieldValue(field);
         }
         else if (field.equals("short"))
         {
            shortStr = getFieldValue(field);
         }
         else if (field.equals("long"))
         {
            longStr = getFieldValue(field);
         }
         else if (bib2gls.isKnownField(field))
         {
            String value = getFieldValue(field);

            if (field.equals("parent"))
            {
               parentid = value;
            }
            else if (field.equals("plural"))
            {
               plural = value;
            }

            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, value);

            if (tertiaryFields != null && !field.equals("location"))
            {
               if (tertiaryFields.length() > 0)
               {
                  tertiaryFields.append(',');
               }

               if (tertiaryType != null && field.equals("type"))
               {
                  value = tertiaryType;
                  tertiaryType = null;
               }
               else if (tertiaryCategory != null && field.equals("category"))
               {
                  value = tertiaryCategory;
                  tertiaryCategory = null;
               }

               tertiaryFields.append(String.format("%s={%s}", field, value));
            }
         }
      }

      if (nameStr == null)
      {
         nameStr = getFallbackValue("name");

         writePluralIfInherited(writer, nameStr, parentid, plural, sep);
      }

      writer.println("}%");

      if (tertiaryFields != null)
      {
         if (tertiaryCategory != null)
         {
            if (tertiaryFields.length() > 0)
            {
               tertiaryFields.append(',');
            }

            tertiaryFields.append(String.format("category={%s}",
                tertiaryCategory));
         }

         if (tertiaryType != null)
         {
            if (tertiaryFields.length() > 0)
            {
               tertiaryFields.append(',');
            }

            tertiaryFields.append(String.format("type={%s}",
                tertiaryType));
         }

         writer.format("{%s}", tertiaryFields);
      }

      writer.println(String.format("{%s}{%s}{%s}%n{%s}", 
        nameStr, shortStr, longStr, descStr));

      writeInternalFields(writer);
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      if (isPrimary())
      {
         writer.println("\\ifdef\\glsuseabbrvfont");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuseabbrvfont}{\\glsuseabbrvfont}");
         writer.println("}%");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuseabbrvfont}[2]{{\\glssetabbrvfmt{#2}\\glsabbrvfont{#1}}}");
         writer.println("}%");

         // syntax: {label}{duallabel}{opts}{name}{short}{long}{description}

         writer.format("\\providecommand{\\%s}[7]{%%%n", getCsName());

         writer.println("  \\longnewglossaryentry*{#1}{%");
         writer.println("      name={\\protect\\bibglsuseabbrvfont{#4}{\\glscategory{#2}}},%");
         writer.println("      category={index},#3}{}%");

      }
      else
      {
         writer.println("\\ifdef\\glsuselongfont");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuselongfont}{\\glsuselongfont}");
         writer.println("}%");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuselongfont}[2]{{\\glssetabbrvfmt{#2}\\glslongfont{#1}}}");
         writer.println("}%");


         // syntax: {label}{tertiarylabel}{opts}{tertiary opts}{name}{short}{long}{description}

         writer.format("\\providecommand{\\%s}[8]{%%%n", getCsName());

         writer.println("  \\newabbreviation[#3]{#1}{#6}{#7}%");

         writer.println("  \\longnewglossaryentry*{#2}%");
         writer.println("  {name={\\protect\\bibglsuselongfont{#7}{\\glscategory{#1}}},#4}%");

         writer.println("  {#8}%");
      }

      writer.println("}");
   }
}
