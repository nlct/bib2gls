/*
    Copyright (C) 2022 Nicola L.C. Talbot
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

import com.dickimawbooks.texparserlib.*;

public class GlsLike
{
   public GlsLike(String prefix, String csname)
   {
      this(prefix, csname, false);
   }

   public GlsLike(String prefix, String csname, boolean isGlsLinkLike)
   {
      this.prefix = prefix;
      this.csname = csname;
      this.isGlsLinkLike = isGlsLinkLike;
   }

   public String getPrefix()
   {
      return prefix;
   }

   public String getName()
   {
      return csname;
   }

   public void setFamily(GlsLikeFamily family)
   {
      this.family = family;
   }

   public GlsLikeFamily getFamily()
   {
      return family;
   }

   public boolean isGlsLinkLike()
   {
      return isGlsLinkLike;
   }

   @Override
   public String toString()
   {
      return String.format("%s[prefix=%s,csname=%s]", getClass().getSimpleName(),
       prefix, csname);
   }

   private String prefix, csname;
   private GlsLikeFamily family;
   private boolean isGlsLinkLike;
}
