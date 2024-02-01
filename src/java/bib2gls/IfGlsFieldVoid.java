/*
    Copyright (C) 2022-2024 Nicola L.C. Talbot
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

public class IfGlsFieldVoid extends EntryFieldCommand
{
   public IfGlsFieldVoid(Bib2Gls bib2gls)
   {
      this("ifglsfieldvoid", bib2gls);
   }

   public IfGlsFieldVoid(String name, Bib2Gls bib2gls)
   {
      super(name, bib2gls);

      this.warnIfFieldMissing = false;
   }

   public Object clone()
   {
      return new IfGlsFieldVoid(getName(), bib2gls);
   }

   public TeXObjectList expandonce(TeXParser parser)
      throws IOException
   {
      return expandonce(parser, parser);
   }

   public TeXObjectList expandonce(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      String fieldLabel = popLabelString(parser, stack);
      String entryLabel = popLabelString(parser, stack);

      TeXObject truePart = popArg(parser, stack);

      TeXObject falsePart = popArg(parser, stack);

      TeXObjectList expanded = new TeXObjectList();
      TeXObjectList pending = expanded;

      process(parser, entryLabel, fieldLabel, truePart, falsePart, pending);

      return expanded;
   }

   protected void process(TeXParser parser,
      String entryLabel, String fieldLabel,
      TeXObject truePart, TeXObject falsePart, TeXObjectList pending)
      throws IOException
   {
      TeXObjectList obj = null;

      Bib2GlsEntry entry = fetchEntry(entryLabel);

      if (entry != null)
      {
         obj = getFieldValue(parser, entry, fieldLabel);
      }

      if (obj == null || obj.isEmpty())
      {
         // false part

         pending.add(falsePart);
      }
      else
      {
         // true part

         pending.add(truePart);
      }
   }
}
