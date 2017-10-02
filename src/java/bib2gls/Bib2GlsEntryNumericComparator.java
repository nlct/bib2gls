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

public class Bib2GlsEntryNumericComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryNumericComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    String sort, String sortField, String groupField)
   {
      this.sortField = sortField;
      this.groupField = groupField;
      this.bib2gls = bib2gls;
      this.entries = entries;
      this.reverse = sort.endsWith("-reverse");
      this.sort = sort;
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
                   && value.matches(".*[\\\\\\$\\{\\}].*"))
         {  
            value = bib2gls.interpret(value, list);
         }
      }

      Number number = null;

      try
      {
         if (sort.equals("integer") || sort.equals("integer-reverse"))
         {
            number = Integer.valueOf(value);
         }
         else if (sort.equals("float") || sort.equals("float-reverse"))
         {
            number = Float.valueOf(value);
         }
         else if (sort.equals("double") || sort.equals("double-reverse"))
         {
            number = Double.valueOf(value);
         }
         else if (sort.equals("hex") || sort.equals("hex-reverse"))
         {
            number = Integer.valueOf(value, 16);
         }
         else if (sort.equals("octal") || sort.equals("octal-reverse"))
         {
            number = Integer.valueOf(value, 8);
         }
         else if (sort.equals("binary") || sort.equals("binary-reverse"))
         {
            number = Integer.valueOf(value, 2);
         }
         else
         {
            throw new IllegalArgumentException(
              "Unrecognised numeric sort option: "+sort);
         }
      }
      catch (NumberFormatException e)
      {
         bib2gls.warning(bib2gls.getMessage("warning.cant.parse.sort",
             value, id));

         value = "0";
         number = Integer.valueOf(0);
      }

      entry.setNumericSort(number);

      entry.putField("sort", value);

      if (bib2gls.useGroupField() && entry.getFieldValue(groupField) == null
           && !entry.hasParent())
      {
         GlsResource resource = bib2gls.getCurrentResource();

         GroupTitle grpTitle = resource.getGroupTitle(entry, number.intValue());
         String args;

         if (grpTitle == null)
         {
            String entryType = resource.getType(entry);
            grpTitle = new NumberGroupTitle(number, entryType);
            resource.putGroupTitle(grpTitle, entry);
            args = grpTitle.toString();
         }
         else
         {
            args = grpTitle.format(value);
         }

         entry.putField(groupField,
            String.format("\\%s%s", grpTitle.getCsLabelName(), args));
      }

      if (bib2gls.getVerboseLevel() > 0)
      {
         bib2gls.verbose(String.format("%s -> '%s'", id, value));
      }

      return value;
   }

   protected int compare(Number num1, Number num2)
   {
      if (num1 instanceof Integer && num2 instanceof Integer)
      {
         return reverse ? ((Integer)num2).compareTo((Integer)num1) 
          : ((Integer)num1).compareTo((Integer)num2);
      }

      if (num1 instanceof Float && num2 instanceof Float)
      {
         return reverse ? ((Float)num2).compareTo((Float)num1) 
          : ((Float)num1).compareTo((Float)num2);
      }

      if (num1 instanceof Double && num2 instanceof Double)
      {
         return reverse ? ((Double)num2).compareTo((Double)num1) 
          : ((Double)num1).compareTo((Double)num2);
      }

      if (num1 instanceof Long && num2 instanceof Long)
      {
         return reverse ? ((Long)num2).compareTo((Long)num1) 
          : ((Long)num1).compareTo((Long)num2);
      }

      double n1;
      double n2;

      if (reverse)
      {
         n2 = num1.doubleValue();
         n1 = num2.doubleValue();
      }
      else
      {
         n1 = num1.doubleValue();
         n2 = num2.doubleValue();
      }

      if (n1 == n2) return 0;

      return n1 < n2 ? -1 : 1;
   }

   public int compare(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      if (bib2gls.getCurrentResource().flattenSort())
      {
         return compare(entry1.getNumericSort(), 
            entry2.getNumericSort());
      }

      if (entry1.getId().equals(entry2.getParent()))
      {
         // entry1 is the parent of entry2
         // so entry1 must come before (be less than) entry2
         // (even with a reverse sort)

         return -1;
      }

      if (entry2.getId().equals(entry1.getParent()))
      {
         // entry2 is the parent of entry1
         // so entry1 must come after (be greater than) entry2
         // (even with a reverse sort)

         return 1;
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

         int result = compare(e1.getNumericSort(), e2.getNumericSort());

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

   private String sortField, groupField, sort;

   private Bib2Gls bib2gls;

   private Vector<Bib2GlsEntry> entries;

   private boolean reverse;
}
