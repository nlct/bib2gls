/*
    Copyright (C) 2017-2022 Nicola L.C. Talbot
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
import java.io.File;
import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.BibParser;

public class Bib2GlsBibParser extends BibParser
{
   public Bib2GlsBibParser(Bib2Gls bib2gls, GlsResource resource, Charset bibCharset)
    throws IOException
   {
      super(bib2gls, bibCharset, false);

      this.resource = resource;

      init();
   }

   private void init()
   {
      bibParser = new TeXParser(this);

      int atcode = bibParser.getCatCode('@');
      int hashcode = bibParser.getCatCode('#');
      int tildecode = TeXParser.TYPE_ACTIVE;

      if (maketildeother)
      {
         tildecode = bibParser.getCatCode('~');
      }

      bibParser.setCatCode('@', TeXParser.TYPE_ACTIVE);
      bibParser.setCatCode('#', TeXParser.TYPE_OTHER);

      if (maketildeother)
      {
         parser.setCatCode('~', TeXParser.TYPE_OTHER);
      }
   }

   public TeXParser getParser()
   {
      return bibParser;
   }

   protected void addPredefined()
   {
      parser.putActiveChar(new Bib2GlsAt(resource));

      boolean nbsp = resource.useNonBreakSpace();

      parser.putActiveChar(new Bib2GlsNbsp(nbsp));
      parser.putControlSequence(new Bib2GlsNoBreakSpace(nbsp));
   }

   public void parse(File file)
      throws IOException
   {
      parser.parse(file, getCharSet());
   }

   public void beginParse(File file, Charset encoding)
      throws IOException
   {
      super.beginParse(file, encoding);
      currentFile = file;
   }

   public String getBase()
   {
      if (currentFile == null) return null;

      String base = currentFile.getName();

      int idx = base.lastIndexOf(".bib");

      if (idx == -1)
      {
         idx = base.toLowerCase().lastIndexOf(".bib");
      }

      return idx < 0 ? base.substring(0, idx+1) : base;
   }

   private GlsResource resource;
   private TeXParser bibParser;
   private File currentFile=null;
}
