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
import com.dickimawbooks.texparserlib.bib.BibUserString;

import com.dickimawbooks.bibglscommon.Bib2GlsException;

/**
 * Represents the MGP quark. The group is from the most recent
 * successful pattern match in the conditional for the current field assignment list.
 * The group may be referenced by index or by name.
 */
public class FieldValueGroupMatch implements FieldValueElement
{
   public FieldValueGroupMatch(String name)
   {
      this.name = name;
      this.index = 0;
   }

   public FieldValueGroupMatch(int index)
   {
      this.index = index;
      this.name = null;
   }

   @Override
   public BibValue getValue(Bib2GlsEntry entry)
     throws Bib2GlsException,IOException
   {
      TeXParser parser = entry.getResource().getBibParser();

      String value = getStringValue(entry);

      if (value == null)
      {
         return null;
      }

      TeXObjectList content;

      if (value.indexOf("\\") != -1)
      {
         content = new TeXObjectList();
         parser.scan(value, content);
      }
      else
      {
         content = parser.getListener().createString(value);
      }

      return new BibUserString(content);
   }

   @Override
   public String getStringValue(Bib2GlsEntry entry) throws Bib2GlsException
   {
      if (name == null)
      {
         return entry.getResource().getLastMatchGroup(index);
      }
      else
      {
         return entry.getResource().getLastMatchGroup(name);
      }
   }

   @Override
   public String toString()
   {
      if (name == null)
      {
         return String.format("\\MGP{%d}", index);
      }
      else
      {
         return String.format("\\MGP{%s}", name);
      }
   }

   private String name;
   private int index;
}
