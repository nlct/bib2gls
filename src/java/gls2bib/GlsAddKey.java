/*
    Copyright (C) 2020-2023 Nicola L.C. Talbot
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
import com.dickimawbooks.texparserlib.latex.AtGobble;
import com.dickimawbooks.texparserlib.latex.GobbleOptMandOpt;

public class GlsAddKey extends ControlSequence
{
   public GlsAddKey()
   {
      this("glsaddkey", false);
   }

   public GlsAddKey(String name, boolean storageOnly)
   {
      super(name);
      this.storageOnly = storageOnly;
   }

   public Object clone()
   {
      return new GlsAddKey(getName(), storageOnly);
   }

   public void process(TeXParser parser) throws IOException
   {
      TeXObject obj = parser.pop();

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

      parser.popNextArg();//key
      parser.popNextArg();//default value

      if (storageOnly)
      {
         obj = parser.popNextArg();

         if (obj instanceof TeXObjectList)
         {
            obj = ((TeXObjectList)obj).pop();
         }

         if (obj instanceof ControlSequence)
         {
            parser.putControlSequence(
              new AtGobble(((ControlSequence)obj).getName()));
         }
         else
         {
            throw new TeXSyntaxException(
               parser,
               TeXSyntaxException.ERROR_CS_EXPECTED,
               obj.format(), obj.getClass().getSimpleName());
         }

      }
      else
      {
         for (int i = 0; i < 2; i++)
         {
            obj = parser.popNextArg();

            if (obj instanceof TeXObjectList)
            {
               obj = ((TeXObjectList)obj).pop();
            }

            if (obj instanceof ControlSequence)
            {
               parser.putControlSequence(
                 new AtGobble(((ControlSequence)obj).getName()));
            }
            else
            {
               throw new TeXSyntaxException(
                  parser,
                  TeXSyntaxException.ERROR_CS_EXPECTED,
                  obj.format(), obj.getClass().getSimpleName());
            }
         }

         for (int i = 0; i < 3; i++)
         {
            obj = parser.popNextArg();

            if (obj instanceof TeXObjectList)
            {
               obj = ((TeXObjectList)obj).pop();
            }

            if (obj instanceof ControlSequence)
            {
               parser.putControlSequence(
                 new GobbleOptMandOpt(((ControlSequence)obj).getName()));
            }
            else
            {
               throw new TeXSyntaxException(
                  parser,
                  TeXSyntaxException.ERROR_CS_EXPECTED,
                  obj.format(), obj.getClass().getSimpleName());
            }
         }
      }

   }

   public void process(TeXParser parser, TeXObjectList list) throws IOException
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

      list.popArg(parser);//key
      list.popArg(parser);//default

      if (storageOnly)
      {
         obj = list.popArg(parser);

         if (obj instanceof TeXObjectList)
         {
            obj = ((TeXObjectList)obj).pop();
         }

         if (obj instanceof ControlSequence)
         {
            parser.putControlSequence(
              new AtGobble(((ControlSequence)obj).getName()));
         }
         else
         {
            throw new TeXSyntaxException(
               parser,
               TeXSyntaxException.ERROR_CS_EXPECTED,
               obj.format(), obj.getClass().getSimpleName());
         }
      }
      else
      {
         for (int i = 0; i < 2; i++)
         {
            obj = list.popArg(parser);

            if (obj instanceof TeXObjectList)
            {
               obj = ((TeXObjectList)obj).pop();
            }

            if (obj instanceof ControlSequence)
            {
               parser.putControlSequence(
                 new AtGobble(((ControlSequence)obj).getName()));
            }
            else
            {
               throw new TeXSyntaxException(
                  parser,
                  TeXSyntaxException.ERROR_CS_EXPECTED,
                  obj.format(), obj.getClass().getSimpleName());
            }
         }

         for (int i = 0; i < 3; i++)
         {
            obj = list.popArg(parser);

            if (obj instanceof TeXObjectList)
            {
               obj = ((TeXObjectList)obj).pop();
            }

            if (obj instanceof ControlSequence)
            {
               parser.putControlSequence(
                 new GobbleOptMandOpt(((ControlSequence)obj).getName()));
            }
            else
            {
               throw new TeXSyntaxException(
                  parser,
                  TeXSyntaxException.ERROR_CS_EXPECTED,
                  obj.format(), obj.getClass().getSimpleName());
            }
         }
      }
   }

   private boolean storageOnly = false;
}
