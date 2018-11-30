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
package com.dickimawbooks.gls2bib;

/**
 * Converts .tex files containing <code>\\newglossaryentry</code> commands to a
 * .bib file suitable for use with bib2gls. This is quite a
 * primitive command line application. It doesn't have the security
 * checks that bib2gls has. This application is essentially designed
 * for one-off conversion from glossaries-extra.sty definitions to 
 * definitions required by bib2gls for users wanting to change their
 * documents from makeindex/xindy to bib2gls.
 */

import java.util.Vector;
import java.util.HashMap;
import java.util.Properties;
import java.util.Locale;
import java.text.MessageFormat;
import java.io.*;

import java.net.URL;

// Requires Java 1.7:
import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.primitives.Relax;
import com.dickimawbooks.texparserlib.primitives.Undefined;
import com.dickimawbooks.texparserlib.latex.LaTeXParserListener;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.NewCommand;

public class Gls2Bib extends LaTeXParserListener
  implements Writeable,TeXApp
{
   public Gls2Bib(String texFilename, String bibFilename, String inCharset,
     String outCharset, String spaceSub, String langTag, boolean ignoreSort, int verbose)
    throws Gls2BibException,IOException
   {
      super(null);

      this.verboseLevel = verbose;
      this.ignoreSortField = ignoreSort;

      initMessages(langTag);

      this.texFile = new File(texFilename);
      this.bibFile = new File(bibFilename);
      this.bibCharsetName = outCharset;
      this.spaceSub = spaceSub;

      if (" ".equals(spaceSub))
      {
         this.spaceSub = null;
      }

      if (inCharset == null)
      {
         charset = Charset.defaultCharset();
      }
      else
      {
         charset = Charset.forName(inCharset);
      }

      if (bibCharsetName == null)
      {
         bibCharsetName = charset.name();
      }

      initKeyToFieldMap();

      setWriteable(this);

      texParser = new TeXParser(this);
   }

   private void initKeyToFieldMap()
   {
      keyToFieldMap = new HashMap<String,String>();
      keyToFieldMap.put("sortvalue", "sort");
      keyToFieldMap.put("firstpl", "firstplural");
      keyToFieldMap.put("desc", "description");
      keyToFieldMap.put("descplural", "descriptionplural");
      keyToFieldMap.put("useri", "user1");
      keyToFieldMap.put("userii", "user2");
      keyToFieldMap.put("useriii", "user3");
      keyToFieldMap.put("useriv", "user4");
      keyToFieldMap.put("userv", "user5");
      keyToFieldMap.put("uservi", "user6");
      keyToFieldMap.put("longpl", "longplural");
      keyToFieldMap.put("shortpl", "shortplural");
   }

   public TeXApp getTeXApp()
   {
      return this;
   }

   public boolean ignoreSort()
   {
      return ignoreSortField;
   }

   public String getSpaceSub()
   {
      return spaceSub;
   }

   public void debug(String message)
   {
      if (verboseLevel >= DEBUG)
      {
         System.out.println(message);
      }
   }

   public void debug(Throwable e)
   {
      if (verboseLevel >= DEBUG)
      {
         e.printStackTrace();
      }
   }

   protected void addPredefined()
   {
      super.addPredefined();

      parser.putControlSequence(new NewGlossaryEntry(this));
      parser.putControlSequence(new NewGlossaryEntry(
        "newentry", this));
      parser.putControlSequence(new NewGlossaryEntry(
       "provideglossaryentry", this, true));
      parser.putControlSequence(new LongNewGlossaryEntry(this));
      parser.putControlSequence(new LongNewGlossaryEntry(
        "longprovideglossaryentry", this, true));
      parser.putControlSequence(new NewAbbreviation(this));
      parser.putControlSequence(new NewAbbreviation(
        "newacronym", "acronym", this));
      parser.putControlSequence(new NewTerm(this));
      parser.putControlSequence(new NewNumber(this));
      parser.putControlSequence(new NewNumber("newnum", this));
      parser.putControlSequence(new NewSymbol(this));
      parser.putControlSequence(new NewSymbol("newsym", this));
      parser.putControlSequence(new NewDualEntry(this));
      parser.putControlSequence(new GlsExpandFields(this));
      parser.putControlSequence(new GlsExpandFields(
       "glsnoexpandfields", false, this));
      parser.putControlSequence(new GlsSetExpandField(this));
      parser.putControlSequence(new GlsSetExpandField(
        "glssetnoexpandfield", false, this));
   }

   // Ignore unknown control sequences
   public ControlSequence createUndefinedCs(String name)
   {
      return new Undefined(name,
       verboseLevel == SILENT ? Undefined.ACTION_IGNORE: Undefined.ACTION_WARN);
   }

   @Override
   public void newcommand(byte overwrite,
     String type, String csName, boolean isShort,
     int numParams, TeXObject defValue, TeXObject definition)
   throws IOException
   {
      if (csName.equals("newdualentry") && 
          overwrite == NewCommand.OVERWRITE_FORBID)
      {
         // allow \newcommand{\newdualentry} to overwrite default
         // definition

         message(getMessage("gls2bib.override.newdualentry"));
         addLaTeXCommand(csName, isShort, numParams, defValue, definition);
         return;
      }

      super.newcommand(overwrite, type, csName, isShort,
        numParams, defValue, definition);
   }

   // No write performed by parser (just gathering information)
   public void write(String text)
     throws IOException
   {
   }

   public void writeln(String text)
     throws IOException
   {
   }

   public void write(char c)
     throws IOException
   {
   }

   public void writeCodePoint(int codePoint)
     throws IOException
   {
   }

   public void overwithdelims(TeXObject firstDelim,
     TeXObject secondDelim, TeXObject before, TeXObject after)
    throws IOException
   {
      debug("Ignoring \\overwithdelims");
   }

   public void abovewithdelims(TeXObject firstDelim,
     TeXObject secondDelim, TeXDimension thickness, TeXObject before,
     TeXObject after)
    throws IOException
   {
      debug("Ignoring \\abovewithdelims");
   }

   public void skipping(Ignoreable ignoreable)
      throws IOException
   {
   }

   public void href(String url, TeXObject text)
      throws IOException
   {
      debug("Ignoring \\href");
   }

   public void subscript(TeXObject arg)
     throws IOException
   {
      debug("Ignoring _");
   }

   public void superscript(TeXObject arg)
     throws IOException
   {
      debug("Ignoring ^");
   }

   public void includegraphics(KeyValList options, String imgName)
     throws IOException
   {
      debug("Ignoring \\includegraphics");
   }

   public boolean isWriteAccessAllowed(File file)
   {
      return file.canWrite();
   }

   public boolean isWriteAccessAllowed(TeXPath path)
   {
      return path.getFile().canWrite();
   }

   public boolean isReadAccessAllowed(File file)
   {
      return file.canRead();
   }

   public boolean isReadAccessAllowed(TeXPath path)
   {
      return path.getFile().canRead();
   }

   /*
    *  TeXApp method. (Ignore.)
    */ 
   
   public void copyFile(File orgFile, File newFile)
     throws IOException,InterruptedException
   {
      if (verboseLevel >= DEBUG)
      {
         System.err.format(
           "Ignoring unexpected request to copy files %s -> %s%n",
           orgFile.toString(), newFile.toString());
      }
   }

   public String requestUserInput(String message)
     throws IOException
   {
      if (verboseLevel >= DEBUG)
      {
         System.err.format(
           "Ignoring unexpected request for user input. Message: %s%n",
           message);
      }

      return "";
   }

   public String kpsewhich(String arg)
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
      if (verboseLevel >= DEBUG)
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
      if (verboseLevel >= DEBUG)
      {// shouldn't happen
         System.err.format(
           "Ignoring unexpected request to convert %s to %s%n",
           wmfFile.toString(), epsFile.toString());
      }
   }

   /*
    *  TeXApp method.
    */ 
   public void progress(int percent)
   {
   }

   /*
    *  TeXApp method.
    */ 
   public void substituting(TeXParser parser, String original, 
     String replacement)
   {
      debug(getMessage("warning.substituting", original, replacement));
   }

   public void substituting(String original, String replacement)
   {
      debug(getMessage("warning.substituting", original, replacement));
   }

   /*
    *  TeXApp method. 
    */ 
   public String getMessage(String label)
   {
      if (messages == null) return label;

      String msg = label;

      try
      {
         msg = messages.getMessage(label);
      }
      catch (IllegalArgumentException e)
      {
         warning(String.format(
           "Error fetching message for label '%s': %s", 
            label, e.getMessage()), e);
      }

      return msg;
   }

   /*
    *  TeXApp method.
    */ 
   public String getMessage(String label, String param)
   {
      if (messages == null)
      {// message system hasn't been initialised
         return String.format("%s[%s]", label, param);
      }

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
      if (messages == null)
      {// message system hasn't been initialised

         String param = (params.length == 0 ? "" : params[0]);

         for (int i = 1; i < params.length; i++)
         {
            param += ","+params[0];
         }

         return String.format("%s[%s]", label, param);
      }

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
      if (messages == null)
      {// message system hasn't been initialised

         String param = (params.length == 0 ? "" : params[0].toString());

         for (int i = 1; i < params.length; i++)
         {
            param += ","+params[0].toString();
         }

         return String.format("%s[%s]", label, param);
      }

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

   public String getMessage(TeXParser parser, String label, Object... params)
   {
      if (parser == null)
      {
         return getMessage(label, params);
      }

      int lineNum = parser.getLineNumber();
      File file = parser.getCurrentFile();

      if (lineNum == -1 || file == null)
      {
         return getMessage(label, params);
      }
      else
      {
         return fileLineMessage(file, lineNum, getMessage(label, params));
      }
   }

   public String getMessageWithFallback(String label,
       String fallbackFormat, Object... params)
   {
      if (messages == null)
      {// message system hasn't been initialised

         MessageFormat fmt = new MessageFormat(fallbackFormat);
         return fmt.format(fallbackFormat, params);
      }

      try
      {
         return messages.getMessage(label, params);
      }
      catch (IllegalArgumentException e)
      {
         warning("Can't find message for label: "+label, e);

         MessageFormat fmt = new MessageFormat(fallbackFormat);
         return fmt.format(fallbackFormat, params);
      }
   }

   public String getChoiceMessage(String label, int argIdx,
     String choiceLabel, int numChoices, Object... params)
   {
      if (messages == null)
      {// message system hasn't been initialised

         String param = (params.length == 0 ? "" : params[0].toString());

         for (int i = 1; i < params.length; i++)
         {
            param += ","+params[0].toString();
         }

         return String.format("%s[%s]", label, param);
      }

      String msg = label;

      try
      {
         msg = messages.getChoiceMessage(label, argIdx,
            choiceLabel, numChoices, params);
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
      if (verboseLevel != SILENT)
      {
         System.out.println(text);
      }
   }

   public static String fileLineMessage(File file, int lineNum,
     String message)
   {
      return String.format("%s:%d: %s", file.toString(), lineNum,
         message);
   }

   public void endParse(File file)
      throws IOException
   {
   }

   public void beginParse(File file, Charset charset)
      throws IOException
   {
      message(getMessage("message.reading", file));

      if (charset == null)
      {
         message(getMessage("message.default.charset", 
            Charset.defaultCharset()));
      }
      else
      {
         message(getMessage("message.tex.charset", charset));
      }
   }

   public Charset getCharSet()
   {
      return charset;
   }

   /*
    *  TeXApp method.
    */ 
   public void warning(TeXParser parser, String message)
   {
      if (verboseLevel != SILENT)
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
      warning(fileLineMessage(file, line, message));
   }

   public void warning(File file, int line, String message, Exception e)
   {
      warning(fileLineMessage(file, line, message), e);
   }

   public void warning(String message)
   {
      if (verboseLevel != SILENT)
      {
         message = getMessageWithFallback("warning.title",
            "Warning: {0}", message);

         System.err.println(message);
      }
   }

   public void warning()
   {
      if (verboseLevel != SILENT)
      {
         System.err.println();
      }
   }

   public void warning(String message, Exception e)
   {
      if (verboseLevel >= NORMAL)
      {
         System.err.println(message);

         if (verboseLevel >= DEBUG)
         {
            e.printStackTrace();
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
         error(((TeXSyntaxException)e).getMessage(this));
      }
      else
      {
         String msg = e.getMessage();

         if (msg == null)
         {
            msg = e.getClass().getSimpleName();
         }

         error(msg);
      }

      if (verboseLevel >= DEBUG)
      {
         e.printStackTrace();
      }
   }

   public void error(String msg)
   {
      msg = getMessageWithFallback("error.title", "Error: {0}", msg);

      System.err.println(msg);
   }

   /*
    *  TeXApp method.
    */ 
   // shouldn't be needed here
   public float emToPt(float emValue)
   {
      warning(getParser(),
         "Can't convert from em to pt, no font information loaded");

      return 9.5f*emValue;
   }

   // shouldn't be needed here
   public float exToPt(float exValue)
   {
      warning(getParser(),
         "Can't convert from ex to pt, no font information loaded");

      return 4.4f*exValue;
   }

   public void addData(GlsData entryData)
   {
      data.add(entryData);
   }

   public boolean hasEntry(String label)
   {
      for (GlsData entryData : data)
      {
         if (entryData.getId().equals(label))
         {
            return true;
         }
      }

      return false;
   }

   public void process() throws IOException
   {
      data = new Vector<GlsData>();

      parser.parse(texFile, charset);

      PrintWriter out = null;

      try
      {
         if (data.isEmpty())
         {
            throw new IOException("No entries found");
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

         for (GlsData entry : data)
         {
            entry.writeBibEntry(out);
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

   private String getLanguageFileName(String tag)
   {
      return String.format("/resources/bib2gls-%s.xml", tag);
   }

   private void initMessages(String langTag) throws Gls2BibException,IOException
   {
      Locale locale;

      if (langTag == null || "".equals(langTag))
      {
         locale = Locale.getDefault();
      }
      else
      {
         locale = Locale.forLanguageTag(langTag);
      }

      String lang = locale.toLanguageTag();

      String name = getLanguageFileName(lang);

      URL url = getClass().getResource(name);

      String jar = "";

      if (verboseLevel >= DEBUG)
      {
         jar = getClass().getProtectionDomain().getCodeSource().getLocation()
               .toString();
      }

      if (url == null)
      {
         debug(String.format("Can't find language resource: %s!%s", jar, name));

         lang = locale.getLanguage();

         name = getLanguageFileName(lang);

         debug("Trying: "+name);

         url = getClass().getResource(name);

         if (url == null)
         {
            debug(String.format("Can't find language resource: %s!%s",
                    jar, name));

            String script = locale.getScript();

            if (script != null && !script.isEmpty())
            {
               name = getLanguageFileName(String.format("%s-%s", lang, script));

               debug("Trying: "+name);

               url = getClass().getResource(name);

               if (url == null && !lang.equals("en"))
               {
                  debug(String.format(
                    "Can't find language resource: %s!%s%nDefaulting to 'en'",
                    jar, name));

                  url = getClass().getResource(getLanguageFileName("en"));
               }
            }
            else if (!lang.equals("en"))
            {
               debug("Defaulting to 'en'");
               url = getClass().getResource(getLanguageFileName("en"));
            }

            if (url == null)
            {
               throw new Gls2BibException("Can't find language resource file.");
            }
         }
      }

      InputStream in = null;

      try
      {
         debug("Reading "+url);

         in = url.openStream();

         Properties prop = new Properties();

         prop.loadFromXML(in);

         in.close();
         in = null;

         messages = new Gls2BibMessages(prop);
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }
   }

   public static void version()
   {
      System.out.format("convertgls2bib v%s (%s)%n", VERSION, DATE);
      System.out.format("Copyright 2017-%s Nicola Talbot%n", 
        DATE.substring(0,4));
      System.out.print("License GPLv3+: GNU GPL version 3 or later");
      System.out.println(" <http://gnu.org/licenses/gpl.html>");
      System.out.print("This is free software: you are free to change");
      System.out.println(" and redistribute it.");
      System.out.println("There is NO WARRANTY, to the extent permitted by law.");

   }

   public static void help()
   {
      System.out.println(
        "convertgls2bib [<options>] <tex file> <bib file>");
      System.out.println("Options:");

      System.out.println("--version (or -v)\tDisplay version information");
      System.out.println("--help (or -h)\t\tDisplay help");
      System.out.println("--texenc <encoding>\t.tex file encoding");
      System.out.println("--bibenc <encoding>\t.bib file encoding");
      System.out.println("--ignore-sort\t\tignore sort field (default)");
      System.out.println("--no-ignore-sort\tdon't ignore sort field");
      System.out.println("--space-sub <value>\tsubstitute spaces in labels with <value>");
      System.out.println("--locale <lang tag>\tuse language resource file for locale given by <lang tag>");
      System.out.println("--silent\t\tsuppress messages");
      System.out.println("--verbose\t\tnormal messages");
      System.out.println("--debug\t\t\tdebug mode");
   }

   public static void main(String[] args)
   {
      String texFile = null;
      String bibFile = null;
      String texCharset = null;
      String bibCharset = null;
      String spaceSub = null;
      String langTag = null;
      boolean ignoreSort = true;
      int verbose = NORMAL;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--help") || args[i].equals("-h"))
         {
            help();
            System.exit(0);
         }
         else if (args[i].equals("--version") || args[i].equals("-v"))
         {
            version();
            System.exit(0);
         }
         else if (args[i].equals("--texenc"))
         {
            if (i == args.length-1)
            {
               System.err.println("Missing <encoding> after "+args[i]);
               System.exit(1);
            }

            texCharset = args[++i];
         }
         else if (args[i].equals("--bibenc"))
         {
            if (i == args.length-1)
            {
               System.err.println("Missing <encoding> after "+args[i]);
               System.exit(1);
            }

            bibCharset = args[++i];
         }
         else if (args[i].equals("--space-sub"))
         {
            if (i == args.length-1)
            {
               System.err.println("Missing <value> after "+args[i]);
               System.exit(1);
            }

            spaceSub = args[++i];
         }
         else if (args[i].equals("--locale"))
         {
            if (i == args.length-1)
            {
               System.err.println("Missing <lang tag> after "+args[i]);
               System.exit(1);
            }

            langTag = args[++i];
         }
         else if (args[i].equals("--ignore-sort"))
         {
            ignoreSort = true;
         }
         else if (args[i].equals("--no-ignore-sort"))
         {
            ignoreSort = false;
         }
         else if (args[i].equals("--debug"))
         {
            verbose = DEBUG;
         }
         else if (args[i].equals("--silent"))
         {
            verbose = SILENT;
         }
         else if (args[i].equals("--verbose"))
         {
            verbose = NORMAL;
         }
         else if (args[i].startsWith("-"))
         {
            System.err.println("Unknown option: "+args[i]);
            System.err.println("Use --help for help.");
            System.exit(1);
         }
         else if (texFile == null)
         {
            texFile = args[i];
         }
         else if (bibFile == null)
         {
            bibFile = args[i];
         }
         else
         {
            System.err.println("Too many arguments");
            System.err.println("Use --help for help.");
            System.exit(1);
         }
      }

      if (texFile == null)
      {
          System.err.println("Missing <tex file>");
          help();
          System.exit(1);
      }

      if (bibFile == null)
      {
          System.err.println("Missing <bib file>");
          help();
          System.exit(1);
      }

      try
      {
         Gls2Bib gls2bib = new Gls2Bib(texFile, bibFile, texCharset, bibCharset,
           spaceSub, langTag, ignoreSort, verbose);

         gls2bib.process();
      }
      catch (IOException e)
      {
         System.err.println(e.getMessage());
         System.exit(2);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         System.exit(3);
      }
   }

   public boolean fieldExpansionOn(String field)
   {
      if (expandFieldMap != null)
      {
         Boolean bool = expandFieldMap.get(field);

         if (bool != null)
         {
            return bool.booleanValue();
         }
      }

      return expandFields;
   }

   public void setFieldExpansion(boolean on)
   {
      expandFields = on;
   }

   public void setFieldExpansion(String field, boolean on)
   {
      String val = keyToFieldMap.get(field);

      if (val != null)
      {
         field = val;
      }

      if (expandFieldMap == null)
      {
         expandFieldMap = new HashMap<String,Boolean>();
      }

      expandFieldMap.put(field, Boolean.valueOf(on));
   }

   public static final String VERSION = "1.7.20181129";
   public static final String DATE = "2018-11-29";

   private Vector<GlsData> data;

   private File texFile, bibFile;

   private Charset charset=null;

   private String bibCharsetName=null;

   private String spaceSub = null;

   private boolean ignoreSortField=true;

   private Gls2BibMessages messages;

   public static final int SILENT=0, NORMAL=1, DEBUG=2;

   private int verboseLevel=NORMAL;

   private boolean expandFields = false;

   private HashMap<String,Boolean> expandFieldMap;

   private HashMap<String,String> keyToFieldMap;

   private TeXParser texParser;
}
