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
package com.dickimawbooks.gls2bib;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

public class GlsData
{
   public GlsData(String label)
   {
      this(label, "entry");
   }

   public GlsData(String label, String type)
   {
      this.id = label;
      this.type = type;
      fields = new HashMap<String,String>();
   }

   public String getId()
   {
      return id;
   }

   // top-level braces should already be added to the fieldValue
   // if required
   public void putField(String fieldName, String fieldValue)
   {
      fields.put(fieldName, fieldValue);
   }

   public void writeBibEntry(PrintWriter writer)
     throws IOException
   {
      writer.format("@%s{%s", type, id);

      Iterator<String> it = fields.keySet().iterator();

      boolean hasFields = false;

      while (it.hasNext())
      {
         String key = it.next();

         // grouping should already have been added
         writer.format(",%n  %s = %s", key, fields.get(key));

         hasFields = true;
      }

      if (hasFields)
      {
         writer.println();
      }

      writer.println("}");
      writer.println();
   }

   private String id, type;

   private HashMap<String,String> fields;
}
