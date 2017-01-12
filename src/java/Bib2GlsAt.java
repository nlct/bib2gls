package com.dickimawbooks.bib2gls;

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsAt extends At
{
   protected void process(TeXParser parser, TeXObjectList entryTypeList,
     TeXObject contents, TeXObject eg)
     throws IOException
   {
      if (!(contents instanceof TeXObjectList))
      {
         throw new BibTeXSyntaxException(parser,
           BibTeXSyntaxException.ERROR_EXPECTING_OR,
           new String[] {"{", "("});
      }

      BibParser bibParser = (BibParser)parser.getListener();

      Bib2Gls bib2gls = (Bib2Gls)bibParser.getTeXApp();

      String entryType = entryTypeList.toString(parser).trim().toLowerCase();

      BibData data;

      if (entryType.equals("entry"))
      {
         data = new Bib2GlsEntry();
      }
      else if (entryType.equals("index"))
      {
         data = new Bib2GlsIndex();
      }
      else if (entryType.equals("acronym")
            || entryType.equals("abbreviation"))
      {
         data = new Bib2GlsAbbrev(entryType);
      }
      else if (entryType.equals("symbol")
            || entryType.equals("number"))
      {
         data = new Bib2GlsSymbol(entryType);
      }
      else
      {
         data = BibData.createBibData(entryType);

         if (data instanceof BibEntry)
         {
            parser.getListener().getTeXApp().warning(parser,
               String.format("Ignoring unknown entry type: %s", entryType));
         }
      }

      data.parseContents(parser, (TeXObjectList)contents, eg);

      bibParser.addBibData(data);

      if (data instanceof Bib2GlsEntry)
      {
         bib2gls.addEntry((Bib2GlsEntry)data);
      }
   }

}
