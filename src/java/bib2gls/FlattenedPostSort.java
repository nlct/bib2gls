/*
    Copyright (C) 2018-2021 Nicola L.C. Talbot
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

public class FlattenedPostSort extends ControlSequence
{
   public FlattenedPostSort()
   {
      this("bibglsflattenedchildpostsort");
   }

   public FlattenedPostSort(String name)
   {
      super(name);
   }

   public Object clone()
   {
      return new FlattenedPostSort(getName());
   }

   public void process(TeXParser parser) throws IOException
   {
      TeXObject arg1 = parser.popNextArg();
      TeXObject arg2 = parser.popNextArg();

      arg1.process(parser);
      parser.getListener().getWriteable().write(", ");
      arg2.process(parser);
   }

   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      TeXObject arg1 = stack.popArg(parser);
      TeXObject arg2 = stack.popArg(parser);

      arg1.process(parser, stack);
      parser.getListener().getWriteable().write(", ");
      arg2.process(parser, stack);
   }
}
