/*
    Copyright (C) 2017-2022 Nicola L.C. Talbot
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
         case NUMBER_BEFORE_LETTER:
         case NUMBER_AFTER_LETTER:
         case NUMBER_BETWEEN:
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
         case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:
         case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:
         break;
         default:
           throw new IllegalArgumentException(
            "Invalid punctuation option: "+puncPosition);
      }
   }

   @Override
   protected String adjustSort(Bib2GlsEntry entry, String sortStr)
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

                     String strVal = new String(Character.toChars(nextCp));

                     int val = nextCp - Integer.valueOf(strVal);

                     // Check (if val isn't zero, fallback on Latin
                     // digit 0)

                     strVal = new String(Character.toChars(val));

                     if (Integer.valueOf(strVal) == 0)
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

         sortStr = builder.toString();
      }

      if (!sortStr.isEmpty())
      {
         entry.setSortObject(getSortList(sortStr));
      }

      return super.adjustSort(entry, sortStr);
   }

   protected int compareElements(ComponentList list1, ComponentList list2)
   {
      int n1 = list1.size();
      int n2 = list2.size();

      int i = 0;
      int j = 0;

      int result;

      for ( ; i < n1 && j < n2; i++, j++)
      {
         SortComponent comp1 = list1.get(i);
         SortComponent comp2 = list2.get(j);

         // upperlower and lowerupper are awkward with between setting

         if ((caseStyle == UPPERLOWER || caseStyle == LOWERUPPER)
              && numberPosition == NUMBER_BETWEEN)
         {
            boolean isNum1 = (comp1 instanceof ComponentNumber);
            boolean isNum2 = (comp2 instanceof ComponentNumber);

            if (isNum1 && isNum2)
            {
               SortComponent next1 = (i < n1-1 ? list1.get(i+1) : 
                 NULL_COMPONENT);
               SortComponent next2 = (j < n2-1 ? list2.get(j+1) : 
                 NULL_COMPONENT);

               if (next1 instanceof ComponentLetter
                   && next2 instanceof ComponentLetter
                   && next1.getValue() == next2.getValue())
               {
                  result = comp1.compareTo(comp2);
               }
               else
               {// skip the number
                  result = 0;
               }
            }
            else if (isNum1 && !isNum2)
            {
               SortComponent nextC = (i < n1-1 ? list1.get(i+1) : 
                 NULL_COMPONENT);

               if (nextC instanceof ComponentLetter)
               {
                  int cp1 = nextC.getValue();
                  int cp2 = comp2.getValue();

                  if (cp1 == cp2)
                  {// character following number is a letter identical to comp2
                   // compare number with letter
                     result = comp1.compareTo(comp2);
                  }
                  else if (Character.toLowerCase(cp1)
                    == Character.toLowerCase(cp2))
                  {// Character following number is a letter 
                   // that's lowercase-identical to comp2.
                   // Compare both letters instead.
                     result = nextC.compareTo(comp2);

                     if (result == 0)
                     {// compare number with letter
                        result = comp1.compareTo(comp2);
                     }
                  }
                  else
                  {// skip the number
                     result = nextC.compareTo(comp2);
                     i++;
                  }
               }
               else
               {
                  result = comp1.compareTo(comp2);
               }
            }
            else if (!isNum1 && isNum2)
            {
               SortComponent nextC = (j < n2-1 ? list2.get(j+1) : 
                 NULL_COMPONENT);

               if (nextC instanceof ComponentLetter)
               {
                  int cp1 = comp1.getValue();
                  int cp2 = nextC.getValue();

                  if (cp1 == cp2)
                  {// character following number is a letter identical to comp1
                   // compare number with letter
                     result = comp1.compareTo(comp2);
                  }
                  else if (Character.toLowerCase(cp1)
                    == Character.toLowerCase(cp2))
                  {// Character following number is a letter 
                   // that's lowercase-identical to comp1.
                   // Compare both letters instead.
                     result = comp1.compareTo(nextC);

                     if (result == 0)
                     {// compare number with letter
                        result = comp1.compareTo(comp2);
                     }
                  }
                  else
                  {// skip the number
                     result = comp1.compareTo(nextC);
                     j++;
                  }
               }
               else
               {
                  result = comp1.compareTo(comp2);
               }
            }
            else
            {
               result = comp1.compareTo(comp2);
            }

         }
         else
         {
            result = comp1.compareTo(comp2);
         }

         if (result != 0)
         {
            return result;
         }
      }

      if (n1 == n2)
      {
         return 0;
      }

      return n1 < n2 ? -1 : 1;
   }

   protected int compareElements(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      Object object1 = entry1.getSortObject();
      Object object2 = entry2.getSortObject();

      ComponentList list1 = null;
      ComponentList list2 = null;

      if (object1 instanceof ComponentList)
      {
         list1 = (ComponentList)object1;
      }

      if (object2 instanceof ComponentList)
      {
         list2 = (ComponentList)object2;
      }

      if (list1 == null)
      {
         String str1 = entry1.getFieldValue(sortStorageField);

         if (str1 == null || str1.isEmpty())
         {
            if (list2 != null)
            {
               return -1;
            }

            String str2 = entry2.getFieldValue(sortStorageField);

            if (str2 == null || str2.isEmpty())
            {
               return 0;
            }
            else
            {
               return -1;
            }
         }
         else
         {
            list1 = getSortList(str1);
            entry1.setSortObject(list1);
         }
      }

      if (list2 == null)
      {
         String str2 = entry1.getFieldValue(sortStorageField);

         if (str2 == null || str2.isEmpty())
         {
            if (list1 != null)
            {
               return 1;
            }

            String str1 = entry1.getFieldValue(sortStorageField);

            if (str1 == null || str1.isEmpty())
            {
               return 0;
            }
            else
            {
               return 1;
            }
         }
         else
         {
            list2 = getSortList(str2);
            entry2.setSortObject(list2);
         }
      }

      return compareElements(list1, list2);
   }

   public ComponentList getSortList(String str)
   {
      ComponentList list = new ComponentList(str.length());

      for (int i = 0; i < str.length(); )
      {
         int cp = str.codePointAt(i);

         Matcher m = Bib2Gls.INT_PATTERN.matcher(str.substring(i));
         Integer num = null;

         if (m.matches())
         {
            try
            {
               num = Bib2Gls.parseInt(m.group(1));

               i += m.end(1);
            }
            catch (NumberFormatException e)
            {// shouldn't happen since pattern match ensures correct format
               i += Character.charCount(cp);
            }
         }
         else
         {
            i += Character.charCount(cp);
         }

         if (num == null)
         {
            if (Character.isLetter(cp))
            {
               SortComponent prev = list.isEmpty() ? null : list.lastElement();

               if ((caseStyle == UPPERLOWER || caseStyle == LOWERUPPER)
                     && numberPosition == NUMBER_BETWEEN
                     && !(prev instanceof ComponentNumber))
               {
                  boolean isUpper = Character.isUpperCase(cp)
                                   || Character.isTitleCase(cp);

                  if (caseStyle == UPPERLOWER)
                  {
                     list.add(isUpper ? MIN_COMPONENT : MAX_COMPONENT);
                  }
                  else
                  {
                     list.add(isUpper ? MAX_COMPONENT : MIN_COMPONENT);
                  }
               }
               else if (caseStyle == TOLOWER)
               {
                  cp = Character.toLowerCase(cp);
               }

               list.add(new ComponentLetter(cp));
            }
            else if (Character.isWhitespace(cp))
            {
               list.add(new ComponentSpace(cp));
            }
            else
            {
               list.add(new ComponentOther(cp));
            }
         }
         else
         {
            list.add(new ComponentNumber(num));
         }
      }

      return list;
   }

   class ComponentList extends Vector<SortComponent>
   {
      public ComponentList()
      {
         super();
      }

      public ComponentList(int capacity)
      {
         super(capacity);
      }

      public String toString()
      {
         StringBuilder builder = new StringBuilder(capacity());

         for (SortComponent element : this)
         {
            builder.append(element.toString());
         }

         return builder.toString();
      }
   }

   class SortComponent
   {
      public SortComponent(int theValue)
      {
         value = theValue;
      }

      public int getValue()
      {
         return value;
      }

      public String toString()
      {
         if (value == 0 || value == Integer.MIN_VALUE
              || value == Integer.MAX_VALUE)
         {
            if (bib2gls.getDebugLevel() > 0)
            {
               return value == 0 ? "" : (value < 0 ? "\u2199" : "\u2197");
            }
            else
            {
               return "";
            }
         }
         else if (value <= Character.MAX_VALUE)
         {
            return String.format("%c", (char)value);
         }
         else
         {
            return new String(Character.toChars(getValue()));
         }
      }

      public int compareTo(SortComponent component)
      {
         if (value == component.value)
         {
            return 0;
         }
         else if (value < component.value)
         {
            return -1;
         }

         return 1;
      }

      private int value;
   }

   class ComponentLetter extends SortComponent
   {
      public ComponentLetter(int codePoint)
      {
         super(codePoint);
      }

      public int compareTo(SortComponent other)
      {
         if (other instanceof ComponentLetter)
         {
            // both are letters
            return compare(getValue(), other.getValue());
         }

         if (other instanceof ComponentNumber)
         {// other is a number

            switch (numberPosition)
            {
               case NUMBER_FIRST:
               case NUMBER_BEFORE_LETTER:
                 return 1;
               case NUMBER_LAST:
               case NUMBER_AFTER_LETTER:
                 return -1;
               case NUMBER_BETWEEN:

                 boolean isUpper = Character.isUpperCase(getValue())
                    || Character.isTitleCase(getValue());
                 boolean isLower = Character.isLowerCase(getValue());

                 switch (caseStyle)
                 {
                    case CASE:
                    case UPPERLOWER:
                      return isUpper ? -1 : 1;
                    case TOLOWER:
                      // numbers always before letters
                      return 1;
                    case LOWERUPPER:
                      return isLower ? -1 : 1;
                    default:
                      // shouldn't happen
                      throw new IllegalArgumentException(
                        "Invalid case setting: "+caseStyle);
                 }

               default:
                  // shouldn't happen

                 throw new IllegalArgumentException(
                    "Invalid case setting: "+caseStyle);
            }
         }

         if (other instanceof ComponentSpace
          || other instanceof ComponentOther)
         {
            // other is either space or punctuation 

            switch (puncPosition)
            {
               case PUNCTUATION_SPACE_FIRST:
               case SPACE_PUNCTUATION_FIRST:

                  // punctuation/space come before letters
                  return 1;

               case PUNCTUATION_SPACE_LAST:
               case SPACE_PUNCTUATION_LAST:

                  // punctuation/space come after letters
                  return -1;

               case PUNCTUATION_FIRST_SPACE_LAST:

                  // order: punctuation, letter, space

                  if (other instanceof ComponentOther)
                  {// other is punctuation
                     return 1;
                  }
                  else
                  {// other is space
                     return -1;
                  }

               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  // order: punctuation, letter, no space

                  return 1;

               case SPACE_FIRST_PUNCTUATION_LAST:

                  // order: space, letter, punctuation

                  if (other instanceof ComponentSpace)
                  {// other is space
                     return 1;
                  }
                  else
                  {// other is punctuation
                     return -1;
                  }

               case PUNCTUATION_LAST_SPACE_ZERO:
               case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:

                  // order: letter, punctuation, no space

                  return -1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         return super.compareTo(other);
      }
   }

   class ComponentSpace extends SortComponent
   {
      public ComponentSpace(int codePoint)
      {
         super(codePoint);
      }

      public int compareTo(SortComponent other)
      {
         if (other instanceof ComponentSpace)
         {// other is a space
            return super.compareTo(other);
         }

         if (other instanceof ComponentLetter)
         {// other is a letter
            switch (puncPosition)
            {
               case PUNCTUATION_SPACE_FIRST:
               case SPACE_PUNCTUATION_FIRST:
               case SPACE_FIRST_PUNCTUATION_LAST:
               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  return -1;

               case PUNCTUATION_SPACE_LAST:
               case SPACE_PUNCTUATION_LAST:
               case PUNCTUATION_FIRST_SPACE_LAST:
               case PUNCTUATION_LAST_SPACE_ZERO:
               case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:

                  return 1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         if (other instanceof ComponentOther)
         {// other is punctuation

            switch (puncPosition)
            {
               case PUNCTUATION_SPACE_FIRST:
               case PUNCTUATION_SPACE_LAST:
               case PUNCTUATION_FIRST_SPACE_LAST:
               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  // punctuation before space
                  return 1;

               case SPACE_PUNCTUATION_FIRST:
               case SPACE_PUNCTUATION_LAST:
               case SPACE_FIRST_PUNCTUATION_LAST:
               case PUNCTUATION_LAST_SPACE_ZERO:
               case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:

                  // space before punctuation
                  return -1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         if (other instanceof ComponentNumber)
         {// other is a number

            switch (numberPosition)
            {
               case NUMBER_FIRST:
                 return 1;
               case NUMBER_LAST:
                 return -1;
            }

            switch (puncPosition)
            {
               case SPACE_FIRST_PUNCTUATION_LAST:
               case PUNCTUATION_SPACE_FIRST:
               case SPACE_PUNCTUATION_FIRST:
               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  return -1;

               case PUNCTUATION_FIRST_SPACE_LAST:
               case PUNCTUATION_SPACE_LAST:
               case SPACE_PUNCTUATION_LAST:
               case PUNCTUATION_LAST_SPACE_ZERO:
               case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:

                  return 1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         return super.compareTo(other);
      }
   }

   class ComponentOther extends SortComponent
   {
      public ComponentOther(int codePoint)
      {
         super(codePoint);
      }

      public int compareTo(SortComponent other)
      {
         if (other instanceof ComponentOther)
         {// other is punctuation
            return super.compareTo(other);
         }

         if (other instanceof ComponentLetter)
         {// other is a letter
            switch (puncPosition)
            {
               case PUNCTUATION_SPACE_FIRST:
               case SPACE_PUNCTUATION_FIRST:
               case PUNCTUATION_FIRST_SPACE_LAST:
               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  return -1;

               case PUNCTUATION_SPACE_LAST:
               case SPACE_PUNCTUATION_LAST:
               case SPACE_FIRST_PUNCTUATION_LAST:

                  return 1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         if (other instanceof ComponentSpace)
         {// other is white space
            switch (puncPosition)
            {
               case PUNCTUATION_SPACE_FIRST:
               case PUNCTUATION_SPACE_LAST:
               case PUNCTUATION_FIRST_SPACE_LAST:
               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  // punctuation before space
                  return -1;

               case SPACE_PUNCTUATION_FIRST:
               case SPACE_PUNCTUATION_LAST:
               case SPACE_FIRST_PUNCTUATION_LAST:
               case PUNCTUATION_LAST_SPACE_ZERO:
               case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:

                  // space before punctuation
                  return 1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         if (other instanceof ComponentNumber)
         {// other is a number

            switch (numberPosition)
            {
               case NUMBER_FIRST:
                 return 1;
               case NUMBER_LAST:
                 return -1;
            }

            switch (puncPosition)
            {
               case PUNCTUATION_FIRST_SPACE_LAST:
               case PUNCTUATION_SPACE_FIRST:
               case SPACE_PUNCTUATION_FIRST:
               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  return -1;

               case SPACE_FIRST_PUNCTUATION_LAST:
               case PUNCTUATION_SPACE_LAST:
               case SPACE_PUNCTUATION_LAST:
               case PUNCTUATION_LAST_SPACE_ZERO:
               case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:

                  return 1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         return super.compareTo(other);
      }
   }

   class ComponentNumber extends SortComponent
   {
      public ComponentNumber(int value)
      {
         super(value);
      }

      public ComponentNumber(Number num)
      {
         this(num.intValue());
      }

      public String toString()
      {
         if (getValue() == Integer.MAX_VALUE || getValue() == Integer.MIN_VALUE)
         {
            return super.toString();
         }
         else
         {
            return ""+getValue();
         }
      }

      public int compareTo(SortComponent other)
      {
         if (other instanceof ComponentNumber)
         {// other is a number

            if (this == other)
            {
               return 0;
            }

            // a missing number could represent 0 or 1
            if (this == MIN_COMPONENT || this == MAX_COMPONENT)
            {
               return other.getValue() <= 0 ? 1 : -1;
            }

            if (other == MIN_COMPONENT || other == MAX_COMPONENT)
            {
               return getValue() >= 0 ? 1 : -1;
            }

            return super.compareTo(other);
         }

         if (other instanceof ComponentLetter)
         {// other is a letter
            switch (numberPosition)
            {
               case NUMBER_FIRST:
               case NUMBER_BEFORE_LETTER:
                 return -1;
               case NUMBER_LAST:
               case NUMBER_AFTER_LETTER:
                 return 1;
               case NUMBER_BETWEEN:

                 boolean isUpper = Character.isUpperCase(other.getValue())
                    || Character.isTitleCase(other.getValue());
                 boolean isLower = Character.isLowerCase(other.getValue());

                 switch (caseStyle)
                 {
                    case CASE:
                    case UPPERLOWER:
                      return isUpper ? 1 : -1;
                    case TOLOWER:
                      // numbers always before letters
                      return -1;
                    case LOWERUPPER:
                      return isLower ? 1 : -1;
                    default:
                      // shouldn't happen
                      throw new IllegalArgumentException(
                        "Invalid case setting: "+caseStyle);
                 }

               default:
                  // shouldn't happen

                 throw new IllegalArgumentException(
                    "Invalid case setting: "+caseStyle);
            }
         }

         if (other instanceof ComponentSpace)
         {// other is white space
            switch (numberPosition)
            {
               case NUMBER_FIRST:
                 return -1;
               case NUMBER_LAST:
                 return 1;
            }

            switch (puncPosition)
            {
               case SPACE_FIRST_PUNCTUATION_LAST:
               case PUNCTUATION_SPACE_FIRST:
               case SPACE_PUNCTUATION_FIRST:
               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  return 1;

               case PUNCTUATION_FIRST_SPACE_LAST:
               case PUNCTUATION_SPACE_LAST:
               case SPACE_PUNCTUATION_LAST:
               case PUNCTUATION_LAST_SPACE_ZERO:
               case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:

                  return -1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         if (other instanceof ComponentOther)
         {// other is punctuation
            switch (numberPosition)
            {
               case NUMBER_FIRST:
                 return -1;
               case NUMBER_LAST:
                 return 1;
            }

            switch (puncPosition)
            {
               case PUNCTUATION_FIRST_SPACE_LAST:
               case PUNCTUATION_SPACE_FIRST:
               case SPACE_PUNCTUATION_FIRST:
               case PUNCTUATION_FIRST_SPACE_ZERO:
               case PUNCTUATION_FIRST_SPACE_ZERO_MATCH_NEXT:

                  return 1;

               case SPACE_FIRST_PUNCTUATION_LAST:
               case PUNCTUATION_SPACE_LAST:
               case SPACE_PUNCTUATION_LAST:
               case PUNCTUATION_LAST_SPACE_ZERO:
               case PUNCTUATION_LAST_SPACE_ZERO_MATCH_NEXT:

                  return -1;

               default:
               // shouldn't happen but keep compiler happy

                 throw new IllegalArgumentException(
                   "Invalid letter-number-punc setting: "+puncPosition);
            }
         }

         return super.compareTo(other);
      }
   }

   public static final int NUMBER_BEFORE_LETTER=0;
   public static final int NUMBER_AFTER_LETTER=1;
   public static final int NUMBER_BETWEEN=2;
   public static final int NUMBER_FIRST=3;
   public static final int NUMBER_LAST=4;

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

   private final SortComponent NULL_COMPONENT = new ComponentLetter(0);
   private final SortComponent MIN_COMPONENT 
     = new ComponentNumber(Integer.MIN_VALUE);
   private final SortComponent MAX_COMPONENT
     = new ComponentNumber(Integer.MAX_VALUE);

   private int numberPosition, puncPosition;
}
