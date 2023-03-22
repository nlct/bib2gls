/*
    Copyright (C) 2023 Nicola L.C. Talbot
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
package com.dickimawbooks.bib2gls;

import java.util.Vector;
import java.io.IOException;

import com.dickimawbooks.texparserlib.*;

public class FieldValueList extends Vector<FieldValueElement>
{
   public FieldValueList()
   {
      super();
   }

   public FieldValueList(int capacity)
   {
      super(capacity);
   }

   public static FieldValueList pop(GlsResource resource, TeXObjectList stack)
     throws Bib2GlsSyntaxException,IOException
   {
      TeXParser parser = resource.getParser();
      Bib2Gls bib2gls = resource.getBib2Gls();

      FieldValueList list = new FieldValueList();
      boolean add = true;

      TeXObjectList quoted = null;

      while (stack.size() > 0)
      {
         TeXObject object = stack.peek();

         if (quoted != null)
         {
            if (object instanceof Group)
            {
              /* Need to allow for the possibility of an opening brace 
                 in one quote that is closed in another quote.
               */

              stack.pop();
              stack.push(((Group)object).splitTokens(parser), true);
            }
            else if (object instanceof SingleToken
                  && ((SingleToken)object).getCharCode() == '"')
            {
               stack.pop();
               list.add(new FieldValueString(quoted, true));
               quoted = null;
               add = false;
            }
            else
            {
               quoted.add(object);
               stack.pop();
            }
         }
         else if (object instanceof WhiteSpace)
         {
            stack.pop();
         }
         else if (object instanceof Group)
         {
            if (add)
            {
               object = stack.popArg(parser);

               list.add(new FieldValueString(object, false));
               add = false;
            }
            else
            {
               throw new Bib2GlsSyntaxException(bib2gls.getMessage(
                "error.expected_before", "+", object.toString(parser)));
            }
         }
         else if (object instanceof SingleToken)
         {
            int cp = ((SingleToken)object).getCharCode();
            int catCode = ((SingleToken)object).getCatCode();

            if (catCode == TeXParser.TYPE_BG)
            {
               if (add)
               {
                  object = stack.popArg(parser);

                  list.add(new FieldValueString(object, false));
                  add = false;
               }
               else
               {
                  throw new Bib2GlsSyntaxException(bib2gls.getMessage(
                   "error.expected_before", "+", object.toString(parser)));
               }
            }
            else if (cp == '+')
            {
               stack.pop();
               add = true;
            }
            else if (cp == '"')
            {
               stack.pop();

               if (!add)
               {
                  throw new Bib2GlsSyntaxException(bib2gls.getMessage(
                   "error.expected_before", "+", object.toString(parser)));
               }

               quoted = parser.getListener().createStack();
               add = false;
            }
            else if (cp == '[' || cp == ',')
            {
               break;
            }
            else if (add)
            {
               list.add(Field.popField(resource, stack));
               add = false;
            }
            else
            {
               throw new Bib2GlsSyntaxException(bib2gls.getMessage(
                  "error.invalid.condition", object.toString(parser)));
            }
         }
         else
         {
            throw new Bib2GlsSyntaxException(bib2gls.getMessage(
               "error.invalid.condition", object.toString(parser)));
         }
      }

      if (add && !list.isEmpty())
      {
         throw new Bib2GlsSyntaxException(bib2gls.getMessage(
            "error.expected_field_or_string_after", list.toString()+" + "));
      }

      return list;
   }
}
