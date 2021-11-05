/*
    Copyright (C) 2017-2021 Nicola L.C. Talbot
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
import com.dickimawbooks.texparserlib.bib.*;

public class GlsHyperLink extends ControlSequence
{
   public GlsHyperLink(Bib2Gls bib2gls)
   {
      this("glshyperlink", bib2gls);
   }

   public GlsHyperLink(String name, Bib2Gls bib2gls)
   {
      super(name);
      this.bib2gls = bib2gls;
   }

   public Object clone()
   {
      return new GlsHyperLink(getName(), bib2gls);
   }

   public void process(TeXParser parser)
      throws IOException
   {
      TeXObject obj = parser.popNextArg('[', ']');

      TeXObject label = parser.popNextArg();

      if (obj == null)
      {
         Bib2GlsEntry entry = bib2gls.getCurrentResource().getEntry(
          label.toString(parser));

         if (entry == null) return;

         BibValueList val = entry.getField("name");

         if (val == null)
         {
            val = entry.getFallbackContents("name");
         }

         if (val == null) return;

         obj = ((BibValueList)val.clone()).expand(parser);
      }

      parser.push(obj);
   }

   public void process(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      TeXObject obj = stack.popArg(parser, '[', ']');

      TeXObject label = stack.popArg(parser);

      if (obj == null)
      {
         Bib2GlsEntry entry = bib2gls.getCurrentResource().getEntry(
          label.toString(parser));

         if (entry == null) return;

         BibValueList val = entry.getField("name");

         if (val == null)
         {
            val = entry.getFallbackContents("name");
         }

         if (val == null) return;

         obj = ((BibValueList)val.clone()).expand(parser);
      }

      stack.push(obj);
   }

   private Bib2Gls bib2gls;
}
