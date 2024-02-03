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
 * Tests if the value of a field element is a substring of
 * the value of another field element.
 * If the first field element evaluates to empty or null, the
 * condition returns false. If the second field element evaluates to
 * null, it's treated as an empty string (in which case, the condition 
 * will be false). Note that an empty or null
 * value is not considered to be a prefix of another empty or
 * null value even though they may be considered equal.
 */
public class FieldPrefixOfField implements Conditional
{
   public FieldPrefixOfField(FieldValueElement fieldValueElem1,
     FieldValueElement fieldValueElem2)
   {
      this(fieldValueElem1, fieldValueElem2, false);
   }

   public FieldPrefixOfField(FieldValueElement fieldValueElem1,
      FieldValueElement fieldValueElem2, boolean negate)
   {
      this.fieldValueElem1 = fieldValueElem1;
      this.fieldValueElem2 = fieldValueElem2;
      this.negate = negate;
   }

   public boolean booleanValue(Bib2GlsEntry entry)
   throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      boolean result;

      String fieldValue1 = fieldValueElem1.getStringValue(entry);
      String fieldValue2 = null;

      if (fieldValue1 == null || fieldValue1.isEmpty())
      {
         result = false;
      }
      else
      {
         fieldValue2 = fieldValueElem2.getStringValue(entry);

         if (fieldValue2 == null)
         {
            fieldValue2 = "";
         }

         result = fieldValue2.startsWith(fieldValue1);
      }

      if (negate)
      {
         result = !result;
      }

      if (bib2gls.isDebuggingOn())
      {
         if (fieldValue2 == null)
         {
            bib2gls.logAndPrintMessage(
              String.format(
               "Entry: %s%nCondition: %s%nValue 1: \"%s\"%nResult: %s",
                entry, toString(), fieldValue1, result)
            );
         }
         else
         {
            bib2gls.logAndPrintMessage(
              String.format(
               "Entry: %s%nCondition: %s%nValue 1: \"%s\"%nValue 2: \"%s\"%nResult: %s",
                entry, toString(), fieldValue1, fieldValue2, result)
            );
         }
      }

      return result;
   }

   @Override
   public String toString()
   {
      return String.format("%s \\%s %s", 
        fieldValueElem1, negate ? "NOTPREFIXOF" : "PREFIXOF", fieldValueElem2);
   }

   protected FieldValueElement fieldValueElem1, fieldValueElem2;
   protected boolean negate;
}
