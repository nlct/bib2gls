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
import java.text.MessageFormat;
import java.text.BreakIterator;
import java.io.*;

import java.net.URL;

import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.primitives.Undefined;
import com.dickimawbooks.texparserlib.generic.UndefinedActiveChar;
import com.dickimawbooks.texparserlib.latex.LaTeXParserListener;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.NewCommand;
import com.dickimawbooks.texparserlib.latex.NewDocumentCommand;
import com.dickimawbooks.texparserlib.latex.Overwrite;

public abstract class BibGlsCommon extends BibGlsTeXApp
{
   protected void initialise(String[] args)
    throws Bib2GlsException,IOException
   {
      super.initialise(args);

      listener = new BibGlsCommonListener(this, preambleOnly);
      parser = new TeXParser(listener);
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

   protected void addPredefinedCommands()
   {
      // don't complain about redefining unknown commands
      parser.putControlSequence(new NewCommand("renewcommand",
        Overwrite.ALLOW));

      parser.putControlSequence(new NewDocumentCommand(
      "RenewDocumentCommand", Overwrite.ALLOW));

   }

   public boolean newcommandOverride(boolean isRobust, Overwrite overwrite,
     String type, String csName, boolean isShort,
     int numParams, TeXObject defValue, TeXObject definition)
   throws IOException
   {  
      return false;
   }

   @Override
   protected int argCount(String arg)
   {
      if (arg.equals("--texenc")
       || arg.equals("--bibenc")
       || arg.equals("--space-sub")
       || arg.equals("-s")
       || arg.equals("--ignore-fields")
       || arg.equals("-f"))
      {
         return 1;
      }

      return 0;
   }

   @Override
   protected void parseArgs(ArrayDeque<String> deque)
    throws Bib2GlsSyntaxException
   {
      String arg;
      String[] returnVals = new String[2];

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
         else if (isArg(deque, arg, "--texenc", returnVals))
         {
            if (returnVals[0] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("common.missing.encoding.value",
                  arg));
            }

            charset = Charset.forName(returnVals[0]);
         }
         else if (isArg(deque, arg, "--bibenc", returnVals))
         {
            if (returnVals[0] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("common.missing.encoding.value",
                  arg));
            }

            bibCharsetName = returnVals[0];
         }
         else if (isArg(deque, arg, "--space-sub", "-s", returnVals))
         {
            if (returnVals[0] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("common.missing.arg.value",
                  arg));
            }

            spaceSub = returnVals[0];

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
         else if (isArg(deque, arg, "--ignore-fields", "-f", returnVals))
         {
            if (returnVals[0] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("common.missing.arg.value",
                  arg));
            }

            String list = returnVals[0].trim();

            if (list.isEmpty())
            {
               customIgnoreFields = null;
            }
            else
            {
               customIgnoreFields = list.split(" *, *");
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
         else if (arg.startsWith("-"))
         {
            if (!parseArg(deque, arg, returnVals))
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("common.unknown.arg",
                  arg, "--help"));
            }
         }
         else if (texFile == null)
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
         bibCharsetName = charset.name();
      }
   }

   public boolean isCustomIgnoreField(String field)
   {
      if (customIgnoreFields == null)
      {
         return false;
      }

      for (String f : customIgnoreFields)
      {
         if (f.equals(field))
         {
            return true;
         }
      }

      return false;
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
   }

   protected void otherHelp()
   {
   }

   protected abstract void syntaxInfo();

   public void help()
   {
      System.out.println(getMessage("common.syntax", getApplicationName()));

      System.out.println();
      syntaxInfo();

      System.out.println();
      System.out.println(getMessage("common.syntax.options.general"));
      System.out.println();

      printSyntaxItem(getMessage("common.syntax.version", "--version",
       "-v"));
      printSyntaxItem(getMessage("common.syntax.help", "--help", "-h"));
      printSyntaxItem(getMessage("common.syntax.silent",
        "--silent", "-q", "--quiet"));
      printSyntaxItem(getMessage("common.syntax.verbose",
        "--verbose"));
      printSyntaxItem(getMessage("common.syntax.debug",
        "--debug"));
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
         System.exit(1);
      }
      catch (Bib2GlsException e)
      {
         System.err.println(e.getMessage());
         System.exit(3);
      }
      catch (IOException e)
      {
         System.err.println(e.getMessage());
         System.exit(2);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         System.exit(4);
      }
   }

   protected File texFile=null, bibFile=null;

   protected String bibCharsetName=null;

   protected boolean overwriteFiles=true;
   protected boolean preambleOnly=false;

   protected String spaceSub = null;

   protected String[] customIgnoreFields = null;

   protected TeXParser parser;
   protected BibGlsCommonListener listener;

}
