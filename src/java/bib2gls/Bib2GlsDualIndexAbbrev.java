/*
    Copyright (C) 2017-2023 Nicola L.C. Talbot
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
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;
import java.text.CollationKey;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2GlsDualIndexAbbrev extends Bib2GlsDualEntry
{
   public Bib2GlsDualIndexAbbrev(Bib2Gls bib2gls)
   {
      this(bib2gls, "dualindexabbreviation");
   }

   public Bib2GlsDualIndexAbbrev(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   public HashMap<String,String> getMappings()
   {
      return getResource().getDualIndexAbbrevMap();
   }

   public String getFirstMap()
   {
      return getResource().getFirstDualIndexAbbrevMap();
   }

   public boolean backLink()
   {
      return getResource().backLinkFirstDualIndexAbbrevMap();
   }

   protected Bib2GlsDualEntry createDualEntry()
   {
      return new Bib2GlsDualIndexAbbrev(bib2gls, getEntryType()+"secondary");
   }

   public void checkRequiredFields()
   {
      if (getField("short") == null)
      {
         missingFieldWarning("short");
      }

      if (getField("long") == null)
      {
         missingFieldWarning("long");
      }
   }

   @Override
   public String getSortFallbackField()
   {
      String field = resource.getCustomEntryDefaultSortField(getOriginalEntryType());

      if (field != null)
      {
         return field;
      }

      return isPrimary() ?
           "name" : resource.getAbbrevDefaultSortField();
   }

   public String getFallbackValue(String field)
   {
      String val;

      if (field.equals("sort"))
      {
         return getSortFallbackValue();
      }
      else if (field.equals("name"))
      {
         String fallbackField = resource.getAbbrevDefaultNameField();
         val = getFieldValue(fallbackField);

         return val == null ? getFallbackValue(fallbackField) : val;
      }
      else if (field.equals("text"))
      {
         String fallbackField = resource.getAbbrevDefaultTextField();
         val = getFieldValue(fallbackField);

         return val == null ? getFallbackValue(fallbackField) : val;
      }

      return super.getFallbackValue(field);
   }

   public BibValueList getFallbackContents(String field)
   {
      BibValueList val;

      if (field.equals("sort"))
      {
         return getSortFallbackContents();
      }
      else if (field.equals("name"))
      {
         String fallbackField = resource.getAbbrevDefaultNameField();
         val = getField(fallbackField);

         return val == null ? getFallbackContents(fallbackField) : val;
      }
      else if (field.equals("text"))
      {
         String fallbackField = resource.getAbbrevDefaultTextField();
         val = getField(fallbackField);

         return val == null ? getFallbackContents(fallbackField) : val;
      }


      return super.getFallbackContents(field);
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\%s", getCsName());

      writer.format("{%s}", getId());

      if (isPrimary())
      {
         writer.format("{%s}", getDual().getId());
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
         }
         else if (bib2gls.getDebugLevel() > 0 && 
            !bib2gls.isInternalField(field) &&
            !bib2gls.isKnownSpecialField(field))
         {
            bib2gls.debugMessage("warning.ignoring.unknown.field", field);
         }
      }

      if (nameStr == null)
      {
         nameStr = getFallbackValue("name");

         writePluralIfInherited(writer, nameStr, parentid, plural, sep);
      }

      writer.println(String.format("}%%%n{%s}{%s}{%s}%n{%s}", 
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

         writer.println("\\ifdef\\glsuselongfont");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuselongfont}{\\glsuselongfont}");
         writer.println("}%");
         writer.println("{%");
         writer.println("  \\providecommand*{\\bibglsuselongfont}[2]{{\\glssetabbrvfmt{#2}\\glslongfont{#1}}}");
         writer.println("}%");

         // syntax: {label}{duallabel}{opts}{name}{short}{long}{description}

         writer.format("\\providecommand{\\%s}[7]{%%%n", getCsName());

         writer.println("  \\longnewglossaryentry*{#1}{%");
         writer.print("      name={\\protect");

         if (resource.getAbbrevDefaultNameField().equals("short"))
         {
            writer.print("\\bibglsuseabbrvfont");
         }
         else
         {
            writer.print("\\bibglsuselongfont");
         }

         writer.println("{#4}{\\glscategory{#2}}},%");
         writer.println("      category={index},#3}{}%");
      }
      else
      {
         // syntax: {label}{opts}{name}{short}{long}{description}

         writer.format("\\providecommand{\\%s}[6]{%%%n", getCsName());

         writer.println("  \\ifstrempty{#6}%");
         writer.println("  {\\newabbreviation[#2]{#1}{#4}{#5}}%");
         writer.println("  {\\newabbreviation[#2,description={#6}]{#1}{#4}{#5}}%");
      }

      writer.println("}");
   }
}
