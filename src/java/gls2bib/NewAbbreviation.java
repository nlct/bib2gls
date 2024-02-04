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

public class NewAbbreviation extends NewGlossaryEntry
{
   public NewAbbreviation(Gls2Bib gls2bib)
   {
      super("newabbreviation", "abbreviation", gls2bib);
   }

   public NewAbbreviation(String name, Gls2Bib gls2bib)
   {
      super(name, "abbreviation", gls2bib);
   }

   public NewAbbreviation(String name, String type, Gls2Bib gls2bib)
   {
      super(name, type, gls2bib);
   }

   public NewAbbreviation(String name, String type, Gls2Bib gls2bib,
      boolean provide)
   {
      super(name, type, gls2bib, provide);
   }

   public Object clone()
   {
      return new NewAbbreviation(getName(), getType(), gls2bib, isProvide());
   }

   public String getDefaultGlossaryType()
   {
      return "abbreviations";
   }

   public String getDefaultCategory()
   {
      return "abbreviation";
   }

   protected void processEntry(TeXParser parser, String labelStr,
     TeXObject shortArg, TeXObject longArg, KeyValList fields)
   throws IOException
   {
      if (fields == null)
      {
         fields = new KeyValList();
      }

      fields.put("long", longArg);
      fields.put("short", shortArg);

      processEntry(parser, labelStr, fields);
   }

   public void process(TeXParser parser) throws IOException
   {
      process(parser, parser);
   }

   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      KeyValList options = TeXParserUtils.popOptKeyValList(parser, stack);

      String labelStr = popLabelString(parser, stack);

      TeXObject shortArg = popArg(parser, stack);
      TeXObject longArg = popArg(parser, stack);

      processEntry(parser, labelStr, shortArg, longArg, options);
   }

}
