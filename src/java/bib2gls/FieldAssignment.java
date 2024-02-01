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

import com.dickimawbooks.bibglscommon.Bib2GlsException;

/**
 * Used to store each field assignment specification (obtained from
 * the assign-fields option).
 */
public class FieldAssignment
{
   public FieldAssignment(String destField, FieldValueList valueList,
       Conditional cond, Boolean override)
   {
      if (destField == null)
      {
         throw new NullPointerException();
      }

      this.destField = destField;
      this.fieldEvaluation = new FieldEvaluation(valueList, cond);
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
      return fieldEvaluation.getValue(entry);
   }

   public String getStringValue(Bib2GlsEntry entry)
   throws Bib2GlsException,IOException
   {
      return fieldEvaluation.getStringValue(entry);
   }

   @Override
   public String toString()
   {
      if (override == null)
      {
         return String.format("%s = %s", destField, fieldEvaluation);
      }
      else if (override)
      {
         return String.format("%s =[o] %s", destField, fieldEvaluation);
      }
      else
      {
         return String.format("%s =[n] %s", destField, fieldEvaluation);
      }
   }

   private String destField;
   private FieldEvaluation fieldEvaluation;
   private Boolean override;
}
