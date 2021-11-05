/*
    Copyright (C) 2017-2021 Nicola L.C. Talbot
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

import java.io.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;
import java.text.CollationKey;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2GlsDualEntryAbbrev extends Bib2GlsDualAbbrevEntry
{
   public Bib2GlsDualEntryAbbrev(Bib2Gls bib2gls)
   {
      this(bib2gls, "dualentryabbreviation");
   }

   public Bib2GlsDualEntryAbbrev(Bib2Gls bib2gls, String entryType)
   {
      super(bib2gls, entryType);

      bib2gls.warning(bib2gls.getMessage("warning.deprecated.type",
       entryType, "dualabbreviationentry"));
   }

}
