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
 * Compares the value of a field element with the value of another field element.
 * If either field element evaluates to null, it will be treated as empty.
 */
public class FieldFieldMatch implements Conditional
{
   public FieldFieldMatch(FieldValueElement fieldValueElem1,
     Relational relation, FieldValueElement fieldValueElem2)
   {
      this.fieldValueElem1 = fieldValueElem1;
      this.fieldValueElem2 = fieldValueElem2;
      this.relation = relation;
   }

   public boolean booleanValue(Bib2GlsEntry entry)
    throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();

      String fieldValue1 = fieldValueElem1.getStringValue(entry);

      if (fieldValue1 == null)
      {
         fieldValue1 = "";
      }

      String fieldValue2 = fieldValueElem2.getStringValue(entry);

      if (fieldValue2 == null)
      {
         fieldValue2 = "";
      }

      boolean result = compare(fieldValue1, fieldValue2);

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.logAndPrintMessage(
           String.format(
             "Entry: %s%nCondition: %s%nValue 1: \"%s\"%nValue 2: \"%s\"%nResult: %s",
               entry, toString(), fieldValue1, fieldValue2, result
           )
         );
      }

      return result;
   }

   protected boolean compare(String fieldValue1, String fieldValue2)
   {
      int result = fieldValue1.compareTo(fieldValue2);

      switch (relation)
      {
         case EQUALS: return result == 0;
         case NOT_EQUALS: return result != 0;
         case LT: return result < 0;
         case LE: return result <= 0;
         case GT: return result > 0;
         case GE: return result >= 0;
      }

      throw new AssertionError("Missing Relational enum " + relation);
   }

   @Override
   public String toString()
   {
      return String.format("%s %s %s", fieldValueElem1, relation, fieldValueElem2);
   }

   protected FieldValueElement fieldValueElem1, fieldValueElem2;
   protected Relational relation;
}
