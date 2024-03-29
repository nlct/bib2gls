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
package com.dickimawbooks.bibgls.gls2bib;

import java.util.Iterator;
import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;

public class GlsSetExpandField extends ControlSequence
{
   public GlsSetExpandField(Gls2Bib gls2bib)
   {
      this("glssetexpandfield", true, gls2bib);
   }

   public GlsSetExpandField(String name, boolean on, Gls2Bib gls2bib)
   {
      super(name);

      this.gls2bib = gls2bib;
      this.on = on;
   }

   public Object clone()
   {
      return new GlsSetExpandField(getName(), on, gls2bib);
   }

   public void process(TeXParser parser) throws IOException
   {
      TeXObject arg = parser.popNextArg();

      if (arg instanceof Expandable)
      {
         TeXObjectList expanded = ((Expandable)arg).expandfully(parser);

         if (expanded != null)
         {
            arg = expanded;
         }
      }

      gls2bib.setFieldExpansion(arg.toString(parser), on);
   }

   public void process(TeXParser parser, TeXObjectList list) throws IOException
   {
      TeXObject arg = list.popArg(parser);

      if (arg instanceof Expandable)
      {
         TeXObjectList expanded = ((Expandable)arg).expandfully(parser, list);

         if (expanded != null)
         {
            arg = expanded;
         }
      }

      gls2bib.setFieldExpansion(arg.toString(parser), on);
   }

   protected Gls2Bib gls2bib;
   private boolean on;
}
