/*
    Copyright (C) 2023 Nicola L.C. Talbot
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
 * The assign-fields option allows a more complex way of referencing
 * a field value. This enum is part of the field reference parsing.
 * The field spec is <em>field-ref</em> &gt; <em>identifier</em>
 * where <em>field-ref</em> may be one of the keywords:
 * "self", "parent", "root", "entrytype" or "entrylabel". If the 
 * "<em>field-ref</em> &gt;" part is omitted "self &gt;" is assumed.
 * The first three reference types (self, parent and root) indicate
 * the entry that the field belongs to: "self" is the actual entry
 * under consideration, "parent" is the entry's parent, and "root"
 * is the entry's hierarchical root. If the entry has no parent then
 * both the "parent" and "root" referenced values will be considered
 * undefined. The "entrytype" and "entrylabel" keywords indicate
 * that the <em>identifier</em> part references the original or
 * actual entry type (the @<em>entrytype</em> not the type field) or 
 * the entry's label. Otherwise <em>identifier</em> should be the
 * field label.
 *
 * Multiple <em>field-ref</em> &gt; instances are permitted. For
 * example: <code>parent &gt; entrytype &gt; original</code>
 * indicates the original value of the parent's entry type (as
 * specified in the bib file). A grandparent entry can be referenced
 * with <code>parent &gt; parent &gt;</code>.
 * Note that <code>self &gt; parent &gt;</code> is valid syntax but
 * it can be simplified to just <code>parent &gt;</code>.
 * However "entrytype" and "entrylabel" must be followed by
 * <em>identifier</em> not another <em>field-ref</em>.
 *
 * Note that since "parent" is also a valid field label, if the
 * actual value of the entry's parent label is required then the
 * <em>identifier</em> part should be parent. For example,
 * "self &gt; parent". Since the <em>field-ref</em> may be omitted
 * in the case of "self", this can be written more briefly as just
 * "parent". If, on the other hand, you want the value of the
 * parent's "name" field, you would need "parent &gt; name".
 */

public enum FieldReference
{
   SELF("self"), PARENT("parent"), ROOT("root"), ENTRY_TYPE("entrytype"),
   ENTRY_LABEL("entrylabel");

   FieldReference(String id)
   {
      this.id = id;
   }

   /**
    * Gets field reference according to the given tag.
    * @param tag the reference tag which may be one of: "self",
    * "parent" or "root". An empty or null tag is equivalent to "self"
    * @return the corresponding FieldReference
    * @throws IllegalArgumentException if the tag isn't valid
    */ 
   public static FieldReference getReference(String tag)
    throws IllegalArgumentException
   {
      if (tag == null || tag.isEmpty() || tag.equals("self"))
      {
         return SELF;
      }
      else if (tag.equals("parent"))
      {
         return PARENT;
      }
      else if (tag.equals("root"))
      {
         return ROOT;
      }
      else if (tag.equals("entrytype"))
      {
         return ENTRY_TYPE;
      }
      else if (tag.equals("entrylabel"))
      {
         return ENTRY_LABEL;
      }
      else
      {
         throw new IllegalArgumentException("Invalid field reference tag: "+tag);
      }
   }

   /**
    * Gets the entry relative to the given entry.
    * @param entry the base entry
    * @return the entry relative to the base entry according to this
    * reference or null if the relative entry doesn't exist (for
    * example, if the base entry doesn't have a parent and the
    * reference is PARENT or ROOT)
    */ 
   public Bib2GlsEntry getEntry(Bib2GlsEntry entry)
   {
      if (this == SELF || this == ENTRY_TYPE || this == ENTRY_LABEL)
      {
         return entry;
      }

      String parentId = entry.getParent();

      if (parentId == null)
      {  
         return null;
      }

      GlsResource resource = entry.getResource();

      Bib2GlsEntry parent = resource.getEntry(parentId);

      if (parent == null || this == PARENT)
      {
         return parent;
      }

      // ROOT
      return parent.getHierarchyRoot();
   }

   public String getTag()
   {
      return id;
   }

   private final String id;
}
