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
import java.util.IllformedLocaleException;
import java.util.MissingResourceException;
import java.text.Collator;

public class SortSettings
{
   public SortSettings()
   {
      sortMethod = null;
   }

   public SortSettings(String method)
   {
      setMethod(method);
   }

   public static boolean isValidSortMethod(String method)
   {
      if (method == null || "unsrt".equals(method) || "none".equals(method)
         || "random".equals(method) || "combine".equals(method))

      {
         return true;
      }

      if (method.endsWith("-reverse"))
      {
         method = method.substring(0, method.lastIndexOf("-reverse"));
      }

      if (method.equals("doc")
       || method.equals("locale")
       || method.equals("custom"))
      {
         return true;
      }

      if (method.equals("letter-case")
       || method.equals("letter-nocase")
       || method.equals("letter-lowerupper")
       || method.equals("letter-upperlower"))
      {
         return true;
      }

      if (method.equals("letternumber-case")
       || method.equals("letternumber-nocase")
       || method.equals("letternumber-lowerupper")
       || method.equals("letternumber-upperlower"))
      {
         return true;
      }

      if (method.equals("integer")
       || method.equals("hex")
       || method.equals("octal")
       || method.equals("binary")
       || method.equals("float")
       || method.equals("double")
       || method.equals("numeric")
       || method.equals("currency")
       || method.equals("percent")
       || method.equals("numberformat"))
      {
         return true;
      }

      if (method.equals("date")
       || method.equals("datetime")
       || method.equals("time"))
      {
         return true;
      }

      // is method a valid language tag?

      try
      {
         Locale loc = new Locale.Builder().setLanguageTag(method).build();

         try
         {
            String lang = loc.getISO3Language();
         }
         catch (MissingResourceException e)
         {
            return false;
         }
      }
      catch (IllformedLocaleException e)
      {
         return false;
      }

      return true;
   }

   public boolean requiresSorting()
   {
      return sortMethod != null && !sortMethod.equals("use")
       && !sortMethod.equals("none");
   }

   public boolean isOrderOfRecords()
   {
      return "use".equals(sortMethod);
   }

   public boolean isUnsrt()
   {
      return "none".equals(sortMethod);
   }

   public boolean isDateTimeSort()
   {
      return sortMethod != null && sortMethod.startsWith("datetime");
   }

   public boolean isDateOrTimeSort()
   {
      return sortMethod != null && (sortMethod.startsWith("date")
        || sortMethod.startsWith("time"));
   }

   public boolean isDateSort()
   {
      return "date".equals(sortMethod)
          || "date-reverse".equals(sortMethod);
   }

   public boolean isTimeSort()
   {
      return "time".equals(sortMethod)
          || "time-reverse".equals(sortMethod);
   }

   public boolean isLetter()
   {
      return sortMethod != null && sortMethod.startsWith("letter-");
   }

   public boolean isLetterNumber()
   {
      return sortMethod != null && sortMethod.startsWith("letternumber-");
   }

   public boolean isNumeric()
   {
      return isNonLocaleNumeric() || isLocaleNumeric();
   }

   public boolean isLocaleNumeric()
   {
      if (sortMethod == null) return false;

      return sortMethod.startsWith("numeric")
            || sortMethod.startsWith("currency")
            || sortMethod.startsWith("percent")
            || sortMethod.startsWith("numberformat");
   }

   public boolean isNonLocaleNumeric()
   {
      if (sortMethod == null) return false;

      return sortMethod.startsWith("integer") 
            || sortMethod.startsWith("float")
            || sortMethod.startsWith("double")
            || sortMethod.startsWith("hex")
            || sortMethod.startsWith("octal")
            || sortMethod.startsWith("binary");
   }


   public boolean isReverse()
   {
      return sortMethod != null && sortMethod.endsWith("-reverse");
   }

   public int caseStyle()
   {
      if (sortMethod.contains("-nocase"))
      {
         return Bib2GlsEntryLetterComparator.TOLOWER;
      }

      if (sortMethod.contains("-upperlower"))
      {
         return Bib2GlsEntryLetterComparator.UPPERLOWER;
      }

      if (sortMethod.contains("-lowerupper"))
      {
         return Bib2GlsEntryLetterComparator.LOWERUPPER;
      }

      return Bib2GlsEntryLetterComparator.CASE;
   }

   public boolean isRandom()
   {
      return "random".equals(sortMethod);
   }

   public boolean isCustom()
   {
      return "custom".equals(sortMethod) || "custom-reverse".equals(sortMethod);
   }

   public boolean hasCustomRule()
   {
      return collationRule != null;
   }

   public boolean isCustomNumeric()
   {
      return sortMethod != null && sortMethod.startsWith("numberformat");
   }

   public boolean hasCustomNumericRule()
   {
      return numberFormat != null;
   }

   public void setMethod(String method)
   {
      if (!isValidSortMethod(method))
      {
         throw new IllegalArgumentException("Invalid sort method:" +method);
      }

      if ("unsrt".equals(method))
      {
         sortMethod = "none";
      }
      else
      {
         sortMethod = method;
      }
   }

   public String getMethod()
   {
      return sortMethod;
   }

   public void setSortField(String field)
   {
      sortField = field;
   }

   public String getSortField()
   {
      return sortField;
   }

   public void setCollationRule(String rule)
   {
      collationRule = rule;
   }

   public String getCollationRule()
   {
      return collationRule;
   }

   public void setLetterNumberRule(int rule)
   {
      letterNumberRule = rule;
   }

   public int getLetterNumberRule()
   {
      return letterNumberRule;
   }

   public void setLetterNumberPuncRule(int rule)
   {
      letterNumberPuncRule = rule;
   }

   public int getLetterNumberPuncRule()
   {
      return letterNumberPuncRule;
   }

   public void setDateFormat(String format)
   {
      dateFormat = format;
   }

   public String getDateFormat()
   {
      return dateFormat;
   }

   public void setDateLocale(String langTag)
   {
      dateLocale = langTag;
   }

   public String getDateLocaleSetting()
   {
      return dateLocale;
   }

   public Locale getLocale()
   {
      if (sortMethod.equals("locale") || sortMethod.equals("locale-reverse"))
      {
         return Locale.getDefault();
      }
      else
      {
         String method = sortMethod;

         int idx = method.lastIndexOf("-reverse");

         if (idx > -1)
         {
            method = method.substring(0, idx);
         }

         return Locale.forLanguageTag(method);
      }
   }

   public Locale getDateLocale()
   {
      if (dateLocale.equals("locale"))
      {
         return Locale.getDefault();
      }
      else
      {
         return Locale.forLanguageTag(dateLocale);
      }
   }

   public void setNumberLocale(String locale)
   {
      numberLocale = locale;
   }

   public Locale getNumberLocale()
   {
      if (numberLocale.equals("locale"))
      {
         return Locale.getDefault();
      }
      else
      {
         return Locale.forLanguageTag(numberLocale);
      }
   }

   public void setDocLocale(String locale)
   {
      if ("doc".equals(sortMethod))
      {
         sortMethod = locale;
      }
      else if ("doc-reverse".equals(sortMethod))
      {
         sortMethod = locale+"-reverse";
      }

      if ("doc".equals(dateLocale))
      {
         dateLocale = locale;
      }

      if ("doc".equals(numberLocale))
      {
         numberLocale = locale;
      }
   }

   public void setNumberFormat(String format)
   {
      numberFormat = format;
   }

   public String getNumberFormat()
   {
      return numberFormat;
   }

   public void verboseMessages(Bib2Gls bib2gls)
   {
      verboseMessages(bib2gls, "sort");
   }

   public void verboseMessages(Bib2Gls bib2gls, String id)
   {
      bib2gls.verbose(bib2gls.getMessage(
       String.format("message.%s.mode", id),
       sortMethod == null ? "none" : sortMethod));

      bib2gls.verbose(bib2gls.getMessage(
       String.format("message.%s.field", id),
       sortField));

      if (isDateOrTimeSort())
      {
         bib2gls.verbose(bib2gls.getMessage(
          String.format("message.%s.date.locale", id),
          getDateLocale().toLanguageTag()));

         if (dateFormat != null)
         {
            bib2gls.verbose(bib2gls.getMessage(
             String.format("message.%s.date.format", id),
             dateFormat));
         }
      }
      else if (isLocaleNumeric())
      {
         bib2gls.verbose(bib2gls.getMessage(
          String.format("message.%s.numeric.locale", id),
          getNumberLocale().toLanguageTag()));

         if (numberFormat != null)
         {
            bib2gls.verbose(bib2gls.getMessage(
             String.format("message.%s.numeric.format", id),
             numberFormat));
         }
      }
   }

   public void setCollatorStrength(int strength)
   {
      collatorStrength = strength;
   }

   public int getCollatorStrength()
   {
      return collatorStrength;
   }

   public void setCollatorDecomposition(int decomposition)
   {
      collatorDecomposition = decomposition; 
   }

   public int getCollatorDecomposition()
   {
      return collatorDecomposition;
   }

   public void setBreakPoint(int type)
   {
      breakPoint = type;
   }

   public int getBreakPoint()
   {
      return breakPoint;
   }

   public void setBreakPointMarker(String marker)
   {
      breakPointMarker = marker;
   }

   public String getBreakPointMarker()
   {
      return breakPointMarker;
   }


   public void setSuffixOption(int option)
   {
      sortSuffixOption=option;
   }

   public int getSuffixOption()
   {
      return sortSuffixOption;
   }

   public void setSuffixMarker(String marker)
   {
      sortSuffixMarker = marker;
   }

   public String getSuffixMarker()
   {
      return sortSuffixMarker;
   }

   private String sortMethod=null;
   private String sortField="sort";
   private String collationRule=null;
   private String dateLocale="locale";
   private String dateFormat=null;
   private String numberLocale="locale";
   private String numberFormat=null;

   private int letterNumberRule
     = Bib2GlsEntryLetterNumberComparator.NUMBER_BEFORE_LOWER;
   private int letterNumberPuncRule
     = Bib2GlsEntryLetterNumberComparator.PUNCTUATION_SPACE_FIRST;

   private int collatorStrength=Collator.PRIMARY;
   private int collatorDecomposition=Collator.CANONICAL_DECOMPOSITION;
   private int breakPoint = Bib2GlsEntryComparator.BREAK_WORD;
   private String breakPointMarker = "|";

   private int sortSuffixOption=SORT_SUFFIX_NON_UNIQUE;

   private String sortSuffixMarker = "";

   public static final int SORT_SUFFIX_NONE=0;
   public static final int SORT_SUFFIX_NON_UNIQUE=1;

}
