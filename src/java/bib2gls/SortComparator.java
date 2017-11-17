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

import java.util.Vector;
import java.util.Comparator;
import java.util.HashMap;

import com.dickimawbooks.texparserlib.bib.BibValueList;

public abstract class SortComparator implements Comparator<Bib2GlsEntry>
{
   public SortComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries, SortSettings settings,
    String sortField, String groupField, String entryType)
   {
      this.sortField = sortField;
      this.groupField = groupField;
      this.entryType = entryType;
      this.bib2gls = bib2gls;
      this.entries = entries;
      this.settings = settings;

   }

   protected boolean useSortSuffix()
   {
      return (settings.getSuffixOption() != SortSettings.SORT_SUFFIX_NONE);
   }

   protected String getType(Bib2GlsEntry entry)
   {
      GlsResource resource = bib2gls.getCurrentResource();

      String type = resource.getType(entry, entryType);

      if (type == null)
      {
         type = "";
      }

      return type;
   }

   protected String adjustSort(Bib2GlsEntry entry, String sortStr)
   {
      return sortStr;
   }

   protected String padNumbers(Bib2GlsEntry entry, String sortStr, int pad)
   {
      StringBuilder builder = new StringBuilder();
      boolean sign=false;

      for (int i = 0, strLength = sortStr.length(); i < strLength; )
      {
         int cp = sortStr.codePointAt(i);
         i += Character.charCount(cp);

         if (cp == '+')
         {
            int nextCp = (i < strLength ? sortStr.codePointAt(i) : -1);
   
            if (Character.isDigit(nextCp))
            {
               builder.append(settings.getPadPlus());
            }
            else
            {
               builder.appendCodePoint(cp);
            }
  
            sign=true;
         }
         else if (cp == bib2gls.SUBSCRIPT_PLUS)
         {
            int nextCp = (i < strLength ? sortStr.codePointAt(i) : -1);
 
            if (bib2gls.isSubscriptDigit(nextCp))
            {
               builder.append(settings.getPadPlus());
            }
            else
            {
               builder.appendCodePoint(cp);
            }

            sign=true;
         }
         else if (cp == bib2gls.SUPERSCRIPT_PLUS)
         {
            int nextCp = (i < strLength ? sortStr.codePointAt(i) : -1);
   
            if (bib2gls.isSuperscriptDigit(nextCp))
            {
               builder.append(settings.getPadPlus());
            }
            else
            {
               builder.appendCodePoint(cp);
            }
   
            sign=true;
         }
         else if (cp == '-' || cp == bib2gls.MINUS)
         {
            int nextCp = (i < strLength ? sortStr.codePointAt(i) : -1);
   
            if (Character.isDigit(nextCp))
            {
               builder.append(settings.getPadMinus());
            }
            else
            {
               builder.appendCodePoint(cp);
            }

            sign=true;
         }
         else if (cp == bib2gls.SUBSCRIPT_MINUS)
         {
            int nextCp = (i < strLength ? sortStr.codePointAt(i) : -1);
   
            if (bib2gls.isSubscriptDigit(nextCp))
            {
               builder.append(settings.getPadMinus());
            }
            else
            {
               builder.appendCodePoint(cp);
            }
         }
         else if (cp == bib2gls.SUPERSCRIPT_MINUS)
         {
            int nextCp = (i < strLength ? sortStr.codePointAt(i) : -1);
   
            if (bib2gls.isSuperscriptDigit(nextCp))
            {
               builder.append(settings.getPadMinus());
            }
            else
            {
               builder.appendCodePoint(cp);
            }
   
            sign=true;
         }
         else if (Character.isDigit(cp))
         {
            if (!sign)
            {
               builder.append(settings.getPadPlus());
            }

            int zeroChar = '0';

            try
            {
               zeroChar = cp-Integer.parseInt(String.format("%c", cp));
            }
            catch (NumberFormatException e)
            {//shouldn't happen
               bib2gls.debug(e);
            }

            StringBuilder subStr = new StringBuilder();
            subStr.appendCodePoint(cp);
            int n = 1;

            if (i < strLength)
            {
               cp = sortStr.codePointAt(i);

               while (Character.isDigit(cp))
               {
                  n++;
                  i += Character.charCount(cp);

                  if (i >= strLength) break;

                  subStr.appendCodePoint(cp);
                  cp = sortStr.codePointAt(i);
               }
            }

            for ( ; n < pad; n++)
            {
               builder.appendCodePoint(zeroChar);
            }

            builder.append(subStr);

            sign=false;
         }
         else if (bib2gls.isSubscriptDigit(cp))
         {
            if (!sign)
            {
               builder.append(settings.getPadPlus());
            }

            int zeroChar = bib2gls.SUBSCRIPT_ZERO;

            StringBuilder subStr = new StringBuilder();
            subStr.appendCodePoint(cp);
            int n = 1;

            if (i < strLength)
            {
               cp = sortStr.codePointAt(i);

               while (bib2gls.isSubscriptDigit(cp))
               {
                  n++;
                  i += Character.charCount(cp);

                  if (i >= strLength) break;

                  subStr.appendCodePoint(cp);
                  cp = sortStr.codePointAt(i);
               }
            }

            for ( ; n < pad; n++)
            {
               builder.appendCodePoint(zeroChar);
            }

            builder.append(subStr);

            sign=false;
         }
         else if (bib2gls.isSuperscriptDigit(cp))
         {
            if (!sign)
            {
               builder.append(settings.getPadPlus());
            }

            int zeroChar = bib2gls.SUPERSCRIPT_ZERO;

            StringBuilder subStr = new StringBuilder();
            subStr.appendCodePoint(cp);
            int n = 1;

            if (i < strLength)
            {
               cp = sortStr.codePointAt(i);

               while (bib2gls.isSuperscriptDigit(cp))
               {
                  n++;
                  i += Character.charCount(cp);

                  if (i >= strLength) break;

                  subStr.appendCodePoint(cp);
                  cp = sortStr.codePointAt(i);
               }
            }

            for ( ; n < pad; n++)
            {
               builder.appendCodePoint(zeroChar);
            }

            builder.append(subStr);

            sign=false;
         }
         else
         {
            builder.appendCodePoint(cp);

            sign=false;
         }
      }

      return builder.toString();
   }

   protected String updateSortValue(Bib2GlsEntry entry, 
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
                   && value.matches("(?s).*[\\\\\\$\\{\\}\\~].*"))
         {
            value = bib2gls.interpret(value, list, settings.isTrimOn());
         }

         if (useSortSuffix())
         {
            value = applySuffix(entry, value);
         }
      }

      if (settings.getNumberPad() > 1)
      {
         value = padNumbers(entry, value, settings.getNumberPad());
      }

      value = adjustSort(entry, value);

      entry.putField(sortStorageField, value);

      if (settings.getIdenticalSortAction() 
         == SortSettings.IDENTICAL_SORT_USE_FIELD)
      {
         String field = settings.getIdenticalSortField();

         String fallbackValue = entry.getFieldValue(field);
         BibValueList list = entry.getField(field);

         if (fallbackValue == null)
         {
            fallbackValue = "";
         }
         else if (bib2gls.useInterpreter() && list != null
                   && fallbackValue.matches("(?s).*[\\\\\\$\\{\\}\\~].*"))
         {
            fallbackValue = bib2gls.interpret(fallbackValue, list, 
               settings.isTrimOn());
         }

         entry.putField(sortFallbackField, fallbackValue);
      }

      return value;
   }

   protected String applySuffix(Bib2GlsEntry entry, String value)
   {
      switch (settings.getSuffixOption())
      {
         case SortSettings.SORT_SUFFIX_FIELD:

           String field = settings.getSuffixField();
           String fieldValue = entry.getFieldValue(field);

           if (fieldValue != null)
           {
              if (bib2gls.useInterpreter()
                   && fieldValue.matches(".*[\\\\\\$\\{\\}\\~].*"))
              {
                 BibValueList list = entry.getField(field);

                 if (list != null)
                 {
                    fieldValue = bib2gls.interpret(fieldValue,
                      list, settings.isTrimOn());
                 }
              }

              String suff = settings.getSuffixMarker() + fieldValue;

              if (bib2gls.getVerboseLevel() > 0)
              {
                  bib2gls.logMessage(
                    bib2gls.getMessage("message.sort_suffix",
                       suff, value, entry.getId()));
              }

              value += suff;
           }

         break;
         case SortSettings.SORT_SUFFIX_NON_UNIQUE:

            String suff = sortSuffix(value, entry);

            if (suff != null)
            {
               suff = settings.getSuffixMarker() + suff;

               if (bib2gls.getVerboseLevel() > 0)
               {
                   bib2gls.logMessage(
                      bib2gls.getMessage("message.sort_suffix",
                       suff, value, entry.getId()));
               }

               value += suff;
            }
         break;
      }

      return value;
   }

   protected String sortSuffix(String sort, Bib2GlsEntry entry)
   {
      if (sortCount == null) return null;

      String parentId = entry.getParent();

      String type = getType(entry);

      /*
       Non-unique sort keys aren't a problem across different 
       hierarchical levels. The biggest problem occurs when two
       or more siblings have the same sort value and one or more 
       of the siblings has child entries, as this can cause all
       the children to be clumped together after the last of the 
       sibling set.

       The sortCount hash map keeps track of all the sort values 
       used for a particular level. The simplest method is to use 
       a combination of the parent label and the sort value 
       for sub-entries. To avoid the odd possibility of a top-level
       sort value coincidentally matching a sub-entry's parent id
       and sort value combination, the key to the hash map
       use a control code as a separator for sub-entries since there 
       shouldn't be control codes in the label.
       (0x1F is the unit separator control code.)

       The entry type needs to be at the top level, if provided, 
       as it shouldn't matter if the duplicates are in separate glossaries.
      */

      String key = String.format("%s\u001f%s\u001f%s",
          type, parentId == null? "" : parentId, sort);

      Integer num = sortCount.get(key);

      if (num == null)
      {
         sortCount.put(key, Integer.valueOf(0));

         if (bib2gls.getDebugLevel() > 0)
         {
            bib2gls.logMessage(String.format("%s: %s -> 0",
              entry.getId(), key));
         }

         return null;
      }

      bib2gls.verbose(bib2gls.getMessage("message.non_unique_sort",
        sort, entry.getOriginalId()));

      num = Integer.valueOf(num.intValue()+1);

      sortCount.put(key, num);

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.logMessage(String.format("%s: %s -> %d",
           entry.getId(), key, num));
      }

      return num.toString();
   }

   protected abstract int compareElements(Bib2GlsEntry entry1, 
     Bib2GlsEntry entry2);

   private int getIdenticalSortFallback(Bib2GlsEntry entry1, 
     Bib2GlsEntry entry2)
   {
      bib2gls.debug(bib2gls.getMessage("warning.identical",
        entry1.getId(), entry2.getId()));

      switch (settings.getIdenticalSortAction())
      {
         case SortSettings.IDENTICAL_SORT_USE_ID:

            bib2gls.debug(bib2gls.getMessage("warning.identical.id"));

            return entry1.getId().compareTo(entry2.getId());

         case SortSettings.IDENTICAL_SORT_USE_ORIGINAL_ID:

            bib2gls.debug(bib2gls.getMessage("warning.identical.original_id"));

            return entry1.getOriginalId().compareTo(
               entry2.getOriginalId());

         case SortSettings.IDENTICAL_SORT_USE_FIELD:

            String value1 = entry1.getFieldValue(sortFallbackField);

            String value2 = entry2.getFieldValue(sortFallbackField);

            int result = value1.compareTo(value2);

            if (bib2gls.getDebugLevel() > 0)
            {
               bib2gls.logMessage(bib2gls.getMessage("warning.identical.field", 
                 settings.getIdenticalSortField(), value1, value2, result));
            }

            return result;
      }

      bib2gls.debug(bib2gls.getMessage("warning.identical.none"));

      return 0;
   }

   public int compare(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      boolean reverse = settings.isReverse();

      if (bib2gls.getCurrentResource().flattenSort())
      {
         int result = compareElements(entry1, entry2);

         if (result == 0)
         {
            result = getIdenticalSortFallback(entry1, entry2);
         }

         return reverse ? -result : result;
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

      if (entry1.equals(entry2))
      {
         return 0;
      }

      int n1 = entry1.getHierarchyCount();
      int n2 = entry2.getHierarchyCount();

      int n = Integer.min(n1, n2);

      for (int i = 0; i < n; i++)
      {
         Bib2GlsEntry e1 = entry1.getHierarchyElement(i);
         Bib2GlsEntry e2 = entry2.getHierarchyElement(i);

         int result = compareElements(e1, e2);

         if (result == 0)
         {
            result = getIdenticalSortFallback(entry1, entry2);
         }

         if (reverse)
         {
            result = -result;
         }

         if (bib2gls.getDebugLevel() > 1)
         {
            bib2gls.logAndPrintMessage(String.format("%s %c %s",
              e1.getFieldValue(sortStorageField),
              result == 0 ? '=' : (result < 0 ? '<' : '>'),
              e2.getFieldValue(sortStorageField)));
         }

         if (result != 0)
         {
            return result;
         }
      }

      // hierarchy needs preserving

      return (n1 == n2 ? 0 : (n1 < n2 ? -1 : 1));
   }

   protected void setActualSortField(Bib2GlsEntry entry)
   {
      String value = entry.getFieldValue(sortStorageField);

      entry.putField("sort", Bib2Gls.replaceSpecialChars(value));
   }

   public void sortEntries() throws Bib2GlsException
   {
      if (settings.getSuffixOption() == SortSettings.SORT_SUFFIX_NON_UNIQUE)
      {
         sortCount = new HashMap<String,Integer>();
      }

      for (Bib2GlsEntry entry : entries)
      {
         entry.updateHierarchy(entries);
         updateSortValue(entry, entries);
         setActualSortField(entry);
      }

      entries.sort(this);
   }

   protected String sortStorageField = "bib2gls@sort";

   protected String sortFallbackField = "sortfallback";

   protected String sortField, groupField, entryType;

   private HashMap<String,Integer> sortCount;

   protected Bib2Gls bib2gls;

   protected Vector<Bib2GlsEntry> entries;

   protected SortSettings settings;
}