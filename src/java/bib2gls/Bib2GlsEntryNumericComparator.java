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
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

import com.dickimawbooks.texparserlib.bib.BibValueList;

public class Bib2GlsEntryNumericComparator extends SortComparator
{
   public Bib2GlsEntryNumericComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries, SortSettings settings,
    String sortField, String groupField, String entryType)
   {
      super(bib2gls, entries, settings, sortField, groupField,
        entryType);

      sortMethod = settings.getMethod();
      sort = settings.getUnderlyingMethod();

      Locale locale = settings.getNumberLocale();

      if (sort.equals("numeric"))
      {
         if (locale == null)
         {
            numberFormat = NumberFormat.getNumberInstance();
         }
         else
         {
            numberFormat = NumberFormat.getNumberInstance(locale);
         }
      }
      else if (sort.equals("currency"))
      {
         if (locale == null)
         {
            numberFormat = NumberFormat.getCurrencyInstance();
         }
         else
         {
            numberFormat = NumberFormat.getCurrencyInstance(locale);
         }
      }
      else if (sort.equals("percent"))
      {
         if (locale == null)
         {
            numberFormat = NumberFormat.getPercentInstance();
         }
         else
         {
            numberFormat = NumberFormat.getPercentInstance(locale);
         }
      }
      else if (sort.equals("numberformat"))
      {
         DecimalFormatSymbols syms;

         if (locale == null)
         {
            syms = DecimalFormatSymbols.getInstance();
         }
         else
         {
            syms = DecimalFormatSymbols.getInstance(locale);
         }

         numberFormat = new DecimalFormat(settings.getNumberFormat(), syms);
      }
   }

   protected boolean useSortSuffix()
   {
      return false;
   }

   protected String adjustSort(Bib2GlsEntry entry, String value)
   {
      String id = entry.getId();

      Number number = null;

      try
      {
         if (numberFormat != null)
         {
            number = numberFormat.parse(value);
         }
         else if (sort.equals("integer"))
         {
            number = Integer.valueOf(value);
         }
         else if (sort.equals("float"))
         {
            number = Float.valueOf(value);
         }
         else if (sort.equals("double"))
         {
            number = Double.valueOf(value);
         }
         else if (sort.equals("hex"))
         {
            number = Integer.valueOf(value, 16);
         }
         else if (sort.equals("octal"))
         {
            number = Integer.valueOf(value, 8);
         }
         else if (sort.equals("binary"))
         {
            number = Integer.valueOf(value, 2);
         }
         else
         {
            throw new IllegalArgumentException(
              "Unrecognised numeric sort option: "+sortMethod);
         }
      }
      catch (NumberFormatException | ParseException e)
      {
         bib2gls.warning(bib2gls.getMessage("warning.cant.parse.sort",
             value, id));

         value = "0";
         number = Integer.valueOf(0);
      }

      entry.setNumericSort(number);

      return value;
   }

   protected long getDefaultGroupId(Bib2GlsEntry entry, int codePoint, 
     Object sortValue)
   {
      return ((Number)sortValue).longValue();
   }

   protected GroupTitle createDefaultGroupTitle(int codePoint, Object sortValue,
      String type)
   {
      return new NumberGroupTitle((Number)sortValue, type);
   }

   protected String updateSortValue(Bib2GlsEntry entry, 
      Vector<Bib2GlsEntry> entries)
   {
      String value = super.updateSortValue(entry, entries);

      String id = entry.getId();
      Number number = entry.getNumericSort();

      GlsResource resource = bib2gls.getCurrentResource();

      String type = getType(entry);

      if (bib2gls.useGroupField() && entry.getFieldValue(groupField) == null
           && !entry.hasParent())
      {
         setGroupTitle(entry, -1, number, value, type);
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
         return ((Integer)num1).compareTo((Integer)num2);
      }

      if (num1 instanceof Float && num2 instanceof Float)
      {
         return ((Float)num1).compareTo((Float)num2);
      }

      if (num1 instanceof Double && num2 instanceof Double)
      {
         return ((Double)num1).compareTo((Double)num2);
      }

      if (num1 instanceof Long && num2 instanceof Long)
      {
         return ((Long)num1).compareTo((Long)num2);
      }

      double n1;
      double n2;

      n1 = num1.doubleValue();
      n2 = num2.doubleValue();

      if (n1 == n2) return 0;

      return n1 < n2 ? -1 : 1;
   }

   protected int compareElements(Bib2GlsEntry entry1,
     Bib2GlsEntry entry2)
   {
      Number val1 = entry1.getNumericSort();
      Number val2 = entry2.getNumericSort();

      int result = compare(val1, val2);

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.logMessage(String.format("[%s] %s <=> [%s] %s = %d", 
           entry1.getId(), val1, entry2.getId(), val2, result));
      }

      return result;
   }

   private String sort, sortMethod;

   private NumberFormat numberFormat;
}
