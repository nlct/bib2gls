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
import java.util.HashMap;

import com.dickimawbooks.texparserlib.bib.BibValueList;

public class Bib2GlsEntryComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries,
    Locale locale, String sortField,
    int strength, int decomposition)
   {
      this.sortField = sortField;
      this.bib2gls = bib2gls;
      this.entries = entries;

      isDutch = locale.getLanguage().equals(new Locale("nl").getLanguage());

      collator = Collator.getInstance(locale);
      collator.setStrength(strength);
      collator.setDecomposition(decomposition);

      if (collator instanceof RuleBasedCollator && bib2gls.getDebugLevel() > 0)
      {
         bib2gls.debug(bib2gls.getMessage("message.collator.rules",
           ((RuleBasedCollator)collator).getRules()));
      }
   }

   public Bib2GlsEntryComparator(Bib2Gls bib2gls,
    Vector<Bib2GlsEntry> entries, String sortField,
    int strength, int decomposition, String rules)
   throws ParseException
   {
      this.sortField = sortField;
      this.bib2gls = bib2gls;
      this.entries = entries;

      collator = new RuleBasedCollator(rules);
      collator.setStrength(strength);
      collator.setDecomposition(decomposition);

      String docLocale = bib2gls.getDocDefaultLocale();

      if (docLocale != null)
      {
         Locale locale = Locale.forLanguageTag(docLocale);
         isDutch = locale.getLanguage().equals(new Locale("nl").getLanguage());
      }

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.debug(bib2gls.getMessage("message.collator.rules",
           ((RuleBasedCollator)collator).getRules()));
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
      }

      entry.putField("sort", value);

      String grp = null;

      CollationKey key = collator.getCollationKey(value);
      entry.setCollationKey(key);

      if (bib2gls.useGroupField() && value.length() > 0)
      {
         if (entry.getFieldValue("group") != null)
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

               switch (collator.getStrength())
               {
                 case Collator.PRIMARY:

                    String norm = Normalizer.normalize(
                      str.toLowerCase(), Normalizer.Form.NFD);
                    norm = norm.replaceAll("\\p{M}", "");

                    if (collator.compare(str, norm) == 0)
                    {
                       grp = norm;
                    }

                 break;
                 case Collator.SECONDARY:

                    norm = str.toLowerCase();

                    if (collator.compare(str, norm) == 0)
                    {
                       grp = norm;
                    }

                 break;
               }
            }

            if (!grp.isEmpty())
            {
               cp = grp.codePointAt(0);
            }

            if (collator.getStrength() == Collator.TERTIARY)
            {
               // don't title-case the group
            }
            else if (isDutch && grp.equals("ij"))
            {
               grp = "IJ";
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

               GlsResource resource = bib2gls.getCurrentResource();

               GroupTitle grpTitle = resource.getGroupTitle(elem);
               String args;

               if (grpTitle == null)
               {
                  grpTitle = new GroupTitle(grp, str, elem);
                  resource.putGroupTitle(grpTitle);
                  args = grpTitle.toString();
               }
               else
               {
                  args = String.format("{%s}{%s}{%d}", grp, str, elem);

                  if (grpTitle.getTitle().matches(".*[^\\p{ASCII}].*")
                      && grp.matches("\\p{ASCII}+"))
                  {
                     grpTitle.setTitle(grp);
                  }
               }

               entry.putField("group", 
                 String.format("\\bibglslettergroup%s", args));
            }
            else
            {
               if (str.equals("\\") || str.equals("{") ||
                str.equals("}"))
               {
                  str = "\\char`\\"+str;
               }

               entry.putField("group", 
                 String.format("\\bibglsothergroup{%s}{%d}", str, elem));
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

               GlsResource resource = bib2gls.getCurrentResource();

               GroupTitle grpTitle = resource.getGroupTitle(cp);
               String args;

               if (grpTitle == null)
               {
                  grpTitle = new GroupTitle(grp, str, cp);
                  resource.putGroupTitle(grpTitle);
                  args = grpTitle.toString();
               }
               else
               {
                  args = String.format("{%s}{%s}{%d}", grpTitle.getTitle(), 
                    str, cp);
               }

               entry.putField("group", 
                 String.format("\\bibglslettergroup%s", args));
            }
            else
            {
               if (str.equals("\\") || str.equals("{") ||
                str.equals("}"))
               {
                  str = "\\char`\\"+str;
               }

               entry.putField("group", 
                 String.format("\\bibglsothergroup{%s}{%X}", 
                               str, codePoint));
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

      int n1 = entry1.getHierarchyCount();
      int n2 = entry2.getHierarchyCount();

      int n = Integer.min(n1, n2);

      if (n1 == n2 && entry1.getId().equals(entry2.getId()))
      {
         return 0;
      }

      for (int i = 0; i < n; i++)
      {
         Bib2GlsEntry e1 = entry1.getHierarchyElement(i);
         Bib2GlsEntry e2 = entry2.getHierarchyElement(i);

         int result = e1.getCollationKey().compareTo(e2.getCollationKey());

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

   public void sortEntries() throws Bib2GlsException
   {
      bib2gls.debug(bib2gls.getMessage("message.setting.sort",
        collator.getStrength(), collator.getDecomposition()));

      for (Bib2GlsEntry entry : entries)
      {
         entry.updateHierarchy(entries);
         updateSortValue(entry, entries);
      }

      entries.sort(this);
   }


   private String sortField;

   private Collator collator;

   private boolean isDutch = false;

   private Bib2Gls bib2gls;

   private Vector<Bib2GlsEntry> entries;
}
