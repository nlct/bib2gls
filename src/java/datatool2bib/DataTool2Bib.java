/*
    Copyright (C) 2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.datatool2bib;

/**
 * Converts .tex files containing datatool.sty database commands to a
 * .bib file suitable for use with bib2gls. Similar in principle to
 * convertgls2bib but for converting datatool databases.
 */

import java.util.Vector;
import java.util.HashMap;
import java.util.Properties;
import java.util.Locale;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.ArrayDeque;
import java.text.MessageFormat;
import java.text.BreakIterator;
import java.io.*;

import java.net.URL;

import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.LaTeXGenericCommand;
import com.dickimawbooks.texparserlib.latex.AtGobble;
import com.dickimawbooks.texparserlib.latex.GobbleOpt;
import com.dickimawbooks.texparserlib.latex.AtFirstOfOne;
import com.dickimawbooks.texparserlib.latex.datatool.*;

import com.dickimawbooks.bibgls.common.*;

public class DataTool2Bib extends BibGlsConverter
{
   protected void addCustomCommands()
   {
      parser.putControlSequence(new GobbleOpt("DTLwrite", 1, 1));

      parser.putControlSequence(new DTLgidxSetDefaultDB());
      parser.putControlSequence(new NewGidx(this));
      parser.putControlSequence(new NewTerm(this));
      parser.putControlSequence(new NewAcro(this));

      parser.putControlSequence(new GenericCommand(true,
       "datagidxwordifygreek", null, TeXParserUtils.createStack(listener,
       new TeXCsRef("def"), new TeXCsRef("alpha"),
          listener.createGroup("alpha"),
       new TeXCsRef("def"), new TeXCsRef("beta"),
          listener.createGroup("beta"),
       new TeXCsRef("def"), new TeXCsRef("gamma"),
          listener.createGroup("gamma"),
       new TeXCsRef("def"), new TeXCsRef("delta"),
          listener.createGroup("delta"),
       new TeXCsRef("def"), new TeXCsRef("epsilon"),
          listener.createGroup("epsilon"),
       new TeXCsRef("def"), new TeXCsRef("varepsilon"),
          listener.createGroup("epsilon"),
       new TeXCsRef("def"), new TeXCsRef("zeta"),
          listener.createGroup("zeta"),
       new TeXCsRef("def"), new TeXCsRef("eta"),
          listener.createGroup("eta"),
       new TeXCsRef("def"), new TeXCsRef("theta"),
          listener.createGroup("theta"),
       new TeXCsRef("def"), new TeXCsRef("vartheta"),
          listener.createGroup("theta"),
       new TeXCsRef("def"), new TeXCsRef("iota"),
          listener.createGroup("iota"),
       new TeXCsRef("def"), new TeXCsRef("kappa"),
          listener.createGroup("kappa"),
       new TeXCsRef("def"), new TeXCsRef("lambda"),
          listener.createGroup("lambda"),
       new TeXCsRef("def"), new TeXCsRef("mu"),
          listener.createGroup("mu"),
       new TeXCsRef("def"), new TeXCsRef("nu"),
          listener.createGroup("nu"),
       new TeXCsRef("def"), new TeXCsRef("xi"),
          listener.createGroup("xi"),
       new TeXCsRef("def"), new TeXCsRef("pi"),
          listener.createGroup("pi"),
       new TeXCsRef("def"), new TeXCsRef("varpi"),
          listener.createGroup("pi"),
       new TeXCsRef("def"), new TeXCsRef("rho"),
          listener.createGroup("rho"),
       new TeXCsRef("def"), new TeXCsRef("sigma"),
          listener.createGroup("sigma"),
       new TeXCsRef("def"), new TeXCsRef("varsigma"),
          listener.createGroup("sigma"),
       new TeXCsRef("def"), new TeXCsRef("tau"),
          listener.createGroup("tau"),
       new TeXCsRef("def"), new TeXCsRef("upsilon"),
          listener.createGroup("upsilon"),
       new TeXCsRef("def"), new TeXCsRef("phi"),
          listener.createGroup("phi"),
       new TeXCsRef("def"), new TeXCsRef("avarphi"),
          listener.createGroup("phi"),
       new TeXCsRef("def"), new TeXCsRef("chi"),
          listener.createGroup("chi"),
       new TeXCsRef("def"), new TeXCsRef("psi"),
          listener.createGroup("psi"),
       new TeXCsRef("def"), new TeXCsRef("omega"),
          listener.createGroup("omega"),
       new TeXCsRef("def"), new TeXCsRef("Gamma"),
          listener.createGroup("Gamma"),
       new TeXCsRef("def"), new TeXCsRef("Delta"),
          listener.createGroup("Delta"),
       new TeXCsRef("def"), new TeXCsRef("Theta"),
          listener.createGroup("Theta"),
       new TeXCsRef("def"), new TeXCsRef("Lambda"),
          listener.createGroup("Lambda"),
       new TeXCsRef("def"), new TeXCsRef("Xi"),
          listener.createGroup("Xi"),
       new TeXCsRef("def"), new TeXCsRef("Pi"),
          listener.createGroup("Pi"),
       new TeXCsRef("def"), new TeXCsRef("Sigma"),
          listener.createGroup("Sigma"),
       new TeXCsRef("def"), new TeXCsRef("Upsilon"),
          listener.createGroup("Upsilon"),
       new TeXCsRef("def"), new TeXCsRef("Phi"),
          listener.createGroup("Phi"),
       new TeXCsRef("def"), new TeXCsRef("Psi"),
          listener.createGroup("Psi"),
       new TeXCsRef("def"), new TeXCsRef("Omega"),
          listener.createGroup("Omega")
      )));

      parser.putControlSequence(
        new TextualContentCommand("DTLgidxCounter", "page"));
      parser.putControlSequence(
        new AtGobble("DTLgidxAddLocationType"));
      parser.putControlSequence(
        new AtGobble("DTLgidxSetCompositor"));
      parser.putControlSequence(
        new AtGobble("DTLgidxGobble"));
      parser.putControlSequence(
        new AtFirstOfOne("DTLgidxNoFormat"));

      parser.putControlSequence(new AtGobble("glsadd"));
      parser.putControlSequence(new AtGobble("glsaddall"));

      parser.putControlSequence(new AtGobble("glsreset"));
      parser.putControlSequence(new AtGobble("glsunset"));
      parser.putControlSequence(new AtGobble("glsresetall"));
      parser.putControlSequence(new AtGobble("glsunsetall"));

      parser.putControlSequence(new LaTeXGenericCommand(true,
       "newtermaddfield", "omomom", new TeXObjectList(),
       TeXParserUtils.createStack(parser,
        new TeXObjectList(), new TeXObjectList(), new TeXObjectList())));
   }

   @Override
   public boolean isSpecialUsePackage(KeyValList options, String styName, 
     boolean loadParentOptions, TeXObjectList stack)
   throws IOException
   {
      if (styName.equals("datatool") || styName.equals("datatool-base")
          || styName.equals("datagidx"))
      {
         if (options != null)
         {
            for (Iterator<String> it = options.keySet().iterator();
                 it.hasNext(); )
            {
               String key = it.next();

               TeXObject value = options.get(key);

               datatoolSty.processSetupOption(key, value, stack);
            }
         }

         return true;
      }
      else
      {
         return false;
      }
   }

   @Override
   public void process() throws IOException,Bib2GlsException
   {
      datatoolSty = (DataToolSty)listener.requirepackage("datatool", null);
      datagidxSty = (DataGidxSty)listener.requirepackage("datagidx", null);

      addCustomCommands();

      if (setup != null && !setup.isEmpty())
      {
         TeXReader reader = new TeXReader(this, String.format("\\DTLsetup{%s}", setup));

         parser.parse(reader);
      }

      if (readOpts == null)
      {
         parser.parse(texFile, charset);
      }
      else
      {
         TeXPath texPath = new TeXPath(parser, texFile);

         TeXReader reader = new TeXReader(this, String.format("\\DTLread[%s]{%s}",
           readOpts, texPath.getTeXPath(false)));

         parser.parse(reader);
      }

      int numDatabases = datatoolSty.getDataBaseCount();
      int numGidxDataBases = (gidxdata == null ? 0 : gidxdata.size());

      int total = numDatabases + numGidxDataBases;

      if (total == 0)
      {
         throw new Bib2GlsException(
            getMessage("datatool2bib.no.databases"));
      }
      else if (total == 1)
      {
         split = false;
      }

      message(getMessage("datatool2bib.databases.found", numDatabases));
      message(getMessage("datatool2bib.gidxdata.found", numGidxDataBases));

      Enumeration<String> nameEnum = datatoolSty.getDataBaseNames();

      Charset bibCharset;

      if (bibCharsetName == null)
      {
         bibCharset = getDefaultCharset();
         bibCharsetName = bibCharset.name();
      }
      else
      {
         bibCharset = Charset.forName(bibCharsetName);
      }

      PrintWriter out = null;

      if (split)
      {
         String base = bibFile.getName();
         File parentFile = bibFile.getParentFile();

         int idx = base.lastIndexOf(".");

         if (idx > 0)
         {
            base = base.substring(0, idx);
         }

         while (nameEnum != null && nameEnum.hasMoreElements())
         {
            String dbName = nameEnum.nextElement();

            if (skipDataGidx && dbName.equals("datagidx"))
            {
               verboseMessage("datatool2bib.skipping.database", dbName);
               continue;
            }

            File file = new File(parentFile, base+"-"+dbName+".bib");

            if (!overwriteFiles && file.exists())
            {
               throw new IOException(getMessage("error.file_exists.nooverwrite",
                  file, "--overwrite"));
            }

            message(getMessage("message.writing", file));

            try
            {
               out = new PrintWriter(createBufferedWriter(file.toPath(), bibCharset));

               out.println("% Encoding: "+bibCharsetName);

               writeEntries(datatoolSty.getDataBase(dbName), out);
            }
            finally
            {
               if (out != null)
               {
                  out.close();
               }
            }
         }

         if (gidxdata != null)
         {
            for (String dbName : gidxdata.keySet())
            {
               File file = new File(parentFile, base+"-"+dbName+".bib");

               if (!overwriteFiles && file.exists())
               {
                  throw new IOException(getMessage("error.file_exists.nooverwrite",
                     file, "--overwrite"));
               }

               message(getMessage("message.writing", file));

               try
               {
                  out = new PrintWriter(createBufferedWriter(file.toPath(), bibCharset));

                  out.println("% Encoding: "+bibCharsetName);

                  writeGidxPreamble(out);

                  writeEntries(dbName, gidxdata.get(dbName), out);
               }
               finally
               {
                  if (out != null)
                  {
                     out.close();
                  }
               }
            }
         }
      }
      else
      {
         try
         {
            if (!overwriteFiles && bibFile.exists())
            {
               throw new IOException(getMessage("error.file_exists.nooverwrite",
                  bibFile, "--overwrite"));
            }

            message(getMessage("message.writing", bibFile));

            out = new PrintWriter(createBufferedWriter(bibFile.toPath(), bibCharset));

            out.println("% Encoding: "+bibCharsetName);

            while (nameEnum != null && nameEnum.hasMoreElements())
            {
               String dbName = nameEnum.nextElement();

               if (dbName.equals("datagidx"))
               {
                  verboseMessage("datatool2bib.skipping.database", dbName);
                  continue;
               }

               writeEntries(datatoolSty.getDataBase(dbName), out);
            }

            if (gidxdata != null)
            {
               writeGidxPreamble(out);

               for (String dbName : gidxdata.keySet())
               {
                  writeEntries(dbName, gidxdata.get(dbName), out);
               }
            }
         }
         finally
         {
            if (out != null)
            {
               out.close();
            }
         }
      }
   }

   public String processLabel(DataToolEntry entry)
    throws IOException
   {
      TeXObject content = entry.getContents();

      if (isDebuggingOn())
      {
         logAndPrintMessage(getMessage("datatool2bib.processing.entry_label",
          content.toString(parser)));
      }

      String label = parser.expandToString(content, null);

      return processLabel(label);
   }

   protected void writeEntries(DataBase db, PrintWriter out)
     throws IOException,Bib2GlsException
   {
      message(getMessage("datatool2bib.database", db.getName()));

      String labelPrefix;

      if (autoLabelPrefix == null)
      {
         labelPrefix = processLabel(db.getName());
         autoLabelIdx = 0;
      }
      else
      {
         labelPrefix = autoLabelPrefix;
      }

      DataToolHeaderRow headers = db.getHeaders();
      DataToolRows data = db.getData();

      int colCount = headers.size();
      int rowCount = data.size();

      if (colCount == 0 || rowCount == 0)
      {
         message(getMessage("datatool2bib.database.empty", db.getName()));
         return;
      }

      HashMap<Integer,String> idxFieldMap = new HashMap<Integer,String>();

      int labelColIdx = 0;
      int descFieldIdx = 0;
      int fallbackLabelColIdx = 0;

      for (DataToolHeader header : headers)
      {
         int colIdx = header.getColumnIndex();
         String colKey = header.getColumnLabel();

         if (!autoLabel)
         {
            if (colKey.equals(labelColumn))
            {
               labelColIdx = colIdx;
            }
            else if (colKey.equals(fallbackLabelColumn))
            {
               fallbackLabelColIdx = colIdx;
            }
         }

         String field = getFieldName(colKey);

         if (field != null)
         {
            idxFieldMap.put(Integer.valueOf(colIdx), field);

            if (isIndexConversionOn() && field.equalsIgnoreCase("description"))
            {
               descFieldIdx = colIdx;
            }
         }
      }

      if (!autoLabel)
      {
         if (labelColIdx < 1)
         {
            if (fallbackLabelColIdx > 0)
            {
               verboseMessage("datatool2bib.replace.missing.label.column",
                 db.getName(), labelColumn, fallbackLabelColumn);

               labelColIdx = fallbackLabelColIdx;
            }

            if (labelColIdx < 1)
            {
               warning(
                 getMessage("datatool2bib.missing.label.column",
                  db.getName(), labelColumn, "--label", "--auto-label"));

               return;
            }
         }
      }

      for (DataToolEntryRow row : data)
      {
         out.println();

         String entryType = "entry";

         if (isIndexConversionOn()
              && (descFieldIdx == 0 || row.getEntry(descFieldIdx) == null))
         {
            entryType = "index";
         }

         out.format("@%s{", applyFieldCase(entryType));

         DataToolEntry entry;
         String rowLabel = "";

         if (autoLabel)
         {
            rowLabel = labelPrefix + (++autoLabelIdx);
         }
         else
         {
            entry = row.getEntry(labelColIdx);
            String orgLabel = "";

            if (entry != null)
            {
               rowLabel = processLabel(entry);
            }

            if (rowLabel.isEmpty())
            {
               if (entry != null)
               {
                  orgLabel = entry.toString(parser);
               }

               if (fallbackLabelColIdx > 0 && fallbackLabelColIdx != labelColIdx)
               {
                  verboseMessage("datatool2bib.using.fallback", orgLabel,
                    fallbackLabelColumn);

                  entry = row.getEntry(fallbackLabelColIdx);

                  if (entry != null)
                  {
                     rowLabel = processLabel(entry);

                     if (rowLabel.isEmpty())
                     {
                        orgLabel = entry.toString(parser);
                     }
                  }
               }

               if (rowLabel.isEmpty())
               {
                  verboseMessage("datatool2bib.using.autolabel", orgLabel);

                  rowLabel = labelPrefix + (++autoLabelIdx);
               }
            }
         }

         out.print(rowLabel);

         for (Integer idx : idxFieldMap.keySet())
         {
            entry = row.getEntry(idx);

            if (entry != null)
            {
               TeXObject content = processValue(entry.getContents());
               String field = idxFieldMap.get(idx);

               out.println(",");

               String strVal = content.toString(getParser());

               out.format("  %s = {%s}", field, strVal);

               if (dataValueSuffix != null || dataCurrencySuffix != null)
               {
                  DataElement element = datatoolSty.getElement(content);
                  DatumType datumType = element.getDatumType();

                  if (datumType.isNumeric())
                  {
                     DataNumericElement dataNum = (DataNumericElement)content;

                     if (dataValueSuffix != null)
                     {
                        String numVal;

                        if (datumType == DatumType.INTEGER)
                        {
                           numVal = "" + dataNum.intValue();
                        }
                        else
                        {
                           numVal = "" + dataNum.doubleValue();
                        }

                        if (!numVal.equals(strVal))
                        {
                           out.println(",");

                           out.format("  %s%s = {%s}", field,
                             dataValueSuffix, numVal);
                        }
                     }

                     if (datumType == DatumType.CURRENCY
                          && dataCurrencySuffix != null)
                     {
                        TeXObject sym = dataNum.getCurrencySymbol();

                        if (sym != null)
                        {
                           out.println(",");

                           out.format("  %s%s = {%s}", field,
                             dataCurrencySuffix,
                             sym.toString(parser));
                        }
                     }
                  }
               }
            }
         }

         out.println();
         out.println("}");
      }
   }

   protected void writeGidxPreamble(PrintWriter out)
    throws IOException
   {
      out.format("@%s{\"", applyFieldCase("preamble"));
      out.println("\\providecommand{\\IfNotBibGls}[2]{#1}");
      out.println("\\providecommand{\\DTLgidxName}[2]{\\IfNotBibGls{#1 #2}{#2\\datatoolpersoncomma #1}}");
      out.println("\\providecommand{\\DTLgidxOffice}[2]{\\IfNotBibGls{#2 (#1)}{#2\\datatoolpersoncomma #1}}");
      out.println("\\providecommand{\\DTLgidxPlace}[2]{\\IfNotBibGls{#2}{#2\\datatoolplacecomma #1}}");
      out.println("\\providecommand{\\DTLgidxSubject}[2]{\\IfNotBibGls{#2}{#2\\datatoolsubjectcomm #1}}");
      out.println("\\providecommand{\\DTLgidxRank}[2]{\\IfNotBibGls{#1~#2}{#2.}}");
      out.println("\\providecommand{\\DTLgidxParticle}[2]{\\IfNotBibGls{#1~#2}{#2.}}");
      out.println("\\providecommand{\\DTLgidxParen}[1]{\\IfNotBibGls{ (#1)}{\\datatoolparenstart #1}}");
      out.println("\\providecommand{\\DTLgidxIgnore}[1]{\\IfNotBibGls{#1}{}}");
      out.println("\\providecommand{\\DTLgidxSaint}[1]{\\IfNotBibGls{#1}{Saint}}");
      out.println("\\providecommand{\\DTLgidxMac}[1]{\\IfNotBibGls{#1}{Mac}}");
      out.println("\\providecommand{\\DTLgidxNameNum}[1]{\\IfNotBibGls{\\csuse{@Roman}{#1}}{\\csuse{two@digits}{#1}}}");
      out.println("\"}");
   }

   protected void writeEntries(String dbName,
      Vector<GidxData> datalist, PrintWriter out)
     throws IOException,Bib2GlsException
   {
      message(getMessage("datatool2bib.datagidx", dbName));

      String labelPrefix;

      if (autoLabelPrefix == null)
      {
         labelPrefix = processLabel(dbName);
         autoLabelIdx = 0;
      }
      else
      {
         labelPrefix = autoLabelPrefix;
      }

      String orgDescriptionField = "description";
      String orgShortField = "short";
      String orgLongField = "long";
      String orgNameField = "name";
      String orgTextField = "text";
      String orgPluralField = "plural";

      String fallbackField = fallbackLabelColumn.toLowerCase();

      if (keyToFieldMap != null)
      {
         for (String field : keyToFieldMap.keySet())
         {
            String map = keyToFieldMap.get(field).toLowerCase();

            if (map.equals("description"))
            {
               orgDescriptionField = field;
            }
            else if (map.equals("short"))
            {
               orgShortField = field;
            }
            else if (map.equals("long"))
            {
               orgLongField = field;
            }
            else if (map.equals("name"))
            {
               orgNameField = field;
            }
            else if (map.equals("text"))
            {
               orgTextField = field;
            }
            else if (map.equals("plural"))
            {
               orgPluralField = field;
            }
         }
      }

      HashMap<String,String> labelMap = new HashMap<String,String>();


      for (GidxData data : datalist)
      {
         String orgLabel = data.getLabel();
         String label = processLabel(orgLabel);

         KeyValList fields = data.getFields();

         if (label.isEmpty())
         {
            verboseMessage("datatool2bib.using.fallback", orgLabel, fallbackField);

            label = fields.getString(fallbackField, parser, null);

            if (label == null)
            {
               label = fields.getString(fallbackLabelColumn, parser, null);
            }

            if (label != null)
            {
               orgLabel = label;
               label = processLabel(label);
            }

            if (label == null || label.isEmpty())
            {
               verboseMessage("datatool2bib.using.autolabel", orgLabel);
               label = labelPrefix + (++autoLabelIdx);
            }
         }

         if (!label.equals(data.getLabel()))
         {
            labelMap.put(data.getLabel(), label);

            verboseMessage("datatool2bib.label.changed", data.getLabel(), label);
         }

         out.println();

         String entrytype = "entry";

         if (fields.get(orgShortField) != null
           && fields.get(orgLongField) != null)
         {
            entrytype = "abbreviation";

            if (stripAcronymText)
            {
               fields.remove(orgTextField);
               fields.remove(orgPluralField);
            }

            if (stripAcronymName)
            {
               fields.remove(orgNameField);
            }
         }
         else if (isIndexConversionOn()
                    && fields.get(orgDescriptionField) == null)
         {
            entrytype = "index";
         }

         out.format("@%s{%s", applyFieldCase(entrytype), label);
         int fieldCount = 0;

         TeXObject nameVal = fields.get(orgNameField);
         TeXObject textVal = fields.get(orgTextField);

         if (textVal != null && nameVal != null 
              && textVal.equals(nameVal))
         {
            fields.remove(orgTextField);
         }

         for (String field : fields.keySet())
         {
            TeXObject value = fields.get(field);

            if (value == null) continue;

            String bibfield = getFieldName(field);

            /**
             * It's possible to map the "label" field to a valid
             * field name, so only ignore specifically "label"
             * rather than labelColumn.
             */

            if (bibfield != null && !bibfield.equalsIgnoreCase("label"))
            {
               String valStr = value.toString(parser);

               if (bibfield.equalsIgnoreCase("name"))
               {
                  if (entrytype.equals("index") && valStr.equals(label))
                  {
                     continue;
                  }

                  if (entrytype.equals("abbreviation")
                       && valStr.equals(fields.get(orgShortField).toString(parser)))
                  {
                     continue;
                  }
               }

               fieldCount++;

               if (bibfield.equalsIgnoreCase("parent"))
               {
                  String parentLabel = valStr;
                  String mapLabel = labelMap.get(parentLabel);

                  if (mapLabel == null)
                  {
                     valStr = processLabel(parentLabel);

                     if (valStr.isEmpty())
                     {
                        valStr = labelPrefix + (++autoLabelIdx);
                     }
                  }
                  else
                  {
                     valStr = mapLabel;
                  }

                  if (!valStr.equals(parentLabel))
                  {
                     labelMap.put(parentLabel, valStr);
                  }
               }
               else if (bibfield.equalsIgnoreCase("see")
                     || bibfield.equalsIgnoreCase("seealso")
                     || bibfield.equalsIgnoreCase("alias"))
               {
                  String[] xrlist = valStr.split(" *, *");
                  StringBuilder builder = new StringBuilder();

                  for (String xr : xrlist)
                  {
                     String mapLabel = labelMap.get(xr);

                     if (builder.length() > 0)
                     {
                        builder.append(',');
                     }

                     if (mapLabel == null)
                     {
                        String xrLabel = processLabel(xr);

                        if (xrLabel.isEmpty())
                        {
                           xrLabel = labelPrefix + (++autoLabelIdx);
                           labelMap.put(xrLabel, xr);
                        }

                        builder.append(xrLabel);
                     }
                     else
                     {
                        builder.append(mapLabel);
                     }
                  }

                  valStr = builder.toString();
               }

               out.println(",");

               out.format("  %s = {%s}", bibfield, valStr);
            }
         }

         if (fieldCount > 0)
         {
            out.println();
         }

         out.println("}");
      }
   }

   public void addGidxDatabase(String dbname)
   {
      if (gidxdata == null)
      {
         gidxdata = new HashMap<String,Vector<GidxData>>();
      }

      Vector<GidxData> list = gidxdata.get(dbname);

      if (list == null)
      {
         list = new Vector<GidxData>();
         gidxdata.put(dbname, list);
      }

      ControlSequence cs = parser.getControlSequence(DATAGIDX_DEFAULT_DATABASE);

      if (cs == null || cs.isEmpty())
      {
         parser.putControlSequence(true,
           new TextualContentCommand(DATAGIDX_DEFAULT_DATABASE,
           dbname));
      }
   }

   public void addTerm(String dbname, GidxData data)
    throws IOException
   {
      if (gidxdata == null)
      {
         gidxdata = new HashMap<String,Vector<GidxData>>();
      }

      Vector<GidxData> datalist = gidxdata.get(dbname);

      if (datalist == null)
      {
         datalist = new Vector<GidxData>();
         gidxdata.put(dbname, datalist);
      }

      KeyValList fields = data.getFields();

      for (String field : fields.keySet())
      {
         TeXObject value = fields.get(field);

         fields.put(field, processValue(value));
      }

      datalist.add(data);
   }

   protected TeXObject processValue(TeXObject value)
   {
      if (parser.isStack(value)
        && ( stripGlsAdd || stripAcronymFont || stripCaseChange ) )
      {
         TeXObjectList stack = (TeXObjectList)value;

         TeXObjectList list = listener.createStack();

         try
         {
            processTermValue(stack, list);
         }
         catch (TeXSyntaxException e)
         {
            warning(e.getMessage(this));
            list.addAll(stack);
         }
         catch (IOException e)
         {
            warning(e.getMessage());
            list.addAll(stack);
         }

         return list;
      }
      else
      {
         return value;
      }
   }

   protected void processTermValue(TeXObjectList stack, TeXObjectList list)
    throws IOException
   {
      while (!stack.isEmpty())
      {
         TeXObject obj = stack.pop();

         if (stripGlsAdd && TeXParserUtils.isControlSequence(obj, "glsadd"))
         {
            stack.popArg(parser);
         }
         else if (stripAcronymFont
            && TeXParserUtils.isControlSequence(obj, "acronymfont"))
         {
            obj = stack.popArg(parser);
            stack.push(obj, true);
         }
         else if (stripCaseChange
            && TeXParserUtils.isControlSequence(obj,
             "capitalisewords", "xcapitalisewords", "ecapitalisewords",
             "capitalisefmtwords", "xcapitalisefmtwords", "ecapitalisefmtwords",
             "makefirstuc", "xmakefirstuc", "emakefirstuc", "glsmakefirstuc",
             "uppercase", "lowercase", "glsuppercase", "glslowercase",
             "MakeTextUppercase", "MakeTextLowercase",
             "MakeUppercase", "MakeLowercase", "mfirstucMakeUppercase"
           ))
         {
            obj = stack.popArg(parser);
            stack.push(obj, true);
         }
         else if (obj instanceof TeXObjectList)
         {
            TeXObjectList subList = ((TeXObjectList)obj).createList();
            list.add(subList);

            processTermValue((TeXObjectList)obj, subList);
         }
         else
         {
            list.add(obj);
         }
      }
   }

   @Override
   protected void syntaxInfo()
   {
      printSyntaxItem(getMessage("datatool2bib.syntax.info", "datatool2bib"));
   }

   @Override
   protected void ioHelp()
   {
      super.ioHelp();

      printSyntaxItem(getMessage("datatool2bib.syntax.split",
        "--[no-]split"));

   }

   @Override
   protected void filterHelp()
   {
      super.filterHelp();

      printSyntaxItem(getMessage("datatool2bib.syntax.skip-datagidx",
        "--[no-]skip-datagidx"));
   }

   @Override
   protected void adjustHelp()
   {
      super.adjustHelp();

      printSyntaxItem(getMessage("datatool2bib.syntax.label",
        "--label", "-L", "--label "+labelColumn));

      printSyntaxItem(getMessage("datatool2bib.syntax.fallback-label",
        "--fallback-label", "-F", "--fallback-label "+fallbackLabelColumn));

      printSyntaxItem(getMessage("datatool2bib.syntax.auto-label",
        "--[no-]auto-label", "-a"));

      printSyntaxItem(getMessage("datatool2bib.syntax.auto-label-prefix",
        "--auto-label-prefix"));

      printSyntaxItem(getMessage("datatool2bib.syntax.strip-glsadd",
        "--[no-]strip-glsadd"));

      printSyntaxItem(getMessage("datatool2bib.syntax.strip-acronym-font",
        "--[no-]strip-acronym-font"));

      printSyntaxItem(getMessage("datatool2bib.syntax.strip-acronym-text",
        "--[no-]strip-acronym-text"));

      printSyntaxItem(getMessage("datatool2bib.syntax.strip-acronym-name",
        "--[no-]strip-acronym-name"));

      printSyntaxItem(getMessage("datatool2bib.syntax.strip-case-change",
        "--[no-]strip-case-change"));
   }

   @Override
   protected void otherHelp()
   {
      printSyntaxItem(getMessage("datatool2bib.syntax.other"));
      System.out.println();

      printSyntaxItem(getMessage("datatool2bib.syntax.setup",
        "--setup"));

      printSyntaxItem(getMessage("datatool2bib.syntax.read",
        "--[no-]read", "-r"));

      printSyntaxItem(getMessage("datatool2bib.syntax.save-datum",
        "--[no-]save-datum", "--save-value '-value' --save-currency '-currency'"));
      printSyntaxItem(getMessage("datatool2bib.syntax.save-value",
        "--[no-]save-value"));
      printSyntaxItem(getMessage("datatool2bib.syntax.save-currency",
        "--[no-]save-currency"));

   }

   @Override
   protected int argCount(String arg)
   {
      if ( arg.equals("--label") || arg.equals("-L")
        || arg.equals("--fallback-label") || arg.equals("-F")
        || arg.equals("--read") || arg.equals("-r")
        || arg.equals("--setup")
        || arg.equals("--auto-label-prefix")
        || arg.equals("--save-value")
        || arg.equals("--save-currency")
         )
      {
         return 1;
      }

      return super.argCount(arg);
   }

   @Override
   protected boolean parseArg(ArrayDeque<String> deque, String arg,
      BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException
   {
      if (isArg(deque, arg, "-L", "--label", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         labelColumn = returnVals[0].toString();
      }
      else if (isArg(deque, arg, "-F", "--fallback-label", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         fallbackLabelColumn = returnVals[0].toString();
      }
      else if (arg.equals("--auto-label") || arg.equals("-a"))
      {
         autoLabel = true;
      }
      else if (arg.equals("--no-auto-label"))
      {
         autoLabel = false;
         autoLabelPrefix = null;
      }
      else if (isArg(deque, arg, "--auto-label-prefix", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         autoLabel = true;
         autoLabelPrefix = returnVals[0].toString().trim();

         if (autoLabelPrefix.isEmpty())
         {
            autoLabelPrefix = null;
         }
      }
      else if (isArg(deque, arg, "-r", "--read", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         readOpts = returnVals[0].toString().trim();

         if (readOpts.isEmpty())
         {
            readOpts = null;
         }
      }
      else if (arg.equals("--no-read"))
      {
         readOpts = null;
      }
      else if (isArg(deque, arg, "--setup", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         setup = returnVals[0].toString();
      }
      else if (arg.equals("--save-datum"))
      {
         dataValueSuffix = "-value";
         dataCurrencySuffix = "-currency";
      }
      else if (arg.equals("--no-save-datum"))
      {
         dataValueSuffix = null;
         dataCurrencySuffix = null;
      }
      else if (isArg(deque, arg, "--save-value", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         dataValueSuffix = returnVals[0].toString();
      }
      else if (arg.equals("--no-save-value"))
      {
         dataValueSuffix = null;
      }
      else if (isArg(deque, arg, "--save-currency", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         dataCurrencySuffix = returnVals[0].toString();
      }
      else if (arg.equals("--no-save-currency"))
      {
         dataCurrencySuffix = null;
      }
      else if (arg.equals("--split"))
      {
         split = true;
      }
      else if (arg.equals("--no-split"))
      {
         split = false;
      }
      else if (arg.equals("--strip-glsadd"))
      {
         stripGlsAdd = true;
      }
      else if (arg.equals("--no-strip-glsadd"))
      {
         stripGlsAdd = false;
      }
      else if (arg.equals("--strip-acronym-font"))
      {
         stripAcronymFont = true;
      }
      else if (arg.equals("--no-strip-acronym-font"))
      {
         stripAcronymFont = false;
      }
      else if (arg.equals("--strip-acronym-text"))
      {
         stripAcronymText = true;
      }
      else if (arg.equals("--no-strip-acronym-text"))
      {
         stripAcronymText = false;
      }
      else if (arg.equals("--strip-acronym-name"))
      {
         stripAcronymName = true;
      }
      else if (arg.equals("--no-strip-acronym-name"))
      {
         stripAcronymName = false;
      }
      else if (arg.equals("--strip-case-change"))
      {
         stripCaseChange = true;
      }
      else if (arg.equals("--no-strip-case-change"))
      {
         stripCaseChange = false;
      }
      else if (arg.equals("--skip-datagidx"))
      {
         skipDataGidx = true;
      }
      else if (arg.equals("--no-skip-datagidx"))
      {
         skipDataGidx = false;
      }
      else
      {
         return super.parseArg(deque, arg, returnVals);
      }

      return true;
   }

   @Override
   public String getApplicationName()
   {
      return NAME;
   }

   @Override
   public String getCopyrightStartYear()
   {
      return "2024";
   }

   public static void main(String[] args)
   {
      DataTool2Bib datatool2bib = new DataTool2Bib();
      datatool2bib.run(args);
   }

   public static final String NAME = "datatool2bib";

   private boolean split = false;

   private String labelColumn="Label";
   private String fallbackLabelColumn="Name";
   private boolean autoLabel = false;
   private String autoLabelPrefix = null;
   private String readOpts = null;
   private int autoLabelIdx = 0;
   private String setup = null;

   private boolean stripGlsAdd = true;
   private boolean stripAcronymFont = true;
   private boolean stripAcronymText = true;
   private boolean stripAcronymName = true;
   private boolean stripCaseChange = false;
   private boolean skipDataGidx = true;

   private String dataValueSuffix = null;
   private String dataCurrencySuffix = null;

   private DataToolSty datatoolSty;
   private DataGidxSty datagidxSty;

   private HashMap<String,Vector<GidxData>> gidxdata;

   public static final String DATAGIDX_DEFAULT_DATABASE
    = "l__datagidx_default_database_tl";
}
