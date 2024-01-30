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
package com.dickimawbooks.bib2gls;

import java.io.*;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class AtCompoundSet extends BibEntry
{
   public AtCompoundSet(GlsResource resource)
   {
      this(resource, "compoundset");
   }

   public AtCompoundSet(GlsResource resource, String entryType)
   {
      super(entryType);
      this.resource = resource;
   }

   public void parseContents(TeXParser parser,
    TeXObjectList contents, TeXObject endGroupChar)
     throws IOException
   {
      super.parseContents(parser, contents, endGroupChar);

      BibValueList bibvallist;
      TeXObjectList list;
      String mainLabel = null;

      bibvallist = getField("main");

      if (bibvallist != null)
      {
         list = bibvallist.expand(parser);
         mainLabel = list.toString(parser);
      }

      bibvallist = getField("elements");

      if (bibvallist == null)
      {
         throw new BibTeXSyntaxException(parser,
           BibTeXSyntaxException.ERROR_EXPECTING, "elements");
      }

      list = bibvallist.expand(parser);

      String elementList = list.toString(parser);

      bibvallist = getField("options");

      String options = null;

      if (bibvallist != null)
      {
         list = bibvallist.expand(parser);

         options = list.toString(parser);
      }

      try
      {
         compoundEntry = new CompoundEntry(getId(), elementList);

         if (mainLabel != null && !mainLabel.isEmpty())
         {
            compoundEntry.setMainLabel(mainLabel);
         }

         if (options != null)
         {
            compoundEntry.setOptions(options);
         }

         resource.addCompoundEntry(compoundEntry);
      }
      catch (Bib2GlsException e)
      {
         throw new BibTeXSyntaxException(e, parser,
           "error.syntax", e.getMessage());
      }
   }

   public CompoundEntry getCompoundEntry()
   {
      return compoundEntry;
   }

   private CompoundEntry compoundEntry;
   private GlsResource resource;
}
