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
package com.dickimawbooks.bibgls.gls2bib;

import java.util.Iterator;
import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;

public class NewNumber extends NewGlossaryEntry
{
   public NewNumber(Gls2Bib gls2bib)
   {
      super("glsxtrnewnumber", "number", gls2bib);
   }

   public NewNumber(String name, Gls2Bib gls2bib)
   {
      super(name, "number", gls2bib);
   }

   public NewNumber(String name, String type, Gls2Bib gls2bib)
   {
      super(name, type, gls2bib);
   }

   public NewNumber(String name, String type, Gls2Bib gls2bib,
      boolean provide)
   {
      super(name, type, gls2bib, provide);
   }

   public Object clone()
   {
      return new NewNumber(getName(), getType(), gls2bib, isProvide());
   }

   public String getDefaultGlossaryType()
   {
      return "numbers";
   }

   public String getDefaultCategory()
   {
      return "number";
   }

   @Override
   protected void processEntry(TeXParser parser, String labelStr,
     KeyValList fields)
   throws IOException
   {
      TeXObject name = null;

      if (fields == null)
      {
         fields = new KeyValList();
      }
      else
      {
         name = fields.get("name");
      }

      if (name == null) 
      {
         name = parser.getListener().createString(labelStr);

         labelStr = gls2bib.processLabel(labelStr);

         if (!name.toString(parser).equals(labelStr))
         {
            fields.put("name", name);
         }
      }

      super.processEntry(parser, labelStr, fields);
   }

   public void process(TeXParser parser) throws IOException
   {
      process(parser, parser);
   }

   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      KeyValList options = TeXParserUtils.popOptKeyValList(parser, stack);

      String labelStr = popLabelString(parser, stack);

      processEntry(parser, labelStr, options);
   }

}
