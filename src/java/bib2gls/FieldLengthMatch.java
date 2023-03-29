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

/**
 * Compares the length of a field with a numeric value. Note that 
 * the length is the number of characters in the detokenized string
 * not a token count.
 */

public class FieldLengthMatch extends FieldNumberMatch
{
   public FieldLengthMatch(Field field, Relational relation, Number value)
   {
      super(field, relation, value);
   }

   public boolean booleanValue(Bib2GlsEntry entry)
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      String fieldValue = null;

      try
      {
         fieldValue = field.getStringValue(entry);
      }
      catch (IOException e)
      {
         bib2gls.debug(e);
      }

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
      return String.format("\\LEN{%s} %s %s", field, relation, value);
   }
}
