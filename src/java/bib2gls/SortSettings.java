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
      return "custom".equals(sortMethod);
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
      if (sortMethod.equals("locale"))
      {
         return Locale.getDefault();
      }
      else
      {
         return Locale.forLanguageTag(sortMethod);
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

   private String sortMethod=null;
   private String sortField="sort";
   private String collationRule=null;
   private String dateLocale="locale";
   private String dateFormat=null;
   private String numberLocale="locale";
   private String numberFormat=null;
   private int letterNumberRule
     = Bib2GlsEntryLetterNumberComparator.NUMBER_BEFORE_LOWER;
}