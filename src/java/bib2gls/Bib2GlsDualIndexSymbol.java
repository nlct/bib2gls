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

public class Bib2GlsDualIndexSymbol extends Bib2GlsDualEntry
{
   public Bib2GlsDualIndexSymbol(Bib2Gls bib2gls)
   {
      this(bib2gls, "dualindexsymbol", "symbol");
   }

   public Bib2GlsDualIndexSymbol(Bib2Gls bib2gls, String entryType)
   {
      this(bib2gls, entryType, "symbol");
   }

   public Bib2GlsDualIndexSymbol(Bib2Gls bib2gls, String entryType,
      String defaultCategory)
   {
      super(bib2gls, entryType);
      category = defaultCategory;
   }

   public HashMap<String,String> getMappings()
   {
      return getResource().getDualIndexSymbolMap();
   }

   public String getFirstMap()
   {
      return getResource().getFirstDualIndexSymbolMap();
   }

   public boolean backLink()
   {
      return getResource().backLinkFirstDualIndexSymbolMap();
   }

   protected Bib2GlsDualEntry createDualEntry()
   {
      return new Bib2GlsDualIndexSymbol(bib2gls, getEntryType()+"secondary");
   }

   public void checkRequiredFields()
   {
      if (getField("symbol") == null)
      {
         missingFieldWarning("symbol");
      }
   }

   @Override
   public String getSortFallbackField()
   {
      return isPrimary() ?
           "name" : resource.getSymbolDefaultSortField();
   }

   public String getFallbackValue(String field)
   {
      String val;

      if (field.equals("sort"))
      {
         String fallbackField = getSortFallbackField();

         if (fallbackField.equals("id"))
         {
            return getOriginalId();
         }

         val = getFieldValue(fallbackField);

         return val == null ? getFallbackValue(fallbackField) : val;
      }
      else if (field.equals("name"))
      {
         return getOriginalId();
      }

      return super.getFallbackValue(field);
   }

   public BibValueList getFallbackContents(String field)
   {
      if (field.equals("sort"))
      {
         String fallbackField = getSortFallbackField();

         if (fallbackField.equals("id"))
         {
            return getIdField();
         }

         BibValueList val = getField(fallbackField);

         return val == null ? getFallbackContents(fallbackField) : val;
      }
      else if (field.equals("name") && bib2gls.useInterpreter())
      {
         return getIdField();
      }

      return super.getFallbackContents(field);
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\%s{%s}%%%n{", getCsName(), getId());

      String sep = "";
      String descStr = "";
      String nameStr = null;
      String symbolStr = "";

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
         else if (field.equals("symbol"))
         {
            symbolStr = getFieldValue(field);
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

      if (nameStr == null)
      {
         nameStr = getFallbackValue("name");
      }

      writer.println(String.format("}%%%n{%s}{%s}%n{%s}", 
        nameStr, symbolStr, descStr));
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      // syntax: {label}{opts}{name}{symbol}{description}

      writer.format("\\providecommand{\\%s}[5]{%%%n", getCsName());

      writer.print("  \\longnewglossaryentry*{#1}{name={#3},");

      if (isPrimary())
      {
         writer.println("category={index},");
      }
      else
      {
         writer.format("category={%s},", category);
      }

      writer.println("symbol={#4},#2}{#5}%");

      writer.println("}");
   }

   private String category;
}
