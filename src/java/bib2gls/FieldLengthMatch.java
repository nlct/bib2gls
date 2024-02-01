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

import com.dickimawbooks.bibgls.common.Bib2GlsException;

/**
 * Compares the length of a field element with a numeric value. Note that 
 * the length is the number of characters in the detokenized string
 * not a token count. Corresponds to the LEN quark in the left side 
 * of a numeric conditional context.
 */

public class FieldLengthMatch extends FieldNumberMatch
{
   public FieldLengthMatch(FieldValueElement fieldValueElem,
     Relational relation, Number value)
   {
      super(fieldValueElem, relation, value);
   }

   public boolean booleanValue(Bib2GlsEntry entry)
   throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      String fieldValue = fieldValueElem.getStringValue(entry);

      int num1 = 0;

      if (fieldValue != null)
      {
         num1 = fieldValue.length();
      }

      boolean result;

      int num2 = value.intValue();

      result = compare(num1, num2);

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.logAndPrintMessage(
           String.format(
            "Entry: %s%nCondition: %s%nValue: \"%s\"%nResult: %s",
             entry, toString(), fieldValue, result
           )
         );
      }

      return result;
   }

   @Override
   public String toString()
   {
      return String.format("\\LEN{%s} %s %s", fieldValueElem, relation, value);
   }
}
