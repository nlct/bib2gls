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

import java.util.Vector;
import java.io.IOException;

import com.dickimawbooks.texparserlib.TeXObjectList;
import com.dickimawbooks.texparserlib.UndefAction;

import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.LaTeXSty;

import com.dickimawbooks.texparserlib.auxfile.AuxData;
import com.dickimawbooks.texparserlib.html.L2HStringConverter;

public class InterpreterListener extends L2HStringConverter
{
   public InterpreterListener(Bib2Gls bib2gls, Vector<AuxData> data,
     Vector<String> customPackages)
   {
      super(new Bib2GlsAdapter(bib2gls), data, customPackages != null);
      this.bib2gls = bib2gls;
      this.customPackages = customPackages;

      setUndefinedAction(UndefAction.WARN);
      setUseMathJax(false);
      setIsInDocEnv(true);
   }

   @Override
   public void writeCodePoint(int codePoint) throws IOException
   {
      if (getWriter() == null) return;

      if (codePoint == '&')
      {
         getWriter().write("&amp;");
      }
      else if (codePoint == '<')
      {
         getWriter().write("&le;");
      }
      else if (codePoint == '>')
      {
         getWriter().write("&ge;");
      }
      else
      {
         getWriter().write(new String(Character.toChars(codePoint)));
      }
   }

   @Override
   protected LaTeXSty getLaTeXSty(KeyValList options, String styName,
     boolean loadParentOptions, TeXObjectList stack)
     throws IOException
   {
      if (styName.equals("texjavahelp"))
      {
         return new Bib2GlsTeXJavaHelpSty(options, this, loadParentOptions);
      }

      return super.getLaTeXSty(options, styName, loadParentOptions, stack);
   }

   @Override
   public void parsePackageFile(LaTeXSty sty, TeXObjectList stack) throws IOException
   {
      if (isParsePackageSupportOn() 
           && customPackages.contains(sty.getName()))
      {
         sty.parseFile(stack);
      }
   }

   Vector<String> customPackages;
   Bib2Gls bib2gls;
}
