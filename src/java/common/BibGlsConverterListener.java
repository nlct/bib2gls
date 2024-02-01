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

public class BibGlsConverterListener extends LaTeXParserListener
  implements Writeable
{
   public BibGlsConverterListener(BibGlsConverter texApp, boolean preambleOnly)
   {
      super(null);

      this.texApp = texApp;
      this.preambleOnly = preambleOnly;

      setWriteable(this);
   }

   @Override
   public TeXApp getTeXApp()
   {
      return texApp;
   }

   @Override
   public void substituting(String original, String replacement)
   {
      texApp.substituting(getParser(), original, replacement);
   }

   @Override
   protected void addPredefined()
   {
      super.addPredefined();

      texApp.addPredefinedCommands();
    }

   @Override
   public void newcommand(boolean isRobust, Overwrite overwrite,
     String type, String csName, boolean isShort,
     int numParams, TeXObject defValue, TeXObject definition)
   throws IOException
   {  
      if (texApp.newcommandOverride(isRobust, overwrite, type, csName, isShort,
           numParams, defValue, definition))
      {
         return; 
      }
      
      super.newcommand(isRobust, overwrite, type, csName, isShort,
        numParams, defValue, definition);
   }


   // Ignore unknown control sequences
   @Override
   public ControlSequence createUndefinedCs(String name)
   {
      return new Undefined(name,
       texApp.isSilent() ? UndefAction.IGNORE: UndefAction.WARN);
   }

   @Override
   public ActiveChar getUndefinedActiveChar(int charCode)
   {
      return new UndefinedActiveChar(charCode,
       texApp.isSilent() ? UndefAction.IGNORE: UndefAction.WARN);
   }

   @Override
   public void beginDocument(TeXObjectList stack)
     throws IOException
   {
      super.beginDocument(stack);

      if (preambleOnly)
      {
         endDocument(stack);
      }
   }


   // No write performed by parser (just gathering information)
   @Override
   public void write(String text)
     throws IOException
   {
   }

   @Override
   public void writeln(String text)
     throws IOException
   {
   }

   @Override
   public void writeliteralln(String text)
     throws IOException
   {
   }

   @Override
   public void writeliteral(String text)
     throws IOException
   {
   }

   @Override
   public void write(char c)
     throws IOException
   {
   }

   @Override
   public void writeCodePoint(int codePoint)
     throws IOException
   {
   }

   @Override
   public void overwithdelims(TeXObject firstDelim,
     TeXObject secondDelim, TeXObject before, TeXObject after)
    throws IOException
   {
      texApp.debug("Ignoring \\overwithdelims");
   }

   @Override
   public void abovewithdelims(TeXObject firstDelim,
     TeXObject secondDelim, TeXDimension thickness, TeXObject before,
     TeXObject after)
    throws IOException
   {
      texApp.debug("Ignoring \\abovewithdelims");
   }

   @Override
   public void skipping(Ignoreable ignoreable)
      throws IOException
   {
   }

   @Override
   public void href(String url, TeXObject text)
      throws IOException
   {
      texApp.debug("Ignoring \\href");
   }

   @Override
   public void subscript(TeXObject arg)
     throws IOException
   {
      texApp.debug("Ignoring _");
   }

   @Override
   public void superscript(TeXObject arg)
     throws IOException
   {
      texApp.debug("Ignoring ^");
   }

   @Override
   public void includegraphics(TeXObjectList stack, KeyValList options, String imgName)
     throws IOException
   {
      texApp.debug("Ignoring \\includegraphics");
   }

   @Override
   public void endParse(File file)
      throws IOException
   {
   }

   @Override
   public void beginParse(File file, Charset charset)
      throws IOException
   {
      texApp.message(
        texApp.getMessage("message.reading", file));

      if (charset == null)
      {
         texApp.message(
            texApp.getMessage("message.default.charset", 
            texApp.getDefaultCharset()));
      }
      else
      {
         texApp.message(
           texApp.getMessage("message.tex.charset", charset));
      }
   }

   // shouldn't be needed here
   @Override
   public float emToPt(float emValue)
   {
      texApp.warning(getParser(),
         "Can't convert from em to pt, no font information loaded");

      return 9.5f*emValue;
   }

   // shouldn't be needed here
   @Override
   public float exToPt(float exValue)
   {
      texApp.warning(getParser(),
         "Can't convert from ex to pt, no font information loaded");

      return 4.4f*exValue;
   }

   protected boolean preambleOnly;
   protected BibGlsConverter texApp;
}
