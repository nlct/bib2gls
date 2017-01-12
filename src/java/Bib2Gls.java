package com.dickimawbooks.bib2gls;

import java.util.Vector;
import java.util.HashMap;
import java.io.*;
import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.aux.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2Gls implements TeXApp
{
   public Bib2Gls(int debug, int verbose) 
   {
      debugLevel = debug;
      verboseLevel = verbose;

      if (verboseLevel >= 0)
      {
         version();
      }
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

      if (debugLevel > 0)
      {
         System.err.format("Unknown TeX charset: %s%n", texCharset);
      }

      return texCharset;
   }

   public void process(File auxFile) throws IOException,Bib2GlsException
   {
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
         throw new Bib2GlsException("Missing \\glsxtr@resource in aux file.");
      }

      if (records.size() == 0)
      {
         throw new Bib2GlsException("Missing \\glsxtr@record in aux file.");
      }

      if (fields.size() == 0)
      {
         warning(parser, "Missing fields in aux file (make sure glossaries-extra.sty version is at least 1.11)");

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
         resource.process(parser);
      }
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

   public void info(String message)
   {
      if (verboseLevel > 0)
      {
         System.out.println(message);
      }
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
      {
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
      {
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
      if (verboseLevel > 0)
      {
         System.out.println(original+" -> "+replacement);
      }
   }

   /*
    *  TeXApp method. TODO: label needs converting to message
    *  string.
    */ 
   public String getMessage(String label)
   {
      return label;
   }

   /*
    *  TeXApp method. TODO: label needs converting to message string
    *  with given parameter.
    */ 
   public String getMessage(String label, String param)
   {
      return String.format("%s[%s]", label, param);
   }

   /*
    *  TeXApp method. TODO: label needs converting to message string
    *  with given parameters.
    */ 
   public String getMessage(String label, String[] params)
   {
      String msg = label;

      String pre = "[";

      for (int i = 0; i < params.length; i++)
      {
         msg += pre + params[i];
         pre = ",";
      }

      msg += "]";

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
   }

   /*
    *  TeXApp method.
    */ 
   public void warning(TeXParser parser, String message)
   {
      if (verboseLevel >= 0)
      {
         System.err.println(message);
      }
   }

   public void warning(String message, Exception e)
   {
      if (verboseLevel >= 0)
      {
         System.err.println(message);
      }

      if (debugLevel > 0)
      {
         e.printStackTrace();
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
      }

      if (debugLevel > 0)
      {
         e.printStackTrace();
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

   public static void version()
   {
      System.out.format("%s version %s %s%n", NAME, VERSION, DATE);
   }

   public static void copyright()
   {
      System.out.println("Copyright 2017 Nicola Talbot");
   }

   public static void help()
   {
      System.out.format("Usage: %s [options] <aux file>%n%n", NAME);

      System.out.println("Helper application for the glossaries-extra package.");
      System.out.println("See the manual for further details.");
      System.out.println();

      System.out.println("Options:");
      System.out.println("--help or -h\tDisplay this help message and exit.");
      System.out.println("--version or -v\tDisplay version and exit.");
      System.out.println("--debug [<n>]\tSet the debug mode.");
      System.out.println("--verbose\tSwitch on verbose mode.");
      System.out.println("--noverbose\tSwitch off verbose mode (default).");
      System.out.println("--silent\tOnly display error messages.");

      System.exit(0);
   }

   public static void main(String[] args)
   {
      String auxFileName = null;
      int debug = 0;
      int verbose = 0;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--debug"))
         {
            if (i == args.length-1 || args[i+1].startsWith("-"))
            {
               debug = 1;
               continue;
            }

            try
            {
               debug = Integer.parseInt(args[i+1]);
               i++;
            }
            catch (NumberFormatException e)
            {
               // argument missing
               debug = 1;
            }
         }
         else if (args[i].equals("--verbose"))
         {
            verbose = 1;
         }
         else if (args[i].equals("--noverbose"))
         {
            verbose = 0;
         }
         else if (args[i].equals("--silent"))
         {
            verbose = -1;
         }
         else if (args[i].equals("-h") || args[i].equals("--help"))
         {
            help();
         }
         else if (args[i].equals("-v") || args[i].equals("--version"))
         {
            version();
            copyright();
            System.exit(0);
         }
         else if (args[i].startsWith("-"))
         {
            System.err.format("Unknown option: %s%n", args[i]);
            System.exit(1);
         }
         else if (auxFileName == null)
         {
            auxFileName = args[i];
         }
         else
         {
            System.err.println("Only one aux file permitted.");
            System.exit(1);
         }
      }

      if (auxFileName == null)
      {
         System.err.println("Missing aux file. Use --help for help.");
         System.exit(1);
      }

      if (!auxFileName.endsWith(".aux"))
      {
         auxFileName = auxFileName+".aux";
      }

      File auxFile = new File(auxFileName);

      if (!auxFile.exists())
      {
         System.err.format("No such file: %s", auxFileName);
         System.exit(0);
      }

      Bib2Gls bib2gls = new Bib2Gls(debug, verbose);

      try
      {
         bib2gls.process(auxFile);
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

   private Charset texCharset;
}
