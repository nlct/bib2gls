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

import com.dickimawbooks.texparserlib.bib.BibValueList;

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

         BibValueList list = entry.getField(sortField);

         if (value == null)
         {
            value = entry.getFallbackValue(sortField);
            list = entry.getFallbackContents(sortField);
         }

         if (value == null)
         {
            value = id;

            bib2gls.debug(bib2gls.getMessage("warning.no.default.sort",
              id));
         }
         else if (bib2gls.useInterpreter() && list != null 
                   && value.matches(".*[\\\\\\$].*"))
         {  
            value = bib2gls.interpret(value, list);
         }
      }

      if (ignoreCase)
      {
         value = value.toLowerCase();
      }

      entry.putField("sort", value);

      String grp = null;

      if (bib2gls.useGroupField() && value.length() > 0)
      {
         int codePoint = value.codePointAt(0);

         String str = String.format("%c", codePoint);

         if (Character.isAlphabetic(codePoint))
         {
            grp = (ignoreCase ? str.toUpperCase() : str);

            entry.putField("group", 
               String.format("\\bibglslettergroup{%s}{%d}{%d}{%s}{%d}", 
                             grp, 0, 0, str, codePoint));
         }
         else
         {
            if (str.equals("\\"))
            {
               str = "\\char`\\\\";
            }

            entry.putField("group", 
               String.format("\\bibglsothergroup{%s}{%d}", 
                             str, codePoint));
         }
      }

      if (bib2gls.getVerboseLevel() > 0)
      {
         StringBuilder builder = new StringBuilder();

         for (int i = 0, n = value.length(); i < n; )
         {
            int codepoint = value.codePointAt(i);
            i += Character.charCount(codepoint);

            if (builder.length() == 0)
            {
               builder.append(codepoint);
            }
            else
            {
               builder.append(' ');
               builder.append(codepoint);
            }
         }

         if (grp == null)
         {
            bib2gls.verbose(String.format("%s -> '%s' [%s]", id, value,
             builder));
         }
         else
         {
            bib2gls.verbose(String.format("%s -> '%s' (%s) [%s]", 
              id, value, grp, builder));
         }
      }

      return value;
   }

   protected int compare(String str1, String str2)
   {
      int n1 = str1.length();
      int n2 = str2.length();

      int i = 0;
      int j = 0;

      while (i < n1 && j < n2)
      {
         int cp1 = str1.codePointAt(i);
         int cp2 = str2.codePointAt(j);

         i += Character.charCount(cp1);
         j += Character.charCount(cp2);

         if (cp1 < cp2)
         {
            return -1;
         }
         else
         {
            return 1;
         }
      }

      if (n1 < n2)
      {
         return -1;
      }
      else if (n2 > n1)
      {
         return 1;
      }

      return 0;
   }

   public int compare(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      if (bib2gls.getCurrentResource().flattenSort())
      {
         return compare(entry1.getFieldValue("sort"), 
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

         int result = compare(e1.getFieldValue("sort"), 
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
