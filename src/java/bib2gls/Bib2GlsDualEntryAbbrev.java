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

public class Bib2GlsDualEntryAbbrev extends Bib2GlsDualEntry
{
   public Bib2GlsDualEntryAbbrev(Bib2Gls bib2gls)
   {
      this(bib2gls, "dualentryabbreviation");
   }

   public Bib2GlsDualEntryAbbrev(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   public HashMap<String,String> getMappings()
   {
      return getResource().getDualEntryAbbrevMap();
   }

   public String getFirstMap()
   {
      return getResource().getFirstDualEntryAbbrevMap();
   }

   public boolean backLink()
   {
      return getResource().backLinkFirstDualEntryAbbrevMap();
   }

   protected Bib2GlsEntry createDualEntry()
   {
      return new Bib2GlsDualEntryAbbrev(bib2gls, getEntryType()+"secondary");
   }

   public void checkRequiredFields(TeXParser parser)
   {
      if (getField("short") == null)
      {
         missingFieldWarning(parser, "short");
      }

      if (getField("long") == null)
      {
         missingFieldWarning(parser, "long");
      }

      if (getField("description") == null)
      {
         missingFieldWarning(parser, "description");
      }
   }

   public String getFallbackValue(String field)
   {
      String val;

      if (field.equals("name"))
      {
         val = getFieldValue("short");

         if (val != null) return val;
      }

      return super.getFallbackValue(field);
   }

   public BibValueList getFallbackContents(String field)
   {
      BibValueList val;

      if (field.equals("name"))
      {
         val = getField("short");

         if (val != null) return val;
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
         else
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, getFieldValue(field));
         }
      }

      writer.println(String.format("}%%%n{%s}{%s}%%%n{%s}",
        shortStr, longStr, descStr));
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      // syntax: {label}{opts}{short}{long}{description}

      writer.format("\\providecommand{\\%s}[5]{%%%n", getCsName());

      if (getEntryType().endsWith("secondary"))
      {
         writer.println("  \\longnewglossaryentry*{#1}{#2}{#5}%");
      }
      else
      {
         writer.println("  \\newabbreviation[#2]{#1}{#3}{#4}%");
      }

      writer.println("}");
   }
}
