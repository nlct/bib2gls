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

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class BibGlsDateTime extends ControlSequence
  implements Expandable
{
   public BibGlsDateTime()
   {
      this("bibglsdatetime", true, true);
   }

   public BibGlsDateTime(String name, boolean hasDate, boolean hasTime)
   {
      super(name);

      if (!hasDate && !hasTime)
      {
         throw new IllegalArgumentException(
           "BibGlsDateTime can't have both date=false and time=false");
      }

      this.hasDate = hasDate;
      this.hasTime = hasTime;
   }

   public Object clone()
   {
      return new BibGlsDateTime(getName(), hasDate, hasTime);
   }

   public TeXObjectList expandonce(TeXParser parser)
    throws IOException
   {
      return expandonce(parser, parser);
   }

   public TeXObjectList expandonce(TeXParser parser, TeXObjectList stack)
    throws IOException
   {
      int n = (hasDate && hasTime ? 13 : 7);

      TeXObject object = null;

      for (int i = 0; i < n; i++)
      {
         if (stack == parser)
         {
            object = parser.popNextArg();
         }
         else
         {
            object = stack.popArg(parser);
         }
      }

      TeXObjectList expanded = new TeXObjectList();
      expanded.add(object);

      return expanded;
   }

   public TeXObjectList expandfully(TeXParser parser)
    throws IOException
   {
      TeXObjectList expanded = expandonce(parser);

      return expanded.expandfully(parser);
   }

   public TeXObjectList expandfully(TeXParser parser, TeXObjectList stack)
    throws IOException
   {
      TeXObjectList expanded = expandonce(parser, stack);

      return expanded.expandfully(parser, stack);
   }

   public void process(TeXParser parser)
      throws IOException
   {
      expandonce(parser).process(parser);
   }

   public void process(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      expandonce(parser, stack).process(parser, stack);
   }

   private boolean hasDate, hasTime;
}
