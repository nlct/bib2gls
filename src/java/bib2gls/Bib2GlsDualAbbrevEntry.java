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

public class Bib2GlsDualAbbrevEntry extends Bib2GlsDualEntry
{
   public Bib2GlsDualAbbrevEntry(Bib2Gls bib2gls)
   {
      this(bib2gls, "dualabbreviationentry");
   }

   public Bib2GlsDualAbbrevEntry(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   public HashMap<String,String> getMappings()
   {
      return getResource().getDualAbbrevEntryMap();
   }

   public String getFirstMap()
   {
      return getResource().getFirstDualAbbrevEntryMap();
   }

   public boolean backLink()
   {
      return getResource().backLinkFirstDualAbbrevEntryMap();
   }

   protected Bib2GlsDualEntry createDualEntry()
   {
      return new Bib2GlsDualAbbrevEntry(bib2gls, getEntryType()+"secondary");
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

      if (getField("description") == null)
      {
         missingFieldWarning("description");
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

      return isPrimary() ? resource.getAbbrevDefaultSortField() : super.getSortFallbackField();
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
      writer.format("\\%s{%s}%%%n{", getCsName(), getId());

      String sep = "";
      String shortStr = "";
      String longStr = "";
      String descStr = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         if (field.equals("short"))
         {
            shortStr = getFieldValue(field);
         }
         else if (field.equals("long"))
         {
            longStr = getFieldValue(field);
         }
         else if (field.equals("description"))
         {
            descStr = getFieldValue(field);
         }
         else if (bib2gls.isKnownField(field))
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, getFieldValue(field));
         }
         else if (bib2gls.getDebugLevel() > 0 && 
            !bib2gls.isInternalField(field) &&
            !bib2gls.isKnownSpecialField(field))
         {
            bib2gls.debugMessage("warning.ignoring.unknown.field", field);
         }
      }

      writer.println(String.format("}%%%n{%s}{%s}%%%n{%s}",
        shortStr, longStr, descStr));

      writeInternalFields(writer);
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      // syntax: {label}{opts}{short}{long}{description}

      writer.format("\\providecommand{\\%s}[5]{%%%n", getCsName());

      if (isPrimary())
      {
         writer.println("  \\newabbreviation[#2]{#1}{#3}{#4}%");
      }
      else
      {
         writer.println("  \\longnewglossaryentry*{#1}{#2}{#5}%");
      }

      writer.println("}");
   }
}
