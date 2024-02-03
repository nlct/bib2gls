/*
    Copyright (C) 2021-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.bib2gls;

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public abstract class EntryFieldCommand extends Command
{
   public EntryFieldCommand(String name, Bib2Gls bib2gls)
   {
      super(name);
      this.bib2gls = bib2gls;
   }

   protected Bib2GlsEntry fetchEntry(String entryLabel)
      throws IOException
   {
      // Try the current resource set first

      GlsResource currentResource = bib2gls.getCurrentResource();

      Bib2GlsEntry entry = currentResource.getEntry(entryLabel);

      if (entry == null)
      {
         if (bib2gls.isDebuggingOn())
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
                  if (bib2gls.isDebuggingOn())
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

   protected BibValueList getFieldBibList(TeXParser parser,
      Bib2GlsEntry entry, String fieldLabel)
      throws IOException
   {
      String key = fieldLabel;
      BibValueList val = entry.getField(key);

      if (val == null)
      {
         key = bib2gls.getFieldKey(fieldLabel);

         if (!key.equals(fieldLabel))
         {
            val = entry.getField(key);
         }
      }

      if (val == null)
      {
         val = entry.getFallbackContents(key);
      }

      if (val == null && warnIfFieldMissing)
      {
         bib2gls.warning(String.format("\\%s{%s} -> %s", getName(),
           entry.getId(),
           bib2gls.getMessage("message.field.not.set", key)));
      }

      return val;
   }

   protected TeXObjectList getFieldValue(TeXParser parser,
      Bib2GlsEntry entry, String fieldLabel)
      throws IOException
   {
      BibValueList val = getFieldBibList(parser, entry, fieldLabel);

      if (val == null) return null;

      return ((BibValueList)val.clone()).expand(parser);
   }

   protected Bib2Gls bib2gls;
   protected boolean warnIfFieldMissing = true;
}
