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
package com.dickimawbooks.gls2bib;

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

   private void processEntry(TeXParser parser, TeXObject labelArg,
     KeyValList fields, TeXObject descriptionArg, boolean trimDesc)
     throws IOException
   {
      if (trimDesc && descriptionArg instanceof TeXObjectList)
      {
         TeXObjectList list = (TeXObjectList)descriptionArg;

         for (int i = list.size()-1; i >= 0; i--)
         {
            if (list.get(i) instanceof WhiteSpace)
            {
               list.remove(i);
            }
            else if (!(list.get(i) instanceof Ignoreable))
            {
               break;
            }
         }
      }

      fields.put("description", descriptionArg);
      processEntry(parser, labelArg.toString(parser), fields);
   }

   public void process(TeXParser parser) throws IOException
   {
      TeXObject obj = parser.peekStack();

      boolean isStar = false;

      if (obj != null && (obj instanceof CharObject)
        && ((CharObject)obj).getCharCode() == '*')
      {
         isStar = true;
         parser.popStack();
      }

      TeXObject labelArg = parser.popNextArg();

      if (labelArg instanceof Expandable)
      {
         TeXObjectList expanded = ((Expandable)labelArg).expandfully(parser);

         if (expanded != null)
         {
            labelArg = expanded;
         }
      }

      TeXObject keyListArg = parser.popNextArg();

      TeXObject descriptionArg = parser.popNextArg();

      processEntry(parser, labelArg, 
        KeyValList.getList(parser, keyListArg),
        descriptionArg, isStar);
   }

   public void process(TeXParser parser, TeXObjectList list) throws IOException
   {
      TeXObject obj = list.peekStack();

      boolean isStar = false;

      if (obj != null && (obj instanceof CharObject)
        && ((CharObject)obj).getCharCode() == '*')
      {
         isStar = true;
         list.popStack(parser);
      }

      TeXObject labelArg = list.popArg(parser);

      if (labelArg instanceof Expandable)
      {
         TeXObjectList expanded = ((Expandable)labelArg).expandfully(parser,
            list);

         if (expanded != null)
         {
            labelArg = expanded;
         }
      }

      processEntry(parser, labelArg, 
        KeyValList.getList(parser, list.popArg(parser)),
        list.popArg(parser), isStar);
   }

}
