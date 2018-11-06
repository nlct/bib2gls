/*
    Copyright (C) 2017 Nicola L.C. Talbot
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

public class GlsHierName extends GlsUseField
{
   public GlsHierName(Bib2Gls bib2gls)
   {
      this("glsxtrhiername", CASE_NO_CHANGE, false, bib2gls);
   }

   public GlsHierName(String name, Bib2Gls bib2gls)
   {
      this(name, CASE_NO_CHANGE, false, bib2gls);
   }

   public GlsHierName(String name, int caseChange, boolean topLevelChange, 
      Bib2Gls bib2gls)
   {
      super(name, caseChange, bib2gls);
      this.topLevelChange = topLevelChange;
   }

   public Object clone()
   {
      return new GlsHierName(getName(), getCaseChange(), topLevelChange, 
        bib2gls);
   }

   protected void process(TeXParser parser, TeXObjectList stack,
     String entryLabel)
      throws IOException
   {
      Bib2GlsEntry entry = bib2gls.getCurrentResource().getEntry(entryLabel);

      if (entry == null) return;

      String parentLabel = entry.getParent();

      if (parentLabel != null)
      {
         process(parser, stack, parentLabel);

         ControlSequence cs = parser.getListener().getControlSequence(
          "glsxtrhiernamesep");

         cs.process(parser);
      }

      int change = CASE_NO_CHANGE;

      if (parentLabel == null || !topLevelChange)
      {
         change = getCaseChange();
      }

      BibValueList val = entry.getField("short");

      String fieldLabel = (val == null ? "name" : "short");

      process(parser, stack, entry, fieldLabel, change);
   }

   public void process(TeXParser parser)
      throws IOException
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

      String entryLabel = arg.toString(parser);

      process(parser, parser, entryLabel);
   }

   public void process(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      TeXObject arg = stack.popArg(parser);

      if (arg instanceof Expandable)
      {
         TeXObjectList expanded = ((Expandable)arg).expandfully(parser, stack);

         if (expanded != null)
         {
            arg = expanded;
         }
      }

      String entryLabel = arg.toString(parser);

      process(parser, stack, entryLabel);
   }

   private boolean topLevelChange;
}
