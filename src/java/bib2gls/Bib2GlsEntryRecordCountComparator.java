/*
    Copyright (C) 2020-2024 Nicola L.C. Talbot
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

import java.util.Vector;
import java.util.Iterator;
import java.util.Set;

import com.dickimawbooks.texparserlib.bib.BibValueList;
import com.dickimawbooks.bibgls.common.Bib2GlsException;

public class Bib2GlsEntryRecordCountComparator extends Bib2GlsEntryNumericComparator
{
   public Bib2GlsEntryRecordCountComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries, SortSettings settings,
    String groupField, String entryType, boolean overrideType)
   throws Bib2GlsException
   {
      // No actual sort field used. There isn't a 'record'
      // field in Bib2GlsEntry's set of fields.
      super(bib2gls, entries, settings, "", groupField, entryType, overrideType);

      if (!bib2gls.isRecordCountSet())
      {
         throw new Bib2GlsException(bib2gls.getMessage("error.sort.requires.switch",
           settings.getMethod(), "--record-count"));
      }
   }

   protected String adjustSort(Bib2GlsEntry entry, String value)
   {
      String id = entry.getId();

      int total = 0;

      Set<GlsRecord> keys = bib2gls.getRecordCountKeySet();

      if (keys != null)
      {
         for (Iterator<GlsRecord> it = keys.iterator(); it.hasNext(); )
         {
            GlsRecord rec = it.next();

            if (rec.getLabel().equals(id))
            {
               Integer count = bib2gls.getRecordCount(rec);
               total += count;
            }
         }
      }

      entry.setNumericSort(Integer.valueOf(total));

      return ""+total;
   }

}
