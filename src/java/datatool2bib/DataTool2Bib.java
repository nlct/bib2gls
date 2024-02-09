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
import com.dickimawbooks.texparserlib.latex.datatool.*;

import com.dickimawbooks.bibgls.common.*;

public class DataTool2Bib extends BibGlsConverter
{
   @Override
   protected void addPredefinedCommands(TeXParser parser)
   {
      super.addPredefinedCommands(parser);

      parser.putControlSequence(new DTLgidxSetDefaultDB());
      parser.putControlSequence(new NewGidx(this));
      parser.putControlSequence(new NewTerm(this));

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
   }

   @Override
   public void process() throws IOException,Bib2GlsException
   {
      datatoolSty = (DataToolSty)listener.requirepackage("datatool", null);

      if (readOpts == null)
      {
         parser.parse(texFile, charset);
      }
      else
      {
         TeXPath texPath = new TeXPath(parser, texFile);

         TeXReader reader = new TeXReader(String.format("\\DTLread[%s]{%s}",
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

            File file = new File(parentFile, base+"-"+dbName+".bib");

            if (!overwriteFiles && file.exists())
            {
               throw new IOException(getMessage("error.file_exists.nooverwrite",
                  file, "--overwrite"));
            }

            message(getMessage("message.writing", file));

            try
            {
               if (bibCharsetName == null)
               {
                  out = new PrintWriter(file);
               }
               else
               {
                  out = new PrintWriter(file, bibCharsetName);

                  out.println("% Encoding: "+bibCharsetName);
               }

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
                  if (bibCharsetName == null)
                  {
                     out = new PrintWriter(file);
                  }
                  else
                  {
                     out = new PrintWriter(file, bibCharsetName);

                     out.println("% Encoding: "+bibCharsetName);
                  }

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

            if (bibCharsetName == null)
            {
               out = new PrintWriter(bibFile);
            }
            else
            {
               out = new PrintWriter(bibFile, bibCharsetName);

               out.println("% Encoding: "+bibCharsetName);
            }

            while (nameEnum != null && nameEnum.hasMoreElements())
            {
               String dbName = nameEnum.nextElement();

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

      for (DataToolHeader header : headers)
      {
         int colIdx = header.getColumnIndex();
         String label = header.getColumnLabel();

         if (!autoLabel && label.equals(labelColumn))
         {
            labelColIdx = colIdx;
         }

         String field = getFieldName(label);

         if (field != null)
         {
            idxFieldMap.put(Integer.valueOf(colIdx), field);

            if (isIndexConversionOn() && field.equals("description"))
            {
               descFieldIdx = colIdx;
            }
         }
      }

      if (!autoLabel && labelColIdx < 1)
      {
         throw new Bib2GlsException(
           getMessage("datatool2bib.missing.label.column",
            db.getName(), labelColumn));
      }

      for (DataToolEntryRow row : data)
      {
         out.println();

         if (isIndexConversionOn()
              && (descFieldIdx == 0 || row.getEntry(descFieldIdx) == null))
         {
            out.print("@index{");
         }
         else
         {
            out.print("@entry{");
         }

         DataToolEntry entry;
         String rowLabel = "";

         if (autoLabel)
         {
            rowLabel = labelPrefix + (++autoLabelIdx);
         }
         else
         {
            entry = row.getEntry(labelColIdx);

            if (entry != null)
            {
               rowLabel = processLabel(entry);
            }

            if (rowLabel.isEmpty())
            {
               rowLabel = labelPrefix + (++autoLabelIdx);
            }
         }

         out.print(rowLabel);

         for (Integer idx : idxFieldMap.keySet())
         {
            entry = row.getEntry(idx);

            if (entry != null)
            {
               TeXObject content = entry.getContents();
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
      out.println("@preamble{");
      out.println("\\providecommand{\\DTLgidxName}[2]{#2, #1}");
      out.println("\\providecommand{\\DTLgidxOffice}[2]{#2, #1}");
      out.println("\\providecommand{\\DTLgidxPlace}[2]{#2, #1}");
      out.println("\\providecommand{\\DTLgidxSubject}[2]{#2, #1}");
      out.println("\\providecommand{\\DTLgidxRank}[2]{#2, #1}");
      out.println("\\providecommand{\\DTLgidxParticle}[2]{#2, #1}");
      out.println("\\providecommand{\\DTLgidxParen}[1]{ (#1)}");
      out.println("\\providecommand{\\DTLgidxSaint}[1]{Saint}");
      out.println("\\providecommand{\\DTLgidxMac}[1]{Mac}");
      out.println("\\providecommand{\\DTLgidxNameNum}[1]{\\csuse{two@digits}{#1}}");
      out.println("}");
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

      if (keyToFieldMap != null)
      {
         for (String field : keyToFieldMap.keySet())
         {
            String map = keyToFieldMap.get(field);

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
         }
      }

      HashMap<String,String> labelMap = new HashMap<String,String>();

      for (GidxData data : datalist)
      {
         String label = processLabel(data.getLabel());

         if (label.isEmpty())
         {
            label = labelPrefix + (++autoLabelIdx);
         }

         if (!label.equals(data.getLabel()))
         {
            labelMap.put(data.getLabel(), label);
         }

         KeyValList fields = data.getFields();

         out.println();

         String entrytype = "entry";

         if (fields.get(orgShortField) != null
           && fields.get(orgLongField) != null)
         {
            entrytype = "abbreviation";
         }
         else if (isIndexConversionOn()
                    && fields.get(orgDescriptionField) == null)
         {
            entrytype = "index";
         }

         out.format("@%s{%s", entrytype, label);
         int fieldCount = 0;

         for (String field : fields.keySet())
         {
            TeXObject value = fields.get(field);

            field = getFieldName(field);

            if (field != null && !field.equals("label") && value != null)
            {
               String valStr = value.toString(parser);

               if (!(field.equals("name") && valStr.equals(label)))
               {
                  fieldCount++;

                  if (field.equals("parent"))
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

                  out.println(",");

                  out.format("  %s = {%s}", field, valStr);
               }
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

      list.add(data);
   }

   @Override
   protected void syntaxInfo()
   {
      printSyntaxItem(getMessage("datatool2bib.syntax.info", "bib2gls"));
   }

   @Override
   protected int argCount(String arg)
   {
      if ( arg.equals("--label") || arg.equals("-L")
        || arg.equals("--read") || arg.equals("-r")
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

         readOpts = returnVals[0].toString();
      }
      else if (arg.equals("--no-read"))
      {
         readOpts = null;
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
   private boolean autoLabel = false;
   private String autoLabelPrefix = null;
   private String readOpts = null;
   private int autoLabelIdx = 0;

   private String dataValueSuffix = null;
   private String dataCurrencySuffix = null;

   private DataToolSty datatoolSty;

   private HashMap<String,Vector<GidxData>> gidxdata;

   public static final String DATAGIDX_DEFAULT_DATABASE
    = "l__datagidx_default_database_tl";
}
