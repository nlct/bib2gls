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

import java.io.*;
import java.util.Vector;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsBibTeXEntry extends Bib2GlsEntry 
 implements Bib2GlsMultiEntry
{
   public Bib2GlsBibTeXEntry(Bib2Gls bib2gls)
   {
      this(bib2gls, "bibtexentry");
   }

   public Bib2GlsBibTeXEntry(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);

      contributorList = new Vector<Bib2GlsEntry>();
   }

   public void checkRequiredFields()
   {// no required fields
   }

   @Override
   public String getSortFallbackField()
   {
      String field = resource.getCustomEntryDefaultSortField(getOriginalEntryType());

      if (field != null)
      {
         return field;
      }

      return resource.getBibTeXEntryDefaultSortField();
   }

   public String getFallbackValue(String field)
   {
      String val;

      if (field.equals("sort"))
      {
         return getSortFallbackValue();
      }

      if (field.equals("name"))
      {
         return getOriginalId();
      }

      return super.getFallbackValue(field);
   }

   public BibValueList getFallbackContents(String field)
   {
      BibValueList val;

      if (field.equals("sort"))
      {
         return getSortFallbackContents();
      }

      if (field.equals("name") && bib2gls.useInterpreter())
      {
         String name = getOriginalId();
         BibValueList list = new BibValueList();
         list.add(new BibUserString(
            bib2gls.getInterpreterListener().createGroup(name)));

         return list;
      }

      return super.getFallbackContents(field);
   }

   @Override
   public void parseFields() throws Bib2GlsException,IOException
   {
      if (!fieldsParsed())
      {// bibtex's type key conflicts with bib2gls's so if found rename
         BibValueList value = removeField("type");

         if (value != null)
         {
            putField("bibtype", value);
         }
      }

      super.parseFields();
   }

   public void populate(BibParser parserListener) throws IOException
   {
      Vector<Contributor> contributors = getAuthors(parserListener.getParser());

      if (contributors != null)
      {
         addContributors(parserListener, contributors);
      }

      contributors = getEditors(parserListener.getParser());

      if (contributors != null)
      {
         addContributors(parserListener, contributors);
      }
   }

   protected void addContributors(BibParser parserListener, 
      Vector<Contributor> contributors)
    throws IOException
   {
      for (Contributor contributor : contributors)
      {
         if (contributor instanceof EtAl)
         {
            continue;
         }

         TeXObjectList nameList = new TeXObjectList();
         nameList.add(new TeXCsRef("bibglscontributor"));

         Group group = parserListener.createGroup();
         nameList.add(group);

         TeXObject forenames = contributor.getForenamesObject();

         if (forenames != null)
         {
            group.add((TeXObject)forenames.clone());
         }

         TeXObject von = contributor.getVonPartObject();

         group = parserListener.createGroup();
         nameList.add(group);

         if (von != null)
         {
            group.add((TeXObject)von.clone());
         }

         TeXObject surname = contributor.getSurnameObject();

         group = parserListener.createGroup();
         nameList.add(group);

         if (surname != null)
         {
            group.add((TeXObject)surname.clone());
         }

         TeXObject suffix = contributor.getSuffixObject();

         group = parserListener.createGroup();
         nameList.add(group);

         if (suffix != null)
         {
            group.add((TeXObject)suffix.clone());
         }

         BibValueList value = new BibValueList();
         value.add(new BibUserString(nameList));

         String label = bib2gls.convertToLabel(parserListener.getParser(), 
           value, resource, false);

         BibEntry entry = parserListener.getBibEntry(label);

         if (entry == null)
         {
            entry = new Bib2GlsContributor(bib2gls);
            entry.setId(label);
            entry.putField("name", value);

            parserListener.addBibData(entry);
         }

         if (entry instanceof Bib2GlsEntry)
         {
            contributorList.add((Bib2GlsEntry)entry);
         }

         if (entry instanceof Bib2GlsContributor)
         {
            ((Bib2GlsContributor)entry).addTitle(this);
         }
      }
   }

   public Vector<Bib2GlsEntry> getContributors()
   {
      return contributorList;
   }

   public void writeInternalFields(PrintWriter writer)
   throws IOException
   {
      super.writeInternalFields(writer);

      for (Bib2GlsEntry contributor : contributorList)
      {
         if (contributor.isSelected())
         {
            writer.println(String.format("\\glsxtrfieldlistadd{%s}{%s}{%s}",
               getId(), contributor.getEntryType(), contributor.getId()));
         }
      }
   }

   public void writeCsDefinition(PrintWriter writer)
   throws IOException
   {
      super.writeCsDefinition(writer);

      resource.writeBibGlsContributorDef(writer);
   }

   @Override
   public void initCrossRefs()
    throws IOException
   {
      for (Bib2GlsEntry contributor : contributorList)
      {
         addDependency(contributor.getId());
         contributor.addCrossRefdBy(this);
      }

      super.initCrossRefs();
   }

   private Vector<Bib2GlsEntry> contributorList;
}
