/*
    Copyright (C) 2020-2021 Nicola L.C. Talbot
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

public class NewGlossary extends ControlSequence
{
   public NewGlossary()
   {
      this("newglossary");
   }

   public NewGlossary(String name)
   {
      this(name, STANDARD);
   }

   public NewGlossary(String name, int defType)
   {
      super(name);

      switch (defType)
      {
         case STANDARD:
         case ALT:
         case IGNORED:
         break;
         default:
           throw new IllegalArgumentException("Invalid defType "+defType);
      }

      this.defType = defType;
   }

   public Object clone()
   {
      return new NewGlossary(getName(), defType);
   }

   public void process(TeXParser parser) throws IOException
   {
      if (defType == ALT)
      {
         parser.popNextArg();// name
         parser.popNextArg();// tag
      }
      else
      {
         TeXObject obj = parser.popStack();

         boolean isStar;

         if (obj instanceof CharObject &&
           ((CharObject)obj).getCharCode() == (int)'*')
         {
            isStar = true;
         }
         else
         {
            isStar = false;
            parser.push(obj);
         }

         if (defType == STANDARD)
         {
            if (isStar)
            {
               parser.popNextArg();//type
            }
            else
            {
               parser.popNextArg('[', ']');//
               parser.popNextArg();// type
               parser.popNextArg();
               parser.popNextArg();
            }
         }
      }

      parser.popNextArg();//title or label

      if (defType != IGNORED)
      {
         parser.popNextArg('[', ']');//counter
      }
   }

   public void process(TeXParser parser, TeXObjectList list) throws IOException
   {
      if (defType == ALT)
      {
         list.popArg(parser);// name
         list.popArg(parser);// tag
      }
      else
      {
         TeXObject obj = list.pop();

         boolean isStar;

         if (obj instanceof CharObject &&
            ((CharObject)obj).getCharCode() == (int)'*')
         {
            isStar = true;
         }
         else
         {
            isStar = false;
            list.push(obj);
         }

         if (defType == STANDARD)
         {
            if (isStar)
            {
               list.popArg(parser);//type
            }
            else
            {
               list.popArg(parser, '[', ']');
               list.popArg(parser);// type
               list.popArg(parser);
               list.popArg(parser);
            }
         }
      }

      list.popArg(parser);//title or label

      if (defType != IGNORED)
      {
         list.popArg(parser, '[', ']');//counter
      }
   }

   public static final int STANDARD=0, ALT=1, IGNORED=2;
   private int defType = STANDARD;
}
