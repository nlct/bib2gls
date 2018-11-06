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

public class GlsUseField extends ControlSequence
{
   public GlsUseField(Bib2Gls bib2gls)
   {
      this("glsxtrusefield", CASE_NO_CHANGE, bib2gls);
   }

   public GlsUseField(String name, Bib2Gls bib2gls)
   {
      this(name, CASE_NO_CHANGE, bib2gls);
   }

   public GlsUseField(String name, int caseChange, Bib2Gls bib2gls)
   {
      super(name);

      switch (caseChange)
      {
         case CASE_NO_CHANGE:
         case CASE_SENTENCE:
         case CASE_TO_UPPER:
         case CASE_TITLE_CASE:
            this.caseChange = caseChange;
         break;
         default:
           throw new IllegalArgumentException("Invalid case change "+caseChange);
      }

      this.bib2gls = bib2gls;
   }

   public Object clone()
   {
      return new GlsUseField(getName(), getCaseChange(), bib2gls);
   }

   public int getCaseChange()
   {
      return caseChange;
   }

   protected void process(TeXParser parser, TeXObjectList stack,
      String entryLabel, String fieldLabel)
      throws IOException
   {
      // Try the current resource set first

      GlsResource currentResource = bib2gls.getCurrentResource();

      Bib2GlsEntry entry = currentResource.getEntry(entryLabel);

      if (entry == null)
      {
         if (bib2gls.getDebugLevel() > 0)
         {
            bib2gls.debug(String.format("\\%s -> %s", getName(),
              bib2gls.getMessage("warning.unknown_entry_in_current_resource", 
                   entryLabel, currentResource)));
         }

         // Try other resource sets, if there are any

         for (GlsResource resource : bib2gls.getResources())
         {
            if (resource != currentResource)
            {
               entry = resource.getEntry(entryLabel);

               if (entry != null)
               {
                  if (bib2gls.getDebugLevel() > 0)
                  {
                     bib2gls.debug(String.format("\\%s -> %s", getName(),
                       bib2gls.getMessage("message.found_entry_in_resource", 
                            entryLabel, resource)));
                  }

                  break;
               }
            }
         }

         if (entry == null)
         {
            bib2gls.warning(String.format("\\%s -> %s", getName(),
              bib2gls.getMessage("warning.unknown_entry", entryLabel)));
            return;
         }
      }

      process(parser, stack, entry, fieldLabel, caseChange);
   }

   protected void process(TeXParser parser, TeXObjectList stack,
      Bib2GlsEntry entry, String fieldLabel, int caseChange)
      throws IOException
   {
      BibValueList val = entry.getField(fieldLabel);

      if (val == null)
      {
         val = entry.getFallbackContents(fieldLabel);
      }

      if (val == null)
      {
         bib2gls.warning(String.format("\\%s{%s} -> %s", getName(),
           entry.getId(),
           bib2gls.getMessage("message.field.not.set", fieldLabel)));
         return;
      }

      TeXObjectList obj = ((BibValueList)val.clone()).expand(parser);

      switch (caseChange)
      {
         case CASE_SENTENCE:
           bib2gls.getCurrentResource().toSentenceCase(obj,
             parser.getListener());
         break;
         case CASE_TO_UPPER:
           bib2gls.getCurrentResource().toUpperCase(obj,
             parser.getListener());
         break;
         case CASE_TITLE_CASE:
           bib2gls.getCurrentResource().toTitleCase(obj,
             parser.getListener());
         break;
      }

      stack.push(obj);
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

      arg = parser.popNextArg();

      if (arg instanceof Expandable)
      {
         TeXObjectList expanded = ((Expandable)arg).expandfully(parser);

         if (expanded != null)
         {
            arg = expanded;
         }
      }

      String fieldLabel = arg.toString(parser);

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

      arg = stack.popArg(parser);

      if (arg instanceof Expandable)
      {
         TeXObjectList expanded = ((Expandable)arg).expandfully(parser, stack);

         if (expanded != null)
         {
            arg = expanded;
         }
      }

      String fieldLabel = arg.toString(parser);

      process(parser, stack, entryLabel, fieldLabel);
   }

   protected Bib2Gls bib2gls;

   public static final int CASE_NO_CHANGE=0;
   public static final int CASE_SENTENCE=1;
   public static final int CASE_TO_UPPER=2;
   public static final int CASE_TITLE_CASE=3;

   private int caseChange = CASE_NO_CHANGE;
}
