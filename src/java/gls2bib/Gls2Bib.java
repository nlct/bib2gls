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
import java.util.Properties;
import java.util.Locale;
import java.text.MessageFormat;
import java.io.*;

import java.net.URL;

// Requires Java 1.7:
import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.primitives.Relax;
import com.dickimawbooks.texparserlib.latex.LaTeXParserListener;
import com.dickimawbooks.texparserlib.latex.KeyValList;

public class Gls2Bib extends LaTeXParserListener
  implements Writeable
{
   public Gls2Bib(String texFilename, String bibFilename, String inCharset,
     String outCharset, String spaceSub, String langTag)
    throws Gls2BibException,IOException
   {
      super(null);

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

      setWriteable(this);
   }

   public String getSpaceSub()
   {
      return spaceSub;
   }

   public TeXApp getTeXApp()
   {
      return texApp;
   }

   public void debug(String message)
   {
      if (debugLevel > 0)
      {
         System.out.println(message);
      }
   }

   public void debug(Throwable e)
   {
      if (debugLevel > 0)
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

   }

   // Ignore unknown control sequences
   public ControlSequence createUndefinedCs(String name)
   {
      return new Gls2BibUndefined(name);
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
   }

   public void abovewithdelims(TeXObject firstDelim,
     TeXObject secondDelim, TeXDimension thickness, TeXObject before,
     TeXObject after)
    throws IOException
   {
   }

   public void skipping(Ignoreable ignoreable)
      throws IOException
   {
   }
   public void href(String url, TeXObject text)
      throws IOException
   {
   }

   public void subscript(TeXObject arg)
     throws IOException
   {
   }

   public void superscript(TeXObject arg)
     throws IOException
   {
   }

   public void includegraphics(KeyValList options, String imgName)
     throws IOException
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

   public static String fileLineMessage(File file, int lineNum,
     String message)
   {
      return String.format("%s:%d: %s", file.toString(), lineNum,
         message);
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

   public void endParse(File file)
      throws IOException
   {
   }

   public void beginParse(File file)
      throws IOException
   {
   }

   public Charset getCharSet()
   {
      return charset;
   }

   public void warning(String message)
   {
      System.out.println(message);
   }

   public void warning(String message, Throwable e)
   {
      System.out.println(message);
      debug(e);
   }

   // shouldn't be needed here
   public float emToPt(float emValue)
   {
      getTeXApp().warning(getParser(),
         "Can't convert from em to pt, no font information loaded");

      return 9.5f*emValue;
   }

   // shouldn't be needed here
   public float exToPt(float exValue)
   {
      getTeXApp().warning(getParser(),
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
      // Just use generic adapter.
      // kpsewhich isn't used to find files or to determine
      // openin_any or openout_any

      texApp = new TeXAppAdapter();

      data = new Vector<GlsData>();

      TeXParser parser = new TeXParser(this);

      parser.parse(texFile);

      PrintWriter out = null;

      try
      {
         if (data.isEmpty())
         {
            throw new IOException("No entries found");
         }

         if (bibCharsetName == null)
         {
            out = new PrintWriter(bibFile);
         }
         else
         {
            out = new PrintWriter(bibFile, bibCharsetName);
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

      if (debugLevel > 0)
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
      System.out.println(String.format("gls2bib v%s (%s)", VERSION, DATE));
   }

   public static void help()
   {
      System.out.println(
        "gls2bib [<options>] <tex file> <bib file>");
      System.out.println("Options:");
      System.out.println("--texenc <encoding>\t.tex file encoding");
      System.out.println("--bibenc <encoding>\t.bib file encoding");
      System.out.println("--space-sub <value>\tsubstitute spaces in labels with <value>");
      System.out.println("--locale <lang tag>\tuse language resource file for locale given by <lang tag>");
   }

   public static void main(String[] args)
   {
      String texFile = null;
      String bibFile = null;
      String texCharset = null;
      String bibCharset = null;
      String spaceSub = null;
      String langTag = null;

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
            help();
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
           spaceSub, langTag);

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

   private static final String VERSION="0.9b";
   private static final String DATE="EXPERIMENTAL";

   private TeXApp texApp;
   private Vector<GlsData> data;

   private File texFile, bibFile;

   private Charset charset=null;

   private String bibCharsetName=null;

   private String spaceSub = null;

   private Gls2BibMessages messages;

   private int debugLevel = 0;//TODO implement
}
