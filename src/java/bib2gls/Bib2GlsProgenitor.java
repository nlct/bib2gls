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

public class Bib2GlsProgenitor extends Bib2GlsEntry 
  implements Bib2GlsMultiEntry
{
   public Bib2GlsProgenitor(Bib2Gls bib2gls)
   {
      this(bib2gls, "progenitor");
   }

   public Bib2GlsProgenitor(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);

      progeny = new Vector<Bib2GlsEntry>();
   }

   public void checkRequiredFields()
   {
      if (getField("progeny") == null || progeny.size() == 0)
      {
         missingFieldWarning("adoptparents");
      }
   }

   public String getFallbackValue(String field)
   {
      if (field.equals("name"))
      {
         return getOriginalId();
      }
      else
      {
         return super.getFallbackValue(field);
      }
   }

   public BibValueList getFallbackContents(String field)
   {
      if (field.equals("name"))
      {
         if (!bib2gls.useInterpreter())
         {
            bib2gls.warningMessage("warning.interpreter.needed.fallback",
              field, getId());
            return null;
         }

         String name = getOriginalId();
         BibValueList list = new BibValueList();
         list.add(new BibUserString(
            bib2gls.getInterpreterListener().createGroup(name)));

         return list;
      }
      else
      {
         return super.getFallbackContents(field);
      }
   }

   protected void initMissingFields()
   {
      if (getResource().getLabelPrefix() != null
         && getFieldValue("name") == null)
      {
         putField("name", getOriginalId());
      }
   }

   @Override
   protected Vector<String> processSpecialFields(
     boolean mfirstucProtect, String[] protectFields, String idField,
     Vector<String> interpretFields)
    throws IOException
   {
      interpretFields = super.processSpecialFields(mfirstucProtect,
        protectFields, idField, interpretFields);

      for (String field : Bib2Gls.SPAWN_SPECIAL_FIELDS)
      {    
         interpretFields = processField(field, mfirstucProtect,
           protectFields, idField, interpretFields);
      }

      return interpretFields;
   }

   public void populate(BibParser parserListener) throws IOException
   {
      BibValueList value = getField("adoptparents");

      if (value == null)
      {
         String progenyField = resource.getOriginalField("adoptparents");

         if (progenyField != null)
         {
            value = getField(progenyField);
         }
      }

      if (value == null)
      {
         // checkRequiredFields() will generate an error if this entry
         // is selected.
         return;
      }

      TeXParser parser = parserListener.getParser();

      TeXObjectList list = value.expand(parser);

      String strVal = list.toString(parser);

      if (resource.isInterpretLabelFieldsEnabled()
           && strVal.matches("(?s).*[\\\\\\{\\}].*"))
      {
         strVal = bib2gls.interpret(strVal, value, true);

         list = parser.getListener().createString(strVal);
      }

      CsvList csvList = CsvList.getList(parser, list);
      list = new TeXObjectList();

      String progenitorLabel = getOriginalId();
      String processedProgenitorLabel = processLabel(progenitorLabel);
      String adoptedParentField = resource.getAdoptedParentField();

      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < csvList.size(); i++)
      {
         String parentLabel = csvList.getValue(i).toString(parser).trim();

         String progenyEntryType = null;

         if (getEntryType().startsWith("spawn"))
         {
            progenyEntryType = getEntryType().substring(5);
         }
         else
         {
            progenyEntryType = "index";
         }

         String spawnedLabel = parentLabel+"."+getOriginalId();

         bib2gls.debugMessage("message.spawning", spawnedLabel, 
           getOriginalId());

         Bib2GlsEntry spawnedEntry = Bib2GlsAt.createBib2GlsEntry(bib2gls,
            "spawned"+progenyEntryType);

         if (spawnedEntry == null)
         {// shouldn't happen
            throw new NullPointerException();
         }

         progeny.add(spawnedEntry);

         spawnedEntry.setId(getPrefix(), spawnedLabel);
         spawnedEntry.setBase(getBaseFile());
         spawnedEntry.setOriginalEntryType(getOriginalEntryType());

         for (Iterator<String> it = getKeySet().iterator(); it.hasNext(); )
         {
            String field = it.next();

            if (!field.equals("alias") && !field.equals("parent"))
            {
               value = getField(field);

               if (value != null)
               {
                  spawnedEntry.putField(field, (BibValueList)value.clone());
               }
            }
         }

         spawnedEntry.putField("progenitor", processedProgenitorLabel);
         spawnedEntry.putField(adoptedParentField, parentLabel);

         value = new BibValueList();
         value.add(new BibUserString(parserListener.createString(
            processedProgenitorLabel)));
         spawnedEntry.putField("progenitor", value);

         value = new BibValueList();
         value.add(new BibUserString(parserListener.createString(parentLabel)));
         spawnedEntry.putField(adoptedParentField, value);

         addDependency(spawnedEntry.getId());

         if (list.size() > 0)
         {
            list.add(parserListener.getOther(','));
            builder.append(',');
         }

         String id = spawnedEntry.getId();
         list.addAll(parserListener.createString(id));
         builder.append(id);

         parserListener.addBibData(spawnedEntry);

         if (bib2gls.getDebugLevel() > 0)
         {
            for (Iterator<String> it = spawnedEntry.getKeySet().iterator();
                  it.hasNext(); )
            {
               String field = it.next();
               value = spawnedEntry.getField(field);
               bib2gls.debug(String.format("%s={%s}", field, 
                 value == null ? "" : value.expand(parser).toString(parser)));
            }
         }
      }

      value = new BibValueList();
      value.add(new BibUserString(list));

      putField("progeny", value);
      putField("progeny", builder.toString());
   }

   public void writeExtraFields(PrintWriter writer)
   throws IOException
   {
      String progeny = getFieldValue("progeny");

      if (progeny == null)
      {
         bib2gls.debugMessage("message.entry.lost.field", getId(), "progeny");
      }
      else
      {
         writer.println(String.format("\\GlsXtrSetField{%s}{progeny}{%s}",
           getId(), progeny));
      }
   }

   private Vector<Bib2GlsEntry> progeny;
}
