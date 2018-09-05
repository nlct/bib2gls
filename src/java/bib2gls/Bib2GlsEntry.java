/*
    Copyright (C) 2017 Nicola L.C. Talbot
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
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Calendar;
import java.util.Date;
import java.text.CollationKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2GlsEntry extends BibEntry
{
   public Bib2GlsEntry(Bib2Gls bib2gls)
   {
      this(bib2gls, "entry");
   }

   public Bib2GlsEntry(Bib2Gls bib2gls, String entryType)
   {
      super(entryType.toLowerCase());
      this.bib2gls = bib2gls;
      this.originalEntryType = entryType;

      resource = bib2gls.getCurrentResource();

      labelPrefix = resource.getLabelPrefix();

      fieldValues = new HashMap<String,String>();
      deps = new Vector<String>();

      String[] counters = resource.getLocationCounters();

      if (counters == null)
      {
         records = new Vector<GlsRecord>();
      }
      else
      {
         recordMap = new HashMap<String,Vector<GlsRecord>>(counters.length);

         for (String counter : counters)
         {
            recordMap.put(counter, new Vector<GlsRecord>());
         }
      }
   }

   public String getBase()
   {
      return base;
   }

   public void setBase(String base)
   {
      if (base != null && base.endsWith(".bib"))
      {
         this.base = base.substring(0, base.length()-4);
      }
      else
      {
         this.base = base;
      }
   }

   public void setDual(Bib2GlsEntry dualEntry)
   {
      dual = dualEntry;
   }

   public Bib2GlsEntry getDual()
   {
      return dual;
   }

   public GlsResource getResource()
   {
      return resource;
   }

   public String getPrefix()
   {
      return labelPrefix;
   }

   public String getSuffix()
   {
      return labelSuffix;
   }

   public void setSuffix(String suffix)
   {
      labelSuffix = suffix;
   }

   public String getId()
   {
      String id = super.getId();

      if (id == null)
      {
         return bib2gls.getMessage("message.missing.id");
      }

      if (labelPrefix == null && labelSuffix == null)
      {
         return id;
      }

      if (labelPrefix == null)
      {
         return id+labelSuffix;
      }

      if (labelSuffix == null)
      {
         return labelPrefix+id;
      }

      return labelPrefix+id+labelSuffix;
   }

   public String getOriginalId()
   {
      return super.getId();
   }

   public void setId(String prefix, String label)
   {
      labelPrefix = prefix;
      setId(label);
   }

   public void setOriginalEntryType(String originalEntryType)
   {
      this.originalEntryType = originalEntryType;
   }

   public String getOriginalEntryType()
   {
      return originalEntryType;
   }

   public String processLabel(String label)
   {
      return processLabel(label, false);
   }

   public String processLabel(String label, boolean isCs)
   {
      if (label.startsWith("dual."))
      {
         String prefix = resource.getDualPrefix();

         if (prefix == null)
         {
            label = label.substring(5);
         }
         else if (!prefix.equals("dual."))
         {
            label = String.format("%s%s", prefix, label.substring(5));
         }
      }
      else if (label.startsWith("tertiary."))
      {
         String prefix = resource.getTertiaryPrefix();

         if (prefix == null)
         {
            label = label.substring(9);
         }
         else if (!prefix.equals("tertiary."))
         {
            label = String.format("%s%s", prefix, label.substring(5));
         }
      }
      else
      {
         Matcher m = EXT_PREFIX_PATTERN.matcher(label);

         if (m.matches())
         {
            try
            {
               String prefix = resource.getExternalPrefix(
                  Integer.parseInt(m.group(1)));

               if (prefix == null)
               {
                  label = m.group(2);
               }
               else
               {
                  label = String.format("%s%s", prefix, m.group(2));
               }
            }
            catch (NumberFormatException e)
            {
               // shouldn't happen as pattern enforces correct
               // format

               bib2gls.debug(e);
            }
         }
         else if (isCs)
         {
            String csLabelPrefix = resource.getCsLabelPrefix();

            if (csLabelPrefix != null)
            {
               label = String.format("%s%s", 
                  resource.getCsLabelPrefix(), label);
            }
         }
         else if (labelPrefix != null)
         {
            label = String.format("%s%s", labelPrefix, label);
         }
      }

      return label;
   }

   // does the control sequence given by csname have [options]{label}
   // syntax (with a * or + prefix)?
   private boolean isGlsCsOptLabel(String csname)
   {
      if (csname.equals("gls") || csname.equals("glspl") 
       || csname.equals("acrfull") || csname.equals("acrlong")
       || csname.equals("acrshort") || csname.equals("acrfullpl")
       || csname.equals("acrlongpl") || csname.equals("acrshortpl")
       || csname.equals("cgls") || csname.equals("cglspl")
       || csname.equals("pgls") || csname.equals("pglspl")
       || csname.equals("glsadd") || csname.equals("glsdisp")
       || csname.equals("glslink") || csname.equals("glsxtrfull")
       || csname.equals("glsxtrfullpl") || csname.equals("glsxtrshort")
       || csname.equals("glsxtrshortpl") || csname.equals("glsxtrlong")
       || csname.equals("glsxtrlongpl") || csname.equals("glsps")
       || csname.equals("glspt") || csname.equals("glshyperlink"))
      {
         return true;
      }
      else if (bib2gls.checkAcroShortcuts() 
            && (csname.equals("ac") || csname.equals("acs")
             || csname.equals("acsp") || csname.equals("acl")
             || csname.equals("aclp") || csname.equals("acf")
             || csname.equals("acfp")))
      {
         return true;
      }
      else if (bib2gls.checkAbbrvShortcuts() 
            && (csname.equals("ab") || csname.equals("abp")
             || csname.equals("as") || csname.equals("asp")
             || csname.equals("al") || csname.equals("alp")
             || csname.equals("af") || csname.equals("afp")))
      {
         return true;
      }
      else if (csname.startsWith("glsxtr"))
      {
         Vector<String> fields = bib2gls.getFields();
         HashMap<String,String> map = bib2gls.getFieldMap();

         for (String field : fields)
         {
            if (csname.equals("glsxtr"+field))
            {
               return true;
            }

            String label = map.get(field);

            if (label != null && csname.equals("glsxtr"+label))
            {
               return true;
            }
         }
      }
      else if (csname.startsWith("gls"))
      {
         Vector<String> fields = bib2gls.getFields();
         HashMap<String,String> map = bib2gls.getFieldMap();

         for (String field : fields)
         {
            if (csname.equals("gls"+field))
            {
               return true;
            }

            String label = map.get(field);

            if (label != null && csname.equals("gls"+label))
            {
               return true;
            }
         }
      }

      return false;
   }

   // is the given cs name likely to cause a problem for
   // \makefirstuc? (Just check for common ones.)
   private boolean isCsProblematic(String csname)
   {
      return csname.equals("foreignlanguage")
           ||csname.equals("textcolor")
           ||csname.equals("ensuremath")
           ||csname.equals("cite")
           ||csname.equals("citep")
           ||csname.equals("citet")
           ||csname.equals("autoref")
           ||csname.equals("cref")
           ||csname.equals("ref");
   }

   private void checkGlsCs(TeXParser parser, TeXObjectList list, 
      boolean mfirstucProtect, String fieldName)
    throws IOException
   {
      for (int i = 0; i < list.size(); i++)
      {
         TeXObject object = list.get(i);

         if (object.isPar() 
             || (object instanceof TeXCsRef
                  && ((TeXCsRef)object).getName().equals("par")))
         {
            // paragraph breaks need to be replaced with \glspar 

            list.set(i, new TeXCsRef("glspar"));
         }
         else if (object instanceof TeXCsRef)
         {
            String csname = ((TeXCsRef)object).getName();

            String glsLikeLabelPrefix = bib2gls.getGlsLikePrefix(csname);

            boolean found = false;

            csname = csname.toLowerCase();

            boolean glslike = (glsLikeLabelPrefix != null || csname.matches("dgls(pl)?"));

            try
            {
               if (csname.equals("glssee")
                || csname.equals("glsxtrindexseealso"))
               {// \glssee[tag]{label}{xr-label-list}
                // or \glsxtrindexseealso{label}{xr-label-list}

                  found = (i==0);

                  TeXObject arg = list.get(++i);

                  while (arg instanceof Ignoreable)
                  {
                     arg = list.get(++i);
                  }

                  if (arg instanceof CharObject)
                  {
                     int code = ((CharObject)arg).getCharCode();

                     if (code == '[')
                     {// skip optional argument
                        for (; i < list.size(); i++)
                        {
                           arg = list.get(i);

                           if (arg instanceof CharObject
                              && ((CharObject)arg).getCharCode() == ']')
                           {
                              arg = list.get(++i);
                              break;
                           }
                        }

                        while (arg instanceof Ignoreable)
                        {
                           arg = list.get(++i);
                        }
                     }
                  }

                  if (arg instanceof Group)
                  {
                     arg = ((Group)arg).toList();
                  }

                  String label = arg.toString(parser);

                  String newLabel = processLabel(label, true);

                  if (!newLabel.equals(label))
                  {
                     label = newLabel;

                     list.set(i, parser.getListener().createGroup(label));
                  }

                  if (bib2gls.getVerboseLevel() > 0)
                  {
                     bib2gls.logMessage(bib2gls.getMessage(
                        "message.crossref.found", getId(),
                        object.toString(parser), label));
                  }

                  addDependency(label);

                  // get next argument

                  arg = list.get(++i);

                  while (arg instanceof Ignoreable)
                  {
                     arg = list.get(++i);
                  }

                  Group grp = parser.getListener().createGroup();

                  CsvList csvlist = CsvList.getList(parser, arg);

                  for (int j = 0, m = csvlist.size()-1; j <= m; j++)
                  {
                     TeXObject obj = csvlist.get(j);

                     label = processLabel(obj.toString(parser), true);

                     grp.add(parser.getListener().createString(label));

                     if (j < m)
                     {
                        grp.add(parser.getListener().getOther(','));
                     }

                     if (bib2gls.getVerboseLevel() > 0)
                     {
                        bib2gls.logMessage(bib2gls.getMessage(
                           "message.crossref.found", getId(),
                           object.toString(parser), label));
                     }

                     addDependency(label);
                  }

                  list.set(i, grp);
               }
               else if (csname.equals("glsxtrp"))
               {// \glsxtrp{field}{label}

                  found = (i==0);

                  // skip first argument
                  TeXObject arg = list.get(++i);

                  while (arg instanceof Ignoreable)
                  {
                     arg = list.get(++i);
                  }

                  arg = list.get(++i);

                  while (arg instanceof Ignoreable)
                  {
                     arg = list.get(++i);
                  }

                  if (arg instanceof Group)
                  {
                     arg = ((Group)arg).toList();
                  }

                  String label = arg.toString(parser);
                  String newLabel = processLabel(label, true);

                  if (!label.equals(newLabel))
                  {
                     label = newLabel;
                     list.set(i, parser.getListener().createGroup(label));
                  }

                  if (bib2gls.getVerboseLevel() > 0)
                  {
                     bib2gls.logMessage(bib2gls.getMessage(
                        "message.crossref.found", getId(),
                        object.toString(parser), label));
                  }

                  addDependency(label);
               }
               else if (isGlsCsOptLabel(csname) || glslike)
               {
                  found = (i==0);

                  TeXObject arg = list.get(++i);

                  while (arg instanceof Ignoreable)
                  {
                     arg = list.get(++i);
                  }

                  String pre = "";
                  String opt = "";

                  if (arg instanceof CharObject)
                  {
                     int code = ((CharObject)arg).getCharCode();

                     if (code == '*' || code == '+')
                     {
                        pre = arg.toString(parser);

                        arg = list.get(++i);

                        while (arg instanceof Ignoreable)
                        {
                           arg = list.get(++i);
                        }

                        if (arg instanceof CharObject)
                        {
                           code = ((CharObject)arg).getCharCode();
                        }
                     }

                     if (code == '[')
                     {
                        opt = "[";

                        for (; i < list.size(); i++)
                        {
                           arg = list.get(i);

                           opt += arg.toString(parser);

                           if (arg instanceof CharObject
                              && ((CharObject)arg).getCharCode() == ']')
                           {
                              arg = list.get(++i);

                              break;
                           }
                        }

                        while (arg instanceof Ignoreable)
                        {
                           arg = list.get(++i);
                        }
                     }
                  }

                  String label;

                  int start = i;

                  if (arg instanceof BgChar)
                  {
                     label = "";

                     while (! ((arg = list.get(++i)) instanceof EgChar))
                     {
                        label += arg.toString(parser);
                     }
                  }
                  else if (arg instanceof Group)
                  {
                     label = ((Group)arg).toList().toString(parser);
                  }
                  else
                  {
                     label = arg.toString(parser);
                  }

                  // Don't replace the label for \dgls etc
                  // or the \gls-like commands that may have the
                  // prefix hidden from bib2gls.

                  if (!glslike)
                  {
                     String newLabel = processLabel(label, true);

                     if (!label.equals(newLabel))
                     {
                        label = newLabel;

                        for ( ; i > start; i--)
                        {
                           list.remove(i);
                        }

                        list.set(i, parser.getListener().createGroup(label));
                     }
                  }
                  else if (glsLikeLabelPrefix != null)
                  {
                     label = glsLikeLabelPrefix+label;
                  }

                  if (bib2gls.getVerboseLevel() > 0)
                  {
                     bib2gls.logMessage(bib2gls.getMessage(
                        "message.crossref.found", getId(),
                        object.toString(parser), label));
                  }

                  addDependency(label);

                  if (bib2gls.checkNestedLinkTextField(fieldName)
                   && !csname.equals("glsps") && !csname.equals("glspt"))
                  {
                     bib2gls.warning(parser, 
                       bib2gls.getMessage("warning.potential.nested.link",
                       getId(), fieldName,
                       String.format("\\%s%s%s", ((TeXCsRef)object).getName(), 
                         pre, opt),
                       label));
                  }

               }
               else if (isCsProblematic(csname))
               {
                  found = (i==0);
               }
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
               bib2gls.warning(parser, 
                 bib2gls.getMessage("warning.can.find.arg", csname));
            }

            if (found && mfirstucProtect)
            {
               // found a problematic command at the start of a
               // field. Protect the field from first letter
               // upper casing by inserting an empty group.

               bib2gls.verbose(parser, 
                 bib2gls.getMessage("message.uc.protecting",
                   object.toString(parser)));
               list.add(0, parser.getListener().createGroup());
               i++;
            }
         }
         else if (object instanceof TeXObjectList)
         {
            if (object instanceof MathGroup && (i==0)
                && mfirstucProtect
                && bib2gls.mfirstucMathShiftProtection())
            {
               bib2gls.verbose(parser, 
                 bib2gls.getMessage("message.uc.protecting", 
                   object.toString(parser)));
               list.add(0, parser.getListener().createGroup());
               i++;
            }

            checkGlsCs(parser, (TeXObjectList)object, false, fieldName);
         }
      }
   }

   protected boolean fieldsParsed()
   {
      return fieldsParsed;
   }

   public void parseFields(TeXParser parser)
     throws IOException
   {
      if (fieldsParsed) return;

      fieldsParsed = true;

      if (resource.hasFieldAliases())
      {
         if (bib2gls.getVerboseLevel() > 0)
         {
            bib2gls.logMessage(bib2gls.getMessage("message.field.alias.check",
              getOriginalId()));
         }

         for (Iterator<String> it = resource.getFieldAliasesIterator();
              it.hasNext(); )
         {
            String field = it.next();
            String map = resource.getFieldAlias(field);

            BibValueList val = removeField(field);

            if (val != null)
            {
               TeXObjectList list = val.expand(parser);
               BibUserString bibVal = new BibUserString(list);
               val.clear();
               val.add(bibVal);

               putField(map, val);

               if (bib2gls.getVerboseLevel() > 0)
               {
                  bib2gls.logMessage(field+"=>"
                    +map+"={"+list.toString(parser)+"}");
               }
            }
         }
      }

      String idField = resource.getSaveOriginalIdField();

      if (idField != null && bib2gls.isKnownField(idField))
      {
         BibUserString bibVal = new BibUserString(
            parser.getListener().createString(getOriginalId()));
         BibValueList val = new BibValueList();
         val.add(bibVal);
         putField(idField, val);
         putField(idField, getOriginalId());
      }

      Vector<String> fields = bib2gls.getFields();

      if (resource.hasSkippedFields())
      {
         String[] skip = resource.getSkipFields();

         for (String field : skip)
         {
            removeField(field);
         }
      }

      if (resource.isCreateMissingParentsEnabled())
      {
         orgParentValue = getField("parent");

         if (orgParentValue != null)
         {
            TeXObjectList list = orgParentValue.expand(parser);
            orgParentValue = new BibValueList();
            orgParentValue.add(new BibUserString((TeXObjectList)list.clone()));
         }
      }

      boolean mfirstucProtect = bib2gls.mfirstucProtection();
      String[] protectFields = bib2gls.mfirstucProtectionFields();
      if (resource.changeShortCase())
      {
         BibValueList value = getField("short");

         if (value != null)
         {
            TeXObjectList list = value.expand(parser);

            putField("short", 
               resource.applyShortCaseChange(parser, value));
         }
      }

      if (resource.changeDescriptionCase())
      {
         BibValueList value = getField("description");

         if (value != null)
         {
            putField("description", 
               resource.applyDescriptionCaseChange(parser, value));
         }
      }

      if (resource.changeDualShortCase())
      {
         BibValueList value = getField("dualshort");

         if (value != null)
         {
            putField("dualshort", 
               resource.applyShortCaseChange(parser, value));
         }
      }

      String shortPluralSuffix = resource.getShortPluralSuffix();
      String dualShortPluralSuffix = resource.getDualShortPluralSuffix();

      if (shortPluralSuffix != null)
      {
         BibValueList value = getField("shortplural");

         if (value == null)
         {
            value = getField("short");

            if (value != null)
            {
               TeXObjectList newVal = (TeXObjectList)value.getContents(true);

               BibValueList list = new BibValueList();

               if (newVal != null)
               {
                  list.add(new BibUserString(newVal));
               }

               if (!shortPluralSuffix.isEmpty())
               {
                  list.add(new BibUserString(
                   parser.getListener().createString(shortPluralSuffix)));
               }

               putField("shortplural", list);
            }
         }
      }

      if (dualShortPluralSuffix != null)
      {
         BibValueList value = getField("dualshortplural");

         if (value == null)
         {
            value = getField("dualshort");

            if (value != null)
            {
               TeXObjectList newVal = (TeXObjectList)value.getContents(true);

               BibValueList list = new BibValueList();

               if (newVal != null)
               {
                  list.add(new BibUserString(newVal));
               }

               if (!shortPluralSuffix.isEmpty())
               {
                  list.add(new BibUserString(
                   parser.getListener().createString(dualShortPluralSuffix)));
               }

               putField("dualshortplural", list);
            }
         }
      }

      if (resource.hasFieldCopies())
      {
         boolean override = resource.isReplicateOverrideOn();

         for (Iterator<String> it=resource.getFieldCopiesIterator();
              it.hasNext();)
         {
            String field = it.next();

            BibValueList val = getField(field);

            if (val != null)
            {
               Vector<String> dupList = resource.getFieldCopy(field);

               for (String dup : dupList)
               {
                  if (getField(dup) == null || override)
                  {
                     putField(dup, (BibValueList)val.clone());
                  }
               }
            }
         }
      }

      if (bib2gls.useGroupField())
      {
         String groupVal = resource.getGroupField();

         if (groupVal != null)
         {
            putField("group", groupVal);
         }
      }

      for (String field : fields)
      {
         BibValueList value = getField(field);

         if (value != null && !field.equals(idField))
         {
            // expand any variables

            TeXObjectList list = value.expand(parser);

            if (value.size() > 1 
               || !(value.firstElement() instanceof BibUserString))
            {
               BibUserString bibVal = new BibUserString(list);
               value.clear();
               value.add(bibVal);
            }

            if (resource.isBibTeXAuthorField(field))
            {
               list = convertBibTeXAuthorField(parser, field, value);
               value.clear();
               value.add(new BibUserString(list));
            }

            boolean isLabelifyList = resource.isLabelifyListField(field);

            if (isLabelifyList || resource.isLabelifyField(field))
            {
               String strVal = bib2gls.convertToLabel(parser,
                  value, resource, isLabelifyList);

               list = parser.getListener().createString(strVal);

               value.clear();
               value.add(new BibUserString(list));

               putField(field, strVal);
            }

            if (field.equals("parent") || field.equals("category")
               || field.equals("type") || field.equals("group")
               || field.equals("seealso") || field.equals("alias"))
            {
               // fields that should only expand to a simple label
               // (cross-referencing fields processed elsewhere)

               String strVal = list.toString(parser);

               if (resource.isInterpretLabelFieldsEnabled() 
                    && strVal.matches("(?s).*[\\\\\\{\\}].*"))
               {
                  // no point checking for other special characters
                  // as they won't expand to a simple alphanumeric string

                  strVal = bib2gls.interpret(strVal, value, true);
               }

               if (field.equals("parent"))
               {
                  putField(field, processLabel(strVal));
               }
               else
               {
                  putField(field, strVal);
               }
            }
            else if (bib2gls.isKnownField(field))
            {
               boolean protect = mfirstucProtect;

               if (protect && protectFields != null)
               {
                  protect = false;

                  for (String pf : protectFields)
                  {
                     if (pf.equals(field))
                     {
                        protect = true;
                        break;
                     }
                  }
               }

               checkGlsCs(parser, list, protect, field);

               if (field.equals("description"))
               {
                  if (resource.isStripTrailingNoPostOn())
                  {
                     int n = list.size();

                     for (int i = n-1; i >= 0; i--)
                     {
                        TeXObject obj = list.get(i);

                        if (obj instanceof Ignoreable)
                        {
                           list.remove(i);
                        }
                        else
                        {
                           if (obj instanceof ControlSequence)
                           {
                              String name = ((ControlSequence)obj).getName();

                              if (name.equals("nopostdesc")
                               || name.equals("glsxtrnopostpunc"))
                              {
                                 list.remove(i);
                              }
                           }

                           break;
                        }
                     }
                  }

                  switch (resource.getPostDescDot())
                  {
                     case GlsResource.POST_DESC_DOT_ALL:
                       list.add(parser.getListener().getOther('.'));
                       break;
                     case GlsResource.POST_DESC_DOT_CHECK:

                       int n = list.size();

                       for (int i = n-1; i >= 0; i--)
                       {
                          TeXObject obj = list.get(i);

                          if (obj instanceof CharObject)
                          {
                             int codePoint = ((CharObject)obj).getCharCode();
                             int charType = Character.getType(codePoint);

                             if (charType != Character.END_PUNCTUATION
                              && charType != Character.FINAL_QUOTE_PUNCTUATION)
                             {
                                if (charType != Character.OTHER_PUNCTUATION)
                                {
                                   list.add(parser.getListener().getOther('.'));
                                }

                                break;
                             }
                          }
                          else if (obj instanceof ControlSequence
                          && (((ControlSequence)obj).getName().equals("nopostdesc")
                            || ((ControlSequence)obj).getName().equals("glsxtrnopostpunc")))
                          {
                             break;
                          }
                          else if (!(obj instanceof Ignoreable))
                          {
                             list.add(parser.getListener().getOther('.'));
                             break;
                          }
                       }
                  }
               }

               if (resource.isCheckEndPuncOn(field))
               {
                  CharObject endPunc = getEndPunc(list);

                  if (endPunc != null)
                  {
                     putField(field+"endpunc", endPunc.toString(parser));
                  }
               }

               putField(field, list.toString(parser));
            }
         }
      }

      // the name can't have its case changed until it's been
      // checked and been assigned a fallback if not present.

      if (resource.changeNameCase())
      {
         changeNameCase(parser);
      }

      if (resource.isCopyAliasToSeeEnabled())
      {
         BibValueList value = getField("alias");

         if (value != null)
         {
            BibValueList seeValue = getField("see");

            // Is there a 'see' field?

            if (seeValue == null)
            {
               putField("see", value);
            }
            else
            {
               TeXObjectList list = seeValue.expand(parser);
               list.add(parser.getListener().getOther(','));
               list.addAll(value.expand(parser));
            }
         }
      }
   }

   protected void changeNameCase(TeXParser parser)
    throws IOException
   {
      BibValueList value = getField("name");

      if (value == null)
      {
         value = getFallbackContents("name");

         if (value == null)
         {
            return;
         }

         value = (BibValueList)value.clone();
      }

      BibValueList textValue = getField("text");

      if (textValue == null)
      {
         putField("text", value);
         putField("text", value.expand(parser).toString(parser));
      }

      value = resource.applyNameCaseChange(parser, value);

      TeXObjectList list = BibValueList.stripDelim(value.expand(parser));

      putField("name", value);
      putField("name", list.toString(parser));
   }

   public void convertFieldToDateTime(TeXParser parser,
     String field, String dateFormat,
     Locale dateLocale, boolean hasDate, boolean hasTime)
   throws IOException
   {
      String id = getId();

      bib2gls.debugMessage("message.datetime.field.check",
        id, field, hasDate, hasTime);

      BibValueList valueList = getField(field);

      if (valueList == null)
      {
         bib2gls.debugMessage("message.field.notset", field, id);
         return;
      }

      String valueStr = getFieldValue(field);

      TeXObjectList originalList = (TeXObjectList)valueList.expand(parser).clone();
      String originalValue = valueStr;

      if (bib2gls.useInterpreter() && 
           valueStr.matches("(?s).*[\\\\\\$\\{\\}\\~].*"))
      {
         valueStr = bib2gls.interpret(valueStr, valueList, true);
      }

      DateFormat format = SortSettings.getDateFormat(
        dateLocale, dateFormat, hasDate, hasTime);

      Date dateValue;

      try
      {
         dateValue = format.parse(valueStr);
      }
      catch (ParseException excp)
      {
         if (format instanceof SimpleDateFormat)
         {
            bib2gls.warningMessage(
                "warning.cant.parse.datetime.pattern",
                field, id, ((SimpleDateFormat)format).toPattern());
         }
         else
         {
            bib2gls.warningMessage("warning.cant.parse.datetime.pattern",
                field, id, dateFormat);
         }

         bib2gls.debug(excp);

         return;
      }

      Calendar calendar;

      if (dateLocale == null)
      {
         calendar = Calendar.getInstance();
      }
      else
      {
         calendar = Calendar.getInstance(dateLocale);
      }

      calendar.setTime(dateValue);

      TeXObjectList list = new TeXObjectList();
      TeXParserListener listener = parser.getListener();
      Group grp;

      StringBuilder builder = new StringBuilder();

      if (hasDate)
      {
         if (hasTime)
         {
            list.add(new TeXCsRef("bibglsdatetime"));
            builder.append("\\bibglsdatetime");
         }
         else
         {
            list.add(new TeXCsRef("bibglsdate"));
            builder.append("\\bibglsdate");
         }

         int year = calendar.get(Calendar.YEAR);
         int month = calendar.get(Calendar.MONTH)+1;
         int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
         int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
         int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
         int era = calendar.get(Calendar.ERA);

         // Ensure compatibility with pgfcalendar and datetime2
         switch (dayOfWeek)
         {
            case Calendar.MONDAY:
              dayOfWeek = 0;
            break;
            case Calendar.TUESDAY:
              dayOfWeek = 1;
            break;
            case Calendar.WEDNESDAY:
              dayOfWeek = 2;
            break;
            case Calendar.THURSDAY:
              dayOfWeek = 3;
            break;
            case Calendar.FRIDAY:
              dayOfWeek = 4;
            break;
            case Calendar.SATURDAY:
              dayOfWeek = 5;
            break;
            case Calendar.SUNDAY:
              dayOfWeek = 6;
            break;
         }

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(year));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(month));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(dayOfMonth));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(dayOfWeek));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(dayOfYear));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(era));

         builder.append(String.format("{%d}{%d}{%d}{%d}{%d}{%d}",
           year, month, dayOfMonth, dayOfWeek, dayOfYear, era));
      }
      else
      {
         list.add(new TeXCsRef("bibglstime"));
         builder.append("\\bibglstime");
      }

      if (hasTime)
      {
         int hour = calendar.get(Calendar.HOUR_OF_DAY);
         int minute = calendar.get(Calendar.MINUTE);
         int second = calendar.get(Calendar.SECOND);
         int millisec = calendar.get(Calendar.MILLISECOND);
         int dst = calendar.get(Calendar.DST_OFFSET);
         int zoneOffset = calendar.get(Calendar.ZONE_OFFSET);

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(hour));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(minute));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(second));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(millisec));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(dst));

         grp = listener.createGroup();
         list.add(grp);
         grp.add(new UserNumber(zoneOffset));

         builder.append(String.format("{%d}{%d}{%d}{%d}{%d}{%d}",
           hour, minute, second, millisec, dst, zoneOffset));
      }

      grp = listener.createGroup();
      grp.addAll(originalList);

      builder.append(String.format("{%s}", originalValue));

      valueList.clear();
      valueList.add(new BibUserString(list));

      putField(field, valueList);
      putField(field, builder.toString());
   }

   protected TeXObjectList convertBibTeXAuthorField(TeXParser parser,
     String field, BibValueList value)
   throws IOException
   {
      Vector<Contributor> contributors = parseContributors(parser, 
        value);

      int n = contributors.size();

      TeXParserListener listener = parser.getListener();

      TeXObjectList list = new TeXObjectList();

      list.add(new TeXCsRef("bibglscontributorlist"));

      Group subgrp = listener.createGroup();

      list.add(subgrp);

      for (int i = 0; i < n; i++)
      {
         if (i > 0)
         {
            subgrp.add(listener.getOther(','));
         }

         Contributor contributor = contributors.get(i);

         String forenames = contributor.getForenames();
         String von = contributor.getVonPart();
         String surname = contributor.getSurname();
         String suffix = contributor.getSuffix();

         subgrp.add(new TeXCsRef("bibglscontributor"));

         if (forenames == null)
         {
            subgrp.add(listener.createGroup());
         }
         else
         {
            subgrp.add(listener.createGroup(forenames.trim()));
         }

         if (von == null)
         {
            subgrp.add(listener.createGroup());
         }
         else
         {
            subgrp.add(listener.createGroup(von.trim()));
         }

         if (surname == null)
         {
            subgrp.add(listener.createGroup());
         }
         else
         {
            subgrp.add(listener.createGroup(surname.trim()));
         }

         if (suffix == null)
         {
            subgrp.add(listener.createGroup());
         }
         else
         {
            subgrp.add(listener.createGroup(suffix.trim()));
         }
      }

      list.add(listener.createGroup(String.format("%d", n)));

      return list;
   }

   protected boolean isSentenceTerminator(int codePoint)
   {
      String list = bib2gls.getMessage("sentence.terminators");

      for (int i = 0; i < list.length(); )
      {
         int cp = list.codePointAt(i);
         i += Character.charCount(cp);

         if (cp == codePoint)
         {
            return true;
         }
      }

      return false;
   }

   protected CharObject getEndPunc(TeXObjectList list)
   {
      int n = list.size();

      for (int i = n-1; i >= 0; i--)
      {
         TeXObject obj = list.get(i);

         if (obj instanceof CharObject)
         {
            CharObject charObj = (CharObject)obj;

            int codePoint = charObj.getCharCode();
            int charType = Character.getType(codePoint);

            if (charType == Character.OTHER_PUNCTUATION)
            {
               if (isSentenceTerminator(codePoint))
               {
                  return charObj;
               }

               return null;
            }

            if (charType != Character.FINAL_QUOTE_PUNCTUATION
                 && charType != Character.END_PUNCTUATION)
            {
               return null;
            }
         }
         else if (obj instanceof TeXObjectList)
         {
            return getEndPunc((TeXObjectList)obj);
         }
         else if (!(obj instanceof Ignoreable))
         {
            return null;
         }
      }

      return null;
   }

   public String getFallbackValue(String field)
   {
      if (field.equals("text"))
      {
         return fieldValues.get("name");
      }
      else if (field.equals("name"))
      {
         // get the parent 

         String parentid = fieldValues.get("parent");

         if (parentid == null) return null;

         Bib2GlsEntry parent = resource.getEntry(parentid);

         if (parent == null) return null;

         String value = parent.getFieldValue("name");

         if (value != null) return value;

         return parent.getFallbackValue("name");
      }
      else if (field.equals("sort"))
      {
         String value = fieldValues.get("name");

         if (value != null) return value;

         return getFallbackValue("name");
      }
      else if (field.equals("first"))
      {
         String value = getFieldValue("text");

         if (value != null) return value;

         return getFallbackValue("text");
      }
      else if (field.equals("plural"))
      {
         String value = getFieldValue("text");

         if (value == null)
         {
            value = getFallbackValue("text");
         }

         if (value != null)
         {
            String suffix = resource.getPluralSuffix();

            return suffix == null ? value : value+suffix;
         }
      }
      else if (field.equals("firstplural"))
      {
         String value = getFieldValue("first");

         if (value == null)
         {
             value = fieldValues.get("first");
         }

         if (value != null)
         {
            String suffix = resource.getPluralSuffix();

            return suffix == null ? value : value+suffix;
         }

         value = getFieldValue("plural");

         return value == null ? getFallbackValue("plural") : value;
      }
      else if (field.equals("shortplural"))
      {
         String value = getFieldValue("short");

         if (value != null)
         {
            String suffix = resource.getShortPluralSuffix();

            return suffix == null ? value : value+suffix;
         }
      }
      else if (field.equals("longplural"))
      {
         String value = getFieldValue("long");

         if (value != null)
         {
            String suffix = resource.getPluralSuffix();

            return suffix == null ? value : value+suffix;
         }
      }

      return null;
   }

   public BibValueList getFallbackContents(String field)
   {
      if (field.equals("text"))
      {
         return getField("name");
      }
      else if (field.equals("name"))
      {
         // get the parent 

         String parentid = fieldValues.get("parent");

         if (parentid == null) return null;

         Bib2GlsEntry parent = resource.getEntry(parentid);

         if (parent == null) return null;

         BibValueList value = parent.getField("name");

         if (value != null) return value;

         return parent.getFallbackContents("name");
      }
      else if (field.equals("sort"))
      {
         BibValueList contents = getField("name");

         return contents == null ? getFallbackContents("name") : contents;
      }
      else if (field.equals("first"))
      {
         BibValueList contents = getField("text");

         return contents == null ? getFallbackContents("text") : contents;
      }
      else if (field.equals("plural"))
      {
         BibValueList contents = getField("text");

         if (contents == null)
         {
            contents = getFallbackContents("text");
         }

         return plural(contents, "glspluralsuffix");
      }
      else if (field.equals("firstplural"))
      {
         BibValueList contents = getField("first");

         if (contents == null)
         {
            contents = getField("plural");

            return contents == null ? getFallbackContents("plural") : contents;
         }

         return plural(contents, "glspluralsuffix");
      }
      else if (field.equals("longplural"))
      {
         return plural(getField("long"), "glspluralsuffix");
      }
      else if (field.equals("shortplural"))
      {
         return plural(getField("short"), "abbrvpluralsuffix");
      }
      else if (field.equals("duallongplural"))
      {
         return plural(getField("duallong"), "glspluralsuffix");
      }
      else if (field.equals("dualshortplural"))
      {
         return plural(getField("dualshort"), "abbrvpluralsuffix");
      }

      return null;
   }

   protected BibValueList plural(BibValueList contents, String suffixCsName)
   {
      if (contents == null) return null;

      contents = (BibValueList)contents.clone();

      contents.add(new BibUserString(new TeXCsRef(suffixCsName)));

      return contents;
   }

   public void checkRequiredFields()
   {
      if (getField("name") == null && getField("parent") == null)
      {
         missingFieldWarning("name");
      }

      if (getField("description") == null)
      {
         missingFieldWarning("description");
      }
   }

   protected void missingFieldWarning(String field)
   {
      bib2gls.warningMessage("warning.missing.field", getId(), field);
   }

   public String getCsName()
   {
      return String.format("bibglsnew%s", getEntryType());
   }

   public void writeCsDefinition(PrintWriter writer)
   throws IOException
   {
      // syntax: {label}{opts}{name}{description}

      writer.format("\\providecommand{\\%s}[4]{%%%n", getCsName());
      writer.print(" \\longnewglossaryentry*{#1}");
      writer.println("{name={#3},#2}{#4}%");
      writer.println("}");
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\%s{%s}%%%n{", getCsName(), getId());

      String description = "";
      String name = null;
      String sep = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         String value = fieldValues.get(field);

         if (value == null) continue;

         if (field.equals("description"))
         {
            description = value;
         }
         else if (field.equals("name"))
         {
            name = value;
         }
         else if (bib2gls.isKnownField(field))
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, value);
         }
         else if (bib2gls.getDebugLevel() > 0 && 
            !bib2gls.isInternalField(field))
         {
            bib2gls.debugMessage("warning.ignoring.unknown.field", field);
         }
      }

      if (name == null)
      {
         name = getFallbackValue("name");
      }

      writer.println("}%");
      writer.println(String.format("{%s}%%", name));
      writer.println(String.format("{%s}", description));
   }

   public void writeLocList(PrintWriter writer)
   throws IOException
   {
      if (locationList == null) return;

      for (String loc : locationList)
      {
         writer.println(String.format("\\glsxtrfieldlistadd{%s}{loclist}{%s}", 
           getId(), loc));
      }
   }

   public void writeIndexCounterField(PrintWriter writer)
   throws IOException
   {
      if (indexCounterRecord == null) return;

      writer.format("\\GlsXtrSetField{%s}{%s}{%s}%n",
         getId(), "indexcounter", indexCounterRecord.getLocation());
   }

   public Set<String> getFieldSet()
   {
      return fieldValues.keySet();
   }

   public String getFieldValue(String field)
   {
      return fieldValues.get(field);
   }

   public String putField(String label, String value)
   {
      if (label == null)
      {
         throw new NullPointerException("null label not permitted");
      }

      if (value == null)
      {
         throw new NullPointerException(
          "null value not permitted for field "+label);
      }

      if (bib2gls.trimFields())
      {
         value = value.trim();
      }

      return fieldValues.put(label, value);
   }

   public String removeFieldValue(String label)
   {
      return fieldValues.remove(label);
   }

   public String getParent()
   {
      return fieldValues.get("parent");
   }

   public boolean hasParent()
   {
      return getParent() != null;
   }

   public String getAlias()
   {
      return getFieldValue("alias");
   }

   public boolean hasAlias()
   {
      return getAlias() != null;
   }

   public boolean hasCrossRefs()
   {
      return (crossRefs != null && crossRefs.length > 0)
         || (alsocrossRefs != null && alsocrossRefs.length > 0)
         || hasAlias();
   }

   public String[] getCrossRefs()
   {
      return crossRefs;
   }

   public String[] getAlsoCrossRefs()
   {
      return alsocrossRefs;
   }

   public void addCrossRefdBy(Bib2GlsEntry entry)
   {
      if (crossRefdBy == null)
      {
         crossRefdBy = new Vector<Bib2GlsEntry>();
      }

      if (!crossRefdBy.contains(entry))
      {
         crossRefdBy.add(entry);
      }
   }

   public Iterator<Bib2GlsEntry> getCrossRefdByIterator()
   {
      return crossRefdBy.iterator();
   }


   public boolean hasDependent(String label)
   {
      return deps.contains(label);
   }

   public void addDependency(String label)
   {
      if (!deps.contains(label) && !label.equals(getId()))
      {
         deps.add(label);
      }
   }

   public Iterator<String> getDependencyIterator()
   {
      return deps.iterator();
   }

   public boolean hasDependencies()
   {
      return deps.size() > 0;
   }

   public boolean equals(Object other)
   {
      if (other == null || !(other instanceof Bib2GlsEntry)) return false;

      return getId().equals(((Bib2GlsEntry)other).getId());
   }

   public Vector<GlsRecord> getRecords()
   {
      if (records == null)
      {
         Vector<GlsRecord> list = new Vector<GlsRecord>();

         Iterator<String> it = recordMap.keySet().iterator();

         while (it.hasNext())
         {
            String key = it.next();

            list.addAll(recordMap.get(key));
         }

         return list;
      }

      return records;
   }

   public int recordCount()
   {
      int n = 0;

      if (supplementalRecords != null) n = supplementalRecords.size();

      if (ignoredRecords != null) n = ignoredRecords.size();

      if (records != null) return n + records.size();

      for (String counter : resource.getLocationCounters())
      {
         n += recordMap.get(counter).size();
      }

      return n;
   }

   public int mainRecordCount()
   {
      if (records != null) return records.size();

      int n = 0;

      for (String counter : resource.getLocationCounters())
      {
         n += recordMap.get(counter).size();
      }

      return n;
   }

   public int supplementalRecordCount()
   {
      return supplementalRecords == null ? 0 : supplementalRecords.size();
   }

   public int ignoredRecordCount()
   {
      return ignoredRecords == null ? 0 : ignoredRecords.size();
   }

   public boolean hasRecords()
   {
      return recordCount() > 0;
   }

   public void addRecord(GlsSeeRecord record)
   {
      // add as an ignored record

      addIgnoredRecord(new GlsRecord(bib2gls, record.getLabel(),
       "", "page", "glsignore", ""));

      StringBuilder builder = new StringBuilder();

      if (crossRefTag == null)
      {
         crossRefTag = record.getTag();
      }

      if (crossRefTag != null)
      {
         builder.append(String.format("[%s]", crossRefTag));
      }

      if (crossRefs == null)
      {
         crossRefs = record.getXrLabels();

         for (int i = 0; i < crossRefs.length; i++)
         {
            if (bib2gls.getVerboseLevel() > 0)
            {
               bib2gls.logMessage(bib2gls.getMessage(
                  "message.crossref.found", getId(),
                  "\\glssee", crossRefs[i]));
            }

            addDependency(crossRefs[i]);

            if (i > 0)
            {
               builder.append(',');
            }

            builder.append(crossRefs[i]);
         }
      }
      else
      {
         Vector<String> list = new Vector<String>();

         String[] newRefs = record.getXrLabels();

         char sep = 0;

         for (int i = 0; i < crossRefs.length; i++)
         {
            if (list.contains(crossRefs[i]))
            {
               continue;
            }

            if (sep == 0)
            {
               sep = ',';
            }
            else
            {
               builder.append(sep);
            }

            list.add(crossRefs[i]);
            builder.append(crossRefs[i]);
         }

         for (int i = 0; i < newRefs.length; i++)
         {
            if (list.contains(newRefs[i]))
            {
               continue;
            }

            if (sep == 0)
            {
               sep = ',';
            }
            else
            {
               builder.append(sep);
            }

            list.add(newRefs[i]);

            if (bib2gls.getVerboseLevel() > 0)
            {
               bib2gls.logMessage(bib2gls.getMessage(
                  "message.crossref.found", getId(),
                  "\\glssee", newRefs[i]));
            }

            addDependency(newRefs[i]);
            builder.append(newRefs[i]);
         }

         crossRefs = new String[list.size()];

         list.toArray(crossRefs);
      }

      putField("see", builder.toString());
   }

   public boolean hasTriggerRecord()
   {
      return triggerRecordFound;
   }

   public void addRecord(GlsRecord record)
   {
      if (record.getFormat().equals("glsignore"))
      {
         bib2gls.debugMessage("message.ignored.record", record);
         addIgnoredRecord(record);
         return;
      }

      if (record.getFormat().equals("glstriggerrecordformat"))
      {
         triggerRecordFound = true;
         bib2gls.debugMessage("message.ignored.record", record);
         addIgnoredRecord(record);
         return;
      }


      if (indexCounterRecord == null)
      {
         String indexCounter = resource.getSaveIndexCounter();

         if (indexCounter != null && record.getCounter().equals("wrglossary"))
         {
            if (indexCounter.equals("true"))
            {
               indexCounterRecord = record;
            }
            else if (record.getFormat().equals(indexCounter))
            {
               indexCounterRecord = record;
            }
         }
      }

      GlsRecord primary = null;
      int setting = GlsResource.SAVE_PRIMARY_LOCATION_OFF;

      if (resource.isPrimaryLocation(record.getFormat()))
      {
         primary = record;

         if (primaryRecords == null)
         {
            primaryRecords = new Vector<GlsRecord>();
         }

         primaryRecords.add(primary);

         setting = resource.getSavePrimaryLocationSetting();
      }

      if (records != null)
      {
         if (!records.contains(record))
         {
            bib2gls.debugMessage("message.adding.record", record,
             getId());

            if (primary != null 
                 && setting == GlsResource.SAVE_PRIMARY_LOCATION_DEFAULT_FORMAT)
            {
               record = (GlsRecord)record.clone();
               record.setFormat("glsnumberformat");
               records.add(record);
            }
            else if (primary == null
                || setting == GlsResource.SAVE_PRIMARY_LOCATION_RETAIN)
            {
               records.add(record);
            }
            else if (setting == GlsResource.SAVE_PRIMARY_LOCATION_START)
            {
               records.add(primaryRecords.size()-1, record);
            }
         }
      }
      else if (primary == null
                || setting != GlsResource.SAVE_PRIMARY_LOCATION_REMOVE)
      {
         String counter = record.getCounter();
         Vector<GlsRecord> list = recordMap.get(counter);

         if (list != null && !list.contains(record))
         {
            bib2gls.debugMessage("message.adding.counter.record", record,
             getId(), counter);

            if (primary != null
               || setting == GlsResource.SAVE_PRIMARY_LOCATION_DEFAULT_FORMAT)
            {
               record = (GlsRecord)record.clone();
               record.setFormat("glsnumberformat");
               list.add(record);
            }
            else if (primary == null
                  || setting == GlsResource.SAVE_PRIMARY_LOCATION_RETAIN)
            {
               list.add(record);
            }
            else if (resource.getSavePrimaryLocationSetting()
                == GlsResource.SAVE_PRIMARY_LOCATION_START)
            {
               // this is more awkward to insert

               boolean done=false;

               for (int i = 0; i < list.size(); i++)
               {
                  if (!resource.isPrimaryLocation(list.get(i).getFormat()))
                  {
                     list.add(i, record);
                     done=true;
                     break;
                  }
               }

               if (!done)
               {
                  list.add(record);
               }
            }
         }
      }
   }

   public void clearRecords()
   {
      bib2gls.debugMessage("message.clearing.records", getId());

      if (records == null)
      {
         Iterator<String> it = recordMap.keySet().iterator();

         while (it.hasNext())
         {
            String key = it.next();

            Vector<GlsRecord> list = recordMap.get(key);

            list.clear();
         }
      }
      else
      {
         records.clear();
      }
   }

   public void addSupplementalRecord(GlsRecord record)
   {
      if (supplementalRecords == null)
      {
         supplementalRecords = new Vector<GlsRecord>();
      }

      if (!bib2gls.isMultipleSupplementarySupported())
      {
         String fmt = record.getFormat();

         if (fmt.startsWith("("))
         {
            fmt = "(glsxtrsupphypernumber";
         }
         else if (fmt.startsWith(")"))
         {
            fmt = ")glsxtrsupphypernumber";
         }
         else
         {
            fmt = "glsxtrsupphypernumber";
         }

         record.setFormat(fmt);
      }
      else if (record instanceof SupplementalRecord)
      {
         if (supplementalRecordMap == null)
         {
            supplementalRecordMap = new HashMap<TeXPath,Vector<GlsRecord>>();
         }

         TeXPath source = ((SupplementalRecord)record).getSource();

         Vector<GlsRecord> list = supplementalRecordMap.get(source);

         if (list == null)
         {
            list = new Vector<GlsRecord>();
            supplementalRecordMap.put(source, list);
         }

         if (!list.contains(record))
         {
            list.add(record);
         }
      }

      if (!supplementalRecords.contains(record))
      {
         bib2gls.debugMessage("message.adding.supplemental.record", getId());
         supplementalRecords.add(record);
      }
   }

   public void addIgnoredRecord(GlsRecord record)
   {
      if (ignoredRecords == null)
      {
         ignoredRecords = new Vector<GlsRecord>();
      }

      if (!ignoredRecords.contains(record))
      {
         ignoredRecords.add(record);
      }
   }

   public static void insertRecord(GlsRecord record, Vector<GlsRecord> list)
   {
      for (int i = 0, n = list.size(); i < n; i++)
      {
         GlsRecord r = list.get(i);

         if (r.equals(record))
         {
            return;
         }

         int result = record.compareTo(r);

         if (result <= 0)
         {
            list.add(i, record);
            return;
         }
      }

      list.add(record);
   }

   public void copyRecordsFrom(Bib2GlsEntry entry)
   {
      if (entry.records != null)
      {
         for (GlsRecord record : entry.records)
         {
            bib2gls.debugMessage(
               "message.copying.record", record,
                 entry.getId(), getId());

            if (record.getFormat().equals("glsignore")
              || record.getFormat().equals("glstriggerrecordformat"))
            {
               addIgnoredRecord(record.copy(getId()));
            }
            else
            {
               insertRecord(record.copy(getId()), records);
            }
         }
      }
      else if (entry.recordMap != null)
      {
         for (Iterator<String> it = entry.recordMap.keySet().iterator();
              it.hasNext(); )
         {
            String counter = it.next();

            Vector<GlsRecord> list = entry.recordMap.get(counter);

            if (list != null)
            {
               Vector<GlsRecord> thisList = recordMap.get(counter);

               if (thisList == null)
               {
                  thisList = new Vector<GlsRecord>();
                  recordMap.put(counter, thisList);
               }

               for (GlsRecord record : list)
               {
                  bib2gls.debugMessage(
                     "message.copying.record", record,
                       entry.getId(), getId());

                  if (record.getFormat().equals("glsignore")
                    || record.getFormat().equals("glstriggerrecordformat"))
                  {
                     addIgnoredRecord(record.copy(getId()));
                  }
                  else
                  {
                     insertRecord(record.copy(getId()), thisList);
                  }
               }
            }
         }
      }

      if (entry.supplementalRecords != null)
      {
         for (GlsRecord record : entry.supplementalRecords)
         {
            bib2gls.debugMessage(
               "message.copying.record", record,
                 entry.getId(), getId());

            addSupplementalRecord(record.copy(getId()));
         }
      }
   }

   private StringBuilder updateLocationList(int minRange, String suffixF,
     String suffixFF, int gap, Vector<GlsRecord> recordList,
     StringBuilder builder)
   throws Bib2GlsException
   {
      GlsRecord prev = null;
      int count = 0;
      StringBuilder mid = new StringBuilder();

      int[] maxGap = new int[1];
      maxGap[0] = 0;

      boolean start=true;

      GlsRecord rangeStart=null;
      String rangeFmt = null;

      int startRangeIdx = 0;

      for (int i = 0, n = recordList.size(); i < n; i++)
      {
         GlsRecord record = recordList.get(i);
         String delimN = (i == n-1 ? "\\bibglslastDelimN " : "\\bibglsdelimN ");

         locationList.add(record.getListTeXCode());
   
         Matcher m = RANGE_PATTERN.matcher(record.getFormat());
   
         if (m.matches())
         {
            char paren = m.group(1).charAt(0);

            count = 0;
            mid.setLength(0);
   
            if (paren == '(')
            {
               if (rangeStart != null)
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                    "error.nested.range", record, rangeStart));
               }
   
               rangeStart = record;
               rangeFmt = m.group(2);
   
               if (builder == null)
               {
                  builder = new StringBuilder();
               }
               else if (!start)
               {
                  builder.append(delimN);
               }

               startRangeIdx = builder.length();

               builder.append("\\bibglsrange{");
               builder.append(record.getFmtTeXCode());

            }
            else
            {
               if (rangeStart == null)
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                    "error.range.missing.start", record));
               }

               builder.append("\\delimR ");
               builder.append(record.getFmtTeXCode());
               builder.append("}");
               rangeStart = null;
               rangeFmt = null;
            }
         }
         else if (rangeStart != null)
         {
             String recordFmt = record.getFormat();

             if (!(rangeStart.getPrefix().equals(record.getPrefix())
               &&  rangeStart.getCounter().equals(record.getCounter())))
             {
                bib2gls.warningMessage(
                    "error.inconsistent.range", record, rangeStart);

                String content = String.format("\\bibglsinterloper{%s}", 
                  record.getFmtTeXCode());

                builder.insert(startRangeIdx, content);

                startRangeIdx += content.length();
             }
             else if ( ((rangeFmt.isEmpty()
                         ||rangeFmt.equals("glsnumberformat"))
                       && (recordFmt.isEmpty()
                         ||recordFmt.equals("glsnumberformat")))
                      || rangeFmt.equals(recordFmt))
             {
                bib2gls.debugMessage("message.merge.range",
                  record, rangeStart);
             }
             else
             {
                if (record.getFormat().equals("glsnumberformat")
                 || rangeFmt.isEmpty())
                {
                   bib2gls.verboseMessage(
                      "message.inconsistent.range", record, rangeStart);
                }
                else
                {
                   bib2gls.warningMessage(
                      "error.inconsistent.range", record, rangeStart);
                }

                String content = String.format("\\bibglsinterloper{%s}", 
                  record.getFmtTeXCode());

                builder.insert(startRangeIdx, content);

                startRangeIdx += content.length();
             }
   
         }
         else if (prev == null)
         {
            count = 1;
   
            if (builder == null)
            {
               builder = new StringBuilder();
            }
            else if (!start)
            {
               builder.append(delimN);
            }

            builder.append(record.getFmtTeXCode());
         }
         else if (minRange < Integer.MAX_VALUE
                  && record.follows(prev, gap, maxGap))
         {
            count++;

            mid.append(delimN);
            mid.append(record.getFmtTeXCode());
         }
         else if (count==2 && suffixF != null)
         {
            builder.append(suffixF);
            builder.append(delimN);
            builder.append(record.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
            maxGap[0] = 0;
         }
         else if (count > 2 && suffixFF != null)
         {
            builder.append(suffixFF);
            builder.append(delimN);
            builder.append(record.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
            maxGap[0] = 0;
         }
         else if (count >= minRange)
         {
            builder.append("\\delimR ");
            builder.append(prev.getFmtTeXCode());

            if (maxGap[0] > 1)
            {
               builder.append("\\bibglspassim ");
            }

            maxGap[0] = 0;

            builder.append(delimN);
            builder.append(record.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
         }
         else
         {
            builder.append(mid);
            builder.append(delimN);
            builder.append(record.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
            maxGap[0] = 0;
         }

         prev = record;
         start = false;
      }

      if (rangeStart != null)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.range.missing.end", rangeStart));
      }
      else if (prev != null && mid.length() > 0)
      {
         if (count >= minRange)
         {
            builder.append("\\delimR ");
            builder.append(prev.getFmtTeXCode());

            if (maxGap[0] > 1)
            {
               builder.append("\\bibglspassim ");
            }
         }
         else
         {
            builder.append(mid);
         }
      }

      return builder;
   }

   public void updateLocationList()
   throws Bib2GlsException
   {
      int minRange = resource.getMinLocationRange();
      String suffixF = resource.getSuffixF();
      String suffixFF = resource.getSuffixFF();
      int seeLocation = resource.getSeeLocation();
      int seealsoLocation = resource.getSeeAlsoLocation();
      int aliasLocation = resource.getAliasLocation();
      boolean showLocationPrefix = resource.showLocationPrefix();
      boolean showLocationSuffix = resource.showLocationSuffix();
      int gap = resource.getLocationGap();

      StringBuilder builder = null;

      locationList = new Vector<String>();

      int numRecords = mainRecordCount()+supplementalRecordCount();

      String alias = getAlias();

      if (aliasLocation == PRE_SEE && alias != null)
      {
         builder = new StringBuilder();
         builder.append("\\bibglsusealias{");
         builder.append(getId());
         builder.append("}");

         if (numRecords > 0)
         {
            builder.append("\\bibglsaliassep ");
         }

         StringBuilder listBuilder = new StringBuilder();
         listBuilder.append("\\glsseeformat");

         listBuilder.append("{");
         listBuilder.append(alias);
         listBuilder.append("}{}");

         locationList.add(listBuilder.toString());
      }
      else if (seeLocation == PRE_SEE && crossRefs != null)
      {
         builder = new StringBuilder();
         builder.append("\\bibglsusesee{");
         builder.append(getId());
         builder.append("}");

         if (numRecords > 0)
         {
            builder.append("\\bibglsseesep ");
         }

         StringBuilder listBuilder = new StringBuilder();
         listBuilder.append("\\glsseeformat");

         if (crossRefTag != null)
         {
            listBuilder.append('[');
            listBuilder.append(crossRefTag);
            listBuilder.append(']');
         }

         listBuilder.append("{");

         for (int i = 0; i < crossRefs.length; i++)
         {
            if (i > 0) listBuilder.append(",");

            listBuilder.append(processLabel(crossRefs[i]));
         }

         listBuilder.append("}{}");

         locationList.add(listBuilder.toString());
      }
      else if (seealsoLocation == PRE_SEE && alsocrossRefs != null)
      {
         builder = new StringBuilder();
         builder.append("\\bibglsuseseealso{");
         builder.append(getId());
         builder.append("}");

         if (numRecords > 0)
         {
            builder.append("\\bibglsseealsosep ");
         }

         StringBuilder listBuilder = new StringBuilder();
         listBuilder.append("\\glsxtruseseealsoformat");

         listBuilder.append("{");

         for (int i = 0; i < alsocrossRefs.length; i++)
         {
            if (i > 0) listBuilder.append(",");

            listBuilder.append(processLabel(alsocrossRefs[i]));
         }

         listBuilder.append("}");

         locationList.add(listBuilder.toString());
      }

      boolean hasLocationList = (numRecords > 0);

      if (alias != null 
           && resource.aliasLocations() != GlsResource.ALIAS_LOC_KEEP)
      {
         hasLocationList = false;
      }

      if (hasLocationList)
      {
         if (showLocationPrefix)
         {
            if (builder == null)
            {
               builder = new StringBuilder();
            }

            builder.append(String.format("\\bibglslocprefix{%d}",
              numRecords));
         }

         String supplSep = "";

         if (records == null)
         {
            String sep = "";

            for (String counter : resource.getLocationCounters())
            {
               Vector<GlsRecord> list = recordMap.get(counter);

               if (list.size() > 0)
               {
                  if (builder == null)
                  {
                     builder = new StringBuilder();
                  }

                  builder.append(String.format(
                   "%s\\bibglslocationgroup{%s}{%s}{",
                   sep, list.size(), counter));

                  builder = updateLocationList(minRange, suffixF, suffixFF, gap,
                    list, builder);

                  builder.append("}");

                  sep = "\\bibglslocationgroupsep ";
                  supplSep = "\\bibglssupplementalsep ";
               }
            }
         }
         else if (records.size() > 0)
         {
            builder = updateLocationList(minRange, suffixF, suffixFF, gap,
              records, builder);
            supplSep = "\\bibglssupplementalsep ";
         }

         if (supplementalRecords != null && supplementalRecords.size() > 0)
         {
            if (builder == null)
            {
               builder = new StringBuilder();
            }

            builder.append(String.format("%s\\bibglssupplemental{%d}{", 
              supplSep, supplementalRecords.size()));

            if (bib2gls.isMultipleSupplementarySupported())
            {
               // Fetch the list from resources to maintain correct
               // order.
               Vector<TeXPath> sources = resource.getSupplementalPaths();

               String supplSubSep = "";

               for (TeXPath source : sources)
               {
                  Vector<GlsRecord> subList = supplementalRecordMap.get(source);

                  if (subList != null)
                  {
                     builder.append(String.format(
                       "%s\\bibglssupplementalsublist{%d}{%s}{", 
                       supplSubSep, subList.size(), 
                       bib2gls.getTeXPathHref(source)));

                     supplSubSep = "\\bibglssupplementalsubsep ";

                     builder = updateLocationList(minRange, suffixF, suffixFF, 
                      gap, subList, builder);

                     builder.append("}");
                  }
               }
            }
            else
            {
               builder = updateLocationList(minRange, suffixF, suffixFF, gap,
                 supplementalRecords, builder);
            }

            builder.append("}");
         }
      }

      if (aliasLocation == POST_SEE && alias != null)
      {
         if (builder == null)
         {
            builder = new StringBuilder();
         }

         if (hasLocationList)
         {
            builder.append("\\bibglsaliassep ");
         }

         builder.append("\\bibglsusealias{");
         builder.append(getId());
         builder.append("}");

         StringBuilder listBuilder = new StringBuilder();

         listBuilder.append("\\glsseeformat");

         listBuilder.append("{");
         listBuilder.append(alias);
         listBuilder.append("}{}");

         locationList.add(listBuilder.toString());
      }
      else if (seeLocation == POST_SEE && crossRefs != null)
      {
         if (builder == null)
         {
            builder = new StringBuilder();
         }

         if (hasLocationList)
         {
            builder.append("\\bibglsseesep ");
         }

         builder.append("\\bibglsusesee{");
         builder.append(getId());
         builder.append("}");

         StringBuilder listBuilder = new StringBuilder();

         listBuilder.append("\\glsseeformat");

         if (crossRefTag != null)
         {
            listBuilder.append('[');
            listBuilder.append(crossRefTag);
            listBuilder.append(']');
         }

         listBuilder.append("{");

         for (int i = 0; i < crossRefs.length; i++)
         {
            if (i > 0) listBuilder.append(",");

            listBuilder.append(processLabel(crossRefs[i]));
         }

         listBuilder.append("}{}");

         locationList.add(listBuilder.toString());
      }
      else if (seealsoLocation == POST_SEE && alsocrossRefs != null)
      {
         if (builder == null)
         {
            builder = new StringBuilder();
         }

         if (hasLocationList)
         {
            builder.append("\\bibglsseealsosep ");
         }

         builder.append("\\bibglsuseseealso{");
         builder.append(getId());
         builder.append("}");

         StringBuilder listBuilder = new StringBuilder();

         listBuilder.append("\\glsxtruseseealsoformat");

         listBuilder.append("{");

         for (int i = 0; i < alsocrossRefs.length; i++)
         {
            if (i > 0) listBuilder.append(",");

            listBuilder.append(processLabel(alsocrossRefs[i]));
         }

         listBuilder.append("}");

         locationList.add(listBuilder.toString());
      }

      if (builder != null)
      {
         if (showLocationSuffix && (numRecords > 0 || crossRefs != null
             || alsocrossRefs != null))
         {
            builder.append(String.format("\\bibglslocsuffix{%d}",
              numRecords));
         }

         putField("location", builder.toString());
      }
   }

   public String getPrimaryRecordList() throws Bib2GlsException
   {
      if (primaryRecords == null)
      {
         return null;
      }

      StringBuilder primaryBuilder = new StringBuilder();

      primaryBuilder = updateLocationList(resource.getMinLocationRange(), 
        resource.getSuffixF(), resource.getSuffixFF(), 
        resource.getLocationGap(),
        primaryRecords, primaryBuilder);

      return String.format("\\bibglsprimary{%d}{%s}",
              primaryRecords.size(), primaryBuilder);
   }

   public void initAlias(TeXParser parser) throws IOException
   {
      String alias = getFieldValue("alias");
      BibValueList value = getField("alias");

      if (alias == null)
      {
         if (value != null)
         {
            alias = value.expand(parser).toString(parser);
         }
      }

      if (alias == null)
      {
         if (bib2gls.getVerboseLevel() > 0)
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.field.not.set", "alias"));
         }
      }
      else
      {
         alias = processLabel(alias);

         if (bib2gls.getVerboseLevel() > 0)
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.crossref.found", getId(),
               "alias", alias));
         }

         addDependency(alias);
         putField("alias", alias);

         resource.setAliases(true);
      }
   }

   public void initCrossRefs(TeXParser parser)
    throws IOException
   {
      if (bib2gls.getVerboseLevel() > 0)
      {
         bib2gls.logMessage(bib2gls.getMessage(
            "message.checking.crossrefs", getId()));
      }

      initAlias(parser);

      // Is there a 'see' field?
      BibValueList value = getField("see");

      BibValueList seeAlsoValue = getField("seealso");

      if (value == null)
      {// no 'see' field, is there a 'seealso' field?

         if (bib2gls.getVerboseLevel() > 0)
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.field.not.set", "see"));
         }

         if (seeAlsoValue != null)
         {
            initAlsoCrossRefs(parser, seeAlsoValue, getFieldValue("seealso"));
            return;
         }

         if (bib2gls.getVerboseLevel() > 0)
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.field.not.set", "seealso"));
         }

         // check for \glssee moved
         // now added with addRecord(GlsSeeRecord)
      }

      if (seeAlsoValue != null)
      {
         bib2gls.warningMessage("warning.field.clash", "see", "seealso");
      }

      if (value == null)
      {// not found
         return;
      }

      TeXObjectList valList = value.expand(parser);

      StringBuilder builder = new StringBuilder();

      initSeeRef(parser, valList, builder);
   }

   private void initSeeRef(TeXParser parser, TeXObjectList valList,
    StringBuilder builder)
    throws IOException
   {
      if (valList instanceof Group)
      {
         valList = ((Group)valList).toList();
      }

      TeXObject opt = valList.popArg(parser, '[', ']');

      if (opt != null)
      {
         crossRefTag = opt.toString(parser);

         builder.append('[');
         builder.append(crossRefTag);
         builder.append(']');
      }

      CsvList csvList = CsvList.getList(parser, valList);

      int n = csvList.size();

      if (n == 0) return;

      crossRefs = new String[n];

      for (int i = 0; i < n; i++)
      {
         TeXObject xr = csvList.get(i);

         if (xr instanceof TeXObjectList)
         {
            xr = GlsResource.trimList((TeXObjectList)xr);
         }

         crossRefs[i] = xr.toString(parser);

         String label = processLabel(crossRefs[i]);

         if (bib2gls.getVerboseLevel() > 0)
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.crossref.found", getId(), "see", label));
         }

         addDependency(label);
         builder.append(label);

         if (i != n-1)
         {
            builder.append(',');
         }
      }

      putField("see", builder.toString());
   }

   private void initAlsoCrossRefs(TeXParser parser, BibValueList value, 
     String strValue)
    throws IOException
   {
      if (strValue == null || strValue.isEmpty() 
           || !bib2gls.isKnownField("seealso"))
      {
         initAlsoCrossRefs(parser, value);
      }
      else
      {
         StringBuilder builder = new StringBuilder();
         String sep = "";

         alsocrossRefs = strValue.trim().split("\\s*,\\s*");

         for (String label : alsocrossRefs)
         {
            label = processLabel(label);
   
            if (bib2gls.getVerboseLevel() > 0)
            {
               bib2gls.logMessage(bib2gls.getMessage(
                  "message.crossref.found", getId(), "seealso", label));
            }
   
            addDependency(label);

            builder.append(sep);
            builder.append(label);
            sep = ",";
         }

         putField("seealso", builder.toString());
      }
   }

   private void initAlsoCrossRefs(TeXParser parser, BibValueList value)
    throws IOException
   {
      StringBuilder builder = new StringBuilder();

      TeXObjectList valList = value.expand(parser);

      if (!bib2gls.isKnownField("seealso"))
      {// seealso field not supported, so replicate see=[\seealsoname]

         bib2gls.warningMessage(
           "warning.field.unsupported", "seealso", "1.16");

         crossRefTag = "\\seealsoname ";
         builder.append("[\\seealsoname]");
         initSeeRef(parser, valList, builder);

         return;
      }

      if (valList instanceof Group)
      {
         valList = ((Group)valList).toList();
      }

      CsvList csvList = CsvList.getList(parser, valList);

      int n = csvList.size();

      if (n == 0) return;

      alsocrossRefs = new String[n];

      for (int i = 0; i < n; i++)
      {
         TeXObject xr = csvList.get(i);

         if (xr instanceof TeXObjectList)
         {
            xr = GlsResource.trimList((TeXObjectList)xr);
         }

         alsocrossRefs[i] = xr.toString(parser);

         String label = processLabel(alsocrossRefs[i]);

         if (bib2gls.getVerboseLevel() > 0)
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.crossref.found", getId(), "seealso", label));
         }

         addDependency(label);
         builder.append(label);

         if (i != n-1)
         {
            builder.append(',');
         }
      }

      putField("seealso", builder.toString());
   }

   public void setCollationKey(CollationKey key)
   {
      collationKey = key;
   }

   public CollationKey getCollationKey()
   {
      return collationKey;
   }

   public void setGroupId(String id)
   {
      groupId = id;
   }

   public String getGroupId()
   {
      return groupId;
   }

   public Bib2GlsEntry createParent(TeXParser texParser)
   {
      if (orgParentValue == null)
      {
         return null;
      }

      String label = getParent();
      String orgLabel = label;

      if (label == null)
      {
         return null;
      }

      Bib2GlsEntry parentEntry = new Bib2GlsIndex(bib2gls);

      if (labelPrefix != null && label.startsWith(labelPrefix))
      {
         parentEntry.setId(labelPrefix, label.substring(labelPrefix.length()));
      }
      else
      {
         parentEntry.setId(null, label);
      }

      parentEntry.base = base;

      parentEntry.putField("name", orgParentValue);

      try
      {
         parentEntry.parseFields(texParser);
      }
      catch (TeXSyntaxException e)
      {
         bib2gls.error(bib2gls.getMessage( 
           "error.create.missing.parent.failed", orgLabel, getId(), 
             e.getMessage(bib2gls)));
         bib2gls.debug(e);
      }
      catch (IOException e)
      {
         bib2gls.error(bib2gls.getMessage( 
           "error.create.missing.parent.failed", orgLabel, getId(), 
             e.getMessage()));
         bib2gls.debug(e);
      }

      String type = getFieldValue("type");

      if (type != null)
      {
         parentEntry.putField("type", type);
      }

      return parentEntry;
   }

   public static Bib2GlsEntry getEntry(String entryId,
     Vector<Bib2GlsEntry> entries)
   {
      for (Bib2GlsEntry entry : entries)
      {
         if (entry.getId().equals(entryId))
         {
            return entry;
         }
      }

      return null;
   }

   public int getLevel(Vector<Bib2GlsEntry> entries)
   {
      String parentId = getParent();

      if (parentId == null) return 0;

      Bib2GlsEntry parent = getEntry(parentId, entries);

      if (parent != null)
      {
         return parent.getLevel(entries)+1;
      }

      return 0;
   }

   public void moveUpHierarchy(Vector<Bib2GlsEntry> entries)
   {
      String parentId = getParent();
      String childId = getId();

      if (parentId == null)
      {
         return;
      }

      Bib2GlsEntry parent = getEntry(parentId, entries);

      String grandparentId = null;

      if (parent != null)
      {
         parent.removeChild(childId);

         grandparentId = parent.getParent();
      }

      if (grandparentId == null)
      {
         removeField("parent");
         removeFieldValue("parent");
         return;
      }

      Bib2GlsEntry grandparent = getEntry(grandparentId, entries);

      if (grandparent == null) return;

      grandparent.addChild(this);

      putField("parent", parent.getField("parent"));
      putField("parent", grandparentId);
   }

   private void addHierarchy(Bib2GlsEntry entry, Vector<Bib2GlsEntry> entries)
     throws Bib2GlsException
   {
      if (hierarchy.contains(entry))
      {
         throw new Bib2GlsException(bib2gls.getMessage(
            "error.cyclic.hierarchy", entry.getId()));
      }

      hierarchy.add(0,entry);

      String parentId = entry.getParent();

      if (parentId == null)
      {
         return;
      }

      Bib2GlsEntry parent = getEntry(parentId, entries);

      if (parent == null)
      {
         if (resource.isStripMissingParentsEnabled())
         {
            bib2gls.verboseMessage(
              "message.removing.missing.parent", parentId, entry.getId());
            entry.removeField("parent");
            entry.fieldValues.remove("parent");
         }
         else
         {
            bib2gls.warningMessage(
              "warning.cant.find.parent", parentId, entry.getId());
         }
      }
      else
      {
         addHierarchy(parent, entries);
      }
   }

   public void updateHierarchy(Vector<Bib2GlsEntry> entries)
     throws Bib2GlsException
   {
      hierarchy = new Vector<Bib2GlsEntry>();

      addHierarchy(this, entries);
   }

   public int getHierarchyCount()
   {
      return hierarchy == null ? 0 : hierarchy.size();
   }

   public Bib2GlsEntry getHierarchyElement(int i)
   {
      return hierarchy.get(i);
   }

   public Number getNumericSort()
   {
      return numericSort;
   }

   public void setNumericSort(Number num)
   {
      numericSort = num;
   }

   public Object getSortObject()
   {
      return sortObject;
   }

   public void setSortObject(Object obj)
   {
      sortObject = obj;
   }

   public void addChild(Bib2GlsEntry child)
   {
      if (children == null)
      {
         children = new Vector<Bib2GlsEntry>();
      }

      if (!children.contains(child))
      {
         children.add(child);
      }
   }

   public int getChildCount()
   {
      return children == null ? 0 : children.size();
   }

   public Vector<Bib2GlsEntry> getChildren()
   {
      return children;
   }

   public Bib2GlsEntry getChild(int i)
   {
      return children.get(i);
   }

   private Bib2GlsEntry removeChild(String id)
   {
      if (children == null) return null;

      for (int i = 0; i < children.size(); i++)
      {
         if (children.get(i).getId().equals(id))
         {
            return children.remove(i);
         }
      }

      return null;
   }

   public String toString()
   {
      return getId();
   }

   public void setSelected(boolean selected)
   {
      this.selected = selected;
   }

   public boolean isSelected()
   {
      return selected;
   }

   private Vector<GlsRecord> records;
   private HashMap<String,Vector<GlsRecord>> recordMap;
   private Vector<GlsRecord> ignoredRecords;

   private Vector<GlsRecord> supplementalRecords;
   private HashMap<TeXPath,Vector<GlsRecord>> supplementalRecordMap;

   private Vector<GlsRecord> primaryRecords = null;

   private boolean selected = false;

   private String base="";

   private String originalEntryType;

   private Vector<Bib2GlsEntry> children;

   private HashMap<String,String> fieldValues;

   private Vector<String> deps;

   private Vector<Bib2GlsEntry> hierarchy;

   private Vector<Bib2GlsEntry> crossRefdBy;

   private BibValueList orgParentValue=null;

   private String crossRefTag = null;
   private String[] crossRefs = null;
   private String[] alsocrossRefs = null;

   public static final int NO_SEE=0, PRE_SEE=1, POST_SEE=2;

   protected Bib2Gls bib2gls;

   protected GlsResource resource;

   private CollationKey collationKey;

   private String groupId=null;

   private String labelPrefix = null, labelSuffix;

   private Bib2GlsEntry dual = null;

   private Number numericSort = null;

   private Object sortObject = null;

   private boolean fieldsParsed = false;

   private boolean triggerRecordFound=false;

   private Vector<String> locationList = null;

   private GlsRecord indexCounterRecord = null;

   private static final Pattern EXT_PREFIX_PATTERN = Pattern.compile(
     "ext(\\d+)\\.(.*)");

   private static final Pattern RANGE_PATTERN = Pattern.compile(
     "(\\(|\\))(.*)");
}
