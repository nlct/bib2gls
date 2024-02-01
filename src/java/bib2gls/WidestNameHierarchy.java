/*
    Copyright (C) 2017-2024 Nicola L.C. Talbot
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

import java.util.HashMap;
import java.util.Iterator;

public class WidestNameHierarchy extends HashMap<Integer,WidestName>
{
   public WidestNameHierarchy()
   {
     super();
   }

   public Iterator<Integer> getKeyIterator()
   {
      return keySet().iterator();
   }

   public void update(Bib2GlsEntry entry, String name, double width)
   {
      int level = entry.getHierarchyCount();

      if (level > 0) level--;

      Integer key = Integer.valueOf(level);

      WidestName widestName = get(key);

      if (widestName == null)
      {
         put(key, new WidestName(entry.getId(), name, width));
      }
      else
      {
         widestName.update(entry.getId(), name, width);
      }
   }
}
