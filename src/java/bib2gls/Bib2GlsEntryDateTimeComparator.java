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

public class Bib2GlsEntryDateTimeComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryDateTimeComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    String sort, String sortField, String groupField, String entryType,
    Locale locale, String format,
    boolean date, boolean time, boolean reverse)
   {
      this.sortField = sortField;
      this.groupField = groupField;
      this.bib2gls = bib2gls;
      this.entries = entries;
      this.reverse = reverse;
      this.hasDate = date;
      this.hasTime = time;

      if (date && time)
      {
         sortDateFormat = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ssZ");
      }
      else if (date)
      {
         sortDateFormat = new SimpleDateFormat("yyyy-MM-dd");
         calendar = (locale == null ? Calendar.getInstance() :
                    Calendar.getInstance(locale));
      }
      else if (time)
      {
         sortDateFormat = new SimpleDateFormat("HH:mm:ssZ");
      }

      init(locale, format, date, time);
   }

   private void init(Locale locale, String format, boolean date, boolean time)
   {
      int type = DateFormat.DEFAULT;

      if (format == null || "default".equals(format))
      {
         type = DateFormat.DEFAULT;
         format = null;
      }
      else if (format.equals("full"))
      {
         type = DateFormat.FULL;
         format = null;
      }
      else if (format.equals("long"))
      {
         type = DateFormat.LONG;
         format = null;
      }
      else if (format.equals("medium"))
      {
         type = DateFormat.MEDIUM;
         format = null;
      }
      else if (format.equals("short"))
      {
         type = DateFormat.SHORT;
         format = null;
      }

      if (format == null)
      {
         if (date && time)
         {
            if (locale == null)
            {
               dateFormat = DateFormat.getDateTimeInstance(type, type);
            }
            else
            {
               dateFormat = DateFormat.getDateTimeInstance(type, type, locale);
            }
         }
         else if (date)
         {
            if (locale == null)
            {
               dateFormat = DateFormat.getDateInstance(type);
            }
            else
            {
               dateFormat = DateFormat.getDateInstance(type, locale);
            }
         }
         else if (time)
         {
            if (locale == null)
            {
               dateFormat = DateFormat.getTimeInstance(type);
            }
            else
            {
               dateFormat = DateFormat.getTimeInstance(type, locale);
            }
         }
         else
         {
             throw new IllegalArgumentException(
                "Can't have both date=false and time=false");
         }
      }
      else
      {
         if (locale == null)
         {
            dateFormat = new SimpleDateFormat(format);
         }
         else
         {
            dateFormat = new SimpleDateFormat(format, locale);
         }
      }
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
            value = "";

            bib2gls.debug(bib2gls.getMessage("warning.no.default.sort",
              id));
         }
         else if (bib2gls.useInterpreter() && list != null 
                   && value.matches(".*[\\\\\\$\\{\\}].*"))
         {  
            value = bib2gls.interpret(value, list);
         }
      }

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
            bib2gls.warning(bib2gls.getMessage(
                "warning.cant.parse.pattern.sort",
                value, id, ((SimpleDateFormat)dateFormat).toPattern()));
         }
         else
         {
            bib2gls.warning(bib2gls.getMessage("warning.cant.parse.sort",
                value, id));
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


      entry.putField("sort", value);
      entry.setNumericSort(num);

      String grp = null;

      String type = entryType;

      GlsResource resource = bib2gls.getCurrentResource();

      if (type == null)
      {
         type = resource.getType(entry);

         if (type == null)
         {
            type = "";
         }
      }

      if (bib2gls.useGroupField() && value.length() > 0
           && !entry.hasParent())
      {
         if (entry.getFieldValue(groupField) == null)
         {
            long groupId = num.longValue();

            GroupTitle grpTitle = resource.getGroupTitle(type, groupId);
            String args;

            if (grpTitle == null)
            {
               grpTitle = new DateTimeGroupTitle(dateFormat, dateValue, 
                 type, hasDate, hasTime);

               resource.putGroupTitle(grpTitle, entry);
               args = grpTitle.toString();
            }
            else
            {
               args = grpTitle.format(dateFormat.format(dateValue));
            }

            entry.putField(groupField, 
               String.format("\\%s%s", grpTitle.getCsLabelName(), args)); 
         }
      }

      if (bib2gls.getVerboseLevel() > 0)
      {
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
      return reverse ? ((Long)num2).compareTo((Long)num1) 
          : ((Long)num1).compareTo((Long)num2);
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

   private String sortField, groupField, entryType;

   private Bib2Gls bib2gls;

   private Vector<Bib2GlsEntry> entries;

   private boolean reverse, hasDate, hasTime;

   private DateFormat dateFormat, sortDateFormat;

   private Calendar calendar=null;
}
