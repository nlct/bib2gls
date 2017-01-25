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

import java.util.Locale;
import java.util.Vector;
import java.util.Comparator;
import java.text.Collator;
import java.text.CollationKey;
import java.text.Normalizer;
import java.util.HashMap;

public class Bib2GlsEntryLetterComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryLetterComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    String sort, String sortField,
    boolean ignoreCase)
   {
      this.sortField = sortField;
      this.bib2gls = bib2gls;
      this.entries = entries;
      this.ignoreCase = ignoreCase;
   }

   private String updateSortValue(Bib2GlsEntry entry, 
      Vector<Bib2GlsEntry> entries)
   {
      String id = entry.getId();

      String value = null;

      if (sortField.equals("id"))
      {
         value = id;
      }
      else
      {
         value = entry.getFieldValue(sortField);

         if (value == null)
         {
            value = entry.getFallbackValue(sortField);

            if (value == null)
            {
               value = id;

               bib2gls.debug(bib2gls.getMessage("warning.no.default.sort",
                 id));
            }
         }
      }

      entry.putField("sort", 
         ignoreCase ? value.toLowerCase() : value);

      String grp = null;

      if (bib2gls.useGroupField() && value.length() > 0)
      {
         int codePoint = value.codePointAt(0);

         String str = String.format("%c", codePoint);

         if (Character.isAlphabetic(codePoint))
         {
            grp = ignoreCase ?  str.toUpperCase() : str;

            entry.putField("group", 
               String.format("\\bibglslettergroup{%s}{%d}{%d}{%s}{%d}", 
                             grp, 0, 0, str, codePoint));
         }
         else
         {
            entry.putField("group", 
               String.format("\\bibglsothergroup{%s}{%d}", 
                             str, codePoint));
         }
      }

      if (bib2gls.getDebugLevel() > 0)
      {
         if (grp == null)
         {
            bib2gls.debug(String.format("%s -> '%s'", id, value));
         }
         else
         {
            bib2gls.debug(String.format("%s -> '%s' (%s)", 
              id, value, grp));
         }
      }

      return value;
   }

   public int compare(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      if (bib2gls.getCurrentResource().flattenSort())
      {
         return entry1.getFieldValue("sort").compareTo(
            entry2.getFieldValue("sort"));
      }

      int n1 = entry1.getHierarchyCount();
      int n2 = entry2.getHierarchyCount();

      int n = Integer.min(n1, n2);

      if (n1 == n2 && entry1.getId().equals(entry2.getId()))
      {
         return 0;
      }

      for (int i = 0; i < n; i++)
      {
         Bib2GlsEntry e1 = entry1.getHierarchyElement(i);
         Bib2GlsEntry e2 = entry2.getHierarchyElement(i);

         int result = e1.getFieldValue("sort").compareTo(
            e2.getFieldValue("sort"));

         if (result != 0)
         {
            return result;
         }
      }

      return (n1 == n2 ? 0 : (n1 < n2 ? -1 : 1));
   }

   public void sortEntries() throws Bib2GlsException
   {
      for (Bib2GlsEntry entry : entries)
      {
         entry.updateHierarchy(entries);
         updateSortValue(entry, entries);
      }

      entries.sort(this);
   }

   private String sortField;

   private Bib2Gls bib2gls;

   private Vector<Bib2GlsEntry> entries;

   private boolean ignoreCase;
}
