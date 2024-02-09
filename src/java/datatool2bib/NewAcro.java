/*
    Copyright (C) 2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.datatool2bib;

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.*;

public class NewAcro extends NewTerm
{
   public NewAcro(DataTool2Bib datatool2bib)
   {
      this("newacro", datatool2bib);
   }

   public NewAcro(String name, DataTool2Bib datatool2bib)
   {
      super(name, datatool2bib);
   }

   @Override
   public Object clone()
   {
      return new NewAcro(getName(), datatool2bib);
   }

   @Override
   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      KeyValList options = TeXParserUtils.popOptKeyValList(parser, stack);
      TeXObject shortArg = popArg(parser, stack);
      TeXObject longArg = popArg(parser, stack);

      if (options == null)
      {
         options = new KeyValList();
      }

      TeXObject name = (TeXObject)shortArg.clone();

      options.put("short", shortArg);
      options.put("long", longArg);

      processEntry(name, options, parser, stack);
   }

}
