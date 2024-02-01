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
import com.dickimawbooks.texparserlib.latex.datatool.*;

import com.dickimawbooks.bibgls.common.*;

public class DataTool2Bib extends BibGlsConverter
{
   @Override
   public void process() throws IOException,Bib2GlsException
   {
      DataToolSty datatoolSty = (DataToolSty)listener.requirepackage("datatool", null);

      parser.parse(texFile, charset);

      int numDatabases = datatoolSty.getDataBaseCount();

      if (numDatabases == 0)
      {
         throw new Bib2GlsException(
            getMessage("datatool2bib.no.databases"));
      }
      else if (numDatabases == 1)
      {
         split = false;
      }

      message(getMessage("datatool2bib.databases.found", numDatabases));

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

         while (nameEnum.hasMoreElements())
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

            while (nameEnum.hasMoreElements())
            {
               String dbName = nameEnum.nextElement();

               writeEntries(datatoolSty.getDataBase(dbName), out);
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

   protected String processLabel(String label)
   {
      if (spaceSub == null)
      {
         return label.replace(" ", "");
      }
      else
      {
         return label.replace(" ", spaceSub);
      }
   }

   protected void writeEntries(DataBase db, PrintWriter out)
     throws IOException
   {
      message(getMessage("datatool2bib.database", db.getName()));

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

      int labelColIdx = -1;

      for (DataToolHeader header : headers)
      {
         int colIdx = header.getColumnIndex();
         String label = header.getColumnLabel();

         if (label.equals(labelColumn))
         {
            labelColIdx = colIdx;
         }

         if (!isCustomIgnoreField(label))
         {
            String map = keyToFieldMap.get(label);

            if (map != null)
            {
               label = map;
            }

            label = processLabel(label);
            idxFieldMap.put(Integer.valueOf(colIdx), label);
         }
      }

// TODO
   }

   @Override
   protected void syntaxInfo()
   {
      printSyntaxItem(getMessage("datatool2bib.syntax.info", "bib2gls"));
   }

   @Override
   protected int argCount(String arg)
   {
      if (arg.equals("--key-map") || arg.equals("-m")
        || arg.equals("--label") || arg.equals("L"))
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
      else if (isListArg(deque, arg, "-m", "--key-map", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         if (keyToFieldMap == null)
         {
            keyToFieldMap = new HashMap<String,String>();
         }

         for (String s : returnVals[0].listValue())
         {
            String[] map = s.split(" *= *");

            if (map.length != 2)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("datatool2bib.syntax.invalid_map",
                  s, arg));
            }

            keyToFieldMap.put(map[0], map[1]);
         }
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

   private HashMap<String,String> keyToFieldMap;

   private boolean split = false;

   private String labelColumn="Label";
}
