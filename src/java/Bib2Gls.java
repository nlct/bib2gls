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

import java.util.Vector;
import java.util.HashMap;
import java.util.Properties;
import java.util.Locale;
import java.io.*;
import java.nio.charset.Charset;
import java.net.URL;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.aux.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2Gls implements TeXApp
{
   public Bib2Gls() 
   {
   }

   private String texToJavaCharset(String texCharset)
   {
      if (texCharset.equals("ascii"))
      {
         return "US-ASCII";
      }
      else if (texCharset.equals("latin1"))
      {
         return "ISO-8859-1";
      }
      else if (texCharset.equals("latin2"))
      {
         return "ISO-8859-2";
      }
      else if (texCharset.equals("latin3"))
      {
         return "ISO-8859-3";
      }
      else if (texCharset.equals("latin4"))
      {
         return "ISO-8859-4";
      }
      else if (texCharset.equals("latin5"))
      {
         return "ISO-8859-5";
      }
      else if (texCharset.equals("latin9"))
      {
         return "ISO-8859-9";
      }
      else if (texCharset.equals("latin10"))
      {
         return "ISO-8859-10";
      }
      else if (texCharset.equals("decmulti"))
      {
         return "DEC-MCS";
      }
      else if (texCharset.equals("cp850"))
      {
         return "Cp850";
      }
      else if (texCharset.equals("cp852"))
      {
         return "Cp852";
      }
      else if (texCharset.equals("cp858"))
      {
         return "Cp858";
      }
      else if (texCharset.equals("cp437") || texCharset.equals("cp437de")) 
      {
         return "Cp437";
      }
      else if (texCharset.equals("865"))
      {
         return "Cp865";
      }
      else if (texCharset.equals("applemac"))
      {
         return "MacRoman";
      }
      else if (texCharset.equals("macce"))
      {
         return "MacCentralEurope";
      }
      else if (texCharset.equals("next"))
      {
         // don't known appropriate Java encoding label for this one
      }
      else if (texCharset.equals("cp1250"))
      {
         return "Cp1250";
      }
      else if (texCharset.equals("cp1252") || texCharset.equals("ansinew"))
      {
         return "Cp1252";
      }
      else if (texCharset.equals("cp1257"))
      {
         return "Cp1257";
      }
      else if (texCharset.equals("utf8"))
      {
         return "UTF-8";
      }

      debug(getMessage("error.unknown.tex.charset", texCharset));

      return texCharset;
   }

   public void process(String[] args) throws IOException,Bib2GlsException
   {
      initMessages();

      parseArgs(args);

      if (verboseLevel >= 0)
      {
         version();
      }

      AuxParser auxParser = new AuxParser(this)
      {
         protected void addPredefined()
         {
            super.addPredefined();

            addAuxCommand("glsxtr@resource", 2);
            addAuxCommand("glsxtr@fields", 1);
            addAuxCommand("glsxtr@record", 5);
            addAuxCommand("glsxtr@texencoding", 1);
         }
      };

      TeXParser parser = auxParser.parseAuxFile(auxFile);

      glsresources = new Vector<GlsResource>();
      fields = new Vector<String>();
      fieldMap = new HashMap<String,String>();
      records = new Vector<GlsRecord>();
      dependencies = new Vector<String>();

      texCharset = Charset.defaultCharset();

      for (AuxData data : auxParser.getAuxData())
      {
         String name = data.getName();
 
         if (name.equals("glsxtr@resource"))
         {
            glsresources.add(new GlsResource(parser, data));
         }
         else if (name.equals("glsxtr@texencoding"))
         {
             try
             {
                texCharset = Charset.forName(
                   texToJavaCharset(data.getArg(0).toString(parser)));
             }
             catch (Exception e)
             {
                error(e);
                texCharset = Charset.defaultCharset();
             }
         }
         else if (name.equals("glsxtr@fields"))
         {
            CsvList csvList = CsvList.getList(parser, data.getArg(0));

            for (TeXObject object : csvList)
            {
               TeXObjectList objectList = (TeXObjectList)object;
               TeXObject arg = objectList.popArg(parser);
               String field = arg.toString(parser);

               fields.add(field);

               arg = objectList.popArg(parser);

               String map = arg.toString(parser);

               if (!map.equals(field))
               {
                  fieldMap.put(field, map);
               }
            }
         }
         else if (name.equals("glsxtr@record"))
         {
            GlsRecord record = new GlsRecord(data.getArg(0).toString(parser),
                        data.getArg(1).toString(parser),
                        data.getArg(2).toString(parser),
                        data.getArg(3).toString(parser),
                        data.getArg(4).toString(parser));

            if (!records.contains(record))
            {// skip duplicates
               records.add(record);
            }
         }
      }

      if (glsresources.size() == 0)
      {
         throw new Bib2GlsException(getMessage(
           "error.missing.aux.cs.require_cs",
           "glsxtr@resource", "glsxtrresourcefile"));
      }

      if (records.size() == 0)
      {
         throw new Bib2GlsException(getMessage("error.missing.records"));
      }

      if (fields.size() == 0)
      {
         warning(parser, 
           getMessage("error.missing.aux.new.cs", "glsxtr@fields", "1.11"));

         fields.add("name");
         fields.add("sort");
         fieldMap.put("sort", "sortvalue");
         fields.add("type");
         fields.add("first");
         fields.add("firstplural");
         fieldMap.put("firstplural", "firstpl");
         fields.add("text");
         fields.add("plural");
         fields.add("description");
         fieldMap.put("description", "desc");
         fields.add("descriptionplural");
         fieldMap.put("description", "descplural");
         fields.add("symbol");
         fields.add("symbolplural");
         fields.add("user1");
         fieldMap.put("user1", "useri");
         fields.add("user2");
         fieldMap.put("user2", "userii");
         fields.add("user3");
         fieldMap.put("user3", "useriii");
         fields.add("user4");
         fieldMap.put("user4", "useriv");
         fields.add("user5");
         fieldMap.put("user5", "userv");
         fields.add("user6");
         fieldMap.put("user6", "uservi");
         fields.add("long");
         fields.add("longplural");
         fieldMap.put("long", "longpl");
         fields.add("short");
         fields.add("shortplural");
         fieldMap.put("short", "shortpl");
         fields.add("counter");
         fields.add("parent");
         fields.add("loclist");
         fields.add("see");
         fields.add("category");
      }

      for (GlsResource resource : glsresources)
      {
         // parse all the bib files
         resource.parse(parser);
      }

      // the data needs processing in a separate loop in case of
      // unrecorded cross-references across different bib files.

      for (GlsResource resource : glsresources)
      {
         resource.processData();
      }

      if (logWriter != null)
      {
         try
         {
            logWriter.close();
         }
         finally
         {
            logWriter = null;
         }
      }
   }

   public void addDependent(String id)
   {
      if (!dependencies.contains(id))
      {
         verbose(getMessage("message.added.dep", id));
         dependencies.add(id);
      }
   }

   public Vector<String> getDependencies()
   {
      return dependencies;
   }

   public Charset getTeXCharset()
   {
      return texCharset;
   }

   public Vector<String> getFields()
   {
      return fields;
   }

   public HashMap<String,String> getFieldMap()
   {
      return fieldMap;
   }

   public Vector<GlsRecord> getRecords()
   {
      return records;
   }

   public boolean hasRecord(String id)
   {
      for (GlsRecord record : records)
      {
         if (id.equals(record.getLabel()))
         {
            return true;
         }
      }

      return false;
   }

   public void logMessage(String message)
   {
      if (logWriter != null)
      {
         logWriter.println(message);
      }
   }

   public void debug(String message)
   {
      if (debugLevel > 0)
      {
         System.out.println(message);
      }

      logMessage(message);
   }

   /*
    *  TeXApp method. TODO: needs implementing to allow bib file to
    *  be found on TeX's path.
    */ 
   public String kpsewhich(String arg)
     throws IOException,InterruptedException
   {
      return null;
   }

   /*
    *  TeXApp method needs defining, but not needed for
    *  the purposes of this application.
    */ 
   public void epstopdf(File epsFile, File pdfFile)
     throws IOException,InterruptedException
   {
      if (debugLevel > 0)
      {// shouldn't happen
         System.err.format(
           "Ignoring unexpected request to convert %s to %s%n",
           epsFile.toString(), pdfFile.toString());
      }
   }

   /*
    *  TeXApp method needs defining, but not needed for
    *  the purposes of this application.
    */ 
   public void wmftoeps(File wmfFile, File epsFile)
     throws IOException,InterruptedException
   {
      if (debugLevel > 0)
      {// shouldn't happen
         System.err.format(
           "Ignoring unexpected request to convert %s to %s%n",
           wmfFile.toString(), epsFile.toString());
      }
   }

   /*
    *  TeXApp method.
    */ 
   public void substituting(TeXParser parser, String original, 
     String replacement)
   {
      verbose(getMessage("warning.substituting", original, replacement));
   }

   /*
    *  TeXApp method. 
    */ 
   public String getMessage(String label)
   {
      String msg = label;

      try
      {
         msg = messages.getMessage(label);
      }
      catch (IllegalArgumentException e)
      {
         warning("Can't find message for label: "+label, e);
      }

      return msg;
   }

   /*
    *  TeXApp method.
    */ 
   public String getMessage(String label, String param)
   {
      String msg = label;

      try
      {
         msg = messages.getMessage(label, param);
      }
      catch (IllegalArgumentException e)
      {
         warning("Can't find message for label: "+label, e);
      }

      return msg;
   }

   /*
    *  TeXApp method.
    */ 
   public String getMessage(String label, String[] params)
   {
      String msg = label;

      try
      {
         msg = messages.getMessage(label, (Object[])params);
      }
      catch (IllegalArgumentException e)
      {
         warning("Can't find message for label: "+label, e);
      }

      return msg;
   }

   public String getMessage(String label, Object... params)
   {
      String msg = label;

      try
      {
         msg = messages.getMessage(label, params);
      }
      catch (IllegalArgumentException e)
      {
         warning("Can't find message for label: "+label, e);
      }

      return msg;
   }

   /*
    *  TeXApp method.
    */ 
   public void message(String text)
   {
      if (verboseLevel >= 0)
      {
         System.out.println(text);
      }

      logMessage(text);
   }

   public void verbose(String text)
   {
      if (verboseLevel > 0)
      {
         System.out.println(text);
      }

      logMessage(text);
   }

   public static String fileLineMessage(File file, int lineNum,
     String message)
   {
      return String.format("%s:%d: %s", file.toString(), lineNum,
         message);
   }

   /*
    *  TeXApp method.
    */ 
   public void warning(TeXParser parser, String message)
   {
      if (verboseLevel >= 0)
      {
         int lineNum = parser.getLineNumber();
         File file = parser.getCurrentFile();

         if (lineNum == -1 || file == null)
         {
            warning(message);
         }
         else
         {
            warning(file, lineNum, message);
         }
      }
   }

   public void warning(File file, int line, String message)
   {
      if (verboseLevel >= 0)
      {
         warning(fileLineMessage(file, line, message));
      }
   }

   public void warning(File file, int line, String message, Exception e)
   {
      if (verboseLevel >= 0)
      {
         warning(fileLineMessage(file, line, message), e);
      }
   }

   public void warning(String message)
   {
      if (verboseLevel >= 0)
      {
         System.err.println(message);
      }

      logMessage(message);
   }

   public void warning(String message, Exception e)
   {
      if (verboseLevel >= 0)
      {
         System.err.println(message);
      }

      logMessage(message);

      if (debugLevel > 0)
      {
         e.printStackTrace();

         for (StackTraceElement elem : e.getStackTrace())
         {
            logMessage(elem.toString());
         }
      }
   }

   /*
    *  TeXApp method.
    */ 
   public void error(Exception e)
   {
      if (e instanceof TeXSyntaxException)
      {
         System.err.println(((TeXSyntaxException)e).getMessage(this));
      }
      else
      {
         String msg = e.getMessage();

         if (msg == null)
         {
            msg = e.getClass().getSimpleName();
         }

         System.err.println(msg);
         logMessage(msg);
      }

      if (debugLevel > 0)
      {
         e.printStackTrace();

         for (StackTraceElement elem : e.getStackTrace())
         {
            logMessage(elem.toString());
         }
      }
   }

   /*
    *  TeXApp method needs defining, but unlikely to be needed for
    *  the purposes of this application.
    */ 
   public void copyFile(File orgFile, File newFile)
     throws IOException,InterruptedException
   {
      if (debugLevel > 0)
      {
         System.err.format(
           "Ignoring unexpected request to copy files %s -> %s%n",
           orgFile.toString(), newFile.toString());
      }
   }

   /*
    *  TeXApp method needs defining, but unlikely to be needed for
    *  the purposes of this application. Just return empty string.
    */ 
   public String requestUserInput(String message)
     throws IOException
   {
      if (debugLevel > 0)
      {
         System.err.format(
           "Ignoring unexpected request for user input. Message: %s%n",
           message);
      }

      return "";
   }

   public void version()
   {
      System.out.println(getMessage("about.version", NAME, VERSION, DATE));
   }

   public void license()
   {
      System.out.println("Copyright 2017 Nicola Talbot");
      System.out.println(getMessage("about.license"));
   }

   public void help()
   {
      System.out.println(getMessage("syntax.usage", NAME));
      System.out.println();

      System.out.println(getMessage("syntax.info"));
      System.out.println();

      System.out.println(getMessage("syntax.options"));
      System.out.println();
      System.out.println(getMessage("syntax.help", "--help", "-h"));
      System.out.println(getMessage("syntax.version", "--version", "-v"));
      System.out.println(getMessage("syntax.debug", "--debug"));
      System.out.println(getMessage("syntax.nodebug", "--nodebug"));
      System.out.println(getMessage("syntax.verbose", "--verbose"));
      System.out.println(getMessage("syntax.noverbose", "--noverbose"));
      System.out.println(getMessage("syntax.silent", "--silent"));
      System.out.println(getMessage("syntax.log", "--log-file", "-t"));

      System.exit(0);
   }

   private String getLanguageFileName(String tag)
   {
      return String.format("/resources/bib2gls-%s.xml", tag);
   }

   private void initMessages() throws Bib2GlsException,IOException
   {
      Locale locale = Locale.getDefault();

      String lang = locale.toLanguageTag();

      URL url = getClass().getResource(getLanguageFileName(lang));

      if (url == null)
      {
         lang = locale.getLanguage();

         url = getClass().getResource(getLanguageFileName(lang));

         if (url == null)
         {
            String script = locale.getScript();

            if (script != null)
            {
               url = getClass().getResource(
                getLanguageFileName(String.format("%s-%s", lang, script)));

               if (url == null)
               {
                  url = getClass().getResource(getLanguageFileName("en"));
               }
            }
            else
            {
               url = getClass().getResource(getLanguageFileName("en"));
            }

            if (url == null)
            {
               throw new Bib2GlsException("Can't find language resource file.");
            }
         }
      }

      InputStream in = null;

      try
      {
         in = url.openStream();

         Properties prop = new Properties();

         prop.loadFromXML(in);

         in.close();
         in = null;

         messages = new Bib2GlsMessages(prop);
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }
   }

   private void parseArgs(String[] args)
   {
      String dirName = null;
      String auxFileName = null;
      String logName = null;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--debug"))
         {
            if (i == args.length-1 || args[i+1].startsWith("-"))
            {
               debugLevel = 1;
               continue;
            }

            try
            {
               debugLevel = Integer.parseInt(args[i+1]);
               i++;
            }
            catch (NumberFormatException e)
            {
               // argument missing
               debugLevel = 1;
            }
         }
         else if (args[i].equals("--nodebug"))
         {
            debugLevel = 0;
         }
         else if (args[i].equals("--verbose"))
         {
            verboseLevel = 1;
         }
         else if (args[i].equals("--noverbose"))
         {
            verboseLevel = 0;
         }
         else if (args[i].equals("--silent"))
         {
            verboseLevel = -1;
         }
         else if (args[i].equals("-h") || args[i].equals("--help"))
         {
            help();
         }
         else if (args[i].equals("-v") || args[i].equals("--version"))
         {
            version();
            license();
            System.exit(0);
         }
         else if (args[i].equals("-t") || args[i].equals("--log-file"))
         {
            i++;

            if (i == args.length)
            {
               System.err.println(getMessage("error.missing.value", args[i-1]));
               System.err.println(getMessage("syntax.use.help"));
               System.exit(1);
            }

            logName = args[i];
         }
         else if (args[i].startsWith("-"))
         {
            System.err.println(getMessage(
              "error.syntax.unknown_option", args[i]));
            System.err.println(getMessage("syntax.use.help"));
            System.exit(1);
         }
         else if (auxFileName == null)
         {
            auxFileName = args[i];
         }
         else
         {
            System.err.println(getMessage("error.only.one.aux"));
            System.exit(1);
         }
      }

      if (auxFileName == null)
      {
         System.err.println(getMessage("error.no.aux"));
         System.exit(1);
      }

      if (!auxFileName.endsWith(".aux"))
      {
         auxFileName = auxFileName+".aux";
      }

      File dir = null;

      if (dirName != null)
      {
         dir = new File(dirName);
         auxFile = new File(dir, auxFileName);
      }
      else
      {
         auxFile = new File(auxFileName);
         dir = auxFile.getParentFile();
      }

      if (!auxFile.exists())
      {
         System.err.println(getMessage("error.file.not.found", auxFileName));
         System.exit(0);
      }

      File logFile = null;

      if (logName == null)
      {
         String base = auxFile.getName();

         logFile = new File(dir,
            base.substring(0,base.lastIndexOf("."))+".glg");
      }
      else
      {
         logFile = new File(logName);
      }

      try
      {
         logWriter = new PrintWriter(new FileWriter(logFile));

         logMessage(getMessage("about.version", NAME, VERSION, DATE));
      }
      catch (IOException e)
      {
         logWriter = null;
         System.err.println(getMessage("error.cant.open.log", 
            logFile.toString()));
         error(e);
      }
   }

   public static void main(String[] args)
   {
      Bib2Gls bib2gls = new Bib2Gls();

      try
      {
         bib2gls.process(args);
      }
      catch (Exception e)
      {
         bib2gls.error(e);
      }
   }

   public static final String NAME = "bib2gls";
   public static final String VERSION = "1.0";
   public static final String DATE = "2017-01-10";
   public int debugLevel = 0;
   public int verboseLevel = 0;

   private Vector<GlsResource> glsresources;
   private Vector<String> fields;
   private Vector<GlsRecord> records;

   private HashMap<String,String> fieldMap;
   private Vector<String> dependencies;

   private Charset texCharset;

   private Bib2GlsMessages messages;

   private File auxFile;

   private PrintWriter logWriter=null;
}
