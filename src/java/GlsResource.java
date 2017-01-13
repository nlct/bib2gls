package com.dickimawbooks.bib2gls;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.Collator;
import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.aux.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class GlsResource
{
   public GlsResource(TeXParser parser, AuxData data)
    throws IOException
   {
      sources = new Vector<TeXPath>();

      init(parser, data.getArg(0), data.getArg(1));
   }

   private void init(TeXParser parser, TeXObject opts, TeXObject arg)
      throws IOException
   {
      TeXPath texPath = new TeXPath(parser, 
        arg.toString(parser), "glstex");

      texFile = texPath.getFile();

      String filename = texPath.getTeXPath(true);

      KeyValList list = KeyValList.getList(parser, opts);

      TeXObject srcList = null;

      for (Iterator<String> it = list.keySet().iterator(); it.hasNext(); )
      {
         String opt = it.next();

         if (opt.equals("src"))
         {
            srcList = list.get("src");

            if (srcList instanceof TeXObjectList 
                && ((TeXObjectList)srcList).size() == 1)
            {
               srcList = ((TeXObjectList)srcList).popArg(parser);
            }

            CsvList csvList = CsvList.getList(parser, srcList);

            int n = csvList.size();

            if (n == 0)
            {
               sources.add(new TeXPath(parser, filename, "bib"));
            }
            else
            {
               for (TeXObject obj : csvList)
               {
                  sources.add(new TeXPath(parser, obj.toString(parser), "bib"));
               }
            }
         }
         else if (opt.equals("type"))
         {
            type = list.get(opt).toString(parser);
         }
         else if (opt.equals("sort"))
         {
            sort = list.get(opt).toString(parser);

            if (sort.equals("none") || sort.equals("unsrt"))
            {
               sort = null;
            }
            else if (sort.isEmpty())
            {
               sort = "locale";
            }
         }
         else if (opt.equals("strength"))
         { // collator strength

            String strength = list.get(opt).toString(parser);

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
                 String.format("Invalid strength value: %s", strength));
            }
         }
         else if (opt.equals("decomposition"))
         { // collator decomposition

            String decomposition = list.get(opt).toString(parser);

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
                 String.format("Invalid decomposition value: %s", decomposition));
            }
         }
         else if (opt.equals("charset"))
         {
            bibCharset = Charset.forName(list.get(opt).toString(parser));
         }
         else if (opt.equals("suffixF"))
         {
            suffixF = list.get(opt).toString(parser);
         }
         else if (opt.equals("suffixFF"))
         {
            suffixFF = list.get(opt).toString(parser);
         }
         else if (opt.equals("see"))
         {
            String loc = list.get(opt).toString(parser);

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
                String.format("Invalid 'see' value: %s", loc));
            }
         }
         else
         {
            throw new IllegalArgumentException(
             String.format("Unknown option: %s", opt));
         }
      }

      if (srcList == null)
      {
         sources.add(new TeXPath(parser, filename, "bib"));
      }
   }

   public void parse(TeXParser parser)
   throws IOException
   {
      bib2gls = (Bib2Gls)parser.getListener().getTeXApp();

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

               while ((line = reader.readLine()) != null)
               {
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
                        bib2gls.warning(
                         String.format("Ignoring unknown encoding: %s", 
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

         for (BibData data : list)
         {
            if (data instanceof Bib2GlsEntry)
            {
               Bib2GlsEntry entry = (Bib2GlsEntry)data;

               if (type != null)
               {
                  entry.putField("type", type);
               }

               // does this entry have any records?

               for (GlsRecord record : records)
               {
                  if (record.getLabel().equals(entry.getId()))
                  {
                     entry.addRecord(record);
                  }
               }

               // does this entry have a "see" field?

               entry.initCrossRefs(parser);

               bibData.add(entry);

               for (Iterator<String> it = entry.getDependencyIterator();
                    it.hasNext(); )
               {
                  String dep = it.next();

                  bib2gls.addDependent(dep);
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
         bib2gls.info("Adding parent: "+parentId);
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

   public void processData()
      throws IOException
   {
      if (bibData == null)
      {
         throw new NullPointerException(
            "No data (parse must come before processData)");
      }

      Vector<Bib2GlsEntry> entries = new Vector<Bib2GlsEntry>();

      Vector<String> fields = bib2gls.getFields();
      Vector<GlsRecord> records = bib2gls.getRecords();

      if (sort == null)
      {
         // add all entries that have been recorded in the order of
         // definition

         for (Bib2GlsEntry entry : bibData)
         {
            if (entry.hasRecords())
            {
               addHierarchy(entry, entries);
               entries.add(entry);
            }
         }
      }
      else
      {
         // add all recorded entries in order of records.

         for (int i = 0; i < records.size(); i++)
         {
            GlsRecord record = records.get(i);

            Bib2GlsEntry entry = getEntry(record.getLabel());

            if (entry != null && !entries.contains(entry))
            {
               addHierarchy(entry, entries);
               entries.add(entry);
            }
         }
      }

      // add any dependencies

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

      // sort if required

      if (sort != null && !sort.equals("use"))
      {
         entries.sort(new Bib2GlsEntryComparator(sort, sortField, 
            collatorStrength, collatorDecomposition));
      }

      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(texFile, bib2gls.getTeXCharset().name());

         writer.println("\\providecommand{\\bibglsseesep}{, }");

         // syntax: {label}{opts}{name}{description}

         writer.println("\\providecommand{\\bibglsnewentry}[4]{%");
         writer.print(" \\longnewglossaryentry{#1}");
         writer.println("{name={#3},#2}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewsymbol}[4]{%");
         writer.print(" \\longnewglossaryentry{#1}");
         writer.println("{name={#3},sort={#1},category={symbol},#2}{#4}%");
         writer.println("}");

         writer.println("\\providecommand{\\bibglsnewnumber}[4]{%");
         writer.print(" \\longnewglossaryentry{#1}");
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
            entry.updateLocationList(minLocationRange,
              suffixF, suffixFF, seeLocation);
            entry.writeBibEntry(writer);

            writer.println();
         }
      }
      finally
      {
         if (writer != null)
         {
            writer.close();
         }
      }
   }

   private File texFile;

   private Vector<TeXPath> sources;

   private String type = null, sort = null, sortField = "sort";

   private Charset bibCharset = null;

   private int minLocationRange = 3;

   private String suffixF, suffixFF;

   private String preamble = null;

   private Vector<Bib2GlsEntry> bibData;

   private Bib2Gls bib2gls;

   private int collatorStrength=Collator.PRIMARY;

   private int collatorDecomposition=Collator.NO_DECOMPOSITION;

   private int seeLocation=Bib2GlsEntry.POST_SEE;
}

