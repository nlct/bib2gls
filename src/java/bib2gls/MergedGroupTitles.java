/*
    Copyright (C) 2021-2023 Nicola L.C. Talbot
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

import java.util.Vector;

public class MergedGroupTitles extends GroupTitle
{
   public MergedGroupTitles(Bib2Gls bib2gls, Vector<GroupTitle> titleList)
   {
      super(bib2gls, "", "", ++mergedGroupCount, null, null);

      if (titleList.size() < 2)
      {
         throw new IllegalArgumentException(
          "MergedGroupTitles requires 2 or more titles, list contains "+titleList.size());
      }

      titles = new GroupTitle[titleList.size()];

      for (int i = 0; i < titles.length; i++)
      {
         titles[i] = titleList.get(i);
      }

      type = titles[0].type;
      parent = titles[0].parent;
      level = titles[0].level;
      supportsHierarchy = titles[0].supportsHierarchy;
   }

   @Override
   protected String getNonHierCsSetName()
   {
      return "bibglssetmergedgrouptitle";
   }

   @Override
   protected String getNonHierCsLabelName()
   {
      return "bibglsmergedgroup";
   }

   @Override
   public String format(String other)
   {
      StringBuilder builder = new StringBuilder(
        String.format("{%d}{%s}{%d}{\\%s%s}{", getId(), type == null ? "" : type,
         titles.length, titles[0].getCsTitleName(), titles[0].toString()));

      int lastIdx = titles.length-1;

      for (int i = 1; i < lastIdx; i++)
      {
         builder.append(String.format("{\\%s%s}", 
           titles[i].getCsTitleName(), titles[i].toString()));
      }

      builder.append(String.format("}{\\%s%s}", 
        titles[lastIdx].getCsTitleName(), titles[lastIdx].toString()));

      if (supportsHierarchy)
      {
         builder.append(String.format("{%s}{%d}",
           parent == null ? "" : parent, level));
      }

      return builder.toString();
   }

   public GroupTitle[] getGroupTitles()
   {
      return titles;
   }

   private GroupTitle[] titles;

   private static long mergedGroupCount = 0L;
}
