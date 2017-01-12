package com.dickimawbooks.bib2gls;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
      TeXPath texPath = new TeXPath(parser, 
        data.getArg(1).toString(parser), "glstex");

      texFile = texPath.getFile();

      String filename = texPath.getTeXPath(true);

      KeyValList list = KeyValList.getList(parser, data.getArg(0));

      TeXObject srcList = list.get("src");

      sources = new Vector<TeXPath>();

      if (srcList == null)
      {
         sources.add(new TeXPath(parser, filename, "bib"));
      }
      else
      {
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

      TeXObject object = list.get("type");

      if (object != null)
      {
         type = object.toString(parser);
      }

      object = list.get("sort");

      if (object != null)
      {
         sort = object.toString(parser);

         if (sort.equals("none") || sort.equals("unsrt"))
         {
            sort = null;
         }
         else if (sort.isEmpty())
         {
            sort = "locale";
         }
      }

      object = list.get("charset");

      if (object != null)
      {
         bibCharset = Charset.forName(object.toString(parser));
      }

      object = list.get("suffixF");

      if (object != null)
      {
         suffixF = object.toString(parser);
      }

      object = list.get("suffixFF");

      if (object != null)
      {
         suffixFF = object.toString(parser);
      }

   }

   public void process(TeXParser parser)
   throws IOException
   {
      Bib2Gls bib2gls = (Bib2Gls)parser.getListener().getTeXApp();

      Vector<Bib2GlsEntry> entries = new Vector<Bib2GlsEntry>();

      Vector<String> fields = bib2gls.getFields();
      Vector<GlsRecord> records = bib2gls.getRecords();

      for (TeXPath src : sources)
      {
         File bibFile = src.getFile();

         bib2gls.message(bib2gls.getMessage("message.reading", 
          bibFile.toString()));

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

         for (int i = 0; i < records.size(); i++)
         {
            GlsRecord record = records.get(i);

            BibData data = bibParserListener.getBibEntry(record.getLabel());

            if (data != null && data instanceof Bib2GlsEntry)
            {
               Bib2GlsEntry entry = (Bib2GlsEntry)data;

               if (!entries.contains(entry))
               {
                  entries.add(entry);
               }

               entry.addRecord(record);
            }
         }
      }

      // Add dependencies

      for (int i = 0; i < entries.size(); i++)
      {
         Bib2GlsEntry entry = entries.get(i);

         for (Iterator<String> it = entry.getDependencyIterator();
              it.hasNext(); )
         {
            String dep = it.next();

            if (!Bib2GlsEntry.inList(dep, entries))
            {
               Bib2GlsEntry data = bib2gls.getEntry(dep);

               if (data == null)
               {
                  bib2gls.warning(String.format(
                    "%s: undefined dependent entry: %s", entry.getId(), dep));
               }
               else
               {
                  entries.add(data);
               }
            }
         }
      }

      // sort if required

      if (sort != null)
      {
         entries.sort(new Bib2GlsEntryComparator(sort, sortField, records));
      }

      bib2gls.message(bib2gls.getMessage("message.writing", 
       texFile.toString()));

      PrintWriter writer = null;

      try
      {
         writer = new PrintWriter(texFile, bib2gls.getTeXCharset().name());

         for (Bib2GlsEntry entry : entries)
         {
            entry.updateLocationList(minLocationRange,
              suffixF, suffixFF);
            entry.writeBibEntry(writer);
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
}

