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
import com.dickimawbooks.texparserlib.bib.BibValue;
import com.dickimawbooks.texparserlib.bib.BibValueList;
import com.dickimawbooks.texparserlib.bib.BibUserString;

public class Field implements FieldValueElement
{
   public Field(GlsResource resource, FieldReference fieldRef,
     String name, Field follow)
     throws Bib2GlsException
   {
      if (fieldRef == null || resource == null)
      {
         throw new NullPointerException();
      }

      this.resource = resource;
      this.fieldRef = fieldRef;
      setFollow(follow);
      setName(name);
   }

   public Field(GlsResource resource, FieldReference fieldRef)
     throws Bib2GlsException
   {
      this(resource, fieldRef, null, null);
   }

   public String getName()
   {
      return name;
   }

   public void setCaseChange(FieldCaseChange caseChange)
   {
      this.caseChange = caseChange;
   }

   public FieldCaseChange getCaseChange()
   {
      return caseChange;
   }

   protected void setName(String name)
     throws Bib2GlsException
   {
      if (name != null)
      {
         Bib2Gls bib2gls = resource.getBib2Gls();

         if (fieldRef == FieldReference.ENTRY_TYPE
            || fieldRef == FieldReference.ENTRY_LABEL)
         {
            if (!(name.equals("original") || name.equals("actual")))
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                 "error.invalid.field_identifier", name, fieldRef.getTag()));
            }
         }
         else if (bib2gls.isPrivateNonBibField(name))
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.invalid.field_name", name));
         }
         else if (!(bib2gls.isKnownField(name)
                    || bib2gls.isKnownSpecialField(name)
                    || bib2gls.isNonBibField(name)
                    || name.equals("entrytype")
                    || name.equals("entrylabel")
                    || name.equals("original")
                    || name.equals("actual")))
         {
            resource.addUserField(name);
         } 
      }

      this.name = name;
   }

   public Field getFollow()
   {
      return follow;
   }

   protected void setFollow(Field field)
     throws IllegalArgumentException
   {
      if (field != null && (fieldRef == FieldReference.ENTRY_TYPE
            || fieldRef == FieldReference.ENTRY_LABEL))
      {
         throw new IllegalArgumentException(
          "Field reference "+field.fieldRef.getTag()
          + " can't follow "+fieldRef.getTag());
      }

      follow = field;
   }

   public Field getLast()
   {
      if (follow == null)
      {
         return this;
      }
      else
      {
         return follow.getLast();
      }
   }

   @Override
   public BibValue getValue(Bib2GlsEntry entry)
    throws IOException
   {
      Bib2GlsEntry refEntry = fieldRef.getEntry(entry);

      if (refEntry == null)
      {
         return null;
      }

      TeXParser parser = resource.getBibParser();

      BibValue bibValue = null;
      TeXObject value = null;

      if (follow == null)
      {
         if (fieldRef == FieldReference.ENTRY_TYPE)
         {
            String text;

            if (name.equals("original"))
            {
               text = refEntry.getOriginalEntryType();
            }
            else
            {
               text = refEntry.getEntryType();
            }

            value = parser.getListener().createString(text);
            bibValue = new BibUserString(value);
         }
         else if (fieldRef == FieldReference.ENTRY_LABEL)
         {
            String text;

            if (name.equals("original"))
            {
               text = refEntry.getOriginalId();
            }
            else
            {
               text = refEntry.getId();
            }

            value = parser.getListener().createString(text);
            bibValue = new BibUserString(value);
         }
         else
         {
            bibValue = refEntry.getField(name);

            if (bibValue == null)
            {
               MissingFieldAction action = resource.getFallbackOnMissingAssignAction();

               if (action == MissingFieldAction.FALLBACK)
               {
                  bibValue = refEntry.getFallbackContents(name);
               }
               else if (action == MissingFieldAction.EMPTY)
               {
                  bibValue = new BibValueList();
                  value = new TeXObjectList();
               }
            }
         }
      }
      else
      {
         return follow.getValue(refEntry);
      }

      if (bibValue != null && caseChange != FieldCaseChange.NO_CHANGE)
      {
         if (value == null)
         {
            value = bibValue.expand(parser);
         }

         if (!value.isEmpty() && parser.isStack(value))
         {
            TeXObjectList list = (TeXObjectList)value.clone();

            switch (caseChange)
            {
               case UC:
                  resource.toUpperCase(list, parser.getListener());
               break;
               case LC:
                  resource.toLowerCase(list, parser.getListener());
               break;
               case FIRST_UC:
                  resource.toSentenceCase(list, parser.getListener());
               break;
               case FIRST_LC:
                  resource.toNonSentenceCase(list, parser.getListener());
               break;
               case TITLE:
                  resource.toTitleCase(list, parser.getListener());
               break;
            }

            bibValue = new BibUserString(list);
         }
      }

      return bibValue;
   }

   @Override
   public String getStringValue(Bib2GlsEntry entry)
   throws IOException
   {
      Bib2GlsEntry refEntry = fieldRef.getEntry(entry);

      if (refEntry == null)
      {
         return null;
      }

      String text = null;

      if (follow == null)
      {
         TeXParser parser = resource.getBibParser();

         if (fieldRef == FieldReference.ENTRY_TYPE)
         {
            if (name.equals("original"))
            {
               text = refEntry.getOriginalEntryType();
            }
            else
            {
               text = refEntry.getEntryType();
            }
         }
         else if (fieldRef == FieldReference.ENTRY_LABEL)
         {
            if (name.equals("original"))
            {
               text = refEntry.getOriginalId();
            }
            else
            {
               text = refEntry.getId();
            }
         }
         else
         {
            text = refEntry.getFieldValue(name);
         }

         if (text == null)
         {
            BibValue val = getValue(entry);

            if (val == null) return null;

            TeXObjectList valList = val.expand(parser);

            return valList.toString(parser);
         }

         if (!(caseChange == FieldCaseChange.NO_CHANGE || text.isEmpty()))
         {
            switch (caseChange)
            {
               case UC:
                  text = resource.toUpperCase(text).toString();
               break;
               case LC:
                  text = resource.toLowerCase(text).toString();
               break;
               case FIRST_UC:
                  text = resource.toSentenceCase(text).toString();
               break;
               case FIRST_LC:
                  text = resource.toNonSentenceCase(text).toString();
               break;
               case TITLE:
                  text = resource.toTitleCase(text).toString();
               break;
            }
         }

      }
      else
      {
         text = follow.getStringValue(refEntry);
      }

      return text;
   }

   public static Field popField(GlsResource resource, TeXObjectList stack)
     throws Bib2GlsException,IOException
   {
      Bib2Gls bib2gls = resource.getBib2Gls();
      TeXParser parser = resource.getParser();

      StringBuilder current = new StringBuilder();
      StringBuilder full = new StringBuilder();

      String tag = null;
      Field field = null;

      if (bib2gls.getDebugLevel() > 0)
      {
         bib2gls.logAndPrintMessage("Parsing field from "+stack.toString(parser));
      }

      while (!stack.isEmpty())
      {
         TeXObject object = stack.peek();

         if (object instanceof WhiteSpace)
         {
            if (tag == null && current.length() > 0)
            {
               tag = current.toString();
               current.setLength(0);
            }

            full.append(object.toString(parser));
            stack.pop();
         }
         else if (object instanceof SingleToken)
         {
            int cp = ((SingleToken)object).getCharCode();

            boolean isFollow = false;

            if (cp == FOLLOW_MARKER.charAt(0) && stack.size() > 1)
            {
               TeXObject nextObj = stack.get(1);

               if (nextObj instanceof SingleToken)
               {
                  int nextCp = ((SingleToken)nextObj).getCharCode();

                  if (nextCp == FOLLOW_MARKER.charAt(1))
                  {
                     isFollow = true;
                  }
               }
            }

            if (isFollow)
            {
               if (current.length() > 0)
               {
                  tag = current.toString();
                  current.setLength(0);
               }

               try
               {
                  if (field == null)
                  {
                     field = new Field(resource, FieldReference.getReference(tag));
                  }
                  else
                  {
                     field.getLast().setFollow(
                       new Field(resource, FieldReference.getReference(tag)));
                  }
               }
               catch (IllegalArgumentException e)
               {
                  String remaining = stack.toString(parser).trim();

                  if (remaining.isEmpty())
                  {
                     throw new Bib2GlsException(
                       bib2gls.getMessage("error.invalid.field_ref", tag),
                       e);
                  }
                  else
                  {
                     throw new Bib2GlsException(bib2gls.getMessage(
                       "error.invalid.field_ref_before", tag, remaining), e);
                  }
               }

               tag = null;

               full.append(FOLLOW_MARKER);
               stack.pop();
               stack.pop();
            }
            else if (cp == ',' || cp == '=' || cp == '<' || cp == '>'
                    || cp == '+' || cp == '[' || cp == ']'
                    || cp == '('  || cp == ')'
                    || cp == '!' || cp == '&' || cp == '|'
                    || tag != null)
            {
               break;
            }
            else
            {
               current.appendCodePoint(cp);
               full.appendCodePoint(cp);
               stack.pop();
            }
         }
         else if (tag != null || object instanceof ControlSequence)
         {
            break;
         }
         else
         {
            String str = object.toString(parser);
            current.append(str);
            full.append(str);
            stack.pop();
         }
      }

      if (tag == null && current.length() > 0)
      {
         tag = current.toString();
      }

      if (tag == null)
      {
         if (field != null)
         {
            Field lastField = field.getLast();

            if (lastField.fieldRef == FieldReference.PARENT)
            {
               lastField.fieldRef = FieldReference.SELF;
               lastField.name = "parent";

               return field;
            }
         }

         String parsed = full.toString().trim();

         if (parsed.isEmpty())
         {
            String remaining = stack.toString(parser).trim();

            if (remaining.isEmpty())
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                 "error.missing.field"));
            }
            else
            {
               throw new Bib2GlsException(bib2gls.getMessage(
                 "error.missing.field_before", remaining));
            }
         }
         else
         {
            throw new Bib2GlsException(bib2gls.getMessage(
              "error.missing.field_after", parsed));
         }
      }

      if (field == null)
      {
         return new Field(resource, FieldReference.SELF, tag, null);
      }

      field.getLast().setName(tag);

      return field;
   }

   public String toString()
   {
      if (follow == null)
      {
         return fieldRef.getTag()+" " + FOLLOW_MARKER + " "+name;
      }
      else
      {
         return fieldRef.getTag()+" " + FOLLOW_MARKER + " "+follow.toString();
      }
   }

   private GlsResource resource;
   private FieldReference fieldRef;
   private String name;
   private Field follow;
   private FieldCaseChange caseChange = FieldCaseChange.NO_CHANGE;

   public static final String FOLLOW_MARKER = "->";
}
