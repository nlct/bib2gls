/*
    Copyright (C) 2021 Nicola L.C. Talbot
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

public class GlsXtrIfHasField extends Command
{
   public GlsXtrIfHasField(Bib2Gls bib2gls)
   {
      this("glsxtrifhasfield", true, bib2gls);
   }

   public GlsXtrIfHasField(String name, boolean defaultScope, Bib2Gls bib2gls)
   {
      super(name);

      this.bib2gls = bib2gls;
      this.defaultScope = defaultScope;
   }

   public Object clone()
   {
      return new GlsXtrIfHasField(getName(), defaultScope, bib2gls);
   }

   protected Bib2GlsEntry fetchEntry(String entryLabel)
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
         }
      }

      return entry;
   }

   public TeXObjectList expandonce(TeXParser parser)
      throws IOException
   {
      return expandonce(parser, parser);
   }

   public TeXObjectList expandonce(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      boolean addScope = defaultScope;

      TeXObject object = stack.peekStack();

      if (object instanceof CharObject)
      {
         if (((CharObject)object).getCharCode() == (int)'*')
         {
            addScope = false;
            stack.popStack(parser);
         }
      }

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

      String fieldLabel = arg.toString(parser);

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

      TeXObject truePart;

      if (parser == stack)
      {
         truePart = parser.popNextArg();
      }
      else
      {
         truePart = stack.popArg(parser);
      }

      TeXObject falsePart;

      if (parser == stack)
      {
         falsePart = parser.popNextArg();
      }
      else
      {
         falsePart = stack.popArg(parser);
      }

      TeXObjectList expanded = new TeXObjectList();
      TeXObjectList pending = expanded;

      if (addScope)
      {
         Group grp = parser.getListener().createGroup();
         expanded.add(grp);
         pending = grp;
      }

      process(parser, entryLabel, fieldLabel, truePart, falsePart, pending);

      return expanded;
   }

   protected void process(TeXParser parser,
      String entryLabel, String fieldLabel,
      TeXObject truePart, TeXObject falsePart, TeXObjectList pending)
      throws IOException
   {
      Bib2GlsEntry entry = fetchEntry(entryLabel);

      if (entry == null) return;

      BibValueList val = entry.getField(fieldLabel);

      if (val == null)
      {
         val = entry.getFallbackContents(fieldLabel);
      }

      if (val == null || val.isEmpty())
      {
         // false part

         pending.add(falsePart);
      }
      else
      {
         // true part

         TeXObjectList obj = ((BibValueList)val.clone()).expand(parser);
         pending.add(new TeXCsRef("def"));
         pending.add(new TeXCsRef("glscurrentfieldvalue"));
         Group grp = parser.getListener().createGroup();
         grp.add(obj);
         pending.add(grp);

         pending.add(truePart);
      }
   }

   protected Bib2Gls bib2gls;
   protected boolean defaultScope;
}
