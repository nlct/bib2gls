/*
    Copyright (C) 2023-2024 Nicola L.C. Talbot
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

import com.dickimawbooks.texparserlib.bib.BibValue;
import com.dickimawbooks.texparserlib.bib.BibValueList;
import com.dickimawbooks.texparserlib.bib.BibUserString;

import com.dickimawbooks.bibglscommon.Bib2GlsException;

/**
 * Represents the LABELIFY quark.
 */
public class FieldValueLabelify implements FieldValueElement
{
   public FieldValueLabelify(FieldValueElement fieldValueElem, boolean allowLists)
   {
      this.fieldValueElem = fieldValueElem;
      this.allowLists = allowLists;
   }

   @Override
   public BibValue getValue(Bib2GlsEntry entry)
     throws Bib2GlsException,IOException
   {
      String value = getStringValue(entry);

      if (value == null)
      {
         return null;
      }
      else
      {
         TeXParser parser = entry.getResource().getParser();

         return new BibUserString(parser.getListener().createString(value));
      }
   }

   @Override
   public String getStringValue(Bib2GlsEntry entry)
   throws IOException,Bib2GlsException
   {
      Bib2Gls bib2gls = entry.getBib2Gls();
      BibValue value = fieldValueElem.getValue(entry);

      if (value == null)
      {
         return null;
      }
      else
      {
         GlsResource resource = entry.getResource();

         TeXParser parser = resource.getParser();

         TeXObjectList list = (TeXObjectList)value.expand(parser).clone();

         BibValueList bibValueList;

         if (value instanceof BibValueList)
         {
            bibValueList = (BibValueList)value;
         }
         else
         {
            bibValueList = new BibValueList();
            bibValueList.add(value);
         }

         return bib2gls.convertToLabel(parser, bibValueList, resource, allowLists);
      }
   }

   @Override
   public String toString()
   {
      if (allowLists)
      {
         return String.format("\\LABELIFYLIST{%s}", fieldValueElem);
      }
      else
      {
         return String.format("\\LABELIFY{%s}", fieldValueElem);
      }
   }

   private FieldValueElement fieldValueElem;
   private boolean allowLists=false;
}
