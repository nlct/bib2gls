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

import java.io.IOException;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.TeXObjectList;
import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.WhiteSpace;

import com.dickimawbooks.texparserlib.bib.BibValue;
import com.dickimawbooks.texparserlib.bib.BibUserString;

/**
 * Represents a case-changing quark.
 */
public class FieldValueCase implements FieldValueElement
{
   public FieldValueCase(FieldValueElement fieldValueElem,
      FieldCaseChange caseChange)
   {
      this.fieldValueElem = fieldValueElem;
      this.caseChange = caseChange;
   }

   @Override
   public BibValue getValue(Bib2GlsEntry entry)
     throws Bib2GlsException,IOException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      BibValue bibValue = fieldValueElem.getValue(entry);

      if (bibValue == null || caseChange == FieldCaseChange.NO_CHANGE)
      {
         return bibValue;
      }

      GlsResource resource = entry.getResource();
      TeXParser parser = resource.getParser();
      TeXObjectList list = (TeXObjectList)bibValue.expand(parser).clone();

      if (!list.isEmpty())
      {
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
      }

      return new BibUserString(list);
   }

   @Override
   public String getStringValue(Bib2GlsEntry entry)
   throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      String text = fieldValueElem.getStringValue(entry);

      if (text == null)
      {
         return null;
      }

      GlsResource resource = entry.getResource();

      if (!text.isEmpty())
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

      return text;
   }

   @Override
   public String toString()
   {
      return String.format("\\%s{%s}", caseChange, fieldValueElem);
   }

   private FieldValueElement fieldValueElem;
   private FieldCaseChange caseChange;
}
