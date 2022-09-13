/*
    Copyright (C) 2017-2021 Nicola L.C. Talbot
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

public class GroupTitle
{
   public GroupTitle(Bib2Gls bib2gls, String title, String actual, long id,
     String type, String parent)
   {
      this.bib2gls = bib2gls;
      this.title = title;
      this.actual = actual;
      this.id = id; 
      this.type = type;
      this.parent = parent;
   }

   public void setSupportsHierarchy(boolean supportsHierarchy, int level)
   {
      this.supportsHierarchy = supportsHierarchy;
      this.level = level;
   }

   public boolean hasHierarchySupport()
   {
      return supportsHierarchy;
   }

   public String getTitle()
   {
      return title;
   }

   public void setTitle(String title)
   {
      this.title = title;
   }

   public String getActual()
   {
      return actual;
   }

   public void setActual(String actual)
   {
      this.actual = actual;
   }

   public String getType()
   {
      return type;
   }

   public long getId()
   {
      return id;
   }

   public String getParent()
   {
      return parent;
   }

   public int getLevel()
   {
      return level;
   }

   @Override
   public boolean equals(Object other)
   {
      return other != null && (other instanceof GroupTitle)
              && getKey().equals(((GroupTitle)other).getKey());
   }

   public String getKey()
   {
      return getKey(type, id, parent);
   }

   public static String getKey(String entryType, long groupId, String parent)
   {
      String key = "";

      if (entryType == null || entryType.isEmpty())
      {
         key += groupId;
      }
      else
      {
         key = String.format("%s.%d", entryType, groupId);
      }

      if (parent != null && !parent.isEmpty())
      {
         key += "|" + parent;
      }

      return key;
   }

   protected String getNonHierCsSetName()
   {
      return "bibglssetlettergrouptitle";
   }

   public String getCsSetName()
   {
      return supportsHierarchy ? getNonHierCsSetName() + "hier" : getNonHierCsSetName();
   }

   protected String getNonHierCsLabelName()
   {
      return "bibglslettergroup";
   }

   public String getCsLabelName()
   {
      return supportsHierarchy ? getNonHierCsLabelName() + "hier" : getNonHierCsLabelName();
   }

   public String getCsTitleName()
   {
      return supportsHierarchy ? getNonHierCsLabelName() + "titlehier" : getNonHierCsLabelName()+"title";
   }

   public String toString()
   {
      return format(actual);
   }

   public String format()
   {
      return format(actual);
   }

   public String format(String letter)
   {
      if (supportsHierarchy)
      {
         return String.format("{%s}{%s}{%d}{%s}{%s}{%d}", title, 
          bib2gls.replaceSpecialChars(letter), id,
          type == null ? "" : type, parent == null ? "" : parent, level);
      }
      else
      {
         return String.format("{%s}{%s}{%d}{%s}", title, 
          bib2gls.replaceSpecialChars(letter), id,
          type == null ? "" : type);
      }
   }

   public void mark()
   {
      done = true;
   }

   public boolean isDone()
   {
      return done;
   }

   public void setStartIndex(int i)
   {
      startIndex = i;
   }

   public void setEndIndex(int i)
   {
      endIndex = i;
   }

   public int getStartIndex()
   {
      return startIndex;
   }

   public int getEndIndex()
   {
      return endIndex;
   }

   public void resetIndexes()
   {
      startIndex = -1;
      endIndex = -1;
   }

   protected String title, actual, type, parent;

   private long id;

   private boolean done=false;

   protected boolean supportsHierarchy = false;

   protected int level = 0;

   private int startIndex=-1, endIndex=-1;

   protected Bib2Gls bib2gls;
}
