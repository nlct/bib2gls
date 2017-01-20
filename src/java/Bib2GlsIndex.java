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

public class Bib2GlsIndex extends Bib2GlsEntry
{
   public Bib2GlsIndex(String prefix, Bib2Gls bib2gls)
   {
      this(prefix, bib2gls, "index");
   }

   public Bib2GlsIndex(String prefix, Bib2Gls bib2gls, String entryType)
   {
      super(prefix, bib2gls, entryType);
   }

   public void checkRequiredFields(TeXParser parser)
   {// no required fields
   }

   public String getDefaultSort()
   {
      String name = getFieldValue("name");

      return name == null ? getOriginalId() : name;
   }

   public String getFallbackField(String field)
   {
      if (field.equals("name"))
      {
         return getOriginalId();
      }
      else
      {
         return super.getFallbackField(field);
      }
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\bibglsnewterm{%s}%%%n{", getId());

      String sep = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         writer.format("%s", sep);

         sep = String.format(",%n");
         writer.format("%s={%s}", field, getFieldValue(field));
      }

      writer.println("}");
   }
}
