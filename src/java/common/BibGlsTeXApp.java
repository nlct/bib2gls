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
package com.dickimawbooks.bibgls.common;

/**
 * Common TeXApp.
 */

import java.util.Properties;
import java.util.Locale;
import java.util.ArrayDeque;
import java.text.MessageFormat;
import java.text.BreakIterator;
import java.io.*;

import java.net.URL;

import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;

public abstract class BibGlsTeXApp implements TeXApp
{
   protected BibGlsTeXApp()
   {
   }

   protected void initialise(String[] args)
    throws Bib2GlsException,IOException
   {
      ArrayDeque<String> deque = preparse(args);

      initMessages();

      parseArgs(deque);
   }

   public boolean isSilent()
   {
      return (verboseLevel == SILENT);
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

   @Override
   public boolean isWriteAccessAllowed(File file)
   {
      return file.canWrite();
   }

   @Override
   public boolean isWriteAccessAllowed(TeXPath path)
   {
      return isWriteAccessAllowed(path.getFile());
   }

   @Override
   public boolean isReadAccessAllowed(File file)
   {
      return file.canRead();
   }

   @Override
   public boolean isReadAccessAllowed(TeXPath path)
   {
      return isReadAccessAllowed(path.getFile());
   }

   @Override
   public void substituting(TeXParser parser, String original,
     String replacement)
   {  
      debug(getMessage("warning.substituting", original, replacement));
   }

   @Override
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

   @Override
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

   /*
    *  TeXApp method needs defining, but not needed for
    *  the purposes of this application.
    */ 
   @Override
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
   @Override
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
    *  TeXApp method needs defining, but not needed for
    *  the purposes of this application.
    */ 
   @Override
   public void convertimage(int inPage, String[] inOptions, File inFile,
     String[] outOptions, File outFile)
     throws IOException,InterruptedException
   {
      if (verboseLevel >= DEBUG)
      {// shouldn't happen
         System.err.format(
           "Ignoring unexpected request to convert %s to %s%n",
           inFile.toString(), outFile.toString());
      }
   }

   /*
    *  TeXApp method.
    */ 
   @Override
   public void progress(int percent)
   {
   }

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

   /*
    *  TeXApp method.
    */ 
   @Override
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
   @Override
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


   @Override
   public Charset getDefaultCharset()
   {
      return charset == null ? Charset.defaultCharset() : charset;
   }

   /*
    *  TeXApp method.
    */ 
   @Override
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
   @Override
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

   public String getLanguageFileName(String tag)
   {
      return String.format("/resources/bib2gls-%s.xml", tag);
   }

   private void initMessages() throws Bib2GlsException,IOException
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
               throw new Bib2GlsException("Can't find language resource file.");
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

   public void printSyntaxItem(String message)
   {
      String[] split = message.split("\t", 2);

      if (split.length == 2)
      {
         String desc = split[1].replaceAll(" *\\n", " ");

         int syntaxLength = split[0].length();
         int descLength = desc.length();

         System.out.print("  "+split[0]);

         int numSpaces = SYNTAX_ITEM_TAB - syntaxLength - 2;

         if (numSpaces <= 0)
         {
            numSpaces = 2;
         }

         int indent = syntaxLength+2+numSpaces;

         int width = SYNTAX_ITEM_LINEWIDTH-indent;

         for (int i = 0; i < numSpaces; i++)
         {
            System.out.print(' ');
         }

         if (width >= descLength)
         {
            System.out.println(desc);
         }
         else
         {
            BreakIterator boundary = BreakIterator.getLineInstance();
            boundary.setText(desc);

            int start = boundary.first();
            int n = 0;

            int defWidth = SYNTAX_ITEM_LINEWIDTH - SYNTAX_ITEM_TAB;
            numSpaces = SYNTAX_ITEM_TAB;

            for (int end = boundary.next();
               end != BreakIterator.DONE;
               start = end, end = boundary.next())
            {
               int len = end-start;
               n += len;

               if (n >= width)
               {
                  System.out.println();

                  for (int i = 0; i < numSpaces; i++)
                  {
                     System.out.print(' ');
                  }

                  n = len;
                  width = defWidth;
               }

               System.out.print(desc.substring(start,end));
            }

            System.out.println();

         }
      }
      else if (split.length == 1)
      {
         String desc = split[0].replaceAll(" *\\n", " ");

         int descLength = desc.length();

         if (descLength <= SYNTAX_ITEM_LINEWIDTH)
         {
            System.out.println(desc);
         }
         else
         {
            BreakIterator boundary = BreakIterator.getLineInstance();
            boundary.setText(desc);

            int start = boundary.first();
            int n = 0;

            for (int end = boundary.next();
               end != BreakIterator.DONE;
               start = end, end = boundary.next())
            {
               int len = end-start;
               n += len;

               if (n >= SYNTAX_ITEM_LINEWIDTH)
               {
                  System.out.println();
                  n = len;
               }

               System.out.print(desc.substring(start,end));
            }

            System.out.println();
         }
      }
   }

   public void version()
   {
      System.out.println(getMessageWithFallback("about.version",
        "{0} version {1} ({2})", getApplicationName(), VERSION, DATE));
   }

   public void license()
   {
      System.out.println();
      System.out.format("Copyright %s Nicola Talbot%n",
       getCopyrightDate());
      System.out.println(getMessage("about.license"));
      System.out.println("https://github.com/nlct/bib2gls");
   }

   public void libraryVersion()
   {
      System.out.println();
      System.out.println(getMessageWithFallback("about.library.version",
        "Bundled with {0} version {1} ({2})",
        "texparserlib.jar", TeXParser.VERSION, TeXParser.VERSION_DATE));
      System.out.println("https://github.com/nlct/texparser");
   }

   @Override
   public String getApplicationVersion()
   {
      return VERSION;
   }

   public abstract String getCopyrightStartYear();

   public String getCopyrightDate()
   {
      String startYr = getCopyrightStartYear();
      String endYr = DATE.substring(0, 4);

      if (startYr.equals(endYr))
      {
         return endYr;
      }
      else
      {
         return String.format("%s-%s", startYr, endYr);
      }
   }
   
   protected ArrayDeque<String> preparse(String[] args)
     throws Bib2GlsSyntaxException
   {
      ArrayDeque<String> deque = new ArrayDeque<String>(args.length);

      // Just check for verbosity and language tag.
      // The rest is checked after the message system has been
      // initialised.
      verboseLevel = NORMAL;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--locale"))
         {
            if (i == args.length-1)
            {
               throw new Bib2GlsSyntaxException(
               "Missing <lang tag> after "+args[i]);
            }

            langTag = args[++i];
         }
         else if (args[i].equals("--debug"))
         {
            verboseLevel = DEBUG;
         }
         else if (args[i].equals("--silent")
                 || args[i].equals("--quiet")
                 || args[i].equals("-q"))
         {
            verboseLevel = SILENT;
         }
         else if (args[i].equals("--verbose"))
         {
            verboseLevel = NORMAL;
         }
         else
         {
            deque.add(args[i]);

            if (args[i].startsWith("-"))
            {
               String[] split = args[i].split("=", 2);

               int n = argCount(split[0]);

               if (split.length == 2)
               {
                  n--;
               }

               for (int j = 0; j < n; j++)
               {
                  i++;

                  if (i < args.length)
                  {
                     deque.add(args[i]);
                  }
               }
            }
         }
      }

      return deque;
   }

   protected boolean parseArg(ArrayDeque<String> deque, String arg,
      String[] returnVals)
    throws Bib2GlsSyntaxException
   {
      return false;
   }

   protected abstract int argCount(String arg);

   protected boolean isArg(ArrayDeque<String> deque, String arg,
     String longName, String[] returnVals)
   {
      return isArg(deque, arg, longName, null, returnVals);
   }

   protected boolean isArg(ArrayDeque<String> deque, String arg,
     String longName, String shortName, String[] returnVals)
   {
      String[] split = arg.split("=", 2);

      int n = 0;

      if (split[0].equals(longName))
      {
         n = argCount(longName);

         if (n == 0)
         {
            returnVals[0] = null;
         }
         else if (split.length == 1)
         {
            returnVals[0] = deque.poll();
         }
         else
         {
            returnVals[0] = split[1];
         }
      }
      else if (shortName != null && arg.equals(shortName))
      {
         n = argCount(shortName);

         if (n == 0)
         {
            returnVals[0] = null;
         }
         else
         {
            returnVals[0] = deque.poll();
         }
      }
      else
      {
         return false;
      }

      for (int i = 1; i < n; i++)
      {
         returnVals[i] = deque.poll();
      }

      return true;
   }

   protected abstract void parseArgs(ArrayDeque<String> deque)
    throws Bib2GlsSyntaxException;

   protected void furtherInfo()
   {
      System.out.println(getMessage("syntax.userguide", "bib2gls", "texdoc bib2gls"));
      System.out.println(getMessage("syntax.ctan", "bib2gls",
         "https://ctan.org/pkg/bib2gls"));
      System.out.println(getMessage("syntax.home", "bib2gls",
        "https://www.dickimaw-books.com/software/bib2gls/"));
   }

   protected Charset charset = Charset.defaultCharset();

   protected Bib2GlsMessages messages;

   public static final int SILENT=0, NORMAL=1, DEBUG=2;

   protected String langTag = null;
   protected int verboseLevel=NORMAL;

   public static final int SYNTAX_ITEM_LINEWIDTH=78;
   public static final int SYNTAX_ITEM_TAB=30;

   public static final String VERSION = "3.9.20140201";
   public static final String DATE = "2024-02-01";

}
