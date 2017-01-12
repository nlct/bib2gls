package com.dickimawbooks.bib2gls;

import java.util.Locale;
import java.util.Vector;
import java.util.Comparator;
import java.text.Collator;

public class Bib2GlsEntryComparator implements Comparator<Bib2GlsEntry>
{
   public Bib2GlsEntryComparator(String sort, String sortField,
     Vector<GlsRecord> records)
   {
      this.sortField = sortField;

      if (sort.equals("locale"))
      {
         collator = Collator.getInstance();
         sortType = STRING_SORT;
      }
      else if (sort.equals("use"))
      {
         sortType = USE_SORT;
         this.records = records;
      }
      else
      {
         collator = Collator.getInstance(Locale.forLanguageTag(sort));
         sortType = STRING_SORT;
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

      if (sortType == USE_SORT)
      {
         int idx1 = -1;
         int idx2 = -1;

         for (int i = 0, n = records.size(); i < n; i++)
         {
            String label = records.get(i).getLabel();

            if (idx1 == -1 && label.equals(id1))
            {
               idx1 = i;
            }

            if (idx2 == -1 && label.equals(id2))
            {
               idx2 = i;
            }

            if (idx1 != -1 && idx2 != -1)
            {
               break;
            }
         }

         return idx1 < idx2 ? -1 : 1;
      }

      String value1 = entry1.getFieldValue(sortField);
      String value2 = entry2.getFieldValue(sortField);

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

      return collator.compare(value1, value2);
   }

   private static final int STRING_SORT=0, USE_SORT=1;

   private int sortType = STRING_SORT;

   private String sortField;

   private Collator collator;

   private Vector<GlsRecord> records;
}
