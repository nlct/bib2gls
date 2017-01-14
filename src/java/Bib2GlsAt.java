/*
    Copyright (C) 2017 Nicola L.C. Talbot
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

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsAt extends At
{
   protected void process(TeXParser parser, TeXObjectList entryTypeList,
     TeXObject contents, TeXObject eg)
     throws IOException
   {
      if (!(contents instanceof TeXObjectList))
      {
         throw new BibTeXSyntaxException(parser,
           BibTeXSyntaxException.ERROR_EXPECTING_OR,
           new String[] {"{", "("});
      }

      BibParser bibParser = (BibParser)parser.getListener();

      Bib2Gls bib2gls = (Bib2Gls)bibParser.getTeXApp();

      String entryType = entryTypeList.toString(parser).trim().toLowerCase();

      BibData data;

      if (entryType.equals("entry"))
      {
         data = new Bib2GlsEntry(bib2gls);
      }
      else if (entryType.equals("index"))
      {
         data = new Bib2GlsIndex(bib2gls);
      }
      else if (entryType.equals("acronym")
            || entryType.equals("abbreviation"))
      {
         data = new Bib2GlsAbbrev(bib2gls, entryType);
      }
      else if (entryType.equals("symbol")
            || entryType.equals("number"))
      {
         data = new Bib2GlsSymbol(bib2gls, entryType);
      }
      else
      {
         data = BibData.createBibData(entryType);

         if (data instanceof BibEntry)
         {
            bib2gls.warning(parser,
               bib2gls.getMessage("warning.ignoring.unknown.entry.type", 
               entryType));
         }
      }

      data.parseContents(parser, (TeXObjectList)contents, eg);

      bibParser.addBibData(data);
   }

}
