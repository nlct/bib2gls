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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class GlsRecord
{
   public GlsRecord(String label, String prefix, String counter,
      String format, String location)
   {
      this.label = label;
      this.prefix = prefix;
      this.counter = counter;
      this.format = format;
      this.location = location;
   }

   public String getLabel()
   {
      return label;
   }

   public String getPrefix()
   {
      return prefix;
   }

   public String getCounter()
   {
      return counter;
   }

   public String getFormat()
   {
      return format;
   }

   public void setFormat(String newFormat)
   {
      format = newFormat;
   }

   public String getLocation()
   {
      return location;
   }

   public String getListTeXCode()
   {
      return String.format("\\glsnoidxdisplayloc{%s}{%s}{%s}{%s}",
         prefix, counter, format, location);
   }

   public String getFmtTeXCode()
   {
      if (format.isEmpty())
      {
         return String.format("\\setentrycounter[%s]{%s}%s",
            prefix, counter, location);
      }

      char c = format.charAt(0);

      if (c == '(' || c == ')')
      {
         if (format.length() == 1)
         {
            return String.format("\\setentrycounter[%s]{%s}%s",
               prefix, counter, location);
         }
         else
         {
            return String.format("\\setentrycounter[%s]{%s}\\%s{%s}",
               prefix, counter, format.substring(1), location);
         }
      }

      return String.format("\\setentrycounter[%s]{%s}\\%s{%s}",
         prefix, counter, format, location);
   }

   public boolean equals(Object obj)
   {
      if (obj == null || !(obj instanceof GlsRecord)) return false;

      GlsRecord record = (GlsRecord)obj;

      return label.equals(record.label)
           && prefix.equals(record.prefix)
           && counter.equals(record.counter)
           && format.equals(record.format)
           && location.equals(record.location);
   }

   /*
    * Match all parts except the format.
    */ 
   public boolean partialMatch(GlsRecord record)
   {
      return label.equals(record.label)
           && prefix.equals(record.prefix)
           && counter.equals(record.counter)
           && location.equals(record.location);
   }

   // does location for this follow location for other record?
   public boolean follows(GlsRecord record, int gap)
   {
      if (!prefix.equals(record.prefix)
        ||!counter.equals(record.counter)
        ||!format.equals(record.format))
      {
         return false;
      }

      return consecutive(record.location, location, gap);
   }

   // is location2 one more than location1?
   public static boolean consecutive(String location1, String location2,
     int gap)
   {
      if (location1.isEmpty() || location2.isEmpty())
      {
         return false;
      }

      Matcher m1 = CS_PATTERN.matcher(location1);
      Matcher m2 = CS_PATTERN.matcher(location2);

      if (m1.matches() && m2.matches())
      {
         String prefix1 = m1.group(1);
         String prefix2 = m2.group(1);

         String cs1 = m1.group(2);
         String cs2 = m2.group(2);

         if (!cs1.equals(cs2))
         {
            return false;
         }

         String loc1 = m1.group(3);
         String loc2 = m2.group(3);

         if (loc1.equals(loc2))
         {
            return consecutive(prefix1, prefix2, gap);
         }

         return consecutive(loc1, loc2, gap);
      }

      m1 = DIGIT_PATTERN.matcher(location1);
      m2 = DIGIT_PATTERN.matcher(location2);

      if (m1.matches() && m2.matches())
      {
         String prefix1 = m1.group(1);
         String prefix2 = m2.group(1);

         if (prefix1 == null) prefix1 = "";
         if (prefix2 == null) prefix2 = "";

         String sep1 = m1.group(2);
         String sep2 = m2.group(2);

         String suffix1 = m1.group(3);
         String suffix2 = m2.group(3);

         if (suffix1.equals(suffix2))
         {
            if (suffix1.equals("0"))
            {
               return sep1.equals(sep2) ?
                      consecutive(prefix1, prefix2, gap) :
                      consecutive(prefix1+sep1, prefix2+sep2, gap);
            }
            else
            {
               return false;
            }
         }

         if (!prefix1.equals(prefix2) || !sep1.equals(sep2))
         {
            return false;
         }

         try
         {
            int loc1 = Integer.parseInt(suffix1);
            int loc2 = Integer.parseInt(suffix2);
            int diff = loc2 - loc1;

            return 0 < diff && diff <= gap;
         }
         catch (NumberFormatException e)
         {// shouldn't happen (integer pattern matched)
            e.printStackTrace();
         }

         return false;
      }

      m1 = ROMAN_LC_PATTERN.matcher(location1);
      m2 = ROMAN_LC_PATTERN.matcher(location2);

      if (m1.matches() && m2.matches()
       && !(   m1.group(3).isEmpty()
            && m1.group(4) == null
            && m1.group(5) == null
            && m1.group(6) == null
           )
       && !(   m2.group(3).isEmpty()
            && m2.group(4) == null
            && m2.group(5) == null
            && m2.group(6) == null
           )
         )
      {
         String prefix1 = m1.group(1);
         String prefix2 = m2.group(1);

         String sep1 = m1.group(2);
         String sep2 = m2.group(2);

         int loc1 = romanToDecimal(m1.group(3), m1.group(4), m1.group(5),
                    m1.group(6));
         int loc2 = romanToDecimal(m2.group(3), m2.group(4), m2.group(5),
                    m2.group(6));

         if (loc1 == loc2)
         {
            return sep1.equals(sep2) ?
                   consecutive(prefix1, prefix2, gap) :
                   consecutive(prefix1+sep1, prefix2+sep2, gap);
         }

         if (!prefix1.equals(prefix2) || !sep1.equals(sep2))
         {
            return false;
         }

         int diff = loc2 - loc1;

         return 0 < diff && diff <= gap ;
      }

      m1 = ROMAN_UC_PATTERN.matcher(location1);
      m2 = ROMAN_UC_PATTERN.matcher(location2);

      if (m1.matches() && m2.matches()
       && !(   m1.group(3).isEmpty()
            && m1.group(4) == null
            && m1.group(5) == null
            && m1.group(6) == null
           )
       && !(   m2.group(3).isEmpty()
            && m2.group(4) == null
            && m2.group(5) == null
            && m2.group(6) == null
           )
         )
      {
         String prefix1 = m1.group(1);
         String prefix2 = m2.group(1);

         String sep1 = m1.group(2);
         String sep2 = m2.group(2);

         String hundreds1 = m1.group(4);
         String tens1 = m1.group(5);
         String ones1 = m1.group(6);

         String hundreds2 = m2.group(4);
         String tens2 = m2.group(5);
         String ones2 = m2.group(6);

         int loc1 = romanToDecimal(m1.group(3).toLowerCase(),
            hundreds1 == null ? null : hundreds1.toLowerCase(), 
            tens1 == null ? null : tens1.toLowerCase(),
            ones1 == null ? null : ones1.toLowerCase());
         int loc2 = romanToDecimal(m2.group(3).toLowerCase(),
            hundreds2 == null ? null : hundreds2.toLowerCase(),
            tens2 == null ? null : tens2.toLowerCase(),
            ones2 == null ? null : ones2.toLowerCase());

         if (loc1 == loc2)
         {
            return sep1.equals(sep2) ?
                   consecutive(prefix1, prefix2, gap) :
                   consecutive(prefix1+sep1, prefix2+sep2, gap);
         }

         if (!prefix1.equals(prefix2) || !sep1.equals(sep2))
         {
            return false;
         }

         int diff = loc2 - loc1;

         return 0 < diff && diff <= gap ;
      }

      m1 = ALPHA_PATTERN.matcher(location1);
      m2 = ALPHA_PATTERN.matcher(location2);

      if (m1.matches() && m2.matches())
      {
         String prefix1 = m1.group(1);
         String prefix2 = m2.group(1);

         if (prefix1 == null) prefix1 = "";
         if (prefix2 == null) prefix2 = "";

         String sep1 = m1.group(2);
         String sep2 = m2.group(2);

         String suffix1 = m1.group(3);
         String suffix2 = m2.group(3);

         if (suffix1 == null)
         {
            sep1 = m1.group(4);
            suffix1 = m1.group(5);
         }

         if (suffix2 == null)
         {
            sep2 = m2.group(4);
            suffix2 = m2.group(5);
         }

         if (suffix1.equals(suffix2))
         {
            return sep1.equals(sep2) ?
                   consecutive(prefix1, prefix2, gap) :
                   consecutive(prefix1+sep1, prefix2+sep2, gap);
         }

         if (!prefix1.equals(prefix2) || !sep1.equals(sep2))
         {
            return false;
         }

         int loc1 = suffix1.codePointAt(0);
         int loc2 = suffix2.codePointAt(0);

         int diff = loc2 - loc1;

         return 0 < diff && diff <= gap ;
      }

      return false;
   }

   // arguments should already have been checked against the pattern
   private static int romanToDecimal(String thousands, String hundreds, 
     String tens, String ones)
   {
      int n = 0;

      if (thousands != null && !thousands.isEmpty())
      {
         n = 1000*thousands.length();
      }

      if ("c".equals(hundreds))
      {
         n += 100;
      }
      else if ("cc".equals(hundreds))
      {
         n += 200;
      }
      else if ("ccc".equals(hundreds))
      {
         n += 300;
      }
      else if ("cd".equals(hundreds))
      {
         n += 400;
      }
      else if ("d".equals(hundreds))
      {
         n += 500;
      }
      else if ("dc".equals(hundreds))
      {
         n += 600;
      }
      else if ("dcc".equals(hundreds))
      {
         n += 700;
      }
      else if ("dccc".equals(hundreds))
      {
         n += 800;
      }
      else if ("cm".equals(hundreds))
      {
         n += 900;
      }

      if ("x".equals(tens))
      {
         n += 10;
      }
      else if ("xx".equals(tens))
      {
         n += 20;
      }
      else if ("xxx".equals(tens))
      {
         n += 30;
      }
      else if ("xl".equals(tens))
      {
         n += 40;
      }
      else if ("l".equals(tens))
      {
         n += 50;
      }
      else if ("lx".equals(tens))
      {
         n += 60;
      }
      else if ("lxx".equals(tens))
      {
         n += 70;
      }
      else if ("lxxx".equals(tens))
      {
         n += 80;
      }
      else if ("xc".equals(tens))
      {
         n += 90;
      }

      if ("i".equals(ones))
      {
         n += 1;
      }
      else if ("ii".equals(ones))
      {
         n += 2;
      }
      else if ("iii".equals(ones))
      {
         n += 3;
      }
      else if ("iv".equals(ones))
      {
         n += 4;
      }
      else if ("v".equals(ones))
      {
         n += 5;
      }
      else if ("vi".equals(ones))
      {
         n += 6;
      }
      else if ("vii".equals(ones))
      {
         n += 7;
      }
      else if ("viii".equals(ones))
      {
         n += 8;
      }
      else if ("ix".equals(ones))
      {
         n += 9;
      }

      return n;
   }

   public String toString()
   {
      return String.format(
        "{%s}{%s}{%s}{%s}{%s}",
         label, prefix, counter, format, location);
   }

   private String label, prefix, counter, format, location;

   private static final Pattern DIGIT_PATTERN
     = Pattern.compile("(.*?)([^\\p{javaDigit}]?)(\\p{javaDigit}+)");

   private static final Pattern ROMAN_LC_PATTERN
     = Pattern.compile("(.*?)(.??)(m*)(c{1,3}|c?d|dc{1,3}|cm)?(x{1,3}|x?l|lx{1,3}|xc)?(i{1,3}|i?v|vi{1,3}|ix)?");

   private static final Pattern ROMAN_UC_PATTERN
     = Pattern.compile("(.*?)(.??)(M*)(C{1,3}|C?D|DC{1,3}|CM)?(X{1,3}|X?L|LX{1,3}|XC)?(I{1,3}|I?V|VI{1,3}|IX)?");

   private static final Pattern ALPHA_PATTERN
     = Pattern.compile("(.*?)(?:([^\\p{javaLowerCase}]?)(\\p{javaUpperCase}))|(?:([^\\p{javaUpperCase}]?)(\\p{javaUpperCase}))");

   private static final Pattern CS_PATTERN
     = Pattern.compile("(.*?)(?:\\\\protect\\s*)?(\\\\[\\p{javaAlphabetic}@]+)\\s*\\{([\\p{javaDigit}\\p{javaAlphabetic}]+)\\}");
}
