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

public class NewGlossaryEntry extends ControlSequence
{
   public NewGlossaryEntry(Gls2Bib gls2bib)
   {
      this("newglossaryentry", "entry", gls2bib, false);
   }

   public NewGlossaryEntry(String name, Gls2Bib gls2bib)
   {
      this(name, "entry", gls2bib, false);
   }

   public NewGlossaryEntry(String name, String type, 
     Gls2Bib gls2bib)
   {
      this(name, type, gls2bib, false);
   }

   public NewGlossaryEntry(String name, Gls2Bib gls2bib, boolean provide)
   {
      this(name, "entry", gls2bib, provide);
   }

   public NewGlossaryEntry(String name, String type, Gls2Bib gls2bib, boolean provide)
   {
      super(name);

      this.gls2bib = gls2bib;
      this.provide = provide;
      this.type = type;
   }

   public Object clone()
   {
      return new NewGlossaryEntry(getName(), getType(), gls2bib, provide);
   }

   protected void processEntry(TeXParser parser, String label,
    KeyValList valuesArg)
   {
      if (provide && gls2bib.hasEntry(label))
      {
         return;
      }

      GlsData data = new GlsData(label, getType());

      Iterator<String> it = valuesArg.keySet().iterator();

      while (it.hasNext())
      {
         String field = it.next();
         TeXObject object = valuesArg.getValue(field);

         if ((object instanceof Group) && !(object instanceof MathGroup))
         {
            data.putField(field, object.toString(parser));
         }
         else
         {
            data.putField(field, 
               String.format("{%s}", object.toString(parser)));
         }

      }

      gls2bib.addData(data);
   }

   private void processEntry(TeXParser parser, TeXObject labelArg,
    KeyValList valuesArg)
   throws IOException
   {
      processEntry(parser, labelArg.toString(parser), valuesArg);
   }

   public void process(TeXParser parser) throws IOException
   {
      processEntry(parser, parser.popNextArg(), 
        KeyValList.getList(parser, parser.popNextArg()));
   }

   public void process(TeXParser parser, TeXObjectList list) throws IOException
   {
      processEntry(parser, list.popArg(parser), 
        KeyValList.getList(parser, list.popArg(parser)));
   }

   public boolean isProvide()
   {
      return provide;
   }

   public String getType()
   {
      return type;
   }

   private String type;
   protected Gls2Bib gls2bib;
   private boolean provide=false;
}
