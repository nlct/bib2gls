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
package com.dickimawbooks.bib2gls;

import java.util.Vector;
import java.util.HashMap;
import java.util.Properties;
import java.util.Locale;
import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;

// Requires Java 1.7:
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Files;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.aux.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.CsvList;
import com.dickimawbooks.texparserlib.html.L2HStringConverter;
import com.dickimawbooks.texparserlib.bib.BibValueList;

public class Bib2Gls implements TeXApp
{
   public Bib2Gls(int debug, int verbose)
     throws IOException,InterruptedException,Bib2GlsException
   {
      debugLevel = debug;
      verboseLevel = verbose;

      if (debug > 0 && verbose > -1)
      {
         version();
      }

      initMessages();

      initSecuritySettings();

      if (verboseLevel >= 0 && debug == 0)
      {
         version();
      }

      formatMap = new HashMap<String,String>();
      texFiles = new Vector<File>();
   }

   private void initSecuritySettings()
     throws IOException,InterruptedException,Bib2GlsException
   {
      String openin = kpsewhich("--var-value=openin_any");
      String openout = kpsewhich("--var-value=openout_any");

      int openinAny = (openin == null || openin.isEmpty() ? -1 
                       : openin.charAt(0));
      int openoutAny = (openout == null || openout.isEmpty() ? -1 
                       : openout.charAt(0));

      if (openinAny == 'a' || openinAny == 'p' || openinAny == 'r')
      {
         openin_any = (char)openinAny;
      }
      else
      {
         throw new IllegalArgumentException(
           "Invalid openin_any value returned by kpsewhich: "+openin);
      }

      if (openoutAny == 'a' || openoutAny == 'p' || openoutAny == 'r')
      {
         openout_any = (char)openoutAny;
      }
      else
      {
         throw new IllegalArgumentException(
           "Invalid openout_any value returned by kpsewhich: "+openout);
      }

      try
      {
         String texmfoutputPath = kpsewhich("--var-value=TEXMFOUTPUT");

         if (texmfoutputPath != null && !texmfoutputPath.isEmpty())
         {
            File f = (new TeXPath(null, texmfoutputPath, false)).getFile();

            if (!f.isDirectory())
            {
               // not a directory so ignore it

               System.err.println("TEXMFOUT not a directory: "+texmfoutputPath);
            }

            texmfoutput = f.toPath();
         }
      }
      catch (IOException e)
      {// TEXMFOUTPUT not set
      }

      String cwdPath = System.getProperty("user.dir");

      if (cwdPath == null)
      {
         throw new IOException("Can't determine current working directory");
      }

      File cwdFile = new File(cwdPath);

      if (!cwdFile.isDirectory())
      {
         throw new IOException("CWD is not a directory: "+cwdPath);
      }

      cwd = cwdFile.toPath();
   }

   public void checkReadAccess(TeXPath path)
     throws IOException
   {
      if (!isReadAccessAllowed(path))
      {
         throw new IOException(getMessage("error.openin.forbidden",
           path));
      }
   }

   public void checkReadAccess(Path path)
     throws IOException
   {
      if (!isReadAccessAllowed(path))
      {
         throw new IOException(getMessage("error.openin.forbidden",
           path));
      }
   }

   public void checkReadAccess(File file)
     throws IOException
   {
      if (!isReadAccessAllowed(file.toPath()))
      {
         throw new IOException(getMessage("error.openin.forbidden",
           openin_any, file));
      }
   }

   public boolean isReadAccessAllowed(TeXPath path)
   {
      debug(getMessage("message.checking.read", path));

      if (openin_any == 'a')
      {
         return true;
      }

      if (path.isHidden())
      {
         return false;
      }

      if (openin_any == 'r')
      {// not a hidden file, so allow read access
         return true;
      }

      // paranoid setting on

      if (path.wasFoundByKpsewhich())
      {
         // If the file was found by kpsewhich, it must be on TeX's
         // path, so allow read access.

         return true;
      }

      Path base = path.getBaseDir();
      Path relPath = path.getRelativePath();

      Path p = (base == null ? relPath.normalize() : 
         base.resolve(relPath).normalize());

      if (p.isAbsolute())
      {
         if (p.startsWith(cwd))
         {
            // on the current working directory path so allow

            return true;
         }

         if (texmfoutput != null)
         {
            return p.startsWith(texmfoutput);
         }

         return false;
      }

      // Not absolute path. Is it on the cwd path?

      return p.toAbsolutePath().normalize().startsWith(cwd);
   }

   public boolean isReadAccessAllowed(File file)
   {
      return isReadAccessAllowed(file.toPath());
   }

   public boolean isReadAccessAllowed(Path path)
   {
      debug(getMessage("message.checking.read", path));

      if (openin_any == 'a')
      {
         return true;
      }

      if (isHidden(path))
      {
         return false;
      }

      if (openin_any == 'r')
      {// not a hidden file, so allow read access
         return true;
      }

      // paranoid setting on

      if (path.isAbsolute())
      {
         path = path.normalize();

         if (path.startsWith(cwd))
         {
            // on the current working directory path so allow

            return true;
         }

         if (texmfoutput != null)
         {
            return path.startsWith(texmfoutput);
         }

         return false;
      }

      // Not absolute path. Is it on the cwd path?

      return path.toAbsolutePath().normalize().startsWith(cwd);
   }

   public void checkWriteAccess(Path path)
     throws IOException
   {
      if (!isWriteAccessAllowed(path))
      {
         throw new IOException(getMessage("error.openout.forbidden",
           path));
      }
   }

   public void checkWriteAccess(File file)
     throws IOException
   {
      if (!isWriteAccessAllowed(file.toPath()))
      {
         throw new IOException(getMessage("error.openout.forbidden",
           file));
      }
   }

   public void registerTeXFile(File file) throws IOException
   {
      checkWriteAccess(file);

      if (texFiles.contains(file))
      {
         throw new IOException(getMessage("error.duplicate.resource",
           file));
      }

      texFiles.add(file);
   }


   public boolean isWriteAccessAllowed(TeXPath path)
   {
      return isWriteAccessAllowed(path.getPath());
   }

   public boolean isWriteAccessAllowed(File file)
   {
      return isWriteAccessAllowed(file.toPath());
   }

   public boolean isWriteAccessAllowed(Path path)
   {
      debug(getMessage("message.checking.write", path));

      // Forbid certain extension to be on the safe side

      String name = path.getName(path.getNameCount()-1).toString().toLowerCase();

      int idx = name.lastIndexOf(".");

      if (idx > -1)
      {
         name = name.substring(idx+1);

         if (name.equals("tex") || name.equals("ltx")
           ||name.equals("sty") || name.equals("cls")
           ||name.equals("bib") || name.equals("dtx")
           ||name.equals("ins") || name.equals("def")
           ||name.equals("ldf"))
         {
            debug(getMessageWithFallback("error.forbidden.ext",
              "Write access forbidden for extension: {0}", name));
            return false;
         }
      }

      if (openout_any == 'a')
      {
         return true;
      }

      if (isHidden(path))
      {
         return false;
      }

      if (openout_any == 'r')
      {// not a hidden file, so allow write access
         return true;
      }

      // paranoid setting on

      if (path.isAbsolute())
      {
         path = path.normalize();

         if (path.startsWith(cwd))
         {
            // on the current working directory path so allow

            return true;
         }

         if (texmfoutput != null)
         {
            return path.startsWith(texmfoutput);
         }

         return false;
      }

      // Not absolute path. Is it on the cwd path?

      return path.toAbsolutePath().normalize().startsWith(cwd);
   }

   public boolean isHidden(Path path)
   {
      for (int i = path.getNameCount()-1; i >= 0; i--)
      {
         String name = path.getName(i).toString();

         if (name.startsWith(".") && !name.equals(".") && !name.equals(".."))
         {
            return true;
         }
      }

      return false;
   }

   public File getWritableFile(File file)
     throws IOException
   {
      File dir = file.getParentFile();

      if (dir == null)
      {
         dir = cwd.toFile();
      }

      if (texmfoutput != null && !dir.canWrite())
      {
         warning(getMessage("warning.dir.no.write", dir, texmfoutput));
         file = new File(texmfoutput.toFile(), file.getName());
      }

      checkWriteAccess(file);

      return file;
   }

   public Path resolvePath(Path path)
   {
      return basePath.resolve(path).normalize();
   }

   public File resolveFile(File file)
   {
      if (file.getParentFile() == null)
      {
         file = new File(basePath.toFile(), file.getName());
      }
      else
      {
         file = basePath.resolve(file.toPath()).toFile();
      }

      return file;
   }

   public File resolveFile(String name)
   {
      return resolveFile(new File(name));
   }

   private String texToJavaCharset(String texCharset)
    throws Bib2GlsException
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

      throw new Bib2GlsException(texCharset);
   }

   public boolean useInterpreter()
   {
      return interpret;
   }

   public Vector<String> getPackages()
   {
      return packages;
   }

   public boolean fontSpecLoaded()
   {
      return fontspec;
   }

   // Search for some packages that texparserlib.jar recognises
   private void parseLog() throws IOException
   {
      String auxname = auxFile.getName();
      String name = auxname;

      int idx = name.lastIndexOf(".aux");

      if (idx < 1)
      {
         debug("Can't determine log file from "+auxFile);
         return;
      }

      String basename = name.substring(0, idx);
      name = basename+".log";

      File logFile = new File(auxFile.getParentFile(), name);

      packages = new Vector<String>();

      BufferedReader in = null;

      try
      {
         checkReadAccess(logFile);

         in = new BufferedReader(new FileReader(logFile));

         String line = null;

         while ((line = in.readLine()) != null)
         {
            Matcher m = PATTERN_PACKAGE.matcher(line);

            if (m.matches())
            {
               String pkg = m.group(1).toLowerCase();

               if (pkg.equals("amsmath")
                 ||pkg.equals("amssymb")
                 ||pkg.equals("pifont")
                 ||pkg.equals("textcase")
                 ||pkg.equals("wasysym")
                 ||pkg.equals("lipsum")
                 ||pkg.equals("natbib")
                 ||pkg.equals("mhchem")
                 ||pkg.equals("bpchem")
                 ||pkg.equals("stix")
                 ||pkg.equals("textcomp")
                 ||pkg.equals("mnsymbol")
                 ||pkg.equals("xspace")
                 ||pkg.equals("siunitx")
               )
               {
                  packages.add(pkg);
               }
               else if (pkg.equals("fontspec"))
               {
                  fontspec = true;
               }
               else if (pkg.equals("glossaries-extra"))
               {
                  try
                  {
                     Calendar minVersion = Calendar.getInstance();
                     minVersion.set(2017, 02, 03);

                     int year = Integer.parseInt(m.group(2));
                     int mon = Integer.parseInt(m.group(3));
                     int day = Integer.parseInt(m.group(4));

                     Calendar cal = Calendar.getInstance();
                     cal.set(year, mon, day);

                     if (cal.compareTo(minVersion) < 0)
                     {
                        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");

                        warning(getMessage("error.sty.too.old", 
                         "glossaries-extra", df.format(cal.getTime()), 
                           df.format(minVersion.getTime())));
                     }
                  }
                  catch (Exception e)
                  {
                     // something strange has happened

                     warning(getMessage("error.no.sty.version", 
                       "glossaries-extra"));
                     debug(e);
                  }
               }
            }
            else if (line.contains(auxname))
            {
               break;
            }
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      if (debugLevel > 0 && packages.size() > 0)
      {
         if (packages.size() == 1)
         {
            debug(getMessage("message.1.sty"));
         }
         else
         {
            debug(getMessage("message.2.sty", packages.size()));
         }

         for (String sty : packages)
         {
            debug(sty);
         }

         debug();
      }
   }

   private void initInterpreter(Vector<AuxData> data)
     throws IOException
   {
      L2HStringConverter listener = new L2HStringConverter(
         new Bib2GlsAdapter(this), data)
      {
         public void writeCodePoint(int codePoint) throws IOException
         {
            if (getWriter() == null) return;

            if (codePoint == '&')
            {
               getWriter().write("&amp;");
            }
            else if (codePoint == '<')
            {
               getWriter().write("&le;");
            }
            else if (codePoint == '>')
            {
               getWriter().write("&ge;");
            }
            else if (codePoint > 0xffff)
            {
               getWriter().write(String.format("%c%c", 
                   Character.highSurrogate(codePoint), 
                   Character.lowSurrogate(codePoint)));
            }
            else
            {
               getWriter().write(codePoint);
            }
         }
      };

      listener.setUseMathJax(false);
      listener.setIsInDocEnv(true);

      interpreter = new TeXParser(listener);

      interpreter.setCatCode('@', TeXParser.TYPE_LETTER);

      Vector<String> packages = getPackages();

      if (packages != null)
      {
         for (String sty : packages)
         {
            listener.usepackage(null, sty);
         }
      }

      listener.putControlSequence(new EnableTagging());
   }

   public void provideCommand(String csName, String text)
   {
      if (interpreter != null)
      {
         L2HStringConverter listener = 
           (L2HStringConverter)interpreter.getListener();

         listener.putControlSequence(new GenericCommand(csName, null, 
              listener.createString(text)));
      }
   }

   public void processPreamble(BibValueList list)
     throws IOException
   {
      if (interpreter == null) return;

      interpreter.addAll(list.expand(interpreter));

      if (getDebugLevel() > 0)
      {
         debug(String.format(
           "%n%s%n%s%n%n",
            getMessage("message.parsing.code"),
            interpreter.toString(interpreter)));
      }

      while (interpreter.size() > 0)
      {
         TeXObject obj = interpreter.pop();
         obj.process(interpreter);
      }
   }

   /*
    *  Attempts to interpret LaTeX code. This won't work on anything
    *  complicated and assumes custom user commands are provided in
    *  the .bib file @preamble{...} 
    *  Some standard LaTeX commands are recognised.
    */ 
   public String interpret(String texCode, BibValueList bibVal)
   {
      if (interpreter == null) return texCode;

      try
      {
         StringWriter writer = new StringWriter();
         ((L2HStringConverter)interpreter.getListener()).setWriter(writer);

         TeXObjectList objList = bibVal.expand(interpreter);

         if (objList == null) return texCode;

         interpreter.addAll(objList);

         if (getDebugLevel() > 0)
         {
            debug(String.format(
              "%n%s%n%s%n%n",
               getMessage("message.parsing.code"),
               interpreter.toString(interpreter)));
         }

         while (interpreter.size() > 0)
         {
            TeXObject obj = interpreter.pop();
            obj.process(interpreter);
         }

         String result = writer.toString();

         if (getDebugLevel() > 0)
         {
            debug(String.format("texparserlib:--> %s", result));
         }

         // Strip any html markup and trim leading/trailing spaces

         result = result.replaceAll("<[^>]+>", "").trim();

         result = result.replaceAll("\\&le;", "<");
         result = result.replaceAll("\\&ge;", ">");
         result = result.replaceAll("\\&amp;", "&");

         logMessage(String.format("texparserlib: %s -> %s", 
            texCode, result));

         return result;
      }
      catch (IOException e)
      {// too complicated

         return texCode;
      }
   }

   public void process(String[] args) 
     throws IOException,InterruptedException,Bib2GlsException
   {
      parseArgs(args);

      if (interpret)
      {
         try
         {
            parseLog();
         }
         catch (IOException e)
         {
            // Parsing the log file isn't essential.

            debug(e);
         }
      }

      AuxParser auxParser = new AuxParser(this)
      {
         protected void addPredefined()
         {
            super.addPredefined();

            addAuxCommand("glsxtr@resource", 2);
            addAuxCommand("glsxtr@fields", 1);
            addAuxCommand("glsxtr@record", 5);
            addAuxCommand("glsxtr@texencoding", 1);
            addAuxCommand("glsxtr@langtag", 1);
            addAuxCommand("glsxtr@shortcutsval", 1);
            addAuxCommand("glsxtr@pluralsuffixes", 4);
         }
      };

      TeXParser parser = auxParser.parseAuxFile(auxFile);

      glsresources = new Vector<GlsResource>();
      fields = new Vector<String>();
      fieldMap = new HashMap<String,String>();
      records = new Vector<GlsRecord>();
      dependencies = new Vector<String>();

      Vector<AuxData> auxData = auxParser.getAuxData();

      if (interpret)
      {
         initInterpreter(auxData);
      }

      Vector<AuxData> resourceData = new Vector<AuxData>();

      String pluralSuffix = "s";
      String shortPluralSuffix = "s";
      String acrPluralSuffix = "s";
      String defShortPluralSuffix = "s";

      for (AuxData data : auxData)
      {
         String name = data.getName();
 
         if (name.equals("glsxtr@resource"))
         {
            // defer creating resources until all aux data
            // processed.
            resourceData.add(data);
         }
         else if (name.equals("glsxtr@shortcutsval"))
         {
            // command line option overrides aux setting

            if (shortcuts == null)
            {
               setShortCuts(data.getArg(0).toString(parser));
            }
         }
         else if (name.equals("glsxtr@pluralsuffixes"))
         {
            pluralSuffix = data.getArg(0).toString(parser);
            shortPluralSuffix = data.getArg(1).toString(parser);
            acrPluralSuffix = data.getArg(2).toString(parser);
            defShortPluralSuffix = data.getArg(3).toString(parser);
         }
         else if (name.equals("glsxtr@langtag"))
         {
            setDocDefaultLocale(data.getArg(0).toString(parser));
         }
         else if (texCharset == null && name.equals("glsxtr@texencoding"))
         {
             try
             {
                texCharset = Charset.forName(
                   texToJavaCharset(data.getArg(0).toString(parser)));
             }
             catch (Bib2GlsException e)
             {
                texCharset = Charset.defaultCharset();

                warning(getMessage("error.unknown.tex.charset",
                  e.getMessage(), texCharset, "--tex-encoding"));
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

               debug("Adding field: "+field+" ("+map+")");
            }
         }
         else if (name.equals("glsxtr@record"))
         {
            GlsRecord newRecord = new GlsRecord(data.getArg(0).toString(parser),
                        data.getArg(1).toString(parser),
                        data.getArg(2).toString(parser),
                        data.getArg(3).toString(parser),
                        data.getArg(4).toString(parser));

            // skip duplicates

            boolean found = false;

            for (GlsRecord existingRecord : records)
            {
               if (existingRecord.equals(newRecord))
               {// exact match, skip
                  found = true;
                  break;
               }
               else if (existingRecord.partialMatch(newRecord))
               {
                  // matches everything except the format

                  String newFmt = newRecord.getFormat();
                  String existingFmt = existingRecord.getFormat();

                  // Any format overrides the default "glsnumberformat"

                  if (newFmt.equals("glsnumberformat"))
                  {// discard the new record

                     debug();
                     debug(getMessage("warning.discarding.conflicting.record",
                       newFmt, existingFmt, newRecord, existingRecord));
                     debug();
                  }
                  else if (existingFmt.equals("glsnumberformat"))
                  {// override the existing record

                     debug();
                     debug(getMessage("warning.discarding.conflicting.record",
                       newFmt, existingFmt, existingRecord, newRecord));
                     debug();

                     existingRecord.setFormat(newFmt);
                  } 
                  else
                  {
                     String newMap = formatMap.get(newFmt);
                     String existingMap = formatMap.get(existingFmt);

                     if (newMap != null && newMap.equals(existingFmt))
                     {
                        // discard new record

                        if (debugLevel > 0)
                        {
                           debug();
                           debug(getMessage(
                             "warning.discarding.conflicting.record.using.map",
                             newFmt, newMap, newRecord, existingRecord));
                           debug();
                        }
                     }
                     else if (existingMap != null && existingMap.equals(newFmt))
                     {
                        // discard existing record

                        if (debugLevel > 0)
                        {
                           debug();
                           debug(getMessage(
                             "warning.discarding.conflicting.record.using.map",
                             existingFmt, existingMap, 
                             existingRecord, newRecord));
                           debug();
                        }

                        existingRecord.setFormat(newFmt);
                     }
                     else if (existingMap != null && newMap != null
                              && existingMap.equals(newMap))
                     {
                        // replace both records with mapping

                        if (debugLevel > 0)
                        {
                           debug();
                           debug(getMessage(
                             "warning.discarding.conflicting.record.using.map2",
                             existingFmt, existingMap, 
                             newFmt, newMap, 
                             existingRecord, newRecord,
                             String.format("{%s}{%s}{%s}{%s}{%s}", 
                              existingRecord.getLabel(),
                              existingRecord.getPrefix(),
                              existingRecord.getCounter(),
                              newMap,
                              existingRecord.getLocation())));
                           debug();
                        }

                        existingRecord.setFormat(newMap);
                     }
                     else
                     {
                        // no map found. Discard the new record with a warning

                        warning();
                        warning(
                          getMessage("warning.discarding.conflicting.record",
                          newFmt, existingFmt, newRecord, existingRecord));
                        warning();
                     }
                  }

                  found = true;
                  break;
               }
            }

            if (!found)
            {
               records.add(newRecord);
            }
         }
      }

      provideCommand("glspluralsuffix", pluralSuffix);
      provideCommand("abbrvpluralsuffix", shortPluralSuffix);
      provideCommand("acrpluralsuffix", acrPluralSuffix);
      provideCommand("glsxtrabbrvpluralsuffix", defShortPluralSuffix);

      if (texCharset == null)
      {
         texCharset = Charset.defaultCharset();

         logMessage(getMessage("message.unknown.tex.charset", texCharset,
           "--tex-encoding"));
      }
      else
      {
         verbose(getMessage("message.tex.charset", texCharset));
      }

      for (AuxData data : resourceData)
      {
         glsresources.add(new GlsResource(parser, data,
            pluralSuffix, shortPluralSuffix));
      }

      if (glsresources.size() == 0)
      {
         throw new Bib2GlsException(getMessage(
           "error.missing.aux.cs.require_cs_or",
           "glsxtr@resource", "glsxtrresourcefile", 
           "GlsXtrLoadResources"));
      }

      if (fields.size() == 0)
      {
         warning(parser, 
           getMessage("error.missing.aux.new.cs", "glsxtr@fields", "1.11"));

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

      // add the fields needed for dualabbreviation
      addField("dualshort");
      addField("dualshortplural");
      addField("duallong");
      addField("duallongplural");

      for (int i = 0; i < glsresources.size(); i++)
      {
         currentResource = glsresources.get(i);

         // parse all the bib files
         currentResource.parse(parser);
      }

      currentResource = null;

      int count = 0;

      // the data needs processing in a separate loop in case of
      // unrecorded cross-references across different bib files.

      for (int i = 0; i < glsresources.size(); i++)
      {
         currentResource = glsresources.get(i);

         // If 'master' option was used, n will be -1
         int n = currentResource.processData();

         if (n > 0) count += n;
      }

      currentResource = null;

      if (count == 0 && records.size() == 0)
      {
         error(getMessage("error.missing.records"));
      }

      if (glsresources.size() > 1)
      {
         message(getChoiceMessage("message.written.total", 0,
            "entry", 3, count));
      }

      if (logWriter != null)
      {
         try
         {
            logWriter.close();
         }
         finally
         {
            logWriter = null;
         }

         message(getMessage("message.log.file", logFile));
      }
   }

   public GlsResource getCurrentResource()
   {
      return currentResource;
   }

   public void addDependent(String id)
   {
      if (!dependencies.contains(id))
      {
         verbose(getMessage("message.added.dep", id));
         dependencies.add(id);
      }
   }

   public Vector<String> getDependencies()
   {
      return dependencies;
   }

   public Charset getTeXCharset()
   {
      return texCharset;
   }

   public boolean checkAcroShortcuts()
   {
      return checkAcroShortcuts;
   }

   public boolean checkAbbrvShortcuts()
   {
      return checkAbbrvShortcuts;
   }

   // is the given field likely to occur in link text?
   public boolean checkNestedLinkTextField(String fieldName)
   {
      if (nestedLinkCheckFields == null) return false;

      for (int i = 0; i < nestedLinkCheckFields.length; i++)
      {
         if (fieldName.equals(nestedLinkCheckFields[i]))
         {
            return true;
         }
      }

      return false;
   }

   public boolean useGroupField()
   {
      return addGroupField;
   }

   public Vector<String> getFields()
   {
      return fields;
   }

   public void addField(String fieldName)
   {
      if (!fields.contains(fieldName))
      {
         fields.add(fieldName);
      }
   }

   public HashMap<String,String> getFieldMap()
   {
      return fieldMap;
   }

   public boolean isKnownField(String name)
   {
      if (fields.isEmpty())
      {
         debug("Empty field list when checking for field '"+name+"'");
      }

      for (String field : fields)
      {
         if (field.equals(name))
         {
            return true;
         }

         if (name.equals(fieldMap.get(field)))
         {
            return true;
         }
      }

      return false;
   }

   public Vector<GlsRecord> getRecords()
   {
      return records;
   }

   public boolean hasRecord(String id)
   {
      for (GlsRecord record : records)
      {
         if (id.equals(record.getLabel()))
         {
            return true;
         }
      }

      return false;
   }

   public boolean mfirstucProtection()
   {
      return mfirstucProtect;
   }

   public boolean mfirstucMathShiftProtection()
   {
      return mfirstucMProtect;
   }

   public String[] mfirstucProtectionFields()
   {
      return mfirstucProtectFields;
   }

   public void logMessage(String message)
   {
      if (logWriter != null)
      {
         logWriter.println(message);
      }
   }

   public void logMessage()
   {
      if (logWriter != null)
      {
         logWriter.println();
      }
   }

   public int getDebugLevel()
   {
      return debugLevel;
   }

   public int getVerboseLevel()
   {
      return verboseLevel;
   }

   public void debug(String message)
   {
      if (debugLevel > 0)
      {
         System.out.println(message);
         logMessage(message);
      }
   }

   public void debug(Throwable e)
   {
      if (debugLevel > 0)
      {
         String message = e.getMessage();

         if (message == null)
         {
            message = e.getClass().getSimpleName();
         }

         System.out.println(message);
         logMessage(message);

         e.printStackTrace();
      }
   }

   public void debug(String msgPrefix, Throwable e)
   {
      if (debugLevel > 0)
      {
         String message = e.getMessage();

         if (message == null)
         {
            message = e.getClass().getSimpleName();
         }

         if (msgPrefix != null)
         {
            message = msgPrefix+message;
         }

         System.out.println(message);
         logMessage(message);

         e.printStackTrace();
      }
   }

   public void debug()
   {
      if (debugLevel > 0)
      {
         System.out.println();
         logMessage();
      }
   }

   /*
    *  TeXApp method.
    */ 
   public String kpsewhich(String arg)
     throws IOException,InterruptedException
   {
      debug(getMessageWithFallback("message.running", 
        "Running {0}",
        String.format("kpsewhich '%s'", arg)));

      Process process =
        new ProcessBuilder("kpsewhich", arg).start();

      int exitCode = process.waitFor();

      String line = null;

      if (exitCode == 0)
      {
         InputStream stream = process.getInputStream();

         if (stream == null)
         {
            throw new IOException(
             getMessageWithFallback("error.cant.open.process.stream", 
             "Unable to open input stream from process: {0}",
             String.format("kpsewhich '%s'", arg)));
         }

         BufferedReader reader = null;

         try
         {
            reader = new BufferedReader(new InputStreamReader(stream));

            line = reader.readLine();

            debug(getMessageWithFallback("message.process.result",
                  "Processed returned: {0}", line));
         }
         finally
         {
            if (reader != null)
            {
               reader.close();
            }
         }
      }
      else
      {
         String msg = getMessageWithFallback("error.app_failed",
           "{0} failed with exit code {1}",
           String.format("kpsewhich '%s'", arg),  exitCode);

         debug(msg);

         throw new FileNotFoundException(msg);
      }

      return line;
   }

   public TeXPath getBibFilePath(TeXParser parser, String filename)
     throws IOException,InterruptedException
   {
      // TeXPath will convert from TeX path names (using /)
      // to OS path names.

      TeXPath path = new TeXPath(parser, filename, "bib", false);

      File bibFile = path.getFile();

      if (!bibFile.exists())
      {
         debug(getMessage("error.file.not.found", bibFile));

         File f = bibFile;

         if (basePath.equals(cwd))
         {
            // try finding the file in the aux file's directory

            File dir = auxFile.getParentFile();

            f = new File(dir, bibFile.getName());
         }
         else
         {
            f = resolvePath(path.getRelativePath()).toFile();

            if (!f.exists())
            {
               debug(getMessage("error.file.not.found", f));

               // try finding the file in the aux file's directory

               File dir = auxFile.getParentFile();

               f = new File(dir, bibFile.getName());
            }
         }

         if (!f.exists())
         {
            // use kpsewhich to find it

            String loc = kpsewhich(bibFile.getName());

            if (loc != null && !loc.isEmpty())
            {
               return new TeXPath(parser, loc, "", false);
            }
         }

         if (f.exists())
         {
            path = new TeXPath(parser, f);
         }
         else
         {
            debug(getMessage("error.file.not.found", f));
         }
      }

      return path;
   }

   /*
    *  TeXApp method needs defining, but not needed for
    *  the purposes of this application.
    */ 
   public void epstopdf(File epsFile, File pdfFile)
     throws IOException,InterruptedException
   {
      if (debugLevel > 0)
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
      if (debugLevel > 0)
      {// shouldn't happen
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
      verbose(getMessage("warning.substituting", original, replacement));
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
      if (verboseLevel >= 0)
      {
         System.out.println(text);
      }

      logMessage(text);
   }

   public void verbose(String text)
   {
      if (verboseLevel > 0)
      {
         System.out.println(text);
      }

      logMessage(text);
   }

   public void verbose()
   {
      if (verboseLevel > 0)
      {
         System.out.println();
      }

      logMessage();
   }

   public void verbose(TeXParser parser, String message)
   {
      int lineNum = parser.getLineNumber();
      File file = parser.getCurrentFile();

      if (lineNum != -1 && file != null)
      {
         message = fileLineMessage(file, lineNum, message);
      }

      verbose(message);
   }

   public static String fileLineMessage(File file, int lineNum,
     String message)
   {
      return String.format("%s:%d: %s", file.toString(), lineNum,
         message);
   }

   /*
    *  TeXApp method.
    */ 
   public void warning(TeXParser parser, String message)
   {
      if (verboseLevel >= 0)
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
      if (verboseLevel >= 0)
      {
         warning(fileLineMessage(file, line, message));
      }
   }

   public void warning(File file, int line, String message, Exception e)
   {
      if (verboseLevel >= 0)
      {
         warning(fileLineMessage(file, line, message), e);
      }
   }

   public void warning(String message)
   {
      message = getMessageWithFallback("warning.title",
         "Warning: {0}", message);

      if (verboseLevel >= 0)
      {
         System.err.println(message);
      }

      logMessage(message);
   }

   public void warning()
   {
      if (verboseLevel >= 0)
      {
         System.err.println();
      }

      logMessage();
   }

   public void warning(String message, Exception e)
   {
      if (verboseLevel >= 0)
      {
         System.err.println(message);
      }

      logMessage(message);

      if (debugLevel > 0)
      {
         e.printStackTrace();

         for (StackTraceElement elem : e.getStackTrace())
         {
            logMessage(elem.toString());
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

      if (debugLevel > 0)
      {
         e.printStackTrace();

         for (StackTraceElement elem : e.getStackTrace())
         {
            logMessage(elem.toString());
         }
      }

      exitCode = 2;
   }

   public void error(String msg)
   {
      msg = getMessageWithFallback("error.title", "Error: {0}", msg);

      System.err.println(msg);
      logMessage(msg);
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

   public void version()
   {
      System.out.println(getMessageWithFallback("about.version",
        "{0} version {1} ({2})", NAME, VERSION, DATE));
   }

   public void license()
   {
      System.out.println("Copyright 2017 Nicola Talbot");
      System.out.println(getMessage("about.license"));
   }

   public void help()
   {
      System.out.println(getMessage("syntax.usage", NAME));
      System.out.println();

      System.out.println(getMessage("syntax.info"));
      System.out.println();

      System.out.println(getMessage("syntax.options"));
      System.out.println();
      System.out.println(getMessage("syntax.help", "--help", "-h"));
      System.out.println(getMessage("syntax.version", "--version", "-v"));
      System.out.println(getMessage("syntax.debug", "--debug"));
      System.out.println(getMessage("syntax.nodebug", "--no-debug",
        "--nodebug"));
      System.out.println(getMessage("syntax.verbose", "--verbose"));
      System.out.println(getMessage("syntax.noverbose",
        "--no-verbose", "--noverbose"));
      System.out.println(getMessage("syntax.silent", "--silent"));

      System.out.println();
      System.out.println(getMessage("syntax.log", "--log-file", "-t"));
      System.out.println(getMessage("syntax.dir", "--dir", "-d"));

      System.out.println();
      System.out.println(getMessage("syntax.interpret", "--interpret"));
      System.out.println(getMessage("syntax.no.interpret", "--no-interpret"));
      System.out.println();

      System.out.println();
      System.out.println(getMessage("syntax.mfirstuc",
         "--mfirstuc-protection", "-u"));

      System.out.println();
      System.out.println(getMessage("syntax.no.mfirstuc",
         "--no-mfirstuc-protection"));

      System.out.println();
      System.out.println(getMessage("syntax.no.math.mfirstuc",
         "--no-mfirstuc-math-protection"));

      System.out.println();
      System.out.println(getMessage("syntax.math.mfirstuc",
         "--mfirstuc-math-protection", "--no-mfirstuc-protection"));

      System.out.println();
      System.out.println(getMessage("syntax.check.shortcuts",
         "--shortcuts"));

      System.out.println();
      System.out.println(getMessage("syntax.check.nested",
         "--nested-link-check"));

      System.out.println();
      System.out.println(getMessage("syntax.nocheck.nested",
         "--no-nested-link-check", "--nested-link-check"));

      System.out.println();
      System.out.println(getMessage("syntax.format.map",
         "--map-format", "-m"));

      System.out.println();
      System.out.println(getMessage("syntax.group",
         "--group", "-g"));

      System.out.println();
      System.out.println(getMessage("syntax.no.group",
         "--no-group"));

      System.out.println();
      System.out.println(getMessage("syntax.trim.fields",
         "--trim-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.no.trim.fields",
         "--no-trim-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.tex.encoding",
         "--tex-encoding"));

      System.exit(0);
   }

   private String getLanguageFileName(String tag)
   {
      return String.format("/resources/bib2gls-%s.xml", tag);
   }

   private void initMessages() throws Bib2GlsException,IOException
   {
      Locale locale = Locale.getDefault();
      docLocale = locale.toLanguageTag();

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

            if (script != null)
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

   private void setShortCuts(String value)
   {
      if (value.equals("acro") || value.equals("acronyms"))
      {
         shortcuts=value;
         checkAcroShortcuts = true;
      }
      else if (value.equals("abbr")
            || value.equals("abbreviations"))
      {
         shortcuts=value;
         checkAbbrvShortcuts = true;
      }
      else if (value.equals("all")
            || value.equals("true"))
      {
         shortcuts=value;
         checkAcroShortcuts = true;
         checkAbbrvShortcuts = true;
      }
      else if (value.equals("none")
            || value.equals("false"))
      {
         shortcuts=value;
         checkAcroShortcuts = false;
         checkAbbrvShortcuts = false;
      }
      else
      {
         throw new IllegalArgumentException(
           "Invalid shortcut value: "+value);
      }
   }

   public void setDocDefaultLocale(String languageTag)
   {
      docLocale = languageTag;
   }

   public String getDocDefaultLocale()
   {
      return docLocale;
   }

   public void setTrimFields(boolean trimFields)
   {
      this.trimFields = trimFields;
   }

   public boolean trimFields()
   {
      return trimFields;
   }

   private void parseArgs(String[] args)
     throws IOException,Bib2GlsSyntaxException
   {
      String dirName = null;
      String auxFileName = null;
      String logName = null;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--debug"))
         {
            if (i == args.length-1 || args[i+1].startsWith("-"))
            {
               debugLevel = 1;
               verboseLevel = 1;
               continue;
            }

            try
            {
               debugLevel = Integer.parseInt(args[i+1]);
               i++;
            }
            catch (NumberFormatException e)
            {
               // argument missing
               debugLevel = 1;
            }

            verboseLevel = debugLevel;
         }
         else if (args[i].equals("--no-debug") || args[i].equals("--nodebug"))
         {
            debugLevel = 0;
         }
         else if (args[i].equals("--verbose"))
         {
            verboseLevel = 1;
         }
         else if (args[i].equals("--no-verbose") 
           || args[i].equals("--noverbose"))
         {
            verboseLevel = 0;
         }
         else if (args[i].equals("--silent"))
         {
            verboseLevel = -1;
         }
         else if (args[i].equals("-h") || args[i].equals("--help"))
         {
            help();
         }
         else if (args[i].equals("-v") || args[i].equals("--version"))
         {
            version();
            license();
            System.exit(0);
         }
         else if (args[i].equals("-t") || args[i].equals("--log-file"))
         {
            i++;

            if (i == args.length)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", args[i-1]));
            }

            logName = args[i];
         }
         else if (args[i].equals("--interpret"))
         {
            interpret = true;
         }
         else if (args[i].equals("--no-interpret"))
         {
            interpret = false;
         }
         else if (args[i].equals("--no-mfirstuc-protection"))
         {
            mfirstucProtect = false;
         }
         else if (args[i].equals("--mfirstuc-protection")
               || args[i].equals("-u"))
         {
            mfirstucProtect = true;

            i++;

            if (i == args.length)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", args[i-1]));
            }

            if (args[i].equals("all"))
            {
               mfirstucProtectFields = null;
            }
            else if (args[i].isEmpty())
            {
               mfirstucProtect = false;
            }
            else
            {
               mfirstucProtectFields = args[i].split(" *, *");
            }
         }
         else if (args[i].equals("--no-mfirstuc-math-protection"))
         {
            mfirstucMProtect = false;
         }
         else if (args[i].equals("--mfirstuc-math-protection"))
         {
            mfirstucMProtect = true;
         }
         else if (args[i].equals("--shortcuts"))
         {
            i++;

            if (i == args.length)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", args[i-1]));
            }

            try
            {
               setShortCuts(args[i]);
            }
            catch (IllegalArgumentException e)
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.invalid.choice.value", 
                 args[i-1], args[i]), e);
            }
         }
         else if (args[i].equals("--nested-link-check"))
         {
            i++;

            if (i == args.length)
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.missing.value", args[i-1]));
            }

            if (args[i].equals("none") || args[i].isEmpty())
            {
               nestedLinkCheckFields = null;
            }
            else
            {
               nestedLinkCheckFields = args[i].split(" *, *");
            }
         }
         else if (args[i].equals("--no-nested-link-check"))
         {
            nestedLinkCheckFields = null;
         }
         else if (args[i].equals("--dir") || args[i].equals("-d"))
         {
            i++;

            if (i == args.length)
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.missing.value", args[i-1]));
            }

            dirName = args[i];
         }
         else if (args[i].equals("--map-format") || args[i].equals("-m"))
         {
            i++;

            if (i == args.length)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", args[i-1]));
            }

            String[] split = args[i].trim().split(" *, *");

            for (String value : split)
            {
               String[] values = value.split(" *: *");

               if (values.length != 2)
               {
                  throw new Bib2GlsSyntaxException(
                    getMessage("error.invalid.opt.value", args[i-1], args[i]));
               }

               formatMap.put(values[0], values[1]);
            }
         }
         else if (args[i].equals("--group") || args[i].equals("-g"))
         {
            addGroupField = true;
         }
         else if (args[i].equals("--no-group"))
         {
            addGroupField = false;
         }
         else if (args[i].equals("--tex-encoding"))
         {
            i++;

            if (i == args.length)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", args[i-1]));
            }

            texCharset = Charset.forName(args[i]);
         }
         else if (args[i].equals("--trim-fields"))
         {
            trimFields = true;
         }
         else if (args[i].equals("--no-trim-fields"))
         {
            trimFields = false;
         }
         else if (args[i].startsWith("-"))
         {
            throw new Bib2GlsSyntaxException(getMessage(
              "error.syntax.unknown_option", args[i]));
         }
         else if (auxFileName == null)
         {
            auxFileName = args[i];
         }
         else
         {
            throw new Bib2GlsSyntaxException(getMessage("error.only.one.aux"));
         }
      }

      if (auxFileName == null)
      {
         throw new Bib2GlsSyntaxException(getMessage("error.no.aux"));
      }

      if (!auxFileName.endsWith(".aux"))
      {
         auxFileName = auxFileName+".aux";
      }

      File dir = null;

      auxFile = new File(auxFileName);

      if (dirName != null)
      {
         dir = new File(dirName);
         basePath = dir.toPath();

         if (!dir.exists())
         {
            System.err.println(getMessage("error.dir.not.found", dirName));
            System.exit(1);
         }

         if (!dir.isDirectory())
         {
            System.err.println(getMessage("error.not.dir", dirName));
            System.exit(1);
         }

         if (auxFile.getParentFile() == null)
         {
            auxFile = new File(dir, auxFileName);
         }
         else
         {
            auxFile = dir.toPath().resolve(auxFile.toPath()).toFile();
         }
      }
      else
      {
         dir = auxFile.getParentFile();
         basePath = cwd;
      }

      if (!auxFile.exists())
      {
         System.err.println(getMessage("error.file.not.found", auxFileName));
         System.exit(0);
      }

      logFile = null;

      if (logName == null)
      {
         String base = auxFile.getName();

         logFile = new File(dir,
            base.substring(0,base.lastIndexOf("."))+".glg");
      }
      else
      {
         logFile = resolveFile(logName);
      }

      logFile = getWritableFile(logFile);

      try
      {
         logWriter = new PrintWriter(new FileWriter(logFile));

         logMessage(getMessage("about.version", NAME, VERSION, DATE));

         debug(String.format(
            "openin_any=%s%nopenout_any=%s%nTEXMFOUTPUT=%s%ncwd=%s", 
             openin_any, openout_any, 
             texmfoutput == null ? "" : texmfoutput,
             cwd));
      }
      catch (IOException e)
      {
         logWriter = null;
         System.err.println(getMessage("error.cant.open.log", 
            logFile.toString()));
         error(e);
      }
   }

   public static void main(String[] args)
   {
      Bib2Gls bib2gls = null;
      int debug = 0;
      int verbose = 0;

      // Quickly check for debug mode for debugging messages needed
      // before parseArgs().
      //
      // parseArgs() will perform error checking on the syntax
      // and allow multiple "--debug" or "--no-debug" to override the
      // first instance.

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--debug"))
         {
            try
            {
               if (args[i+1].startsWith("-"))
               {
                  debug = 1;
               }
               else
               {
                  i++;
                  debug = Integer.parseInt(args[i]);
               }
            }
            catch (Exception e)
            {
               debug = 1;
            }

            break;
         }
         else if (args[i].equals("--no-debug") || args[i].equals("--nodebug"))
         {
            break;
         }
         else if (args[i].equals("--silent"))
         {
            verbose = -1;
            break;
         }
      }

      try
      {
         bib2gls = new Bib2Gls(debug, verbose);

         bib2gls.process(args);

         if (bib2gls.exitCode != 0)
         {
            System.exit(bib2gls.exitCode);
         }
      }
      catch (Bib2GlsSyntaxException e)
      {
         System.err.println(e.getMessage());
         System.err.println(bib2gls.getMessage("syntax.use.help"));
         System.exit(1);
      }
      catch (Exception e)
      {
         if (bib2gls == null)
         {
            System.err.println("Fatal error: "+e.getMessage());

            if (debug > 0)
            {
               e.printStackTrace();
            }
         }
         else
         {
            bib2gls.error(e);
         }

         System.exit(3);
      }
   }

   public static final String NAME = "bib2gls";
   public static final String VERSION = "0.2a";
   public static final String DATE = "EXPERIMENTAL";
   //public static final String DATE = "2017-02-04";
   public int debugLevel = 0;
   public int verboseLevel = 0;

   private char openout_any='p';
   private char openin_any='p';
   private Path cwd;
   private Path texmfoutput = null;
   private Path basePath;

   private File auxFile;
   private File logFile;
   private PrintWriter logWriter=null;

   public static final Pattern PATTERN_PACKAGE = Pattern.compile(
       "Package: ([^\\s]+)(?:\\s+(\\d{4})/(\\d{2})/(\\d{2}))?.*");

   private boolean fontspec = false;

   private Vector<GlsResource> glsresources;
   private Vector<String> fields;
   private Vector<GlsRecord> records;

   private HashMap<String,String> fieldMap;
   private Vector<String> dependencies;

   private HashMap<String,String> formatMap;

   private Vector<File> texFiles;

   private boolean addGroupField = false;

   private Charset texCharset = null;

   private Bib2GlsMessages messages;

   private boolean mfirstucProtect = true;
   private boolean mfirstucMProtect = true;
   private String[] mfirstucProtectFields = null;

   private String shortcuts=null;

   private boolean checkAcroShortcuts = false;
   private boolean checkAbbrvShortcuts = false;

   private GlsResource currentResource = null;

   private String docLocale;

   private boolean trimFields = false;

   private boolean interpret = true;

   private Vector<String> packages = null;;

   private TeXParser interpreter = null;

   private int exitCode;

   private String[] nestedLinkCheckFields = new String[]
    {"name", "text", "plural", "first", "firstplural",
     "long", "longplural", "short", "shortplural", "symbol"};
}
