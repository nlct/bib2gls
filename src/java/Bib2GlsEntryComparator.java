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

public class Bib2GlsEntryComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    String sort, String sortField,
    int strength, int decomposition)
   {
      this.sortField = sortField;
      this.bib2gls = bib2gls;
      this.entries = entries;

      if (sort.equals("locale"))
      {
         collator = Collator.getInstance();
      }
      else
      {
         collator = Collator.getInstance(Locale.forLanguageTag(sort));
      }

      collator.setStrength(strength);
      collator.setDecomposition(decomposition);
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
              id, sortField));
         }
         else if (bib2gls.useInterpreter() && list != null
                   && value.matches(".*[\\\\\\$].*"))
         {
            value = bib2gls.interpret(value, list);
         }
      }

      entry.putField("sort", value);

      String grp = null;

      byte[] bits = null;

      CollationKey key = collator.getCollationKey(value);
      entry.setCollationKey(key);

      if (bib2gls.useGroupField() && value.length() > 0)
      {
         bits = key.toByteArray();

         int codePoint = value.codePointAt(0);

         String str = String.format("%c", codePoint);

         byte bit1 = (bits.length > 0 ? bits[0] : 0);
         byte bit2 = (bits.length > 1 ? bits[1] : 0);

         if (Character.isAlphabetic(codePoint))
         {
            Character c = (bit1 == 0 ? getGroup(bit2) : null);

            if (c == null)
            {
               grp = str.toUpperCase();
            }
            else
            {
               grp = c.toString();
            }

            entry.putField("group", 
               String.format("\\bibglslettergroup{%s}{%d}{%d}{%s}{%d}", 
                             grp, bit1, bit2, str, codePoint));
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
         StringBuilder keyList = new StringBuilder();

         if (bits == null)
         {
            bits = key.toByteArray();
         }

         for (byte b : bits)
         {
            if (keyList.length() > 0)
            {
               keyList.append(' ');
            }

            keyList.append(b);
         }

         if (grp == null)
         {
            bib2gls.verbose(String.format("%s -> '%s' [%s]", 
              id, value, keyList));
         }
         else
         {
            bib2gls.verbose(String.format("%s -> '%s' (%s) [%s]", 
              id, value, grp, keyList));
         }
      }

      return value;
   }

   public int compare(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      if (bib2gls.getCurrentResource().flattenSort())
      {
         return entry1.getCollationKey().compareTo(entry2.getCollationKey());
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

         int result = e1.getCollationKey().compareTo(e2.getCollationKey());

         if (result != 0)
         {
            return result;
         }
      }

      return (n1 == n2 ? 0 : (n1 < n2 ? -1 : 1));
   }

   public Collator getCollator()
   {
      return collator;
   }

   public void sortEntries() throws Bib2GlsException
   {
      bib2gls.debug(bib2gls.getMessage("message.setting.sort",
        collator.getStrength(), collator.getDecomposition()));

      if (bib2gls.useGroupField())
      {
         groupMap = new HashMap<Byte,Character>();

         for (char c = 'A'; c <= 'Z'; c++)
         {
            CollationKey key = collator.getCollationKey(""+c);

            byte[] bits = key.toByteArray();

            if (bits.length >= 2 && bits[0] == 0)
            {
               groupMap.put(bits[1], c);
            }
         }
      }

      for (Bib2GlsEntry entry : entries)
      {
         entry.updateHierarchy(entries);
         updateSortValue(entry, entries);
      }

      entries.sort(this);
   }

   private Character getGroup(byte bit)
   {
      if (groupMap == null) return null;

      return groupMap.get(bit);
   }

   private String sortField;

   private Collator collator;

   private Bib2Gls bib2gls;

   private HashMap<Byte,Character> groupMap = null;

   private Vector<Bib2GlsEntry> entries;
}
