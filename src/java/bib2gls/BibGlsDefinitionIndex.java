/*
    Copyright (C) 2021-2023 Nicola L.C. Talbot
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

public class BibGlsDefinitionIndex extends GlsUseField
{
   public BibGlsDefinitionIndex(Bib2Gls bib2gls)
   {
      this("bibglsdefinitionindex", bib2gls);
   }

   public BibGlsDefinitionIndex(String name, Bib2Gls bib2gls)
   {
      super(name, bib2gls);
   }

   public Object clone()
   {
      return new BibGlsDefinitionIndex(getName(), bib2gls);
   }

   public String getFieldLabel()
   {
      return bib2gls.getCurrentResource().getDefinitionIndexField();
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

      TeXObjectList expanded = new TeXObjectList();

      String fieldLabel = getFieldLabel();

      if (fieldLabel != null)
      {
         process(parser, entryLabel, fieldLabel, expanded);
      }

      return expanded;
   }
}
