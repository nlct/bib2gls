/*
    Copyright (C) 2017-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.bibgls.gls2bib;

import java.util.Iterator;
import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;

public class NewGlossaryEntry extends ControlSequence
{
   public NewGlossaryEntry(Gls2Bib gls2bib)
   {
      this("newglossaryentry", "entry", gls2bib, false);
   }

   public NewGlossaryEntry(String name, Gls2Bib gls2bib)
   {
      this(name, "entry", gls2bib, false);
   }

   public NewGlossaryEntry(String name, String type, 
     Gls2Bib gls2bib)
   {
      this(name, type, gls2bib, false);
   }

   public NewGlossaryEntry(String name, Gls2Bib gls2bib, boolean provide)
   {
      this(name, "entry", gls2bib, provide);
   }

   public NewGlossaryEntry(String name, String type, Gls2Bib gls2bib, boolean provide)
   {
      super(name);

      this.gls2bib = gls2bib;
      this.provide = provide;
      this.type = type;
   }

   public Object clone()
   {
      return new NewGlossaryEntry(getName(), getType(), gls2bib, provide);
   }

   protected void processEntry(TeXParser parser, String label,
    KeyValList valuesArg)
   throws IOException
   {
      label = gls2bib.processLabel(label);

      if (provide && gls2bib.hasEntry(label))
      {
         return;
      }

      String glosType = null;
      String category = null;

      String entryType = getType();

      boolean checkDesc = gls2bib.isIndexConversionOn() && entryType.equals("entry");

      GlsData data = new GlsData(label, entryType);

      Iterator<String> it = valuesArg.keySet().iterator();

      while (it.hasNext())
      {
         String field = it.next();
         String bibfield = gls2bib.getFieldName(field);

         TeXObject object = valuesArg.getValue(field);

         if (field.equals("type"))
         {
            // Ignore type=\glsdefaulttype

            ControlSequence cs = null;

            if (object instanceof TeXObjectList)
            {
               TeXObjectList list = (TeXObjectList)object;
               list.trim();

               // Ignore if type is \glsdefaulttype

               if (list.size() == 1)
               {
                  TeXObject elem = list.firstElement();

                  if (elem instanceof ControlSequence)
                  {
                     cs = (ControlSequence)elem;
                  }
               }
            }
            else if (object instanceof ControlSequence)
            {
               cs = (ControlSequence)object;
            }

            if (cs != null && cs.getName().equals("glsdefaulttype"))
            {
               bibfield = null;
            }
            else if (gls2bib.isSplitTypeOn())
            {
               glosType = parser.expandToString(object, parser);

               if (!glosType.isEmpty())
               {
                  data.setGlossaryType(glosType);
               }
            }
         }
         else if (field.equals("category"))
         {
            if (gls2bib.isSplitCategoryOn())
            {
               category = parser.expandToString(object, parser);

               if (!category.isEmpty())
               {
                  data.setCategory(category);
               }
            }
         }
         else if (bibfield == null)
         {// skip remaining tests
         }
         else if (field.equals("see") && (object instanceof TeXObjectList))
         {
            // convert see=[\seealsoname] or see=[\alsoname] to seealso=

            TeXObjectList list = (TeXObjectList)object;

            if (list.size() > 3)
            {
               TeXObject elem1 = list.get(0);
               TeXObject elem2 = list.get(1);
               TeXObject elem3 = list.get(2);
               TeXObject elem4 = list.get(3);

               if (elem1 instanceof CharObject 
                    && ((CharObject)elem1).getCharCode() == '['
                && elem2 instanceof ControlSequence
                    && 
                    ( ((ControlSequence)elem2).getName().equals("seealsoname")
                      || ((ControlSequence)elem2).getName().equals("alsoname")
                    )
                  )
               {
                  TeXObject newVal = null;

                  if ((elem3 instanceof CharObject
                       && ((CharObject)elem3).getCharCode() == ']'))
                  {
                     if (list.size() == 4)
                     {
                        newVal = elem4;
                     }
                     else
                     {
                        newVal = new TeXObjectList();

                        for (int i = 3; i < list.size(); i++)
                        {
                           ((TeXObjectList)newVal).add(list.get(i));
                        }
                     }
                  }
                  else if (elem3 instanceof Ignoreable
                       && elem4 instanceof CharObject
                       && ((CharObject)elem4).getCharCode() == ']')
                  {
                     if (list.size() == 5)
                     {
                        newVal = list.get(4);
                     }
                     else
                     {
                        newVal = new TeXObjectList();

                        for (int i = 4; i < list.size(); i++)
                        {
                           ((TeXObjectList)newVal).add(list.get(i));
                        }
                     }
                  }

                  if (newVal != null)
                  {
                     field = "seealso";
                     object = newVal;
                  }
               }
            }
         }

         if (bibfield == null)
         {
            gls2bib.debug(gls2bib.getMessage("message.ignore.field", field, label));
            continue;
         }

         if (gls2bib.fieldExpansionOn(field))
         {
            object = TeXParserUtils.expandFully(object, parser, parser);
         }

         if ((field.equals("see") || field.equals("seealso")
               || field.equals("alias"))
             && object instanceof TeXObjectList
             && !((TeXObjectList)object).isEmpty())
         {
            TeXObjectList list = (TeXObjectList)object;

            TeXObject opt = list.popArg(parser, TeXObjectList.POP_SHORT,
              '[', ']');

            TeXObjectList newVal = new TeXObjectList();

            if (opt != null)
            {
               // If there's an optional part, then this doesn't
               // contain a label, so leave it unchanged.
               newVal.add(parser.getListener().getOther('['));
               newVal.add(opt);
               newVal.add(parser.getListener().getOther(']'));
            }

            String[] xrLabels = parser.expandToString(list, null).trim().split(" *, *");

            for (int i = 0; i < xrLabels.length; i++)
            {
               if (i > 0)
               {
                  newVal.add(parser.getListener().getOther(','));
               }

               newVal.addAll(parser.getListener().createString(
                 gls2bib.processLabel(xrLabels[i])));
            }

            object = newVal;
         }
         else if (checkDesc && field.equals("description"))
         {
            boolean doConversion = false;
            ControlSequence cs = null;

            if (object instanceof TeXObjectList)
            {
               TeXObjectList list = (TeXObjectList)object;

               if (list.isEmpty())
               {
                  doConversion = true;
               }
               else if (list.size() == 1)
               {
                  TeXObject firstElem = list.firstElement();

                  if (firstElem instanceof ControlSequence)
                  {
                     cs = (ControlSequence)firstElem;
                  }
               }
            }
            else if (object instanceof ControlSequence)
            {
               cs = (ControlSequence)object;
            }

            if (cs != null && (cs.getName().equals("nopostdesc")
               || cs.getName().equals("glsxtrnopostpunc")))
            {
               doConversion = true;
            }

            checkDesc = false;

            if (doConversion)
            {
               data.setEntryType("index");
               continue;
            }
         }

         if (field.equals("nonumberlist"))
         {
            if (object instanceof Expandable)
            {
               TeXObjectList expanded = ((Expandable)object).expandfully(parser);

               if (expanded != null)
               {
                  object = expanded;
               }
            }

            String val = object.toString(parser);

            if (val.isEmpty() || val.equals("true"))
            {
               data.putField(bibfield, "true");
            }
            else
            {
               gls2bib.warning(parser, gls2bib.getMessage("gls2bib.discarding.field",
                 field, val, label));
            }
         }
         else if ((object instanceof Group) && !(object instanceof MathGroup))
         {
            data.putField(bibfield, object.toString(parser));
         }
         else
         {
            data.putField(bibfield, 
               String.format("{%s}", object.toString(parser)));
         }
      }

      if (glosType == null && gls2bib.isSplitTypeOn())
      {
         glosType = getDefaultGlossaryType();

         if (glosType != null)
         {
            data.setGlossaryType(glosType);
         }
      }

      if (category == null && gls2bib.isSplitCategoryOn())
      {
         category = getDefaultCategory();

         if (category != null)
         {
            data.setCategory(category);
         }
      }

      gls2bib.addData(data);
   }

   public String getDefaultGlossaryType()
   {
      return null;
   }

   public String getDefaultCategory()
   {
      return null;
   }

   public void process(TeXParser parser) throws IOException
   {
      process(parser, parser);
   }

   public void process(TeXParser parser, TeXObjectList stack) throws IOException
   {
      String labelStr = popLabelString(parser, stack);
      KeyValList keyValList = TeXParserUtils.popKeyValList(parser, stack);

      processEntry(parser, labelStr, keyValList);
   }

   public boolean isProvide()
   {
      return provide;
   }

   public String getType()
   {
      return type;
   }

   private String type;
   protected Gls2Bib gls2bib;
   private boolean provide=false;
}
