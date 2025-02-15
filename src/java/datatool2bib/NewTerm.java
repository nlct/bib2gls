/*
    Copyright (C) 2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.datatool2bib;

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.generic.Symbol;
import com.dickimawbooks.texparserlib.latex.*;

public class NewTerm extends ControlSequence
{
   public NewTerm(DataTool2Bib datatool2bib)
   {
      this("newterm", datatool2bib);
   }

   public NewTerm(String name, DataTool2Bib datatool2bib)
   {
      super(name);
      this.datatool2bib = datatool2bib;
   }

   public Object clone()
   {
      return new NewTerm(getName(), datatool2bib);
   }

   protected void processEntry(TeXObject name, KeyValList options,
     TeXParser parser, TeXObjectList stack)
   throws IOException
   {
      String dbName = options.getString("database", parser, stack);

      if (dbName == null)
      {
         dbName = TeXParserUtils.getControlSequenceValue(
           DataTool2Bib.DATAGIDX_DEFAULT_DATABASE, "untitled", parser, stack);
      }

      options.put("name", name);

      String label = options.getString("label", parser, stack);

      if (label == null)
      {
         if (TeXParserUtils.isString(name, parser))
         {
            label = name.toString(parser);
         }
         else
         {
            TeXObject obj = (TeXObject)name.clone();

            parser.startGroup();

            parser.putControlSequence(true, new AtGobble("glsadd"));
            parser.putControlSequence(true, new AtFirstOfOne("MakeTextUppercase"));
            parser.putControlSequence(true, new AtFirstOfOne("MakeTextLowercase"));
            parser.putControlSequence(true, new AtFirstOfOne("MakeLowercase"));
            parser.putControlSequence(true, new AtFirstOfOne("acronymfont"));
            parser.putControlSequence(true, new AtFirstOfOne("textrm"));
            parser.putControlSequence(true, new AtFirstOfOne("texttt"));
            parser.putControlSequence(true, new AtFirstOfOne("textsf"));
            parser.putControlSequence(true, new AtFirstOfOne("textbf"));
            parser.putControlSequence(true, new AtFirstOfOne("textmd"));
            parser.putControlSequence(true, new AtFirstOfOne("textit"));
            parser.putControlSequence(true, new AtFirstOfOne("textsl"));
            parser.putControlSequence(true, new AtFirstOfOne("emph"));
            parser.putControlSequence(true, new AtFirstOfOne("textsuperscript"));

            ControlSequence cs = parser.getControlSequence("datagidxconvertchars");

            if (cs != null)
            {
               TeXParserUtils.process(cs, parser, stack);
            }

            parser.putControlSequence(true, new AtFirstOfOne("ensuremath"));

            parser.putControlSequence(true, new AtGobble("DTLgidxParen"));
            parser.putControlSequence(true,
              new AtNumberOfNumber("DTLgidxName", 2, 2));

            parser.putControlSequence(true,
              new DataGidxAtInvert("DTLgidxPlace"));

            parser.putControlSequence(true,
              new DataGidxAtInvert("DTLgidxSubject"));

            parser.putControlSequence(true,
              new AtNumberOfNumber("DTLgidxOffice", 2, 2));
            parser.putControlSequence(true,
              new DataGidxAtBothOfTwo("DTLgidxParticle"));

            parser.putControlSequence(true, new AtGobble("__datagidx_punc:n"));

            cs = parser.getControlSequence("datagidxwordifygreek");

            if (cs != null)
            {
               TeXParserUtils.process(cs, parser, stack);
            }

            cs = parser.getControlSequence("newtermlabelhook");

            if (cs != null)
            {
               TeXParserUtils.process(cs, parser, stack);
            }

            obj = TeXParserUtils.purify(obj, parser, stack);

            label = obj.toString(parser);

            parser.endGroup();
         }
      }

      GidxData data = new GidxData(label, options);

      boolean abbreviation = false;

      if (options.get("short") != null)
      {
         TeXObject longArg = options.getValue("long");

         if (longArg != null)
         {
            abbreviation = true;
            data.setEntryType("abbreviation");

            // Strip case changing commands from description field at this
            // point to allow comparison with long value.

            if (!datatool2bib.isCustomIgnoreField("description"))
            {
               TeXObject desc = options.getValue("description");

               if (desc != null)
               {
                  if (datatool2bib.isStripCaseChangeOn() && parser.isStack(desc))
                  {
                     TeXObjectList list = new TeXObjectList();
                     datatool2bib.stripCaseChangers((TeXObjectList)desc, parser, list);
                     desc = list;
                  }

                  if (desc.toString(parser).equals(longArg.toString(parser)))
                  {
                     options.remove("description");
                  }
               }
            }
         }
      }

      if (!abbreviation && datatool2bib.isDetectSymbolsOn())
      {
         TeXObject obj = null;

         if (parser.isStack(name))
         {
            obj = ((TeXObjectList)name).peekStack();
         }
         else
         {
            obj = name;
         }

         if (obj instanceof TeXCsRef)
         {
            obj = parser.getListener().getControlSequence(((TeXCsRef)obj).getName());
         }

         if (obj instanceof CharObject
               && Character.isAlphabetic(((CharObject)obj).getCharCode()))
         {
            // no change
         }
         else if (obj instanceof MathGroup || obj instanceof Symbol
            || TeXParserUtils.isControlSequence(obj, "ensuremath"))
         {
            data.setEntryType("symbol");
         }
         else
         {
            String nameStr = datatool2bib.interpret((TeXObject)name.clone());

            if (nameStr.isEmpty() || !Character.isAlphabetic(nameStr.codePointAt(0)))
            {
               try
               {
                  Integer.parseInt(nameStr);
                  data.setEntryType("number");
               }
               catch (NumberFormatException e)
               {
                  data.setEntryType("symbol");
               }
            }
         }
      }

      datatool2bib.addTerm(dbName, data);
   }

   public void process(TeXParser parser) throws IOException
   {
      process(parser, parser);
   }

   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      KeyValList options = TeXParserUtils.popOptKeyValList(parser, stack);
      TeXObject name = popArg(parser, stack);

      if (options == null)
      {
         options = new KeyValList();
      }

      processEntry(name, options, parser, stack);
   }


   protected DataTool2Bib datatool2bib;
}
