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
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

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

      preInitMessages();

      initMessages();

      postInitMessages();

      parseArgs(deque);

      setDebugLevel(debugLevel);
   }

   protected void preInitMessages()
    throws Bib2GlsException,IOException
   {
   }

   protected void postInitMessages()
    throws Bib2GlsException,IOException
   {
   }

   protected void setDebugLevel(int level)
   {
      if (level < 0)
      {
         throw new IllegalArgumentException(getMessage(
           "error.invalid.opt.minint.value", "--debug",
             level, 0));
      }

      debugLevel = level;

      if (level > 0)
      {
         verboseLevel = DEBUG;
      }
   }

   protected void setDebugLevel(String... modes)
    throws Bib2GlsSyntaxException
   {
      verboseLevel = DEBUG;

      try
      {
         debugLevel = TeXParser.getDebugLevelFromModeList(modes);
      }
      catch (TeXSyntaxException e)
      {
         throw new Bib2GlsSyntaxException(
          getMessage("error.invalid.option_syntax", "--debug-mode",
           e.getMessage(this)));
      }
   }

   public boolean isSilent()
   {
      return (verboseLevel == SILENT);
   }

   public boolean isDebuggingOn()
   {
      return (debugLevel > 0 || verboseLevel >= DEBUG);
   }

   public boolean isVerbose()
   {
      return verboseLevel >= VERBOSE;
   }

   public int getDebugLevel()
   {     
      return debugLevel;
   }     
         
   public int getVerboseLevel()
   {
      return verboseLevel;
   }  

   public void logMessageNoLn(String msg)
   {
      if (logWriter != null)
      {
         logWriter.print(msg);
      }
   }

   public void logMessage(String msg)
   {
      if (logWriter != null)
      {
         logWriter.println(msg);
      }
   }

   public void logMessage()
   {
      if (logWriter != null)
      {
         logWriter.println();
      }
   }

   public void exit()
   {
      if (logWriter != null)
      {
         logWriter.close();
            message(getMessageWithFallback(
              "message.log.file",
              "Transcript written to {0}.",
               transcriptFile));
   
         logWriter = null;
      }

      System.exit(exitCode);
   }

   public void logAndPrintMessageNoLn(String message)
   {
      logMessageNoLn(message);
      System.out.print(message);
   }

   public void logAndPrintMessage(String message)
   {     
      logMessage(message);
      System.out.println(message);
   }
            
   public void logAndPrintMessage()
   {     
      logMessage();
      System.out.println();
   }     

   public void debug()
   { 
      if (isDebuggingOn())
      {  
         System.out.println();
         logMessage();
      }  
   }        

   public void debug(String message)
   {
      if (isDebuggingOn())
      {
         logAndPrintMessage(message);
      }
   }

   public void debug(Throwable e)
   {
      if (isDebuggingOn())
      {
         e.printStackTrace();
      }
   }

   public void debugMessage(String key, Object... params)
   {
      if (isDebuggingOn())
      {
         logAndPrintMessage(getMessage(key, params));
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

   /*
    *  TeXApp method. This is used by the TeX parser library
    *  when substituting deprecated commands.
    */   
   @Override
   public void substituting(TeXParser parser, String original,
     String replacement)
   {  
      verboseMessage("warning.substituting", original, replacement);
   }

   /*
    *  TeXApp method needs defining, but shouldn't be needed for
    *  the purposes of this application. This is mainly used by
    *  the TeX parser library to copy images to a designated output
    *  directory when performing LaTeX -> LaTeX or LaTeX -> HTML
    *  actions.
    */ 
   @Override
   public void copyFile(File orgFile, File newFile)
     throws IOException,InterruptedException
   {
      if (isDebuggingOn())
      {
         System.err.format(
           "Ignoring unexpected request to copy files %s -> %s%n",
           orgFile.toString(), newFile.toString());
      }
   }

   /*
    * TeXApp method needs defining, but unlikely to be needed for
    * the purposes of this application. (It doesn't make any 
    * sense to have something like \read-1 in a bib field.)
    * Just return empty string.
    */ 
   @Override
   public String requestUserInput(String message)
     throws IOException
   {
      if (isDebuggingOn())
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
      if (isDebuggingOn())
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
      if (isDebuggingOn())
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
      if (isDebuggingOn())
      {// shouldn't happen
         System.err.format(
           "Ignoring unexpected request to convert %s to %s%n",
           inFile.toString(), outFile.toString());
      }
   }

   /*
    *  TeXApp method used for progress updates for long actions,
    *  such as loading datatool files.
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
    *  TeXApp method used for obtaining a message from a given label.
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

   public String getMessageIfExists(String label)
   {
      if (messages == null) return null;
   
      String text = messages.getMessageIfExists(label);

      if (text == null && isDebuggingOn())
      {
         debug("No message for label '"+label+"'");
      }

      return text;
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

   public String getLocalisationText(String prefix, Locale locale, String suffix,
     String defaultValue)
   {
      if (locale != null)
      {
         String langTag = locale.toLanguageTag();

         String text = getMessageIfExists(String.format("%s.%s.%s",
            prefix, langTag, suffix));

         if (text != null)
         {
            return text;
         }

         String country = locale.getCountry();
         String language = locale.getLanguage();
   
         if (country != null && !country.isEmpty()
             && language != null && !language.isEmpty())
         {
            String tag = String.format("%s-%s", language, country);
   
            if (!tag.equals(langTag))
            {
               text = getMessageIfExists(String.format("%s.%s.%s",
                  prefix, tag, suffix));
         
               if (text != null)
               {
                  return text;
               }
            } 
         }

         if (language != null && !language.isEmpty() && !language.equals(langTag))
         {
            text = getMessageIfExists(String.format("%s.%s.%s",
               prefix, language, suffix));

            if (text != null)
            {
               return text;
            }
         }
      }

      return defaultValue;
   }

   public String getLocalisationText(String prefix, Locale locale, String suffix)
   {
      String text = getLocalisationTextIfExists(prefix, locale, suffix);

      if (text == null)
      {
         warning(String.format("Can't find message for label: %s.%s", prefix, suffix));
         text = String.format("%s.%s", prefix, suffix);
      }

      return text;
   }

   public String getLocalisationTextIfExists(String prefix, Locale locale, String suffix)
   {
      if (locale != null)
      {
         String text = getLocalisationText(prefix, locale, suffix, null);

         if (text != null)
         {
            return text;
         }
      }

      if (defaultLocale != null && !defaultLocale.equals(locale))
      {
         String text = getLocalisationText(prefix, defaultLocale, suffix, null);

         if (text != null)
         {
            return text;
         }
      }

      if (docLocale != null
            && (locale == null || !locale.toLanguageTag().equals(docLocale)))
      {
         String text = getMessageIfExists(String.format("%s.%s.%s",
            prefix, docLocale, suffix));

         if (text != null)
         {
            return text;
         }
      }

      return getMessageIfExists(String.format("%s.%s", prefix, suffix));
   }

   /*
    *  TeXApp method for providing informational messages to the use.
    */ 
   @Override
   public void message(String text)
   {
      if (!isSilent())
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

   public void verboseMessage(String key, Object... params)
   {
      verbose(getMessage(key, params));
   }

   public void verbose(String text)
   {
      if (isVerbose())
      {
         System.out.println(text);
      }

      logMessage(text);
   }

   public void verbose()
   {
      if (isVerbose())
      {
         System.out.println();
      }  

      logMessage();
   }     
         
   public void verbose(TeXParser parser, String message)
   {     
      if (isVerbose())
      {
         int lineNum = parser.getLineNumber();
         File file = parser.getCurrentFile();

         if (lineNum != -1 && file != null)
         {
            message = fileLineMessage(file, lineNum, message);
         }
   
         verbose(message);
      }
   }

   @Override
   public Charset getDefaultCharset()
   {
      return defaultCharset;
   }

   /*
    *  TeXApp method for providing warning messages.
    */ 
   @Override
   public void warning(TeXParser parser, String message)
   {
      if (!isSilent())
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
      if (!isSilent())
      {
         warning(fileLineMessage(file, line, message));
      }
   }

   public void warning(File file, int line, String message, Exception e)
   {
      if (!isSilent())
      {
         warning(fileLineMessage(file, line, message), e);
      }
   }

   public void warning(String message)
   {
      message = getMessageWithFallback("warning.title",
         "Warning: {0}", message);
   
      if (!isSilent())
      {    
         System.err.println(message);
      }

      logMessage(message);
   }

   public void warningMessage(String key, Object... params)
   {
      warning(getMessage(key, params));
   }

   public void warning()
   {
      if (!isSilent())
      {
         System.err.println();
      }

      logMessage();
   }

   public void warning(String message, Exception e)
   {
      if (!isSilent())
      {
         System.err.println(message);
      }

      logMessage(message);

      if (isDebuggingOn())
      {  
         e.printStackTrace();
   
         for (StackTraceElement elem : e.getStackTrace())
         {
            logMessage(elem.toString());
         }
      }
   }

   /*
    *  TeXApp method for providing error messages.
    */ 
   @Override
   public void error(Exception e)
   {
      if (e instanceof TeXSyntaxException)
      {
         warning(((TeXSyntaxException)e).getMessage(this));

         if (isDebuggingOn())
         {
            e.printStackTrace();

            for (StackTraceElement elem : e.getStackTrace())
            {
               logMessage(elem.toString());
            }
         }

      }
      else if (e instanceof NoSuchFileException)
      {
         error(getMessage(TeXSyntaxException.ERROR_FILE_NOT_FOUND,
          ((NoSuchFileException)e).getFile()));
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

      if (isDebuggingOn())
      {
         e.printStackTrace();

         for (StackTraceElement elem : e.getStackTrace())
         {
            logMessage(elem.toString());
         }
      }
   }

   public void error(String msg)
   {
      msg = getMessageWithFallback("error.title", "Error: {0}", msg);

      System.err.println(msg);
      logMessage(msg);
   }

   public String getLanguageFileName(String tag)
   {
      return String.format("/resources/bib2gls-%s.xml", tag);
   }

   protected Locale initMessageLocale(Locale locale)
   {
      return Locale.forLanguageTag(langTag);
   }

   protected void initMessages() throws Bib2GlsException,IOException
   {
      Locale locale = null;

      if (langTag == null || "".equals(langTag))
      {
         if (defaultLocale == null)
         {
            locale = Locale.getDefault();
         }
         else 
         {  
            locale = defaultLocale;
         }
      }
      else
      {
         locale = initMessageLocale(locale);
      }

      URL url = getLanguageUrl("bib2gls", locale, "en");

      if (url == null)
      {
         throw new Bib2GlsException("Can't find language resource file.");
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
            in = null;
         }
      }
   }

   protected String getLanguageFileName(String prefix, String tag)
   {   
      return String.format("/resources/%s-%s.xml", prefix, tag);
   } 

   protected URL getLanguageUrl(String prefix, Locale locale, String defaultTag)
   {        
      String lang = locale.toLanguageTag();
    
      String name = getLanguageFileName(prefix, lang);

      URL url = getClass().getResource(name);
       
      String jar = null;

      if (isDebuggingOn())
      {  
         jar = getClass().getProtectionDomain().getCodeSource().getLocation()
               .toString();
      }     

      if (url == null)
      {
         if (jar != null)
         {
            debug(String.format("Can't find language resource: %s!%s",
               jar, name));
         }

         lang = locale.getLanguage();

         name = getLanguageFileName(prefix, lang);

         debug("Trying: "+name);

         url = getClass().getResource(name);

         if (url == null)
         {
            debug(String.format("Can't find language resource: %s!%s",
                    jar, name));

            String script = locale.getScript();

            if (script != null && !script.isEmpty())
            {
               name = getLanguageFileName(prefix,
                  String.format("%s-%s", lang, script));

               debug("Trying: "+name);

               url = getClass().getResource(name);

               if (url == null && defaultTag != null && !lang.equals(defaultTag))
               {
                  debug(String.format(
                    "Can't find language resource: %s!%s%nDefaulting to '%s'",
                    jar, name, defaultTag));

                  url = getClass().getResource(
                    getLanguageFileName(prefix, defaultTag));
               }
            }
            else if (defaultTag != null && !lang.equals(defaultTag))
            {
               if (isDebuggingOn())
               {
                  debug(String.format("Defaulting to '%s'", defaultTag));
               }

               url = getClass().getResource(
                  getLanguageFileName(prefix, defaultTag));
            }
         }
      }

      return url;
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
      if (!shownVersion)
      {
         System.out.println(getMessageWithFallback("about.version",
           "{0} version {1} ({2})", getApplicationName(), VERSION, DATE));
         shownVersion = true;
      }
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
         if (args[i].equals("--locale") || args[i].equals("-l"))
         {
            if (i == args.length-1)
            {
               throw new Bib2GlsSyntaxException(
               "Missing <lang tag> after "+args[i]);
            }

            langTag = args[++i];
         }
         else if (args[i].startsWith("--locale="))
         {
            String[] split = args[i].split("=", 2);

            langTag = split[1];
         }
         else if (args[i].startsWith("--debug="))
         {
            String[] split = args[i].split("=", 2);

            try
            {
               debugLevel = Integer.parseInt(split[1]);

               if (debugLevel > 0)
               {
                  verboseLevel = DEBUG;
               }
               else
               {
                  verboseLevel = NORMAL;
               }
            }
            catch (NumberFormatException e)
            {
               throw new Bib2GlsSyntaxException(
                 "Invalid debug level '"+split[1]+"'", e);
            }
         }
         else if (args[i].equals("--debug"))
         {
            verboseLevel = DEBUG;

            if (i + 1 < args.length)
            {
               // optional numeric argument

               try
               {
                  debugLevel = Integer.parseInt(args[i+1]);
                  i++;

                  if (debugLevel == 0)
                  {
                     verboseLevel = NORMAL;
                  }
               }
               catch (NumberFormatException e)
               {
               }
            }
         }
         else if (args[i].equals("--no-debug") || args[i].equals("--nodebug"))
         {
            debugLevel = 0;
            verboseLevel = NORMAL;
         }
         else if (args[i].equals("--silent")
                 || args[i].equals("--quiet")
                 || args[i].equals("-q"))
         {
            debugLevel = 0;
            verboseLevel = SILENT;
         }
         else if (args[i].equals("--verbose"))
         {
            verboseLevel = VERBOSE;
         }
         else if (args[i].equals("--no-verbose")
               || args[i].equals("--noverbose"))
         {
            verboseLevel = NORMAL;
         }
         else
         {
            deque.add(args[i]);

            if (args[i].startsWith("-"))
            {
               if (args[i].equals("--debug-mode"))
               {
                  verboseLevel = DEBUG;
               }

               String[] split = args[i].split("=", 2);

               int n = argCount(split[0]);

               if (n == -1)
               {
                  if (i + 1 < args.length && !args[i+1].startsWith("-"))
                  {
                     n = 1;
                  }
                  else
                  {
                     n = 0;
                  }
               }

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

   /**
    * Gets the number of required arguments for a command line
    * switch. This should return the number or -1 for a single
    * optional argument (which should not start with "-").
    * @param arg switch
    * @return number of required arguments or -1 for a single
    * optional argument
    */ 
   protected int argCount(String arg)
   {
      if (arg.equals("--debug-mode")
         || arg.equals("--default-encoding")
         )
      {
         return 1;
      }

      return 0;
   }

   protected boolean isArg(ArrayDeque<String> deque, String arg,
     String longName, BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException
   {
      return isArg(deque, arg, null, longName, returnVals, BibGlsArgValueType.STRING);
   }

   protected boolean isArg(ArrayDeque<String> deque, String arg,
     String longName, BibGlsArgValue[] returnVals,
     BibGlsArgValueType type)
    throws Bib2GlsSyntaxException
   {
      return isArg(deque, arg, null, longName, returnVals, type);
   }

   protected boolean isArg(ArrayDeque<String> deque, String arg,
     String shortName, String longName, BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException
   {
      return isArg(deque, arg, shortName, longName, returnVals,
         BibGlsArgValueType.STRING);
   }

   protected boolean isIntArg(ArrayDeque<String> deque, String arg,
     String longName, BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException
   {
      return isArg(deque, arg, null, longName, returnVals,
         BibGlsArgValueType.INT);
   }

   protected boolean isIntArg(ArrayDeque<String> deque, String arg,
     String shortName, String longName, BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException
   {
      return isArg(deque, arg, shortName, longName, returnVals,
         BibGlsArgValueType.INT);
   }

   protected boolean isListArg(ArrayDeque<String> deque, String arg,
     String longName, BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException
   {
      return isArg(deque, arg, null, longName, returnVals,
         BibGlsArgValueType.LIST);
   }

   protected boolean isListArg(ArrayDeque<String> deque, String arg,
     String shortName, String longName, BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException
   {
      return isArg(deque, arg, shortName, longName, returnVals,
         BibGlsArgValueType.LIST);
   }

   protected boolean isArg(ArrayDeque<String> deque, String arg,
     String shortName, String longName, BibGlsArgValue[] returnVals,
     BibGlsArgValueType type)
    throws Bib2GlsSyntaxException
   {
      String[] split = arg.split("=", 2);
      String argName = split[0];

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
            if (n == -1)
            {
               String val = deque.peekFirst();

               if (val != null && !val.startsWith("-"))
               {
                  returnVals[0] = BibGlsArgValue.create(this, argName, deque.poll(), type);
               }
               else
               {
                  returnVals[0] = null;
               }
            }
            else
            {
               returnVals[0] = BibGlsArgValue.create(this, argName, deque.poll(), type);
            }
         }
         else
         {
            returnVals[0] = BibGlsArgValue.create(this, argName, split[1], type);
         }
      }
      else if (shortName != null && arg.equals(shortName))
      {
         argName = shortName;

         n = argCount(shortName);

         if (n == 0)
         {
            returnVals[0] = null;
         }
         else if (n == -1)
         {
            String val = deque.peekFirst();

            if (val != null && !val.startsWith("-"))
            {
               returnVals[0] = BibGlsArgValue.create(this, argName, deque.poll(), type);
            }
            else
            {
               returnVals[0] = null;
            }
         }
         else
         {
            returnVals[0] = BibGlsArgValue.create(this, argName, deque.poll(), type);
         }
      }
      else
      {
         return false;
      }

      for (int i = 1; i < n; i++)
      {
         returnVals[0] = BibGlsArgValue.create(this, argName, deque.poll(), type);
      }

      return true;
   }

   protected boolean isIntArg(ArrayDeque<String> deque, String arg,
     String longName, BibGlsArgValue[] returnVals, int defValue)
    throws Bib2GlsSyntaxException
   {
      return isIntArg(deque, arg, null, longName, returnVals, defValue);
   }

   /**
    * Test for switch that takes a single optional integer argument.
    */ 
   protected boolean isIntArg(ArrayDeque<String> deque, String arg,
     String shortName, String longName, BibGlsArgValue[] returnVals,
     int defValue)
    throws Bib2GlsSyntaxException
   {
      String[] split = arg.split("=", 2);

      if (split[0].equals(longName))
      {
         if (split.length == 1)
         {
            String val = deque.peekFirst();

            if (val == null)
            {
               returnVals[0] = null;
            }
            else
            {
               try
               {
                  int i = Integer.parseInt(val);
                  returnVals[0] = new BibGlsArgValue(deque.poll(), i);
               }
               catch (NumberFormatException e)
               {
                  returnVals[0] = null;
               }
            }
         }
         else
         {
            returnVals[0] = BibGlsArgValue.create(this, split[0], split[1], 
              BibGlsArgValueType.INT);
         }
      }
      else if (shortName != null && arg.equals(shortName))
      {
         String val = deque.peekFirst();

         if (val == null)
         {
            returnVals[0] = null;
         }
         else
         {
            try
            {
               int i = Integer.parseInt(val);
               returnVals[0] = new BibGlsArgValue(deque.poll(), i);
            }
            catch (NumberFormatException e)
            {
               returnVals[0] = null;
            }
         }
      }
      else
      {
         return false;
      }

      return true;
   }


   /**
    * Parse command line word that doesn't starts with "-" if recognised.
    * @return true if successful or false if not valid 
    */
   protected abstract void parseArg(ArrayDeque<String> deque, String arg)
    throws Bib2GlsSyntaxException;

   /**
    * Parse command line argument that starts with "-" if recognised.
    * @return true if successful or false if not valid 
    */
   protected abstract boolean parseArg(ArrayDeque<String> deque, String arg,
      BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException;

   protected abstract void initSettings()
    throws Bib2GlsSyntaxException;

   protected abstract void postSettings()
    throws Bib2GlsSyntaxException,IOException;

   protected int maxArgParams()
   {
      return 1;
   }

   protected abstract void help();

   protected void parseArgs(ArrayDeque<String> deque)
    throws Bib2GlsSyntaxException,IOException
   {
      String arg;
      BibGlsArgValue[] returnVals = new BibGlsArgValue[maxArgParams()];

      initSettings();

      while ((arg = deque.poll()) != null)
      {
         if (arg.equals("--help") || arg.equals("-h"))
         {
            help();
            System.exit(0);
         }
         else if (arg.equals("--version") || arg.equals("-v"))
         {
            version();
            license();
            libraryVersion();
            System.exit(0);
         }
         else if (isListArg(deque, arg, "--debug-mode", returnVals))
         {        
            if (returnVals[0] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", arg));
            }

            setDebugLevel(returnVals[0].listValue());
         }
         else if (isArg(deque, arg, "--default-encoding", returnVals))
         {
            if (returnVals[0] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", arg));
            }

            try
            {
               defaultCharset = Charset.forName(arg);
            }
            catch (UnsupportedCharsetException e)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.unknown.charset", arg), e);
            }
         }
         else if (arg.startsWith("-"))
         {
            if (!parseArg(deque, arg, returnVals))
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("common.unknown.arg",
                  arg, "--help"));
            }
         }
         else
         {
            parseArg(deque, arg);
         }
      }

      postSettings();
   }

   protected void furtherInfo()
   {
      System.out.println(getMessage("syntax.tutorial", "bib2gls", "texdoc bib2gls-begin"));
      System.out.println(getMessage("syntax.userguide", "bib2gls", "texdoc bib2gls"));
      System.out.println(getMessage("syntax.ctan", "bib2gls",
         "https://ctan.org/pkg/bib2gls"));
      System.out.println(getMessage("syntax.home", "bib2gls",
        "https://www.dickimaw-books.com/software/bib2gls/"));
      System.out.println(getMessage("syntax.faq", "bib2gls",
        "https://www.dickimaw-books.com/faq.php?category=bib2gls"));
   }

   protected Charset defaultCharset = Charset.defaultCharset();
   protected Locale defaultLocale = null;

   protected Bib2GlsMessages messages;

   public static final int SILENT=-1, NORMAL=0, VERBOSE=1, DEBUG=2;

   protected String langTag = null;
   protected int debugLevel = 0; // TeX Parser verbosity
   protected int verboseLevel=NORMAL; // bib2gls verbosity
   protected boolean shownVersion = false;
   protected String docLocale = null;

   protected File transcriptFile = null;
   protected PrintWriter logWriter = null;

   protected int exitCode = 0;

   public static final int SYNTAX_ITEM_LINEWIDTH=78;
   public static final int SYNTAX_ITEM_TAB=30;

   public static final String VERSION = "3.9.20240401";
   public static final String DATE = "2024-04-01";

}
