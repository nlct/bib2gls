/*
    Copyright (C) 2017-2023 Nicola L.C. Talbot
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

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateTimeGroupTitle extends GroupTitle
{
   public DateTimeGroupTitle(Bib2Gls bib2gls, DateFormat dateFormat, Date date, String type,
      String parent, boolean showDate, boolean showTime)
   {
      super(bib2gls, null, dateFormat.format(date), date.getTime(), type, parent);

      SimpleDateFormat format;

      if (showDate && showTime)
      {
         format = new SimpleDateFormat("{yyyy}{MM}{dd}{HH}{mm}{ss}{Z}");
         csname = "datetimegroup";
      }
      else if (showDate)
      {
         format = new SimpleDateFormat("{yyyy}{MM}{dd}{G}");
         csname = "dategroup";
      }
      else
      {
         format = new SimpleDateFormat("{HH}{mm}{ss}{Z}");
         csname = "timegroup";
      }

      setTitle(format.format(date));
   }

   @Override
   protected String getNonHierCsSetName()
   {
      return "bibglsset"+csname+"title";
   }

   @Override
   protected String getNonHierCsLabelName()
   {
      return "bibgls"+csname;
   }

   @Override
   public String format(String letter)
   {
      if (supportsHierarchy)
      {
         return String.format("%s{%s}{%d}{%s}{%s}{%d}",
            getTitle(), getActual(), getId(),
            type == null ? "" : getType(), parent == null ? "" : parent, level);
      }
      else
      {
         return String.format("%s{%s}{%d}{%s}", getTitle(), getActual(), getId(),
            type == null ? "" : getType());
      }
   }

   private String csname;
}
