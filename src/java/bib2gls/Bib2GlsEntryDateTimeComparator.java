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
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.dickimawbooks.texparserlib.bib.BibValueList;

public class Bib2GlsEntryDateTimeComparator extends SortComparator
{
   public Bib2GlsEntryDateTimeComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries, SortSettings settings,
    String sortField, String groupField, String entryType)
   {
      super(bib2gls, entries, settings, sortField, groupField,
        entryType);

      String format = settings.getDateFormat();
      Locale locale = settings.getDateLocale();

      if (settings.isDateTimeSort())
      {
         hasDate = true;
         hasTime = true;

         sortDateFormat = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ssZ");
      }
      else if (settings.isDateSort())
      {
         hasDate = true;
         hasTime = false;

         sortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

         calendar = (locale == null ? Calendar.getInstance() :
                    Calendar.getInstance(locale));
      }
      else
      {
         hasDate = false;
         hasTime = true;

         sortDateFormat = new SimpleDateFormat("HH:mm:ssZ");
      }

      dateFormat = SortSettings.getDateFormat(locale, format, hasDate, hasTime);
   }

   protected boolean useSortSuffix()
   {
      return false;
   }

   protected String adjustSort(Bib2GlsEntry entry, String value)
   {
      String id = entry.getId();

      Date dateValue;

      try
      {
         dateValue = dateFormat.parse(value);
      }
      catch (ParseException excp)
      {
         dateValue = new Date();

         if (dateFormat instanceof SimpleDateFormat)
         {
            bib2gls.warningMessage(
                "warning.cant.parse.pattern.sort",
                value, id, ((SimpleDateFormat)dateFormat).toPattern());
         }
         else
         {
            bib2gls.warningMessage("warning.cant.parse.sort",
                value, id);
         }
      }

      Long num;

      value = sortDateFormat.format(dateValue);

      if (calendar == null)
      {
         num = Long.valueOf(dateValue.getTime());
      }
      else
      {
         calendar.setTime(dateValue);

         int era = calendar.get(Calendar.ERA);

         if (era == 0)
         {
            era = -1;
         }

         num = Long.valueOf((calendar.get(Calendar.YEAR)*10000L
              + calendar.get(Calendar.MONTH)*100L
              + calendar.get(Calendar.DAY_OF_MONTH))
              * era);

         value = String.format("%+d %s", era, value);
      }

      entry.setNumericSort(num);
      entry.setSortObject(dateValue);

      return value;
   }

   protected long getDefaultGroupId(Bib2GlsEntry entry, int codePoint, 
     Object sortValue)
   {
      return entry.getNumericSort().longValue();
   }

   protected GroupTitle createDefaultGroupTitle(int codePoint,
      Object sortValue, String type)
   {
      return new DateTimeGroupTitle(dateFormat, (Date)sortValue, 
                 type, hasDate, hasTime);
   }


   protected String updateSortValue(Bib2GlsEntry entry, 
      Vector<Bib2GlsEntry> entries)
   {
      String value = super.updateSortValue(entry, entries);

      Number num = entry.getNumericSort();
      Date dateValue = (Date)entry.getSortObject();

      String grp = null;

      GlsResource resource = bib2gls.getCurrentResource();

      String type = resource.getType(entry, entryType);

      if (type == null)
      {
         type = "";
      }

      if (bib2gls.useGroupField() && value.length() > 0
           && !entry.hasParent())
      {
         if (entry.getFieldValue(groupField) == null)
         {
            grp = setGroupTitle(entry, -1, dateValue,
              dateFormat.format(dateValue), type);
         }
      }

      if (bib2gls.getVerboseLevel() > 0)
      {
         String id = entry.getId();

         if (grp == null)
         {
            bib2gls.verbose(String.format("%s -> '%s' [%d]", id, value, num));
         }
         else
         {
            bib2gls.verbose(String.format("%s -> '%s' [%d] (%s)", 
              id, value, num, grp));
         }
      }

      return value;
   }

   protected int compare(Number num1, Number num2)
   {
      return ((Long)num1).compareTo((Long)num2);
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

   private boolean hasDate, hasTime;

   private DateFormat dateFormat, sortDateFormat;

   private Calendar calendar=null;
}
