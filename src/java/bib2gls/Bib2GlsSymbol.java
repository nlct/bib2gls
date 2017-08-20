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
import java.util.Set;
import java.util.Iterator;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsSymbol extends Bib2GlsEntry
{
   public Bib2GlsSymbol(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   public void checkRequiredFields(TeXParser parser)
   {
      BibValueList name = getField("name");
      BibValueList parent = getField("parent");
      BibValueList description = getField("description");

      if (name != null || (parent != null && description != null))
      {
         return;
      }

      if (name == null && parent == null)
      {
         missingFieldWarning(parser, "name");
      }

      if (parent != null && description == null && name == null)
      {
         missingFieldWarning(parser, "description");
      }
   }

   public String getFallbackValue(String field)
   {
      if (field.equals("sort"))
      {
         return getOriginalId();
      }

      return super.getFallbackValue(field);

   }

   public BibValueList getFallbackContents(String field)
   {
      if (field.equals("sort"))
      {
         return getIdField();
      }

      return super.getFallbackContents(field);
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      // syntax: {label}{opts}{name}{description}

      writer.format("\\providecommand{\\%s}[4]{%%%n", getCsName());

      writer.print(" \\longnewglossaryentry*{#1}");
      writer.format("{name={#3},sort={#1},category={%s},#2}{#4}", 
         getEntryType());

      writer.println("}");
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\%s{%s}%%%n{", getCsName(), getId());

      String sep = "";
      String name = "";
      String description = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         if (field.equals("name"))
         {
            name = getFieldValue(field);
         }
         else if (field.equals("description"))
         {
            description = getFieldValue(field);
         }
         else
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, getFieldValue(field));
         }
      }

      writer.println(String.format("}%%%n{%s}%%%n{%s}", name,
         description));
   }

}
