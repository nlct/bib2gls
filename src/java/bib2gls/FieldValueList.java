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
     throws Bib2GlsException,IOException
   {
      TeXParser parser = resource.getParser();
      Bib2Gls bib2gls = resource.getBib2Gls();

      FieldValueList list = new FieldValueList();
      boolean add = true;

      TeXObjectList quoted = null;
      TeXObjectList popped = new TeXObjectList();

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

              popped.add(stack.pop());
              stack.push(((Group)object).splitTokens(parser), true);
            }
            else if (object instanceof SingleToken
                  && ((SingleToken)object).getCharCode() == '"')
            {
               popped.add(stack.pop());
               list.add(new FieldValueString(quoted, true));
               quoted = null;
               add = false;
            }
            else if (object instanceof ControlSequence)
            {
               popped.add(stack.pop());

               String name = ((ControlSequence)object).getName();

               if (name.equals("\""))
               {
                  object = parser.getListener().getOther('"');
               }

               quoted.add(object);
            }
            else
            {
               quoted.add(object);
               popped.add(stack.pop());
            }
         }
         else if (FieldCaseChange.isFieldCaseChange(object))
         {
            if (add)
            {
               TeXObject cs = object;

               FieldCaseChange fieldCaseChange
                  = FieldCaseChange.getFieldCaseChange(object);

               popped.add(stack.pop());

               object = stack.popArg(parser);

               if (parser.isStack(object))
               {
                  TeXObjectList substack = (TeXObjectList)object;

                  Field field = Field.popField(resource, substack);
                  field.setCaseChange(fieldCaseChange);

                  list.add(field);

                  substack.popLeadingWhiteSpace();

                  if (!substack.isEmpty())
                  {
                     throw new Bib2GlsException(bib2gls.getMessage(
                       "error.unexpected_content_in_arg",
                        substack.toString(parser), cs));
                  }

                  add = false;

                  popped.addAll(TeXParserUtils.createGroup(parser,
                    parser.getListener().createString(field.toString())));
               }
               else if (!popped.isEmpty())
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                     "error.invalid.expected_after",
                       "> + [", popped.toString(parser), object.toString(parser)));
               }
               else
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                     "error.invalid.expected", "> + [", object.toString(parser)));
               }
            }
            else if (!popped.isEmpty())
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                  "error.invalid.expected_after",
                    "> + [", popped.toString(parser), object.toString(parser)));
            }
            else
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                  "error.invalid.expected", "> + [", object.toString(parser)));
            }
         }
         else if (object instanceof WhiteSpace)
         {
            popped.add(stack.pop());
         }
         else if (object instanceof Group)
         {
            if (add)
            {
               object = stack.popArg(parser);

               FieldValueString val = new FieldValueString(object, false);
               list.add(val);

               popped.add(parser.getListener().createString(val.toString()));
               add = false;
            }
            else
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                "error.expected_before_after", "+", 
                 object.toString(parser), popped.toString(parser)));
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

                  FieldValueString val = new FieldValueString(object, false);
                  list.add(val);

                  popped.add(parser.getListener().createString(val.toString()));
                  add = false;
               }
               else
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                   "error.expected_before_after", "+",
                      object.toString(parser), popped.toString(parser)));
               }
            }
            else if (cp == '+')
            {
               popped.add(stack.pop());
               add = true;
            }
            else if (cp == '"')
            {
               if (!add)
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                   "error.expected_before_after", "+", 
                      object.toString(parser), popped.toString(parser)));
               }

               popped.add(stack.pop());

               quoted = parser.getListener().createStack();
               add = false;
            }
            else if (cp == '[' || cp == ',')
            {
               break;
            }
            else if (add)
            {
               Field field = Field.popField(resource, stack);

               list.add(field);
               add = false;
               popped.addAll(parser.getListener().createString(field.toString()));
            }
            else if (!popped.isEmpty())
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                  "error.invalid.expected_after",
                    "> + [", popped.toString(parser), object.toString(parser)));
            }
            else
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                  "error.invalid.expected", "> + [", object.toString(parser)));
            }
         }
         else if (add)
         {
            if (!popped.isEmpty())
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                  "error.expected_field_or_string_condition_after",
                    popped.toString(parser), object.toString(parser)));
            }
            else
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                  "error.expected_field_or_string_condition",
                  object.toString(parser)));
            }
         }
         else if (!popped.isEmpty())
         {
            throw new Bib2GlsException(bib2gls.getMessage(
               "error.invalid.expected_after",
                 "> + [", popped.toString(parser), object.toString(parser)));
         }
         else
         {
            throw new Bib2GlsException(bib2gls.getMessage(
               "error.invalid.expected", "> + [", object.toString(parser)));
         }
      }

      if (add && !list.isEmpty())
      {
         throw new Bib2GlsException(bib2gls.getMessage(
            "error.expected_field_or_string_after", list.toString()+" + "));
      }

      return list;
   }
}
