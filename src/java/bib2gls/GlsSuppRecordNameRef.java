/*
    Copyright (C) 2018 Nicola L.C. Talbot
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

import com.dickimawbooks.texparserlib.TeXPath;

public class GlsSuppRecordNameRef extends GlsRecordNameRef 
   implements SupplementalRecord
{
   public GlsSuppRecordNameRef(Bib2Gls bib2gls, String label, 
      String prefix, String counter, String format, String location, 
      String title, String href, String hcounter, TeXPath src)
   {
      super(bib2gls, label, prefix, counter, format, location, title, href,
        hcounter);
      this.src = src;
   }

   protected GlsSuppRecordNameRef(Bib2Gls bib2gls, String label, 
      String prefix, String counter, String format, String location, 
      String title, String href, String hcounter, TeXPath src, long index)
   {
      super(bib2gls, label, prefix, counter, format, location, title, href, 
        hcounter, index);
      this.src = src;
   }

   public GlsRecord copy(String newLabel)
   {
      return new GlsSuppRecordNameRef(bib2gls, newLabel, getPrefix(), 
        getCounter(), getFormat(), getLocation(), getTitle(), getHref(),
        getHcounter(), src, getIndex());
   }

   public Object clone()
   {
      return new GlsSuppRecordNameRef(bib2gls, getLabel(), getPrefix(), 
       getCounter(), getFormat(), getLocation(), getTitle(), getHref(),
       getHcounter(), src, getIndex());
   }

   public TeXPath getSource()
   {
      return src;
   }

   public String getFmtTeXCode()
   {
      String fmt = getFormat();

      if (fmt.isEmpty())
      {
         fmt = "glsnumberformat";
      }
      else if (fmt.startsWith("(") || fmt.startsWith(")"))
      {
         if (fmt.length() == 1)
         {
            fmt = "glsnumberformat";
         }
         else
         {
            fmt = fmt.substring(1);
         }
      }

      return String.format(
         "\\glsxtrdisplaylocnameref{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
         getPrefix(), getCounter(), fmt, getLocation(),
         getTitle(), getHref(), getHcounter(), bib2gls.getTeXPathHref(src));
   }

   public boolean locationMatch(GlsRecord record)
   {
      if (!(record instanceof GlsSuppRecordNameRef))
      {
         return false;
      }

      GlsSuppRecordNameRef suppRecord = (GlsSuppRecordNameRef)record;

      if (!src.equals(suppRecord.src))
      {
         return false;
      }

      return super.locationMatch(record);
   }

   public boolean equals(Object obj)
   {
      if (!(obj instanceof GlsSuppRecordNameRef) || !super.equals(obj))
      {
         return false;
      }

      return src.equals(((GlsSuppRecordNameRef)obj).src);
   }

   public boolean partialMatch(GlsRecord record)
   {
      if (!(record instanceof GlsSuppRecordNameRef) 
            || !super.partialMatch(record))
      {
         return false;
      }

      return src.equals(((GlsSuppRecordNameRef)record).src);
   }

   public String toString()
   {
      return String.format(
        "{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
         getLabel(), getPrefix(), getCounter(), getFormat(), 
         getLocation(), getTitle(), getHref(), getHcounter(), 
         bib2gls.getTeXPathHref(src));
   }

   private TeXPath src;
}
