/*
    Copyright (C) 2018-2022 Nicola L.C. Talbot
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
      this("glsxtrhiername", CaseChange.NO_CHANGE, false, bib2gls);
   }

   public GlsHierName(String name, Bib2Gls bib2gls)
   {
      this(name, CaseChange.NO_CHANGE, false, bib2gls);
   }

   public GlsHierName(String name, CaseChange caseChange, boolean topLevelOnlyChange, 
      Bib2Gls bib2gls)
   {
      super(name, caseChange, bib2gls);
      this.topLevelOnlyChange = topLevelOnlyChange;
   }

   public Object clone()
   {
      return new GlsHierName(getName(), getCaseChange(), topLevelOnlyChange, 
        bib2gls);
   }

   public TeXObjectList expandonce(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      TeXObject arg;

      if (stack == parser)
      {
         arg = parser.popNextArg();
      }
      else
      {
         arg = stack.popArg(parser);
      }

      if (arg instanceof Expandable)
      {
         TeXObjectList expanded;

         if (stack == parser)
         {
            expanded = ((Expandable)arg).expandfully(parser);
         }
         else
         {
            expanded = ((Expandable)arg).expandfully(parser, stack);
         }

         if (expanded != null)
         {
            arg = expanded;
         }
      }

      String entryLabel = arg.toString(parser);

      Bib2GlsEntry entry = fetchEntry(entryLabel);

      TeXObjectList expanded = new TeXObjectList();

      if (entry == null)
      {
         return expanded;
      }

      TeXParserListener listener = parser.getListener();

      String parentLabel = entry.getParent();

      if (parentLabel != null)
      {
         expanded.add(this);
         expanded.add(listener.createGroup(parentLabel));
         expanded.add(new TeXCsRef("glsxtrhiernamesep"));
      }

      BibValueList val = entry.getField("short");

      String fieldLabel = (val == null ? "name" : "short");

      if (parentLabel == null || !topLevelOnlyChange)
      {
         process(parser, entry, fieldLabel, getCaseChange(), expanded);
      }
      else
      {
         process(parser, entry, fieldLabel, CaseChange.NO_CHANGE, expanded);
      }

      return expanded;
   }

   private boolean topLevelOnlyChange;
}
