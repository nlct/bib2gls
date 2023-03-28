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
 * Tests if the value of a field is a substring of the value of another field.
 * If the first field is empty or missing, evaluates as false.
 */
public class FieldSuffixOfField implements Conditional
{
   public FieldSuffixOfField(Field field1, Field field2)
   {
      this(field1, field2, false);
   }

   public FieldSuffixOfField(Field field1, Field field2, boolean negate)
   {
      this.field1 = field1;
      this.field2 = field2;
      this.negate = negate;
   }

   public boolean booleanValue(Bib2GlsEntry entry)
   {
      boolean result;

      String fieldValue1 = null;
      String fieldValue2 = null;

      try
      {
         fieldValue1 = field1.getStringValue(entry);
      }
      catch (IOException e)
      {
         entry.getBib2Gls().debug(e);
      }

      if (fieldValue1 == null || fieldValue1.isEmpty())
      {
         result = false;
      }
      else
      {
         try
         {
            fieldValue2 = field2.getStringValue(entry);
         }
         catch (IOException e)
         {
            entry.getBib2Gls().debug(e);
         }

         if (fieldValue2 == null)
         {
            fieldValue2 = "";
         }

         result = fieldValue2.endsWith(fieldValue1);
      }

      return negate ? !result : result;
   }

   @Override
   public String toString()
   {
      return String.format("%s \\%s %s", 
        field1, negate ? "NOTSUFFIXOF" : "SUFFIXOF", field2);
   }

   protected Field field1, field2;
   protected boolean negate;
}
