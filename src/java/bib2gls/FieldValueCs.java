/*
    Copyright (C) 2023 Nicola L.C. Talbot
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

import java.io.IOException;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.TeXObjectList;

import com.dickimawbooks.texparserlib.bib.BibValue;
import com.dickimawbooks.texparserlib.bib.BibValueList;
import com.dickimawbooks.texparserlib.bib.BibUserString;

/**
 * Represents the CS quark.
 */
public class FieldValueCs implements FieldValueElement
{
   public FieldValueCs(FieldValueElement fieldValueElem)
   {
      this.fieldValueElem = fieldValueElem;
   }

   @Override
   public BibValue getValue(Bib2GlsEntry entry)
     throws Bib2GlsException,IOException
   {
      String csname = getCsName(entry);

      if (csname == null)
      {
         return null;
      }

      return new BibUserString(new TeXCsRef(csname));
   }

   @Override
   public String getStringValue(Bib2GlsEntry entry)
   throws IOException,Bib2GlsException
   {
      String csname = getCsName(entry);

      if (csname == null)
      {
         return null;
      }
      else
      {
         if (!Character.isAlphabetic(csname.codePointAt(0)))
         {
            return String.format("\\%s", csname);
         }
         else
         {
            return String.format("\\%s ", csname);
         }
      }
   }

   protected String getCsName(Bib2GlsEntry entry)
   throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      BibValue value = fieldValueElem.getValue(entry);

      if (value == null)
      {
         return null;
      }
      else
      {
         GlsResource resource = entry.getResource();

         TeXParser parser = resource.getParser();

         TeXObjectList list = (TeXObjectList)value.expand(parser).clone();

         String csname = list.toString(parser);

         return csname.isEmpty() ? null : csname;
      }
   }

   @Override
   public String toString()
   {
      return String.format("\\CS{%s}", fieldValueElem);
   }

   private FieldValueElement fieldValueElem;
}
