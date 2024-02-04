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

public class LongNewGlossaryEntry extends NewGlossaryEntry
{
   public LongNewGlossaryEntry(Gls2Bib gls2bib)
   {
      this("longnewglossaryentry", gls2bib);
   }

   public LongNewGlossaryEntry(String name, Gls2Bib gls2bib)
   {
      super(name, gls2bib);
   }

   public LongNewGlossaryEntry(String name, Gls2Bib gls2bib, boolean provide)
   {
      super(name, gls2bib, provide);
   }

   public Object clone()
   {
      return new LongNewGlossaryEntry(getName(), gls2bib, isProvide());
   }

   private void processEntry(TeXParser parser, String labelStr,
     KeyValList fields, TeXObject descriptionArg, boolean trimDesc)
     throws IOException
   {
      if (trimDesc && descriptionArg instanceof TeXObjectList)
      {
         TeXObjectList list = (TeXObjectList)descriptionArg;

         list.trimTrailing();
      }

      fields.put("description", descriptionArg);

      processEntry(parser, labelStr, fields);
   }

   public void process(TeXParser parser) throws IOException
   {
      process(parser, parser);
   }

   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      boolean isStar = (popModifier(parser, stack, '*') != -1);

      String labelStr = popLabelString(parser, stack);

      KeyValList keyValList = TeXParserUtils.popKeyValList(parser, stack);

      TeXObject descArg = TeXParserUtils.popArg(parser, stack,
        TeXObjectList.POP_IGNORE_LEADING_SPACE);

      processEntry(parser, labelStr, keyValList, descArg, isStar);
   }

}
