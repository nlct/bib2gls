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
      this(bib2gls, "dualindexsymbol");
   }

   public Bib2GlsDualIndexSymbol(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
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

   public void checkRequiredFields(TeXParser parser)
   {
      if (getField("dualdescription") == null)
      {
         missingFieldWarning(parser, "dualdescription");
      }

      if (getField("symbol") == null)
      {
         missingFieldWarning(parser, "symbol");
      }
   }

   public String getFallbackValue(String field)
   {
      String val;

      if (field.equals("name"))
      {
         return getOriginalId();
      }

      return super.getFallbackValue(field);
   }

   public BibValueList getFallbackContents(String field)
   {
      if (field.equals("name") && bib2gls.useInterpreter())
      {
         String name = getOriginalId();
         BibValueList list = new BibValueList();
         list.add(new BibUserString(
            bib2gls.getInterpreterListener().createGroup(name)));

         return list;
      }
      else
      {
         return super.getFallbackContents(field);
      }
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

         if (field.equals("description") || field.equals("dualdescription"))
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

      writer.println(String.format("}%%%n{%s}{%s}%n{%s}", 
        nameStr, symbolStr, descStr));
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      // syntax: {label}{opts}{name}{symbol}{description}

      writer.format("\\providecommand{\\%s}[5]{%%%n", getCsName());

      if (isPrimary())
      {
         writer.println("  \\longnewglossaryentry*{#1}{name={#3},category={index},symbol={#4},#2}{}%");
      }
      else
      {
         writer.println("  \\longnewglossaryentry*{#1}{name={#3},category={symbol},symbol={#4},#2}{#5}%");
      }

      writer.println("}");
   }
}
