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

import java.util.Iterator;
import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;

public class NewDualEntry extends NewGlossaryEntry
{
   public NewDualEntry(Gls2Bib gls2bib)
   {
      super("newdualentry", "dualentryabbreviation", gls2bib);
   }

   public NewDualEntry(String name, Gls2Bib gls2bib)
   {
      super(name, "dualentryabbreviation", gls2bib);
   }

   public NewDualEntry(String name, String type, Gls2Bib gls2bib)
   {
      super(name, type, gls2bib);
   }

   public NewDualEntry(String name, String type, Gls2Bib gls2bib,
      boolean provide)
   {
      super(name, type, gls2bib, provide);
   }

   public Object clone()
   {
      return new NewDualEntry(getName(), getType(), gls2bib, isProvide());
   }

   private void processEntry(TeXParser parser, TeXObject labelArg,
     TeXObject shortArg, TeXObject longArg, TeXObject descArg, 
     TeXObject options)
   throws IOException
   {
      KeyValList fields;

      if (options == null)
      {
         fields = new KeyValList();
      }
      else
      {
         fields = KeyValList.getList(parser, options);
      }

      fields.put("long", longArg);
      fields.put("short", shortArg);
      fields.put("description", descArg);

      processEntry(parser, labelArg.toString(parser), fields);
   }

   public void process(TeXParser parser) throws IOException
   {
      TeXObject options = parser.popNextArg('[', ']');

      processEntry(parser, 
        parser.popNextArg(), // label
        parser.popNextArg(), // short
        parser.popNextArg(), // long
        parser.popNextArg(), // description
        options);
   }

   public void process(TeXParser parser, TeXObjectList list) throws IOException
   {
      TeXObject options = list.popArg(parser, '[', ']');

      processEntry(parser, 
        list.popArg(parser), // label
        list.popArg(parser), // short
        list.popArg(parser), // long
        list.popArg(parser), // description
        options);
   }

}
