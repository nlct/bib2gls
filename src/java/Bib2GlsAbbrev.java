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

public class Bib2GlsAbbrev extends Bib2GlsEntry
{
   public Bib2GlsAbbrev(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   public String getDefaultSort()
   {
      String value = getFieldValue("short");

      if (value == null)
      {// shouldn't happen as the "short" field is required.

         return super.getDefaultSort();
      }

      return bib2gls.interpret(value, getField("short"));
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
      String val = super.getFallbackValue(field);

      if (val != null) return val;

      if (field.equals("longplural"))
      {
         val = getFieldValue("long");

         if (val == null)
         {
            val = getFallbackValue("long");

            if (val == null) return null;
         }

         String suffix = getResource().getPluralSuffix();

         return suffix == null ? val : val+suffix;
      }
      else if (field.equals("shortplural"))
      {
         val = getFieldValue("short");

         if (val == null)
         {
            val = getFallbackValue("short");

            if (val == null) return null;
         }

         String suffix = getResource().getShortPluralSuffix();

         return suffix == null ? val : val+suffix;
      }

      return null;
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\bibglsnew%s{%s}%%%n{", getEntryType(), getId());

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
         else
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, getFieldValue(field));
         }
      }

      writer.println(String.format("}%%%n{%s}%%%n{%s}",
        shortText, longText));
   }
}
