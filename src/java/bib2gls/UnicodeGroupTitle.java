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

public class UnicodeGroupTitle extends GroupTitle
{
   public UnicodeGroupTitle(String title, String actual, long id, String type,
      String parent)
   {
      super(title, actual, id, type, parent);
   }

   public static UnicodeGroupTitle createUnicodeGroupTitle(int codePoint,
     String type, String parent, int groupFormation)
   {
      boolean category = (groupFormation == SortSettings.GROUP_UNICODE_CATEGORY
        || groupFormation == SortSettings.GROUP_UNICODE_CATEGORY_SCRIPT);

      boolean script = (groupFormation == SortSettings.GROUP_UNICODE_SCRIPT
        || groupFormation == SortSettings.GROUP_UNICODE_CATEGORY_SCRIPT);

      String title = null;
      long id = getGroupId(codePoint, groupFormation);

      if (category)
      {
         int charCat = Character.getType(codePoint);

         switch (charCat)
         {
            case Character.COMBINING_SPACING_MARK:
              title = "Mc";
            break;
            case Character.CONNECTOR_PUNCTUATION:
              title = "Pc";
            break;
            case Character.CONTROL:
              title = "Cc";
            break;
            case Character.CURRENCY_SYMBOL:
              title = "Sc";
            break;
            case Character.DASH_PUNCTUATION:
              title = "Pd";
            break;
            case Character.DECIMAL_DIGIT_NUMBER:
              title = "Nd";
            break;
            case Character.ENCLOSING_MARK:
              title = "Me";
            break;
            case Character.END_PUNCTUATION:
              title = "Pe";
            break;
            case Character.FINAL_QUOTE_PUNCTUATION:
              title = "Pf";
            break;
            case Character.FORMAT:
              title = "Cf";
            break;
            case Character.INITIAL_QUOTE_PUNCTUATION:
              title = "Pi";
            break;
            case Character.LETTER_NUMBER:
              title = "Nl";
            break;
            case Character.LINE_SEPARATOR:
              title = "Zl";
            break;
            case Character.LOWERCASE_LETTER:
              title = "Ll";
            break;
            case Character.MATH_SYMBOL:
              title = "Sm";
            break;
            case Character.MODIFIER_LETTER:
              title = "Lm";
            break;
            case Character.MODIFIER_SYMBOL:
              title = "Sk";
            break;
            case Character.NON_SPACING_MARK:
              title = "Mn";
            break;
            case Character.OTHER_LETTER:
              title = "Lo";
            break;
            case Character.OTHER_NUMBER:
              title = "No";
            break;
            case Character.OTHER_PUNCTUATION:
              title = "Po";
            break;
            case Character.OTHER_SYMBOL:
              title = "So";
            break;
            case Character.PARAGRAPH_SEPARATOR:
              title = "Zp";
            break;
            case Character.PRIVATE_USE:
              title = "Co";
            break;
            case Character.SPACE_SEPARATOR:
              title = "Zs";
            break;
            case Character.START_PUNCTUATION:
              title = "Ps";
            break;
            case Character.SURROGATE:
              title = "Cs";
            break;
            case Character.TITLECASE_LETTER:
              title = "Lt";
            break;
            case Character.UNASSIGNED:
              title = "Cn";
            break;
            case Character.UPPERCASE_LETTER:
              title = "Lu";
            break;
         }
      }

      if (script)
      {
         Character.UnicodeScript charScript = Character.UnicodeScript.of(
           codePoint);

         String scriptName = 
           charScript == null ? "Unknown" : charScript.toString();

         if (title == null)
         {
            title = scriptName;
         }
         else
         {
            title = String.format("%s.%s", title, scriptName);
         }
      }

      String actual;

      if (codePoint == '\\' || codePoint == '{' || codePoint == '}')
      {
         actual = String.format("\\char %d ", codePoint);
      }
      else
      {
         actual = new String(Character.toChars(codePoint));
      }

      if (title == null)
      {
         title = actual;
      }

      if (!script && !category)
      {
         title = title.toLowerCase();
      }

      return new UnicodeGroupTitle(title, actual, id, type, parent);
   }

   public static long getGroupId(int codePoint, int groupFormation)
   {
      long id = -1;

      boolean category = (groupFormation == SortSettings.GROUP_UNICODE_CATEGORY
        || groupFormation == SortSettings.GROUP_UNICODE_CATEGORY_SCRIPT);

      boolean script = (groupFormation == SortSettings.GROUP_UNICODE_SCRIPT
        || groupFormation == SortSettings.GROUP_UNICODE_CATEGORY_SCRIPT);

      if (category)
      {
         id = Character.getType(codePoint);
      }

      if (script)
      {
         Character.UnicodeScript charScript = Character.UnicodeScript.of(
           codePoint);

         int scriptId = -1;

         Character.UnicodeScript[] scripts = Character.UnicodeScript.values();

         for (int i = 0; i < scripts.length; i++)
         {
            if (charScript == scripts[i])
            {
               scriptId = i+1;
               break;
            }
         }

         if (id == -1)
         {
            id = (scriptId == -1) ? 0 : scriptId;
         }
         else
         {
            id += 1000*scriptId;
         }
      }

      if (id == -1)
      {
         id = codePoint;
      }

      return id;
   }

   @Override
   protected String getNonHierCsSetName()
   {
      return "bibglssetunicodegrouptitle";
   }

   @Override
   protected String getNonHierCsLabelName()
   {
      return "bibglsunicodegroup";
   }

   @Override
   public String format(String other)
   {
      if (supportsHierarchy)
      {
         return String.format("{%s}{%s}{%X}{%s}{%s}", 
          getTitle(),
          Bib2Gls.replaceSpecialChars(other), 
          getId(), type == null ? "" : type, parent == null ? "" : parent);
      }
      else
      {
         return String.format("{%s}{%s}{%X}{%s}", 
          getTitle(),
          Bib2Gls.replaceSpecialChars(other), 
          getId(), type == null ? "" : type);
      }
   }
}
