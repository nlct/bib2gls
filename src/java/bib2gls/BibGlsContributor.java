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

public class BibGlsContributor extends ControlSequence
  implements Expandable
{
   public BibGlsContributor(Bib2Gls bib2gls)
   {
      this("bibglscontributor", bib2gls);
   }

   public BibGlsContributor(String name, Bib2Gls bib2gls)
   {
      super(name);
      this.bib2gls = bib2gls;
   }

   public Object clone()
   {
      return new BibGlsContributor(getName(), bib2gls);
   }

   public TeXObjectList expandonce(TeXParser parser)
    throws IOException
   {
      return expandonce(parser, parser);
   }

   public TeXObjectList expandonce(TeXParser parser, TeXObjectList stack)
    throws IOException
   {
      TeXObject forenames;
      TeXObject von;
      TeXObject surname;
      TeXObject suffix;

      if (stack == parser)
      {
         forenames = parser.popNextArg();
         von = parser.popNextArg();
         surname = parser.popNextArg();
         suffix = parser.popNextArg();
      }
      else
      {
         forenames = stack.popArg(parser);
         von = stack.popArg(parser);
         surname = stack.popArg(parser);
         suffix = stack.popArg(parser);
      }

      TeXObjectList expanded = new TeXObjectList();

      GlsResource resource = bib2gls.getCurrentResource();
      TeXParserListener listener = parser.getListener();

      switch (resource.getContributorOrder())
      {
         case GlsResource.CONTRIBUTOR_ORDER_SURNAME:
           expanded.add(surname);

           if (!(suffix instanceof TeXObjectList
              && ((TeXObjectList)suffix).isEmpty()))
           {
              expanded.add(listener.createString(", "));
              expanded.add(suffix);
           }

           if (!(forenames instanceof TeXObjectList
              && ((TeXObjectList)forenames).isEmpty()))
           {
              expanded.add(listener.createString(", "));
              expanded.add(forenames);
           }

           if (!(von instanceof TeXObjectList
              && ((TeXObjectList)von).isEmpty()))
           {
              expanded.add(listener.createString(", "));
              expanded.add(von);
           }

         break;
         case GlsResource.CONTRIBUTOR_ORDER_VON:

           if (!(von instanceof TeXObjectList
              && ((TeXObjectList)von).isEmpty()))
           {
              expanded.add(von);
              expanded.add(listener.createString(" "));
           }

           expanded.add(surname);

           if (!(suffix instanceof TeXObjectList
              && ((TeXObjectList)suffix).isEmpty()))
           {
              expanded.add(listener.createString(", "));
              expanded.add(suffix);
           }

           if (!(forenames instanceof TeXObjectList
              && ((TeXObjectList)forenames).isEmpty()))
           {
              expanded.add(listener.createString(", "));
              expanded.add(forenames);
           }

         break;
         case GlsResource.CONTRIBUTOR_ORDER_FORENAMES:

           if (!(forenames instanceof TeXObjectList
              && ((TeXObjectList)forenames).isEmpty()))
           {
              expanded.add(forenames);
              expanded.add(listener.createString(" "));
           }

           if (!(von instanceof TeXObjectList
              && ((TeXObjectList)von).isEmpty()))
           {
              expanded.add(von);
              expanded.add(listener.createString(" "));
           }

           expanded.add(surname);

           if (!(suffix instanceof TeXObjectList
              && ((TeXObjectList)suffix).isEmpty()))
           {
              expanded.add(listener.createString(", "));
              expanded.add(suffix);
           }
         break;
      }

      return expanded;
   }

   public TeXObjectList expandfully(TeXParser parser)
    throws IOException
   {
      TeXObjectList expanded = expandonce(parser);

      return expanded.expandfully(parser);
   }

   public TeXObjectList expandfully(TeXParser parser, TeXObjectList stack)
    throws IOException
   {
      TeXObjectList expanded = expandonce(parser, stack);

      return expanded.expandfully(parser, stack);
   }

   public void process(TeXParser parser)
      throws IOException
   {
      expandonce(parser).process(parser);
   }

   public void process(TeXParser parser, TeXObjectList stack)
      throws IOException
   {
      expandonce(parser, stack).process(parser, stack);
   }

   private Bib2Gls bib2gls;
}
