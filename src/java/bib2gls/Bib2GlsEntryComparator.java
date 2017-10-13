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

public class Bib2GlsEntryComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    SortSettings settings, String sortField, String groupField, String entryType)
   throws ParseException
   {
      this.sortField = sortField;
      this.groupField = groupField;
      this.entryType = entryType;
      this.bib2gls = bib2gls;
      this.entries = entries;
      this.sortSuffixMarker = settings.getSuffixMarker();
      this.sortSuffixOption = settings.getSuffixOption();

      int breakPoint = settings.getBreakPoint();
      String breakMarker = settings.getBreakPointMarker();

      if (settings.isCustom())
      {
         collator = new RuleBasedCollator(settings.getCollationRule());

         if (breakPoint != BREAK_NONE)
         {
            String docLocale = bib2gls.getDocDefaultLocale();
   
            setBreakPoint(breakPoint, breakMarker,
                          Locale.forLanguageTag(docLocale));
         }
      }
      else
      {
         Locale locale = settings.getLocale();

         collator = Collator.getInstance(locale);

         setBreakPoint(breakPoint, breakMarker, locale);
      }

      collator.setStrength(settings.getCollatorStrength());
      collator.setDecomposition(settings.getCollatorDecomposition());

      if (collator instanceof RuleBasedCollator && bib2gls.getDebugLevel() > 0)
      {
         bib2gls.debug(bib2gls.getMessage("message.collator.rules",
           ((RuleBasedCollator)collator).getRules()));
      }
   }

   private void setBreakPoint(int breakPoint, String breakMarker,
      Locale locale)
   {
      breakPointMarker = breakMarker;

      switch (breakPoint)
      {
         case BREAK_NONE:
            breakIterator = null;
         break;
         case BREAK_WORD:
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

   private String updateSortValue(Bib2GlsEntry entry, 
      Vector<Bib2GlsEntry> entries)
   {
      String id = entry.getId();

      String value = null;

      if (sortField.equals("id"))
      {
         value = id;
      }
      else
      {
         value = entry.getFieldValue(sortField);

         BibValueList list = entry.getField(sortField);

         if (value == null)
         {
            value = entry.getFallbackValue(sortField);
            list = entry.getFallbackContents(sortField);
         }

         if (value == null)
         {
            value = id;

            bib2gls.debug(bib2gls.getMessage("warning.no.default.sort",
              id, sortField));
         }
         else if (bib2gls.useInterpreter() && list != null
                   && value.matches(".*[\\\\\\$\\{\\}].*"))
         {
            value = bib2gls.interpret(value, list);
         }

         if (sortSuffixOption != SortSettings.SORT_SUFFIX_NONE)
         {
            String suff = sortSuffix(value, entry);

            if (suff != null)
            {
               suff = sortSuffixMarker + suff;

               bib2gls.verbose(bib2gls.getMessage("message.sort_suffix",
                 suff, value, id));

               value += suff;
            }
         }
      }

      entry.putField("sort", value);

      String grp = null;

      value = breakPoints(value).toString();

      if (breakIterator != null)
      {
         bib2gls.debug(bib2gls.getMessage("message.break.points",
           value));
      }

      CollationKey key = collator.getCollationKey(value);
      entry.setCollationKey(key);

      GlsResource resource = bib2gls.getCurrentResource();

      String type = entryType;

      if (type == null)
      {
         type = resource.getType(entry);

         if (type == null)
         {
            type = "";
         }
      }

      if (bib2gls.useGroupField() && value.length() > 0
           && !entry.hasParent())
      {
         if (entry.getFieldValue(groupField) != null)
         {
            // don't overwrite
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
                      str.toLowerCase(), Normalizer.Form.NFD);
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

            // The Dutch ij digraph should have both letters
            // converted to upper case. Other digraphs only have the
            // first letter converted. Rather than hard-coding
            // for just "ij", allow other exceptions to be provided
            // in the language resource file. For example
            // <entry key="grouptitle.case.ij">IJ</entry>

            String grpCase = bib2gls.getMessageIfExists(
              String.format("grouptitle.case.%s", grp));

            if (grpCase != null)
            {
               grp = grpCase;
               cp = Character.toTitleCase(cp);
            }
            else
            {
               if (Character.isAlphabetic(cp))
               {
                  int titleCodePoint = Character.toTitleCase(cp);

                  if (titleCodePoint > 0xffff)
                  {
                     grp = String.format("%c%c%s",
                          Character.highSurrogate(titleCodePoint),
                          Character.lowSurrogate(titleCodePoint),
                          grp.substring(Character.charCount(cp)).toLowerCase());
                  }
                  else
                  {
                     grp = String.format("%c%s", titleCodePoint,
                        grp.substring(Character.charCount(cp)).toLowerCase());
                  }

                  cp = titleCodePoint;
               }
            }

            if (Character.isAlphabetic(cp))
            {
               if (collator.getStrength() != Collator.PRIMARY)
               {
                  elem = cp;
               }

               GroupTitle grpTitle = resource.getGroupTitle(type, elem);
               String args;

               if (grpTitle == null)
               {
                  grpTitle = new GroupTitle(grp, str, elem, type);
                  resource.putGroupTitle(grpTitle, entry);
                  args = grpTitle.toString();
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

               GroupTitle grpTitle = resource.getGroupTitle(type, elem);
               String args;

               if (grpTitle == null)
               {
                  grpTitle = new OtherGroupTitle(str, elem, type);
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
         else
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

            if (Character.isAlphabetic(codePoint))
            {
               grp = str.toUpperCase();
               int cp = grp.codePointAt(0);

               GroupTitle grpTitle = resource.getGroupTitle(type, cp);
               String args;

               if (grpTitle == null)
               {
                  grpTitle = new GroupTitle(grp, str, cp,
                    resource.getType(entry));
                  resource.putGroupTitle(grpTitle, entry);
                  args = grpTitle.toString();
               }
               else
               {
                  args = String.format("{%s}{%s}{%d}{%s}", grpTitle.getTitle(), 
                    str, cp, type);
               }

               entry.putField(groupField, 
                 String.format("\\bibglslettergroup%s", args));
            }
            else
            {
               if (str.equals("\\") || str.equals("{") ||
                str.equals("}"))
               {
                  str = "\\char`\\"+str;
               }

               entry.putField(groupField, 
                 String.format("\\bibglsothergroup{%s}{%X}{%s}", 
                               str, codePoint, type));
            }
         }
      }

      if (bib2gls.getVerboseLevel() > 0)
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

   public int compare(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      if (bib2gls.getCurrentResource().flattenSort())
      {
         return entry1.getCollationKey().compareTo(entry2.getCollationKey());
      }

      if (entry1.getId().equals(entry2.getParent()))
      {
         // entry1 is the parent of entry2
         // so entry1 must come before (be less than) entry2

         return -1;
      }

      if (entry2.getId().equals(entry1.getParent()))
      {
         // entry2 is the parent of entry1
         // so entry1 must come after (be greater than) entry2

         return 1;
      }

      int n1 = entry1.getHierarchyCount();
      int n2 = entry2.getHierarchyCount();

      int n = Integer.min(n1, n2);

      for (int i = 0; i < n; i++)
      {
         Bib2GlsEntry e1 = entry1.getHierarchyElement(i);
         Bib2GlsEntry e2 = entry2.getHierarchyElement(i);

         int result = e1.getCollationKey().compareTo(e2.getCollationKey());

         if (bib2gls.getDebugLevel() > 1)
         {
            bib2gls.logAndPrintMessage(String.format("%s %c %s",
              e1.getFieldValue("sort"),
              result == 0 ? '=' : (result < 0 ? '<' : '>'),
              e2.getFieldValue("sort")));
         }

         if (result != 0)
         {
            return result;
         }
      }

      return (n1 == n2 ? 0 : (n1 < n2 ? -1 : 1));
   }

   public Collator getCollator()
   {
      return collator;
   }

   private String sortSuffix(String sort, Bib2GlsEntry entry)
   {
      if (sortCount == null) return null;

      String parentId = entry.getParent();

      /*
       Non-unique sort keys aren't a problem across different 
       hierarchical levels. The biggest problem occurs when two
       or more siblings have the same sort value and one or more 
       of the siblings has child entries, as this can cause all
       the children to be clumped together after the last of the 
       sibling set.

       The sortCount hash map keeps track of all the sort values 
       used for a particular level. The simplest method is to use 
       the sort value as the key for top-level (parentless) entries 
       and use a combination of the parent label and the sort value 
       for sub-entries. To avoid the odd possibility of a top-level
       sort value coincidentally matching a sub-entry's parent id
       and sort value combination, the key to the hash map
       use a control code as a separator for sub-entries since there 
       shouldn't be control codes in the label.
       (0x1F is the unit separator control code.)
      */

      String key = (parentId == null ? sort
                    : String.format("%s\u001f%s", parentId, sort));

      Integer num = sortCount.get(key);

      if (num == null)
      {
         sortCount.put(key, Integer.valueOf(0));
         return null;
      }

      bib2gls.verbose(bib2gls.getMessage("message.non_unique_sort",
        sort, entry.getOriginalId()));

      num = Integer.valueOf(num.intValue()+1);

      sortCount.put(key, num);

      return num.toString();
   }

   public void sortEntries() throws Bib2GlsException
   {
      bib2gls.debug(bib2gls.getMessage("message.setting.sort",
        collator.getStrength(), collator.getDecomposition()));

      if (sortSuffixOption == SortSettings.SORT_SUFFIX_NON_UNIQUE)
      {
         sortCount =  new HashMap<String,Integer>();
      }

      for (Bib2GlsEntry entry : entries)
      {
         entry.updateHierarchy(entries);
         updateSortValue(entry, entries);
      }

      entries.sort(this);
   }

   public CharSequence breakPoints(String target)
   {
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

          if (Character.isLetterOrDigit(word.charAt(0)))
          {
             buff.append(word);
             buff.append(breakPointMarker);
          }

          start = end;
          end = breakIterator.next();
      }

      return buff;
   }

   private String sortField, groupField, entryType;

   private Collator collator;

   private BreakIterator breakIterator;

   private String breakPointMarker="|";

   private String sortSuffixMarker;

   private int sortSuffixOption;

   private HashMap<String,Integer> sortCount;

   private Bib2Gls bib2gls;

   private Vector<Bib2GlsEntry> entries;

   public static final int BREAK_NONE=0, BREAK_WORD=1, BREAK_CHAR=2,
     BREAK_SENTENCE=3;
}
