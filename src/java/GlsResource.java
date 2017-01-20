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
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.Collator;
import java.text.CollationKey;
import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.aux.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class GlsResource
{
   public GlsResource(TeXParser parser, AuxData data)
    throws IOException,InterruptedException
   {
      sources = new Vector<TeXPath>();

      init(parser, data.getArg(0), data.getArg(1));
   }

   private void init(TeXParser parser, TeXObject opts, TeXObject arg)
      throws IOException,InterruptedException
   {
      bib2gls = (Bib2Gls)parser.getListener().getTeXApp();

      TeXPath texPath = new TeXPath(parser, 
        arg.toString(parser), "glstex");

      texFile = texPath.getFile();

      bib2gls.registerTeXFile(texFile);

      String filename = texPath.getTeXPath(true);

      KeyValList list = KeyValList.getList(parser, opts);

      TeXObject srcList = null;

      for (Iterator<String> it = list.keySet().iterator(); it.hasNext(); )
      {
         String opt = it.next();

         if (opt.equals("src"))
         {
            srcList = list.getValue("src");

            CsvList csvList = CsvList.getList(parser, srcList);

            int n = csvList.size();

            if (n == 0)
            {
               sources.add(bib2gls.getBibFilePath(parser, filename));
            }
            else
            {
               for (TeXObject obj : csvList)
               {
                  sources.add(bib2gls.getBibFilePath(parser, 
                     obj.toString(parser).trim()));
               }
            }
         }
         else if (opt.equals("type"))
         {
            type = list.getValue(opt).toString(parser).trim();
         }
         else if (opt.equals("category"))
         {
            category = list.getValue(opt).toString(parser).trim();
         }
         else if (opt.equals("label-prefix"))
         {
            labelPrefix = list.getValue(opt).toString(parser).trim();

            if (labelPrefix.isEmpty())
            {
               labelPrefix = null;
            }
         }
         else if (opt.equals("sort"))
         {
            sort = list.getValue(opt).toString(parser).trim();

            if (sort.equals("none") || sort.equals("unsrt"))
            {
               sort = null;
            }
            else if (sort.isEmpty())
            {
               sort = "locale";
            }
         }
         else if (opt.equals("sort-field"))
         {
            sortField = list.getValue(opt).toString(parser).trim();

            if (!sortField.equals("id") && !bib2gls.isKnownField(sortField))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, sortField));
            }
         }
         else if (opt.equals("strength"))
         { // collator strength

            String strength = list.getValue(opt).toString(parser).trim();

            if (strength.equals("primary"))
            {
               collatorStrength = Collator.PRIMARY;
            }
            else if (strength.equals("secondary"))
            {
               collatorStrength = Collator.SECONDARY;
            }
            else if (strength.equals("tertiary"))
            {
               collatorStrength = Collator.TERTIARY;
            }
            else if (strength.equals("identical"))
            {
               collatorStrength = Collator.IDENTICAL;
            }
            else
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.choice.value", 
                  opt, strength, "primary, secondary, tertiary, identical"));
            }
         }
         else if (opt.equals("decomposition"))
         { // collator decomposition

            String decomposition = list.getValue(opt).toString(parser).trim();

            if (decomposition.equals("none"))
            {
               collatorDecomposition = Collator.NO_DECOMPOSITION;
            }
            else if (decomposition.equals("canonical"))
            {
               collatorDecomposition = Collator.CANONICAL_DECOMPOSITION;
            }
            else if (decomposition.equals("full"))
            {
               collatorDecomposition = Collator.FULL_DECOMPOSITION;
            }
            else
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.choice.value", 
                  opt, decomposition, "none, canonical, full"));
            }
         }
         else if (opt.equals("charset"))
         {
            bibCharset = Charset.forName(
                           list.getValue(opt).toString(parser).trim());
         }
         else if (opt.equals("suffixF"))
         {
            suffixF = list.getValue(opt).toString(parser).trim();
         }
         else if (opt.equals("suffixFF"))
         {
            suffixFF = list.getValue(opt).toString(parser).trim();
         }
         else if (opt.equals("see"))
         {
            String loc = list.getValue(opt).toString(parser).trim();

            if (loc.equals("omit"))
            {
               seeLocation = Bib2GlsEntry.NO_SEE;
            }
            else if (loc.equals("before"))
            {
               seeLocation = Bib2GlsEntry.PRE_SEE;
            }
            else if (loc.equals("after"))
            {
               seeLocation = Bib2GlsEntry.POST_SEE;
            }
            else
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.choice.value", 
                  opt, loc, "omit, before, after"));
            }
         }
         else if (opt.equals("loc-prefix"))
         {
            TeXObject prefixList = list.getValue(opt);

            CsvList csvList = CsvList.getList(parser, prefixList);

            int n = csvList.size();

            switch (n)
            {
               case 1:

                  String val = csvList.get(0).toString(parser);

                  if (val.equals("false"))
                  {
                     locationPrefix = null;
                     break;
                  }
                  else if (val.equals("list"))
                  {
                     locationPrefix = new String[] {"\\pagelistname "};
                     break;
                  }
                  else if (!val.equals("true"))
                  {
                     locationPrefix = new String[]{val};
                     break;
                  }

               // fall through to n=0 case if val == 'true'
               case 0:

                  locationPrefix = new String[]{bib2gls.getMessage("tag.page"),
                    bib2gls.getMessage("tag.pages")};

               break;

               default:

                  locationPrefix = new String[n];

                  for (int i = 0; i < n; i++)
                  {
                     locationPrefix[i] = csvList.get(i).toString(parser);
                  }
            }
         }
         else if (opt.equals("ignore-fields"))
         {

            CsvList csvList = CsvList.getList(parser, list.getValue(opt));

            int n = csvList.size();

            if (n == 0)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.missing.value", opt));
            }

            skipFields = new String[n];

            for (int i = 0; i < n; i++)
            {
               skipFields[i] = csvList.get(i).toString(parser);
            }
         }
         else if (opt.equals("selection"))
         {
            String val = list.getValue(opt).toString(parser).trim();

            if (val.isEmpty())
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.missing.value", opt));
            }

            selectionMode = -1;

            for (int i = 0; i < SELECTION_OPTIONS.length; i++)
            {
               if (val.equals(SELECTION_OPTIONS[i]))
               {
                  selectionMode = i;
                  break;
               }
            }

            if (selectionMode == -1)
            {
               StringBuilder choices = null;

               for (int i = 0; i < SELECTION_OPTIONS.length; i++)
               {
                  if (choices == null)
                  {
                     choices = new StringBuilder(70);
                  }
                  else
                  {
                     choices.append(", ");
                  }

                  choices.append('\'');
                  choices.append(SELECTION_OPTIONS[i]);
                  choices.append('\'');
               }

               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.choice.value", 
                  opt, val, choices));
            }
         }
         else
         {
            throw new IllegalArgumentException(
             bib2gls.getMessage("error.syntax.unknown_option", opt));
         }
      }

      if (selectionMode == SELECTION_ALL && "use".equals(sort))
      {
         bib2gls.warning(
            bib2gls.getMessage("error.option.clash", "selection=all",
            "sort=use"));

         sort = null;
      }

      bib2gls.verbose(bib2gls.getMessage("message.selection.mode", 
       SELECTION_OPTIONS[selectionMode]));

      bib2gls.verbose(bib2gls.getMessage("message.sort.mode", 
       sort == null ? "unsrt" : sort));

      if (srcList == null)
      {
         sources.add(bib2gls.getBibFilePath(parser, filename));
      }
   }

   public void parse(TeXParser parser)
   throws IOException
   {
      bibData = new Vector<Bib2GlsEntry>();

      Vector<GlsRecord> records = bib2gls.getRecords();

      for (TeXPath src : sources)
      {
         File bibFile = src.getFile();

         Charset srcCharset = bibCharset;

         if (srcCharset == null)
         {
            // search bib file for "% Encoding: <encoding>"

            BufferedReader reader = null;
            Pattern pattern = Pattern.compile("% Encoding: ([^\\s]+)");

            try
            {
               reader = new BufferedReader(new FileReader(bibFile));

               String line;
               int lineNum=0;

               while ((line = reader.readLine()) != null)
               {
                  lineNum++;
                  Matcher m = pattern.matcher(line);

                  if (m.matches())
                  {
                     String encoding = m.group(1);

                     try
                     {
                        srcCharset = Charset.forName(encoding);
                     }
                     catch (Exception e)
                     {
                        bib2gls.warning(bibFile, lineNum,
                         bib2gls.getMessage("warning.ignoring.unknown.encoding", 
                          encoding),
                         e);
                        srcCharset = bibCharset;
                     }

                     reader.close();
                     reader = null;
                     break;
                  }
               }
            }
            finally
            {
               if (reader != null)
               {
                  reader.close();
               }
            }
         }

         BibParser bibParserListener = new BibParser(bib2gls, srcCharset)
         {
            protected void addPredefined()
            {
               parser.putActiveChar(new Bib2GlsAt(labelPrefix));
            }

         };

         TeXParser texParser = bibParserListener.parseBibFile(bibFile);

         Vector<BibData> list = bibParserListener.getBibData();

         for (BibData data : list)
         {
            if (data instanceof Bib2GlsEntry)
            {
               Bib2GlsEntry entry = (Bib2GlsEntry)data;

               if (type != null)
               {
                  entry.putField("type", type);
               }

               if (category != null)
               {
                  entry.putField("category", category);
               }

               // does this entry have any records?

               boolean hasRecords = false;

               for (GlsRecord record : records)
               {
                  if (record.getLabel().equals(entry.getId()))
                  {
                     entry.addRecord(record);

                     hasRecords = true;
                  }
               }

               bibData.add(entry);

               if (hasRecords && selectionMode == SELECTION_RECORDED_AND_DEPS)
               {
                  // does this entry have a "see" field?

                  entry.initCrossRefs(parser);

                  for (Iterator<String> it = entry.getDependencyIterator();
                       it.hasNext(); )
                  {
                     String dep = it.next();

                     bib2gls.addDependent(dep);
                  }
               }
            }
            else if (data instanceof BibPreamble)
            {
                preamble = ((BibPreamble)data).getPreamble().expand(texParser)
                   .toString(texParser);
            }
         }
      }
   }

   private void addHierarchy(Bib2GlsEntry childEntry, 
      Vector<Bib2GlsEntry> entries)
   {
      String parentId = childEntry.getParent();

      if (parentId == null) return;

      // has parent already been added to entries?

      for (Bib2GlsEntry entry : entries)
      {
         if (entry.getId().equals(parentId))
         {
            // already added

            return;
         }
      }

      // find parent in bibData

      Bib2GlsEntry parent = getEntry(parentId);

      if (parent != null)
      {
         bib2gls.verbose(bib2gls.getMessage("message.added.parent", parentId));
         addHierarchy(parent, entries);
         entries.add(parent);
      }
   }

   private Bib2GlsEntry getEntry(String label)
   {
      for (Bib2GlsEntry entry : bibData)
      {
         if (entry.getId().equals(label))
         {
            return entry;
         }
      }

      return null;
   }

   public int processData()
      throws IOException,Bib2GlsException
   {
      if (bibData == null)
      {// shouldn't happen
         throw new NullPointerException(
            "No data (parse must come before processData)");
      }

      Vector<Bib2GlsEntry> entries = new Vector<Bib2GlsEntry>();

      Vector<String> fields = bib2gls.getFields();
      Vector<GlsRecord> records = bib2gls.getRecords();

      if (selectionMode == SELECTION_ALL)
      {
         // select all entries

         for (Bib2GlsEntry entry : bibData)
         {
            entries.add(entry);
         }
      }
      else if (sort == null)
      {
         // add all entries that have been recorded in the order of
         // definition

         for (Bib2GlsEntry entry : bibData)
         {
            if (entry.hasRecords())
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries);
               }

               entries.add(entry);
            }
         }
      }
      else
      {
         // Add all recorded entries in order of records.
         // (This means they'll be in the correct order if sort=use)

         for (int i = 0; i < records.size(); i++)
         {
            GlsRecord record = records.get(i);

            Bib2GlsEntry entry = getEntry(record.getLabel());

            if (entry != null && !entries.contains(entry))
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries);
               }

               entries.add(entry);
            }
         }
      }

      // add any dependencies

      if (selectionMode == SELECTION_RECORDED_AND_DEPS)
      {
         Vector<String> dependencies = bib2gls.getDependencies();

         for (String id : dependencies)
         {
            Bib2GlsEntry dep = getEntry(id);

            if (dep != null && !entries.contains(dep))
            {
               addHierarchy(dep, entries);
               entries.add(dep);
            }
         }
      }

      // sort if required

      int entryCount = entries.size();

      if (sort != null && !sort.equals("use") && entryCount > 0)
      {
         if (sort.equals("letter-case"))
         {
            Bib2GlsEntryLetterComparator comparator = 
               new Bib2GlsEntryLetterComparator(bib2gls, entries, 
                 sort, sortField, false);

            comparator.sortEntries();
         }
         else if (sort.equals("letter-nocase"))
         {
            Bib2GlsEntryLetterComparator comparator = 
               new Bib2GlsEntryLetterComparator(bib2gls, entries, 
                 sort, sortField, true);

            comparator.sortEntries();
         }
         else
         {
            Bib2GlsEntryComparator comparator = 
               new Bib2GlsEntryComparator(bib2gls, entries, sort, sortField, 
                  collatorStrength, collatorDecomposition);

            comparator.sortEntries();
         }
      }

      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));

      // Already checked openout_any in init method

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(texFile, bib2gls.getTeXCharset().name());

         if (seeLocation != Bib2GlsEntry.NO_SEE)
         {
            writer.println("\\providecommand{\\bibglsseesep}{, }");
            writer.println();
         }

         if (bib2gls.useGroupField())
         {
            writer.println("\\providecommand{\\bibglslettergroup}[5]{#1}");
            writer.println("\\providecommand{\\bibglsothergroup}[2]{\\glssymbolsgroupname}");
            writer.println();
         }

         if (locationPrefix != null)
         {
              writer.println("\\providecommand{\\bibglspostprefix}{\\ }");

              if (type == null)
              {
                 writer.println("\\appto\\glossarypreamble{%");
              }
              else
              {
                 writer.format("\\apptoglossarypreamble[%s]{%%%n", type);
              }

              writer.println(" \\providecommand{\\bibglsprefix}[1]{%");
              writer.println("  \\ifcase##1");

              for (int i = 0; i < locationPrefix.length; i++)
              {
                 writer.format("  \\%s %s\\bibglspostprefix%n",
                   (i == locationPrefix.length-1 ? "else" : "or"), 
                   locationPrefix[i]);
              }

              writer.println("  \\fi");
              writer.println(" }");

              writer.println("}");
         }

         // syntax: {label}{opts}{name}{description}

         writer.println("\\providecommand{\\bibglsnewentry}[4]{%");
         writer.print(" \\longnewglossaryentry*{#1}");
         writer.println("{name={#3},#2}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewsymbol}[4]{%");
         writer.print(" \\longnewglossaryentry*{#1}");
         writer.println("{name={#3},sort={#1},category={symbol},#2}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewnumber}[4]{%");
         writer.print(" \\longnewglossaryentry*{#1}");
         writer.println("{name={#3},sort={#1},category={number},#2}{#4}%");
         writer.println("}");

         // syntax: {label}{opts}
         writer.println("\\providecommand*{\\bibglsnewterm}[2]{%");
         writer.println(" \\newglossaryentry{#1}{name={#1},description={},#2}%");
         writer.println("}");

         // syntax: {label}{opts}{short}{long}
         writer.println("\\providecommand{\\bibglsnewacronym}[4]{%");
         writer.println("  \\newacronym[#2]{#1}{#3}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewabbreviation}[4]{%");
         writer.println("  \\newabbreviation[#2]{#1}{#3}{#4}%");
         writer.println("}");

         if (preamble != null)
         {
            writer.println();
            writer.println(preamble);
         }

         writer.println();

         for (Bib2GlsEntry entry : entries)
         {
            bib2gls.verbose(entry.getId());

            entry.updateLocationList(minLocationRange,
              suffixF, suffixFF, seeLocation, locationPrefix != null);

            entry.writeBibEntry(writer);

            writer.println();
         }

         bib2gls.message(bib2gls.getChoiceMessage("message.written", 0,
            "entry", 3, entries.size(), texFile.toString()));

      }
      finally
      {
         if (writer != null)
         {
            writer.close();
         }
      }

      return entryCount;
   }

   public boolean skipField(String field)
   {
      if (skipFields == null)
      {
         return false;
      }

      for (int i = 0; i < skipFields.length; i++)
      {
         if (skipFields[i].equals(field))
         {
            return true;
         }
      }

      return false;
   }

   private File texFile;

   private Vector<TeXPath> sources;

   private String[] skipFields = null;

   private String type=null, category=null, sort = "locale", sortField = "sort";

   private Charset bibCharset = null;

   private int minLocationRange = 3;

   private String suffixF, suffixFF;

   private String preamble = null;

   private Vector<Bib2GlsEntry> bibData;

   private Bib2Gls bib2gls;

   private int collatorStrength=Collator.PRIMARY;

   private int collatorDecomposition=Collator.CANONICAL_DECOMPOSITION;

   private int seeLocation=Bib2GlsEntry.POST_SEE;

   private String[] locationPrefix = null;

   private String labelPrefix = null;

   public static final int SELECTION_RECORDED_AND_DEPS=0;
   public static final int SELECTION_RECORDED_NO_DEPS=1;
   public static final int SELECTION_RECORDED_AND_PARENTS=2;
   public static final int SELECTION_ALL=3;

   private int selectionMode = SELECTION_RECORDED_AND_DEPS;

   private static final String[] SELECTION_OPTIONS = new String[]
    {"recorded and deps", "recorded no deps", "recorded and ancestors", "all"};
}

