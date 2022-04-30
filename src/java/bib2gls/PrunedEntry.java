/*
    Copyright (C) 2022 Nicola L.C. Talbot
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

/**
 * Class to keep track of pruned entries and which "see" or "seealso" 
 * lists the entry was pruned from.
 */
public class PrunedEntry
{
   /**
    * Constructor.
    * @param label the pruned label
    */ 
   public PrunedEntry(String label)
   {
      this.label = label;
      fromSee = new Vector<Bib2GlsEntry>();
      fromSeeAlso = new Vector<Bib2GlsEntry>();
   }

   public String toString()
   {
      return label;
   }

   /**
    * Indicates that this pruned label was removed from the "see"
    * list of the given entry.
    * @param entry the entry with the pruned "see" list
    */ 
   public void fromSee(Bib2GlsEntry entry)
   {
      fromSee.add(entry);
   }

   /**
    * Indicates that this pruned label was removed from the "seealso"
    * list of the given entry.
    * @param entry the entry with the pruned "seealso" list
    */ 
   public void fromSeeAlso(Bib2GlsEntry entry)
   {
      fromSeeAlso.add(entry);
   }

   /**
    * Restores this pruned entry.
    */ 
   public void restore()
   {
      for (Bib2GlsEntry entry : fromSee)
      {
         entry.restorePrunedSee(label);
      }

      for (Bib2GlsEntry entry : fromSeeAlso)
      {
         entry.restorePrunedSeeAlso(label);
      }
   }

   private String label;
   private Vector<Bib2GlsEntry> fromSee, fromSeeAlso;
}
