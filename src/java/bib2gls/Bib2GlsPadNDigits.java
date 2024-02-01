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

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;

public class Bib2GlsPadNDigits extends Command
{
   public Bib2GlsPadNDigits()
   {
      this("bibglspaddigits");
   }

   public Bib2GlsPadNDigits(String name)
   {
      super(name);
   }

   @Override
   public Object clone()
   {
      return new Bib2GlsPadNDigits(getName());
   }

   @Override
   public TeXObjectList expandonce(TeXParser parser, TeXObjectList stack)
   throws IOException
   {
      int numDigits = TeXParserUtils.popInt(parser, stack);
      int val = TeXParserUtils.popInt(parser, stack);

      return parser.getListener().createString(
       String.format("%0"+numDigits+"d", val));
   }

   @Override
   public TeXObjectList expandonce(TeXParser parser)
   throws IOException
   {
      return expandonce(parser, parser);
   }

   @Override
   public TeXObjectList expandfully(TeXParser parser, TeXObjectList stack)
   throws IOException
   {
      return expandonce(parser, stack);
   }

   @Override
   public TeXObjectList expandfully(TeXParser parser)
   throws IOException
   {
      return expandonce(parser);
   }

   @Override
   public void process(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      int numDigits = TeXParserUtils.popInt(parser, stack);
      int val = TeXParserUtils.popInt(parser, stack);

      parser.getListener().getWriteable().write(
       String.format("%0"+numDigits+"d", val));
   }

   @Override
   public void process(TeXParser parser)
      throws IOException
   {
      process(parser, parser);
   }
}
