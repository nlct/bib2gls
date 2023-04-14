/*
    Copyright (C) 2017-2022 Nicola L.C. Talbot
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

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsAt extends At
{
   public Bib2GlsAt(GlsResource theResource)
   {
      super();
      this.resource = theResource;
   }

   protected void process(TeXParser parser, TeXObjectList entryTypeList,
     TeXObject contents, TeXObject eg)
     throws IOException
   {
      if (!(contents instanceof TeXObjectList))
      {
         throw new BibTeXSyntaxException(parser,
           BibTeXSyntaxException.ERROR_EXPECTING_OR, "{", "(");
      }

      Bib2GlsBibParser bibParser = (Bib2GlsBibParser)parser.getListener();

      Bib2Gls bib2gls = (Bib2Gls)bibParser.getTeXApp();

      String entryType = entryTypeList.toString(parser).trim().toLowerCase();
      String originalEntryType = entryType;

      entryType = resource.mapEntryType(entryType);

      if (entryType.startsWith("spawned"))
      {
         bib2gls.warning(parser,
            bib2gls.getMessage("warning.private.entry.type", 
            entryType, "spawn"+entryType.substring(7)));
      }

      BibData data;

      if (entryType.equals("compoundset"))
      {
         data = new AtCompoundSet(resource, entryType);
      }
      else
      {
         data = createBib2GlsEntry(bib2gls, entryType);
      }

      if (data == null)
      {
         String unknownEntryType = resource.getUnknownEntryMap();

         if (unknownEntryType != null 
              && !entryType.equals("preamble") 
              && !entryType.equals("string") 
              && !entryType.equals("comment"))
         {
            entryType = unknownEntryType;
            data = createBib2GlsEntry(bib2gls, entryType);
         }

         if (data == null)
         {
            data = BibData.createBibData(entryType);

            if (data instanceof BibEntry && bib2gls.isWarnUnknownEntryTypesOn())
            {
               bib2gls.warning(parser,
                  bib2gls.getMessage("warning.ignoring.unknown.entry.type", 
                  entryType));
            }
         }
      }

      try
      {
         data.parseContents(parser, (TeXObjectList)contents, eg);
      }
      catch (BibTeXSyntaxException e)
      {
         String id = null;
         StringBuilder builder = null;

         if (data instanceof BibEntry)
         {
            id = ((BibEntry)data).getId();

            if (id == null)
            {
               throw new IOException(bib2gls.getMessage(
                 "error.invalid.id", e.getMessage(bib2gls)), e);
            }

            if (containsSpecialChars(id))
            {
               bib2gls.warning(parser, bib2gls.getMessage(
                "warning.spchars.id", id));
            }

            if (!bib2gls.hasNonASCIILabelSupport())
            {
               if (containsExtendedChars(id))
               {
                  bib2gls.warning(parser, bib2gls.getMessage(
                   "warning.notbasiclatin.id", id));
               }
            }
         }

         builder = new StringBuilder();

         for (TeXObject obj : (TeXObjectList)contents)
         {
            builder.append(obj.toString(parser));

            if (obj instanceof Group)
            {
               break;
            }
         }

         if (builder != null && builder.length() > 0)
         {
            if (id == null)
            {
               throw new IOException(bib2gls.getMessage(
                  "error.bib.contents.parse.before",
                  data.getEntryType(), e.getMessage(bib2gls), builder), e);
            }
            else
            {
               throw new IOException(bib2gls.getMessage(
                  "error.bib.contents.parse.data.before",
                  data.getEntryType(), id, e.getMessage(bib2gls), builder), e);
            }
         }
         else if (id != null)
         {
            throw new IOException(bib2gls.getMessage(
               "error.bib.contents.parse.data",
               data.getEntryType(), id, e.getMessage(bib2gls)), e);
         }
         else
         {
            throw new IOException(bib2gls.getMessage(
               "error.bib.contents.parse",
               data.getEntryType(), e.getMessage(bib2gls)), e);
         }
      }

      if (data instanceof BibPreamble)
      {
         if (bib2gls.getDebugLevel() > 0 
            && !entryType.equals(originalEntryType))
         {
            bib2gls.debug(String.format("@%s => @preamble", originalEntryType));
         }

         BibValueList preamble = ((BibPreamble)data).getPreamble();
         TeXObjectList list = preamble.expand(parser);

         resource.setPreamble(list.toString(parser), preamble);
      }
      else if (data instanceof Bib2GlsEntry)
      {
         ((Bib2GlsEntry)data).setOriginalEntryType(originalEntryType);
         String id = ((Bib2GlsEntry)data).getId();

         if (bib2gls.getDebugLevel() > 0 
            && !entryType.equals(originalEntryType))
         {
            bib2gls.debug(String.format("@%s{%s} => @%s{%s}", originalEntryType,
              id, entryType, id));
         }

         if (bibParser.getBibEntry(id) != null)
         {
            throw new BibTeXSyntaxException(parser,
              BibTeXSyntaxException.ERROR_REPEATED_ENTRY, id);
         } 

         ((Bib2GlsEntry)data).setBase(bibParser.getBaseFile());
      }

      bibParser.addBibData(data);

      if (data instanceof Bib2GlsMultiEntry)
      {
         ((Bib2GlsMultiEntry)data).populate(bibParser);
      }
   }

   public static Bib2GlsEntry createBib2GlsEntry(Bib2Gls bib2gls,
      String entryType)
   {
      if (entryType.matches("^(spawned)?entry$"))
      {
         return new Bib2GlsEntry(bib2gls, entryType);
      }
      else if (entryType.matches("^(spawned)?index$"))
      {
         return new Bib2GlsIndex(bib2gls, entryType);
      }
      else if (entryType.matches("^(spawned)?indexplural$"))
      {
         return new Bib2GlsIndexPlural(bib2gls, entryType);
      }
      else if (entryType.matches("^(spawned)?(acronym|abbreviation)$"))
      {
         return new Bib2GlsAbbrev(bib2gls, entryType);
      }
      else if (entryType.matches("^(spawned)?(symbol|number)$"))
      {
         return new Bib2GlsSymbol(bib2gls, entryType);
      }
      else if (entryType.equals("dualentry"))
      {
         return new Bib2GlsDualEntry(bib2gls, entryType);
      }
      else if (entryType.equals("dualentryabbreviation"))
      {
         return new Bib2GlsDualEntryAbbrev(bib2gls, entryType);
      }
      else if (entryType.equals("dualabbreviationentry"))
      {
         return new Bib2GlsDualAbbrevEntry(bib2gls, entryType);
      }
      else if (entryType.equals("dualindexentry"))
      {
         return new Bib2GlsDualIndexEntry(bib2gls, entryType);
      }
      else if (entryType.equals("dualindexsymbol"))
      {
         return new Bib2GlsDualIndexSymbol(bib2gls, entryType);
      }
      else if (entryType.equals("dualindexnumber"))
      {
         return new Bib2GlsDualIndexSymbol(bib2gls, entryType, "number");
      }
      else if (entryType.equals("dualindexabbreviation"))
      {
         return new Bib2GlsDualIndexAbbrev(bib2gls, entryType);
      }
      else if (entryType.equals("tertiaryindexabbreviationentry"))
      {
         return new Bib2GlsTertiaryIndexAbbrevEntry(bib2gls, entryType);
      }
      else if (entryType.equals("dualabbreviation")
            || entryType.equals("dualacronym"))
      {
         return new Bib2GlsDualAbbrev(bib2gls, entryType);
      }
      else if (entryType.equals("dualsymbol")
            || entryType.equals("dualnumber"))
      {
         return new Bib2GlsDualSymbol(bib2gls, entryType);
      }
      else if (entryType.equals("bibtexentry"))
      {
         return new Bib2GlsBibTeXEntry(bib2gls, entryType);
      }
      else if (entryType.equals("contributor"))
      {
         return new Bib2GlsContributor(bib2gls, entryType);
      }
      else if (entryType.equals("progenitor") 
            || entryType.matches("^spawnindex(plural)?$"))
      {
         return new Bib2GlsProgenitor(bib2gls, entryType);
      }
      else if (entryType.equals("spawnentry"))
      {
         return new Bib2GlsSpawnEntry(bib2gls, entryType);
      }
      else if (entryType.matches("^spawn(abbreviation|acronym)$"))
      {
         return new Bib2GlsSpawnAbbrev(bib2gls, entryType);
      }
      else if (entryType.matches("^spawn(symbol|number)$"))
      {
         return new Bib2GlsSpawnSymbol(bib2gls, entryType);
      }
      else if (entryType.equals("spawndualindexentry"))
      {
         return new Bib2GlsSpawnDualIndexEntry(bib2gls, entryType);
      }

      return null;
   }

   protected boolean containsSpecialChars(String id)
   {
      for (int i = 0, n = id.length(); i < n; )
      {
         int codepoint = id.codePointAt(i);
         i += Character.charCount(codepoint);

         // Unlikely that some of these will occur as it would've
         // confused the parser before reaching this point.
         // (Underscore is now allowed.)

         if (codepoint == '$' || codepoint == '^' || codepoint == '~'
           || codepoint == '#' || codepoint == '{' || codepoint == '}'
           || codepoint == '&' || codepoint == '\\' || codepoint == '%')
         {
            return true;
         }
      }

      return false;
   }

   protected boolean containsExtendedChars(String id)
   {
      for (int i = 0, n = id.length(); i < n; )
      {
         int codepoint = id.codePointAt(i);
         i += Character.charCount(codepoint);

         if (codepoint > 0x007F)
         {
            return true;
         }
      }

      return false;
   }

   private GlsResource resource;
}
