/*
    Copyright (C) 2023-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.bib2gls;

import java.io.IOException;

import com.dickimawbooks.texparserlib.bib.BibValue;

import com.dickimawbooks.bibgls.common.Bib2GlsException;

/**
 * Compares a field element to null.
 */
public class FieldNullMatch implements Conditional
{
   public FieldNullMatch(FieldValueElement fieldValueElem, boolean equals)
   {
      this.fieldValueElem = fieldValueElem;
      this.equals = equals;
   }

   public boolean booleanValue(Bib2GlsEntry entry)
   throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      BibValue value = fieldValueElem.getValue(entry);
      boolean result;

      if (value == null)
      {
         result = equals ? true : false;
      }
      else
      {
         result = equals ? false : true;
      }

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.logAndPrintMessage(
           String.format(
             "Entry: %s%nCondition: %s%nValue: %s%nResult: %s",
              entry, toString(), value, result
           )
         );
      }

      return result;
   }

   @Override
   public String toString()
   {
      if (equals)
      {
         return String.format("%s = NULL", fieldValueElem);
      }
      else
      {
         return String.format("%s <> NULL", fieldValueElem);
      }
   }

   protected boolean equals;
   protected FieldValueElement fieldValueElem;
}
