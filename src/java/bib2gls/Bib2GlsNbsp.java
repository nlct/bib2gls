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
import com.dickimawbooks.texparserlib.generic.Nbsp;

public class Bib2GlsNbsp extends Nbsp
{
   public Bib2GlsNbsp(boolean useNonBreakSpace)
   {
      super();
      this.useNonBreakSpace = useNonBreakSpace;
   }

   public Bib2GlsNbsp(int charCode, boolean useNonBreakSpace)
   {
      super(charCode);
      this.useNonBreakSpace = useNonBreakSpace;
   }

   public Object clone()
   {
      return new Bib2GlsNbsp(getCharCode(), useNonBreakSpace);
   }

   public TeXObjectList expandonce(TeXParser parser)
     throws IOException
   {
      TeXObjectList list = new TeXObjectList();

      if (useNonBreakSpace)
      {
         list.add(parser.getListener().getOther(0x00A0));
      }
      else
      {
         list.add(parser.getListener().getSpace());
      }

      return list;
   }

   public void process(TeXParser parser)
     throws IOException
   {
      if (useNonBreakSpace)
      {
         parser.getListener().getWriteable().writeCodePoint(0x00A0);
      }
      else
      {
         parser.getListener().getWriteable().writeCodePoint(0x0020);
      }
   }

   private boolean useNonBreakSpace;
}
