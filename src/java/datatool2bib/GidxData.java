/*
    Copyright (C) 2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.datatool2bib;

import java.io.IOException;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.latex.KeyValList;

public class GidxData
{
   public GidxData(String label, KeyValList fields)
   {
      this.label = label;
      this.fields = fields;
   }

   public String getLabel()
   {
      return label;
   }

   public KeyValList getFields()
   {
      return fields;
   }

   public String getFieldString(String field, TeXParser parser)
   {
      if (fields == null) return null;

      String val = null;

      try
      {
         val = fields.getString(field, parser, null);
      }
      catch (IOException e)
      {
         parser.getListener().getTeXApp().error(e);
      }

      if (val == null)
      {
         try
         {
            val = fields.getString(field.toLowerCase(), parser, null);
         }
         catch (IOException e)
         {
            parser.getListener().getTeXApp().error(e);
         }
      }

      return val;
   }

   public String getEntryType()
   {
      return entryType;
   }

   public void setEntryType(String entryType)
   {
      this.entryType = entryType;
   }

   protected String entryType = "entry";
   protected String label;
   protected KeyValList fields;
}
