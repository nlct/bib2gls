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

public class WidestName
{
   public WidestName()
   {
      this(null, "", 0.0);
   }

   public WidestName(String theLabel, String theName, double theWidth)
   {
      label = theLabel;
      name = theName;
      width = theWidth;
   }

   public String getLabel()
   {
      return label;
   }

   public String getName()
   {
      return name;
   }

   public double getWidth()
   {
      return width;
   }

   public void update(String newLabel, String newName, double newWidth)
   {
      if (newWidth > width)
      {
         label = newLabel;
         name = newName;
         width = newWidth;
      }
   }

   private String label, name;
   private double width;
}
