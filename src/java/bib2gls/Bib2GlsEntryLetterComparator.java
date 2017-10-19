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

public class Bib2GlsEntryLetterComparator extends SortComparator
{
   public Bib2GlsEntryLetterComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries, SortSettings settings,
    String sortField, String groupField, String entryType)
   {
      super(bib2gls, entries, settings, sortField, groupField,
        entryType);

      this.caseStyle = settings.caseStyle();

      switch (caseStyle)
      {
         case CASE:
         case TOLOWER:
         case UPPERLOWER:
         case LOWERUPPER:
         break;
         default:
            throw new IllegalArgumentException("Invalid caseStyle");
      }

      this.caseStyle = caseStyle;
   }

   protected String adjustSort(Bib2GlsEntry entry, String sortStr)
   {
      if (caseStyle == TOLOWER)
      {
         return sortStr.toLowerCase();
      }

      return sortStr;
   }

   protected String updateSortValue(Bib2GlsEntry entry, 
      Vector<Bib2GlsEntry> entries)
   {
      String value = super.updateSortValue(entry, entries);

      String id = entry.getId();

      String grp = null;

      GlsResource resource = bib2gls.getCurrentResource();

      String type = getType(entry);

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
            grp = (caseStyle == CASE ? str : str.toUpperCase());

            int cp = grp.codePointAt(0);

            if (caseStyle == UPPERLOWER || caseStyle == LOWERUPPER)
            {
               cp = Character.toLowerCase(cp);
            }

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

   protected int compare(int cp1, int cp2)
   {
      if (caseStyle == UPPERLOWER)
      {
         int lcp1 = Character.toLowerCase(cp1);
         int lcp2 = Character.toLowerCase(cp2);

         if (lcp1 == lcp2)
         {
            if (cp1 == cp2)
            {
               return 0;
            }
            else if (Character.isUpperCase(cp1))
            {
               return -1;
            }
            else if (Character.isUpperCase(cp2))
            {
               return 1;
            }
            else
            {// punctuation

               if (cp1 < cp2)
               {
                  return -1;
               }
               else
               {
                  return 1;
               }
            }
         }
         else if (lcp1 < lcp2)
         {
            return -1;
         }
         else if (lcp1 > lcp2)
         {
            return 1;
         }
      }
      else if (caseStyle == LOWERUPPER)
      {
         int lcp1 = Character.toLowerCase(cp1);
         int lcp2 = Character.toLowerCase(cp2);

         if (lcp1 == lcp2)
         {
            if (cp1 == cp2)
            {
               return 0;
            }
            else if (Character.isLowerCase(cp1))
            {
               return -1;
            }
            else if (Character.isLowerCase(cp2))
            {
               return 1;
            }
            else
            {// punctuation

               if (cp1 < cp2)
               {
                  return -1;
               }
               else
               {
                  return 1;
               }
            }
         }
         else if (lcp1 < lcp2)
         {
            return -1;
         }
         else if (lcp1 > lcp2)
         {
            return 1;
         }
      }
      else if (cp1 < cp2)
      {
         return -1;
      }
      else if (cp1 > cp2)
      {
         return 1;
      }

      return 0;
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

         int result = compare(cp1, cp2);

         if (result != 0)
         {
            return result;
         }
      }

      return n1 == n2 ? 0 : (n1 < n2 ? -1 : 1);
   }

   protected int compareElements(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      return compare(entry1.getFieldValue("sort"), 
            entry2.getFieldValue("sort"));
   }


   protected int caseStyle;

   public static final int CASE=0;
   public static final int TOLOWER=1;
   public static final int UPPERLOWER=2;
   public static final int LOWERUPPER=3;
}
