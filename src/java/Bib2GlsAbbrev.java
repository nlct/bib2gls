package com.dickimawbooks.bib2gls;

import java.io.*;
import java.util.Set;
import java.util.Iterator;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;

public class Bib2GlsAbbrev extends Bib2GlsEntry
{
   public Bib2GlsAbbrev(String entryType)
   {
      super(entryType);
   }

   public String getDefaultSort()
   {
      return getFieldValue("short");
   }

   public void checkRequiredFields(TeXParser parser)
   {
      if (getField("short") == null)
      {
         missingFieldWarning(parser, "short");
      }

      if (getField("long") == null)
      {
         missingFieldWarning(parser, "long");
      }
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\bibglsnew%s{%s}%%%n{", getEntryType(), getId());

      String sep = "";
      String shortText = "";
      String longText = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         if (field.equals("short"))
         {
            shortText = getFieldValue(field);
         }
         else if (field.equals("long"))
         {
            longText = getFieldValue(field);
         }
         else
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, getFieldValue(field));
         }
      }

      writer.println(String.format("}%%%n{%s}%%%n{%s}",
        shortText, longText));
   }
}
