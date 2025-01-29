/*
    Copyright (C) 2025 Nicola L.C. Talbot
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

public enum AuxInputAction
{
  FOLLOW, SKIP_AFTER_BIBGLSAUX, SKIP;

  public static String getValidList()
  {
     return "'follow', 'skip-after-bibglsaux' (or 'skip after bibglsaux'), 'skip'";
  }

  public static AuxInputAction valueOfArg(String str)
   throws IllegalArgumentException
  {
     if (str == null) throw new NullPointerException();

     if (str.equals("follow"))
     {
        return FOLLOW;
     }
     else if (str.equals("skip-after-bibglsaux") || str.equals("skip after bibglsaux"))
     {
        return SKIP_AFTER_BIBGLSAUX;
     }
     else if (str.equals("skip"))
     {
        return SKIP;
     }

     return valueOf(str);
  }
}
