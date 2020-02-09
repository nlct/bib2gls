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

public class Bib2GlsIndexPlural extends Bib2GlsIndex
{
   public Bib2GlsIndexPlural(Bib2Gls bib2gls)
   {
      this(bib2gls, "indexplural");
   }

   public Bib2GlsIndexPlural(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   // initialise the text field if a label prefix is supplied

   protected void initMissingFields()
   {
      if (getResource().getLabelPrefix() != null
         && getFieldValue("text") == null)
      {
         putField("text", getOriginalId());
      }
   }

   public String getFallbackValue(String field)
   {
      if (field.equals("text"))
      {
         return getOriginalId();
      }
      else if (field.equals("name"))
      {
         String val = getFieldValue("plural");

         if (val != null)
         {
            return val;
         }

         return getFallbackValue("plural");
      }
      else
      {
         return super.getFallbackValue(field);
      }
   }

   public BibValueList getFallbackContents(String field)
   {
      if (field.equals("text"))
      {
         if (!bib2gls.useInterpreter())
         {
            bib2gls.warningMessage("warning.interpreter.needed.fallback",
              field, getId());
            return null;
         }

         String name = getOriginalId();
         BibValueList list = new BibValueList();
         list.add(new BibUserString(
            bib2gls.getInterpreterListener().createGroup(name)));

         return list;
      }
      else if (field.equals("name"))
      {
         BibValueList val = getField("plural");

         if (val != null)
         {
            return val;
         }

         return getFallbackContents("plural");
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

      String name = null;

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         if (field.equals("name"))
         {
            name = getFieldValue(field);
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

      writer.println("}");

      if (name == null)
      {
         name = getFallbackValue("name");
      }

      writer.println(String.format("{%s}", name));
      writeInternalFields(writer);
   }

   public void writeCsDefinition(PrintWriter writer) throws IOException
   {
      // syntax: {label}{opts}{name}

      writer.format("\\providecommand{\\%s}[3]{%%%n", getCsName());

      writer.print(" \\newglossaryentry{#1}");
      writer.println("{name={#3},category={indexplural},description={},#2}%");

      writer.println("}");
   }
}
