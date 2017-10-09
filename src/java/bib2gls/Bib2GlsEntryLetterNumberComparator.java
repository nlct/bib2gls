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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.dickimawbooks.texparserlib.bib.BibValueList;

public class Bib2GlsEntryLetterNumberComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryLetterNumberComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    String sort, String sortField, String groupField, String entryType,
    boolean ignoreCase, boolean reverse,
    int sortSuffixOption, String sortSuffixMarker)
   {
      this.sortField = sortField;
      this.groupField = groupField;
      this.entryType = entryType;
      this.bib2gls = bib2gls;
      this.entries = entries;
      this.ignoreCase = ignoreCase;
      this.reverse = reverse;
      this.sortSuffixMarker = sortSuffixMarker;
      this.sortSuffixOption = sortSuffixOption;
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

         if (sortSuffixOption != GlsResource.SORT_SUFFIX_NONE)
         {
            String suff = sortSuffix(value, entry);

            if (suff != null)
            {
               suff = sortSuffixMarker + suff;

               bib2gls.verbose(bib2gls.getMessage("message.sort_suffix",
                 suff, value, id));

               value += suff;
            }
         }
      }

      if (ignoreCase)
      {
         value = value.toLowerCase();
      }

      entry.putField("sort", value);

      String grp = null;

      GlsResource resource = bib2gls.getCurrentResource();

      String type = entryType;

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
         int codePoint = value.codePointAt(0);

         String str;

         if (codePoint > 0xffff)
         {
            str = String.format("%c%c",
              Character.highSurrogate(codePoint),
              Character.lowSurrogate(codePoint));
         }
         else
         {
            str = String.format("%c", codePoint);
         }

         if (entry.getFieldValue(groupField) != null)
         {
            // don't overwrite
         }
         else if (Character.isAlphabetic(codePoint))
         {
            grp = (ignoreCase ? str.toUpperCase() : str);

            int cp = grp.codePointAt(0);

            GroupTitle grpTitle = resource.getGroupTitle(type, cp);
            String args;

            if (grpTitle == null)
            {
               grpTitle = new GroupTitle(grp, str, cp, type);
               resource.putGroupTitle(grpTitle, entry);
               args = grpTitle.toString();
            }
            else
            {
               args = grpTitle.format(str);
            }

            entry.putField(groupField, 
               String.format("\\%s%s", grpTitle.getCsLabelName(), args));
         }
         else
         {
            if (str.equals("\\") || str.equals("{") ||
                str.equals("}"))
            {
               str = "\\char`\\"+str;
            }

            GroupTitle grpTitle = resource.getGroupTitle(entryType, codePoint);
            String args;

            if (grpTitle == null)
            {
               grpTitle = new OtherGroupTitle(str, codePoint, type);
               resource.putGroupTitle(grpTitle, entry);
               args = grpTitle.toString();
            }
            else
            {
               args = grpTitle.format(str);
            }

            entry.putField(groupField, 
               String.format("\\%s%s", grpTitle.getCsLabelName(), args)); 
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
         Matcher m = PATTERN_NUMBER.matcher(str1.substring(i));

         Integer num1 = null;
         Integer num2 = null;

         if (m.matches())
         {
            try
            {
               num1 = Bib2Gls.parseInt(m.group(1));

               i += m.end(1);
            }
            catch (NumberFormatException e)
            {// won't happen since pattern match ensures correct format
            }
         }

         m = PATTERN_NUMBER.matcher(str2.substring(j));

         if (m.matches())
         {
            try
            {
               num2 = Bib2Gls.parseInt(m.group(1));

               j += m.end(1);
            }
            catch (NumberFormatException e)
            {// won't happen since pattern match ensures correct format
            }
         }

         if (num1 != null || num2 != null)
         {
            if (num1 == null)
            {
               // numbers come before lower case
               // (case-insensitive sort converts to lower case)

               int cp1 = str1.codePointAt(i);
               i += Character.charCount(cp1);

               if (Character.isLowerCase(cp1))
               {
                  return reverse ? -1 : 1;
               }
               else
               {
                  return reverse ? 1 : -1;
               }
            }
            else if (num2 == null)
            {
               int cp2 = str2.codePointAt(j);
               j += Character.charCount(cp2);

               if (Character.isLowerCase(cp2))
               {
                  return reverse ? 1 : -1;
               }
               else
               {
                  return reverse ? -1 : 1;
               }
            }
            else
            {
               return reverse ? num2.compareTo(num1) : num1.compareTo(num2);
            }
         }
         else
         {
            int cp1 = str1.codePointAt(i);
            int cp2 = str2.codePointAt(j);

            i += Character.charCount(cp1);
            j += Character.charCount(cp2);

            if (cp1 < cp2)
            {
               return reverse ? 1 : -1;
            }
            else if (cp1 > cp2)
            {
               return reverse ? -1 : 1;
            }
         }
      }

      if (n1 < n2)
      {
         return reverse ? 1 : -1;
      }
      else if (n2 > n1)
      {
         return reverse ? -1 : 1;
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

         int result = compare(e1.getFieldValue("sort"), 
            e2.getFieldValue("sort"));

         if (result != 0)
         {
            return result;
         }
      }

      if (n1 == n2)
      {
         return 0;
      }

      if (reverse)
      {
         return n1 < n2 ? 1 : -1;
      }

      return n1 < n2 ? -1 : 1;
   }

   private String sortSuffix(String sort, Bib2GlsEntry entry)
   {
      if (sortCount == null) return null;

      String parentId = entry.getParent();

      // (see comments for Bib2GlsEntryComparator.sortSuffix)

      String key = (parentId == null ? sort
                    : String.format("%s\u001f%s", parentId, sort));

      Integer num = sortCount.get(key);

      if (num == null)
      {
         sortCount.put(key, Integer.valueOf(0));
         return null;
      }

      bib2gls.verbose(bib2gls.getMessage("message.non_unique_sort",
        sort, entry.getOriginalId()));

      num = Integer.valueOf(num.intValue()+1);

      sortCount.put(key, num);

      return num.toString();
   }

   public void sortEntries() throws Bib2GlsException
   {
      if (sortSuffixOption == GlsResource.SORT_SUFFIX_NON_UNIQUE)
      {
         sortCount = new HashMap<String,Integer>();
      }

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

   private boolean ignoreCase, reverse;

   private String sortSuffixMarker;

   private int sortSuffixOption;

   private HashMap<String,Integer> sortCount;

   private static final Pattern PATTERN_NUMBER 
     = Pattern.compile(String.format(
        "([+-]?\\p{javaDigit}+|%s|%s).*",
           Bib2Gls.SUBSCRIPT_INT_PATTERN,
           Bib2Gls.SUPERSCRIPT_INT_PATTERN));
}
