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
import java.util.Vector;
import java.util.Comparator;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.text.CollationElementIterator;
import java.text.ParseException;
import java.text.CollationKey;
import java.text.Normalizer;
import java.text.BreakIterator;
import java.util.HashMap;

import com.dickimawbooks.texparserlib.bib.BibValueList;
import com.dickimawbooks.bibgls.common.Bib2GlsException;

public class Bib2GlsEntryComparator extends SortComparator
{
   public Bib2GlsEntryComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    SortSettings settings, String sortField, String groupField, 
    String entryType, boolean overrideType)
   throws ParseException
   {
      super(bib2gls, entries, settings, sortField, groupField,
        entryType, overrideType);

      int breakPoint = settings.getBreakPoint();

      if (settings.isCustom())
      {
         locale = bib2gls.getCurrentResource().getResourceLocale();

         if (locale == null)
         {
            locale = Locale.getDefault();
         }

         collator = new RuleBasedCollator(settings.getCollationRule());

         if (breakPoint != BREAK_NONE)
         {
            setBreakPoint(breakPoint);
         }
      }
      else
      {
         locale = settings.getLocale();

         collator = Collator.getInstance(locale);

         setBreakPoint(breakPoint);
      }

      collator.setStrength(settings.getCollatorStrength());
      collator.setDecomposition(settings.getCollatorDecomposition());

      if (collator instanceof RuleBasedCollator && bib2gls.isDebuggingOn())
      {
         bib2gls.logMessage(bib2gls.getMessage("message.collator.rules",
           ruleString(((RuleBasedCollator)collator).getRules())));
      }
   }

   private String ruleString(String rules)
   {
      StringBuilder builder = new StringBuilder(rules.length());

      int colCount = 0;

      for (int i = 0; i < rules.length(); )
      {
         int cp = rules.codePointAt(i);
         i += Character.charCount(cp);

         int type = Character.getType(cp);

         if (type == Character.CURRENCY_SYMBOL
          || type == Character.DASH_PUNCTUATION
          || type == Character.DECIMAL_DIGIT_NUMBER
          || type == Character.END_PUNCTUATION
          || type == Character.FINAL_QUOTE_PUNCTUATION
          || type == Character.INITIAL_QUOTE_PUNCTUATION
          || type == Character.LETTER_NUMBER
          || type == Character.LOWERCASE_LETTER
          || type == Character.MATH_SYMBOL
          || type == Character.OTHER_LETTER
          || type == Character.OTHER_NUMBER
          || type == Character.OTHER_PUNCTUATION
          || type == Character.OTHER_SYMBOL
          || type == Character.START_PUNCTUATION
          || type == Character.TITLECASE_LETTER
          || type == Character.UPPERCASE_LETTER)
         {
            colCount++;

            if (colCount > RULE_STRING_BREAK_AFTER 
                 && (cp == '=' || cp == ';' || cp == '<'))
            {
               builder.append(String.format("%n"));
               colCount = 0;
            }

            builder.appendCodePoint(cp);
         }
         else
         {
            char[] chrs = Character.toChars(cp);

            for (char c : chrs)
            {
               builder.append(String.format("\\u%04x", (int)c));
               colCount+=6;
            }
         }
      }

      return builder.toString();
   }

   private void setBreakPoint(int breakPoint)
   {
      switch (breakPoint)
      {
         case BREAK_NONE:
         case BREAK_UPPER_NOTLOWER:
         case BREAK_UPPER_UPPER:
            breakIterator = null;
         break;
         case BREAK_WORD:
         case BREAK_UPPER_NOTLOWER_WORD:
         case BREAK_UPPER_UPPER_WORD:
            breakIterator = BreakIterator.getWordInstance(locale);
         break;
         case BREAK_CHAR:
            breakIterator = BreakIterator.getCharacterInstance(locale);
         break;
         case BREAK_SENTENCE:
            breakIterator = BreakIterator.getSentenceInstance(locale);
         break;
         default:
            throw new IllegalArgumentException("Invalid break identifier: "
              +breakPoint);
      }
   }

   public int getBreakPoint()
   {
      return settings.getBreakPoint();
   }

   public String getBreakPointMarker()
   {
      return settings.getBreakPointMarker();
   }

   @Override
   protected long getDefaultGroupId(Bib2GlsEntry entry,
     int codePoint, Object sortValue)
   {
      String value = sortValue.toString();

      if (value.isEmpty())
      {
         return 0L;
      }
      else
      {
         return codePoint;
      }
   }

   @Override
   protected GroupTitle createDefaultGroupTitle(int codePoint, 
     Object sortValue, String type, String parent)
   {
      String value = sortValue.toString();

      if (value.isEmpty())
      {
         return new EmptyGroupTitle(bib2gls, type, parent);
      }

      String str = new String(Character.toChars(codePoint));
      String grp = str;

      if (Character.isAlphabetic(codePoint))
      {
         grp = str.toUpperCase(locale);
         int cp = grp.codePointAt(0);

         return new GroupTitle(bib2gls, grp, str, cp, type, parent);
      }
      else
      {
         if (str.equals("\\") || str.equals("{") ||
          str.equals("}"))
         {
            str = "\\char`\\"+str;
         }

         return new OtherGroupTitle(bib2gls, str, codePoint, type, parent);
      }
   }

   protected String updateSortValue(Bib2GlsEntry entry, 
      Vector<Bib2GlsEntry> entries)
   {
      String value = super.updateSortValue(entry, entries);

      String id = entry.getId();

      String grp = null;

      if (settings.isBreakAtOn(entry))
      {
         value = breakPoints(value).toString();

         bib2gls.debug(bib2gls.getMessage("message.break.points",
              value));
      }

      entry.putField(sortStorageField, value);

      CollationKey key = collator.getCollationKey(value);
      entry.setCollationKey(key);

      GlsResource resource = bib2gls.getCurrentResource();

      String type = getType(entry);

      if (resource.useGroupField(entry, entries))
      {
         String groupFieldValue = null;
         GroupTitle grpTitle = null;

         if (entry.getFieldValue(groupField) != null)
         {
            // don't overwrite
         }
         else if (value.isEmpty())
         {
            grpTitle = resource.getGroupTitle(type, 0L, entry.getParent());
            String args;

            if (grpTitle == null)
            {
               grpTitle = new EmptyGroupTitle(bib2gls, type, entry.getParent());
               resource.putGroupTitle(grpTitle, entry);
               args = grpTitle.format();
            }
            else
            {
               args = grpTitle.format(value);
            }

            groupFieldValue = String.format("\\%s%s", grpTitle.getCsLabelName(), args);
         }
         else if (collator instanceof RuleBasedCollator)
         {
            CollationElementIterator it =
              ((RuleBasedCollator)collator).getCollationElementIterator(value);

            int elem = it.next();
            int offset = it.getOffset();
            int start = 0;

            while (elem == 0)
            {
               start = offset;
               elem = it.next();
               offset = it.getOffset();
            }

            String str;
            int cp = 0;

            if (elem == CollationElementIterator.NULLORDER)
            {
               bib2gls.debug(bib2gls.getMessage("message.no.collation.element",
                 value));

               if (collator.getStrength() == Collator.PRIMARY)
               {
                  str = Normalizer.normalize(value, Normalizer.Form.NFD);
                  str = str.replaceAll("\\p{M}", "");
                  it =
                    ((RuleBasedCollator)collator).getCollationElementIterator(str);

                  elem = it.next();
                  offset = it.getOffset();
               }
               else
               {
                  cp = value.codePointAt(0);
                  offset = Character.charCount(cp);
                  str = value;
                  elem = cp;
               }

               str = str.substring(0, offset);
               grp = str;
            }
            else
            {
               str = value.substring(start, offset==0?1:offset);

               grp = str;

               int strength = collator.getStrength();

               collator.setStrength(Collator.PRIMARY);

               String norm = Normalizer.normalize(
                      str.toLowerCase(locale), Normalizer.Form.NFD);
               norm = norm.replaceAll("\\p{M}", "");

               if (collator.compare(str, norm) == 0)
               {
                  grp = norm;
               }

               collator.setStrength(strength);
            }

            if (!grp.isEmpty())
            {
               cp = grp.codePointAt(0);
            }

            if (settings.getGroupFormation() != SortSettings.GROUP_DEFAULT)
            {
               grp = setGroupTitle(entry, cp, value, str, type);
            }
            else
            {
               // The Dutch ij digraph should have both letters
               // converted to upper case. Other digraphs only have the
               // first letter converted. Rather than hard-coding
               // for just "ij", allow exceptions to be provided
               // in the language resource file. For example
               // <entry key="grouptitle.case.ij">IJ</entry>
   
               String grpCase = resource.getLocalisationTextIfExists(
                 "grouptitle.case", locale, grp);
   
               if (grpCase != null)
               {
                  grp = grpCase;
                  cp = Character.toTitleCase(cp);
               }
               else if (Character.isAlphabetic(cp))
               {
                  int titleCodePoint = Character.toTitleCase(cp);
   
                  if (cp == titleCodePoint && Character.isLowerCase(cp))
                  {
                     grpCase = grp.toLowerCase(locale);
                     grpCase = resource.toSentenceCase(grpCase, locale).toString();

                     titleCodePoint = grpCase.codePointAt(0);
                  }
                  else
                  {
                     grp = String.format("%c%s", titleCodePoint,
                        grp.substring(Character.charCount(cp)).toLowerCase(locale));
                  }

                  cp = titleCodePoint;
               }
   
               if (Character.isAlphabetic(cp))
               {
                  if (collator.getStrength() != Collator.PRIMARY)
                  {
                     elem = cp;
                  }
   
                  grpTitle = resource.getGroupTitle(type, elem,
                    entry.getParent());
                  String args;
   
                  if (grpTitle == null)
                  {
                     grpTitle = new GroupTitle(bib2gls, grp, str, elem, type, entry.getParent());
                     resource.putGroupTitle(grpTitle, entry);
                     args = grpTitle.format();
                  }
                  else
                  {
                     args = grpTitle.format(str);
   
                     if (grpTitle.getTitle().matches(".*[^\\p{ASCII}].*")
                         && grp.matches("\\p{ASCII}+"))
                     {
                        grpTitle.setTitle(grp);
                     }
                  }
   
                  groupFieldValue = String.format("\\%s%s",
                     grpTitle.getCsLabelName(), args);
               }
               else
               {
                  if (str.equals("\\") || str.equals("{") ||
                   str.equals("}"))
                  {
                     str = "\\char`\\"+str;
                  }
   
                  grpTitle = resource.getGroupTitle(type, elem,
                     entry.getParent());
                  String args;
   
                  if (grpTitle == null)
                  {
                     grpTitle = new OtherGroupTitle(bib2gls, str, elem, type, entry.getParent());
                     resource.putGroupTitle(grpTitle, entry);
                     args = grpTitle.toString();
                  }
                  else
                  {
                     args = grpTitle.format(str);
                  }
   
                  groupFieldValue = 
                    String.format("\\%s%s", grpTitle.getCsLabelName(), args);
               }
            }
         }
         else
         {
            int codePoint = value.codePointAt(0);

            String str = new String(Character.toChars(codePoint));

            if (Character.isAlphabetic(codePoint))
            {
               grp = str.toUpperCase(locale);
               codePoint = grp.codePointAt(0);
            }

            grp = setGroupTitle(entry, codePoint, value, str, type);
         }

         if (groupFieldValue != null)
         {
            resource.assignGroupField(entry, groupField, groupFieldValue,
              grpTitle);
         }
      }

      if (bib2gls.isVerbose())
      {
         StringBuilder keyList = new StringBuilder();

         byte[] bits = key.toByteArray();

         for (byte b : bits)
         {
            if (keyList.length() > 0)
            {
               keyList.append(' ');
            }

            keyList.append(b);
         }

         if (grp == null)
         {
            bib2gls.verbose(String.format("%s -> '%s' [%s]", 
              id, value, keyList));
         }
         else
         {
            bib2gls.verbose(String.format("%s -> '%s' (%s) [%s]", 
              id, value, grp, keyList));
         }
      }

      return value;
   }

   @Override
   public int compareElements(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      CollationKey key1 = entry1.getCollationKey();
      CollationKey key2 = entry2.getCollationKey();

      return key1.compareTo(key2);
   }

   public Collator getCollator()
   {
      return collator;
   }

   public void sortEntries() throws Bib2GlsException
   {
      bib2gls.debug(bib2gls.getMessage("message.setting.sort",
        collator.getStrength(), collator.getDecomposition()));

      super.sortEntries();
   }

   protected CharSequence breakUpperNotLower(String target)
   {
      StringBuffer buff = new StringBuffer();

      for (int i = 0, n = target.length(); i < n; )
      {
         int codePoint = target.codePointAt(i);
         i += Character.charCount(codePoint);

         buff.appendCodePoint(codePoint);

         if (Character.isUpperCase(codePoint))
         {
            int nextCodePoint = (i < n ? target.codePointAt(i) : -1);

            if (!Character.isLowerCase(nextCodePoint))
            {
               buff.append(getBreakPointMarker());
            }
         }
      }

      return buff;
   }

   protected CharSequence breakUpperUpper(String target)
   {
      StringBuffer buff = new StringBuffer();

      for (int i = 0, n = target.length(); i < n; )
      {
         int codePoint = target.codePointAt(i);
         i += Character.charCount(codePoint);

         buff.appendCodePoint(codePoint);

         if (Character.isUpperCase(codePoint))
         {
            int nextCodePoint = (i < n ? target.codePointAt(i) : -1);

            if (Character.isUpperCase(nextCodePoint))
            {
               buff.append(getBreakPointMarker());
            }
         }
      }

      return buff;
   }

   public CharSequence breakPoints(String target)
   {
      switch (getBreakPoint())
      {
         case BREAK_UPPER_NOTLOWER:
         case BREAK_UPPER_NOTLOWER_WORD:
            target = breakUpperNotLower(target).toString();
         break;
         case BREAK_UPPER_UPPER:
         case BREAK_UPPER_UPPER_WORD:
            target = breakUpperUpper(target).toString();
         break;
      }

      if (breakIterator == null)
      {
         return target;
      }

      StringBuffer buff = new StringBuffer();

      breakIterator.setText(target);

      int start = breakIterator.first();
      int end = breakIterator.next();

      while (end != BreakIterator.DONE)
      {
          String word = target.substring(start,end);

          int codePoint = word.codePointAt(0);

          if (Character.isLetterOrDigit(codePoint))
          {
             buff.append(word);
             buff.append(getBreakPointMarker());
          }

          start = end;
          end = breakIterator.next();
      }

      return buff;
   }

   private Collator collator;

   private BreakIterator breakIterator=null;

   protected Locale locale;

   public static final int BREAK_NONE=0, BREAK_WORD=1, BREAK_CHAR=2,
     BREAK_SENTENCE=3, BREAK_UPPER_NOTLOWER=4, BREAK_UPPER_UPPER=5,
     BREAK_UPPER_NOTLOWER_WORD=6, BREAK_UPPER_UPPER_WORD=7;

   public static final int RULE_STRING_BREAK_AFTER=60;
}
