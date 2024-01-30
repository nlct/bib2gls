/*
    Copyright (C) 2018-2024 Nicola L.C. Talbot
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
      this(name, fieldLabel, CaseChange.NO_CHANGE, bib2gls);
   }

   public GlsEntryFieldValue(String name, String fieldLabel,
     CaseChange caseChange, Bib2Gls bib2gls)
   {
      this(name, fieldLabel, caseChange, bib2gls, "");
   }

   public GlsEntryFieldValue(String name, String fieldLabel,
     CaseChange caseChange, Bib2Gls bib2gls, String prefix)
   {
      super(name, caseChange, bib2gls);
      this.fieldLabel = fieldLabel;
      this.prefix = prefix;
   }

   public Object clone()
   {
      return new GlsEntryFieldValue(getName(), fieldLabel, getCaseChange(), 
         bib2gls, prefix);
   }

   public String getFieldLabel()
   {
      return fieldLabel;
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

      String entryLabel = prefix+arg.toString(parser);

      TeXObjectList expanded = new TeXObjectList();

      process(parser, entryLabel, fieldLabel, expanded);

      return expanded;
   }

   private String fieldLabel, prefix="";
}
