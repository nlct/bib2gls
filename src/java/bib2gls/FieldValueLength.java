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
import com.dickimawbooks.texparserlib.bib.BibUserString;

/**
 * Represents the LEN quark. In a field element context, this quark
 * returns a string. The numeric conditional LEN quark is represented by
 * FieldLengthMatch.
 */
public class FieldValueLength implements FieldValueElement
{
   public FieldValueLength(FieldValueElement fieldValueElem)
   {
      this.fieldValueElem = fieldValueElem;
   }

   @Override
   public BibValue getValue(Bib2GlsEntry entry)
     throws Bib2GlsException,IOException
   {
      String val = getStringValue(entry);

      if (val == null)
      {
         return null;
      }
      else
      {
         TeXParser parser = entry.getResource().getParser();

         return new BibUserString(parser.getListener().createString(val));
      }
   }

   @Override
   public String getStringValue(Bib2GlsEntry entry)
   throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      String fieldValue = fieldValueElem.getStringValue(entry);

      if (fieldValue == null)
      {
         return null;
      }
      else
      {
         return ""+fieldValue.length();
      }
   }

   @Override
   public String toString()
   {
      return String.format("\\LEN{%s}", fieldValueElem);
   }

   private FieldValueElement fieldValueElem;
}
