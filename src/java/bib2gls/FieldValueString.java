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
package com.dickimawbooks.bibgls.bib2gls;

import com.dickimawbooks.texparserlib.TeXObject;

import com.dickimawbooks.texparserlib.bib.BibValue;
import com.dickimawbooks.texparserlib.bib.BibUserString;

/**
 * A literal string field value element.
 */
public class FieldValueString implements FieldValueElement
{
   public FieldValueString(TeXObject content, boolean quoted)
   {
      if (content == null)
      {
         throw new NullPointerException();
      }

      this.content = content;
      this.quoted = quoted;
   }

   @Override
   public BibValue getValue(Bib2GlsEntry entry)
   {
      return new BibUserString(content);
   }

   @Override
   public String getStringValue(Bib2GlsEntry entry)
   {
      return content.toString(entry.getResource().getBibParser());
   }

   @Override
   public String toString()
   {
      if (quoted)
      {
         return String.format("\"%s\"", content.format());
      }
      else
      {
         return String.format("{%s}", content.format());
      }
   }

   private TeXObject content;
   private boolean quoted;
}
