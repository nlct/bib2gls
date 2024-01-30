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
package com.dickimawbooks.bib2gls;

import java.io.IOException;

import com.dickimawbooks.texparserlib.bib.BibValue;
import com.dickimawbooks.texparserlib.bib.BibValueList;

/**
 * Used to store a field evaluation. This is a combination of an
 * assignment list and conditional, without a destination.
 */
public class FieldEvaluation
{
   public FieldEvaluation(FieldValueList valueList, Conditional cond)
   {
      if (valueList == null)
      {
         throw new NullPointerException();
      }

      this.valueList = valueList;
      this.condition = cond;
   }

   public BibValue getValue(Bib2GlsEntry entry)
     throws Bib2GlsException,IOException
   {
      entry.getResource().setLastMatch(null);

      if (condition != null && !condition.booleanValue(entry))
      {
         return null;
      }

      return valueList.getValue(entry);
   }

   public String getStringValue(Bib2GlsEntry entry)
   throws Bib2GlsException,IOException
   {
      if (condition != null && !condition.booleanValue(entry))
      {
         return null;
      }

      return valueList.getStringValue(entry);
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < valueList.size(); i++)
      {
         if (i > 0)
         {
            builder.append(" + ");
         }

         builder.append(valueList.get(i));
      }

      if (condition != null)
      {
         builder.append(" [ ");
         builder.append(condition);
         builder.append(" ] ");
      }

      return builder.toString();
   }

   private FieldValueList valueList;
   private Conditional condition;
}
