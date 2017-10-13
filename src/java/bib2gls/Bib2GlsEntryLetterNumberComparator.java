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
    Vector<Bib2GlsEntry> entries, SortSettings settings,
    String sortField, String groupField, String entryType)
   {
      super(bib2gls, entries, settings, sortField, groupField, entryType);

      numberPosition = settings.getLetterNumberRule();

      switch (numberPosition)
      {
         case NUMBER_BEFORE_UPPER:
         case NUMBER_BEFORE_LOWER:
         case NUMBER_BEFORE_LETTER:
         case NUMBER_AFTER_LETTER:
         case NUMBER_FIRST:
         case NUMBER_LAST:
         break;
         default:
           throw new IllegalArgumentException(
            "Invalid number option: "+numberPosition);
      }

      puncPosition = settings.getLetterNumberPuncRule();

      switch (puncPosition)
      {
         case PUNCTUATION_SPACE_FIRST:
         case PUNCTUATION_SPACE_LAST:
         case SPACE_PUNCTUATION_FIRST:
         case SPACE_PUNCTUATION_LAST:
         case SPACE_FIRST_PUNCTUATION_LAST:
         case PUNCTUATION_FIRST_SPACE_LAST:
         case PUNCTUATION_FIRST_SPACE_ZERO:
         case PUNCTUATION_LAST_SPACE_ZERO:
         break;
         default:
           throw new IllegalArgumentException(
            "Invalid punctuation option: "+puncPosition);
      }
   }

   @Override
   protected String adjustSort(String sortStr)
   {
      if (puncPosition == PUNCTUATION_FIRST_SPACE_ZERO
       || puncPosition == PUNCTUATION_LAST_SPACE_ZERO
       || puncPosition == PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT
       || puncPosition == PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT)
      {
         int n = sortStr.length();

         if (n == 0) return sortStr;

         boolean matchNext = (
            puncPosition == PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT
         || puncPosition == PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT);

         StringBuilder builder = new StringBuilder(n);

         for (int i = 0; i < n; )
         {
            int cp = sortStr.codePointAt(i);
            i += Character.charCount(cp);

            if (Character.isWhitespace(cp))
            {// Is this followed by a digit?

               int nextCp = (i < n ? sortStr.codePointAt(i) : 0);
               cp = '0';

               if (nextCp <= 0x7F || !matchNext)
               {// Basic Latin character or don't match next.
                // Don't need to do anything.
               }
               else if (Character.isDigit(nextCp))
               {// Try to match digit group.
                // This assumes the code points for the digits are 
                // consecutive starting from zero.

                  try
                  {
                     // This should set val to the zero digit for this
                     // group.

                     int val = nextCp
                             - Integer.valueOf(String.format("%c", nextCp));

                     // Check (if val isn't zero, fallback on Latin
                     // digit 0)

                     if (Integer.valueOf(String.format("%c", val)) == 0)
                     {
                        cp = val;
                     }
                  }
                  catch (NumberFormatException e)
                  {// shouldn't happen

                     bib2gls.debug(e);
                  }
               }
               else if (Bib2Gls.isSubscriptDigit(nextCp))
               {
                  cp = Bib2Gls.SUBSCRIPT_ZERO;
               }
               else if (Bib2Gls.isSuperscriptDigit(nextCp))
               {
                  cp = Bib2Gls.SUPERSCRIPT_ZERO;
               }
            }

            builder.appendCodePoint(cp);
         }

         return builder.toString();
      }
      else
      {
         return super.adjustSort(sortStr);
      }
   }

   @Override
   protected int compare(int cp1, int cp2)
   {
      if (Character.isLetter(cp1) && Character.isLetter(cp2))
      {
         // both are letters

         return super.compare(cp1, cp2);
      }

      boolean isSpace1 = Character.isWhitespace(cp1);
      boolean isSpace2 = Character.isWhitespace(cp2);

      if (isSpace1 == isSpace2)
      {
         // either both white space or both aren't

         if (cp1 < cp2)
         {
            return reverse ? 1 : -1;
         }
         else if (cp1 > cp2)
         {
            return reverse ? -1 : 1;
         }
         else
         {
            return 0;
         }
      }

      if (isSpace1)
      {
         // cp1 is white space, cp2 isn't

         switch (puncPosition)
         {
            case PUNCTUATION_SPACE_FIRST:
            case PUNCTUATION_SPACE_LAST:
            case PUNCTUATION_FIRST_SPACE_LAST:

               return reverse ? 1 : -1;

            case SPACE_PUNCTUATION_FIRST:
            case SPACE_PUNCTUATION_LAST:
            case SPACE_FIRST_PUNCTUATION_LAST:

               return reverse ? -1 : 1;

            default:
            // shouldn't happen but keep compiler happy

              throw new IllegalArgumentException(
                "Invalid letter-number-punc setting: "+puncPosition);
         }
      }

      // cp2 is white space, cp1 isn't

      switch (puncPosition)
      {
         case PUNCTUATION_SPACE_FIRST:
         case PUNCTUATION_SPACE_LAST:
         case PUNCTUATION_FIRST_SPACE_LAST:

            return reverse ? -1 : 1;

         case SPACE_PUNCTUATION_FIRST:
         case SPACE_PUNCTUATION_LAST:
         case SPACE_FIRST_PUNCTUATION_LAST:

            return reverse ? 1 : -1;

         default:
         // shouldn't happen but keep compiler happy

           throw new IllegalArgumentException(
             "Invalid letter-number-punc setting: "+puncPosition);
      }
   }

   private int compareNumberChar(int cp)
   {
      if (numberPosition == NUMBER_FIRST)
      {
         return -1;
      }
      else if (numberPosition == NUMBER_LAST)
      {
         return 1;
      }
      else if (Character.isLetter(cp))
      {
         return compareNumberLetter(cp);
      }
      else
      {
         switch (puncPosition)
         {
            case PUNCTUATION_SPACE_FIRST:
            case SPACE_PUNCTUATION_FIRST:
            case PUNCTUATION_FIRST_SPACE_ZERO:

               return 1;

            case PUNCTUATION_SPACE_LAST:
            case SPACE_PUNCTUATION_LAST:
            case PUNCTUATION_LAST_SPACE_ZERO:

               return -1;

            case SPACE_FIRST_PUNCTUATION_LAST:

               return Character.isWhitespace(cp) ? 1 : -1;

            case PUNCTUATION_FIRST_SPACE_LAST:

               return Character.isWhitespace(cp) ? -1 : 1;

            default:
            // shouldn't happen but keep compiler happy

              throw new IllegalArgumentException(
                "Invalid letter-number-punc setting: "+puncPosition);
         }
      }

   }

   private int compareNumberLetter(int cp)
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
            default:
            // shouldn't happen but keep compiler happy

              throw new IllegalArgumentException(
                "Invalid letter-number setting: "+numberPosition);
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

   public static final int PUNCTUATION_SPACE_FIRST=0;
   public static final int PUNCTUATION_SPACE_LAST=1;
   public static final int SPACE_PUNCTUATION_FIRST=2;
   public static final int SPACE_PUNCTUATION_LAST=3;
   public static final int SPACE_FIRST_PUNCTUATION_LAST=4;
   public static final int PUNCTUATION_FIRST_SPACE_LAST=5;
   public static final int PUNCTUATION_FIRST_SPACE_ZERO=6;
   public static final int PUNCTUATION_LAST_SPACE_ZERO=7;
   public static final int PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT=8;
   public static final int PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT=9;

   private int numberPosition, puncPosition;
}
