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
import java.util.regex.Pattern;
import java.io.IOException;

import com.dickimawbooks.texparserlib.*;

public class ConditionalList extends Vector<ConditionalListElement>
  implements Conditional
{
   protected ConditionalList()
   {
      super();
   }

   protected ConditionalList(int capacity)
   {
      super(capacity);
   }

   public static ConditionalList popCondition(GlsResource resource,
      TeXObjectList stack, int terminator)
   throws Bib2GlsException,IOException
   {
      ConditionalList condList = new ConditionalList();
      Bib2Gls bib2gls = resource.getBib2Gls();

      TeXParser parser = resource.getParser();

      while (!stack.isEmpty())
      {
         TeXObject obj = stack.peek();

         if (obj instanceof WhiteSpace)
         {
            stack.pop();
         }
         else if (obj instanceof SingleToken)
         {
            int cp = ((SingleToken)obj).getCharCode();

            if (cp == terminator)
            {
               stack.pop();

               condList.validate(resource);
               return condList;
            }
            else if (cp == '!')
            {
               condList.add(new ConditionNegate());
               stack.pop();
            }
            else if (cp == '|')
            {
               condList.add(new ConditionOr());
               stack.pop();
            }
            else if (cp == '&')
            {
               condList.add(new ConditionAnd());
               stack.pop();
            }
            else if (cp == '(')
            {
               stack.pop();

               condList.add(popCondition(resource, stack, ')'));
            }
            else
            {
               condList.add(popComparison(resource, stack));
            }
         }
         else if (obj instanceof ControlSequence)
         {
            condList.add(popComparison(resource, stack));
         }
         else if (condList.isEmpty())
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.condition_in", obj.toString(parser),
               bib2gls.toTruncatedString(parser, stack)));
         }
         else
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.condition_after",
               obj.toString(parser), condList.toString()));
         }
      }

      throw new Bib2GlsException(bib2gls.getMessage(
        "error.invalid.condition_missing_end", (char)terminator));
   }

   public static Conditional popComparison(GlsResource resource,
     TeXObjectList stack)
   throws Bib2GlsException,IOException
   {
      Bib2Gls bib2gls = resource.getBib2Gls();
      TeXParser parser = resource.getParser();

      stack.popLeadingWhiteSpace();

      TeXObject obj = stack.peek();
      String quarkName = null;
      Field field;
      String leftSide;

      if (obj instanceof ControlSequence)
      {
         ControlSequence cs = (ControlSequence)obj;
         quarkName = cs.getName();

         if (!quarkName.equals("LEN"))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.condition_in", obj,
                 bib2gls.toTruncatedString(parser, stack)));
         }

         obj = stack.pop();

         obj = TeXParserUtils.popArg(parser, stack);

         if (parser.isStack(obj))
         {
            TeXObjectList substack = (TeXObjectList)obj;
            field = Field.popField(resource, substack);
            substack.popLeadingWhiteSpace();

            if (!substack.isEmpty())
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                "error.unexpected_content_in_arg",
                  substack.toString(parser), "\\"+quarkName));
            }
         }
         else
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.expected_field", "\\"+quarkName, obj.toString(parser)));
         }

         leftSide = String.format("\\%s{%s}", quarkName, field);
      }
      else
      {
         field = Field.popField(resource, stack);
         leftSide = field.toString();
      }

      stack.popLeadingWhiteSpace();

      obj = stack.pop();

      if (obj == null)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
             "error.invalid.regexp_or_cmp_condition_missing", 
              field));
      }

      if (obj instanceof ControlSequence)
      {
         if ("LEN".equals(quarkName))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.numeric_condition", leftSide,
                 obj.toString(parser)));
         }

         String name = ((ControlSequence)obj).getName();
 
         if (name.equals("IN"))
         {
            return new FieldInField(field, Field.popField(resource, stack));
         }
         else if (name.equals("NIN"))
         {
            return new FieldInField(field, Field.popField(resource, stack), true);
         }
         else if (name.equals("PREFIXOF"))
         {
            return new FieldPrefixOfField(field, Field.popField(resource, stack));
         }
         else if (name.equals("NOTPREFIXOF"))
         {
            return new FieldPrefixOfField(field,
               Field.popField(resource, stack), true);
         }
         else if (name.equals("SUFFIXOF"))
         {
            return new FieldSuffixOfField(field, Field.popField(resource, stack));
         }
         else if (name.equals("NOTSUFFIXOF"))
         {
            return new FieldSuffixOfField(field,
               Field.popField(resource, stack), true);
         }
         else
         {
            throw new Bib2GlsException(bib2gls.getMessage(
                "error.invalid.condition_after", obj.toString(parser), leftSide));
         }
      }

      stack.popLeadingWhiteSpace();

      TeXObject nextObj = stack.peek();

      if (nextObj == null)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
             "error.invalid.regexp_or_cmp_condition_missing", 
              leftSide+obj.toString(parser)));
      }

      int cp = 0;
      int nextCp = 0;
      int nextCatCode = -1;

      if (obj instanceof SingleToken)
      {
         cp = ((SingleToken)obj).getCharCode();
      }

      if (nextObj instanceof SingleToken)
      {
         nextCp = ((SingleToken)nextObj).getCharCode();
         nextCatCode = ((SingleToken)nextObj).getCatCode();
      }

      if (cp == '=' && nextCp == '/')
      {
         if ("LEN".equals(quarkName))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.numeric_condition", leftSide,
                 obj.toString(parser)));
         }

         stack.pop();
         String regexp = popQuoted(resource, stack, '/').toString(parser);

         int flags = 0;

         obj = stack.peek();

         if (obj instanceof SingleToken 
               && ((SingleToken)obj).getCharCode() == 'i')
         {
            flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

            obj = stack.pop();
         }

         Pattern pattern = Pattern.compile(regexp, flags);

         return new FieldPatternMatch(field, pattern);
      }

      Relational rel = null;

      if (cp == '=')
      {
         rel = Relational.EQUALS;
      }
      else if (cp == '<')
      {
         if (nextCp == '=')
         {
            rel = Relational.LE;
            obj = stack.pop();
            stack.popLeadingWhiteSpace();
            nextObj = stack.peek();
         }
         else if (nextCp == '>')
         {
            rel = Relational.NOT_EQUALS;
            obj = stack.pop();
            stack.popLeadingWhiteSpace();
            nextObj = stack.peek();
         }
         else
         {
            rel = Relational.LT;
         }
      }
      else if (cp == '>')
      {
         if (nextCp == '=')
         {
            rel = Relational.GE;
            obj = stack.pop();
            stack.popLeadingWhiteSpace();
            nextObj = stack.peek();
         }
         else
         {
            rel = Relational.GT;
         }
      }
      else
      {
         throw new Bib2GlsException(bib2gls.getMessage(
             "error.invalid.condition_after", 
              obj.toString(parser), field));
      }

      if ("LEN".equals(quarkName))
      {
         try
         {
            return new FieldLengthMatch(field, rel, popNumber(resource, stack));
         }
         catch (NumberFormatException e)
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.numeric_condition",
              quarkName, bib2gls.toTruncatedString(parser, stack)), e);
         }
      }
      else if (nextObj instanceof ControlSequence
          && ((ControlSequence)nextObj).getName().equals("NULL"))
      {
         stack.pop();

         if (!(rel == Relational.EQUALS || rel == Relational.NOT_EQUALS))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
                "error.invalid.null_condition", 
                 rel, field));
         }

         return new FieldNullMatch(field, rel == Relational.EQUALS);
      }
      else if (nextCp == '"')
      {
         stack.pop();
         String strValue = popQuoted(resource, stack, '"').toString(parser);

         boolean insensitive = false;

         obj = stack.peek();

         if (obj instanceof SingleToken 
               && ((SingleToken)obj).getCharCode() == 'i')
         {
            insensitive = true;
            obj = stack.pop();
         }

         return new FieldStringMatch(field, rel, strValue, true, insensitive);
      }
      else if (nextObj instanceof Group || nextCatCode == TeXParser.TYPE_BG)
      {
         String strValue = TeXParserUtils.popArg(parser, stack).toString(parser);

         boolean insensitive = false;

         obj = stack.peek();

         if (obj instanceof SingleToken 
               && ((SingleToken)obj).getCharCode() == 'i')
         {
            insensitive = true;
            obj = stack.pop();
         }

         return new FieldStringMatch(field, rel, strValue, false, insensitive);
      }
      else if ((nextCp >= '0' && nextCp <= '9')
                || nextCp == '.' || nextCp == '+' || nextCp == '-')
      {
         try
         {
            return new FieldNumberMatch(field, rel, popNumber(resource, stack));
         }
         catch (NumberFormatException e)
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.numeric_condition",
              rel, bib2gls.toTruncatedString(parser, stack)), e);
         }
      }
      else
      {
         return new FieldFieldMatch(field, rel, Field.popField(resource, stack));
      }

   }

   public static Number popNumber(GlsResource resource, TeXObjectList stack)
    throws NumberFormatException,IOException
   {
      Bib2Gls bib2gls = resource.getBib2Gls();

      StringBuilder builder = new StringBuilder();

      boolean dotFound = false;
      boolean expFound = false;
      boolean digitFound = false;

      while (!stack.isEmpty())
      {
         TeXObject obj = stack.peek();

         if (obj instanceof SingleToken)
         {
            int cp = ((SingleToken)obj).getCharCode();

            if (cp >= '0' && cp <= '9')
            {
               builder.appendCodePoint(cp);
               digitFound = true;
               stack.pop();
            }
            else if (cp == '+' || cp == '-')
            {
               if (digitFound || (dotFound && !expFound))
               {
                  break;
               }
               else
               {
                  builder.appendCodePoint(cp);
                  stack.pop();
               }
            }
            else if (cp == 'e' || cp == 'E')
            {
               if (expFound)
               {
                  break;
               }
               else
               {
                  builder.appendCodePoint(cp);
                  digitFound = false;
                  expFound = true;
                  stack.pop();
               }
            }
            else if (cp == '.')
            {
               if (dotFound || expFound)
               {
                  break;
               }
               else
               {
                  builder.appendCodePoint(cp);
                  dotFound = true;
                  stack.pop();
               }
            }
            else
            {
               break;
            }
         }
         else
         {
            break;
         }
      }

      String value = builder.toString();

      if (value.isEmpty())
      {
         throw new NumberFormatException(
            bib2gls.getMessage("error.missing_numeric"));
      }

      if (dotFound || expFound)
      {
         return Double.valueOf(value);
      }
      else
      {
         return Integer.valueOf(value);
      }
   }

   public static TeXObjectList popQuoted(GlsResource resource, TeXObjectList stack,
      int terminator)
    throws Bib2GlsException,IOException
   {
      Bib2Gls bib2gls = resource.getBib2Gls();
      TeXParser parser = resource.getParser();

      TeXObjectList list = new TeXObjectList();

      while (!stack.isEmpty())
      {
         TeXObject obj = stack.pop();

         if (obj instanceof SingleToken
               && ((SingleToken)obj).getCharCode() == terminator)
         {
            return list;
         }
         else
         {
            list.add(obj);
         }
      }

      throw new Bib2GlsException(bib2gls.getMessage(
        "error.invalid.condition_missing_end", (char)terminator));
   }

   protected void validate(GlsResource resource) throws Bib2GlsException
   {
      Bib2Gls bib2gls = resource.getBib2Gls();

      if (isEmpty())
      {
         throw new Bib2GlsException(
           bib2gls.getMessage("error.invalid.empty_condition"));
      }

      ConditionalUnary unary = null;
      ConditionalBinary binary = null;
      Conditional cond = null;

      Boolean result = null;

      for (int i = 0; i < size(); i++)
      {
         ConditionalListElement elem = get(i);

         if (elem instanceof ConditionalUnary)
         {
            unary = (ConditionalUnary)elem;

            i++;

            if (i == size())
            {
               throw new Bib2GlsException(
                  bib2gls.getMessage("error.invalid.unary_missing",
                    unary, toString()));
            }

            elem = get(i);
         }

         if (!(elem instanceof Conditional))
         {
            throw new Bib2GlsException(
               bib2gls.getMessage("error.invalid.condition_in", elem, toString()));
         }

         cond = (Conditional)elem;

         if (result == null)
         {
            result = Boolean.TRUE;
         }
         else if (binary != null)
         {
            binary = null;
         }
         else if (unary == null)
         {
            throw new Bib2GlsException(
               bib2gls.getMessage("error.invalid.binary", cond, toString()));
         }
         else
         {
            throw new Bib2GlsException(
               bib2gls.getMessage("error.invalid.binary", 
                unary.toString()+cond.toString(), toString()));
         }

         i++;

         if (i < size())
         {
            elem = get(i);

            if (elem instanceof ConditionalBinary)
            {
               binary = (ConditionalBinary)elem;
            }
            else
            {
               throw new Bib2GlsException(
                  bib2gls.getMessage("error.invalid.binary", elem, toString()));
            }
         }
      }
   }

   @Override
   public boolean booleanValue(Bib2GlsEntry entry) throws IOException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.logAndPrintMessage(
           "Entry: "+entry+". Evaluating condition group: "+toString());
      }

      if (isEmpty())
      {// errors should already be caught by validate()
         bib2gls.debugMessage("error.invalid.empty_condition");
         return false;
      }

      ConditionalUnary unary = null;
      ConditionalBinary binary = null;
      Conditional cond = null;

      Boolean result = null;

      for (int i = 0; i < size(); i++)
      {
         ConditionalListElement elem = get(i);

         if (elem instanceof ConditionalUnary)
         {
            unary = (ConditionalUnary)elem;

            i++;

            if (i == size())
            {
               bib2gls.debugMessage("error.invalid.unary_missing",
                    unary, toString());
               return false;
            }

            elem = get(i);
         }

         if (!(elem instanceof Conditional))
         {
            bib2gls.debugMessage(
               "error.invalid.condition_in", elem, toString());
            return false;
         }

         cond = (Conditional)elem;

         if (result == null)
         {
            if (unary == null)
            {
               result = Boolean.valueOf(cond.booleanValue(entry));
            }
            else
            {
               result = Boolean.valueOf(unary.booleanValue(entry, cond));
            }
         }
         else if (binary != null)
         {
            if (unary == null)
            {
               result = Boolean.valueOf(
                   binary.booleanValue(entry, result.booleanValue(), cond));
            }
            else
            {
               result = Boolean.valueOf(binary.booleanValue(entry,
                 result.booleanValue(), unary, cond));
            }

            binary = null;
         }
         else if (unary == null)
         {
            bib2gls.debugMessage("error.invalid.binary", cond, toString());
            return false;
         }
         else
         {
            bib2gls.debugMessage("error.invalid.binary", 
                unary.toString()+cond.toString(), toString());
            return false;
         }

         i++;

         if (i < size())
         {
            elem = get(i);

            if (elem instanceof ConditionalBinary)
            {
               binary = (ConditionalBinary)elem;
            }
            else
            {
               bib2gls.debugMessage("error.invalid.binary", elem, toString());
               return false;
            }
         }
      }

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.logAndPrintMessage(
           "Entry: "+entry+". Result from condition group: "
              +toString()+" : "+result);
      }

      return result.booleanValue();
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < size(); i++)
      {
         ConditionalListElement elem = get(i);

         if (elem instanceof ConditionalList)
         {
            builder.append("( ");

            builder.append(elem.toString());

            builder.append(" )");
         }
         else if (elem instanceof ConditionalBinary)
         {
            builder.append(" ");

            builder.append(elem.toString());

            builder.append(" ");
         }
         else if (elem instanceof ConditionalUnary)
         {
            builder.append(elem.toString());

            builder.append(" ");
         }
         else
         {
            builder.append(elem.toString());
         }
      }

      return builder.toString();
   }
}
