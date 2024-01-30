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

/*
 * Corresponds to \multiglossaryentry (glossaries-extra.sty v1.48)
 */

public class CompoundEntry
{
   public CompoundEntry(String label, String elementList)
    throws Bib2GlsException
   {
      this.label = label;
      elements = elementList.trim().split("\\s*,\\s*");

      if (elements.length < 2)
      {
         throw new Bib2GlsSyntaxException(
          String.format("at least 2 elements required (%d found)", elements.length));
      }

      mainLabel = elements[elements.length-1];
      options = "";
   }

   public CompoundEntry(String label, String elementList, String main)
    throws Bib2GlsException
   {
      this(label, elementList);
      setMainLabel(main);
   }

   public CompoundEntry(String label, String elementList, String main, String opts)
    throws Bib2GlsException
   {
      this(label, elementList, main);
      setOptions(opts);
   }

   public boolean isElement(String aLabel)
   {
      for (String element : elements)
      {
         if (element.equals(aLabel))
         {
            return true;
         }
      }

      return false;
   }

   public void setMainLabel(String main)
    throws Bib2GlsException
   {
      if (isElement(main))
      {
         mainLabel = main;
      }
      else
      {
         throw new Bib2GlsException(
          String.format("label '%s' not in element list"));
      }
   }

   public void setOptions(String options)
   {
      if (options == null)
      {
         throw new NullPointerException();
      }

      this.options = options;
   }

   public String getLabel()
   {
      return label;
   }

   public String getMainLabel()
   {
      return mainLabel;
   }

   public String[] getElements()
   {
      return elements;
   }

   public String getElementList()
   {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < elements.length; i++)
      {
         if (i > 0) builder.append(',');

         builder.append(elements[i]);
      }

      return builder.toString();
   }

   public String getOptions()
   {
      return options;
   }

   protected String label, mainLabel, options;
   protected String[] elements;
}
