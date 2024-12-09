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
package com.dickimawbooks.bibgls.bib2gls;

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.html.L2HBinarySymbol;

public class BibGlsBinarySymbol extends L2HBinarySymbol
{
   public BibGlsBinarySymbol(String name, int codePoint, Bib2Gls bib2gls)
   {
      super(name, codePoint);
      this.bib2gls = bib2gls;
   }

   @Override
   public Object clone()
   {
      return new BibGlsBinarySymbol(getName(), getCharCode(), bib2gls);
   }

   protected String getReplacementText()
   {
      GlsResource resource = bib2gls.getCurrentResource();
      String text = null;

      if (resource != null && resource.isWordifyMathSymbolOn())
      {
         text = resource.getLocalisationTextIfExists("mathsymbol", getName());

         if (text == null)
         {
            text = getName();
         }
      }

      return text;
   }

   @Override
   public void process(TeXParser parser, TeXObjectList stack)
   throws IOException
   {
      String text = getReplacementText();

      if (text == null)
      {
         super.process(parser, stack);
      }
      else
      {
         parser.getListener().getWriteable().write(text);
      }
   }

   @Override
   public void process(TeXParser parser)
   throws IOException
   {
      String text = getReplacementText();

      if (text == null)
      {
         super.process(parser);
      }
      else
      {
         parser.getListener().getWriteable().write(text);
      }
   }

   Bib2Gls bib2gls;
}
