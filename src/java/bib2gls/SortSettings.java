/*
    Copyright (C) 2017-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.bib2gls;

import java.util.Locale;
import java.util.IllformedLocaleException;
import java.util.MissingResourceException;
import java.util.Vector;
import java.util.HashMap;
import java.util.regex.Pattern;

import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class SortSettings
{
   public SortSettings(GlsResource resource)
   {
      this(null, resource);
   }

   public SortSettings(String method, GlsResource resource)
   {
      setMethod(method);
      this.resource = resource;
      this.bib2gls = resource.getBib2Gls();
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

      if (method.equals("use")
       || method.equals("doc")
       || method.equals("locale")
       || method.equals("resource")
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
       || method.equals("numberformat")
       || method.equals("recordcount"))
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
      return sortMethod != null && !sortMethod.startsWith("use")
       && !sortMethod.equals("none");
   }

   public boolean requiresSortField()
   {
      return requiresSorting() && !sortMethod.startsWith("recordcount");
   }

   public boolean isOrderOfRecords()
   {
      return "use".equals(sortMethod) || "use-reverse".equals(sortMethod);
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

   public boolean isRecordCount()
   {
      if (sortMethod == null) return false;

      return sortMethod.startsWith("recordcount");
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

   public String getMethodName()
   {
      return sortMethod == null ? "none" : sortMethod;
   }


   public String getUnderlyingMethod()
   {
      if (sortMethod == null) return null;

      if (sortMethod.endsWith("-reverse"))
      {
         return sortMethod.substring(0, sortMethod.length()-8);
      }

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

   public static DateFormat getDateFormat(Locale locale, String format, boolean hasDate, boolean hasTime)
   {
      DateFormat dateFormat;

      int type = DateFormat.DEFAULT;

      if (format == null || "default".equals(format))
      {
         type = DateFormat.DEFAULT;
         format = null;
      }
      else if (format.equals("full"))
      {
         type = DateFormat.FULL;
         format = null;
      }
      else if (format.equals("long"))
      {
         type = DateFormat.LONG;
         format = null;
      }
      else if (format.equals("medium"))
      {
         type = DateFormat.MEDIUM;
         format = null;
      }
      else if (format.equals("short"))
      {
         type = DateFormat.SHORT;
         format = null;
      }

      if (format == null)
      {
         if (hasDate && hasTime)
         {
            if (locale == null)
            {
               dateFormat = DateFormat.getDateTimeInstance(type, type);
            }
            else
            {
               dateFormat = DateFormat.getDateTimeInstance(type, type, locale);
            }
         }
         else if (hasDate)
         {
            if (locale == null)
            {
               dateFormat = DateFormat.getDateInstance(type);
            }
            else
            {
               dateFormat = DateFormat.getDateInstance(type, locale);
            }
         }
         else if (hasTime)
         {
            if (locale == null)
            {
               dateFormat = DateFormat.getTimeInstance(type);
            }
            else
            {
               dateFormat = DateFormat.getTimeInstance(type, locale);
            }
         }
         else
         {
             throw new IllegalArgumentException(
                "Can't have both date=false and time=false");
         }
      }
      else
      {
         if (locale == null)
         {
            dateFormat = new SimpleDateFormat(format);
         }
         else
         {
            dateFormat = new SimpleDateFormat(format, locale);
         }
      }

      return dateFormat;
   }

   public Locale getLocale()
   {
      if (sortMethod.equals("resource") || sortMethod.equals("resource-reverse"))
      {
         return resource.getResourceLocale();
      }
      else if (sortMethod.equals("locale") || sortMethod.equals("locale-reverse"))
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

         return bib2gls.getLocale(method);
      }
   }

   public Locale getDateLocale()
   {
      if (dateLocale.equals("resource"))
      {
         return resource.getResourceLocale();
      }
      else if (dateLocale.equals("locale"))
      {
         return Locale.getDefault();
      }
      else
      {
         return bib2gls.getLocale(dateLocale);
      }
   }

   public void setNumberLocale(String locale)
   {
      numberLocale = locale;
   }

   public Locale getNumberLocale()
   {
      if (numberLocale.equals("resource"))
      {
         return resource.getResourceLocale();
      }
      else if (numberLocale.equals("locale"))
      {
         return Locale.getDefault();
      }
      else
      {
         return bib2gls.getLocale(numberLocale);
      }
   }

   public void setDocLocale(String localeTag)
   {
      if (localeTag == null)
      {
         throw new NullPointerException();
      }

      if ("doc".equals(sortMethod))
      {
         sortMethod = localeTag;
      }
      else if ("doc-reverse".equals(sortMethod))
      {
         sortMethod = localeTag+"-reverse";
      }

      if ("doc".equals(dateLocale))
      {
         dateLocale = localeTag;
      }

      if ("doc".equals(numberLocale))
      {
         numberLocale = localeTag;
      }

      docLocale = localeTag;
   }

   public String getDocLocaleTag()
   {
      return docLocale;
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

   public void setBreakAtMatch(HashMap<String,Pattern> matchMap)
   {
      breakAtNotMatch = true;
      breakAtMatchMap = matchMap;
   }

   public void setBreakAtNotMatch(HashMap<String,Pattern> matchMap)
   {
      breakAtNotMatch = false;
      breakAtMatchMap = matchMap;
   }

   public void setBreakAtNotMatchAnd(boolean and)
   {
      breakAtMatchAnd = and;
   }

   public boolean isBreakAtOn(Bib2GlsEntry entry)
   {
      if (breakPoint == Bib2GlsEntryComparator.BREAK_NONE)
      {
         return false;
      }

      if (breakAtMatchMap == null)
      {
         return true;
      }

      return (breakAtNotMatch ^ GlsResource.notMatch(bib2gls, entry, breakAtMatchAnd,
       breakAtMatchMap));
   }

   public void setSuffixOption(int option)
   {
      switch (option)
      {
         case SORT_SUFFIX_NONE:
         case SORT_SUFFIX_NON_UNIQUE:
         case SORT_SUFFIX_FIELD:
            sortSuffixOption=option;
         break;
         default:
           throw new IllegalArgumentException(
              "Invalid identical sort suffix option: "+option);
      }
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

   public void setSuffixField(String field)
   {
      sortSuffixField = field;
   }

   public String getSuffixField()
   {
      return sortSuffixField;
   }

   public void setTrim(boolean value)
   {
      trim = value;
   }

   public boolean isTrimOn()
   {
      return trim;
   }

   public int getIdenticalSortAction()
   {
      return identicalSortAction;
   }

   public void setIdenticalSortAction(int action)
   {
      switch (action)
      {
         case IDENTICAL_SORT_NO_ACTION:
         case IDENTICAL_SORT_USE_ID:
         case IDENTICAL_SORT_USE_FIELD:
         case IDENTICAL_SORT_USE_ORIGINAL_ID:
         case IDENTICAL_SORT_USE_DEF:
         case IDENTICAL_SORT_USE_RECORD:
           identicalSortAction = action;
         break;
         default:
           throw new IllegalArgumentException(
              "Invalid identical sort action: "+action);
      }
   }

   public void setIdenticalSortField(String field)
   {
      identicalSortField = field;
   }

   public String getIdenticalSortField()
   {
      return identicalSortField;
   }

   public void setNumberPad(int pad)
   {
      numberPad = pad;
   }

   public int getNumberPad()
   {
      return numberPad;
   }

   public void setPadPlus(String marker)
   {
      padPlus = marker;
   }

   public String getPadPlus()
   {
      return padPlus;
   }

   public void setPadMinus(String marker)
   {
      padMinus = marker;
   }

   public String getPadMinus()
   {
      return padMinus;
   }

   public String getMissingFieldFallback()
   {
      return missingFieldFallback;
   }

   // check argument valid before calling this method
   public void setMissingFieldFallback(String field)
   {
      if ("".equals(field))
      {
         missingFieldFallback = null;
      }
      else
      {
         missingFieldFallback = field;
      }
   }

   public void setGroupFormation(int setting)
   {
      switch (setting)
      {
         case GROUP_DEFAULT:
         case GROUP_UNICODE_CODEPOINT:
         case GROUP_UNICODE_CATEGORY:
         case GROUP_UNICODE_SCRIPT:
         case GROUP_UNICODE_CATEGORY_SCRIPT:
            groupFormation = setting;
         break;
         default:
            throw new IllegalArgumentException("Invalid group formation: "+setting);
      }
   }

   public int getGroupFormation()
   {
      return groupFormation;
   }

   public void setRegexList(Vector<PatternReplace> list)
   {
      this.regexList = list;
   }

   public Vector<PatternReplace> getRegexList()
   {
      return regexList;
   }

   public SortSettings copy(String method, String sortField)
   {
      if (docLocale != null)
      {
         if ("doc".equals(method))
         {
            method = docLocale;
         }
         else if ("doc-reverse".equals(method))
         {
            method = docLocale+"-reverse";
         }
      }

      SortSettings settings = new SortSettings(method, resource);

      settings.setSortField(sortField);

      settings.collationRule = collationRule;
      settings.dateLocale = dateLocale;
      settings.dateFormat = dateFormat;
      settings.numberLocale = numberLocale;
      settings.numberFormat = numberFormat;
      settings.letterNumberRule = letterNumberRule;
      settings.letterNumberPuncRule = letterNumberPuncRule;
      settings.collatorStrength = collatorStrength;
      settings.collatorDecomposition = collatorDecomposition;
      settings.breakPoint = breakPoint;
      settings.breakPointMarker = breakPointMarker;
      settings.sortSuffixOption = sortSuffixOption;
      settings.sortSuffixMarker = sortSuffixMarker;
      settings.sortSuffixField = sortSuffixField;
      settings.trim = trim;
      settings.identicalSortAction = identicalSortAction;
      settings.groupFormation = groupFormation;
      settings.identicalSortField = identicalSortField;
      settings.numberPad = numberPad;
      settings.padMinus = padMinus;
      settings.padPlus = padPlus;
      settings.missingFieldFallback = missingFieldFallback;
      settings.regexList = regexList;
      settings.docLocale = docLocale;

      return settings;
   }

   private String sortMethod=null;
   private String sortField="sort";
   private String collationRule=null;
   private String dateLocale="resource";
   private String dateFormat=null;
   private String numberLocale="resource";
   private String numberFormat=null;

   private String docLocale;

   private int letterNumberRule
     = Bib2GlsEntryLetterNumberComparator.NUMBER_BETWEEN;
   private int letterNumberPuncRule
     = Bib2GlsEntryLetterNumberComparator.PUNCTUATION_SPACE_FIRST;

   private int collatorStrength=Collator.PRIMARY;
   private int collatorDecomposition=Collator.CANONICAL_DECOMPOSITION;
   private int breakPoint = Bib2GlsEntryComparator.BREAK_WORD;
   private String breakPointMarker = "|";

   private int sortSuffixOption=SORT_SUFFIX_NONE;

   private String sortSuffixMarker = "";

   private String sortSuffixField = null;

   private boolean trim=true;

   public static final int SORT_SUFFIX_NONE=0;
   public static final int SORT_SUFFIX_NON_UNIQUE=1;
   public static final int SORT_SUFFIX_FIELD=2;

   public static final int IDENTICAL_SORT_NO_ACTION=0;
   public static final int IDENTICAL_SORT_USE_ID=1;
   public static final int IDENTICAL_SORT_USE_FIELD=2;
   public static final int IDENTICAL_SORT_USE_ORIGINAL_ID=3;
   public static final int IDENTICAL_SORT_USE_DEF=4;
   public static final int IDENTICAL_SORT_USE_RECORD=5;

   private int identicalSortAction = IDENTICAL_SORT_USE_ID;

   public static final int GROUP_DEFAULT=0;
   public static final int GROUP_UNICODE_CODEPOINT=1;
   public static final int GROUP_UNICODE_CATEGORY=2;
   public static final int GROUP_UNICODE_SCRIPT=3;
   public static final int GROUP_UNICODE_CATEGORY_SCRIPT=4;

   private int groupFormation = GROUP_DEFAULT;

   private String identicalSortField = null;

   private int numberPad=0;

   private String padMinus = "<";
   private String padPlus = ">";

   private String missingFieldFallback=null;

   private Vector<PatternReplace> regexList = null;

   private HashMap<String,Pattern> breakAtMatchMap = null;
   private boolean breakAtNotMatch = false;
   private boolean breakAtMatchAnd = true;

   private Bib2Gls bib2gls;
   private GlsResource resource;
}
