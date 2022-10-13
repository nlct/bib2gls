/*
    Copyright (C) 2017-2022 Nicola L.C. Talbot
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

import java.util.Hashtable;
import java.util.Properties;
import java.util.Iterator;
import java.text.MessageFormat;
import java.text.ChoiceFormat;

public class Gls2BibMessages extends Hashtable<String,MessageFormat>
{
    public Gls2BibMessages(Properties props) throws Gls2BibException
    {
       super(props.isEmpty() ? 10 : props.size());

       Iterator<Object> it = props.keySet().iterator();

       while (it.hasNext())
       {
          Object key = it.next();

          try
          {
             put((String)key, new MessageFormat((String)props.get(key)));
          }
          catch (IllegalArgumentException e)
          {
             throw new Gls2BibException(
              String.format(
               "Property '%s': Invalid message format: %s", 
               key, e.getMessage()),
              e);
          }
       }
    }

    public String getMessage(String label, Object... args)
    {
       MessageFormat msg = get(label);

       if (msg == null)
       {
          throw new IllegalArgumentException(
           "Invalid message label: "+label);
       }

       return msg.format(args);
    }

    public String getChoiceMessage(String label, int argIdx,
      String choiceLabel, int numChoices, Object... args)
    {
       String[] part = new String[numChoices];

       double[] limits = new double[numChoices];

       for (int i = 0; i < numChoices; i++)
       {
          String tag = String.format("message.%d.%s", i, choiceLabel);

          MessageFormat fmt = get(tag);

          if (fmt == null)
          {
             throw new IllegalArgumentException(
              "Invalid message label: "+tag);
          }

          part[i] = fmt.toPattern();
          limits[i] = i;
       }

       MessageFormat fmt = get(label);

       if (fmt == null)
       {
          throw new IllegalArgumentException(
           "Invalid message label: "+label);
       }

       ChoiceFormat choiceFmt = new ChoiceFormat(limits, part);

       fmt.setFormatByArgumentIndex(argIdx, choiceFmt);

       return fmt.format(args);
    }
}
