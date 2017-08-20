/*
    Copyright (C) 2017 Nicola L.C. Talbot
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
import java.util.Vector;

import com.dickimawbooks.texparserlib.TeXObject;

public class GlsSeeRecord
{
   public GlsSeeRecord(String label, TeXObject value)
   {
      if (label == null || value == null)
      {
         throw new NullPointerException();
      }

      this.label = label;
      this.value = value;
   }

   public String getLabel()
   {
      return label;
   }

   public TeXObject getValue()
   {
      return value;
   }

   private String label;
   private TeXObject value;
}
