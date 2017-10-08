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
   }

   public String getFallbackValue(String field)
   {
      String val;

      if (field.equals("name"))
      {
         val = getFieldValue("short");

         return val == null ? getFallbackValue("short") : val;
      }

      return super.getFallbackValue(field);
   }

   public BibValueList getFallbackContents(String field)
   {
      BibValueList val;

      if (field.equals("name"))
      {
         val = getField("short");

         return val == null ? getFallbackContents("short") : val;
      }

      return super.getFallbackContents(field);
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\%s", getCsName());

      if (isPrimary())
      {
         writer.format("{%s}", getDual().getId());
      }

      writer.format("{%s}%%%n{", getId());

      String sep = "";
      String descStr = "";
      String nameStr = null;
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
         else
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, getFieldValue(field));
         }
      }

      if (nameStr == null)
      {
         nameStr = getFallbackValue("name");
      }

      writer.println(String.format("}%%%n{%s}{%s}{%s}%n{%s}", 
        nameStr, shortStr, longStr, descStr));
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      if (isPrimary())
      {
         writer.println("\\providecommand*{\\bibglsuseabbrvfont}[2]{{\\glssetabbrvfmt{#2}\\glsabbrvfont{#1}}}");

         // syntax: {duallabel}{label}{opts}{name}{short}{long}{description}

         writer.format("\\providecommand{\\%s}[7]{%%%n", getCsName());

         writer.println("  \\longnewglossaryentry*{#2}{%");
         writer.println("      name={\\protect\\bibglsuseabbrvfont{#4}{\\glscategory{#1}}},%");
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
