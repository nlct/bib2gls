/*
    Copyright (C) 2021 Nicola L.C. Talbot
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

public class GlsRecordSelection extends GlsRecord
{
   public GlsRecordSelection(Bib2Gls bib2gls, String[] labels,
     String locPrefix, String counter, String format, String location)
   {
      super(bib2gls, labels[0], locPrefix, counter, format, location);
      this.labels = labels;
   }

   public GlsRecordSelection(Bib2Gls bib2gls, String[] labels,
     String locPrefix, String counter, String format, String location, long index)
   {
      super(bib2gls, labels[0], locPrefix, counter, format, location, index);
      this.labels = labels;
   }

   @Override
   public Object clone()
   {
      return new GlsRecordSelection(bib2gls, labels, getPrefix(),
        getCounter(), getFormat(), getLocation(), getIndex());
   }

   @Override
   public GlsRecord copy(String newLabel)
   {
      String[] newLabels = new String[labels.length];

      newLabels[0] = newLabel;

      for (int i = 1; i < labels.length; i++)
      {
         newLabels[i] = labels[i];
      }

      return new GlsRecordSelection(bib2gls, newLabels,
        getPrefix(), getCounter(), getFormat(), getLocation(), getIndex());
   }

   @Override
   public GlsRecord getRecord(GlsResource resource, String entryLabel,
     boolean tryFlipping)
   {
      String recordLabelPrefix = resource.getRecordLabelPrefix();

      for (String currentLabel : labels)
      {
         String recordLabel = currentLabel;

         if (recordLabelPrefix != null && !recordLabel.startsWith(recordLabelPrefix))
         {
            recordLabel = recordLabelPrefix + recordLabel;
         }

         if (recordLabel.equals(entryLabel))
         {
            return copy(currentLabel);
         }

         if (tryFlipping)
         {
            recordLabel = resource.flipLabel(recordLabel);

            if (entryLabel.equals(recordLabel))
            {
               return copy(currentLabel);
            }
         }
      }

      return null;
   }

   @Override
   public GlsRecord getRecord(GlsResource resource, String primaryId,
    String dualId, String tertiaryId)
   {
      String recordLabelPrefix = resource.getRecordLabelPrefix();

      for (String currentLabel : labels)
      {
         String recordLabel = currentLabel;

         if (recordLabelPrefix != null && !recordLabel.startsWith(recordLabelPrefix))
         {
            recordLabel = recordLabelPrefix + recordLabel;
         }

         if (recordLabel.equals(primaryId)
              || recordLabel.equals(dualId)
              || recordLabel.equals(tertiaryId)
            )
         {
            return copy(currentLabel);
         }
      }

      return null;
   }

   @Override
   public Bib2GlsEntry getEntry(GlsResource resource, 
      Vector<Bib2GlsEntry> bibData, 
      Vector<Bib2GlsEntry> dualData)
   {
      String recordLabelPrefix = resource.getRecordLabelPrefix();

      String labelPrefix = resource.getLabelPrefix();
      String dualPrefix = resource.getDualPrefix();
      String tertiaryPrefix = resource.getTertiaryPrefix();

      // iterate over preferred order first
      for (String currentLabel : labels)
      {
         String recordLabel = currentLabel;

         if (recordLabelPrefix != null && !recordLabel.startsWith(recordLabelPrefix))
         {
            recordLabel = recordLabelPrefix + recordLabel;
         }

         for (Bib2GlsEntry entry : bibData)
         {
            String entryId = entry.getId();

            if (recordLabel.equals(entryId)
                 || (labelPrefix != null && !entryId.startsWith(labelPrefix)
                      && recordLabel.equals(labelPrefix+entryId))
               )
            {
               return entry;
            }

            if (dualData == null)
            {
               if (dualPrefix != null
                    && !entryId.startsWith(dualPrefix)
                    && recordLabel.equals(dualPrefix+entryId))
               {
                  return entry;
               }

               if (tertiaryPrefix != null
                    && !entryId.startsWith(tertiaryPrefix)
                    && recordLabel.equals(tertiaryPrefix+entryId))
               {
                  return entry;
               }
            }
         }

         if (dualData != null)
         {
            for (Bib2GlsEntry entry : dualData)
            {
               String entryId = entry.getId();

               if (recordLabel.equals(entryId)
                   || (dualPrefix != null
                    && !entryId.startsWith(dualPrefix)
                    && recordLabel.equals(dualPrefix+entryId)))
               {
                  return entry;
               }

               assert (entry instanceof Bib2GlsDualEntry);

               Bib2GlsDualEntry dual = (Bib2GlsDualEntry)entry;

               if (dual.hasTertiary())
               {
                  entryId = entry.getOriginalId();

                  if (tertiaryPrefix != null)
                  {
                     entryId = tertiaryPrefix+entryId;
                  }

                  if (recordLabel.equals(entryId)
                      || (tertiaryPrefix != null
                       && !entryId.startsWith(tertiaryPrefix)
                       && recordLabel.equals(tertiaryPrefix+entryId)))
                  {
                     return entry;
                  }

               }
            }
         }
      }

      return null;
   }

   @Override
   public Bib2GlsEntry getEntry(GlsResource resource,
      Vector<Bib2GlsEntry> data, boolean tryFlipping)
   {
      String labelPrefix = resource.getLabelPrefix();
      String recordLabelPrefix = resource.getRecordLabelPrefix();

      // iterate over preferred order first
      for (String currentLabel : labels)
      {
         String recordLabel = currentLabel;

         if (recordLabelPrefix != null && !recordLabel.startsWith(recordLabelPrefix))
         {
            recordLabel = recordLabelPrefix + recordLabel;
         }

         for (Bib2GlsEntry entry : data)
         {
            String entryId = entry.getId();

            if (recordLabel.equals(entryId)
                 || (labelPrefix != null && !entryId.startsWith(labelPrefix)
                      && recordLabel.equals(labelPrefix+entryId))
               )
            {
               return entry;
            }

            if (tryFlipping)
            {
               String flippedLabel = resource.flipLabel(recordLabel);

               if (entryId.equals(flippedLabel))
               {
                  return entry;
               }
            }
         }
      }

      return null;
   }

   private String[] labels;
}
