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

public class GlsEntryFieldValue extends GlsUseField
{
   public GlsEntryFieldValue(String name, String fieldLabel, Bib2Gls bib2gls)
   {
      this(name, fieldLabel, CASE_NO_CHANGE, bib2gls);
   }

   public GlsEntryFieldValue(String name, String fieldLabel,
     int caseChange, Bib2Gls bib2gls)
   {
      super(name, caseChange, bib2gls);
      this.fieldLabel = fieldLabel;
   }

   public Object clone()
   {
      return new GlsEntryFieldValue(getName(), fieldLabel, getCaseChange(), 
         bib2gls);
   }

   public String getFieldLabel()
   {
      return fieldLabel;
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

      process(parser, parser, entryLabel, fieldLabel);
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

      process(parser, stack, entryLabel, fieldLabel);
   }

   private String fieldLabel;
}
