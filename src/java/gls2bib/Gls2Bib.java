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
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
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
   public Gls2Bib(String[] args, String langTag, int verbose)
    throws Gls2BibException,IOException
   {
      super(null);

      this.verboseLevel = verbose;
      initMessages(langTag);

      parseArgs(args);

      initKeyToFieldMap();

      setWriteable(this);

      texParser = new TeXParser(this);
   }

   public Gls2Bib(String texFilename, String bibFilename, String inCharset,
     String outCharset, String spaceSub, String langTag, boolean ignoreSort, 
     int verbose, boolean ignoreType)
    throws Gls2BibException,IOException
   {
      super(null);

      this.verboseLevel = verbose;
      this.ignoreSortField = ignoreSort;
      this.ignoreTypeField = ignoreType;

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

   public boolean ignoreType()
   {
      return ignoreTypeField;
   }

   public boolean isSplitTypeOn()
   {
      return splitOnType;
   }

   public boolean isOverwriteOn()
   {
      return overwrite;
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

      HashMap<String,PrintWriter> types = null;

      if (splitOnType)
      {
         types = new HashMap<String,PrintWriter>();
      }

      try
      {
         if (data.isEmpty())
         {
            throw new IOException("No entries found");
         }

         if (!overwrite && bibFile.exists())
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

         for (GlsData entry : data)
         {
            if (splitOnType)
            {
               String type = entry.getGlossaryType();

               if (type == null)
               {
                  entry.writeBibEntry(out);
               }
               else
               {
                  PrintWriter splitOut = types.get(type);

                  if (splitOut == null)
                  {
                     File splitBibFile = new File(bibFile.getParent(), type+".bib");

                    if (!overwrite && splitBibFile.exists())
                    {
                       throw new IOException(getMessage("error.file_exists.nooverwrite",
                          splitBibFile, "--overwrite"));
                    }

                     message(getMessage("message.writing", splitBibFile));

                     if (bibCharsetName == null)
                     {
                        splitOut = new PrintWriter(splitBibFile);
                     }
                     else
                     {
                        splitOut = new PrintWriter(splitBibFile, bibCharsetName);

                        splitOut.println("% Encoding: "+bibCharsetName);
                     }

                     types.put(type, splitOut);
                  }

                  entry.writeBibEntry(splitOut);
               }
            }
            else
            {
               entry.writeBibEntry(out);
            }
         }
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }

         if (types != null)
         {
            Set<String> keySet = types.keySet();

            for (Iterator<String> it=keySet.iterator(); it.hasNext(); )
            {
               String type = it.next();
               types.get(type).close();
            }
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

   public void version()
   {
      System.out.format("%s v%s (%s)%n", APP_NAME, VERSION, DATE);
      System.out.format("Copyright 2017-%s Nicola Talbot%n", 
        DATE.substring(0,4));
      System.out.print("License GPLv3+: GNU GPL version 3 or later");
      System.out.println(" <http://gnu.org/licenses/gpl.html>");
      System.out.print("This is free software: you are free to change");
      System.out.println(" and redistribute it.");
      System.out.println("There is NO WARRANTY, to the extent permitted by law.");

   }

   public void help()
   {
      System.out.println(getMessage("gls2bib.syntax", APP_NAME));
      System.out.println(getMessage("gls2bib.syntax.options"));

      System.out.println(getMessage("gls2bib.syntax.version", "--version",
       "-v"));
      System.out.println(getMessage("gls2bib.syntax.help", "--help", "-h"));
      System.out.println(getMessage("gls2bib.syntax.texenc", "--texenc"));
      System.out.println(getMessage("gls2bib.syntax.bibenc", "--bibenc"));
      System.out.println(getMessage("gls2bib.syntax.ignore-sort",
        "--ignore-sort"));
      System.out.println(getMessage("gls2bib.syntax.no-ignore-sort",
        "--no-ignore-sort"));
      System.out.println(getMessage("gls2bib.syntax.ignore-type",
        "--ignore-type"));
      System.out.println(getMessage("gls2bib.syntax.no-ignore-type",
        "--no-ignore-type"));
      System.out.println(getMessage("gls2bib.syntax.split-on-type",
        "--split-on-type"));
      System.out.println(getMessage("gls2bib.syntax.no-split-on-type",
        "--no-split-on-type"));
      System.out.println(getMessage("gls2bib.syntax.overwrite",
        "--overwrite", "--split-on-type"));
      System.out.println(getMessage("gls2bib.syntax.no-overwrite",
        "--no-overwrite"));
      System.out.println(getMessage("gls2bib.syntax.space-sub",
        "--space-sub"));
      System.out.println(getMessage("gls2bib.syntax.locale",
        "--locale"));
      System.out.println(getMessage("gls2bib.syntax.silent",
        "--silent"));
      System.out.println(getMessage("gls2bib.syntax.verbose",
        "--verbose"));
      System.out.println(getMessage("gls2bib.syntax.debug",
        "--debug"));
   }

   protected void parseArgs(String[] args)
    throws Gls2BibSyntaxException
   {
      texFile = null;
      bibFile = null;
      spaceSub = null;

      charset = Charset.defaultCharset();
      bibCharsetName = null;
      ignoreSortField = true;
      ignoreTypeField = false;
      splitOnType = false;
      int overwrite_setting = -1;

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
               throw new Gls2BibSyntaxException(
                  getMessage("gls2bib.missing.encoding.value",
                  args[i]));
            }

            charset = Charset.forName(args[++i]);
         }
         else if (args[i].equals("--bibenc"))
         {
            if (i == args.length-1)
            {
               throw new Gls2BibSyntaxException(
                  getMessage("gls2bib.missing.encoding.value",
                  args[i]));
            }

            bibCharsetName = args[++i];
         }
         else if (args[i].equals("--space-sub"))
         {
            if (i == args.length-1)
            {
               throw new Gls2BibSyntaxException(
                  getMessage("gls2bib.missing.arg.value",
                  args[i]));
            }

            spaceSub = args[++i];

            if (" ".equals(spaceSub))
            {
               spaceSub = null;
            }
         }
         else if (args[i].equals("--locale"))
         {
            // already read this value in main(String[]).
            i++;
         }
         else if (args[i].equals("--ignore-sort"))
         {
            ignoreSortField = true;
         }
         else if (args[i].equals("--no-ignore-sort"))
         {
            ignoreSortField = false;
         }
         else if (args[i].equals("--ignore-type"))
         {
            ignoreTypeField = true;
         }
         else if (args[i].equals("--no-ignore-type"))
         {
            ignoreTypeField = false;
         }
         else if (args[i].equals("--split-on-type"))
         {
            splitOnType = true;
         }
         else if (args[i].equals("--no-split-on-type"))
         {
            splitOnType = false;
         }
         else if (args[i].equals("--overwrite"))
         {
            overwrite_setting = 1;
         }
         else if (args[i].equals("--no-overwrite"))
         {
            overwrite_setting = 0;
         }
         else if (args[i].equals("--debug")
               || args[i].equals("--silent")
               || args[i].equals("--verbose"))
         {// skip (already checked in main(String[]))
         }
         else if (args[i].startsWith("-"))
         {
            throw new Gls2BibSyntaxException(
               getMessage("gls2bib.unknown.arg",
               args[i], "--help"));
         }
         else if (texFile == null)
         {
            texFile = new File(args[i]);
         }
         else if (bibFile == null)
         {
            if (args[i].toLowerCase().endsWith(".bib"))
            {
               bibFile = new File(args[i]);
            }
            else
            {
               bibFile = new File(args[i]+".bib");
            }
         }
         else
         {
            throw new Gls2BibSyntaxException(
               getMessage("gls2bib.toomany.arg", "--help"));
         }
      }

      if (texFile == null)
      {
          throw new Gls2BibSyntaxException(getMessage("gls2bib.missing.tex.arg",
            getMessage("gls2bib.syntax", APP_NAME), "--help"));
      }

      if (bibFile == null)
      {
          throw new Gls2BibSyntaxException(getMessage("gls2bib.missing.bib.arg",
            getMessage("gls2bib.syntax", APP_NAME), "--help"));
      }

      if (bibCharsetName == null)
      {
         bibCharsetName = charset.name();
      }

      switch (overwrite_setting)
      {
         case -1:
           overwrite = (splitOnType ? false : true);
         break;
         case 0:
           overwrite = false;
         break;
         case 1:
           overwrite = true;
         break;
      }
   }

   public static void main(String[] args)
   {
      // Just check for verbosity and language tag.
      // The rest is checked after the message system has been
      // initialised.
      String langTag = null;
      int verbose = NORMAL;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--texenc")
          || args[i].equals("--bibenc")
          || args[i].equals("--space-sub"))
         {// skip 1 argument
            i++;
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
      }

      try
      {
         Gls2Bib gls2bib = new Gls2Bib(args, langTag, verbose);

         gls2bib.process();
      }
      catch (Gls2BibSyntaxException e)
      {
         System.err.println(e.getMessage());
         System.exit(1);
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

   public static final String VERSION = "1.9.20200129";
   public static final String DATE = "2020-01-29";
   public static final String APP_NAME = "convertgls2bib";

   private Vector<GlsData> data;

   private File texFile, bibFile;

   private Charset charset=null;

   private String bibCharsetName=null;

   private String spaceSub = null;

   private boolean ignoreSortField=true;
   private boolean ignoreTypeField=false;
   private boolean splitOnType=false;
   private boolean overwrite=true;

   private Gls2BibMessages messages;

   public static final int SILENT=0, NORMAL=1, DEBUG=2;

   private int verboseLevel=NORMAL;

   private boolean expandFields = false;

   private HashMap<String,Boolean> expandFieldMap;

   private HashMap<String,String> keyToFieldMap;

   private TeXParser texParser;
}
