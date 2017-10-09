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

import java.io.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;
import java.text.CollationKey;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2GlsDualEntry extends Bib2GlsEntry
{
   public Bib2GlsDualEntry(Bib2Gls bib2gls)
   {
      this(bib2gls, "dualentry");
   }

   public Bib2GlsDualEntry(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   public boolean isPrimary()
   {
      return isprimary;
   }

   public boolean hasTertiary()
   {
      return false;
   }

   public String getFallbackValue(String field)
   {
      String val = super.getFallbackValue(field);

      if (val != null) return val;

      if (field.equals("descriptionplural"))
      {
         val = getFieldValue("description");

         if (val == null)
         {
            return val;
         }

         String suffix = getResource().getDualPluralSuffix();

         return suffix == null ? val : val+suffix;
      }
      else if (field.equals("symbolplural"))
      {
         val = getFieldValue("symbol");

         if (val == null)
         {
            val = getFallbackValue("symbol");
         }

         if (val == null)
         {
            return val;
         }

         String suffix = getResource().getDualPluralSuffix();

         return suffix == null ? val : val+suffix;
      }
      else if (field.equals("longplural"))
      {
         val = getFieldValue("long");

         if (val == null)
         {
            return val;
         }

         String suffix = getResource().getDualPluralSuffix();

         return suffix == null ? val : val+suffix;
      }
      else if (field.equals("shortplural"))
      {
         val = getFieldValue("short");

         if (val == null)
         {
            val = getFallbackValue("short");
         }

         if (val == null)
         {
            return val;
         }

         String suffix = getResource().getDualShortPluralSuffix();

         return suffix == null ? val : val+suffix;
      }

      return null;
   }

   public HashMap<String,String> getMappings()
   {
      return getResource().getDualEntryMap();
   }

   public String getFirstMap()
   {
      return getResource().getFirstDualEntryMap();
   }

   public boolean backLink()
   {
      return getResource().backLinkFirstDualEntryMap();
   }

   protected Bib2GlsDualEntry createDualEntry()
   {
      return new Bib2GlsDualEntry(bib2gls, getEntryType());
   }

   public Bib2GlsEntry createDual()
   {
      GlsResource resource = getResource();
      String dualPrefix = resource.getDualPrefix();
      String label = getOriginalId();

      Bib2GlsDualEntry entry = createDualEntry();
      entry.setId(dualPrefix, label);
      entry.setBase(getBase());
      entry.isprimary=false;

      HashMap<String,String> mappings = getMappings();

      String firstKey = null;
      String dualKey = null;
      String primaryKey = null;

      if (backLink())
      {
         firstKey = getFirstMap();
         dualKey = mappings.get(firstKey);
      }

      for (Iterator<String> it = getKeySet().iterator(); it.hasNext(); )
      {
         String key = getKey(it.next());

         String map = mappings.get(key);
         BibValueList contents = getField(key);
         String value = getFieldValue(key);

         if (contents != null && value != null && !key.equals("alias"))
         {
            if (map == null)
            {
               entry.putField(key, contents);
               entry.putField(key, value);
            }
            else if (!map.equals("alias"))
            {
               entry.putField(map, contents);
               entry.putField(map, value);
            }
         }
      }

      // Has a sort field been supplied without a mapping?

      String dualSortField = resource.getDualSortField();

      if (!dualSortField.equals("sort"))
      {
         BibValueList contents = getField("sort");
         String map = mappings.get("sort");

         if (contents != null && map != null)
         {
            String value = getFieldValue("sort");

            entry.putField(map, contents);
            entry.putField(map, value);
         }
      }

      // check for missing fields.

      for (Iterator<String> it = mappings.keySet().iterator(); it.hasNext();)
      {
         String key = it.next();

         String value = getFieldValue(key);

         if (value == null)
         {
            value = getFallbackValue(key);

            if (value != null)
            {
               putField(key, value);
               putField(key, getFallbackContents(key));
            }
         }

         String map = mappings.get(key);

         if (firstKey != null && primaryKey == null)
         {
            if (map.equals(firstKey))
            {
               primaryKey = key;
            }
         }

         if (entry.getFieldValue(map) == null)
         {
            value = getFallbackValue(key);

            if (value == null)
            {
               bib2gls.verbose(bib2gls.getMessage("message.no.fallback",
                  getEntryType(), key));
            }
            else
            {
               entry.putField(map, getFallbackContents(key));
               entry.putField(map, value);
            }
         }
      }

      if (primaryKey != null)
      {
         String val = getFieldValue(primaryKey);

         putField(primaryKey, String.format("\\glshyperlink[%s]{%s}",
           val, entry.getId()));
      }

      if (dualKey != null)
      {
         String val = entry.getFieldValue(dualKey);

         entry.putField(dualKey, String.format("\\glshyperlink[%s]{%s}",
           val, getId()));
      }

      String dualField = resource.getDualField();

      if (dualField != null)
      {
         entry.putField(dualField, getId());
         putField(dualField, entry.getId());
      }

      return entry;
   }

   private boolean isprimary=true;
}
