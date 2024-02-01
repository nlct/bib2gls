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

import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.ControlSequence;

public enum FieldCaseChange
{
   NO_CHANGE("NOCHANGE"), UC("UC"), LC("LC"), FIRST_UC("FIRSTUC"), FIRST_LC("FIRSTLC"), TITLE("TITLE");

   FieldCaseChange(String tag)
   {
      this.tag = tag;
   }

   public String toString()
   {
      return tag;
   }

   public static boolean isFieldCaseChange(TeXObject object)
   {
      if (object instanceof ControlSequence)
      {
         return isFieldCaseChange(((ControlSequence)object).getName());
      }

      return false;
   }

   public static boolean isFieldCaseChange(String name)
   {
      return name.equals("LC") || name.equals("UC")
          || name.equals("FIRSTLC") || name.equals("FIRSTUC")
          || name.equals("TITLE") || name.equals("NOCHANGE");
   }

   public static FieldCaseChange getFieldCaseChange(TeXObject object)
   {
      if (object instanceof ControlSequence)
      {
         return getFieldCaseChange(((ControlSequence)object).getName());
      }
      else
      {
         throw new IllegalArgumentException(
           "Unknown field case change identifier "
          + object.format());
      }
   }

   public static FieldCaseChange getFieldCaseChange(String name)
   {
      if (name.equals("LC"))
      {
         return LC;
      }
      else if (name.equals("UC"))
      {
         return UC;
      }
      else if (name.equals("FIRSTLC"))
      {
         return FIRST_LC;
      }
      else if (name.equals("FIRSTUC"))
      {
         return FIRST_UC;
      }
      else if (name.equals("TITLE"))
      {
         return TITLE;
      }
      else if (name.equals("NOCHANGE"))
      {
         return NO_CHANGE;
      }

      throw new IllegalArgumentException("Unknown field case change identifier "
       + name);
   }

   protected final String tag;
}
