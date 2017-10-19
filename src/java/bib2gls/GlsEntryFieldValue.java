/*
    Copyright (C) 2017 Nicola L.C. Talbot
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

public class GlsEntryFieldValue extends ControlSequence
{
   public GlsEntryFieldValue(String name, String fieldLabel, Bib2Gls bib2gls)
   {
      this(name, fieldLabel, CASE_NO_CHANGE, bib2gls);
   }

   public GlsEntryFieldValue(String name, String fieldLabel,
     int caseChange, Bib2Gls bib2gls)
   {
      super(name);
      this.fieldLabel = fieldLabel;
      this.caseChange = caseChange;
      this.bib2gls = bib2gls;
   }

   public Object clone()
   {
      return new GlsEntryFieldValue(getName(), fieldLabel, caseChange, bib2gls);
   }

   private void process(TeXParser parser, TeXObjectList stack, String label)
      throws IOException
   {
      Bib2GlsEntry entry = bib2gls.getCurrentResource().getEntry(label);

      if (entry == null) return;

      BibValueList val = entry.getField(fieldLabel);

      if (val == null)
      {
         val = entry.getFallbackContents(fieldLabel);
      }

      if (val == null) return;

      TeXObjectList obj = ((BibValueList)val.clone()).expand(parser);

      switch (caseChange)
      {
         case CASE_SENTENCE:
           GlsResource.toSentenceCase(obj);
         break;
         case CASE_TO_UPPER:
           GlsResource.toUpperCase(obj);
         break;
      }

      stack.push(obj);
   }

   public void process(TeXParser parser)
      throws IOException
   {
      TeXObject label = parser.popNextArg();

      process(parser, parser, label.toString(parser));
   }

   public void process(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      TeXObject label = stack.popArg(parser);

      process(parser, stack, label.toString(parser));
   }

   private Bib2Gls bib2gls;
   private String fieldLabel;
   public static final int CASE_NO_CHANGE=0;
   public static final int CASE_SENTENCE=1;
   public static final int CASE_TO_UPPER=2;

   private int caseChange = CASE_NO_CHANGE;
}
