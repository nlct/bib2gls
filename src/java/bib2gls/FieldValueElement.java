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

import com.dickimawbooks.texparserlib.bib.BibValue;

public interface FieldValueElement
{
   /**
    * Gets the field's value as a BibValue.
    * @param entry the entry being queried
    * @throws Bib2GlsException if there's a problem with the option
    * syntax
    * @throws IOException if something goes wrong with the TeXParser
    * (indicates a problem in parsing, mostly like a bug)
    */ 
   public BibValue getValue(Bib2GlsEntry entry)
     throws IOException,Bib2GlsException;

   /**
    * Gets the field's value as a String.
    * @param entry the entry being queried
    * @throws Bib2GlsException if there's a problem with the option
    * syntax
    * @throws IOException if something goes wrong with the TeXParser
    * (indicates a problem in parsing, mostly like a bug)
    */ 
   public String getStringValue(Bib2GlsEntry entry)
     throws IOException,Bib2GlsException;
}
