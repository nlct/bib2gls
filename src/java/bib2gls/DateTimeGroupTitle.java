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

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateTimeGroupTitle extends GroupTitle
{
   public DateTimeGroupTitle(DateFormat dateFormat, Date date, String type, boolean showDate, boolean showTime)
   {
      super(null, dateFormat.format(date), date.getTime(), type);

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

   public String getCsSetName()
   {
      return "bibglsset"+csname+"title";
   }

   public String getCsLabelName()
   {
      return "bibgls"+csname;
   }

   public String format(String letter)
   {
      return String.format("%s{%s}{%d}{%s}", getTitle(), getActual(), getId(),
       type == null ? "" : getType());
   }

   private String csname;
}
