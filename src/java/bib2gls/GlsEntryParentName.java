/*
    Copyright (C) 2018-2021 Nicola L.C. Talbot
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

public class GlsEntryParentName extends GlsUseField
{
   public GlsEntryParentName(Bib2Gls bib2gls)
   {
      this("glsxtrentryparentname", CaseChange.NONE, bib2gls);
   }

   public GlsEntryParentName(String name, Bib2Gls bib2gls)
   {
      this(name, CaseChange.NONE, bib2gls);
   }

   public GlsEntryParentName(String name, CaseChange caseChange, Bib2Gls bib2gls)
   {
      super(name, caseChange, bib2gls);
   }

   public Object clone()
   {
      return new GlsEntryParentName(getName(), getCaseChange(), 
         bib2gls);
   }

   public TeXObjectList expandonce(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      TeXObject arg;

      if (parser == stack)
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

         if (parser == stack)
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
         if (bib2gls.getDebugLevel() > 0)
         {
            bib2gls.debug(String.format("\\%s: %s", getName(),
             bib2gls.getMessage("warning.unknown_entry", 
             entryLabel)));
         }
      }
      else
      {
         String parentLabel = entry.getFieldValue("parent");

         if (parentLabel == null)
         {
            if (bib2gls.getDebugLevel() > 0)
            {
               bib2gls.debug(String.format("\\%s: %s", 
                 getName(),
                 bib2gls.getMessage("warning.cant.find.parent.name", 
                 entryLabel)));
            }
         }
         else
         {
            process(parser, parentLabel, "name", expanded);
         }
      }

      return expanded;
   }

}
