/*
    Copyright (C) 2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.datatool2bib;

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;

public class DataGidxAtInvert extends Command
{
   public DataGidxAtInvert()
   {
      this("datagidx@invert");
   }

   public DataGidxAtInvert(String name)
   {
      super(name);
   }

   public Object clone()
   {
      return new DataGidxAtInvert(getName());
   }

   public TeXObjectList expandonce(TeXParser parser,
      TeXObjectList stack)
     throws IOException
   {
      TeXObject arg1 = popArg(parser, stack);
      TeXObject arg2 = popArg(parser, stack);

      TeXObjectList expanded = parser.getListener().createStack();

      expanded.add(arg2, true);
      expanded.add(parser.getListener().getOther(','));
      expanded.add(parser.getListener().getSpace());
      expanded.add(arg1, true);

      return expanded;
   }
}
