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

   // does location for this follow location for other record?
   public boolean follows(GlsRecord record)
   {
      if (!prefix.equals(record.prefix)
        ||!counter.equals(record.counter)
        ||!format.equals(record.format))
      {
         return false;
      }

      return consecutive(record.location, location);
   }

   // is location2 one more than location1?
   public static boolean consecutive(String location1, String location2)
   {
      if (location1.isEmpty() || location2.isEmpty())
      {
         return false;
      }

      Matcher m1 = DIGIT_PATTERN.matcher(location1);
      Matcher m2 = DIGIT_PATTERN.matcher(location2);

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
            return sep1.equals(sep2) ?
                   consecutive(prefix1, prefix2) :
                   consecutive(prefix1+sep1, prefix2+sep2);
         }

         if (!prefix1.equals(prefix2) || !sep1.equals(sep2))
         {
            return false;
         }

         try
         {
            int loc1 = Integer.parseInt(suffix1);
            int loc2 = Integer.parseInt(suffix2);

            return loc2 == loc1+1;
         }
         catch (NumberFormatException e)
         {// shouldn't happen (integer pattern matched)
            e.printStackTrace();
         }

         return false;
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
                   consecutive(prefix1, prefix2) :
                   consecutive(prefix1+sep1, prefix2+sep2);
         }

         if (!prefix1.equals(prefix2) || !sep1.equals(sep2))
         {
            return false;
         }

         return suffix2.codePointAt(0) == suffix1.codePointAt(0)+1;
      }

      return false;
   }

   public String toString()
   {
      return String.format(
        "record[label=%s,prefix=%s,counter=%s,format=%s,location=%s]",
         label, prefix, counter, format, location);
   }

   private String label, prefix, counter, format, location;

   private static final Pattern DIGIT_PATTERN
     = Pattern.compile("(.*?)([^0-9]?)([0-9]+)");

   private static final Pattern ALPHA_PATTERN
     = Pattern.compile("(.*?)(?:([^a-z]?)([a-z]))|(?:([^A-Z]?)([A-Z]))");
}
