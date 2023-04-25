/*
    Copyright (C) 2017-2023 Nicola L.C. Talbot
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

import java.io.*;

import com.dickimawbooks.texparserlib.*;

public class Bib2GlsAdapter extends TeXAppAdapter
{
   public Bib2GlsAdapter(Bib2Gls bib2gls)
   {
      super();

      this.bib2gls = bib2gls;
   }

   public String getMessage(String label, Object... params)
   {
      return bib2gls.getMessage(label, params);
   }

   public void message(String text)
   {
      bib2gls.debug("texparserlib: "+text);
   }

   public void warning(TeXParser parser, String message)
   {
      bib2gls.debug("texparserlib: "+message);
   }

   public void error(Exception e)
   {
      if (e instanceof TeXSyntaxException)
      {
         bib2gls.debug("texparserlib: "+ 
          ((TeXSyntaxException)e).getMessage(this));
      }
      else
      {
         bib2gls.debug("texparserlib: ", e);
      }
   }

   public boolean isReadAccessAllowed(TeXPath path)
   {
      return bib2gls.isReadAccessAllowed(path);
   }

   public boolean isWriteAccessAllowed(TeXPath path)
   {
      return false;
   }

   public boolean isWriteAccessAllowed(File file)
   {
      return false;
   }

   private Bib2Gls bib2gls;
}
