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
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;
import java.text.CollationKey;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2GlsDualAbbrev extends Bib2GlsDualEntry
{
   public Bib2GlsDualAbbrev(Bib2Gls bib2gls)
   {
      this(bib2gls, "dualabbreviation");
   }

   public Bib2GlsDualAbbrev(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   public HashMap<String,String> getMappings()
   {
      return getResource().getDualAbbrevMap();
   }

   public String getFirstMap()
   {
      return getResource().getFirstDualAbbrevMap();
   }

   public boolean backLink()
   {
      return getResource().backLinkFirstDualAbbrevMap();
   }

   protected Bib2GlsDualEntry createDualEntry()
   {
      return new Bib2GlsDualAbbrev(bib2gls, getEntryType());
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

      if (getField("dualshort") == null)
      {
         missingFieldWarning("dualshort");
      }

      if (getField("duallong") == null)
      {
         missingFieldWarning("duallong");
      }
   }

   @Override
   public String getSortFallbackField()
   {
      return resource.getAbbrevDefaultSortField();
   }

   public String getFallbackValue(String field)
   {
      String val;

      if (field.equals("sort"))
      {
         String fallbackField = getSortFallbackField();
         val = getFieldValue(fallbackField);

         return val == null ? getFallbackValue(fallbackField) : val;
      }
      else if (field.equals("name"))
      {
         String fallbackField = resource.getAbbrevDefaultNameField();
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
         String fallbackField = getSortFallbackField();
         val = getField(fallbackField);

         return val == null ? getFallbackContents(fallbackField) : val;
      }
      else if (field.equals("name"))
      {
         String fallbackField = resource.getAbbrevDefaultNameField();
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
      String shortText = "";
      String longText = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         if (field.equals("short"))
         {
            shortText = getFieldValue(field);
         }
         else if (field.equals("long"))
         {
            longText = getFieldValue(field);
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

      writer.println(String.format("}%%%n{%s}%%%n{%s}",
        shortText, longText));
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      // syntax: {label}{opts}{short}{long}

      writer.println("\\glsxtrprovidestoragekey{dualshort}{}{}");
      writer.println("\\glsxtrprovidestoragekey{dualshortplural}{}{}");
      writer.println("\\glsxtrprovidestoragekey{duallong}{}{}");
      writer.println("\\glsxtrprovidestoragekey{duallongplural}{}{}");

      writer.format("\\providecommand{\\%s}[4]{%%%n", getCsName());

      String newcs = getEntryType();

      if (newcs.endsWith("acronym"))
      {
         newcs = "acronym";
      }
      else
      {
         newcs = "abbreviation";
      }

      writer.format("  \\new%s[#2]{#1}{#3}{#4}%%%n", newcs);

      writer.println("}");
   }
}
