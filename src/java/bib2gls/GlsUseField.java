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
import com.dickimawbooks.texparserlib.bib.*;

public class GlsUseField extends EntryFieldCommand
{
   public GlsUseField(Bib2Gls bib2gls)
   {
      this("glsxtrusefield", CaseChange.NONE, bib2gls);
   }

   public GlsUseField(String name, Bib2Gls bib2gls)
   {
      this(name, CaseChange.NONE, bib2gls);
   }

   public GlsUseField(String name, CaseChange caseChange, Bib2Gls bib2gls)
   {
      super(name, bib2gls);
      this.caseChange = caseChange;
   }

   public Object clone()
   {
      return new GlsUseField(getName(), getCaseChange(), bib2gls);
   }

   public CaseChange getCaseChange()
   {
      return caseChange;
   }

   public TeXObjectList expandonce(TeXParser parser)
      throws IOException
   {
      return expandonce(parser, parser);
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

      String fieldLabel = arg.toString(parser);

      TeXObjectList expanded = new TeXObjectList();

      process(parser, entryLabel, fieldLabel, expanded);

      return expanded;
   }

   protected void process(TeXParser parser,
      String entryLabel, String fieldLabel, TeXObjectList pending)
      throws IOException
   {
      Bib2GlsEntry entry = fetchEntry(entryLabel);

      if (entry == null) return;

      process(parser, entry, fieldLabel, caseChange, pending);
   }

   protected void process(TeXParser parser,
      Bib2GlsEntry entry, String fieldLabel, CaseChange caseChange,
      TeXObjectList pending)
      throws IOException
   {
      TeXObjectList obj = getFieldValue(parser, entry, fieldLabel);

      if (obj == null) return;

      switch (caseChange)
      {
         case SENTENCE:
           bib2gls.getCurrentResource().toSentenceCase(obj,
             parser.getListener());
         break;
         case TO_UPPER:
           bib2gls.getCurrentResource().toUpperCase(obj,
             parser.getListener());
         break;
         case TITLE:
           bib2gls.getCurrentResource().toTitleCase(obj,
             parser.getListener());
         break;
      }

      pending.add(obj);
   }

   private CaseChange caseChange = CaseChange.NONE;
}
