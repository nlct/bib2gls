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

      texFile = bib2gls.resolveFile(texPath.getFile());

      bib2gls.registerTeXFile(texFile);

      String filename = texPath.getTeXPath(true);

      KeyValList list = KeyValList.getList(parser, opts);

      TeXObject srcList = null;

      for (Iterator<String> it = list.keySet().iterator(); it.hasNext(); )
      {
         String opt = it.next();

         if (opt.equals("src"))
         {
            srcList = list.getValue(opt);

            CsvList csvList = CsvList.getList(parser, srcList);

            int n = csvList.size();

            if (n == 0)
            {
               sources.add(bib2gls.getBibFilePath(parser, filename));
            }
            else
            {
               for (int i = 0; i < n; i++)
               {
                  TeXObject obj = csvList.getValue(i);

                  sources.add(bib2gls.getBibFilePath(parser, 
                     obj.toString(parser).trim()));
               }
            }
         }
         else if (opt.equals("ext-prefixes"))
         {
            CsvList csvList = CsvList.getList(parser, list.getValue(opt));

            int n = csvList.size();

            if (n == 0)
            {
               externalPrefixes = null;
            }
            else
            {
               externalPrefixes = new String[n];

               for (int i = 0; i < n; i++)
               {
                  TeXObject obj = csvList.getValue(i);

                  externalPrefixes[i] = obj.toString(parser).trim();
               }
            }
         }
         else if (opt.equals("dual-entry-map"))
         {
            CsvList csvList = CsvList.getList(parser, list.getValue(opt));

            if (csvList.size() != 2)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   csvList.toString(parser)));
            }

            CsvList list1 = CsvList.getList(parser, csvList.getValue(0));
            CsvList list2 = CsvList.getList(parser, csvList.getValue(1));

            int n = list1.size();

            if (n != list2.size())
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.dual.map", opt, 
                   list.get(opt).toString(parser), n, list2.size()));
            }

            dualEntryMap = new HashMap<String,String>();

            for (int i = 0; i < n; i++)
            {
               TeXObject obj1 = list1.getValue(i);
               TeXObject obj2 = list2.getValue(i);
               dualEntryMap.put(obj1.toString(parser), obj2.toString(parser));
            }
         }
         else if (opt.equals("dual-abbrv-map"))
         {
            CsvList csvList = CsvList.getList(parser, list.getValue(opt));

            if (csvList.size() != 2)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   csvList.toString(parser)));
            }

            CsvList list1 = CsvList.getList(parser, csvList.getValue(0));
            CsvList list2 = CsvList.getList(parser, csvList.getValue(1));

            int n = list1.size();

            if (n != list2.size())
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.dual.map", opt, 
                   list.get(opt).toString(parser), n, list2.size()));
            }

            dualAbbrvMap = new HashMap<String,String>();

            for (int i = 0; i < n; i++)
            {
               TeXObject obj1 = list1.getValue(i);
               TeXObject obj2 = list2.getValue(i);
               dualAbbrvMap.put(obj1.toString(parser), obj2.toString(parser));
            }
         }
         else if (opt.equals("dual-symbol-map"))
         {
            CsvList csvList = CsvList.getList(parser, list.getValue(opt));

            if (csvList.size() != 2)
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", opt, 
                   csvList.toString(parser)));
            }

            CsvList list1 = CsvList.getList(parser, csvList.getValue(0));
            CsvList list2 = CsvList.getList(parser, csvList.getValue(1));

            int n = list1.size();

            if (n != list2.size())
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.dual.map", opt, 
                   list.get(opt).toString(parser), n, list2.size()));
            }

            dualSymbolMap = new HashMap<String,String>();

            for (int i = 0; i < n; i++)
            {
               TeXObject obj1 = list1.getValue(i);
               TeXObject obj2 = list2.getValue(i);
               dualSymbolMap.put(obj1.toString(parser), obj2.toString(parser));
            }
         }
         else if (opt.equals("type"))
         {
            type = list.getValue(opt).toString(parser).trim();
         }
         else if (opt.equals("dual-type"))
         {
            dualType = list.getValue(opt).toString(parser).trim();
         }
         else if (opt.equals("category"))
         {
            category = list.getValue(opt).toString(parser).trim();
         }
         else if (opt.equals("dual-category"))
         {
            dualCategory = list.getValue(opt).toString(parser).trim();
         }
         else if (opt.equals("label-prefix"))
         {
            labelPrefix = list.getValue(opt).toString(parser).trim();

            if (labelPrefix.isEmpty())
            {
               labelPrefix = null;
            }
         }
         else if (opt.equals("dual-prefix"))
         {
            dualPrefix = list.getValue(opt).toString(parser).trim();

            if (dualPrefix.isEmpty())
            {
               dualPrefix = null;
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
         else if (opt.equals("dual-sort"))
         {
            dualSort = list.getValue(opt).toString(parser).trim();

            if (dualSort.equals("none") || dualSort.equals("unsrt"))
            {
               dualSort = "none";
            }
            else if (dualSort.isEmpty())
            {
               dualSort = "locale";
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
         else if (opt.equals("dual-sort-field"))
         {
            dualSortField = list.getValue(opt).toString(parser).trim();

            if (!dualSortField.equals("id") 
             && !bib2gls.isKnownField(dualSortField))
            {
               throw new IllegalArgumentException(
                 bib2gls.getMessage("error.invalid.opt.value", 
                    opt, dualSortField));
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
         else
         {
            throw new IllegalArgumentException(
             bib2gls.getMessage("error.syntax.unknown_option", opt));
         }
      }

      if (selectionMode == SELECTION_ALL && "use".equals(sort))
      {
         bib2gls.warning(
            bib2gls.getMessage("warning.option.clash", "selection=all",
            "sort=use"));

         sort = null;
      }

      if ((labelPrefix == null && dualPrefix == null)
        ||(labelPrefix != null && dualPrefix != null
           && labelPrefix.equals(dualPrefix)))
      {
         throw new IllegalArgumentException(
            bib2gls.getMessage("error.option.clash",
            String.format("label-prefix={%s}", 
               labelPrefix == null ? "" : labelPrefix),
            String.format("dual-prefix={%s}", 
               dualPrefix == null ? "" : dualPrefix)));
      }

      if (dualEntryMap == null)
      {
         dualEntryMap = new HashMap<String,String>();
         dualEntryMap.put("name", "description");
         dualEntryMap.put("plural", "descriptionplural");
         dualEntryMap.put("description", "name");
         dualEntryMap.put("descriptionplural", "plural");
      }

      if (dualAbbrvMap == null)
      {
         dualAbbrvMap = new HashMap<String,String>();
         dualAbbrvMap.put("long", "description");
         dualAbbrvMap.put("longplural", "descriptionplural");
         dualAbbrvMap.put("short", "symbol");
         dualAbbrvMap.put("shortplural", "symbolplural");
         dualAbbrvMap.put("symbol", "short");
         dualAbbrvMap.put("symbolplural", "shortplural");
         dualAbbrvMap.put("description", "long");
         dualAbbrvMap.put("descriptionplural", "longplural");
      }

      if (dualSymbolMap == null)
      {
         dualSymbolMap = new HashMap<String,String>();
         dualSymbolMap.put("name", "symbol");
         dualSymbolMap.put("plural", "symbolplural");
         dualSymbolMap.put("symbol", "name");
         dualSymbolMap.put("symbolplural", "plural");
      }

      if (dualType == null)
      {
         dualType = type;
      }

      if (dualSort == null)
      {
         dualSort = sort;
      }
      else if (dualSort.equals("none"))
      {
         dualSort = null;
      }

      if (dualSortField == null)
      {
         dualSortField = sortField;
      }

      if (bib2gls.getVerboseLevel() > 0)
      {
         bib2gls.logMessage();
         bib2gls.verbose(bib2gls.getMessage("message.selection.mode", 
          SELECTION_OPTIONS[selectionMode]));

         if (skipFields != null)
         {
            bib2gls.verbose(bib2gls.getMessage("message.ignore.fields")); 

            for (int i = 0; i < skipFields.length; i++)
            {
               bib2gls.verbose(skipFields[i]);
            }

            bib2gls.logMessage();
         }

         bib2gls.verbose(bib2gls.getMessage("message.sort.mode", 
          sort == null ? "none" : sort));

         bib2gls.verbose(bib2gls.getMessage("message.sort.field", 
          sortField));

         bib2gls.verbose(bib2gls.getMessage("message.label.prefix", 
            labelPrefix == null ? "" : labelPrefix));

         bib2gls.verbose(bib2gls.getMessage("message.dual.label.prefix", 
            dualPrefix == null ? "" : dualPrefix));

         bib2gls.verbose(bib2gls.getMessage("message.dual.sort.mode", 
            dualSort == null ? "none" : dualSort));

         bib2gls.verbose(bib2gls.getMessage("message.dual.sort.field", 
            dualSortField));

         bib2gls.logMessage();
         bib2gls.verbose(bib2gls.getMessage("message.dual.entry.mappings")); 

         for (Iterator<String> it = dualEntryMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualEntryMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verbose(bib2gls.getMessage("message.dual.symbol.mappings")); 

         for (Iterator<String> it = dualSymbolMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualSymbolMap.get(key)));
         }

         bib2gls.logMessage();

         bib2gls.verbose(bib2gls.getMessage(
            "message.dual.abbreviation.mappings")); 

         for (Iterator<String> it = dualAbbrvMap.keySet().iterator(); 
              it.hasNext(); )
         {
            String key = it.next();
            bib2gls.verbose(String.format("%s -> %s", 
               key, dualAbbrvMap.get(key)));
         }

         bib2gls.logMessage();
      }

      if (srcList == null)
      {
         sources.add(bib2gls.getBibFilePath(parser, filename));
      }
   }

   public void parse(TeXParser parser)
   throws IOException
   {
      bibData = new Vector<Bib2GlsEntry>();
      dualData = new Vector<Bib2GlsEntry>();

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
               parser.putActiveChar(new Bib2GlsAt());
            }

         };

         TeXParser texParser = bibParserListener.parseBibFile(bibFile);

         Vector<BibData> list = bibParserListener.getBibData();

         for (int i = 0; i < list.size(); i++)
         {
            BibData data = list.get(i);

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

               Bib2GlsEntry dual = null;

               if (entry instanceof Bib2GlsDualEntry)
               {
                  dual = ((Bib2GlsDualEntry)entry).createDual();
                  entry.setDual(dual);
                  dual.setDual(entry);
               }

               // does this entry have any records?

               boolean hasRecords = false;
               boolean dualHasRecords = false;

               for (GlsRecord record : records)
               {
                  if (record.getLabel().equals(entry.getId()))
                  {
                     entry.addRecord(record);

                     hasRecords = true;
                  }

                  if (dual != null)
                  {
                     if (record.getLabel().equals(dual.getId()))
                     {
                        dual.addRecord(record);

                        dualHasRecords = true;
                     }
                  }
               }

               if (discard(entry)) continue;

               bibData.add(entry);

               if (dual != null)
               {
                  if (discard(dual)) continue;

                  if (dualSort.equals("combine"))
                  {
                     if (dualCategory != null)
                     {
                        dual.putField("category", dualCategory);
                     }

                     if (dualType != null)
                     {
                        dual.putField("type", dualType);
                     }

                     bibData.add(dual);
                  }
                  else
                  {
                     dualData.add(dual);
                  }
               }

               if (selectionMode == SELECTION_RECORDED_AND_DEPS)
               {
                  if (hasRecords)
                  {
                     // does this entry have a "see" field?

                     entry.initCrossRefs(parser);

                     for (Iterator<String> it = entry.getDependencyIterator();
                          it.hasNext(); )
                     {
                        String dep = it.next();

                        bib2gls.addDependent(dep);
                     }

                     if (dual != null)
                     {
                        bib2gls.addDependent(dual.getId());
                     }
                  }

                  if (dualHasRecords)
                  {
                     // Does the "see" field need setting?

                     if (dual.getFieldValue("see") == null)
                     {
                        dual.initCrossRefs(parser);
                     }

                     for (Iterator<String> it = dual.getDependencyIterator();
                          it.hasNext(); )
                     {
                        String dep = it.next();

                        bib2gls.addDependent(dep);
                     }

                     bib2gls.addDependent(entry.getId());
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
      Vector<Bib2GlsEntry> entries, Vector<Bib2GlsEntry> data)
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

      Bib2GlsEntry parent = getEntry(parentId, data);

      if (parent != null)
      {
         bib2gls.verbose(bib2gls.getMessage("message.added.parent", parentId));
         addHierarchy(parent, entries, data);
         entries.add(parent);
      }
   }

   private Bib2GlsEntry getEntry(String label, Vector<Bib2GlsEntry> data)
   {
      for (Bib2GlsEntry entry : data)
      {
         if (entry.getId().equals(label))
         {
            return entry;
         }
      }

      return null;
   }

   private void processData(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries,
      String entrySort, String entrySortField)
      throws Bib2GlsException
   {
      Vector<String> fields = bib2gls.getFields();
      Vector<GlsRecord> records = bib2gls.getRecords();

      if (selectionMode == SELECTION_ALL)
      {
         // select all entries

         for (Bib2GlsEntry entry : data)
         {
            entries.add(entry);
         }
      }
      else if (entrySort == null)
      {
         // add all entries that have been recorded in the order of
         // definition

         for (Bib2GlsEntry entry : data)
         {
            if (entry.hasRecords())
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
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

            Bib2GlsEntry entry = getEntry(record.getLabel(), data);

            if (entry != null && !entries.contains(entry))
            {
               if (selectionMode == SELECTION_RECORDED_AND_DEPS
                 ||selectionMode == SELECTION_RECORDED_AND_PARENTS)
               {
                  addHierarchy(entry, entries, data);
               }

               entries.add(entry);
            }
         }
      }

      processDepsAndSort(data, entries, entrySort, entrySortField);
   }

   private void processDepsAndSort(Vector<Bib2GlsEntry> data, 
      Vector<Bib2GlsEntry> entries,
      String entrySort, String entrySortField)
      throws Bib2GlsException
   {
      // add any dependencies

      if (selectionMode == SELECTION_RECORDED_AND_DEPS)
      {
         Vector<String> dependencies = bib2gls.getDependencies();

         for (String id : dependencies)
         {
            Bib2GlsEntry dep = getEntry(id, data);

            if (dep != null && !entries.contains(dep))
            {
               addHierarchy(dep, entries, data);
               entries.add(dep);
            }
         }
      }

      // sort if required

      int entryCount = entries.size();

      if (entrySort != null && !entrySort.equals("use") && entryCount > 0)
      {
         if (entrySort.equals("letter-case"))
         {
            Bib2GlsEntryLetterComparator comparator = 
               new Bib2GlsEntryLetterComparator(bib2gls, entries, 
                 entrySort, entrySortField, false);

            comparator.sortEntries();
         }
         else if (entrySort.equals("letter-nocase"))
         {
            Bib2GlsEntryLetterComparator comparator = 
               new Bib2GlsEntryLetterComparator(bib2gls, entries, 
                 entrySort, entrySortField, true);

            comparator.sortEntries();
         }
         else
         {
            Bib2GlsEntryComparator comparator = 
               new Bib2GlsEntryComparator(bib2gls, entries, 
                  entrySort, entrySortField, 
                  collatorStrength, collatorDecomposition);

            comparator.sortEntries();
         }
      }
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

      processData(bibData, entries, sort, sortField);

      Vector<Bib2GlsEntry> dualEntries = null;

      int entryCount = entries.size();

      if (dualData.size() > 0)
      {
         dualEntries = new Vector<Bib2GlsEntry>();

         // If dual-sort=use or none, use the same order as entries. 

         if (dualSort == null || dualSort.equals("use"))
         {
            for (Bib2GlsEntry entry : entries)
            {
               if (entry instanceof Bib2GlsDualEntry)
               {
                  Bib2GlsEntry dual = entry.getDual();

                  if (dualCategory != null)
                  {
                     dual.putField("category", dualCategory);
                  }

                  if (dualType != null)
                  {
                     dual.putField("type", dualType);
                  }

                  dualEntries.add(dual);
               }
            }
         }
         else
         {
            for (Bib2GlsEntry dual : dualData)
            {
               if (dualCategory != null)
               {
                  dual.putField("category", dualCategory);
               }

               if (dualType != null)
               {
                  dual.putField("type", dualType);
               }

               dualEntries.add(dual);
            }
         }

         processDepsAndSort(dualData, dualEntries, dualSort, dualSortField);

         entryCount += dualEntries.size();
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

         writer.println("\\providecommand{\\bibglsnewdualentry}[4]{%");
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

         if (dualEntries != null)
         {
            for (Bib2GlsEntry entry : dualEntries)
            {
               bib2gls.verbose(entry.getId());

               entry.updateLocationList(minLocationRange,
                 suffixF, suffixFF, seeLocation, locationPrefix != null);

               entry.writeBibEntry(writer);

               writer.println();
            }
         }

         bib2gls.message(bib2gls.getChoiceMessage("message.written", 0,
            "entry", 3, entryCount, texFile.toString()));

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

   public String getLabelPrefix()
   {
      return labelPrefix;
   }

   public String getDualPrefix()
   {
      return dualPrefix;
   }

   public String getExternalPrefix(int idx)
   {
      if (externalPrefixes == null) return null;

      if (idx >= 1 && idx <= externalPrefixes.length)
      {
         return externalPrefixes[idx-1];
      }

      return null;
   }

   public String getDualSortField()
   {
      return dualSortField;
   }

   public String getDualDescPluralSuffix()
   {
      return dualDescPluralSuffix;
   }

   public String getDualSymbolPluralSuffix()
   {
      return dualSymbolPluralSuffix;
   }

   public String getPluralSuffix()
   {
      return pluralSuffix;
   }

   public String getShortPluralSuffix()
   {
      return shortPluralSuffix;
   }

   public HashMap<String,String> getDualEntryMap()
   {
      return dualEntryMap;
   }

   public HashMap<String,String> getDualSymbolMap()
   {
      return dualSymbolMap;
   }

   public HashMap<String,String> getDualAbbrvMap()
   {
      return dualAbbrvMap;
   }

   // Allow for entries to be filtered out
   public boolean discard(Bib2GlsEntry entry)
   {
      return false;
   }

   private File texFile;

   private Vector<TeXPath> sources;

   private String[] skipFields = null;

   private String[] externalPrefixes = null;

   private String type=null, category=null, sort = "locale", sortField = "sort";

   private String dualType=null, dualCategory=null, 
      dualSort = null, dualSortField = "sort";

   private String pluralSuffix="\\glspluralsuffix ";

   private String shortPluralSuffix="\\abbrvpluralsuffix ";

   private String dualSymbolPluralSuffix="s", dualDescPluralSuffix="s";

   private Charset bibCharset = null;

   private int minLocationRange = 3;

   private String suffixF, suffixFF;

   private String preamble = null;

   private Vector<Bib2GlsEntry> bibData;

   private Vector<Bib2GlsEntry> dualData;

   private Bib2Gls bib2gls;

   private int collatorStrength=Collator.PRIMARY;

   private int collatorDecomposition=Collator.CANONICAL_DECOMPOSITION;

   private int seeLocation=Bib2GlsEntry.POST_SEE;

   private String[] locationPrefix = null;

   private String labelPrefix = null, dualPrefix="dual.";

   private HashMap<String,String> dualEntryMap, dualAbbrvMap,
      dualSymbolMap;

   public static final int SELECTION_RECORDED_AND_DEPS=0;
   public static final int SELECTION_RECORDED_NO_DEPS=1;
   public static final int SELECTION_RECORDED_AND_PARENTS=2;
   public static final int SELECTION_ALL=3;

   private int selectionMode = SELECTION_RECORDED_AND_DEPS;

   private static final String[] SELECTION_OPTIONS = new String[]
    {"recorded and deps", "recorded no deps", "recorded and ancestors", "all"};
}

