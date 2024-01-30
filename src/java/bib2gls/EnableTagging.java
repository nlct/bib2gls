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
package com.dickimawbooks.bib2gls;

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.AtFirstOfOne;

public class EnableTagging extends ControlSequence
{
   public EnableTagging()
   {
      this("GlsXtrEnableInitialTagging");
   }

   public EnableTagging(String name)
   {
      super(name);
   }

   public Object clone()
   {
      return new EnableTagging(getName());
   }

   public void process(TeXParser parser) throws IOException
   {
      TeXObject arg1 = parser.popNextArg();
      TeXObject arg2 = parser.popNextArg();

      // Ignore first argument. Just need the second one.

      if (!(arg2 instanceof ControlSequence))
      {
         throw new TeXSyntaxException(parser,
            TeXSyntaxException.ERROR_CS_EXPECTED,
            arg2.toString(parser));
      }

      parser.putControlSequence(
        new AtFirstOfOne(((ControlSequence)arg2).getName()));
   }

   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      TeXObject arg1 = stack.popArg(parser);
      TeXObject arg2 = stack.popArg(parser);

      if (!(arg2 instanceof ControlSequence))
      {
         throw new TeXSyntaxException(parser,
            TeXSyntaxException.ERROR_CS_EXPECTED,
            arg2.toString(parser));
      }

      parser.putControlSequence(
        new AtFirstOfOne(((ControlSequence)arg2).getName()));
   }
}
