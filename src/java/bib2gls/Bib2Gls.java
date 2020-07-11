/*
    Copyright (C) 2017-2020 Nicola L.C. Talbot
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
import java.util.Set;
import java.util.Iterator;
import java.util.IllformedLocaleException;
import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.text.Normalizer;

// Requires at least Java 1.7:
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Files;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.auxfile.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.CsvList;
import com.dickimawbooks.texparserlib.latex.AtFirstOfTwo;
import com.dickimawbooks.texparserlib.latex.AtSecondOfTwo;
import com.dickimawbooks.texparserlib.latex.NewCommand;
import com.dickimawbooks.texparserlib.latex.LaTeXSty;
import com.dickimawbooks.texparserlib.latex.fontenc.FontEncSty;
import com.dickimawbooks.texparserlib.latex.textcase.MakeTextLowercase;
import com.dickimawbooks.texparserlib.latex.textcase.MakeTextUppercase;
import com.dickimawbooks.texparserlib.latex.mfirstuc.MfirstucSty;
import com.dickimawbooks.texparserlib.latex.mfirstuc.MakeFirstUc;
import com.dickimawbooks.texparserlib.latex.mfirstuc.CapitaliseWords;
import com.dickimawbooks.texparserlib.html.L2HStringConverter;
import com.dickimawbooks.texparserlib.bib.BibValueList;

public class Bib2Gls implements TeXApp
{
   public Bib2Gls(int debug, int verbose, String langTag)
     throws IOException,InterruptedException,Bib2GlsException
   {
      kpsewhichResults = new HashMap<String,String>();

      try
      {
         pending = new StringWriter();
         pendingWriter = new PrintWriter(pending);
      }
      catch (Throwable e)
      {
         pendingWriter = null;
      }

      debugLevel = debug;
      verboseLevel = verbose;
      shownVersion = false;

      if (debug > 0 && verbose > -1)
      {
         version();
         shownVersion = true;
      }

      initMessages(langTag);

      initSecuritySettings();

      if (verboseLevel >= 0 && !shownVersion)
      {
         version();
         shownVersion = true;
      }

      formatMap = new HashMap<String,String>();

      texFiles = new Vector<File>();

      packages = new Vector<String>();

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
      else if (openinAny == -1)
      {
         // not set, probably MikTeX distribution
         debugMessage("error.missing.value", "openin_any");
         openin_any = 'a';
      }
      else
      {
         warningMessage("error.invalid.opt.value", "openin_any", openin);
         openin_any = 'a';
      }

      if (openoutAny == 'a' || openoutAny == 'p' || openoutAny == 'r')
      {
         openout_any = (char)openoutAny;
      }
      else if (openoutAny == -1)
      {
         // not set, probably MikTeX distribution
         debugMessage("error.missing.value", "openout_any");
         openout_any = 'p';
      }
      else
      {
         warningMessage("error.invalid.opt.value", "openout_any", openout);
         openout_any = 'p';
      }

      try
      {
         String texmfoutputPath = kpsewhich("--var-value=TEXMFOUTPUT");

         if (texmfoutputPath != null && !texmfoutputPath.isEmpty())
         {
            File f = (new TeXPath(null, texmfoutputPath, false)).getFile();

            if (f.isDirectory())
            {
               texmfoutput = f.toPath();
            }
            else
            {
               // not a directory so ignore it

               System.err.println("TEXMFOUT not a directory: "
                 + texmfoutputPath);
            }
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
      debugMessage("message.checking.read", path);

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

      // normalize() eliminates redundant path elements.

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
      debugMessage("message.checking.read", path);

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

         // has kpsewhich found this file?

         String result = kpsewhichResults.get(
            path.getName(path.getNameCount()-1).toString());

         if (result != null)
         {
            File file;

            if (File.separatorChar == '/')
            {
               file = new File(result);
            }
            else
            {
               // convert from TeX path to native:
               String[] split = result.split("/");

               file = new File(split[0]+File.separator);

               for (int i = 0; i < split.length; i++)
               {
                  file = new File(file, split[i]);
               }
            }

            try
            {
               if (Files.isSameFile(path, file.toPath()))
               {
                  return true;
               }
            }
            catch (IOException e)
            {
               debug(e);
            }
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

   /*
   * Used by GlsResource to check the given .glstex file
   * is allowed write access and adds it to the list (ensuring no
   * duplicates). No duplicates should occur with
   * \GlsXtrLoadResources but it's possible if
   * \glsxtrresourcefile is used explicitly.
   */
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
      debugMessage("message.checking.write", path);

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
            if (debugLevel > 0)
            {
               logAndPrintMessage(getMessageWithFallback(
               "error.forbidden.ext",
               "Write access forbidden for extension: {0}", name));
            }

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

   /*
    * A file is considered hidden if any element on the path
    * starts with '.' (not including '.' itself or '..') 
    */ 
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
         warningMessage("warning.dir.no.write", dir, texmfoutput);
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

   /*
    * Convert inputenc.sty encoding options to Java charset names.
    */ 
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
         return "ISO-8859-9";
      }
      else if (texCharset.equals("latin6"))
      {
         return "ISO-8859-10";
      }
      else if (texCharset.equals("latin7"))
      {
         return "ISO-8859-13";
      }
      else if (texCharset.equals("latin8"))
      {
         return "ISO-8859-14";
      }
      else if (texCharset.equals("latin9"))
      {
         return "ISO-8859-15";
      }
      else if (texCharset.equals("latin10"))
      {
         return "ISO-8859-16";
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
      else if (texCharset.equals("cp865"))
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
         // don't know appropriate Java encoding label for this one
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

   public boolean suppressFieldExpansion()
   {
      return !expandFields;
   }

   public boolean useNonBreakSpace()
   {
      return useNonBreakSpace;
   }

   public boolean useInterpreter()
   {
      return interpret;
   }

   public L2HStringConverter getInterpreterListener()
   {
      return interpreter == null ? null :
       (L2HStringConverter)interpreter.getListener();
   }

   public TeXObjectList getWordExceptionList()
   {
      // if mfirstuc has been loaded (which must be done using
      // --packages mfirstuc) then the control sequence with the
      // name "@mfu@nocaplist" will contain the list of exceptions.

      ControlSequence cs = interpreter.getControlSequence("@mfu@nocaplist");

      if (cs == null) return null;

      TeXObjectList wordExceptions = null;

      if (cs instanceof GenericCommand)
      {
         wordExceptions = ((GenericCommand)cs).getDefinition();
      }

      return wordExceptions;
   }

   public Vector<String> getPackages()
   {
      return packages;
   }

   public boolean fontSpecLoaded()
   {
      return fontspec;
   }

   public boolean hyperrefLoaded()
   {
      return hyperref;
   }

   public boolean createHyperGroupsOn()
   {
      return createHyperGroups;
   }

   public void setCreateHyperGroups(boolean flag)
   {
      createHyperGroups = flag;
   }

   /*
   * Search the log file for some packages that texparserlib.jar recognises.
   * Also check and save the glossaries-extra.sty version.
   * If fontspec is loaded, then bib2gls assumes that UTF-8 is 
   * natively supported by the document and will allow non-ASCII 
   * labels in the bib files. If hyperref is loaded, then hyperref
   * support can be implemented.
   */

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

      BufferedReader in = null;

      // Check the list supplied by the --packages switch to find
      // out fontspec is included. It seems unlikely that a
      // XeLaTeX or LuaLaTeX document wouldn't include it, but
      // allow for it just in case. (If a user does --packages
      // fontspec with a pdflatex document, then bib2gls won't
      // warn against non-ASCII labels in the bib files, so
      // they won't find out about it until pdflatex generates 
      // an error.)

      for (String sty : packages)
      {
         if (sty.equals("fontspec"))
         {
            fontspec = true;
            break;
         }
      }

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

               if (pkg.equals("glossaries-extra"))
               {
                  try
                  {
                     Calendar minVersion = Calendar.getInstance();
                     minVersion.set(2017, 1, 3); // v1.12 (2017/02/03)

                     int year = Integer.parseInt(m.group(2));
                     int mon = Integer.parseInt(m.group(3));
                     int day = Integer.parseInt(m.group(4));

                     Calendar cal = Calendar.getInstance();
                     cal.set(year, mon-1, day);

                     SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");

                     glossariesExtraVersion = df.format(cal.getTime());

                     if (cal.compareTo(minVersion) < 0)
                     {

                        warningMessage("error.sty.too.old", 
                         "glossaries-extra", glossariesExtraVersion, 
                           df.format(minVersion.getTime()));
                     }
                     else
                     {
                        // Are multiple supplementary sources
                        // supported?

                        minVersion.set(MIN_MULTI_SUPP_YEAR, 
                           MIN_MULTI_SUPP_MONTH-1, MIN_MULTI_SUPP_DAY);

                        multiSuppSupported = (cal.compareTo(minVersion) >= 0);
                     }
                  }
                  catch (Exception e)
                  {
                     // something strange has happened

                     warningMessage("error.no.sty.version", 
                       "glossaries-extra");
                     debug(e);
                  }
               }
               else if (ignorePackages != null && ignorePackages.contains(pkg))
               {// skip
               }
               else if (pkg.equals("hyperref"))
               {
                  hyperref = true;
               }
               else if (pkg.equals("fontspec"))
               {
                  fontspec = true;
               }
               else if (isAutoSupportPackage(pkg))
               {
                  packages.add(pkg);
               }
            }
            else
            {
               m = PATTERN_ENCDEF.matcher(line);

               if (m.matches())
               {
                  if (fontencList == null)
                  {
                     fontencList = new Vector<String>();
                  }

                  fontencList.add(m.group(1));
               }
               else if (line.contains(auxname))
               {
                  break;
               }
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
            debugMessage("message.1.sty");
         }
         else
         {
            debugMessage("message.2.sty", packages.size());
         }

         for (String sty : packages)
         {
            debug(sty);
         }

         debug();
      }
   }

   public String getGlossariesExtraVersion()
   {
      return glossariesExtraVersion;
   }

   public static boolean isAutoSupportPackage(String pkg)
   {
      for (String name : AUTO_SUPPORT_PACKAGES)
      {
         if (name.compareToIgnoreCase(pkg) == 0)
         {
            return true;
         }
      }

      return false;
   }

   public static boolean isExtraSupportedPackage(String pkg)
   {
      for (String name : EXTRA_SUPPORTED_PACKAGES)
      {
         if (name.equals(pkg))
         {
            return true;
         }
      }

      return false;
   }

   public static boolean isKnownPackage(String pkg)
   {
      return isAutoSupportPackage(pkg) || isExtraSupportedPackage(pkg);
   }

   private void initInterpreter(Vector<AuxData> data)
     throws IOException
   {
      L2HStringConverter listener = new L2HStringConverter(
         new Bib2GlsAdapter(this), data, customPackages != null)
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
            else
            {
               getWriter().write(new String(Character.toChars(codePoint)));
            }
         }

         public void parsePackageFile(LaTeXSty sty) throws IOException
         {
            if (isParsePackageSupportOn() 
                 && customPackages.contains(sty.getName()))
            {
               sty.parseFile();
            }
         }

      };

      listener.setUseMathJax(false);
      listener.setIsInDocEnv(true);
      listener.setSupportUnicodeScript(supportUnicodeSubSuperScripts);

      interpreter = new TeXParser(listener);

      interpreter.setCatCode('@', TeXParser.TYPE_LETTER);

      MfirstucSty mfirstucSty = 
        (MfirstucSty)listener.usepackage(null, "mfirstuc", false);

      Vector<String> packages = getPackages();

      if (packages != null)
      {
         for (String styname : packages)
         {
            LaTeXSty sty = listener.usepackage(null, styname, false);

            if (fontencList != null && styname.equals("fontenc"))
            {
               if (sty == null || !(sty instanceof FontEncSty))
               {
                  sty = listener.getFontEncSty();
               }

               if (sty != null && sty instanceof FontEncSty)
               {
                  for (String enc : fontencList)
                  {
                     debugMessage("message.detected.fontenc", enc);

                     FontEncoding encoding = ((FontEncSty)sty).getEncoding(
                       enc.toUpperCase());

                     if (encoding != null)
                     {
                        // Provide the commands associated with this
                        // encoding.

                        encoding.addDefinitions(interpreter.getSettings());
                     }
                  }

                  fontencList = null;
               }
            }
         }
      }

      // Since the interpreter only has access to code fragments
      // not the entire document, there's no way for it to know the
      // complete set of defined commands, so allow \renewcommand
      // to unconditionally define a command without complaining
      // if it doesn't already exist.

      listener.putControlSequence(new NewCommand("renewcommand",
        NewCommand.OVERWRITE_ALLOW));

      listener.putControlSequence(new NewCommand("glsxtrprovidecommand",
        NewCommand.OVERWRITE_ALLOW));

      listener.putControlSequence(new MakeTextUppercase("bibglsuppercase"));
      listener.putControlSequence(new MakeTextLowercase("bibglslowercase"));
      listener.putControlSequence(new MakeFirstUc("bibglsfirstuc"));
      listener.putControlSequence(new CapitaliseWords(mfirstucSty, 
        "bibglstitlecase"));

      listener.putControlSequence(new EnableTagging());
      listener.putControlSequence(new AtFirstOfTwo("bibglscontributorlist"));
      listener.putControlSequence(new AtFirstOfTwo("bibglshyperlink"));
      listener.putControlSequence(new GlsHyperLink(this));
      listener.putControlSequence(new BibGlsContributor(this));
      listener.putControlSequence(new BibGlsDateTime());
      listener.putControlSequence(
         new BibGlsDateTime("bibglsdate", true, false));
      listener.putControlSequence(
         new BibGlsDateTime("bibglstime", false, true));

      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryname", "name", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentrytext", "text", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryshort", "short", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentrylong", "long", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryfirst", "first", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentrysymbol", "symbol", this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryuseri", "user1", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryuserii", "user2", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryuseriii", "user3", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryuseriv", "user4", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryuserv", "user5", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryuservi", "user6", this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryplural", "plural", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryfirstplural", "firstplural", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentryshortpl", "shortplural", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentrylongpl", "longplural", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsentrysymbolplural", "symbolplural", this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryname", "name", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentrytext", "text", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryshort", "short", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentrylong", "long", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryfirst", "first", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentrysymbol", "symbol", GlsUseField.CASE_SENTENCE, this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryuseri", "user1", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryuserii", "user2", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryuseriii", "user3", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryuseriv", "user4", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryuserv", "user5", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryuservi", "user6", GlsUseField.CASE_SENTENCE, this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryplural", "plural", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryfirstplural", "firstplural", 
         GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentryshortpl", "shortplural", 
         GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentrylongpl", "longplural", 
         GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsentrysymbolplural", "symbolplural", 
         GlsUseField.CASE_SENTENCE, this));

      // Treat \glsaccess... as \glsentry... etc

      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessname", "name", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccesstext", "text", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessshort", "short", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccesslong", "long", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessfirst", "first", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccesssymbol", "symbol", this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessuseri", "user1", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessuserii", "user2", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessuseriii", "user3", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessuseriv", "user4", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessuserv", "user5", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessuservi", "user6", this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessplural", "plural", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessfirstplural", "firstplural", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccessshortpl", "shortplural", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccesslongpl", "longplural", this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "glsaccesssymbolplural", "symbolplural", this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessname", "name", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccesstext", "text", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessshort", "short", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccesslong", "long", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessfirst", "first", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccesssymbol", "symbol", GlsUseField.CASE_SENTENCE, this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessuseri", "user1", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessuserii", "user2", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessuseriii", "user3", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessuseriv", "user4", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessuserv", "user5", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessuservi", "user6", GlsUseField.CASE_SENTENCE, this));

      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessplural", "plural", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessfirstplural", "firstplural", 
         GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccessshortpl", "shortplural", 
         GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccesslongpl", "longplural", 
         GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsEntryFieldValue(
        "Glsaccesssymbolplural", "symbolplural", 
         GlsUseField.CASE_SENTENCE, this));

      listener.putControlSequence(new GlsUseField(this));
      listener.putControlSequence(new GlsUseField(
        "Glsxtrusefield", GlsUseField.CASE_SENTENCE, this));
      listener.putControlSequence(new GlsUseField(
        "GLSxtrusefield", GlsUseField.CASE_TO_UPPER, this));
      listener.putControlSequence(new GlsUseField(
        "glsentrytitlecase", GlsUseField.CASE_TITLE_CASE, this));


      listener.putControlSequence(new GlsEntryParentName(this));

      listener.putControlSequence(new GenericCommand(
        "glsxtrhiernamesep", null, listener.createString(".")));
      listener.putControlSequence(new GlsHierName(this));
      listener.putControlSequence(new GlsHierName("Glsxtrhiername",
        GlsUseField.CASE_SENTENCE, true, this));
      listener.putControlSequence(new GlsHierName("GlsXtrhiername",
        GlsUseField.CASE_SENTENCE, false, this));
      listener.putControlSequence(new GlsHierName("GLSxtrhiername",
        GlsUseField.CASE_TO_UPPER, true, this));
      listener.putControlSequence(new GlsHierName("GLSXTRhiername",
        GlsUseField.CASE_TO_UPPER, false, this));

      listener.putControlSequence(listener.createSymbol("bibglshashchar", '#'));
      listener.putControlSequence(listener.createSymbol("bibglsunderscorechar", '_'));
      listener.putControlSequence(listener.createSymbol("bibglsdollarchar", '$'));
      listener.putControlSequence(listener.createSymbol("bibglsampersandchar", '&'));
      listener.putControlSequence(listener.createSymbol("bibglscircumchar", '^'));
      listener.putControlSequence(listener.createSymbol("glsbackslash", '\\'));
      listener.putControlSequence(listener.createSymbol("glstildechar", '~'));

      listener.putControlSequence(new FlattenedPostSort());
      listener.putControlSequence(new FlattenedPreSort());
      listener.putControlSequence(new HrefChar());
      listener.putControlSequence(new AtSecondOfTwo("bibglshrefunicode"));
      listener.putControlSequence(new HexUnicodeChar());

      // Custom packages may override the definitions of any of the
      // above.

      if (customPackages != null)
      {
         for (String sty : customPackages)
         {
            listener.usepackage(null, sty, false);
         }
      }

   }

   /*
    * Add the command whose control sequence name is given by csName
    * that expands to the given (literal) text. 
    */ 
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

   /*
    * Process the contents of @preamble to pick up any command
    * definitions. 
    */ 

   public void processPreamble(BibValueList list)
     throws IOException
   {
      if (interpreter == null) return;

      interpreter.addAll(list.expand(interpreter));

      if (getDebugLevel() > 0)
      {
         logAndPrintMessage(String.format(
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
    *  Some standard LaTeX commands are recognised. The trim value
    *  determines whether or not to trim leading/trailing spaces
    *  from the result.
    */ 
   public String interpret(String texCode, BibValueList bibVal, boolean trim)
   {
      if (interpreter == null) return texCode;

      try
      {
         L2HStringConverter listener = 
            (L2HStringConverter)interpreter.getListener();

         StringWriter writer = new StringWriter();
         listener.setWriter(writer);

         TeXObjectList objList = bibVal.expand(interpreter);

         if (objList == null) return texCode;

         objList = (TeXObjectList)objList.clone();

         interpreter.addAll(objList);

         if (getDebugLevel() > 0)
         {
            logAndPrintMessage(String.format(
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
            logAndPrintMessage(String.format("texparserlib:--> %s", result));
         }

         // Strip any html markup

         result = result.replaceAll("<[^>]+>", "");

         result = result.replaceAll("\\&le;", "<");
         result = result.replaceAll("\\&ge;", ">");
         result = result.replaceAll("\\&amp;", "&");

         // trim leading/trailing spaces if required

         if (trim)
         {
            result = result.trim();
         }

         logMessage(String.format("texparserlib: %s -> %s", 
            texCode, result));

         return result;
      }
      catch (IOException e)
      {// too complicated

         if (getDebugLevel() > 0)
         {
            debug("texparserlib: ");
            debug(e);
         }

         return texCode;
      }
   }

   /*
    * Converts the given TeX code into a string that's suitable for
    * use as a label.
    */ 
   public String convertToLabel(TeXParser parser, BibValueList value, 
      GlsResource resource, boolean isList)
    throws IOException
   {
      TeXObjectList list = value.expand(parser);

      String strVal = list.toString(parser);
      String original = strVal;

      /*
      * Check for \ { } ~ and $ to determine whether or not to
      * interpret the value.
      * There's no point checking for the other special characters.
      * For example, ^ and _ need to be in maths mode, so $ or \
      * (from \ensuremath or \begin{math} etc) will be present.
      * \_ \# \& and \% will match on the backslash. Comments are best
      * avoided within field values, but ought to have already been
      * removed if present. # and & shouldn't occur without some
      * kind of markup, and would likely be too complicated to
      * interpret anyway.
      */

      if (strVal.matches("(?s).*[\\\\\\{\\}\\~\\$].*"))
      {
         strVal = interpret(strVal, value, true);
      }

      // apply substitutions
      Vector<PatternReplace> regexList = resource.getLabelifySubstitutions();

      if (regexList != null)
      {
         for (PatternReplace patRep : regexList)
         {
            strVal = patRep.replaceAll(strVal);
         }
      }

      // strip all characters that aren't alphanumeric or spaces or
      // the following punctuation characters:
      // . - + : ; | / ! ? * < > @ ' `

      String allowedASCII = " \\.\\-\\+\\:\\;\\|\\/\\!\\?\\*\\<\\>\\@\\'\\`";

      if (isList)
      {// keep commas as well
         strVal = strVal.replaceAll(
            "[^,"+allowedASCII+"\\p{IsAlphabetic}\\p{IsDigit}]", "");
      }
      else
      {
         strVal = strVal.replaceAll(
            "[^"+allowedASCII+"\\p{IsAlphabetic}\\p{IsDigit}]", "");
      }

      if (!fontSpecLoaded())
      {
         /*
          * If fontspec hasn't been loaded, then assume non-ASCII
          * characters aren't allowed. Try to strip accents first
          * (if any non-ASCII character can be decomposed into
          * a basic Latin letter with combining diacritic) before
          * removing forbidden content.
          */ 

         strVal = Normalizer.normalize(strVal, Normalizer.Form.NFD);
         strVal = strVal.replaceAll("[^,"+allowedASCII+"a-zA-Z0-9]",
           "");
      }

      if (isList)
      {
         // remove empty elements
         strVal = strVal.replaceAll(",,+", ",");
         strVal = strVal.replaceAll("^,|,$", "");
      }

      if (verboseLevel > 0 && (!original.equals(strVal) || debugLevel > 0))
      {
         logMessage(String.format("%s: %s -> %s",
          isList? "labelify-list" : "labelify", original, strVal));
      }

      return strVal;
   }

   /*
    * Process the command line arguments and do the main action.
    */ 
   public void process(String[] args) 
     throws IOException,InterruptedException,Bib2GlsException
   {
      parseArgs(args);

      if (saveRecordCount)
      {
         recordCount = new HashMap<GlsRecord,Integer>();
      }

      try
      {
         parseLog();
      }
      catch (IOException e)
      {
         // Parsing the log file isn't essential although it
         // determines the glossaries-extra.sty version,
         // if the TeX engine natively supports Unicode, and if
         // hyperref is available.

         warningMessage("warning.cant.parse.file", logFile, 
           e.getMessage());
         debug(e);
      }

      if (texCharset == null && fontspec)
      {
         // Assume UTF-8 (there may be UTF-8 labels in the
         // aux records so the encoding needs to be set
         // before the aux file is opened).

         try
         {
            texCharset = Charset.forName("UTF-8");
         }
         catch (Exception e)
         {// shouldn't happen
            debug(e);
         }
      }

      AuxParser auxParser = new AuxParser(this, texCharset)
      {
         protected void addPredefined()
         {
            super.addPredefined();

            addAuxCommand("glsxtr@resource", 2);
            addAuxCommand("glsxtr@fields", 1);
            addAuxCommand("glsxtr@record", 5);
            addAuxCommand("glsxtr@recordsee", 2);
            addAuxCommand("glsxtr@record@nameref", 8);
            addAuxCommand("glsxtr@texencoding", 1);
            addAuxCommand("glsxtr@langtag", 1);
            addAuxCommand("glsxtr@shortcutsval", 1);
            addAuxCommand("glsxtr@pluralsuffixes", 4);
            addAuxCommand("@glsxtr@altmodifier", 1);
            addAuxCommand("@glsxtr@newglslike", 2);
            addAuxCommand("@glsxtr@prefixlabellist", 1);

            if (knownGlossaries != null)
            {
               addAuxCommand("@newglossary", 4);
            }
         }
      };

      TeXParser parser = auxParser.parseAuxFile(auxFile);

      glsresources = new Vector<GlsResource>();
      fields = new Vector<String>();
      fieldMap = new HashMap<String,String>();
      records = new Vector<GlsRecord>();
      seeRecords = new Vector<GlsSeeRecord>();
      selectedEntries = new Vector<String>();

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
            // Defer creating resources until all aux data is
            // processed, but strip double-quotes from the second
            // argument. (A literal double-quote can be identified
            // with \" but such a file naming scheme should not be
            // encouraged!)

            TeXObject glsFile = data.getArg(1);

            if (glsFile instanceof TeXObjectList)
            {
               for (int i = ((TeXObjectList)glsFile).size()-1; i >= 0; i--)
               {
                  TeXObject obj = ((TeXObjectList)glsFile).get(i);

                  if (obj instanceof CharObject
                       && ((CharObject)obj).getCharCode() == '"')
                  {
                     ((TeXObjectList)glsFile).remove(i);
                  }
               }
            }

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
         else if (name.equals("@glsxtr@altmodifier"))
         {
            String modifier = data.getArg(0).toString(parser).trim();

            if (!modifier.isEmpty())
            {
               altModifier = modifier.codePointAt(0);
            }
         }
         else if (name.equals("@glsxtr@newglslike"))
         {
            addGlsLike(data.getArg(0).toString(parser), 
             data.getArg(1).toString(parser).substring(1));
         }
         else if (name.equals("@glsxtr@prefixlabellist"))
         {
            String[] split = data.getArg(0).toString(parser).split(",");

            if (split != null && split.length > 0)
            {
               // Use fallback prefix, which is the final element in
               // the list.

               String prefix = split[split.length-1];

               if (!prefix.isEmpty())
               {
                  addGlsLike(prefix, "dgls");
                  addGlsLike(prefix, "dGls");
                  addGlsLike(prefix, "dGLS");
                  addGlsLike(prefix, "dglspl");
                  addGlsLike(prefix, "dGlspl");
                  addGlsLike(prefix, "dGLSpl");
                  addGlsLike(prefix, "dglslink");
                  addGlsLike(prefix, "dglsdisp");
               }
            }
         }
         else if (name.equals("glsxtr@langtag"))
         {
            setDocDefaultLocale(getLocale(data.getArg(0).toString(parser)));
         }
         else if (texCharset == null && name.equals("glsxtr@texencoding"))
         {
             try
             {
                String val = data.getArg(0).toString(parser).trim();

                if (val.equals("\\inputencodingname"))
                {
                   // If the encoding was written as \inputencodingname
                   // then that command was most probably set to \relax
                   // for some reason. In which case ignore it.

                   texCharset = Charset.defaultCharset();
                }
                else
                {
                   texCharset = Charset.forName(texToJavaCharset(val));
                }
             }
             catch (Bib2GlsException e)
             {
                texCharset = Charset.defaultCharset();

                warningMessage("error.unknown.tex.charset",
                  e.getMessage(), texCharset, "--tex-encoding");
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

               if (debugLevel > 0)
               {
                  logAndPrintMessage("Adding field: "+field+" ("+map+")");
               }
            }
         }
         else if (name.equals("glsxtr@recordsee"))
         {
            seeRecords.add(new GlsSeeRecord(
              data.getArg(0), data.getArg(1), parser));
         }
         else if (name.equals("glsxtr@record") 
                   || (useCiteAsRecord && name.equals("citation"))
                   || name.equals("glsxtr@record@nameref"))
         {
            String recordLabel = data.getArg(0).toString(parser);
            String recordPrefix;
            String recordCounter;
            String recordFormat;
            String recordLocation;
            String recordTitle=null;
            String recordHref=null;
            String recordHcounter=null;

            if (data.getNumArgs() >= 5)
            {
               recordPrefix = data.getArg(1).toString(parser);
               recordCounter = data.getArg(2).toString(parser);
               recordFormat = data.getArg(3).toString(parser);
               recordLocation = data.getArg(4).toString(parser);

               if (data.getNumArgs() == 8)
               {
                  recordTitle = data.getArg(5).toString(parser);
                  recordHref = data.getArg(6).toString(parser);
                  recordHcounter = data.getArg(7).toString(parser);
               }

               if (recordCounter.equals("wrglossary"))
               {
                  TeXObject pageRef = AuxData.getPageReference(
                    auxData, parser, "wrglossary."+recordLocation);

                  if (pageRef != null)
                  {
                     recordLocation = String.format(
                       "\\glsxtr@wrglossarylocation{%s}{%s}", 
                       recordLocation, pageRef.toString(parser));
                  }
               }
            }
            else
            {
               if (recordLabel.equals("*"))
               {
                  verboseMessage("message.ignored.record", "\\citation{*}");
                  continue;
               }

               recordPrefix = "";
               recordCounter = "page";
               recordFormat = "glsignore";
               recordLocation = "";
            }

            GlsRecord newRecord;

            if (recordTitle == null)
            {
               newRecord = new GlsRecord(this, recordLabel, recordPrefix,
                  recordCounter, recordFormat, recordLocation);
            }
            else
            {
               newRecord = new GlsRecordNameRef(this, recordLabel, recordPrefix,
                  recordCounter, recordFormat, recordLocation, recordTitle,
                  recordHref, recordHcounter);
            }

            incRecordCount(newRecord);

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

                  // Ranges override individual locations

                  String newPrefix = "";

                  if (newFmt.startsWith("(") || newFmt.startsWith(")"))
                  {
                     newPrefix = newFmt.substring(0, 1);

                     if (newFmt.length() == 1)
                     {
                        newFmt = "glsnumberformat";
                     }
                     else
                     {
                        newFmt = newFmt.substring(1);
                     }
                  }

                  String existingPrefix = "";

                  if (existingFmt.startsWith("(") || existingFmt.startsWith(")"))
                  {
                     existingPrefix = existingFmt.substring(0, 1);

                     if (existingFmt.length() == 1)
                     {
                        existingFmt = "glsnumberformat";
                     }
                     else
                     {
                        existingFmt = existingFmt.substring(1);
                     }
                  }

                  // Any format overrides the default "glsnumberformat"
                  // (or the ignored formats "glsignore"
                  //  and "glstriggerrecordformat")
                  // unless there's a range formation.

                  if (existingPrefix.equals(")") && newPrefix.equals("("))
                  {// One range is finishing and a new range is starting
                   // at the same location.

                     records.add(newRecord);
                  }
                  else if (existingPrefix.equals("(") && newPrefix.equals(")"))
                  {// Start and end of the range occur at the same location.
                   // A bit weird, but allow it if the format is the
                   // same.

                     if (existingFmt.equals(newFmt))
                     {
                        records.add(newRecord);
                     }
                     else
                     {
                        // Format isn't the same. Replace the closing
                        // format with the same as the opening format.

                        warningMessage("warning.conflicting.range.format",
                          existingPrefix+existingFmt, newPrefix+newFmt, 
                          newPrefix+existingFmt);

                        newRecord.setFormat(newPrefix+existingFmt);
                        newRecord.setLocation(existingRecord.getLocation());
                        records.add(newRecord);
                     }
                  }
                  else if (newPrefix.isEmpty() && !existingPrefix.isEmpty())
                  {// discard new record
                   // (keep the record with the range formation)

                     if (debugLevel > 0)
                     {
                        logAndPrintMessage();
                        logAndPrintMessage(getMessage(
                         "warning.discarding.conflicting.record",
                         newFmt, existingPrefix+existingFmt,
                         newRecord, existingRecord));
                        logAndPrintMessage();
                     }
                  }
                  else if (!newPrefix.isEmpty() && existingPrefix.isEmpty())
                  {// discard existing record
                   // (keep the record with the range formation)

                     if (debugLevel > 0)
                     {
                        logAndPrintMessage();
                        logAndPrintMessage(getMessage(
                          "warning.discarding.conflicting.record",
                          newPrefix+newFmt, existingPrefix+existingFmt,
                          existingRecord, newRecord));
                        logAndPrintMessage();
                     }

                     existingRecord.setFormat(newPrefix+newFmt);
                     existingRecord.setLocation(newRecord.getLocation());
                  }
                  else if (isIgnoredFormat(newFmt))
                  {// discard the new record

                     if (debugLevel > 0)
                     {
                        logAndPrintMessage();
                        logAndPrintMessage(getMessage(
                         "warning.discarding.conflicting.record",
                         newPrefix+newFmt, existingPrefix+existingFmt,
                         newRecord, existingRecord));
                        logAndPrintMessage();
                     }
                  }
                  else if (isIgnoredFormat(existingFmt))
                  {// override the existing record

                     if (debugLevel > 0)
                     {
                        logAndPrintMessage();
                        logAndPrintMessage(getMessage(
                          "warning.discarding.conflicting.record",
                          newPrefix+newFmt, existingPrefix+existingFmt,
                          existingRecord, newRecord));
                        logAndPrintMessage();
                     }

                     existingRecord.setFormat(newPrefix+newFmt);
                     existingRecord.setLocation(newRecord.getLocation());
                  } 
                  else if (newFmt.equals("glsnumberformat"))
                  {// discard the new record

                     if (debugLevel > 0)
                     {
                        logAndPrintMessage();
                        logAndPrintMessage(getMessage(
                          "warning.discarding.conflicting.record",
                          newPrefix+newFmt, existingPrefix+existingFmt,
                          newRecord, existingRecord));
                        logAndPrintMessage();
                     }
                  }
                  else if (existingFmt.equals("glsnumberformat"))
                  {// override the existing record

                     if (debugLevel > 0)
                     {
                         logAndPrintMessage();
                         logAndPrintMessage(getMessage(
                           "warning.discarding.conflicting.record",
                           newPrefix+newFmt, existingPrefix+existingFmt,
                           existingRecord, newRecord));
                         logAndPrintMessage();
                     }

                     existingRecord.setFormat(newPrefix+newFmt);
                     existingRecord.setLocation(newRecord.getLocation());
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
                           logAndPrintMessage();
                           logAndPrintMessage(getMessage(
                             "warning.discarding.conflicting.record.using.map",
                             newPrefix+newFmt, newPrefix+newMap, 
                             newRecord, existingRecord));
                           logAndPrintMessage();
                        }
                     }
                     else if (existingMap != null && existingMap.equals(newFmt))
                     {
                        // discard existing record

                        if (debugLevel > 0)
                        {
                           logAndPrintMessage();
                           logAndPrintMessage(getMessage(
                             "warning.discarding.conflicting.record.using.map",
                             existingFmt, 
                             existingMap, 
                             existingRecord, newRecord));
                           logAndPrintMessage();
                        }

                        existingRecord.setFormat(newPrefix+newFmt);
                        existingRecord.setLocation(newRecord.getLocation());
                     }
                     else if (existingMap != null && newMap != null
                              && existingMap.equals(newMap))
                     {
                        // replace both records with mapping

                        if (debugLevel > 0)
                        {
                           logAndPrintMessage();
                           logAndPrintMessage(getMessage(
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
                           logAndPrintMessage();
                        }

                        existingRecord.setFormat(newPrefix+newMap);
                        existingRecord.setLocation(newRecord.getLocation());
                     }
                     else
                     {
                        // no map found. Discard the new record with a warning

                        logMessage();
                        warningMessage(
                          "warning.discarding.conflicting.record",
                          newPrefix+newFmt, 
                          existingPrefix+existingFmt,
                          newRecord, existingRecord);
                        logMessage();
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
         else if (knownGlossaries != null && name.equals("@newglossary"))
         {
            addGlossary(data.getArg(0).toString(parser));
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
         verboseMessage("message.tex.charset", texCharset);
      }

      if (verboseLevel > 0)
      {
         Locale l = getDefaultLocale();

         if (l == null)
         {
            l = Locale.getDefault();
         }

         logMessage(getMessage("message.default.locale", l.toLanguageTag(),
           l.getDisplayName()));
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

      // If --group is used, make sure that the group field is
      // defined.

      if (useGroupField() && !fields.contains("group"))
      {
         warningMessage("warning.no_group_field");
      }

      // add the fields needed for dualabbreviation
      addField("dualshort");
      addField("dualshortplural");
      addField("duallong");
      addField("duallongplural");

      /*
       * Need to allow for cross-references across
       * different resource sets, but this causes all kinds
       * of complications, with changes in prefixes etc and
       * redefining commands in preambles, so this can only
       * be implemented in certain situations. 
       *
       * Each resource set is processed in stages:
       * 1. Initialisation (performed by constructor).
       * 2. Parsing .bib files.
       * 3. Processing entries (field conversions, creation of
       * duals etc, finding dependencies).
       * 4. Selection, sorting and writing .glstex files.
       *
       * Stage 3 may require the interpreter if
       * interpret-label-fields is set. This means that the preamble
       * for that resource must be parsed at the start of stage 3.
       * If different resources have different preambles, any
       * commands provided may override commands in earlier
       * preambles. This means that any resource set that has
       * a preamble must have stage 4 immediately follow stage 3.
       * In order to support cross-resource references, stage 3
       * must be performed in a separate loop to stage 4.
       * Therefore cross-resource references can't be supported
       * if any of the resources have a preamble or allow
       * label fields (such as category or alias) to be interpreted.
       */ 

      int count = 0;

      boolean allowsCrossResourceRefs = true;

      for (int i = 0; i < glsresources.size(); i++)
      {
         currentResource = glsresources.get(i);

         // Stage 2: parse all the bib files for this resource 
         currentResource.parseBibFiles(parser);

         if (allowsCrossResourceRefs && !forceCrossResourceRefs
              && !currentResource.allowsCrossResourceRefs())
         {
            debugMessage("message.cross-resource.notallowed", 
              currentResource);
            allowsCrossResourceRefs = false;
         }
      }

      if (allowsCrossResourceRefs)
      {
         logMessage(getMessage("message.cross-resource.dep.allowed"));

         dependencies = new Vector<String>();

         for (int i = 0; i < glsresources.size(); i++)
         {
            currentResource = glsresources.get(i);

            // Stage 3: interpret preamble, process entry fields,
            // establish dependencies

            currentResource.processBibList(parser);
         }

         for (int i = 0; i < glsresources.size(); i++)
         {
            currentResource = glsresources.get(i);

            // Stage 4: select required entries, sort and write .glstex
            // files

            // If 'master' option was used, n will be -1
            int n = currentResource.processData();

            if (n > 0) count += n;
         }
      }
      else
      {
         logMessage(getMessage("message.cross-resource.dep.notallowed",
           "--force-cross-resource-refs"));

         for (int i = 0; i < glsresources.size(); i++)
         {
            currentResource = glsresources.get(i);

            // Stage 3: interpret preamble, process entry fields,
            // establish dependencies

            currentResource.processBibList(parser);

            // Stage 4: select required entries, sort and write .glstex
            // files

            // If 'master' option was used, n will be -1
            int n = currentResource.processData();

            if (n > 0) count += n;
         }
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

   public void registerDependencies(Bib2GlsEntry entry)
   {
      if (dependencies == null || !entry.hasDependencies()) return;

      for (Iterator<String> it = entry.getDependencyIterator();
           it.hasNext();)
      {
         String dep = it.next();

         addDependent(dep);
      }
   }

   public void addDependent(String id)
   {
      if (!dependencies.contains(id))
      {
         verboseMessage("message.added.dep", id);
         dependencies.add(id);
      }
   }

   public boolean isDependent(String id)
   {
      return dependencies == null ? false : dependencies.contains(id);
   }

   public boolean hasCrossResourceDependencies()
   {
      return dependencies != null;
   }

   public Iterator<String> getDependencyIterator()
   {
      return dependencies == null ? null : dependencies.iterator();
   }

   private boolean isIgnoredFormat(String fmt)
   {
      return fmt.equals("glsignore") || fmt.equals("glstriggerrecordformat");
   }

   public GlsResource getCurrentResource()
   {
      return currentResource;
   }

   public boolean isLastResource(GlsResource resource)
   {
      return glsresources == null ? false : 
                (glsresources.lastElement() == resource);
   }

   public Vector<GlsResource> getResources()
   {
      return glsresources;
   }

   public Charset getTeXCharset()
   {
      return texCharset;
   }

   public boolean useCiteAsRecord()
   {
      return useCiteAsRecord;
   }

   public boolean mergeNameRefOnLocation()
   {
      return mergeNameRefOn == MERGE_NAMEREF_ON_LOCATION;
   }

   public boolean mergeNameRefOnTitle()
   {
      return mergeNameRefOn == MERGE_NAMEREF_ON_TITLE;
   }

   public boolean mergeNameRefOnHref()
   {
      return mergeNameRefOn == MERGE_NAMEREF_ON_HREF;
   }

   public boolean mergeNameRefOnHcounter()
   {
      return mergeNameRefOn == MERGE_NAMEREF_ON_HCOUNTER;
   }

   public boolean mergeWrGlossaryLocations()
   {
      return mergeWrGlossaryLocations;
   }

   public boolean checkAcroShortcuts()
   {
      return checkAcroShortcuts;
   }

   public boolean checkAbbrvShortcuts()
   {
      return checkAbbrvShortcuts;
   }

   public int getAltModifier()
   {
      return altModifier;
   }

   // identify commands that have been defined with \@glsxtrnewgls
   public boolean isGlsLike(String csname)
   {
      if (glsLike == null) return false;

      return glsLike.get(csname) != null;
   }

   public void addGlsLike(String prefix, String csname)
   {
      if (glsLike == null)
      {
         glsLike = new HashMap<String,String>();
      }

      glsLike.put(csname, prefix);
   }

   public String getGlsLikePrefix(String csname)
   {
      return glsLike == null ? null : glsLike.get(csname);
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

   public boolean isMultipleSupplementarySupported()
   {
      return multiSuppSupported;
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

   public boolean isInternalField(String name)
   {
      return name.startsWith("bib2gls@");
   }

   public boolean isNonBibField(String name)
   {
      for (String field : NON_BIB_FIELDS)
      {
         if (field.equals(name))
         {
            return true;
         }
      }

      for (String field : PRIVATE_NON_BIB_FIELDS)
      {
         if (field.equals(name))
         {
            return true;
         }
      }

      return isInternalField(name) || name.startsWith("bibtexentry@")
         || name.endsWith("endpunc") || name.startsWith("recordcount.")
         || name.startsWith("currcount@") || name.startsWith("prevcount@");
   }

   public boolean isPrivateNonBibField(String name)
   {
      for (String field : PRIVATE_NON_BIB_FIELDS)
      {
         if (field.equals(name))
         {
            return true;
         }
      }

      return isInternalField(name) || name.startsWith("bibtexentry@")
         || name.startsWith("recordcount.")
         || name.startsWith("currcount@") || name.startsWith("prevcount@");
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

   // No key available but allow the field to be referenced
   // in certain contexts.
   public boolean isKnownSpecialField(String field)
   {
      for (String spField : SPAWN_SPECIAL_FIELDS)
      {
         if (spField.equals(field))
         {
            return true;
         }
      }

      for (String spField : DUAL_SPECIAL_FIELDS)
      {
         if (spField.equals(field))
         {
            return true;
         }
      }

      return false;
   }

   public boolean isCheckNonBibFieldsOn()
   {
      return checkNonBibFields;
   }

   public boolean isWarnUnknownEntryTypesOn()
   {
      return warnUnknownEntryTypes;
   }

   public boolean isProvideGlossariesOn()
   {
      return knownGlossaries != null;
   }

   public boolean isKnownGlossary(String label)
   {
      return knownGlossaries != null && knownGlossaries.contains(label);
   }

   // avoiding duplicate checks (only use once already checked isProvideGlossariesOn and
   // isKnownGlossary)
   public void addGlossary(String label)
   {
      knownGlossaries.add(label);
   }

   public void selectedEntry(String label)
   {
      selectedEntries.add(label);
   }

   public boolean isEntrySelected(String label)
   {
      return selectedEntries.contains(label);
   }

   public GlsSeeRecord getSeeRecord(String label)
   {
      for (GlsSeeRecord record : seeRecords)
      {
         if (record.getLabel().equals(label))
         {
            return record;
         }
      }

      return null;
   }

   public Vector<GlsRecord> getRecords()
   {
      return records;
   }

   public Vector<GlsSeeRecord> getSeeRecords()
   {
      return seeRecords;
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

   public boolean hasSeeRecord(String id)
   {
      for (GlsSeeRecord record : seeRecords)
      {
         if (id.equals(record.getLabel()))
         {
            return true;
         }
      }

      return false;
   }

   public GlsRecord getRecordCountKey(GlsRecord record)
   {
      if (recordCount == null) return null;

      Set<GlsRecord> keys = recordCount.keySet();

      for (Iterator<GlsRecord> it = keys.iterator(); it.hasNext(); )
      {
         GlsRecord key = it.next();

         boolean match = key.getLabel().equals(record.getLabel())
          && key.getCounter().equals(record.getCounter());

         if (saveRecordCountUnit)
         {
            match = match && key.getLocation().equals(record.getLocation());
         }

         if (match)
         {
            return key;
         }
      }

      return null;
   }

   public boolean isRecordCountSet()
   {
      return recordCount != null;
   }

   public Set<GlsRecord> getRecordCountKeySet()
   {
      return recordCount == null ? null : recordCount.keySet();
   }

   public Integer getRecordCount(GlsRecord key)
   {
      return recordCount == null ? null : recordCount.get(key);
   }

   public void writeRecordCount(String entryLabel, PrintWriter writer)
    throws IOException
   {
      Set<GlsRecord> keys = getRecordCountKeySet();

      if (keys == null) return;

      int total = 0;

      for (Iterator<GlsRecord> it = keys.iterator(); it.hasNext(); )
      {
         GlsRecord record = it.next();

         if (record.getLabel().equals(entryLabel))
         {
            Integer count = getRecordCount(record);
            total += count;

            writer.format("\\bibglssetrecordcount{%s}{%s}{%d}%n",
              entryLabel, record.getCounter(), count);

            if (saveRecordCountUnit)
            {
               writer.format("\\bibglssetlocationrecordcount{%s}{%s}{%s}{%d}%n",
                 entryLabel, record.getCounter(), record.getLocation(), count);
            }
         }
      }

      writer.format("\\bibglssettotalrecordcount{%s}{%d}%n",
         entryLabel, total);
   }

   public static String replaceSpecialChars(String value)
   {
      StringBuilder builder = new StringBuilder();
      boolean cs = false;

      for (int i = 0; i < value.length(); )
      {
         int cp = value.codePointAt(i);
         i += Character.charCount(cp);

         switch (cp)
         {
            case '\\':

               // is it followed by char`\\ ?

               if (i < value.length() 
                    && value.substring(i).startsWith("char`\\"))
               {
                  builder.append(value.substring(i-1, i+7));
                  i = i+7;
               }
               else
               {
                  builder.append("\\glsbackslash ");
                  cs = true;
               }

            break;
            case '%':
               builder.append("\\glspercentchar ");
               cs = true;
            break;
            case '{':
               builder.append("\\glsopenbrace ");
               cs = true;
            break;
            case '}':
               builder.append("\\glsclosebrace ");
               cs = true;
            break;
            case '~':
               builder.append("\\glstildechar ");
               cs = true;
            break;
            case '#':
               builder.append(String.format("\\bibglshashchar ", cp));
               cs = true;
            break;
            case '_':
               builder.append(String.format("\\bibglsunderscorechar ", cp));
               cs = true;
            break;
            case '$':
               builder.append(String.format("\\bibglsdollarchar ", cp));
               cs = true;
            break;
            case '&':
               builder.append(String.format("\\bibglsampersandchar ", cp));
               cs = true;
            break;
            case '^':
               builder.append(String.format("\\bibglscircumchar ", cp));
               cs = true;
            break;
            default:
               if (cs && Character.isWhitespace(cp))
               {
                  builder.append("\\space");
               }
               cs = false;

               builder.appendCodePoint(cp);
         }
      }

      return builder.toString();
   }

   public void writeCommonCommands(PrintWriter writer)
    throws IOException
   {
      if (commonCommandsDone)
      {
         return;
      }

      writer.println("\\providecommand{\\bibglshashchar}{\\expandafter\\@gobble\\string\\#}");
      writer.println("\\providecommand{\\bibglscircumchar}{\\expandafter\\@gobble\\string\\^}");
      writer.println("\\providecommand{\\bibglsdollarchar}{\\expandafter\\@gobble\\string\\$}");
      writer.println("\\providecommand{\\bibglsampersandchar}{\\expandafter\\@gobble\\string\\&}");
      writer.println("\\providecommand{\\bibglsunderscorechar}{\\expandafter\\@gobble\\string\\_}");
      writer.println("\\providecommand{\\bibglshrefchar}[2]{\\glspercentchar #1}");

      if (fontspec)
      {
         writer.println("\\providecommand{\\bibglshrefunicode}[2]{#2}");
      }

      if (hyperref)
      {
         writer.println("\\providecommand{\\bibglshexunicodechar}[1]{\\csname ifHy@unicode\\endcsname\\texorpdfstring{\\symbol{\\string\"#1}}{\\unichar{\\string\"#1}}\\else\\symbol{\\string\"#1}\\fi}");
      }
      else
      {
         writer.println("\\providecommand{\\bibglshexunicodechar}[1]{\\symbol{\\string\"#1}}");
      }

      writer.println("\\providecommand{\\bibglsusesee}[1]{\\glsxtrusesee{#1}}");
      writer.println("\\providecommand{\\bibglsusealias}[1]{%");
      writer.println(" \\glsxtrifhasfield{alias}{#1}%");
      writer.println(" {\\expandafter\\glsseeformat\\expandafter{\\glscurrentfieldvalue}{}}%");
      writer.println(" {}%");
      writer.println("}");
      writer.println("\\providecommand{\\bibglsuseseealso}[1]{\\glsxtruseseealso{#1}}");
      writer.println("\\providecommand{\\bibglsdelimN}{\\delimN}");
      writer.println("\\providecommand{\\bibglslastDelimN}{,~}");
      writer.println("\\providecommand{\\bibglsrange}[1]{#1}");
      writer.println("\\providecommand{\\bibglsinterloper}[1]{#1\\bibglsdelimN }");
      writer.format("\\providecommand{\\bibglspassimname}{%s}%n",
        getMessage("tag.passim"));
      writer.println("\\providecommand{\\bibglspassim}{ \\bibglspassimname}");
      writer.println("\\providecommand*{\\bibglshyperlink}[2]{\\glshyperlink[#1]{#2}}");
      writer.println();

      writer.println("\\providecommand{\\bibglsuppercase}{\\MakeTextUppercase}");
      writer.println("\\providecommand{\\bibglslowercase}{\\MakeTextLowercase}");
      writer.println("\\providecommand{\\bibglsfirstuc}{\\makefirstuc}");
      writer.println("\\providecommand{\\bibglstitlecase}{\\capitalisewords}");
      writer.println();

      writer.println("\\providecommand{\\bibglsprimaryprefixlabel}[1]{}");
      writer.println("\\providecommand{\\bibglsdualprefixlabel}[1]{}");
      writer.println("\\providecommand{\\bibglstertiaryprefixlabel}[1]{}");
      writer.println("\\providecommand{\\bibglsexternalprefixlabel}[2]{}");
      writer.println();

      if (recordCount != null)
      {
         writer.println("\\ifdef\\glsxtrenablerecordcount %glossaries-extra.sty v1.21+");
         writer.println("{\\glsxtrenablerecordcount}");
         writer.println("{");
         writer.println(" \\PackageWarning{bib2gls}{You need at least v1.21 of glossaries-extra with --record-count or --record-count-unit}");
         writer.println("}");
         writer.println();

         writer.println("\\providecommand*{\\bibglssetrecordcount}[3]{%");
         writer.println("   \\GlsXtrSetField{#1}{recordcount.#2}{#3}%");
         writer.println("}");

         writer.println("\\providecommand*{\\bibglssettotalrecordcount}[2]{%");
         writer.println("   \\GlsXtrSetField{#1}{recordcount}{#2}%");
         writer.println("}");

         if (saveRecordCountUnit)
         {
            writer.println("\\ifdef\\glsxtrdetoklocation");
            writer.println("{% glossaries-extra v1.21+");
            writer.println("  \\providecommand*{\\bibglssetlocationrecordcount}[4]{%");
            writer.println("     \\GlsXtrSetField{#1}{recordcount.#2.\\glsxtrdetoklocation{#3}}{#4}%");
            writer.println("  }");
            writer.println("}");
            writer.println("{");
            writer.println("  \\providecommand*{\\bibglssetlocationrecordcount}[4]{%");
            writer.println("     \\GlsXtrSetField{#1}{recordcount.#2.#3}{#4}%");
            writer.println("  }");
            writer.println("}");

         }
      }

      commonCommandsDone = true;
   }

   public void incRecordCount(GlsRecord record)
   {
      if (recordCount == null) return;

      GlsRecord key = getRecordCountKey(record);

      if (key == null)
      {
         recordCount.put(record, Integer.valueOf(1));
      }
      else
      {
         Integer val = recordCount.get(key);
         recordCount.put(key, Integer.valueOf(val+1));
      }
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

   public void logEncodingDetected(Charset charset)
   {
      logMessage(getMessage("message.detected.charset", charset)); 
   }

   public void logEncoding(Charset charset)
   {
      if (charset == null)
      {
         logMessage(getMessage("message.charset",
           getMessage("message.null"))); 
      }
      else
      {
         logMessage(getMessage("message.charset",
           charset.name())); 
      }
   }

   public void logEncoding(String charset)
   {
      if (charset == null)
      {
         logMessage(getMessage("message.charset",
           getMessage("message.null"))); 
      }
      else
      {
         logMessage(getMessage("message.charset", charset)); 
      }
   }

   public void logDefaultEncoding(Charset charset)
   {
      if (charset == null)
      {
         logMessage(getMessage("message.default.charset",
           getMessage("message.null"))); 
      }
      else
      {
         logMessage(getMessage("message.default.charset",
           charset.name())); 
      }
   }

   public void logDefaultEncoding(String charset)
   {
      if (charset == null)
      {
         logMessage(getMessage("message.default.charset",
           getMessage("message.null"))); 
      }
      else
      {
         logMessage(getMessage("message.default.charset",
           charset)); 
      }
   }

   public void logMessageNoLn(String message)
   {
      if (logWriter != null)
      {
         logWriter.print(message);
      }
      else if (pendingWriter != null)
      {
         pendingWriter.print(message);
      }
   }

   public void logMessage(String message)
   {
      if (logWriter != null)
      {
         logWriter.println(message);
      }
      else if (pendingWriter != null)
      {
         pendingWriter.println(message);
      }
   }

   public void logMessage()
   {
      if (logWriter != null)
      {
         logWriter.println();
      }
      else if (pendingWriter != null)
      {
         pendingWriter.println();
      }
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

   public int getDebugLevel()
   {
      return debugLevel;
   }

   public int getVerboseLevel()
   {
      return verboseLevel;
   }

   public void debugMessage(String key, Object... params)
   {
      if (debugLevel > 0)
      {
         logAndPrintMessage(getMessage(key, params));
      }
   }

   public void debug(String message)
   {
      if (debugLevel > 0)
      {
         logAndPrintMessage(message);
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

         logAndPrintMessage(message);

         e.printStackTrace();

         if (logWriter != null)
         {
            e.printStackTrace(logWriter);
         }
         else if (pendingWriter != null)
         {
            e.printStackTrace(pendingWriter);
         }
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

         logAndPrintMessage(message);

         e.printStackTrace();

         if (logWriter != null)
         {
            e.printStackTrace(logWriter);
         }
         else if (pendingWriter != null)
         {
            e.printStackTrace(pendingWriter);
         }
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
      // has kpsewhich already been called with this argument? 

      String result = kpsewhichResults.get(arg);

      if (result != null)
      {
         return result;
      }

      debug(getMessageWithFallback("message.running", 
        "Running {0}",
        String.format("kpsewhich '%s'", arg)));

      Process process = null;
      int exitCode = -1;

      try
      {
         process = new ProcessBuilder("kpsewhich", arg).start();
         exitCode = process.waitFor();
      }
      catch (Exception e)
      {
         debug(e);
         return null;
      }

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
      else if (debugLevel > 0)
      {
         logAndPrintMessage(getMessageWithFallback("error.app_failed",
           "{0} failed with exit code {1}",
           String.format("kpsewhich '%s'", arg),  exitCode));
      }

      kpsewhichResults.put(arg, line);

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
    * Converts a TeXPath reference to a string with special characters
    * replaced for use as a hyperlink reference. (Never
    * underestimate the determination of users who insist on using
    * problematic characters in their file names.)
    *
    * Not sure if Unicode characters cause a problem with
    * hyperlinks with XeLaTeX/LuaLaTeX, so provide both hex and char. 
    * The default definition of \bibglshrefunicode just does the second
    * argument but can be defined to use the code point instead.
    *
    * The potentially problematic ASCII characters (or Unicode if
    * fontspec not loaded) use \bibglshrefchar instead, which does a
    * literal percent followed by the hexadecimal value. The
    * character is included in the syntax but is ignored by default.
    * \bibglshrefchar can locally be set to \@secondoftwo if the
    * file name needs displaying in the document.
    */  
   public String getTeXPathHref(TeXPath src)
   {
      String path = src.toString();

      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < path.length(); )
      {
         int cp = path.codePointAt(i);
         i += Character.charCount(cp);

         if (cp == '-' || cp == '.' || cp == ':' 
              || (cp >= '/' && cp <= '9') 
              || (cp >= 'A' && cp <= 'Z')
              || (cp >= 'a' && cp <= 'z'))
         {
            builder.appendCodePoint(cp);
         }
         else if (fontspec && cp > 0x7F)
         {
            builder.append(String.format("\\bibglshrefunicode{%02X}{%s}", cp,
              new String(Character.toChars(cp))));
         }
         else if (cp == '\\')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\glsbackslash }", cp));
         }
         else if (cp == '%')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\glspercentchar }", cp));
         }
         else if (cp == '#')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\bibglshashchar }", cp));
         }
         else if (cp == '$')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\bibglsdollarchar }", cp));
         }
         else if (cp == '_')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\bibglsunderscorechar }", cp));
         }
         else if (cp == '&')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\bibglsampersandchar }", cp));
         }
         else if (cp == '^')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\bibglscircumchar }", cp));
         }
         else if (cp == '{')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\glsopenbrace }", cp));
         }
         else if (cp == '}')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\glsclosebrace }", cp));
         }
         else if (cp == '~')
         {
            builder.append(String.format(
             "\\bibglshrefchar{%02X}{\\glstildechar }", cp));
         }
         else
         {
            builder.append(String.format("\\bibglshrefchar{%02X}{%s}", cp,
              new String(Character.toChars(cp))));
         }
      }

      return builder.toString();
   }

   /*
    * TeXApp method (texparserlib.jar) needs defining,
    * but not needed for the purposes of this application.
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
    *  TeXApp method. This is used by the TeX parser library
    *  when substituting deprecated commands, but this is
    *  only likely to occur when obtaining the sort value.
    *  Any deprecated commands in the bib fields will be copied
    *  to the glstex file.
    */ 
   public void substituting(TeXParser parser, String original, 
     String replacement)
   {
      verboseMessage("warning.substituting", original, replacement);
   }

   /*
    *  TeXApp method used for progress updates for long actions,
    *  such as loading datatool files, which isn't relevant here.
    */ 
   public void progress(int percent)
   {
   }

   /*
    *  TeXApp method used for obtaining a message from a given label. 
    */ 

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

      return messages.getMessageIfExists(label);
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
    *  TeXApp method for providing informational messages to the user.
    */ 
   public void message(String text)
   {
      if (verboseLevel >= 0)
      {
         System.out.println(text);
      }

      logMessage(text);
   }

   public void verboseMessage(String key, Object... params)
   {
      verbose(getMessage(key, params));
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

   public static boolean isScriptDigit(int cp)
   {
      return isSubscriptDigit(cp) || isSuperscriptDigit(cp);
   }

   public static boolean isSubscriptDigit(int cp)
   {
      switch (cp)
      {
         case SUBSCRIPT_ZERO:
         case SUBSCRIPT_ONE:
         case SUBSCRIPT_TWO:
         case SUBSCRIPT_THREE:
         case SUBSCRIPT_FOUR:
         case SUBSCRIPT_FIVE:
         case SUBSCRIPT_SIX:
         case SUBSCRIPT_SEVEN:
         case SUBSCRIPT_EIGHT:
         case SUBSCRIPT_NINE:
            return true;
      }

      return false;
   }

   public static boolean isSuperscriptDigit(int cp)
   {
      switch (cp)
      {
         case SUPERSCRIPT_ZERO:
         case SUPERSCRIPT_ONE:
         case SUPERSCRIPT_TWO:
         case SUPERSCRIPT_THREE:
         case SUPERSCRIPT_FOUR:
         case SUPERSCRIPT_FIVE:
         case SUPERSCRIPT_SIX:
         case SUPERSCRIPT_SEVEN:
         case SUPERSCRIPT_EIGHT:
         case SUPERSCRIPT_NINE:
            return true;
      }

      return false;
   }

   public static Integer parseInt(String str)
     throws NumberFormatException
   {
      try
      {
         if (str.codePointAt(0) == MINUS)
         {
            return Integer.valueOf("-"+str.substring(1));
         }

         return Integer.valueOf(str);
      }
      catch (NumberFormatException e)
      {
         StringBuilder builder = new StringBuilder();

         for (int i = 0; i < str.length(); )
         {
            int cp = str.codePointAt(i);
            i += Character.charCount(cp);

            switch (cp)
            {
               case SUPERSCRIPT_ZERO:
               case SUBSCRIPT_ZERO:

                 builder.append('0');

               break;
               case SUPERSCRIPT_ONE:
               case SUBSCRIPT_ONE:

                 builder.append('1');

               break;
               case SUPERSCRIPT_TWO:
               case SUBSCRIPT_TWO:

                 builder.append('2');

               break;
               case SUPERSCRIPT_THREE:
               case SUBSCRIPT_THREE:

                 builder.append('3');

               break;
               case SUPERSCRIPT_FOUR:
               case SUBSCRIPT_FOUR:

                 builder.append('4');

               break;
               case SUPERSCRIPT_FIVE:
               case SUBSCRIPT_FIVE:

                 builder.append('5');

               break;
               case SUPERSCRIPT_SIX:
               case SUBSCRIPT_SIX:

                 builder.append('6');

               break;
               case SUPERSCRIPT_SEVEN:
               case SUBSCRIPT_SEVEN:

                 builder.append('7');

               break;
               case SUPERSCRIPT_EIGHT:
               case SUBSCRIPT_EIGHT:

                 builder.append('8');

               break;
               case SUPERSCRIPT_NINE:
               case SUBSCRIPT_NINE:

                 builder.append('9');

               break;
               case SUPERSCRIPT_PLUS:
               case SUBSCRIPT_PLUS:

                 builder.append('+');

               break;
               case SUPERSCRIPT_MINUS:
               case SUBSCRIPT_MINUS:

                 builder.append('-');

               break;
               default:

                 builder.appendCodePoint(cp);
            }
         }

         return Integer.valueOf(builder.toString());
      }
   }

   /*
    *  TeXApp method for providing warning messages.
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

   public void warningMessage(String key, Object... params)
   {
      warning(getMessage(key, params));
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
    *  TeXApp method for providing error messages.
    */ 
   public void error(Exception e)
   {
      if (e instanceof TeXSyntaxException)
      {
         TeXParser p = ((TeXSyntaxException)e).getParser();

         // Only trigger an error when parsing bib files, otherwise
         // warn.

         if (p == null || p.getListener() instanceof Bib2GlsBibParser)
         {
            error(((TeXSyntaxException)e).getMessage(this));
         }
         else
         {
            warning(((TeXSyntaxException)e).getMessage(this));

            if (debugLevel > 0)
            {
               e.printStackTrace();

               for (StackTraceElement elem : e.getStackTrace())
               {
                  logMessage(elem.toString());
               }
            }

            return;
         }
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
    *  TeXApp method needs defining, but shouldn't be needed for
    *  the purposes of this application. This is mainly used by
    *  the TeX parser library to copy images to a designated output
    *  directory when performing LaTeX -> LaTeX or LaTeX -> HTML
    *  actions.
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
    * TeXApp method needs defining, but unlikely to be needed for
    * the purposes of this application. (It doesn't make any 
    * sense to have something like \read-1 in a bib field.)
    * Just return empty string.
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
      System.out.println("https://github.com/nlct/bib2gls");
      System.out.println();
      System.out.format("Copyright 2017-%s Nicola Talbot%n",
       DATE.substring(0,4));
      System.out.println(getMessage("about.license"));
   }

   public void libraryVersion()
   {
      System.out.println();
      System.out.println(getMessageWithFallback("about.library.version",
        "Bundled with {0} version {1} ({2})", 
        "texparserlib.jar", TeXParser.VERSION, TeXParser.VERSION_DATE));
      System.out.println("https://github.com/nlct/texparser");
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
      System.out.println(getMessage("syntax.locale", "--locale", "-l"));
      System.out.println(getMessage("syntax.log", "--log-file", "-t"));
      System.out.println(getMessage("syntax.dir", "--dir", "-d"));

      System.out.println();
      System.out.println(getMessage("syntax.interpret", "--interpret"));
      System.out.println(getMessage("syntax.no.interpret", "--no-interpret"));
      System.out.println();

      System.out.println();
      System.out.println(getMessage("syntax.break.space", "--break-space"));
      System.out.println(getMessage("syntax.no.break.space", "--no-break-space"));

      System.out.println();
      System.out.println(getMessage("syntax.cite.as.record", 
        "--cite-as-record"));
      System.out.println(getMessage("syntax.no.cite.as.record", 
        "--no-cite-as-record"));

      System.out.println();
      System.out.println(getMessage("syntax.warn.non.bib.fields", 
         "--warn-non-bib-fields"));
      System.out.println(getMessage("syntax.no.warn.non.bib.fields", 
         "--no-warn-non-bib-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.warn.unknown.entry.types", 
         "--warn-unknown-entry-types"));
      System.out.println(getMessage("syntax.no.warn.unknown.entry.types", 
         "--no-warn-unknown-entry-types"));

      System.out.println();
      System.out.println(getMessage("syntax.merge.wrglossary.records", 
        "--merge-wrglossary-records"));
      System.out.println(getMessage("syntax.no.merge.wrglossary.records", 
        "--no-merge-wrglossary-records"));

      System.out.println();
      System.out.println(getMessage("syntax.merge.nameref.on",
         "--merge-nameref-on"));

      System.out.println();
      System.out.println(getMessage("syntax.force.cross.resource.refs",
         "--force-cross-resource-refs", "-x"));
      System.out.println(getMessage("syntax.no.force.cross.resource.refs",
         "--no-force-cross-resource-refs"));

      System.out.println();
      System.out.println(getMessage("syntax.support.unicode.script",
        "--support-unicode-script"));
      System.out.println(getMessage("syntax.no.support.unicode.script",
        "--no-support-unicode-script"));
      System.out.println();
      System.out.println(getMessage("syntax.packages", "--packages", "-p"));
      System.out.println();
      System.out.println(getMessage("syntax.ignore.packages", 
        "--ignore-packages", "-k"));
      System.out.println();
      System.out.println(getMessage("syntax.custom.packages", 
        "--custom-packages"));
      System.out.println();
      System.out.println(getMessage("syntax.list.known.packages", 
        "--list-known-packages"));
      System.out.println();

      System.out.println();
      System.out.println(getMessage("syntax.mfirstuc",
         "--mfirstuc-protection", "-u"));

      System.out.println();
      System.out.println(getMessage("syntax.no.mfirstuc",
         "--no-mfirstuc-protection"));

      System.out.println();
      System.out.println(getMessage("syntax.math.mfirstuc",
         "--mfirstuc-math-protection", "--no-mfirstuc-protection"));

      System.out.println();
      System.out.println(getMessage("syntax.no.math.mfirstuc",
         "--no-mfirstuc-math-protection"));

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
      System.out.println(getMessage("syntax.record.count",
         "--record-count", "-c"));

      System.out.println();
      System.out.println(getMessage("syntax.no.record.count",
         "--no-record-count", "--no-record-count-unit"));

      System.out.println();
      System.out.println(getMessage("syntax.record.count.unit",
         "--record-count-unit", "-n", "--record-count"));

      System.out.println();
      System.out.println(getMessage("syntax.no.record.count.unit",
         "--no-record-count-unit"));

      System.out.println();
      System.out.println(getMessage("syntax.trim.fields",
         "--trim-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.trim.only.fields",
         "--trim-only-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.trim.except.fields",
         "--trim-except-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.no.trim.fields",
         "--no-trim-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.expand.fields",
         "--expand-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.no.expand.fields",
         "--no-expand-fields"));

      System.out.println();
      System.out.println(getMessage("syntax.provide.glossaries",
         "--provide-glossaries"));

      System.out.println();
      System.out.println(getMessage("syntax.no.provide.glossaries",
         "--no-provide-glossaries"));

      System.out.println();
      System.out.println(getMessage("syntax.tex.encoding",
         "--tex-encoding"));

      System.exit(0);
   }

   private String getLanguageFileName(String tag)
   {
      return String.format("/resources/bib2gls-%s.xml", tag);
   }

   private void initMessages(String langTag) throws Bib2GlsException,IOException
   {
      Locale locale;

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
         try
         {
            setDocDefaultLocale(langTag);
            locale = getDefaultLocale();
         }
         catch (IllformedLocaleException e)
         {
            locale = Locale.getDefault();
            setDocDefaultLocale(locale);
            error(e.getMessage());
            debug(e);
         }
      }

      String lang = locale.toLanguageTag();

      String name = getLanguageFileName(lang);

      URL url = getClass().getResource(name);

      String jar = null;

      if (debugLevel > 0)
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

   private void setShortCuts(String value)
   {
      if (value.equals("acro") || value.equals("acronyms")
        || value.equals("ac"))
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

   public void setDocDefaultLocale(Locale locale)
   {
      docLocale = locale.toLanguageTag();
      defaultLocale = locale;
   }

   public void setDocDefaultLocale(String languageTag)
    throws IllformedLocaleException
   {
      docLocale = languageTag;
      defaultLocale = new Locale.Builder().setLanguageTag(languageTag).build();
   }

   public String getDocDefaultLocale()
   {
      return docLocale;
   }

   public Locale getDefaultLocale()
   {
      return defaultLocale;
   }

   public Locale getLocale(String langTag)
   {
      try
      {
         return new Locale.Builder().setLanguageTag(langTag).build();
      }
      catch (IllformedLocaleException e)
      {
         Locale locale = defaultLocale;

         if (locale == null)
         {
            locale = Locale.getDefault();
         }

         warningMessage("warning.invalid.locale", langTag, locale);
         return locale;
      }
   }

   public void setTrimFields(boolean trimFields)
   {
      this.trimFields = trimFields;
   }

   @Deprecated
   public boolean trimFields()
   {
      return trimFields;
   }

   public boolean isTrimFieldOn(String field)
   {
      if (trimOnlyFields == null && trimExceptFields == null)
      {
         return trimFields;
      }

      if (trimOnlyFields != null)
      {
         return trimOnlyFields.contains(field);
      }

      return !trimExceptFields.contains(field);
   }

   private static int parseArgVal(String[] args, int i, Object[] argVal)
   {
      String[] sp; 

      if (args[i].startsWith("--"))
      {
         sp = args[i].split("=", 2);
      }
      else
      {
         sp = new String[]{args[i]};
      }

      argVal[0] = sp[0];

      if (sp.length == 2)
      {
         argVal[1] = sp[1];
         return i;
      }

      if (i == args.length-1 || args[i+1].startsWith("-"))
      {
         argVal[1] = null;
         return i; 
      }

      argVal[1] = args[++i];

      return i;
   }

   private int parseArgInt(String[] args, int i, Object[] argVal)
   {
      return parseArgInt(args, i, argVal, this);
   }

   private int parseArgInt(String[] args, int i, Object[] argVal,
     int defVal)
   {
      return parseArgInt(args, i, argVal, defVal, this);
   }

   private static int parseArgInt(String[] args, int i, Object[] argVal,
     Bib2Gls bib2gls)
   {
      i = parseArgVal(args, i, argVal);

      if (argVal[1] != null)
      {
         try
         {
            argVal[1] = new Integer((String)argVal[1]);
         }
         catch (NumberFormatException e)
         {
            if (bib2gls == null)
            {
               throw new IllegalArgumentException("Invalid integer argument",
                 e);
            }
            else
            {
               throw new IllegalArgumentException(bib2gls.getMessage(
                 "error.invalid.opt.int.value", argVal[0], argVal[1]), e);
            }
         }
      }

      return i;
   }

   private static int parseArgInt(String[] args, int i, Object[] argVal,
     int defVal, Bib2Gls bib2gls)
   {
      i = parseArgVal(args, i, argVal);

      if (argVal[1] == null)
      {
         argVal[1] = new Integer(defVal);
      }
      else
      {
         try
         {
            argVal[1] = new Integer((String)argVal[1]);
         }
         catch (NumberFormatException e)
         {
            argVal[1] = new Integer(defVal);
            return i-1;
         }
      }

      return i;
   }

   private static boolean isArg(String arg, String shortArg, String longArg)
   {
      return arg.equals("-"+shortArg) || arg.equals("--"+longArg) 
        || arg.startsWith("--"+longArg+"=");
   }

   private static boolean isArg(String arg, String longArg)
   {
      return arg.equals("--"+longArg) || arg.startsWith("--"+longArg+"=");
   }

   private void parseArgs(String[] args)
     throws IOException,Bib2GlsSyntaxException
   {
      String dirName = null;
      String auxFileName = null;
      String logName = null;
      boolean provideknownGlossaries=false;

      Object[] argVal = new Object[2];

      for (int i = 0; i < args.length; i++)
      {
         if (isArg(args[i], "debug"))
         {
            i = parseArgInt(args, i, argVal, 1);

            int level = ((Integer)argVal[1]).intValue();

            if (level < 0)
            {
               throw new IllegalArgumentException(getMessage(
                 "error.invalid.opt.minint.value", argVal[0],
                   level, 0));
            }

            debugLevel = level;
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
         else if (isArg(args[i], "l", "locale"))
         {
            // already dealt with, but need to increment index and
            // perform syntax check.

            i = parseArgVal(args, i, argVal);

            if (argVal[1] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }
         }
         else if (args[i].equals("-h") || args[i].equals("--help"))
         {
            help();
         }
         else if (args[i].equals("-v") || args[i].equals("--version"))
         {
            if (!shownVersion)
            {
               version();
            }

            license();
            libraryVersion();
            System.exit(0);
         }
         else if (isArg(args[i], "t", "log-file"))
         {
            i = parseArgVal(args, i, argVal);

            if (argVal[1] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            logName = (String)argVal[1];
         }
         else if (isArg(args[i], "p", "packages"))
         {
            i = parseArgVal(args, i, argVal);

            if (argVal[1] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            String[] styList = ((String)argVal[1]).trim().split("\\s*,\\s*");

            for (String sty : styList)
            {
               if (isKnownPackage(sty))
               {
                  packages.add(sty);
               }
               else
               {
                  throw new Bib2GlsSyntaxException(
                     getMessage("error.unsupported.package", sty, 
                     "--custom-packages"));
               }
            }
         }
         else if (isArg(args[i], "custom-packages"))
         {
            i = parseArgVal(args, i, argVal);

            if (argVal[1] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            String[] styList = ((String)argVal[1]).trim().split("\\s*,\\s*");

            if (customPackages == null)
            {
               customPackages = new Vector<String>();
            }

            for (String sty : styList)
            {
               if (isKnownPackage(sty))
               {
                  throw new Bib2GlsSyntaxException(
                     getMessage("error.supported.package", sty, 
                     "--packages"));
               }
               else
               {
                  customPackages.add(sty);
               }
            }
         }
         else if (isArg(args[i], "k", "ignore-packages"))
         {
            i = parseArgVal(args, i, argVal);

            if (argVal[1] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            String[] styList = ((String)argVal[1]).trim().split("\\s*,\\s*");

            if (ignorePackages == null)
            {
               ignorePackages = new Vector<String>();
            }

            for (String sty : styList)
            {
               if (isKnownPackage(sty))
               {
                  ignorePackages.add(sty);
               }
               else
               {
                  warningMessage("error.invalid.opt.value", argVal[0], sty);
               }
            }
         }
         else if (args[i].equals("--list-known-packages"))
         {
            System.out.println(getMessage("message.list.known.packages.auto"));

            for (int j = 0; j < AUTO_SUPPORT_PACKAGES.length; j++)
            {
               if (j%5 == 0)
               {
                  if (j > 0)
                  {
                     System.out.print(",");
                  }

                  System.out.println();
                  System.out.print("\t");
               }
               else
               {
                  System.out.print(", ");
               }

               System.out.print(AUTO_SUPPORT_PACKAGES[j]);
            }

            System.out.println();
            System.out.println();

            System.out.println(getMessage("message.list.known.packages.extra"));

            for (int j = 0; j < EXTRA_SUPPORTED_PACKAGES.length; j++)
            {
               if (j%5 == 0)
               {
                  if (j > 0)
                  {
                     System.out.print(",");
                  }

                  System.out.println();
                  System.out.print("\t");
               }
               else
               {
                  System.out.print(", ");
               }

               System.out.print(EXTRA_SUPPORTED_PACKAGES[j]);
            }

            System.out.println();

            System.out.println(getMessage("message.list.known.packages.info"));

            System.exit(0);
         }
         else if (args[i].equals("--expand-fields"))
         {
            expandFields = true;
         }
         else if (args[i].equals("--no-expand-fields"))
         {
            expandFields = false;
         }
         else if (args[i].equals("--warn-non-bib-fields"))
         {
            checkNonBibFields = true;
         }
         else if (args[i].equals("--no-warn-non-bib-fields"))
         {
            checkNonBibFields = false;
         }
         else if (args[i].equals("--warn-unknown-entry-types"))
         {
            warnUnknownEntryTypes = true;
         }
         else if (args[i].equals("--no-warn-unknown-entry-types"))
         {
            warnUnknownEntryTypes = false;
         }
         else if (args[i].equals("--interpret"))
         {
            interpret = true;
         }
         else if (args[i].equals("--no-interpret"))
         {
            interpret = false;
         }
         else if (args[i].equals("--break-space"))
         {
            useNonBreakSpace = false;
         }
         else if (args[i].equals("--no-break-space"))
         {
            useNonBreakSpace = true;
         }
         else if (args[i].equals("--cite-as-record"))
         {
            useCiteAsRecord = true;
         }
         else if (args[i].equals("--no-cite-as-record"))
         {
            useCiteAsRecord = false;
         }
         else if (args[i].equals("--merge-wrglossary-records"))
         {
            mergeWrGlossaryLocations = true;
         }
         else if (args[i].equals("--no-merge-wrglossary-records"))
         {
            mergeWrGlossaryLocations = false;
         }
         else if (isArg(args[i], "merge-nameref-on"))
         {
            i = parseArgVal(args, i, argVal);

            String arg = (String)argVal[1];

            if (arg == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            if (arg.equals("href"))
            {
               mergeNameRefOn = MERGE_NAMEREF_ON_HREF;
            }
            else if (arg.equals("title"))
            {
               mergeNameRefOn = MERGE_NAMEREF_ON_TITLE;
            }
            else if (arg.equals("location"))
            {
               mergeNameRefOn = MERGE_NAMEREF_ON_LOCATION;
            }
            else if (arg.equals("hcounter"))
            {
               mergeNameRefOn = MERGE_NAMEREF_ON_HCOUNTER;
            }
            else
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.invalid.choice.value", argVal[0], arg));
            }
         }
         else if (args[i].equals("--force-cross-resource-refs") 
                   || args[i].equals("-x"))
         {
            forceCrossResourceRefs = true;
         }
         else if (args[i].equals("--no-force-cross-resource-refs"))
         {
            forceCrossResourceRefs = false;
         }
         else if (args[i].equals("--support-unicode-script"))
         {
            supportUnicodeSubSuperScripts = true;
         }
         else if (args[i].equals("--no-support-unicode-script"))
         {
            supportUnicodeSubSuperScripts = false;
         }
         else if (args[i].equals("--no-mfirstuc-protection"))
         {
            mfirstucProtect = false;
         }
         else if (isArg(args[i], "u", "mfirstuc-protection"))
         {
            i = parseArgVal(args, i, argVal);

            mfirstucProtect = true;

            String arg = (String)argVal[1];

            if (arg == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            if (arg.equals("all"))
            {
               mfirstucProtectFields = null;
            }
            else if (arg.isEmpty())
            {
               mfirstucProtect = false;
            }
            else
            {
               mfirstucProtectFields = arg.split(" *, *");
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
         else if (isArg(args[i], "shortcuts"))
         {
            i = parseArgVal(args, i, argVal);

            String arg = (String)argVal[1];

            if (arg == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            try
            {
               setShortCuts(arg);
            }
            catch (IllegalArgumentException e)
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.invalid.choice.value", 
                 argVal[0], arg), e);
            }
         }
         else if (isArg(args[i], "nested-link-check"))
         {
            i = parseArgVal(args, i, argVal);

            String arg = (String)argVal[1];

            if (arg == null)
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.missing.value", argVal[0]));
            }

            if (arg.equals("none") || arg.isEmpty())
            {
               nestedLinkCheckFields = null;
            }
            else
            {
               nestedLinkCheckFields = arg.split(" *, *");
            }
         }
         else if (args[i].equals("--no-nested-link-check"))
         {
            nestedLinkCheckFields = null;
         }
         else if (isArg(args[i], "d", "dir"))
         {
            i = parseArgVal(args, i, argVal);

            if (argVal[1] == null)
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.missing.value", argVal[0]));
            }

            dirName = (String)argVal[1];
         }
         else if (isArg(args[i], "m", "map-format"))
         {
            i = parseArgVal(args, i, argVal);

            String arg = (String)argVal[1];

            if (arg == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            String[] split = arg.trim().split(" *, *");

            for (String value : split)
            {
               String[] values = value.split(" *: *");

               if (values.length != 2)
               {
                  throw new Bib2GlsSyntaxException(
                    getMessage("error.invalid.opt.value", argVal[0], arg));
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
         else if (args[i].equals("--record-count") || args[i].equals("-c"))
         {
            saveRecordCount = true;
         }
         else if (args[i].equals("--no-record-count"))
         {
            saveRecordCount = false;
            saveRecordCountUnit = false;
         }
         else if (args[i].equals("--record-count-unit") || args[i].equals("-n"))
         {
            saveRecordCountUnit = true;
            saveRecordCount = true;
         }
         else if (args[i].equals("--no-record-count-unit"))
         {
            saveRecordCountUnit = false;
         }
         else if (isArg(args[i], "tex-encoding"))
         {
            i = parseArgVal(args, i, argVal);

            String arg = (String)argVal[1];

            if (arg == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            texCharset = Charset.forName(arg);
         }
         else if (args[i].equals("--trim-fields"))
         {
            trimFields = true;
            trimOnlyFields = null;
            trimExceptFields = null;
         }
         else if (args[i].equals("--no-trim-fields"))
         {
            trimFields = false;
            trimOnlyFields = null;
            trimExceptFields = null;
         }
         else if (isArg(args[i], "trim-only-fields"))
         {
            if (trimExceptFields != null)
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.option.clash",
                  "--trim-only-fields", "--trim-except-fields"));
            }

            i = parseArgVal(args, i, argVal);

            if (argVal[1] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            String[] fieldList = ((String)argVal[1]).trim().split("\\s*,\\s*");

            if (trimOnlyFields == null)
            {
               trimOnlyFields = new Vector<String>();
            }

            for (String field : fieldList)
            {
               trimOnlyFields.add(field);
            }

            trimFields = true;
         }
         else if (isArg(args[i], "trim-except-fields"))
         {
            if (trimOnlyFields != null)
            {
               throw new Bib2GlsSyntaxException(
                 getMessage("error.option.clash",
                  "--trim-only-fields", "--trim-except-fields"));
            }

            i = parseArgVal(args, i, argVal);

            if (argVal[1] == null)
            {
               throw new Bib2GlsSyntaxException(
                  getMessage("error.missing.value", argVal[0]));
            }

            String[] fieldList = ((String)argVal[1]).trim().split("\\s*,\\s*");

            if (trimExceptFields == null)
            {
               trimExceptFields = new Vector<String>();
            }

            for (String field : fieldList)
            {
               trimExceptFields.add(field);
            }

            trimFields = true;
         }
         else if (args[i].equals("--provide-glossaries"))
         {
            provideknownGlossaries=true;
         }
         else if (args[i].equals("--no-provide-glossaries"))
         {
            provideknownGlossaries=false;
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

      if (provideknownGlossaries)
      {
         knownGlossaries = new Vector<String>();
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
      }
      catch (IOException e)
      {
         logWriter = null;
         System.err.println(getMessage("error.cant.open.log", 
            logFile.toString()));
         error(e);
      }

      logMessage(getMessage("about.version", NAME, VERSION, DATE));

      if (getDebugLevel() > 0)
      {
         logMessage("Java "+System.getProperty("java.version"));
         logMessage(String.format("texparserlib.jar %s (%s)",
            TeXParser.VERSION, TeXParser.VERSION_DATE));
      }

      if (logWriter != null)
      {
         logWriter.print(pending.toString());
      }

      pendingWriter.close();
      pendingWriter = null;
      pending = null;

      if (getDebugLevel() > 0)
      {
         logAndPrintMessage(String.format(
            "openin_any=%s%nopenout_any=%s%nTEXMFOUTPUT=%s%ncwd=%s", 
             openin_any, openout_any, 
             texmfoutput == null ? "" : texmfoutput,
             cwd));
      }
   }

   public static void exit(Bib2Gls bib2gls, int exitCode)
   {
      if (bib2gls != null)
      {
         // Make sure transcript file is closed

         if (bib2gls.logWriter != null)
         {
            bib2gls.logWriter.close();
            bib2gls.message(bib2gls.getMessageWithFallback(
              "message.log.file", 
              "Transcript written to {0}.",
               bib2gls.logFile));

            bib2gls.logWriter = null;
         }
      }

      System.exit(exitCode);
   }

   public static void main(String[] args)
   {
      Bib2Gls bib2gls = null;
      int debug = 0;
      int verbose = 0;
      String langTag = null;
      Object[] argVal = new Object[2];

      // Quickly check for options that are needed before parseArgs().

      for (int i = 0; i < args.length; i++)
      {
         if (isArg(args[i], "debug"))
         {
            try
            {
               i = parseArgInt(args, i, argVal, 1, null);

               debug = ((Integer)argVal[1]).intValue();
            }
            catch (Exception e)
            {
               debug = 1;
            }
         }
         else if (args[i].equals("--no-debug") || args[i].equals("--nodebug"))
         {
            debug = 0;
         }
         else if (args[i].equals("--silent"))
         {
            verbose = -1;
         }
         else if (args[i].equals("--verbose"))
         {
            verbose = 1;
         }
         else if (args[i].equals("--noverbose"))
         {
            verbose = 0;
         }
         else if (isArg(args[i], "l", "locale"))
         {
            i = parseArgVal(args, i, argVal);

            langTag = (String)argVal[1];
         }
      }

      try
      {
         bib2gls = new Bib2Gls(debug, verbose, langTag);

         bib2gls.process(args);

         if (bib2gls.exitCode != 0)
         {
            exit(bib2gls, bib2gls.exitCode);
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

         exit(bib2gls, 3);
      }
   }

   public static final String NAME = "bib2gls";
   public static final String VERSION = "2.7";
   public static final String DATE = "2020-07-11";
   public int debugLevel = 0;
   public int verboseLevel = 0;

   private char openout_any='p';
   private char openin_any='a';
   private Path cwd;
   private Path texmfoutput = null;
   private Path basePath;

   private File auxFile;
   private File logFile;
   private PrintWriter logWriter=null;
   private StringWriter pending = null;
   private PrintWriter pendingWriter = null;

   public static final Pattern PATTERN_PACKAGE = Pattern.compile(
       "Package: ([^\\s]+)(?:\\s+(\\d{4})/(\\d{2})/(\\d{2}))?.*");

   public static final Pattern PATTERN_ENCDEF = Pattern.compile(
       "File: ([^ ]+)enc\\.def .*");

   private Vector<String> fontencList = null;

   private boolean fontspec = false;
   private boolean hyperref = false;
   private boolean createHyperGroups = true;
   private boolean checkNonBibFields = true;
   private boolean warnUnknownEntryTypes = true;

   private int altModifier = -1;

   private Vector<GlsResource> glsresources;
   private Vector<String> fields;
   private Vector<GlsRecord> records;
   private Vector<GlsSeeRecord> seeRecords;
   private Vector<String> selectedEntries;

   private Vector<String> knownGlossaries=null;

   public static final String[] SPAWN_SPECIAL_FIELDS =
    new String[] {"progeny", "progenitor", "adoptparents"};

   public static final String[] DUAL_SPECIAL_FIELDS =
    new String[] {"dualprefix", "dualprefixplural", "dualprefixfirst", "dualprefixfirstplural"};

   /* Not including: 'nonumberlist', 'sort', 'type', 'bibtextype',
    * 'counter'.
    */ 
   private static final String[] NON_BIB_FIELDS =
    new String[] {"bibtexcontributor", "bibtexentry", "dual", "group",
     "progenitor", "progeny", "secondarygroup",
     "secondarysort"};

   private static final String[] PRIVATE_NON_BIB_FIELDS =
    new String[] {"childcount", "childlist", "siblingcount", "siblinglist",
     "indexcounter", "originalid", "originalentrytype", "location", "loclist",
     "primarylocations", "recordcount", "currcount", "desc", "descplural", "firstpl",
     "flag", "index", "level", "longpl", "prevcount", "prevunitmax",
     "prevunittotal", "shortpl", "sortvalue", "unitlist", "useri",
     "userii", "useriii", "useriv", "userv", "uservi"
    };

   private HashMap<String,String> glsLike;

   private HashMap<String,String> fieldMap;

   private HashMap<String,String> formatMap;

   private HashMap<GlsRecord,Integer> recordCount=null;

   private Vector<File> texFiles;

   private boolean addGroupField = false, saveRecordCount=false,
     saveRecordCountUnit=false, commonCommandsDone=false;

   private Charset texCharset = null;

   private boolean useCiteAsRecord=false;

   private boolean mergeWrGlossaryLocations = true;

   private byte mergeNameRefOn = MERGE_NAMEREF_ON_HCOUNTER;

   private static final byte MERGE_NAMEREF_ON_HREF=(byte)0;
   private static final byte MERGE_NAMEREF_ON_TITLE=(byte)1;
   private static final byte MERGE_NAMEREF_ON_LOCATION=(byte)2;
   private static final byte MERGE_NAMEREF_ON_HCOUNTER=(byte)3;

   private Bib2GlsMessages messages;

   private boolean mfirstucProtect = true;
   private boolean mfirstucMProtect = true;
   private String[] mfirstucProtectFields = null;

   private String shortcuts=null;

   private boolean checkAcroShortcuts = false;
   private boolean checkAbbrvShortcuts = false;

   private GlsResource currentResource = null;

   private String docLocale = null;
   private Locale defaultLocale = null;

   private boolean trimFields = false;

   private Vector<String> trimOnlyFields = null, trimExceptFields = null;

   private boolean expandFields = false;

   private boolean interpret = true;

   private Vector<String> packages = null, ignorePackages=null,
      customPackages = null;

   public static final String[] AUTO_SUPPORT_PACKAGES = new String[]
    { 
      "amsmath", "amssymb", "bpchem", "fontenc", "fontspec", 
      "fourier", "hyperref", "lipsum", "mhchem", "MnSymbol", 
      "natbib", "pifont", "siunitx", "stix", "textcase", 
      "textcomp", "tipa", "upgreek", "wasysym"
    };

   public static final String[] EXTRA_SUPPORTED_PACKAGES = new String[]
    { "booktabs", "color", "datatool-base", "datatool", "etoolbox",
      "graphics", "graphicx", "ifthen", "jmlrutils", 
      "mfirstuc-english", "probsoln", "shortvrb", "xspace"
    };

   private TeXParser interpreter = null;

   private boolean useNonBreakSpace = true;

   private boolean forceCrossResourceRefs = false;

   // \textsuperscript and \textsubscript will use Unicode
   // super/subscript characters if possible if true:
   private boolean supportUnicodeSubSuperScripts=true;

   private boolean multiSuppSupported=false;

   private String glossariesExtraVersion="????/??/??";

   private Vector<String> dependencies = null;

   private HashMap<String,String> kpsewhichResults;

   private int exitCode;

   private boolean shownVersion = false;

   private String[] nestedLinkCheckFields = new String[]
    {"name", "text", "plural", "first", "firstplural",
     "long", "longplural", "short", "shortplural", "symbol"};

   public static final String MIN_MULTI_SUPP_VERSION="1.36";
   public static final int MIN_MULTI_SUPP_YEAR=2018;
   public static final int MIN_MULTI_SUPP_MONTH=8;
   public static final int MIN_MULTI_SUPP_DAY=18;

   public static final int SUPERSCRIPT_ZERO=0x2070;
   public static final int SUPERSCRIPT_ONE=0x00B9;
   public static final int SUPERSCRIPT_TWO=0x00B2;
   public static final int SUPERSCRIPT_THREE=0x00B3;
   public static final int SUPERSCRIPT_FOUR=0x2074;
   public static final int SUPERSCRIPT_FIVE=0x2075;
   public static final int SUPERSCRIPT_SIX=0x2076;
   public static final int SUPERSCRIPT_SEVEN=0x2077;
   public static final int SUPERSCRIPT_EIGHT=0x2078;
   public static final int SUPERSCRIPT_NINE=0x2079;
   public static final int SUPERSCRIPT_PLUS=0x207A;
   public static final int SUPERSCRIPT_MINUS=0x207B;

   public static final int SUBSCRIPT_ZERO=0x2080;
   public static final int SUBSCRIPT_ONE=0x2081;
   public static final int SUBSCRIPT_TWO=0x2082;
   public static final int SUBSCRIPT_THREE=0x2083;
   public static final int SUBSCRIPT_FOUR=0x2084;
   public static final int SUBSCRIPT_FIVE=0x2085;
   public static final int SUBSCRIPT_SIX=0x2086;
   public static final int SUBSCRIPT_SEVEN=0x2087;
   public static final int SUBSCRIPT_EIGHT=0x2088;
   public static final int SUBSCRIPT_NINE=0x2089;
   public static final int SUBSCRIPT_PLUS=0x208A;
   public static final int SUBSCRIPT_MINUS=0x208B;

   public static final int MINUS=0x2212;

   public static final String SUBSCRIPT_INT_PATTERN =
     String.format("[%c%c]?[%c%c%c%c%c%c%c%c%c%c]+",
           SUBSCRIPT_PLUS, SUBSCRIPT_MINUS, SUBSCRIPT_ZERO,
           SUBSCRIPT_ONE, SUBSCRIPT_TWO, SUBSCRIPT_THREE, 
           SUBSCRIPT_FOUR, SUBSCRIPT_FIVE, SUBSCRIPT_SIX, 
           SUBSCRIPT_SEVEN, SUBSCRIPT_EIGHT, SUBSCRIPT_NINE);

   public static final String SUPERSCRIPT_INT_PATTERN =
     String.format("[%c%c]?[%c%c%c%c%c%c%c%c%c%c]+",
           SUPERSCRIPT_PLUS, SUPERSCRIPT_MINUS, SUPERSCRIPT_ZERO,
           SUPERSCRIPT_ONE, SUPERSCRIPT_TWO, SUPERSCRIPT_THREE, 
           SUPERSCRIPT_FOUR, SUPERSCRIPT_FIVE, SUPERSCRIPT_SIX, 
           SUPERSCRIPT_SEVEN, SUPERSCRIPT_EIGHT, SUPERSCRIPT_NINE);

   public static final Pattern INT_PATTERN
     = Pattern.compile(String.format(
        "([+-%c]?\\p{javaDigit}+|%s|%s).*",
           MINUS,
           SUBSCRIPT_INT_PATTERN,
           SUPERSCRIPT_INT_PATTERN));
}
