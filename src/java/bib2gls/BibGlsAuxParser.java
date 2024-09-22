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
package com.dickimawbooks.bibgls.bib2gls;

import java.io.IOException;
import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.Input;
import com.dickimawbooks.texparserlib.auxfile.*;

public class BibGlsAuxParser extends AuxParser
{
   public BibGlsAuxParser(Bib2Gls bib2gls, Charset auxCharset)
   throws IOException
   {
      super(bib2gls, auxCharset);
      this.bib2gls = bib2gls;
   }

   @Override
   protected void addPredefined()
   {
      super.addPredefined();

      putControlSequence(new Input("@bibgls@input", Input.NOT_FOUND_ACTION_WARN, false));

      putControlSequence(new AuxBibGlsOptions(bib2gls));

      addAuxCommand("glsxtr@resource", 2);
      addAuxCommand("glsxtr@fields", 1);
      addAuxCommand("glsxtr@record", 5);
      addAuxCommand("glsxtr@recordsee", 2);
      addAuxCommand("glsxtr@record@nameref", 8);
      addAuxCommand("glsxtr@select@entry", 5);
      addAuxCommand("glsxtr@select@entry@nameref", 8);
      addAuxCommand("glsxtr@texencoding", 1);
      addAuxCommand("glsxtr@langtag", 1);
      addAuxCommand("glsxtr@shortcutsval", 1);
      addAuxCommand("glsxtr@pluralsuffixes", 4);
      addAuxCommand("@glsxtr@altmodifier", 1);
      addAuxCommand("@glsxtr@newglslike", 2);
      addAuxCommand("@glsxtr@newglslikefamily", 8);
      addAuxCommand("@glsxtr@prefixlabellist", 1);
      addAuxCommand("@glsxtr@multientry", 4);
      addAuxCommand("@glsxtr@mglsrefs", 1);
      addAuxCommand("@glsxtr@mglslike", 1);
      addAuxCommand("@mfu@excls", 1);
      addAuxCommand("@mfu@blockers", 1);
      addAuxCommand("@mfu@mappings", 1);
      addAuxCommand("@newglossary", 4);
   }

   private Bib2Gls bib2gls;
}
