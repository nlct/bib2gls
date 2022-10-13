/*
    Copyright (C) 2017-2022 Nicola L.C. Talbot
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
import java.util.Vector;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsContributor extends Bib2GlsEntry
{
   public Bib2GlsContributor(Bib2Gls bib2gls)
   {
      this(bib2gls, "contributor");
   }

   public Bib2GlsContributor(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);

      titleList = new Vector<Bib2GlsEntry>();
   }

   public void checkRequiredFields()
   {
      if (getField("name") == null)
      {
         missingFieldWarning("name");
      }
   }

   public Vector<Bib2GlsEntry> getTitles()
   {
      return titleList;
   }

   public void addTitle(Bib2GlsEntry title)
   {
      titleList.add(title);
   }

   public void writeInternalFields(PrintWriter writer)
   throws IOException
   {
      super.writeInternalFields(writer);

      for (Bib2GlsEntry title : titleList)
      {
         if (title.isSelected())
         {
            writer.println(String.format(
              "\\glsxtrfieldlistadd{%s}{bibtexentry}{%s}",
                 getId(), title.getId()));
            writer.println(String.format(
              "\\glsxtrfieldlistadd{%s}{bibtex%s}{%s}",
                 getId(), title.getOriginalEntryType(), title.getId()));
         }
      }
   }

   private Vector<Bib2GlsEntry> titleList;
}
