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
 * Common listener for conversion applications.
 */

import java.util.Properties;
import java.util.Locale;
import java.util.ArrayDeque;
import java.util.Vector;
import java.util.HashMap;
import java.text.MessageFormat;
import java.text.BreakIterator;
import java.io.*;

import java.net.URL;

import java.nio.charset.Charset;
import java.nio.file.Files;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.primitives.Undefined;
import com.dickimawbooks.texparserlib.generic.UndefinedActiveChar;
import com.dickimawbooks.texparserlib.latex.LaTeXParserListener;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.NewCommand;
import com.dickimawbooks.texparserlib.latex.latex3.NewDocumentCommand;
import com.dickimawbooks.texparserlib.latex.Overwrite;

public abstract class BibGlsConverter extends BibGlsTeXApp
{
   @Override
   protected void initialise(String[] args)
    throws Bib2GlsException,IOException
   {
      super.initialise(args);

      listener = new BibGlsConverterListener(this, preambleOnly);
      parser = new TeXParser(listener);
      parser.setDebugMode(debugLevel);

      if (debugLevel > 0 && logName == null)
      {
         logName = getApplicationName()+".log";
      }

      initTranscript();

      if (logWriter != null)
      {
         parser.setLogWriter(logWriter);
         parser.setLogging(true);
      }
   }

   @Override
   public String kpsewhich(String arg)
   {
      return null;
   }

   public TeXObjectList createString(String str)
   {
      return listener.createString(str);
   }

   public TeXParser getParser()
   {
      return parser;
   }

   public Charset getCharSet()
   {
      return charset == null ? getDefaultCharset() : charset;
   }

   public BibGlsConverterListener getListener()
   {
      return listener;
   }

   protected void addPredefinedCommands(TeXParser parser)
   {
      // don't complain about redefining unknown commands
      parser.putControlSequence(new NewCommand("renewcommand",
        Overwrite.ALLOW));

      parser.putControlSequence(new NewDocumentCommand(
      "RenewDocumentCommand", Overwrite.ALLOW));

      parser.putControlSequence(new AtFirstOfTwo("IfNotBibGls"));
   }

   public boolean newcommandOverride(boolean isRobust, Overwrite overwrite,
     String type, String csName, boolean isShort,
     int numParams, TeXObject defValue, TeXObject definition)
   throws IOException
   {  
      return false;
   }

   public boolean isSpecialUsePackage(KeyValList options, String styName,
     boolean loadParentOptions, TeXObjectList stack)
   throws IOException
   {
      return false;
   }

   public String processLabel(String label)
   {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < label.length(); )
      {
         int cp = label.codePointAt(i);
         i += Character.charCount(cp);

         if (Character.isWhitespace(cp)
          || Character.isSpaceChar(cp) // include nbsp
            )
         {
            if (spaceSub != null)
            {
               builder.append(spaceSub);
            }
         }
         else if (Character.isISOControl(cp)
                || cp == ',' || cp == '=' || cp == '{' || cp == '}'
                || cp == '$' || cp == '\\' || cp == '^' || cp == '_'
                || cp == '%' || cp == '#' || cp == '&' || cp == '~'
                || cp == '"' || cp == '`' || cp == '\''
                || cp == '(' || cp == ')' || cp == '[' || cp == ']'
            )
         {// skip
         }
         else
         {
            builder.appendCodePoint(cp);
         }
      }

      return builder.toString();
   }

   protected boolean isIgnoredPackage(String styName)
   {
      return false;
   }

   public boolean isIndexConversionOn()
   {
      return noDescEntryToIndex;
   }

   @Override
   protected int argCount(String arg)
   {
      if (arg.equals("--texenc")
       || arg.equals("--bibenc")
       || arg.equals("--space-sub") || arg.equals("-s")
       || arg.equals("--ignore-fields") || arg.equals("-f")
       || arg.equals("--log-file")
       || arg.equals("--field-map") || arg.equals("--key-map") || arg.equals("-m")
       )
      {
         return 1;
      }
      else
      {
         return super.argCount(arg);
      }
   }

   @Override
   protected boolean parseArg(ArrayDeque<String> deque, String arg,
      BibGlsArgValue[] returnVals)
    throws Bib2GlsSyntaxException
   {
      if (isArg(deque, arg, "--texenc", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.encoding.value",
               arg));
         }

         charset = Charset.forName(returnVals[0].toString());
      }
      else if (isArg(deque, arg, "--bibenc", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.encoding.value",
               arg));
         }

         bibCharsetName = returnVals[0].toString();
      }
      else if (isArg(deque, arg, "--log-file", returnVals))
      { 
         if (returnVals[0] == null)
         { 
            throw new Bib2GlsSyntaxException(
               getMessage("common.error.missing.value", arg));
         } 
        
         logName = returnVals[0].toString().trim();

         if (logName.isEmpty())
         {
            logName = null;
         }
      }
      else if (isArg(deque, arg, "-s", "--space-sub", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         spaceSub = returnVals[0].toString();

         if (" ".equals(spaceSub))
         {
            spaceSub = null;
         }
      }
      else if (arg.equals("--overwrite"))
      {
         overwriteFiles = true;
      }
      else if (arg.equals("--no-overwrite"))
      {
         overwriteFiles = false;
      }
      else if (arg.equals("--no-ignore-fields"))
      {
         customIgnoreFields = null;
      }
      else if (isListArg(deque, arg, "-f", "--ignore-fields", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         if (!returnVals[0].toString().isEmpty())
         {
            addCustomIgnoreField(returnVals[0].listValue());
         }
      }
      else if (arg.equals("--no-field-map") || arg.equals("--no-key-map"))
      {
         keyToFieldMap = null;
      }
      // --key-map deprecated synonym
      else if (isArg(deque, arg, "-m", "--field-map", "--key-map", returnVals,
               BibGlsArgValueType.LIST))
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
      else if (isArg(deque, arg, "--field-case", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         String val = returnVals[0].toString().trim();

         if (val.equals("none"))
         {
            fieldCaseChange = CaseChange.NO_CHANGE;
         }
         else if (val.equals("lc"))
         {
            fieldCaseChange = CaseChange.TO_LOWER;
         }
         else if (val.equals("uc"))
         {
            fieldCaseChange = CaseChange.TO_UPPER;
         }
         else if (val.equals("title"))
         {
            fieldCaseChange = CaseChange.TITLE;
         }
         else if (val.equals("sentence"))
         {
            fieldCaseChange = CaseChange.SENTENCE;
         }
         else
         {
            throw new Bib2GlsSyntaxException(
               getMessage("error.invalid.choice.value",
               arg, val, "none, lc, uc, title, sentence"));
         }
      }
      else if (arg.equals("--preamble-only") || arg.equals("-p"))
      {
         preambleOnly = true;
      }
      else if (arg.equals("--no-preamble-only"))
      {
         preambleOnly = false;
      }
      else if (arg.equals("--index-conversion") || arg.equals("-i"))
      {
         noDescEntryToIndex = true;
      }
      else if (arg.equals("--no-index-conversion"))
      {
         noDescEntryToIndex = false;
      }
      else
      {
         return false;
      }

      return true;
   }

   protected void parseArg(ArrayDeque<String> deque, String arg)
    throws Bib2GlsSyntaxException
   {
      if (texFile == null)
      {
         texFile = new File(arg);
      }
      else if (bibFile == null)
      {
         if (arg.toLowerCase().endsWith(".bib"))
         {
            bibFile = new File(arg);
         }
         else
         {
            bibFile = new File(arg+".bib");
         }
      }
      else
      {
         throw new Bib2GlsSyntaxException(
            getMessage("common.toomany.arg", "--help"));
      }
   }

   @Override
   protected void initSettings()
    throws Bib2GlsSyntaxException
   {
   }

   @Override
   protected void postSettings()
    throws Bib2GlsSyntaxException
   {
      if (texFile == null)
      {
          throw new Bib2GlsSyntaxException(
           getMessage("common.missing.tex.arg",
              getMessage("common.syntax", getApplicationName()), "--help"));
      }

      if (bibFile == null)
      {
          throw new Bib2GlsSyntaxException(
            getMessage("common.missing.bib.arg",
              getMessage("common.syntax", getApplicationName()), "--help"));
      }


      if (bibCharsetName == null)
      {
         bibCharsetName = defaultCharset.name();
      }
   }

   public void addCustomIgnoreField(String... fields)
   {
      if (customIgnoreFields == null)
      {
         customIgnoreFields = new Vector<String>();
      }

      for (String f : fields)
      {
         if (!f.isEmpty())
         {
            customIgnoreFields.add(f.toLowerCase());
         }
      }
   }

   public boolean isCustomIgnoreField(String field)
   {
      if (customIgnoreFields == null)
      {
         return false;
      }

      return customIgnoreFields.contains(field.toLowerCase());
   }

   public void addKeyToFieldMap(String key, String field)
   {
      if (keyToFieldMap == null)
      {
         keyToFieldMap = new HashMap<String,String>();
      }

      keyToFieldMap.put(key, field);
   }

   public String getFieldMap(String key)
   {
      if (keyToFieldMap == null)
      {
         return null;
      }

      return keyToFieldMap.get(key);
   } 

   /**
    * Gets the bib field name from the input source label.
    * This will first apply any mapping and then convert to
    * lowercase. Returns null if this field should be omitted.
    */ 
   public String getFieldName(String originalLabel)
   {
      String field = originalLabel;

      if (isCustomIgnoreField(originalLabel))
      {
         field = null;
      }
      else
      {
         if (keyToFieldMap != null)
         {
            String val = keyToFieldMap.get(originalLabel);

            if (val != null)
            {
               field = val;
            }
         }

         if (field.isEmpty())
         {
            if (originalLabel.isEmpty())
            {
               warningMessage("common.empty_field_name");
            }
            else
            {
               warningMessage("common.empty_field_name.changed",
                 originalLabel, "field-map");
            }

            field = null;
         }
      }

      if (field == null)
      {
         return null;
      }
      else
      {
         String mapped = processLabel(applyFieldCase(field));

         if (mapped.isEmpty())
         {
            if (field.equals(originalLabel))
            {
               warningMessage("common.empty_field_name.changed",
                 originalLabel, "--field-map");
            }
            else
            {
               warningMessage("common.empty_field_name.mapped_changed",
                 originalLabel, field, "--field-map");
            }

            return null;
         }

         return mapped;
      }
   }

   protected String applyFieldCase(String field)
   {
      switch (fieldCaseChange)
      {
         case TO_LOWER: return field.toLowerCase();
         case TO_UPPER: return field.toUpperCase();
         case TITLE:
           // spaces not permitted so equivalent
         case SENTENCE:
           // no leading punctuation to worry about
           StringBuilder builder = new StringBuilder(field.length());
           int cp = field.codePointAt(0);
           builder.appendCodePoint(Character.toTitleCase(cp));
           builder.append(field, Character.charCount(cp), field.length());
         return builder.toString();
      }

      return field;
   }

   protected void localeHelp()
   {
      printSyntaxItem(getMessage("common.syntax.texenc", "--texenc"));
      printSyntaxItem(getMessage("common.syntax.bibenc", "--bibenc"));
      printSyntaxItem(getMessage("common.syntax.locale",
        "--locale"));
      System.out.println();
   }

   protected void filterHelp()
   {
      printSyntaxItem(getMessage("common.syntax.preamble-only",
        "--[no-]preamble-only", "-p"));

      printSyntaxItem(getMessage("common.syntax.ignore-fields",
        "--[no-]ignore-fields", "-f"));
   }

   protected void ioHelp()
   {
      printSyntaxItem(getMessage("common.syntax.overwrite",
        "--[no-]overwrite"));
   }

   protected void adjustHelp()
   {
      printSyntaxItem(getMessage("common.syntax.space-sub",
        "--space-sub", "-s"));
      printSyntaxItem(getMessage("common.syntax.index-conversion",
        "--[no-]index-conversion", "-i"));
      printSyntaxItem(getMessage("common.syntax.field-map",
        "--[no-]field-map", "-m"));
      printSyntaxItem(getMessage("common.syntax.field-case", "--field-case"));
   }

   protected void otherHelp()
   {
   }

   protected abstract void syntaxInfo();

   @Override
   public void help()
   {
      System.out.println(getMessage("common.syntax", getApplicationName()));

      System.out.println();
      syntaxInfo();

      System.out.println();
      System.out.println(getMessage("common.syntax.options.general"));
      System.out.println();

      commonHelp();

      printSyntaxItem(getMessage("common.syntax.log-file", "--log-file"));

      System.out.println();

      printSyntaxItem(getMessage("common.syntax.options.locale"));
      System.out.println();

      localeHelp();

      System.out.println(getMessage("common.syntax.options.filter"));
      System.out.println();

      filterHelp();

      System.out.println();

      System.out.println(getMessage("common.syntax.options.io"));
      System.out.println();

      ioHelp();

      System.out.println();
      System.out.println(getMessage("common.syntax.options.adjust"));
      System.out.println();

      adjustHelp();

      otherHelp();

      System.out.println();
      System.out.println(getMessage("syntax.furtherinfo"));
      System.out.println();

      furtherInfo();

   }

   public abstract void process() throws IOException,Bib2GlsException;

   public void run(String[] args)
   {
      try
      {
         initialise(args);
         process();
      }
      catch (Bib2GlsSyntaxException e)
      {
         System.err.println(e.getMessage());
         exitCode = 1;
      }
      catch (Bib2GlsException e)
      {
         System.err.println(e.getMessage());
         exitCode = 3;
      }
      catch (IOException e)
      {
         System.err.println(e.getMessage());
         exitCode = 2;
      }
      catch (Exception e)
      {
         e.printStackTrace();
         exitCode = 4;
      }

      exit();
   }

   protected File texFile=null, bibFile=null;

   protected String bibCharsetName=null;
   protected Charset charset;

   protected boolean overwriteFiles=true;
   protected boolean preambleOnly=false;
   protected boolean noDescEntryToIndex=false;

   protected String spaceSub = null;

   protected Vector<String> customIgnoreFields = null;
   protected HashMap<String,String> keyToFieldMap = null;
   protected CaseChange fieldCaseChange = CaseChange.TO_LOWER;

   protected TeXParser parser;
   protected BibGlsConverterListener listener;
}
