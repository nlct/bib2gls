/*
    Copyright (C) 2022-2024 Nicola L.C. Talbot
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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.dickimawbooks.bibglscommon.Bib2GlsException;
import com.dickimawbooks.bibglscommon.Bib2GlsSyntaxException;

public class RecordCountRule
{
   public RecordCountRule(Bib2Gls bib2gls)
   {
      this.bib2gls = bib2gls;
      ruleType = RuleType.ALL;
   }

   public RecordCountRule(Bib2Gls bib2gls, String rule)
   throws Bib2GlsSyntaxException
   {
      this.bib2gls = bib2gls;
      setRule(rule);
   }

   public void setRule(String rule) throws Bib2GlsSyntaxException
   {
      try
      {
         if (rule.equals("all") || rule.equals("a"))
         {
            ruleType = RuleType.ALL;
         }
         else if (rule.equals("non-ignored") || rule.equals("n"))
         {
            ruleType = RuleType.NON_IGNORED;
         }
         else if (rule.length() > 2 && rule.startsWith("c/") 
                    && rule.endsWith("/"))
         {
            ruleType = RuleType.COUNTER;
            counterPattern = Pattern.compile(rule.substring(2, rule.length()-1));
         }
         else if (rule.length() > 2 && rule.startsWith("f/"))
         {
            int endIdx;

            if (rule.endsWith("/"))
            {
               endIdx = rule.length()-1;
               matchAnd = true;
            }
            else if (rule.endsWith("/and"))
            {
               matchAnd = true;
               endIdx = rule.length()-4;
            }
            else if (rule.endsWith("/or"))
            {
               matchAnd = false;
               endIdx = rule.length()-3;
            }
            else
            {
               throw new Bib2GlsSyntaxException(bib2gls.getMessage(
                 "error.invalid.record.count.rule", rule));
            }

            int idx = rule.indexOf("/c/");

            if (idx == -1)
            {
               ruleType = RuleType.FORMAT;
               formatPattern = Pattern.compile(rule.substring(2, endIdx));
            }
            else
            {
               ruleType = RuleType.FORMAT_COUNTER;
               formatPattern = Pattern.compile(rule.substring(2, idx));
               counterPattern = Pattern.compile(rule.substring(idx+3, endIdx));
            }
         }
         else
         {
            throw new Bib2GlsSyntaxException(bib2gls.getMessage(
              "error.invalid.record.count.rule", rule));
         }
      }
      catch (PatternSyntaxException e)
      {
         throw new Bib2GlsSyntaxException(bib2gls.getMessage(
           "error.invalid.record.count.rule", rule), e);
      }
   }

   public boolean isAllowed(GlsRecord record)
   {
      switch (ruleType)
      {
         case ALL: return true;
         case NON_IGNORED: return !bib2gls.isIgnored(record);
         case FORMAT: 
           return formatPattern.matcher(record.getFormat()).matches();
         case COUNTER:
           return counterPattern.matcher(record.getCounter()).matches();
      }

      if (matchAnd)
      {
        return formatPattern.matcher(record.getFormat()).matches()
            && counterPattern.matcher(record.getCounter()).matches();
      }
      else
      {
        return formatPattern.matcher(record.getFormat()).matches()
            || counterPattern.matcher(record.getCounter()).matches();
      }
   }

   enum RuleType { ALL, NON_IGNORED, FORMAT, COUNTER, FORMAT_COUNTER; }

   private RuleType ruleType;

   private Pattern formatPattern, counterPattern;

   private boolean matchAnd;

   private Bib2Gls bib2gls;
}
