package com.dickimawbooks.bib2gls;

import java.io.*;
import java.util.Set;
import java.util.Iterator;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsIndex extends Bib2GlsEntry
{
   public Bib2GlsIndex()
   {
      this("index");
   }

   public Bib2GlsIndex(String entryType)
   {
      super(entryType);
   }

   public void checkRequiredFields(TeXParser parser)
   {
   }

   public String getDefaultSort()
   {
      return getId();
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.print("\\newterm[");

      String sep = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         writer.format("%s", sep);

         sep = String.format(",%n");
         writer.format("%s={%s}", field, getFieldValue(field));
      }

      writer.println(String.format("]{%s}", getId()));
   }
}
