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

import com.dickimawbooks.texparserlib.bib.BibValue;
import com.dickimawbooks.texparserlib.bib.BibValueList;

/**
 * Used to store each field assignment specification (obtained from
 * the assign-fields option).
 */
public class FieldAssignment
{
   public FieldAssignment(String destField, FieldValueList valueList, Conditional cond)
   {
      if (destField == null || valueList == null)
      {
         throw new NullPointerException();
      }

      this.destField = destField;
      this.valueList = valueList;
      this.condition = cond;
   }

   public String getDestinationField()
   {
      return destField;
   }

   public BibValueList getValue(Bib2GlsEntry entry)
   {
      if (condition != null && !condition.booleanValue(entry))
      {
         return null;
      }

      BibValueList list = new BibValueList();

      for (FieldValueElement elem : valueList)
      {
         BibValue elemVal = elem.getValue(entry);

         if (elemVal == null)
         {
            return null;
         }

         list.add(elemVal);
      }

      return list;
   }

   public String getStringValue(Bib2GlsEntry entry)
   throws IOException
   {
      StringBuilder builder = new StringBuilder();

      for (FieldValueElement elem : valueList)
      {
         String elemVal = elem.getStringValue(entry);

         if (elemVal == null)
         {
            return null;
         }

         builder.append(elemVal);
      }

      return builder.toString();
   }

   @Override
   public String toString()
   {
      if (condition == null)
      {
         return String.format("%s = %s", destField, valueList);
      }
      else
      {
         return String.format("%s = %s [ %s ]", destField, valueList, condition);
      }
   }

   private String destField;
   private FieldValueList valueList;
   private Conditional condition;
}
