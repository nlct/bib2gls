/*
    Copyright (C) 2022 Nicola L.C. Talbot
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

import com.dickimawbooks.texparserlib.*;

public class GlsLikeFamily
{
   public GlsLikeFamily()
   {
   }

   public void setOptions(String options)
   {
      this.options = options;
   }

   public String getOptions()
   {
      return options;
   }

   public void setPrefix(String prefix)
   {
      this.prefix = prefix;
   }

   public String getPrefix()
   {
      return prefix;
   }

   public boolean hasSingular()
   {
      return singular != null && !singular.isEmpty();
   }

   public String getSingular()
   {
      return singular;
   }

   public void setSingular(String csname)
   {
      singular = csname;
   }

   public boolean hasPlural()
   {
      return plural != null && !plural.isEmpty();
   }

   public String getPlural()
   {
      return plural;
   }

   public void setPlural(String csname)
   {
      plural = csname;
   }

   public boolean hasSentence()
   {
      return sentence != null && !sentence.isEmpty();
   }

   public String getSentence()
   {
      return sentence;
   }

   public void setSentence(String csname)
   {
      sentence = csname;
   }

   public boolean hasSentencePlural()
   {
      return sentencepl != null && !sentencepl.isEmpty();
   }

   public String getSentencePlural()
   {
      return sentencepl;
   }

   public void setSentencePlural(String csname)
   {
      sentencepl = csname;
   }

   public boolean hasAllCaps()
   {
      return allcaps != null && !allcaps.isEmpty();
   }

   public String getAllCaps()
   {
      return allcaps;
   }

   public void setAllCaps(String csname)
   {
      allcaps = csname;
   }

   public boolean hasAllCapsPlural()
   {
      return allcapspl != null && !allcapspl.isEmpty();
   }

   public String getAllCapsPlural()
   {
      return allcapspl;
   }

   public void setAllCapsPlural(String csname)
   {
      allcapspl = csname;
   }

   public boolean hasMember(String csname)
   {
      if (csname.equals(singular) || csname.equals(plural)
         || csname.equals(sentence) || csname.equals(sentencepl)
         || csname.equals(allcaps) || csname.equals(allcapspl))
      {
         return true;
      }

      return false;
   }

   public boolean isPlural(String csname)
   {
      if (csname.equals(plural) || csname.equals(sentencepl)
          || csname.equals(allcapspl))
      {
         return true;
      }

      return false;
   }

   public CaseChange getMemberCase(String csname)
   {
      if (csname.equals(sentence) || csname.equals(sentencepl))
      {
         return CaseChange.SENTENCE;
      }
      else if (csname.equals(allcaps) || csname.equals(allcapspl))
      {
         return CaseChange.TO_UPPER;
      }

      return CaseChange.NO_CHANGE;
   }

   public String getMember(CaseChange caseChange, String csname)
   {
      boolean isPlural = isPlural(csname);

      String member = null;

      switch (caseChange)
      {
         case SENTENCE:
            member = isPlural ? sentencepl : sentence;
         break;
         case TO_UPPER:
            member = isPlural ? allcapspl : allcaps;
         break;
         default:
            member = isPlural ? plural : singular;
      }

      if (member == null || member.isEmpty())
      {
         member = csname;
      }

      return member;
   }

   @Override
   public String toString()
   {
      return String.format("%s[options=%s,prefix=%s,singular=%s,plural=%s,sentence=%s,sentenceplural=%s,allcaps=%s,allcapsplural=%s]",
       getClass().getSimpleName(),
       options, prefix, singular, plural, sentence, sentencepl, allcaps, allcapspl);
   }

   private String singular, plural,
     sentence, sentencepl,
     allcaps, allcapspl, prefix, options;
}
