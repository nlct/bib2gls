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

public class OldAcronym extends NewAbbreviation
{
   public OldAcronym(Gls2Bib gls2bib)
   {
      super("oldacronym", "acronym", gls2bib);
   }

   public OldAcronym(String name, Gls2Bib gls2bib)
   {
      super(name, "acronym", gls2bib);
   }

   public OldAcronym(String name, String type, Gls2Bib gls2bib)
   {
      super(name, type, gls2bib);
   }

   public OldAcronym(String name, String type, Gls2Bib gls2bib,
      boolean provide)
   {
      super(name, type, gls2bib, provide);
   }

   public Object clone()
   {
      return new OldAcronym(getName(), getType(), gls2bib, isProvide());
   }

   public String getDefaultGlossaryType()
   {
      return "abbreviations";
   }

   public String getDefaultCategory()
   {
      return "abbreviation";
   }

   public void process(TeXParser parser) throws IOException
   {
      process(parser, parser);
   }

   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      String labelStr = TeXParserUtils.popOptLabelString(parser, stack);

      TeXObject shortArg = TeXParserUtils.popArgExpandFully(parser, stack);

      if (labelStr == null)
      {
         labelStr = shortArg.toString(parser);
      }

      TeXObject longArg = TeXParserUtils.popArgExpandFully(parser, stack);

      KeyValList options = TeXParserUtils.popKeyValList(parser, stack);      

      processEntry(parser, labelStr, shortArg, longArg, options);
   }

}
