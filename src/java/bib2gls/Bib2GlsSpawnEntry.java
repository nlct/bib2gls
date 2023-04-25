/*
    Copyright (C) 2018-2023 Nicola L.C. Talbot
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
import java.util.Iterator;
import java.util.Vector;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2GlsSpawnEntry extends Bib2GlsProgenitor
{
   public Bib2GlsSpawnEntry(Bib2Gls bib2gls)
   {
      this(bib2gls, "spawnentry");
   }

   public Bib2GlsSpawnEntry(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);
   }

   protected void initMissingFields()
   {
   }

   public void checkRequiredFields()
   {
      super.checkRequiredFields();

      if (getField("name") == null && getField("parent") == null)
      {
         missingFieldWarning("name");
      }

      if (getField("description") == null)
      {
         missingFieldWarning("description");
      }
   }

   @Override
   public String getSortFallbackField()
   {
      String field = resource.getCustomEntryDefaultSortField(getOriginalEntryType());

      if (field != null)
      {
         return field;
      }

      return resource.getEntryDefaultSortField();
   }

   public String getFallbackValue(String field)
   {
      if (field.equals("text"))
      {
         return getFieldValue("name");
      }
      else if (field.equals("name"))
      {
         // get the parent 

         String parentid = getFieldValue("parent");

         if (parentid == null) return null;

         Bib2GlsEntry parent = resource.getEntry(parentid);

         if (parent == null) return null;

         String value = parent.getFieldValue("name");

         if (value != null) return value;

         return parent.getFallbackValue("name");
      }
      else if (field.equals("sort"))
      {
         return getSortFallbackValue();
      }
      else if (field.equals("first"))
      {
         String value = getFieldValue("text");

         if (value != null) return value;

         return getFallbackValue("text");
      }
      else if (field.equals("plural"))
      {
         return getPluralFallbackValue();
      }
      else if (field.equals("firstplural"))
      {
         String value = getFieldValue("first");

         if (value == null)
         {
             value = getFieldValue("first");
         }

         if (value != null)
         {
            String suffix = resource.getPluralSuffix();

            return suffix == null ? value : value+suffix;
         }

         value = getFieldValue("plural");

         return value == null ? getFallbackValue("plural") : value;
      }
      else if (field.equals("shortplural"))
      {
         String value = getFieldValue("short");

         if (value != null)
         {
            String suffix = resource.getShortPluralSuffix();

            return suffix == null ? value : value+suffix;
         }
      }
      else if (field.equals("longplural"))
      {
         String value = getFieldValue("long");

         if (value != null)
         {
            String suffix = resource.getPluralSuffix();

            return suffix == null ? value : value+suffix;
         }
      }

      return null;
   }

   public BibValueList getFallbackContents(String field)
   {
      if (field.equals("text"))
      {
         return getField("name");
      }
      else if (field.equals("name"))
      {
         // get the parent 

         String parentid = getFieldValue("parent");

         if (parentid == null) return null;

         Bib2GlsEntry parent = resource.getEntry(parentid);

         if (parent == null) return null;

         BibValueList value = parent.getField("name");

         if (value != null) return value;

         return parent.getFallbackContents("name");
      }
      else if (field.equals("sort"))
      {
         return getSortFallbackContents();
      }
      else if (field.equals("first"))
      {
         BibValueList contents = getField("text");

         return contents == null ? getFallbackContents("text") : contents;
      }
      else if (field.equals("plural"))
      {
         return getPluralFallbackContents();
      }
      else if (field.equals("firstplural"))
      {
         BibValueList contents = getField("first");

         if (contents == null)
         {
            contents = getField("plural");

            return contents == null ? getFallbackContents("plural") : contents;
         }

         return plural(contents, "glspluralsuffix");
      }
      else if (field.equals("longplural"))
      {
         return plural(getField("long"), "glspluralsuffix");
      }
      else if (field.equals("shortplural"))
      {
         return plural(getField("short"), "abbrvpluralsuffix");
      }
      else if (field.equals("duallongplural"))
      {
         return plural(getField("duallong"), "glspluralsuffix");
      }
      else if (field.equals("dualshortplural"))
      {
         return plural(getField("dualshort"), "abbrvpluralsuffix");
      }

      return null;
   }

}
