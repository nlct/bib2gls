/*
    Copyright (C) 2021-2024 Nicola L.C. Talbot
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

import com.dickimawbooks.texparserlib.*;

public class GlsCombinedSep extends Command
{
   public GlsCombinedSep()
   {
      this("glscombinedsep");
   }

   public GlsCombinedSep(String name)
   {
      super(name);
   }

   public Object clone()
   {
      return new GlsCombinedSep(getName());
   }

   public TeXObjectList expandonce(TeXParser parser, TeXObjectList stack)
     throws IOException
   {
      TeXObject arg1, arg2;

      if (parser == stack || stack == null)
      {
         arg1 = parser.popNextArg();
         arg2 = parser.popNextArg();
      }
      else
      {
         arg1 = stack.popArg(parser);
         arg2 = stack.popArg(parser);
      }

      TeXObjectList expanded = new TeXObjectList();
      expanded.add(parser.getListener().getSpace());

      return expanded;
   }
}
