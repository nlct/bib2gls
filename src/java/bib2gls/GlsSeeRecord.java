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

import java.io.IOException;
import java.util.Vector;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class GlsSeeRecord
{
   public GlsSeeRecord(TeXObject labelObj, TeXObject value, TeXParser parser)
    throws IOException
   {
      if (labelObj == null || value == null)
      {
         throw new NullPointerException();
      }

      this.label = labelObj.toString(parser);

      init(value, parser);
   }

   private void init(TeXObject value, TeXParser parser)
    throws IOException
   {
      if (!(value instanceof TeXObjectList))
      {
         xrLabels = new String[1];

         xrLabels[0] = value.toString(parser);

         return;
      }

      TeXObjectList list = (TeXObjectList)value;

      TeXObject obj = list.popArg(parser, '[', ']');

      if (obj != null)
      {
         tag = obj.toString(parser);
      }

      CsvList csvList = CsvList.getList(parser, list);

      int n = csvList.size();

      xrLabels = new String[n];

      for (int i = 0; i < n; i++)
      {
         xrLabels[i] = csvList.getValue(i).toString(parser);
      }
   }

   public String getLabel()
   {
      return label;
   }

   public String getTag()
   {
      return tag;
   }

   public String[] getXrLabels()
   {
      return xrLabels;
   }

   public String toString()
   {
      return String.format("CrossRef[label=%s,tag=%s,n=%d]",
       label, tag, xrLabels.length);
   }

   private String label;
   private String tag=null;
   private String[] xrLabels;
}
