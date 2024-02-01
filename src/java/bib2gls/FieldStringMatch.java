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
 * Compares the value of a field element with a constant string value.
 * If the field element evaluates to null, it will be treated as empty.
 */
public class FieldStringMatch implements Conditional
{
   public FieldStringMatch(FieldValueElement fieldValueElem,
       Relational relation, String value,
     boolean quoted, boolean insensitive)
   {
      this.fieldValueElem = fieldValueElem;
      this.relation = relation;
      this.value = value;
      this.quoted = quoted;
      this.insensitive = insensitive;
   }

   public boolean booleanValue(Bib2GlsEntry entry)
    throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      String fieldValue = fieldValueElem.getStringValue(entry);

      if (fieldValue == null)
      {
         fieldValue = "";
      }

      boolean result = compare(fieldValue);

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.logAndPrintMessage(
           String.format(
             "Entry: %s%nCondition: %s%nValue: \"%s\"%nResult: %s",
             entry, toString(), fieldValue, result)
         );
      }

      return result;
   }

   protected boolean compare(String fieldValue)
   {
      int result;

      if (insensitive)
      {
         result = fieldValue.compareToIgnoreCase(value);
      }
      else
      {
         result = fieldValue.compareTo(value);
      }

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
      String str;

      if (quoted)
      {
         str = String.format("%s %s \"%s\"", fieldValueElem, relation, value);
      }
      else
      {
         str = String.format("%s %s {%s}", fieldValueElem, relation, value);
      }

      return insensitive ? str+"i" : str;
   }

   protected String value;
   protected FieldValueElement fieldValueElem;
   protected Relational relation;
   protected boolean quoted, insensitive;
}
