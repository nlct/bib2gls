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

public class FieldNullMatch implements Conditional
{
   public FieldNullMatch(Field field, boolean equals)
   {
      this.field = field;
      this.equals = equals;
   }

   public boolean booleanValue(Bib2GlsEntry entry) throws IOException
   {
      BibValue value = field.getValue(entry);

      if (value == null)
      {
         return equals ? true : false;
      }
      else
      {
         return equals ? false : true;
      }
   }

   @Override
   public String toString()
   {
      if (equals)
      {
         return String.format("%s = NULL", field);
      }
      else
      {
         return String.format("%s <> NULL", field);
      }
   }

   protected boolean equals;
   protected Field field;
}
