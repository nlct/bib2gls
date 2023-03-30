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
   public FieldAssignment(String destField, FieldValueList valueList,
       Conditional cond, Boolean override)
   {
      if (destField == null || valueList == null)
      {
         throw new NullPointerException();
      }

      this.destField = destField;
      this.valueList = valueList;
      this.condition = cond;
      this.override = override;
   }

   public String getDestinationField()
   {
      return destField;
   }

   public boolean isFieldOverrideOn(GlsResource resource)
   {
      if (override == null)
      {
         return resource.isAssignOverrideOn();
      }
      else
      {
         return override.booleanValue();
      }
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

      builder.append(destField);
      builder.append(" = ");

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

   private String destField;
   private FieldValueList valueList;
   private Conditional condition;
   private Boolean override;
}
