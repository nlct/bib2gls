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
import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.WhiteSpace;

import com.dickimawbooks.texparserlib.bib.BibValue;
import com.dickimawbooks.texparserlib.bib.BibUserString;

/**
 * Represents the TRIM quark.
 */
public class FieldValueTrim implements FieldValueElement
{
   public FieldValueTrim(FieldValueElement fieldValueElem)
   {
      this.fieldValueElem = fieldValueElem;
   }

   @Override
   public BibValue getValue(Bib2GlsEntry entry)
     throws Bib2GlsException,IOException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      BibValue value = fieldValueElem.getValue(entry);

      if (value == null)
      {
         return null;
      }
      else
      {
         TeXParser parser = entry.getResource().getParser();

         TeXObjectList list = (TeXObjectList)value.expand(parser).clone();
         list.popLeadingWhiteSpace();

         for (int i = list.size()-1; i >= 0; i--)
         {
            TeXObject obj = list.get(i);

            if (obj instanceof WhiteSpace)
            {
               list.remove(i);
            }
            else
            {
               break;
            }
         }

         return new BibUserString(list);
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
         return fieldValue.trim();
      }
   }

   @Override
   public String toString()
   {
      return String.format("\\TRIM{%s}", fieldValueElem);
   }

   private FieldValueElement fieldValueElem;
}
