/*
    Copyright (C) 2017-2024 Nicola L.C. Talbot
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Vector;
import java.util.Iterator;

public class GlsRecordNameRef extends GlsRecord
{
   public GlsRecordNameRef(Bib2Gls bib2gls, String label, String prefix, String counter,
      String format, String location, String title, String href, String hcounter)
   {
      super(bib2gls, label, prefix, counter, format, location);
      this.title = title;
      this.href = href;
      this.hcounter = hcounter;
   }

   protected GlsRecordNameRef(Bib2Gls bib2gls, String label, String prefix, String counter,
      String format, String location, String title, String href, String hcounter, long index)
   {
      super(bib2gls, label, prefix, counter, format, location, index);
      this.title = title;
      this.href = href;
      this.hcounter = hcounter;
   }

   @Override
   public GlsRecord copy(String newLabel)
   {
      return new GlsRecordNameRef(bib2gls, newLabel, getPrefix(), 
        getCounter(), getFormat(), getLocation(), title, href, hcounter, 
        getIndex());
   }

   @Override
   public Object clone()
   {
      return new GlsRecordNameRef(bib2gls, getLabel(), getPrefix(), 
       getCounter(), getFormat(), getLocation(), title, href, hcounter,
        getIndex());
   }

   public String getTitle()
   {
      return title;
   }

   public String getHref()
   {
      return href;
   }

   public String getHcounter()
   {
      return hcounter;
   }

   @Override
   public void merge(String newFmt, GlsRecord otherRecord)
   {
      super.merge(newFmt, otherRecord);

      if (otherRecord instanceof GlsRecordNameRef)
      {
         GlsRecordNameRef namerefRec = (GlsRecordNameRef)otherRecord;
         title = namerefRec.title;
         href = namerefRec.href;
         hcounter = namerefRec.hcounter;
      }
   }

   @Override
   public String getFmtTeXCode(String theLocation)
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
         "\\glsxtrdisplaylocnameref{%s}{%s}{%s}{%s}{%s}{%s}{%s}{}",
         getPrefix(), getCounter(), fmt, theLocation,
         getTitle(), getHref(), getHcounter());
   }

   public boolean locationMatch(GlsRecord rec)
   {
      if (bib2gls.mergeNameRefOnLocation()
          || (bib2gls.mergeWrGlossaryLocations() 
            && getCounter().equals("wrglossary")))
      {
         return super.locationMatch(rec);
      }

      if (!(rec instanceof GlsRecordNameRef))
      {
         return false;
      }

      // don't match if the counter name isn't the same

      if (!getCounter().equals(rec.getCounter()))
      {
         return false;
      }

      GlsRecordNameRef nameref = (GlsRecordNameRef)rec;

      if (bib2gls.mergeNameRefOnTitle())
      {
         return title.equals(nameref.title);
      }

      return href.equals(nameref.href);
   }

   public boolean equals(Object obj)
   {
      if (bib2gls.mergeWrGlossaryLocations() 
            && getCounter().equals("wrglossary"))
      {
         return super.equals(obj);
      }

      if (!super.equals(obj))
      {
         return false;
      }

      if (!(obj instanceof GlsRecordNameRef)) return false;

      GlsRecordNameRef rec = (GlsRecordNameRef)obj;

      return href.equals(rec.href) && title.equals(rec.title)
              && hcounter.equals(rec.hcounter);
   }

   /*
    * Match all parts except the format.
    */ 
   public boolean partialMatch(GlsRecord rec)
   {
      if (bib2gls.mergeNameRefOnLocation()
           || (bib2gls.mergeWrGlossaryLocations() 
                && getCounter().equals("wrglossary"))
           || !(rec instanceof GlsRecordNameRef))
      {
         return super.partialMatch(rec);
      }

      if (!getLabel().equals(rec.getLabel()))
      {
         return false;
      }

      // don't match if the counter name isn't the same

      if (!getCounter().equals(rec.getCounter()))
      {
         return false;
      }

      if (bib2gls.mergeNameRefOnHcounter())
      {
         return hcounter.equals(((GlsRecordNameRef)rec).hcounter);
      }

      if (bib2gls.mergeNameRefOnTitle())
      {
         return title.equals(((GlsRecordNameRef)rec).title);
      }

      return href.equals(((GlsRecordNameRef)rec).href);
   }

   public String toString()
   {
      return String.format(
        "{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{}",
         getLabel(), getPrefix(), getCounter(), getFormat(), 
         getLocation(), getTitle(), getHref(), getHcounter());
   }

   private String title, href, hcounter;
}
