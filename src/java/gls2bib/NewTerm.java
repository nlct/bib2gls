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

public class NewTerm extends NewGlossaryEntry
{
   public NewTerm(Gls2Bib gls2bib)
   {
      super("newterm", "index", gls2bib);
   }

   public NewTerm(String name, Gls2Bib gls2bib)
   {
      super(name, "index", gls2bib);
   }

   public NewTerm(String name, String type, Gls2Bib gls2bib)
   {
      super(name, type, gls2bib);
   }

   public NewTerm(String name, String type, Gls2Bib gls2bib,
      boolean provide)
   {
      super(name, type, gls2bib, provide);
   }

   public Object clone()
   {
      return new NewTerm(getName(), getType(), gls2bib, isProvide());
   }

   public String getDefaultGlossaryType()
   {
      return "index";
   }

   public String getDefaultCategory()
   {
      return "index";
   }

   private void processEntry(TeXParser parser, TeXObject labelArg,
     TeXObject options)
   throws IOException
   {
      KeyValList fields;
      String spaceSub = gls2bib.getSpaceSub();
      String labelStr = labelArg.toString(parser);

      if (options == null)
      {
         fields = new KeyValList();

         if (spaceSub != null && labelStr.contains(" "))
         {
            fields.put("name", labelArg);
         }
      }
      else
      {
         fields = KeyValList.getList(parser, options);

         if (spaceSub != null && labelStr.contains(" ")
              && fields.get("name") == null)
         {
            fields.put("name", labelArg);
         }
      }

      processEntry(parser, labelStr, fields);
   }

   public void process(TeXParser parser) throws IOException
   {
      TeXObject options = parser.popNextArg('[', ']');

      TeXObject labelArg = parser.popNextArg();

      if (labelArg instanceof Expandable)
      {
         TeXObjectList expanded = ((Expandable)labelArg).expandfully(parser);

         if (expanded != null)
         {
            labelArg = expanded;
         }
      }

      processEntry(parser, labelArg, options);
   }

   public void process(TeXParser parser, TeXObjectList list) throws IOException
   {
      TeXObject options = list.popArg(parser, '[', ']');

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

      processEntry(parser, labelArg, options);
   }

}
