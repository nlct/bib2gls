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
package com.dickimawbooks.bibgls.bib2gls;

import java.io.IOException;

import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.TeXObjectList;
import com.dickimawbooks.texparserlib.TeXCsRef;
import com.dickimawbooks.texparserlib.ControlSequence;
import com.dickimawbooks.texparserlib.GenericCommand;
import com.dickimawbooks.texparserlib.TextualContentCommand;
import com.dickimawbooks.texparserlib.generic.Symbol;
import com.dickimawbooks.texparserlib.latex.LaTeXSty;
import com.dickimawbooks.texparserlib.latex.LaTeXParserListener;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.LaTeXGenericCommand;
import com.dickimawbooks.texparserlib.latex.AtFirstOfOne;

// minimal support for texjavahelp.sty
public class Bib2GlsTeXJavaHelpSty extends LaTeXSty
{
   public Bib2GlsTeXJavaHelpSty(KeyValList options, LaTeXParserListener listener,
     boolean loadParentOptions)
   throws IOException
   {
      this(options, "texjavahelp", listener, loadParentOptions);
   }

   public Bib2GlsTeXJavaHelpSty(KeyValList options, String name,
     LaTeXParserListener listener, boolean loadParentOptions)
   throws IOException
   {
      super(options, name, listener, loadParentOptions);
   }

   @Override
   public void addDefinitions()
   {
      registerControlSequence(new TextualContentCommand("TeXLive", "TeX Live"));
      registerControlSequence(new TextualContentCommand("MikTeX", "MikTeX"));

      registerControlSequence(new Symbol("dhyphen", '-'));
      registerControlSequence(new Symbol("dcolon", ':'));
      registerControlSequence(new Symbol("dcomma", ','));
      registerControlSequence(new Symbol("dequals", '='));
      registerControlSequence(new Symbol("dfullstop", '.'));
      registerControlSequence(new Symbol("dunderscore", '_'));
      registerControlSequence(new Symbol("dsb", '_'));

      registerControlSequence(new Symbol("dash", 0x2014));
      registerControlSequence(new Symbol("Slash", '/'));

      registerControlSequence(new Symbol("unlimited", 0x221E));
      registerControlSequence(new Symbol("tick", 0x2713));
      registerControlSequence(new Symbol("yes", 0x2714));
      registerControlSequence(new Symbol("proyes", 0x2714));
      registerControlSequence(new Symbol("conyes", 0x2714));
      registerControlSequence(new Symbol("no", 0x2716));
      registerControlSequence(new Symbol("prono", 0x2716));
      registerControlSequence(new Symbol("conno", 0x2716));

      registerControlSequence(new Symbol("tabsym", 0x21B9));
      registerControlSequence(new Symbol("literaltabchar", 0x0009));
      registerControlSequence(new Symbol("upsym", 0x2B71));
      registerControlSequence(new Symbol("continuesymbol", 0x21A9));
      registerControlSequence(new Symbol("codebackslash", '\\'));

      registerControlSequence(new Symbol("leftkeysym", 0x2190));
      registerControlSequence(new Symbol("upkeysym", 0x2191));
      registerControlSequence(new Symbol("rightkeysym", 0x2192));
      registerControlSequence(new Symbol("downkeysym", 0x2193));
      registerControlSequence(new Symbol("backspacekeysym", 0x232B));
      registerControlSequence(new Symbol("spacekeysym", ' '));
      registerControlSequence(new Symbol("visiblespace", 0x2423));
      registerControlSequence(new Symbol("returnsym", 0x21B5));
      registerControlSequence(new Symbol("shiftsym", 0x21E7));

      registerControlSequence(new Symbol("asteriskmarker", 0x2217));
      registerControlSequence(new Symbol("daggermarker", 0x2020));
      registerControlSequence(new Symbol("doubledaggermarker", 0x2021));
      registerControlSequence(new Symbol("sectionmarker", 0x00A7));
      registerControlSequence(new Symbol("lozengemarker", 0x29EB));
      registerControlSequence(new Symbol("pilcrowmarker", 0x00B6));
      registerControlSequence(new Symbol("hashmarker", '#'));
      registerControlSequence(new Symbol("referencemarker", 0x203B));
      registerControlSequence(new Symbol("vdoubleasteriskmarker", 0x2051));
      registerControlSequence(new Symbol("starmarker", 0x2605));
      registerControlSequence(new Symbol("florettemarker", 0x273E));

      registerControlSequence(new Symbol("nlctopenparen", '('));
      registerControlSequence(new Symbol("nlctcloseparen", ')'));
      registerControlSequence(new Symbol("nlctopensqbracket", '['));
      registerControlSequence(new Symbol("nlctclosesqbracket", ']'));

      registerControlSequence(new Symbol("codesym", 0x1F5B9));
      registerControlSequence(new Symbol("resultsym", 0x1F5BA));
      registerControlSequence(new Symbol("warningsym", 0x26A0));
      registerControlSequence(new Symbol("importantsym", 0x2139));
      registerControlSequence(new Symbol("informationsym", 0x1F6C8));
      registerControlSequence(new Symbol("definitionsym", 0x1F4CC));
      registerControlSequence(new GenericCommand(true,
        "valuesettingsym", null, new TeXCsRef("faSliders")));
      registerControlSequence(new Symbol("novaluesettingsym", 0x1D362));
      registerControlSequence(new GenericCommand(true,
        "toggleonsettingsym", null, new TeXCsRef("faToggleOn")));
      registerControlSequence(new GenericCommand(true,
        "toggleoffsettingsym", null, new TeXCsRef("faToggleOff")));
      registerControlSequence(new Symbol("optionvaluesym", 0x1F516));
      registerControlSequence(new Symbol("countersym", 0x2116));
      registerControlSequence(new TextualContentCommand("terminalsym", "\u232A_"));
      registerControlSequence(new Symbol("transcriptsym", 0x1F50E));

      registerControlSequence(new Symbol("shortswitch", '-'));
      registerControlSequence(new TextualContentCommand("longswitch", "--"));

      registerControlSequence(new AtFirstOfOne("code"));
      registerControlSequence(new AtFirstOfOne("styfmt"));
      registerControlSequence(new AtFirstOfOne("clsfmt"));
      registerControlSequence(new AtFirstOfOne("appfmt"));
      registerControlSequence(new AtFirstOfOne("varfmt"));
      registerControlSequence(new AtFirstOfOne("envfmt"));
      registerControlSequence(new AtFirstOfOne("ctrfmt"));
      registerControlSequence(new AtFirstOfOne("filefmt"));
      registerControlSequence(new AtFirstOfOne("optfmt"));
      registerControlSequence(new AtFirstOfOne("dialogfmt"));
      registerControlSequence(new AtFirstOfOne("widgetfmt"));
      registerControlSequence(new AtFirstOfOne("symbolfmt"));
      registerControlSequence(new AtFirstOfOne("menufmt"));
      registerControlSequence(new AtFirstOfOne("keystrokefmt"));
      registerControlSequence(new AtFirstOfOne("disadvantagefmt"));
      registerControlSequence(new AtFirstOfOne("advantagefmt"));
      registerControlSequence(new AtFirstOfOne("faded"));
      registerControlSequence(new AtFirstOfOne("csfmt"));

      registerControlSequence(createSemanticCommand("cspuncfmt", '\\'));
      registerControlSequence(createSemanticCommand("longargfmt", "--"));
      registerControlSequence(createSemanticCommand("shortargfmt", "-"));

      registerControlSequence(createSemanticCommand("meta", 0x2329, 0x232A));
      registerControlSequence(createSemanticCommand("texmeta", 0x2329, 0x232A));
      registerControlSequence(createSemanticCommand("marg", '{', '}'));
      registerControlSequence(createSemanticCommand("margm", "{\u2329", "\u232A}"));
      registerControlSequence(createSemanticCommand("oarg", '[', ']'));
      registerControlSequence(createSemanticCommand("oargm", "[\u2329", "\u232A]"));
      registerControlSequence(createSemanticCommand("qt", 0x201C, 0x201D));
      registerControlSequence(createSemanticCommand("qtt", 0x201C, 0x201D));
      registerControlSequence(createSemanticCommand("xmltagfmt", '<', '>'));
   }

   protected ControlSequence createSemanticCommand(String name, int prefix)
   {
      return createSemanticCommand(name,
        prefix > -1 ? getListener().getOther(prefix) : null,
        null
      );
   }

   protected ControlSequence createSemanticCommand(String name, 
    int prefix, int suffix)
   {
      return createSemanticCommand(name,
        prefix > -1 ? getListener().getOther(prefix) : null,
        suffix > -1 ? getListener().getOther(suffix) : null
      );
   }

   protected ControlSequence createSemanticCommand(String name, 
    String prefix)
   {
      return createSemanticCommand(name,
        prefix != null ? getListener().createString(prefix) : null,
        null
      );
   }

   protected ControlSequence createSemanticCommand(String name, 
    String prefix, String suffix)
   {
      return createSemanticCommand(name,
        prefix != null ? getListener().createString(prefix) : null,
        suffix != null ? getListener().createString(suffix) : null
      );
   }

   protected ControlSequence createSemanticCommand(String name, 
    TeXObject prefix, TeXObject suffix)
   {
      TeXObjectList def = getListener().createStack();

      if (prefix != null)
      {
         def.add(prefix);
      }

      def.add(getListener().getParam(1));

      if (suffix != null)
      {
         def.add(suffix);
      }

      return new LaTeXGenericCommand(true, name, "m", def);
   }
}
