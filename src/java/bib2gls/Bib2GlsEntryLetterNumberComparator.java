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

public class Bib2GlsEntryLetterNumberComparator 
  extends Bib2GlsEntryLetterComparator
{
   public Bib2GlsEntryLetterNumberComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    String sort, String sortField, String groupField, String entryType,
    int caseStyle, boolean reverse, int numberPosition,
    int sortSuffixOption, String sortSuffixMarker)
   {
      super(bib2gls, entries, sort, sortField, groupField, entryType, 
      caseStyle, reverse, sortSuffixOption, sortSuffixMarker);

      switch (numberPosition)
      {
         case NUMBER_BEFORE_UPPER:
         case NUMBER_BEFORE_LOWER:
         case NUMBER_BEFORE_LETTER:
         case NUMBER_AFTER_LETTER:
         case NUMBER_FIRST:
         case NUMBER_LAST:
           this.numberPosition = numberPosition;
         break;
         default:
           throw new IllegalArgumentException(
            "Invalid number option: "+numberPosition);
      }
   }

   private int compareNumberChar(int cp)
   {
      int result = 0;

      switch (numberPosition)
      {
         case NUMBER_BEFORE_LOWER:

            result = Character.isLowerCase(cp) ? -1 : 1;

         break;
         case NUMBER_BEFORE_UPPER:

            result = Character.isUpperCase(cp) ? -1 : 1;

         break;
         case NUMBER_BEFORE_LETTER:

            result = Character.isLetter(cp) ? -1 : 1;

         break;
         case NUMBER_AFTER_LETTER:

            result = Character.isLetter(cp) ? 1 : -1;

         break;
         case NUMBER_FIRST:

            result = -1;

         break;
         case NUMBER_LAST:

            result = 1;

         break;
      }

      return reverse ? -result : result;
   }

   protected int compare(String str1, String str2)
   {
      int n1 = str1.length();
      int n2 = str2.length();

      int i = 0;
      int j = 0;

      while (i < n1 && j < n2)
      {
         Matcher m = Bib2Gls.INT_PATTERN.matcher(str1.substring(i));

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

         m = Bib2Gls.INT_PATTERN.matcher(str2.substring(j));

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
               int result = compareNumberChar(str1.codePointAt(i));

               return reverse ? result : -result;
            }
            else if (num2 == null)
            {
               int result = compareNumberChar(str2.codePointAt(j));

               return reverse ? -result : result;
            }
            else
            {
               int result=reverse? num2.compareTo(num1) : num1.compareTo(num2);

               if (result != 0)
               {
                  return result;
               }
            }
         }
         else
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

   public static final int NUMBER_BEFORE_LOWER=0;
   public static final int NUMBER_BEFORE_UPPER=1;
   public static final int NUMBER_BEFORE_LETTER=2;
   public static final int NUMBER_AFTER_LETTER=3;
   public static final int NUMBER_FIRST=4;
   public static final int NUMBER_LAST=5;

   private int numberPosition;
}
