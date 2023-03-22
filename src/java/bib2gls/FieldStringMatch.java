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
 * Compares the value of a field with a string value. If the field is missing, 
 * it will be treated as empty.
 */
public class FieldStringMatch implements Conditional
{
   public FieldStringMatch(Field field, Relational relation, String value,
     boolean quoted, boolean insensitive)
   {
      this.field = field;
      this.relation = relation;
      this.value = value;
      this.quoted = quoted;
      this.insensitive = insensitive;
   }

   public boolean booleanValue(Bib2GlsEntry entry)
   {
      String fieldValue = null;

      try
      {
         fieldValue = field.getStringValue(entry);
      }
      catch (IOException e)
      {
         entry.getBib2Gls().debug(e);
      }

      if (fieldValue == null)
      {
         fieldValue = "";
      }

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
         str = String.format("%s %s \"%s\"", field, relation, value);
      }
      else
      {
         str = String.format("%s %s {%s}", field, relation, value);
      }

      return insensitive ? str+"i" : str;
   }

   protected String value;
   protected Field field;
   protected Relational relation;
   protected boolean quoted, insensitive;
}
