/*
    Copyright (C) 2021-2022 Nicola L.C. Talbot
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

public class GlsXtrIfFieldEqStr extends EntryFieldCommand
{
   public GlsXtrIfFieldEqStr(Bib2Gls bib2gls)
   {
      this("GlsXtrIfFieldEqStr", false, false, bib2gls);
   }

   public GlsXtrIfFieldEqStr(String name, 
     boolean expandField, boolean expandText, Bib2Gls bib2gls)
   {
      this(name, expandField, expandText, true, bib2gls);
   }

   public GlsXtrIfFieldEqStr(String name, 
     boolean expandField, boolean expandText, boolean defaultScope,
     Bib2Gls bib2gls)
   {
      super(name, bib2gls);

      this.expandField = expandField;
      this.expandText = expandText;
   }

   public Object clone()
   {
      return new GlsXtrIfFieldEqStr(getName(), expandField, expandText,
        defaultScope, bib2gls);
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

      TeXObject text;

      if (parser == stack)
      {
         text = parser.popNextArg();
      }
      else
      {
         text = stack.popArg(parser);
      }

      if (expandText && (text instanceof Expandable))
      {
         TeXObjectList expanded;

         if (parser == stack)
         {
            expanded = ((Expandable)text).expandfully(parser);
         }
         else
         {
            expanded = ((Expandable)text).expandfully(parser, stack);
         }

         if (expanded != null)
         {
            text = expanded;
         }
      }

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

      process(parser, entryLabel, fieldLabel, text, truePart, falsePart, pending);

      return expanded;
   }

   protected void process(TeXParser parser,
      String entryLabel, String fieldLabel, TeXObject text,
      TeXObject truePart, TeXObject falsePart, TeXObjectList pending)
      throws IOException
   {
      TeXObjectList fieldValue = null;

      Bib2GlsEntry entry = fetchEntry(entryLabel);

      if (entry != null)
      {
         fieldValue = getFieldValue(parser, entry, fieldLabel);
      }

      if (fieldValue == null || fieldValue.isEmpty())
      {
         pending.add(falsePart);
      }
      else
      {
         if (expandField)
         {
            TeXObjectList expanded = fieldValue.expandfully(parser);

            if (expanded != null)
            {
               fieldValue = expanded;
            }
         }

         if (fieldValue.equals(text))
         {
            // true part

            if (defaultScope)
            {// \ifglsfieldeq doesn't define this command

               pending.add(new TeXCsRef("def"));
               pending.add(new TeXCsRef("glscurrentfieldvalue"));
               Group grp = parser.getListener().createGroup();
               grp.add(fieldValue);
               pending.add(grp);
            }

            pending.add(truePart);
         }
         else
         {
            pending.add(falsePart);
         }
      }
   }

   protected boolean expandField, expandText, defaultScope;
}
