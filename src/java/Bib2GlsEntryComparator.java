package com.dickimawbooks.bib2gls;

import java.util.Locale;
import java.util.Vector;
import java.util.Comparator;
import java.text.Collator;

public class Bib2GlsEntryComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryComparator(String sort, String sortField)
   {
      this.sortField = sortField;

      if (sort.equals("locale"))
      {
         collator = Collator.getInstance();
      }
      else
      {
         collator = Collator.getInstance(Locale.forLanguageTag(sort));
      }
   }

   public int compare(Bib2GlsEntry entry1, Bib2GlsEntry entry2)
   {
      String id1 = entry1.getId();
      String id2 = entry2.getId();

      if (id1.equals(id2))
      {
         return 0;
      }

      // TODO check for parent

      String value1;
      String value2;

      if (sortField.equals("id"))
      {
         value1 = id1;
         value2 = id2;
      }
      else
      {
         value1 = entry1.getFieldValue(sortField);
         value2 = entry2.getFieldValue(sortField);

         if (value1 == null)
         {
            value1 = entry1.getFallbackField(sortField);

            if (value1 == null) value1 = "";
         }

         if (value2 == null)
         {
            value2 = entry2.getFallbackField(sortField);

            if (value2 == null) value2 = "";
         }
      }

      return collator.compare(value1, value2);
   }

   private String sortField;

   private Collator collator;
}
