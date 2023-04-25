/*
    Copyright (C) 2017-2023 Nicola L.C. Talbot
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

public class NumberGroupTitle extends OtherGroupTitle
{
   public NumberGroupTitle(Bib2Gls bib2gls, Number number, String type, String parent)
   {
      super(bib2gls, number.toString(), number.longValue(), type, parent);
   }

   @Override
   protected String getNonHierCsSetName()
   {
      return "bibglssetnumbergrouptitle";
   }

   @Override
   protected String getNonHierCsLabelName()
   {
      return "bibglsnumbergroup";
   }

   @Override
   public String format(String other)
   {
      if (supportsHierarchy)
      {
         return String.format("{%s}{%d}{%s}{%d}", other, getId(),
          type == null ? "" : type, parent == null ? "" : parent, level);
      }
      else
      {
         return String.format("{%s}{%d}{%s}", other, getId(),
          type == null ? "" : type);
      }
   }
}
