/*
    Copyright (C) 2021-2024 Nicola L.C. Talbot
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
import com.dickimawbooks.texparserlib.latex.CsvList;

public class GlsXtrMultiEntryAdjustedName extends ControlSequence
{
   public GlsXtrMultiEntryAdjustedName(Bib2Gls bib2gls)
   {
      this(bib2gls, "glsxtrmultientryadjustedname", CaseChange.NO_CHANGE);
   }

   public GlsXtrMultiEntryAdjustedName(Bib2Gls bib2gls, 
     String name, CaseChange caseChange)
   {
      super(name);
      this.bib2gls = bib2gls;
      this.caseChange = caseChange;
   }

   public Object clone()
   {
      return new GlsXtrMultiEntryAdjustedName(bib2gls, getName(), caseChange);
   }

   public void process(TeXParser parser)
     throws IOException
   {
      process(parser, parser);
   }

   public void process(TeXParser parser, TeXObjectList stack)
     throws IOException
   {
      TeXObject arg1, arg2, arg3, arg4;

      // No expansion since this command is inserted by bib2gls with
      // all necessary expansion of the labels.

      if (parser == stack || stack == null)
      {
         arg1 = parser.popNextArg();
         arg2 = parser.popNextArg();
         arg3 = parser.popNextArg();
         arg4 = parser.popNextArg();
      }
      else
      {
         arg1 = stack.popArg(parser);
         arg2 = stack.popArg(parser);
         arg3 = stack.popArg(parser);
         arg4 = stack.popArg(parser);
      }

      String label = arg4.toString(parser);

      CompoundEntry comp = bib2gls.getCompoundEntry(label);

      if (comp != null)
      {
         parser.startGroup();

         parser.putControlSequence(true, 
           new GenericCommand("mglscurrentmainlabel", null,
             parser.getListener().createString(comp.getMainLabel())));

         parser.putControlSequence(true, 
           new GenericCommand("mglscurrentmainoptions", null,
             parser.getListener().createString(comp.getOptions())));

         String[] elements = comp.getElements();

         CsvList csvList = new CsvList();

         for (String elem : elements)
         {
            csvList.add(parser.getListener().createString(elem));
         }

         parser.putControlSequence(true, 
           new GenericCommand("mglscurrentmainlist", null, csvList));

         String preList = arg1.toString(parser);
         String postList = arg3.toString(parser);

         TeXObjectList content = new TeXObjectList();

         if (preList.isEmpty())
         {
            switch (caseChange)
            {
               case NO_CHANGE:
                  content.add(new TeXCsRef("glsxtrmultientryadjustednamefmt"));
               break;
               case SENTENCE:
                  content.add(new TeXCsRef("Glsxtrmultientryadjustednamefmt"));
               break;
               case TITLE:
                  content.add(new TeXCsRef("GlsXtrmultientryadjustednamefmt"));
               break;
               case TO_UPPER:
                  content.add(new TeXCsRef("GLSxtrmultientryadjustednamefmt"));
               break;
            }

            Group grp = parser.getListener().createGroup();
            grp.add(arg2);

            content.add(grp);
         }
         else
         {
            String[] labels = preList.split(",");

            for (int i = 0; i < labels.length; i++)
            {
               content.add(new TeXCsRef("def"));
               content.add(new TeXCsRef("mglscurrentlabel"));
               content.add(parser.getListener().createGroup(labels[i]));

               if (i > 0)
               {
                  content.add(new TeXCsRef("glsxtrmultientryadjustednamepresep"));
                  content.add(parser.getListener().createGroup(labels[i-1]));
                  content.add(parser.getListener().createGroup(labels[i]));
               }

               switch (caseChange)
               {
                  case NO_CHANGE:
                    content.add(new TeXCsRef("glsxtrmultientryadjustednameother"));
                  break;
                  case TITLE:
                    content.add(new TeXCsRef("GlsXtrmultientryadjustednameother"));
                  break;
                  case TO_UPPER:
                    content.add(new TeXCsRef("GLSxtrmultientryadjustednameother"));
                  break;
                  case SENTENCE:
                    if (i == 0)
                    {
                       content.add(new TeXCsRef("Glsxtrmultientryadjustednameother"));
                    }
                    else
                    {
                       content.add(new TeXCsRef("glsxtrmultientryadjustednameother"));
                    }
                  break;
               }

               content.add(parser.getListener().createGroup(labels[i]));
            }

            content.add(new TeXCsRef("glsxtrmultientryadjustednamepresep"));
            content.add(parser.getListener().createGroup(labels[labels.length-1]));
            content.add(parser.getListener().createGroup(comp.getMainLabel()));

            switch (caseChange)
            {
               case NO_CHANGE:
               case SENTENCE:
                  content.add(new TeXCsRef("glsxtrmultientryadjustednamefmt"));
               break;
               case TITLE:
                  content.add(new TeXCsRef("GlsXtrmultientryadjustednamefmt"));
               break;
               case TO_UPPER:
                  content.add(new TeXCsRef("GLSxtrmultientryadjustednamefmt"));
               break;
            }

            Group grp = parser.getListener().createGroup();
            grp.add(arg2);

            content.add(grp);
         }

         if (!postList.isEmpty())
         {
            String[] labels = postList.split(",");

            for (int i = 0; i < labels.length; i++)
            {
               content.add(new TeXCsRef("def"));
               content.add(new TeXCsRef("mglscurrentlabel"));
               content.add(parser.getListener().createGroup(labels[i]));

               content.add(new TeXCsRef("glsxtrmultientryadjustednamepostsep"));

               if (i == 0)
               {
                  content.add(parser.getListener().createGroup(comp.getMainLabel()));
               }
               else
               {
                  content.add(parser.getListener().createGroup(labels[i-1]));
               }

               content.add(parser.getListener().createGroup(labels[i]));

               switch (caseChange)
               {
                  case NO_CHANGE:
                  case SENTENCE:
                    content.add(new TeXCsRef("glsxtrmultientryadjustednameother"));
                  break;
                  case TITLE:
                    content.add(new TeXCsRef("GlsXtrmultientryadjustednameother"));
                  break;
                  case TO_UPPER:
                    content.add(new TeXCsRef("GLSxtrmultientryadjustednameother"));
                  break;
               }
            }
         }

         content.process(parser);

         parser.endGroup();
      }
      else
      {
         bib2gls.warning(parser, bib2gls.getMessage(
           "warning.unknown_compound_label", label));

         if (parser == stack || stack == null)
         {
            arg2.process(parser);
         }
         else
         {
            arg2.process(parser, stack);
         }
      }
   }

   private CaseChange caseChange;
   private Bib2Gls bib2gls;
}
