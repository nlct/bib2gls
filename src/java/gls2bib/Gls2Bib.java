/*
    Copyright (C) 2017-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.gls2bib;

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
import java.util.ArrayDeque;
import java.text.MessageFormat;
import java.text.BreakIterator;
import java.io.*;

import java.net.URL;

import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.primitives.Relax;
import com.dickimawbooks.texparserlib.primitives.Undefined;
import com.dickimawbooks.texparserlib.generic.UndefinedActiveChar;
import com.dickimawbooks.texparserlib.latex.LaTeXParserListener;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.NewCommand;
import com.dickimawbooks.texparserlib.latex.latex3.NewDocumentCommand;
import com.dickimawbooks.texparserlib.latex.Overwrite;
import com.dickimawbooks.texparserlib.latex.AtGobble;
import com.dickimawbooks.texparserlib.latex.GobbleOpt;
import com.dickimawbooks.texparserlib.latex.GobbleOptMandOpt;
import com.dickimawbooks.texparserlib.latex.glossaries.LoadGlsEntries;

import com.dickimawbooks.bibgls.common.*;

public class Gls2Bib extends BibGlsConverter
{
   @Override
   protected void initialise(String[] args)
    throws Bib2GlsException,IOException
   {
      super.initialise(args);

      if (customIgnoreFields != null)
      {
         for (String f : customIgnoreFields)
         {
            if (f.equals("sort"))
            {
               ignoreSortField = true;
            }
            else if (f.equals("type"))
            {
               ignoreTypeField = true;
            }
            else if (f.equals("category"))
            {
               ignoreCategoryField = true;
            }
         }
      }

      initInternalFieldMap();
   }

   private void initInternalFieldMap()
   {
      if (internalFieldMap == null)
      {
         internalFieldMap = new HashMap<String,String>();
      }

      internalFieldMap.put("sortvalue", "sort");
      internalFieldMap.put("firstpl", "firstplural");
      internalFieldMap.put("desc", "description");
      internalFieldMap.put("descplural", "descriptionplural");
      internalFieldMap.put("useri", "user1");
      internalFieldMap.put("userii", "user2");
      internalFieldMap.put("useriii", "user3");
      internalFieldMap.put("useriv", "user4");
      internalFieldMap.put("userv", "user5");
      internalFieldMap.put("uservi", "user6");
      internalFieldMap.put("longpl", "longplural");
      internalFieldMap.put("shortpl", "shortplural");
   }

   @Override
   public String getFieldName(String originalLabel)
   {
      if (ignoreSortField && originalLabel.equals("sort"))
      {
         return null;
      }

      if (ignoreTypeField && originalLabel.equals("type"))
      {
         return null;
      }

      if (ignoreCategoryField && originalLabel.equals("category"))
      {
         return null;
      }

      return super.getFieldName(originalLabel);
   }

   public boolean isAbsorbSeeOn()
   {
      return absorbSee;
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

   public boolean ignoreCategory()
   {
      return ignoreCategoryField;
   }

   public boolean isSplitCategoryOn()
   {
      return splitOnCategory;
   }

   @Override
   protected void addPredefinedCommands(TeXParser parser)
   {
      super.addPredefinedCommands(parser);

      parser.putControlSequence(
        new GenericCommand("glslongkey", null, createString("long")));
      parser.putControlSequence(
        new GenericCommand("glslongpluralkey", null, createString("longplural")));
      parser.putControlSequence(
        new GenericCommand("glsshortkey", null, createString("short")));
      parser.putControlSequence(
        new GenericCommand("glsshortpluralkey", null, createString("shortplural")));

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
      parser.putControlSequence(new OldAcronym(this));
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

      parser.putControlSequence(new LoadGlsEntries());

      // ignore common glossary preamble commands

      parser.putControlSequence(new AtGobble("setupglossaries"));
      parser.putControlSequence(new AtGobble("glossariesextrasetup"));
      parser.putControlSequence(new GobbleOpt("makeglossaries"));
      parser.putControlSequence(new Relax("makenoidxglossaries"));
      parser.putControlSequence(new GobbleOpt("GlsXtrLoadResources"));
      parser.putControlSequence(new Relax("noist"));

      parser.putControlSequence(new NewGlossary());
      parser.putControlSequence(new NewGlossary("altnewglossary", 
       NewGlossary.ALT));
      parser.putControlSequence(new NewGlossary("newignoredglossary", 
       NewGlossary.IGNORED));
      parser.putControlSequence(new NewGlossary("provideignoredglossary", 
       NewGlossary.IGNORED));
      parser.putControlSequence(new GobbleOpt("GlsSetXdyLanguage", 1, 1));
      parser.putControlSequence(new AtGobble("GlsSetXdyCodePage"));
      parser.putControlSequence(new AtGobble("GlsAddXdyCounters"));
      parser.putControlSequence(new AtGobble("GlsAddXdyAttribute"));
      parser.putControlSequence(new GobbleOpt("GlsAddXdyLocation", 1, 2)); 
      parser.putControlSequence(new AtGobble("GlsSetXdyLocationClassOrder"));
      parser.putControlSequence(new AtGobble("GlsSetXdyMinRangeLength"));
      parser.putControlSequence(new AtGobble("GlsSetXdyFirstLetterAfterDigits"));
      parser.putControlSequence(new AtGobble("GlsSetXdyNumberGroupOrder"));
      parser.putControlSequence(new AtGobble("GlsAddXdyStyle"));

      parser.putControlSequence(new GobbleOpt("setabbreviationstyle", 1, 1)); 
      parser.putControlSequence(new AtGobble("setacronymstyle")); 

      parser.putControlSequence(new AtGobble("glssetcategoryattribute", 3));
      parser.putControlSequence(new AtGobble("newabbreviationstyle", 3));
      parser.putControlSequence(new AtGobble("newacronymstyle", 3));

      parser.putControlSequence(new AtGobble("setlength", 2));
      parser.putControlSequence(new AtGobble("pagestyle"));

      parser.putControlSequence(new GobbleOpt("glssetwidest", 1, 1)); 
      parser.putControlSequence(new AtGobble("glsdefpostlink", 2)); 
      parser.putControlSequence(new AtGobble("glsdefpostname", 2)); 
      parser.putControlSequence(new AtGobble("glsdefpostdesc", 2)); 
      parser.putControlSequence(new GlsAddKey()); 
      parser.putControlSequence(new GlsAddKey("glsaddstoragekey", true)); 

      parser.putControlSequence(new GlsSee(this)); 
      parser.putControlSequence(new GlsSee(this, "glsxtrindexseealso")); 
      parser.putControlSequence(new GobbleOpt("glsadd", 1, 1)); 
      parser.putControlSequence(new GobbleOpt("glsaddall", 1, 0)); 

      // Don't bother with these if --preamble-only

      if (!preambleOnly)
      {
         parser.putControlSequence(new GobbleOptMandOpt("gls")); 
         parser.putControlSequence(new GobbleOptMandOpt("glspl")); 
         parser.putControlSequence(new GobbleOptMandOpt("Gls")); 
         parser.putControlSequence(new GobbleOptMandOpt("Glspl")); 
         parser.putControlSequence(new GobbleOptMandOpt("GLS")); 
         parser.putControlSequence(new GobbleOptMandOpt("GLSpl")); 

         for (String field : KNOWN_FIELDS)
         {
            parser.putControlSequence(new GobbleOptMandOpt("gls"+field)); 
            parser.putControlSequence(new GobbleOptMandOpt("Gls"+field)); 
            parser.putControlSequence(new GobbleOptMandOpt("GLS"+field)); 

            parser.putControlSequence(new AtGobble("glsentry"+field));
            parser.putControlSequence(new AtGobble("Glsentry"+field));

            parser.putControlSequence(new GobbleOpt("glsfmt"+field, 1, 1));
            parser.putControlSequence(new GobbleOpt("Glsfmt"+field, 1, 1));
            parser.putControlSequence(new GobbleOpt("GLSfmt"+field, 1, 1));
         }

         for (String field : ABBR_FIELDS)
         {
            parser.putControlSequence(new AtGobble("glsentry"+field));
            parser.putControlSequence(new AtGobble("Glsentry"+field));

            parser.putControlSequence(new GobbleOptMandOpt("acr"+field));
            parser.putControlSequence(new GobbleOptMandOpt("Acr"+field));
            parser.putControlSequence(new GobbleOptMandOpt("ACR"+field));

            parser.putControlSequence(new GobbleOptMandOpt("glsxtr"+field));
            parser.putControlSequence(new GobbleOptMandOpt("Glsxtr"+field));
            parser.putControlSequence(new GobbleOptMandOpt("GLSxtr"+field));

            parser.putControlSequence(new GobbleOpt("glsfmt"+field, 1, 1));
            parser.putControlSequence(new GobbleOpt("Glsfmt"+field, 1, 1));
            parser.putControlSequence(new GobbleOpt("GLSfmt"+field, 1, 1));
         }

         parser.putControlSequence(new GobbleOpt("glsdisp", 1, 2)); 
         parser.putControlSequence(new GobbleOpt("glslink", 1, 2)); 

         parser.putControlSequence(new GobbleOptMandOpt("cgls")); 
         parser.putControlSequence(new GobbleOptMandOpt("cglspl")); 
         parser.putControlSequence(new GobbleOptMandOpt("cGls")); 
         parser.putControlSequence(new GobbleOptMandOpt("cGlspl")); 
         parser.putControlSequence(new GobbleOptMandOpt("cGLS")); 
         parser.putControlSequence(new GobbleOptMandOpt("cGLSpl")); 

         parser.putControlSequence(new GobbleOptMandOpt("pgls")); 
         parser.putControlSequence(new GobbleOptMandOpt("pglspl")); 
         parser.putControlSequence(new GobbleOptMandOpt("Pgls")); 
         parser.putControlSequence(new GobbleOptMandOpt("Pglspl")); 
         parser.putControlSequence(new GobbleOptMandOpt("PGLS")); 
         parser.putControlSequence(new GobbleOptMandOpt("PGLSpl")); 

         parser.putControlSequence(new AtGobble("glsentrynumberlist")); 
         parser.putControlSequence(new AtGobble("glsdisplaynumberlist")); 
         parser.putControlSequence(new GobbleOpt("chapter", 1, 1, '*')); 

         parser.putControlSequence(new GobbleOpt("printglossary", 1, 0)); 
         parser.putControlSequence(new GobbleOpt("printnoidxglossary", 1, 0)); 

         parser.putControlSequence(new Relax("printglossaries")); 
         parser.putControlSequence(new Relax("printnoidxglossaries")); 
      }
   }

   @Override
   protected boolean isIgnoredPackage(String styName)
   {
      return styName.startsWith("glossar");
   }

   @Override
   public boolean newcommandOverride(boolean isRobust, Overwrite overwrite,
     String type, String csName, boolean isShort,
     int numParams, TeXObject defValue, TeXObject definition)
   throws IOException
   {
      if (csName.equals("newdualentry") && 
          overwrite == Overwrite.FORBID)
      {
         // allow \newcommand{\newdualentry} to overwrite default
         // definition

         message(getMessage(getParser(), "gls2bib.override.newdualentry"));
         listener.addLaTeXCommand(csName, isShort, numParams, defValue, definition);
         return true;
      }

      return false;
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

   public GlsData getEntry(String label)
   {
      for (GlsData entryData : data)
      {
         if (entryData.getId().equals(label))
         {
            return entryData;
         }
      }

      return null;
   }

   public void process() throws IOException,Bib2GlsException
   {
      listener.requirepackage("etoolbox", null);

      data = new Vector<GlsData>();

      parser.parse(texFile, charset);

      PrintWriter out = null;

      HashMap<String,PrintWriter> splitOuts = null;

      if (splitOnType || splitOnCategory)
      {
         splitOuts = new HashMap<String,PrintWriter>();
      }

      try
      {
         if (data.isEmpty())
         {
            throw new Bib2GlsException(
               getMessage("gls2bib.no.entries"));
         }

         if (!overwriteFiles && bibFile.exists())
         {
            throw new IOException(getMessage("error.file_exists.nooverwrite",
               bibFile, "--overwrite"));
         }

         message(getMessage("message.writing", bibFile));

         Charset bibCharset;

         if (bibCharsetName == null)
         {
            bibCharset = getDefaultCharset();
            bibCharsetName = bibCharset.name();
         }
         else
         {
            bibCharset = Charset.forName(bibCharsetName);
         }

         out = new PrintWriter(createBufferedWriter(bibFile.toPath(), bibCharset));

         out.println("% Encoding: "+bibCharsetName);

         for (GlsData entry : data)
         {
            if (splitOnType || splitOnCategory)
            {
               String type = (splitOnType ? entry.getGlossaryType() : null);
               String category = (splitOnCategory ? entry.getCategory() : null);

               if (type == null && category == null)
               {
                  entry.writeBibEntry(out);
               }
               else
               {
                  String tag;

                  if (type == null)
                  {
                     tag = category;
                  }
                  else if (category == null || type.equals(category))
                  {
                     tag = type;
                  }
                  else
                  {
                     tag = String.format("%s-%s", type, category);
                  }

                  PrintWriter splitOut = splitOuts.get(tag);

                  if (splitOut == null)
                  {
                     File splitBibFile = new File(bibFile.getParent(), tag+".bib");

                    if (!overwriteFiles && splitBibFile.exists())
                    {
                       throw new IOException(getMessage("error.file_exists.nooverwrite",
                          splitBibFile, "--overwrite"));
                    }

                     message(getMessage("message.writing", splitBibFile));

                     splitOut = new PrintWriter(
                       createBufferedWriter(splitBibFile.toPath(), bibCharset));

                     splitOut.println("% Encoding: "+bibCharsetName);

                     splitOuts.put(tag, splitOut);
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

         if (splitOuts != null)
         {
            Set<String> keySet = splitOuts.keySet();

            for (Iterator<String> it=keySet.iterator(); it.hasNext(); )
            {
               String tag = it.next();
               splitOuts.get(tag).close();
            }
         }
      }
   }

   @Override
   protected void filterHelp()
   {
      super.filterHelp();

      printSyntaxItem(getMessage("gls2bib.syntax.ignore-category",
        "--[no-]ignore-category"));
      printSyntaxItem(getMessage("gls2bib.syntax.ignore-type",
        "--[no-]ignore-type"));
      printSyntaxItem(getMessage("gls2bib.syntax.ignore-sort",
        "--[no-]ignore-sort"));
   }

   @Override
   protected void ioHelp()
   {
      super.ioHelp();

      printSyntaxItem(getMessage("gls2bib.syntax.split-on-type",
        "--[no-]split-on-type", "-t"));
      printSyntaxItem(getMessage("gls2bib.syntax.split-on-category",
        "--[no-]split-on-category", "-c"));
   }

   @Override
   protected void adjustHelp()
   {
      super.adjustHelp();

      printSyntaxItem(getMessage("gls2bib.syntax.absorb-see",
        "--[no-]absorb-see"));
   }

   @Override
   protected void syntaxInfo()
   {
      printSyntaxItem(getMessage("gls2bib.syntax.info", "bib2gls"));
   }

   @Override
   protected int argCount(String arg)
   {
      if (arg.equals("--internal-field-map"))
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
      if (arg.equals("--ignore-sort"))
      {
         ignoreSortField = true;
      }
      else if (arg.equals("--no-ignore-sort"))
      {
         ignoreSortField = false;
      }
      else if (arg.equals("--ignore-type"))
      {
         ignoreTypeField = true;
      }
      else if (arg.equals("--no-ignore-type"))
      {
         ignoreTypeField = false;
      }
      else if (arg.equals("--split-on-type") || arg.equals("-t"))
      {
         splitOnType = true;
         ignoreTypeField = true;
         overwriteFiles = false;
      }
      else if (arg.equals("--no-split-on-type"))
      {
         splitOnType = false;
      }
      else if (arg.equals("--ignore-category"))
      {
         ignoreCategoryField = true;
      }
      else if (arg.equals("--no-ignore-category"))
      {
         ignoreCategoryField = false;
      }
      else if (arg.equals("--split-on-category") || arg.equals("-c"))
      {
         splitOnCategory = true;
         ignoreCategoryField = true;
         overwriteFiles = false;
      }
      else if (arg.equals("--no-split-on-category"))
      {
         splitOnCategory = false;
      }
      else if (arg.equals("--absorb-see"))
      {
         absorbSee = true;
      }
      else if (arg.equals("--no-absorb-see"))
      {
         absorbSee = false;
      }
      else if (isListArg(deque, arg, "--internal-field-map", returnVals))
      {
         if (returnVals[0] == null)
         {  
            throw new Bib2GlsSyntaxException(
               getMessage("common.missing.arg.value",
               arg));
         }

         if (internalFieldMap == null)
         {
            internalFieldMap = new HashMap<String,String>();
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

            internalFieldMap.put(map[0], map[1]);
         }
      }
      else
      {
         return super.parseArg(deque, arg, returnVals);
      }

      return true;
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
      String val = internalFieldMap.get(field);

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

   @Override
   public String getApplicationName()
   {
      return NAME;
   }

   @Override
   public String getCopyrightStartYear()
   {
      return "2017";
   }

   public static void main(String[] args)
   {
      Gls2Bib gls2bib = new Gls2Bib();
      gls2bib.run(args);
   }

   public static final String NAME = "convertgls2bib";

   private Vector<GlsData> data;

   private boolean ignoreSortField=true;
   private boolean ignoreTypeField=false;
   private boolean splitOnType=false;
   private boolean ignoreCategoryField=false;
   private boolean splitOnCategory=false;
   private boolean absorbSee=true;

   private boolean expandFields = false;

   private HashMap<String,String> internalFieldMap;

   private HashMap<String,Boolean> expandFieldMap;

   public static final String[] KNOWN_FIELDS = new String[]
   {"name", "text", "plural", "first", "firstplural", "symbol",
    "desc", "useri", "userii", "useriii", "useriv", "userv", "uservi"};

   public static final String[] ABBR_FIELDS = new String[]
   {"short", "shortpl", "long", "longpl", "full", "fullpl"};

}
