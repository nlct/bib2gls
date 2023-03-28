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
 * Compares the value of a field with a numeric value. Note that if
 * the provided numerical value is an Integer the field value will
 * be rounded to an integer, otherwise it will be assumed to be a
 * double. If the field is missing, empty or has a non-numeric value
 * it will be treated as 0.
 */

public class FieldNumberMatch implements Conditional
{
   public FieldNumberMatch(Field field, Relational relation, Number value)
   {
      this.field = field;
      this.relation = relation;
      this.value = value;
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

      boolean result;

      if (value instanceof Integer)
      {
         int num1 = 0;
         int num2 = value.intValue();

         if (fieldValue == null)
         {
            try
            {
               num1 = Integer.parseInt(fieldValue);
            }
            catch (NumberFormatException e)
            {
               try
               {
                  num1 = (int)Math.round(Double.parseDouble(fieldValue));
               }
               catch (NumberFormatException e2)
               {// treat as 0
               }
            }
         }

         result = compare(num1, num2);
      }
      else
      {
         double num1 = 0.0;
         double num2 = value.doubleValue();

         if (fieldValue == null)
         {
            try
            {
               num1 = Double.parseDouble(fieldValue);
            }
            catch (NumberFormatException e)
            {
               try
               {
                  num1 = Double.parseDouble(fieldValue);
               }
               catch (NumberFormatException e2)
               {// treat as 0
               }
            }
         }

         result = compare(num1, num2);
      }

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

   protected boolean compare(int num1, int num2)
   {
      switch (relation)
      {
         case EQUALS: return num1 == num2;
         case NOT_EQUALS: return num1 != num2;
         case LT: return num1 < num2;
         case LE: return num1 <= num2;
         case GT: return num1 > num2;
         case GE: return num1 >= num2;
      }

      throw new AssertionError("Missing Relational enum " + relation);
   }

   protected boolean compare(double num1, double num2)
   {
      switch (relation)
      {
         case EQUALS: return num1 == num2;
         case NOT_EQUALS: return num1 != num2;
         case LT: return num1 < num2;
         case LE: return num1 <= num2;
         case GT: return num1 > num2;
         case GE: return num1 >= num2;
      }

      throw new AssertionError("Missing Relational enum " + relation);
   }

   @Override
   public String toString()
   {
      return String.format("%s %s %s", field, relation, value);
   }

   protected Number value;
   protected Field field;
   private Relational relation;
}
