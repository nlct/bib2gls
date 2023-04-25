/*
    Copyright (C) 2020-2023 Nicola L.C. Talbot
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
package com.dickimawbooks.gls2bib;

import java.util.Iterator;
import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.AtGobble;
import com.dickimawbooks.texparserlib.latex.GobbleOptMandOpt;

public class GlsSee extends ControlSequence
{
   public GlsSee(Gls2Bib gls2bib)
   {
      this(gls2bib, "glssee");
   }

   public GlsSee(Gls2Bib gls2bib, String name)
   {
      super(name);
      this.gls2bib = gls2bib;
   }

   public Object clone()
   {
      return new GlsSee(gls2bib, getName());
   }

   private void absorbSee(TeXParser parser, TeXObject tagArg,
     String entryLabel, String xrList)
   throws IOException
   {
      String key = "see";
      String optArg = "";

      if (tagArg != null)
      {
         optArg = String.format("[%s]", tagArg.toString(parser));

         ControlSequence cs = null;

         if (tagArg instanceof ControlSequence)
         {
            cs = (ControlSequence)tagArg;
         }
         else if (tagArg instanceof TeXObjectList)
         {
            TeXObject obj = ((TeXObjectList)tagArg).popStack(parser);

            if (obj instanceof ControlSequence
                || ((TeXObjectList)tagArg).peekStack() == null)
            {
               cs = (ControlSequence)obj;
            }
         }

         if (cs != null && cs.getName().matches("(see)?alsoname"))
         {
            key = "seealso";
         }
      }

      GlsData entryData = gls2bib.getEntry(entryLabel);

      String original = String.format("%s%s{%s}{%s}", toString(parser), optArg,
           entryLabel, xrList);

      if (entryData == null)
      {
         gls2bib.warning(parser, gls2bib.getMessage(
          "gls2bib.absorbsee.entryundef", original, entryLabel));
      }
      else
      {
         entryData.absorbSee(parser, original, key, xrList, optArg);
      }
   }

   public void process(TeXParser parser) throws IOException
   {
      TeXObject tagArg = parser.popNextArg('[', ']');

      TeXObject entryArg = parser.popNextArg();

      if (entryArg instanceof Expandable)
      {
         TeXObjectList expanded =
             ((Expandable)entryArg).expandfully(parser);

         if (expanded != null)
         {
            entryArg = expanded;
         }
      }

      TeXObject xrListArg = parser.popNextArg();

      if (xrListArg instanceof Expandable)
      {
         TeXObjectList expanded =
             ((Expandable)xrListArg).expandfully(parser);

         if (expanded != null)
         {
            xrListArg = expanded;
         }
      }

      if (gls2bib.isAbsorbSeeOn())
      {
         absorbSee(parser, tagArg, entryArg.toString(parser),
            xrListArg.toString(parser));
      }
   }

   public void process(TeXParser parser, TeXObjectList list) throws IOException
   {
      TeXObject tagArg = list.popArg(parser, '[', ']');

      TeXObject entryArg = list.popArg(parser);

      if (entryArg instanceof Expandable)
      {
         TeXObjectList expanded =
             ((Expandable)entryArg).expandfully(parser, list);

         if (expanded != null)
         {
            entryArg = expanded;
         }
      }

      TeXObject xrListArg = list.popArg(parser);

      if (xrListArg instanceof Expandable)
      {
         TeXObjectList expanded =
             ((Expandable)xrListArg).expandfully(parser, list);

         if (expanded != null)
         {
            xrListArg = expanded;
         }
      }

      if (gls2bib.isAbsorbSeeOn())
      {
         absorbSee(parser, tagArg, entryArg.toString(parser),
            xrListArg.toString(parser));
      }
   }

   private Gls2Bib gls2bib;
}
