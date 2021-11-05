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
   public GroupTitle(String title, String actual, long id,
     String type)
   {
      this.title = title;
      this.actual = actual;
      this.id = id; 
      this.type = type;
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

   public String getKey()
   {
      return getKey(type, id);
   }

   public static String getKey(String entryType, long groupId)
   {
      return entryType == null || entryType.isEmpty() ? ""+groupId :
        String.format("%s.%d", entryType, groupId);
   }

   public String getCsSetName()
   {
      return "bibglssetlettergrouptitle";
   }

   public String getCsLabelName()
   {
      return "bibglslettergroup";
   }

   public String toString()
   {
      return format(actual);
   }

   public String format(String letter)
   {
      return String.format("{%s}{%s}{%d}{%s}", title, 
       Bib2Gls.replaceSpecialChars(letter), id,
       type == null ? "" : type);
   }

   public void mark()
   {
      done = true;
   }

   public boolean isDone()
   {
      return done;
   }

   protected String title, actual, type;

   private long id;

   private boolean done=false;
}
