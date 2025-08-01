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
package com.dickimawbooks.bibgls.bib2gls;

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

import com.dickimawbooks.bibgls.common.Bib2GlsException;

public class Bib2GlsEntry extends BibEntry
{
   public Bib2GlsEntry(Bib2Gls bib2gls)
   {
      this(bib2gls, "entry");
   }

   private Bib2GlsEntry(Bib2Gls bib2gls, long defIndex)
   {
      this(bib2gls, "entry", defIndex);
   }

   public Bib2GlsEntry(Bib2Gls bib2gls, String entryType)
   {
      this(bib2gls, entryType, defIndexCount++);
   }
   
   private Bib2GlsEntry(Bib2Gls bib2gls, String entryType, long defIndex)
   {
      super(entryType.toLowerCase());
      this.bib2gls = bib2gls;
      this.originalEntryType = entryType;
      this.defIndex = defIndex;

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

      String defIndexField = resource.getDefinitionIndexField();

      if (defIndexField != null)
      {
         BibValueList val = new BibValueList();

         val.add(new BibNumber(new UserNumber((int)defIndex)));
         putField(defIndexField, val);
         putField(defIndexField, ""+defIndex);
      }
   }

   public String getBase()
   {
      return base;
   }

   public File getBaseFile()
   {
      return baseFile;
   }

   public void setBase(File baseFile)
   {
      this.baseFile = baseFile;

      String name = baseFile.getName();

      if (name != null && name.endsWith(".bib"))
      {
         base = name.substring(0, name.length()-4);
      }
      else
      {
         base = name;
      }
   }

   @Deprecated
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

   public Bib2Gls getBib2Gls()
   {
      return bib2gls;
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

   @Override
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

   private void setOriginalId(String id)
   {
      super.setId(id);
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

   public String processLabel(String originalLabel, boolean isCs)
   {
      String label = originalLabel;

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

      if (resource.isInsertPrefixOnlyExists())
      {
         Bib2GlsEntry entry = resource.getEntry(label);

         if (entry == null)
         {
            return originalLabel;
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
         if (csname.startsWith("acr"))
         {
            bib2gls.warningMessage("warning.deprecated.cs",
             csname, "glsxtr"+csname.substring(3));
         }

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
   // With mfirstuc v2.08+, these should now be added as exclusions
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

   private void checkGlsCs(TeXObjectList list, 
      boolean mfirstucProtect, String fieldName)
    throws IOException
   {
      TeXParser parser = resource.getBibParser();

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
            String orgcsname = ((TeXCsRef)object).getName();

            GlsLike glsLike = bib2gls.getGlsLike(orgcsname);

            boolean isGlslike = (glsLike != null);

            String glsLikeLabelPrefix = (glsLike == null ? null : glsLike.getPrefix());

            boolean found = false;

            boolean mglslike = false;

            if (!isGlslike)
            {
               mglslike = bib2gls.isMglsCs(orgcsname);
            }

            String csname = orgcsname.toLowerCase();

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

                  if (bib2gls.isVerbose())
                  {
                     bib2gls.logMessage(bib2gls.getMessage(
                        "message.crossref.found", getId(),
                        object.toString(parser), label));
                  }

                  addParsedDependency(label, orgcsname, glsLike, mglslike);

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

                     if (bib2gls.isVerbose())
                     {
                        bib2gls.logMessage(bib2gls.getMessage(
                           "message.crossref.found", getId(),
                           object.toString(parser), label));
                     }

                     addParsedDependency(label, orgcsname, glsLike, mglslike);
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

                  if (bib2gls.isVerbose())
                  {
                     bib2gls.logMessage(bib2gls.getMessage(
                        "message.crossref.found", getId(),
                        object.toString(parser), label));
                  }

                  addParsedDependency(label, orgcsname, glsLike, mglslike);
               }
               else if (isGlslike || mglslike || isGlsCsOptLabel(csname))
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

                     if (code == '*' || code == '+' || code == bib2gls.getAltModifier())
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

                  // Don't replace the label for \dgls \mgls etc
                  // or the \gls-like commands that may have the
                  // prefix hidden from bib2gls.

                  if (mglslike)
                  {
                     if (bib2gls.isVerbose())
                     {
                        bib2gls.logMessage(bib2gls.getMessage(
                           "message.compoundcrossref.found", getId(),
                           object.toString(parser), label));
                     }

                     CompoundEntry comp = bib2gls.getCompoundEntry(label);

                     if (comp == null)
                     {
                        bib2gls.warningMessage(
                          "warning.unknown_compound_label.in_entry", getId());
                     }
                     else
                     {
                        for (String elem : comp.getElements())
                        {
                           addParsedDependency(elem, orgcsname, glsLike, mglslike);
                        }
                     }

                     bib2gls.addMglsRef(label);
                  }
                  else
                  {
                     if (glsLikeLabelPrefix != null && !glsLikeLabelPrefix.isEmpty())
                     {
                        String newLabel = label;

                        String defPrefix = resource.getLabelPrefix();

                        if (!(defPrefix != null && !defPrefix.isEmpty()
                              && glsLikeLabelPrefix.startsWith(defPrefix)))
                        {
                           // don't automatically insert the
                           // label-prefix if the glslike command
                           // starts with the same prefix
                           newLabel = processLabel(label, true);

                           if (newLabel.startsWith(glsLikeLabelPrefix))
                           {
                              newLabel = newLabel.substring(glsLikeLabelPrefix.length());
                           }

                           if (!label.equals(newLabel))
                           {
                              for ( ; i > start; i--)
                              {
                                 list.remove(i);
                              }

                              list.set(i, parser.getListener().createGroup(newLabel));
                           }
                        }

                        label = glsLikeLabelPrefix+newLabel;
                     }
                     else
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

                     if (bib2gls.isVerbose())
                     {
                        bib2gls.logMessage(bib2gls.getMessage(
                           "message.crossref.found", getId(),
                           object.toString(parser), label));
                     }

                     addParsedDependency(label, orgcsname, glsLike, mglslike);
                  }

                  if (bib2gls.checkNestedLinkTextField(fieldName)
                   && !csname.equals("glsps") && !csname.equals("glspt"))
                  {
                     if (csname.equals("glsadd"))
                     {
                       bib2gls.warning(parser, 
                         bib2gls.getMessage("warning.glsadd.in.field",
                         getId(), fieldName, label));
                     }
                     else
                     {
                       bib2gls.warning(parser, 
                         bib2gls.getMessage("warning.potential.nested.link",
                         getId(), fieldName,
                         String.format("\\%s%s%s", ((TeXCsRef)object).getName(), 
                           pre, opt),
                         label));
                     }
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

            checkGlsCs((TeXObjectList)object, false, fieldName);
         }
      }
   }

   protected boolean fieldsParsed()
   {
      return fieldsParsed;
   }

   @Override
   public boolean checkField(String field) throws BibTeXSyntaxException
   {
      if (bib2gls.isCheckNonBibFieldsOn())
      {
         if (bib2gls.isNonBibField(field))
         {
            bib2gls.warningMessage("warning.non_bib_field", 
             field, base==null?"":base, getOriginalId());
         }
      }

      return true;
   }

   public void parseFields() throws Bib2GlsException,IOException
   {
      TeXParser parser = resource.getBibParser();

      if (fieldsParsed) return;

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.logMessage(bib2gls.getMessage("message.parsing.fields", getId()));
      }

      fieldsParsed = true;

      if (resource.hasFieldAliases())
      {
         if (bib2gls.isVerbose())
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

               if (bib2gls.isVerbose())
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
         int action = resource.getSaveOriginalIdAction();

         if (action == GlsResource.SAVE_ORIGINAL_ALWAYS
          || (action == GlsResource.SAVE_ORIGINAL_NO_OVERRIDE 
               && getField(idField) == null)
          || (!getId().equals(getOriginalId()) && 
              (action == GlsResource.SAVE_ORIGINAL_CHANGED_OVERRIDE ||
              (action == GlsResource.SAVE_ORIGINAL_CHANGED_NO_OVERRIDE
                 && getField(idField) == null))))
         {
            BibUserString bibVal = new BibUserString(
               parser.getListener().createString(getOriginalId()));
            BibValueList val = new BibValueList();
            val.add(bibVal);
            putField(idField, val);
            putField(idField, getOriginalId());
         }
      }

      String entryTypeField = resource.getSaveOriginalEntryTypeField();

      if (entryTypeField != null && bib2gls.isKnownField(entryTypeField))
      {
         int action = resource.getSaveOriginalEntryTypeAction();

         if (action == GlsResource.SAVE_ORIGINAL_ALWAYS
          || (action == GlsResource.SAVE_ORIGINAL_NO_OVERRIDE 
               && getField(entryTypeField) == null)
          || (!getEntryType().equals(getOriginalEntryType()) && 
              (action == GlsResource.SAVE_ORIGINAL_CHANGED_OVERRIDE ||
              (action == GlsResource.SAVE_ORIGINAL_CHANGED_NO_OVERRIDE
                 && getField(entryTypeField) == null))))
         {
            BibUserString bibVal = new BibUserString(
               parser.getListener().createString(getOriginalEntryType()));
            BibValueList val = new BibValueList();
            val.add(bibVal);
            putField(entryTypeField, val);
            putField(entryTypeField, getOriginalEntryType());
         }
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
               resource.applyShortCaseChange(value));
         }
      }

      if (resource.changeLongCase())
      {
         BibValueList value = getField("long");

         if (value != null)
         {
            TeXObjectList list = value.expand(parser);

            putField("long", 
               resource.applyLongCaseChange(value));
         }
      }

      if (resource.changeDescriptionCase())
      {
         BibValueList value = getField("description");

         if (value != null)
         {
            putField("description", 
               resource.applyDescriptionCaseChange(value));
         }
      }

      if (resource.changeDualShortCase())
      {
         BibValueList value = getField("dualshort");

         if (value != null)
         {
            putField("dualshort", 
               resource.applyShortCaseChange(value));
         }
      }

      if (resource.changeDualLongCase())
      {
         BibValueList value = getField("duallong");

         if (value != null)
         {
            putField("duallong", 
               resource.applyLongCaseChange(value));
         }
      }

      String shortPluralSuffix = resource.getShortPluralSuffix();
      String dualShortPluralSuffix = resource.getDualShortPluralSuffix();

      appendShortPluralSuffix("short", "shortplural", shortPluralSuffix);

      appendShortPluralSuffix("dualshort", "dualshortplural", 
        dualShortPluralSuffix);

      applyFieldReplication();

      String groupVal = resource.getGroupField();

      if (groupVal != null)
      {
         putField("group", groupVal);
      }

      Vector<String> interpretFields = null;

      for (String field : fields)
      {
         interpretFields = processField(field, mfirstucProtect,
           protectFields, idField, interpretFields);
      }

      interpretFields = processSpecialFields(mfirstucProtect,
           protectFields, idField, interpretFields);

      // The parent label must be set before field assignments can
      // be applied.

      interpretFields = applyFieldAssignments(mfirstucProtect,
           protectFields, idField, interpretFields);

      CompoundEntry compEntry = resource.getCompoundAdjustName(getId());

      if (compEntry != null)
      {
         compoundAdjustName(compEntry);
      }

      // the name can't have its case changed until it's been
      // checked and been assigned a fallback if not present.

      if (resource.changeNameCase())
      {
         changeNameCase();
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

      changeFieldCase();

      if (interpretFields != null)
      {
         byte action = resource.getInterpretFieldAction();

         for (String field : interpretFields)
         {
            BibValueList value = getField(field);
            TeXObjectList list = value.expand(parser);

            String orgStrVal = list.toString(parser);

            String newStrVal = bib2gls.replaceSpecialChars(
              bib2gls.interpret(orgStrVal, value, bib2gls.isTrimFieldOn(field)));

            if (!orgStrVal.equals(newStrVal) && 
                (!newStrVal.isEmpty() 
                   || action == GlsResource.INTERPRET_FIELD_ACTION_REPLACE))
            {
               list.clear();
               list.addAll(parser.getListener().createString(newStrVal));
               value.clear();
               value.add(new BibUserString(list));

               putField(field, newStrVal);
            }
         }
      }

      String[] hexUnicodeFields = resource.getHexUnicodeFields();

      if (hexUnicodeFields != null)
      {
         for (String field : hexUnicodeFields)
         {
            BibValueList value = getField(field);

            if (value != null)
            {
               TeXObjectList list = value.expand(parser);

               if (convertUnicodeCharToHex(list))
               {
                  putField(field, list.toString(parser));
               }
            }
         }
      }

      // has the nonumberlist key been used?

      BibValueList noNumberList = getField("nonumberlist");

      if (noNumberList != null)
      {
         TeXObjectList list = noNumberList.expand(parser);
         String val = list.toString(parser);

         if (val.equals("true"))
         {
             nonumberlist = true;
         }
         else if (val.equals("false"))
         {
             nonumberlist = false;
         }
         else
         {
             throw new TeXSyntaxException(parser, "error.invalid.choice.value",
                val, "true, false");
         }
      }
   }

   private void applyFieldReplication() throws IOException
   {
      String shortPluralSuffix = resource.getShortPluralSuffix();
      String dualShortPluralSuffix = resource.getDualShortPluralSuffix();

      if (resource.hasFieldCopies())
      {
         boolean override = resource.isReplicateOverrideOn();
         MissingFieldAction missingAction
            = resource.getFallbackOnMissingReplicateAction();

         boolean updateShortPlural = false;
         boolean updateDualShortPlural = false;

         for (Iterator<String> it=resource.getFieldCopiesIterator();
              it.hasNext();)
         {
            String field = it.next();

            BibValueList val = getField(field);

            if (val == null)
            {
               if (missingAction == MissingFieldAction.FALLBACK)
               {
                  val = getFallbackContents(field);
               }
               else if (missingAction == MissingFieldAction.EMPTY)
               {
                  val = new BibValueList();
               }
            }

            if (val != null)
            {
               Vector<String> dupList = resource.getFieldCopy(field);

               for (String dup : dupList)
               {
                  if (override || getField(dup) == null)
                  {
                     BibValueList dupValue = (BibValueList)val.clone();

                     if (dup.equals("description") 
                          && resource.changeDescriptionCase())
                     {
                        dupValue = resource.applyDescriptionCaseChange(dupValue);
                     }
                     else if (dup.equals("short"))
                     {
                        if (resource.changeShortCase())
                        {
                           dupValue = resource.applyShortCaseChange(dupValue);
                        }

                        if (shortPluralSuffix != null)
                        {
                           updateShortPlural = true;
                        }
                     }
                     else if (dup.equals("long"))
                     {
                        if (resource.changeLongCase())
                        {
                           dupValue = resource.applyLongCaseChange(dupValue);
                        }
                     }
                     else if (dup.equals("dualshort"))
                     {
                        if (resource.changeDualShortCase())
                        {
                           dupValue = resource.applyShortCaseChange(dupValue);
                        }

                        if (dualShortPluralSuffix != null)
                        {
                           updateDualShortPlural = true;
                        }
                     }
                     else if (dup.equals("duallong"))
                     {
                        if (resource.changeDualLongCase())
                        {
                           dupValue = resource.applyLongCaseChange(dupValue);
                        }
                     }

                     putField(dup, dupValue);
                  }
               }
            }
         }

         if (updateShortPlural)
         {
            appendShortPluralSuffix("short", "shortplural", 
              shortPluralSuffix);
         }

         if (updateDualShortPlural)
         {
            appendShortPluralSuffix("dualshort", "dualshortplural", 
              dualShortPluralSuffix);
         }
      }
   }

   private Vector<String> applyFieldAssignments(boolean mfirstucProtect,
         String[] protectFields, String idField, Vector<String> interpretFields)
     throws Bib2GlsException,IOException
   {
      String shortPluralSuffix = resource.getShortPluralSuffix();
      String dualShortPluralSuffix = resource.getDualShortPluralSuffix();

      Vector<FieldAssignment> fieldAssignments = resource.getFieldAssignments();

      if (fieldAssignments != null)
      {
         boolean updateShortPlural = false;
         boolean updateDualShortPlural = false;

         for (FieldAssignment assignSpec : fieldAssignments)
         {
            boolean override = assignSpec.isFieldOverrideOn(resource);

            String field = assignSpec.getDestinationField();

            if (override || getField(field) == null)
            {
               if (bib2gls.isDebuggingOn())
               {
                  bib2gls.logAndPrintMessage("Entry "+getId()
                    + " evaluating assignment "+assignSpec);
               }

               BibValue val = assignSpec.getValue(this);

               if (val != null)
               {
                  if (bib2gls.isDebuggingOn())
                  {
                     bib2gls.logAndPrintMessage("Value: " + val);
                  }

                  BibValue copy = (BibValue)val.clone();
                  BibValueList value;

                  if (copy instanceof BibValueList)
                  {
                     value = (BibValueList)copy;
                  }
                  else
                  {
                     value = new BibValueList();
                     value.add(copy);
                  }

                  if (field.equals("description") 
                       && resource.changeDescriptionCase())
                  {
                     value = resource.applyDescriptionCaseChange(value);
                  }
                  else if (field.equals("short"))
                  {
                     if (resource.changeShortCase())
                     {
                        value = resource.applyShortCaseChange(value);
                     }

                     if (shortPluralSuffix != null)
                     {
                        updateShortPlural = true;
                     }
                  }
                  else if (field.equals("long"))
                  {
                     if (resource.changeLongCase())
                     {
                        value = resource.applyLongCaseChange(value);
                     }
                  }
                  else if (field.equals("dualshort"))
                  {
                     if (resource.changeDualShortCase())
                     {
                        value = resource.applyShortCaseChange(value);
                     }

                     if (dualShortPluralSuffix != null)
                     {
                        updateDualShortPlural = true;
                     }
                  }
                  else if (field.equals("duallong"))
                  {
                     if (resource.changeDualLongCase())
                     {
                        value = resource.applyLongCaseChange(value);
                     }
                  }

                  putField(field, value);

                  if (bib2gls.isDebuggingOn())
                  {
                     TeXParser parser = resource.getBibParser();

                     bib2gls.logAndPrintMessage("Setting "+field
                       + "=" + value.expand(parser).toString(parser));
                  }

                  interpretFields = processField(field, mfirstucProtect,
                     protectFields, idField, interpretFields);
               }
               else if (bib2gls.isDebuggingOn())
               {
                  bib2gls.logAndPrintMessage(
                   String.format("Value for field '%s' can't be obtained", field));
               }
            }
         }
      }

      return interpretFields;
   }

   private boolean convertUnicodeCharToHex(TeXObjectList list)
   {
      TeXParser parser = resource.getBibParser();

      boolean changed = false;

      for (int i = 0; i < list.size(); i++)
      {
         TeXObject obj = list.get(i);

         if (obj instanceof TeXObjectList)
         {
            if (convertUnicodeCharToHex((TeXObjectList)obj))
            {
               changed = true;
            }
         }
         else if (obj instanceof CharObject)
         {
            TeXObjectList subList = new TeXObjectList();

            subList.add(new TeXCsRef("bibglshexunicodechar"));
            subList.add(parser.getListener().createGroup(String.format("%X",
             ((CharObject)obj).getCharCode())));

            list.set(i, subList);

            changed = true;
         }
      }

      return changed;
   }

   protected Vector<String> processSpecialFields(
     boolean mfirstucProtect, String[] protectFields, String idField,
     Vector<String> interpretFields)
    throws IOException, Bib2GlsException
   {
       String defIndexField = resource.getDefinitionIndexField();

       if (defIndexField != null)
       {
          interpretFields = processField(defIndexField,
            mfirstucProtect, protectFields, idField,
            interpretFields);
       }

       return interpretFields;
   }

   protected Vector<String> processField(String field,
     boolean mfirstucProtect, String[] protectFields, String idField,
     Vector<String> interpretFields)
    throws IOException, Bib2GlsException
   {
      TeXParser parser = resource.getBibParser();

      BibValueList value = getField(field);

      if (value == null || field.equals(idField))
      {
         return interpretFields;
      }

      // expand any variables

      TeXObjectList list = value.expand(parser);

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.debug(String.format(">> %s={%s}", field, list.toString(parser)));
      }

      if (value.size() > 1 
         || !(value.firstElement() instanceof BibUserString))
      {
         BibUserString bibVal = new BibUserString(list);
         value.clear();
         value.add(bibVal);
      }

      if (resource.isBibTeXAuthorField(field))
      {
         list = convertBibTeXAuthorField(field, value);
         value.clear();
         value.add(new BibUserString(list));
      }

      TeXObject suffix = resource.getAppendPrefixFieldObject(field, list);

      if (suffix != null)
      {
         list.add(suffix);
         value.clear();
         value.add(new BibUserString(list));
      }

      String integerFieldFormat = resource.getIntegerFieldFormat(field);
      String decimalFieldFormat = resource.getDecimalFieldFormat(field);

      if (integerFieldFormat != null)
      {
         String newVal = null;

         if (list.size() == 1 && (list.firstElement() instanceof TeXNumber))
         {
            int num = ((TeXNumber)list.firstElement()).getValue();
            newVal = String.format(integerFieldFormat, num);
         }
         else
         {
            String valStr = list.toString(parser);

            try
            {
               int num = Integer.parseInt(valStr);
               newVal = String.format(integerFieldFormat, num);
            }
            catch (NumberFormatException e)
            {// not an integer

               if (decimalFieldFormat != null)
               {
                  try
                  {
                     double num = Double.parseDouble(valStr);
                     newVal = String.format(decimalFieldFormat, num);
                  }
                  catch (NumberFormatException e2)
                  {// not a number
                  }
               }
            }
         }

         if (newVal != null)
         {
            list = parser.getListener().createString(newVal);
            value.clear();
            value.add(new BibUserString(list));
         }
      }
      else if (decimalFieldFormat != null)
      {
         String newVal = null;

         if (list.size() == 1 && (list.firstElement() instanceof TeXNumber))
         {
            double num = (double)((TeXNumber)list.firstElement()).getValue();
            newVal = String.format(decimalFieldFormat, num);
         }
         else
         {
            String valStr = list.toString(parser);

            try
            {
               double num = Double.parseDouble(valStr);
               newVal = String.format(decimalFieldFormat, num);
            }
            catch (NumberFormatException e)
            {// not a number
            }
         }

         if (newVal != null)
         {
            list = parser.getListener().createString(newVal);
            value.clear();
            value.add(new BibUserString(list));
         }
      }

      String encap = resource.getFieldEncap(field);

      if (encap != null)
      {
         Group grp = parser.getListener().createGroup();
         grp.addAll(list);
         list.clear();
         list.add(new TeXCsRef(encap));
         list.add(grp);
         value.clear();
         value.add(new BibUserString(list));
      }

      encap = resource.getFieldEncapIncLabel(field);

      if (encap != null)
      {
         Group grp = parser.getListener().createGroup();
         grp.addAll(list);
         list.clear();
         list.add(new TeXCsRef(encap));
         list.add(grp);
         list.add(parser.getListener().createGroup(getId()));
         value.clear();
         value.add(new BibUserString(list));
      }

      if (resource.isInterpretField(field))
      {
         if (interpretFields == null)
         {
            interpretFields = new Vector<String>();
         }

         interpretFields.add(field);
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

      if (resource.isDependencyListField(field))
      {
         parseCustomDependencyList(list, field);
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
      else
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

         checkGlsCs(list, protect, field);

         if (field.equals("description"))
         {
            checkDescriptionField(list);
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

      if (bib2gls.isDebuggingOn())
      {
         bib2gls.debug(String.format("=>> %s={%s}", field, getFieldValue(field)));
      }

      return interpretFields;
   }

   protected void checkDescriptionField(TeXObjectList list)
    throws IOException, Bib2GlsException
   {
      TeXParser parser = resource.getBibParser();

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

           if (resource.isPostDescriptionDotExcludeTrue(this))
           {
              break;
           }

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

   protected void appendShortPluralSuffix(
     String shortField, String shortPluralField, String suffix)
    throws IOException
   {
      TeXParser parser = resource.getBibParser();

      if (suffix == null || suffix.isEmpty()) return;

      BibValueList value = getField(shortPluralField);

      if (value != null) return;

      value = getField(shortField);

      if (value == null) return;

      TeXObjectList newVal = (TeXObjectList)value.getContents(true);

      BibValueList list = new BibValueList();

      if (newVal != null)
      {
         list.add(new BibUserString(newVal));
      }

      list.add(new BibUserString(parser.getListener().createString(suffix)));

      putField(shortPluralField, list);
   }

   protected boolean changeNameAlsoCopyToText(boolean nameProvided)
   {
      return true;
   }

   protected void changeNameCase()
    throws IOException
   {
      TeXParser parser = resource.getBibParser();

      BibValueList value = getField("name");
      boolean nameProvided = true;

      if (value == null)
      {
         nameProvided = false;

         value = getFallbackContents("name");

         if (value == null)
         {
            return;
         }

         value = (BibValueList)value.clone();
      }

      if (changeNameAlsoCopyToText(nameProvided))
      {
         BibValueList textValue = getField("text");

         if (textValue == null)
         {
            putField("text", value);
            putField("text", value.expand(parser).toString(parser));
         }
      }

      value = resource.applyNameCaseChange(value);

      TeXObjectList list = BibValueList.stripDelim(value.expand(parser));

      putField("name", value);
      putField("name", list.toString(parser));
   }

   protected void compoundAdjustName(CompoundEntry comp)
    throws IOException
   {
      TeXParser parser = resource.getBibParser();
      TeXParserListener listener = parser.getListener();

      BibValueList value = getField("name");
      boolean nameProvided = true;

      if (value == null)
      {
         nameProvided = false;

         value = getFallbackContents("name");

         if (value == null)
         {
            return;
         }

         value = (BibValueList)value.clone();
      }

      if (changeNameAlsoCopyToText(nameProvided))
      {
         BibValueList textValue = getField("text");

         if (textValue == null)
         {
            putField("text", value);
            putField("text", value.expand(parser).toString(parser));

         }
      }

      BibValueList newList = new BibValueList();

      TeXObjectList newContent = new TeXObjectList();
      Group grp1 = listener.createGroup();
      Group grp2 = listener.createGroup();
      Group grp3 = listener.createGroup();

      newContent.add(new TeXCsRef("glsxtrmultientryadjustedname"));
      newContent.add(grp1);
      newContent.add(grp2);
      newContent.add(grp3);
      newContent.add(listener.createGroup(comp.getLabel()));

      TeXObjectList list = BibValueList.stripDelim(value.expand(parser));
      grp2.addAll(list);

      Group g = grp1;
      String id = getId();
      String sep = "";

      for (String elem : comp.getElements())
      {
         if (comp.getMainLabel().equals(elem))
         {
            g = grp3;
            sep = "";
         }
         else
         {
            g.addAll(listener.createString(sep+elem));
            sep = ",";
         }
      }

      BibUserString content = new BibUserString(newContent);
      newList.add(content);

      putField("name", newList);
      putField("name", newContent.toString(parser));
   }

   protected void changeFieldCase()
    throws IOException
   {
      HashMap<String,String> map = resource.getFieldCaseOptions();

      if (map == null) return;

      for (Iterator<String> it = map.keySet().iterator();
           it.hasNext(); )
      {
         String field = it.next();
         String caseChangeOpt = map.get(field);

         if (!caseChangeOpt.equals("none"))
         {
            changeFieldCase(field, caseChangeOpt);
         }
      }
   }

   protected void changeFieldCase(String field, String caseChangeOpt)
    throws IOException
   {
      TeXParser parser = resource.getBibParser();

      BibValueList value = getField(field);

      if (value == null)
      {
         value = getFallbackContents(field);

         if (value == null)
         {
            return;
         }

         value = (BibValueList)value.clone();
      }

      value = resource.applyCaseChange(value, caseChangeOpt);

      TeXObjectList list = BibValueList.stripDelim(value.expand(parser));

      putField(field, value);
      putField(field, list.toString(parser));
   }

   public void convertFieldToDateTime(
     String field, String dateFormat,
     Locale dateLocale, boolean hasDate, boolean hasTime)
   throws IOException
   {
      TeXParser parser = resource.getBibParser();
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

   protected TeXObjectList convertBibTeXAuthorField(
     String field, BibValueList value)
   throws IOException
   {
      TeXParser parser = resource.getBibParser();

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
      String list = resource.getLocalisationText("sentence", "terminators");

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

   public String getSortFallbackField()
   {
      String field = resource.getCustomEntryDefaultSortField(originalEntryType);

      if (field != null)
      {
         return field;
      }

      return resource.getEntryDefaultSortField();
   }

   public String getPluralFallbackValue()
   {
      // get the parent 

      String parentid = fieldValues.get("parent");

      String value = null;

      if (parentid != null)
      {// if the name is missing or is the same as the parent
       // use the parent's plural

         Bib2GlsEntry parent = resource.getEntry(parentid);

         if (parent != null)
         {
            String name = getFieldValue("name");
            String parentName = parent.getFieldValue("name");

            if (name == null || name.equals(parentName))
            {
               value = parent.getFieldValue("plural");

               if (value == null)
               {
                  value = parent.getFallbackValue("plural");

                  if (value != null)
                  {
                     return value;
                  }
               }
               else
               {
                  return value;
               }
            }
         }
      }

      value = getFieldValue("text");

      if (value == null)
      {
         value = getFallbackValue("text");
      }

      if (value != null)
      {
         String suffix = resource.getPluralSuffix();

         return suffix == null ? value : value+suffix;
      }

      return null;
   }

   public String getSortFallbackValue()
   {
      String fallbackField = getSortFallbackField();

      String sep = resource.getFieldConcatenationSeparator();

      String fields[] = fallbackField.split("\\s*\\+\\s*");

      String value = null;

      for (String field : fields)
      {
         if (value != null)
         {
            value += sep;
         }

         if (field.equals("id"))
         {
            if (value == null)
            {
               value = getId();
            }
            else
            {
               value += getId();
            }
         }
         else if (field.equals("original id"))
         {
            if (value == null)
            {
               value = getOriginalId();
            }
            else
            {
               value += getOriginalId();
            }
         }
         else
         {
            String currentValue = fieldValues.get(field);

            if (currentValue == null)
            {
               currentValue = getFallbackValue(field);
            }

            if (currentValue != null)
            {
               if (value == null)
               {
                  value = currentValue;
               }
               else
               {
                  value += currentValue;
               }
            }
         }
      }

      return value;
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
      else if (field.equals("plural"))
      {
         return getPluralFallbackValue();
      }
      else if (field.equals("sort"))
      {
         return getSortFallbackValue();
      }
      else if (field.equals("first"))
      {
         String value = getFieldValue("text");

         if (value != null) return value;

         return getFallbackValue("text");
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
      else if (field.equals("prefixfirst"))
      {
         String value = getFieldValue("prefix");

         if (value != null) return value;

         return getFallbackValue("prefix");
      }
      else if (field.equals("prefixfirstplural"))
      {
         String value = getFieldValue("prefixplural");

         if (value != null) return value;

         return getFallbackValue("prefixplural");
      }
      else if (field.equals("dualprefixfirst"))
      {
         String value = getFieldValue("dualprefix");

         if (value != null) return value;

         return getFallbackValue("dualprefix");
      }
      else if (field.equals("dualprefixfirstplural"))
      {
         String value = getFieldValue("dualprefixplural");

         if (value != null) return value;

         return getFallbackValue("dualprefixplural");
      }

      return null;
   }

   public BibValueList getPluralFallbackContents()
   {
      // get the parent 

      String parentid = fieldValues.get("parent");

      BibValueList contents = null;

      if (parentid != null)
      {
         Bib2GlsEntry parent = resource.getEntry(parentid);

         if (parent != null)
         {
            BibValueList name = getField("name");
            BibValueList parentName = getField("name");

            if (name == null || name.equals(parentName))
            {
               contents = parent.getField("plural");

               if (contents == null)
               {
                  contents = parent.getFallbackContents("plural");

                  if (contents != null)
                  {
                     return contents;
                  }
               }
            }
            else
            {
               return contents;
            }
         }
      }

      contents = getField("text");

      if (contents == null)
      {
         contents = getFallbackContents("text");
      }

      return plural(contents, "glspluralsuffix");
   }

   public BibValueList getSortFallbackContents()
   {
      String fallbackField = getSortFallbackField();

      TeXObjectList list = null;

      String sep = resource.getFieldConcatenationSeparator();

      String fields[] = fallbackField.split("\\+");

      for (String field : fields)
      {
         BibValueList value = null;

         if (fallbackField.equals("original id") 
            || (fallbackField.equals("id") && labelPrefix == null && labelSuffix == null))
         {
            value = getIdField();
         }
         else if (fallbackField.equals("id"))
         {
            value = new BibValueList();
            value.add(new BibUserString(
               resource.getBibParserListener().createString(getId())));
         }
         else
         {
            value = getField(field);

            if (value == null)
            {
               value = getFallbackContents(field);
            }
         }

         if (fields.length == 1)
         {
            return value;
         }

         if (value != null)
         {
            try
            {
               TeXObjectList valueList = ((BibValueList)value.clone()).expand(
                                         resource.getBibParserListener().getParser());

               if (list == null)
               {
                  list = valueList;
               }
               else
               {
                  list.add(resource.getBibParserListener().createString(sep));
                  list.addAll(valueList);
               }
            }
            catch (IOException e)
            {
               bib2gls.debug(e);
               return value;
            }
         }
      }

      if (list == null) return null;

      BibValueList contents = new BibValueList();

      contents.add(new BibUserString(list));

      return contents;
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
      else if (field.equals("plural"))
      {
         return getPluralFallbackContents();
      }
      else if (field.equals("sort"))
      {
         return getSortFallbackContents();
      }
      else if (field.equals("first"))
      {
         BibValueList contents = getField("text");

         return contents == null ? getFallbackContents("text") : contents;
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
      else if (field.equals("prefixfirst"))
      {
         BibValueList contents = getField("prefix");

         return contents == null ? getFallbackContents("prefix") : contents;
      }
      else if (field.equals("prefixfirstplural"))
      {
         BibValueList contents = getField("prefixplural");

         return contents == null ? getFallbackContents("prefixplural") : contents;
      }
      else if (field.equals("dualprefixfirst"))
      {
         BibValueList contents = getField("dualprefix");

         return contents == null ? getFallbackContents("dualprefix") : contents;
      }
      else if (field.equals("dualprefixfirstplural"))
      {
         BibValueList contents = getField("dualprefixplural");

         return contents == null ? getFallbackContents("dualprefixplural") : contents;
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

   @Override
   public void parseContents(TeXParser parser,
    TeXObjectList contents, TeXObject endGroupChar)
     throws IOException
   {
      super.parseContents(parser, contents, endGroupChar);
      initMissingFields();
   }

   protected void initMissingFields()
   {
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
      String parentid = null;
      String plural = null;
      String sep = "";

      Set<String> keyset = getFieldSet();

      Vector<String> omitList = resource.getOmitFieldList(this);

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         if (omitList != null && omitList.contains(field))
         {
            bib2gls.verboseMessage("message.omitting.field", field, getId());
            continue;
         }

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
            if (field.equals("parent"))
            {
               parentid = value;
            }
            else if (field.equals("plural"))
            {
               plural = value;
            }

            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, value);
         }
         else if (bib2gls.isDebuggingOn() && 
            !bib2gls.isInternalField(field) &&
            !bib2gls.isKnownSpecialField(field))
         {
            bib2gls.debugMessage("warning.ignoring.unknown.field", field);
         }
      }

      if (name == null)
      {
         name = getFallbackValue("name");

         if (name == null) name = "";

         writePluralIfInherited(writer, name, parentid, plural, sep);
      }

      if (description == null)
      {
         description = "";
      }

      writer.println("}%");
      writer.println(String.format("{%s}%%", name));
      writer.println(String.format("{%s}", description));

      writeInternalFields(writer);
   }

   protected void writePluralIfInherited(PrintWriter writer, String name,
     String parentid, String plural, String sep)
     throws IOException
   {
      // check if this is a homograph (if it has the same name as
      // its parent then the plural should be the same unless
      // otherwise overridden)

      if (parentid != null && plural == null)
      {
         Bib2GlsEntry parent = resource.getEntry(parentid);

         if (parent != null)
         {
            String parentName = parent.getFieldValue("name");

            if (parentName == null)
            {
               parentName = parent.getFallbackValue("name");
            }

            if (name.equals(parentName))
            {
               plural = parent.getFieldValue("plural");

               if (plural == null)
               {
                  plural = parent.getFallbackValue("plural");
               }

               if (plural != null)
               {
                  writer.format("%splural={%s}", sep, plural);
               }
            }
         }
      }
   }

   public void writeInternalFields(PrintWriter writer) throws IOException
   {
   }

   public void writeLocList(PrintWriter writer)
   throws IOException
   {
      if (locationList == null || nonumberlist) return;

      for (String loc : locationList)
      {
         writer.println(String.format("\\glsxtrfieldlistadd{%s}{loclist}{%s}", 
           getId(), loc));
      }
   }

   public void writeExtraFields(PrintWriter writer)
   throws IOException
   {
      if (getEntryType().startsWith("spawned"))
      {
         String value = getFieldValue("progenitor");

         if (value != null)
         {
            writer.format("\\GlsXtrSetField{%s}{progenitor}{%s}%n", getId(), 
              value);
         }
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

      if (bib2gls.isTrimFieldOn(label))
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

   public void setParent(String parentId)
   {
      fieldValues.put("parent", parentId);
      sortLevel = -1;
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

   public boolean isSeeLabel(String label)
   {
      if (crossRefs == null) return false;

      for (int i = 0; i < crossRefs.length; i++)
      {
         if (crossRefs[i].equals(label))
         {
            return true;
         }
      }

      return false;
   }

   public String[] getAlsoCrossRefs()
   {
      return alsocrossRefs;
   }

   public boolean isSeeAlsoLabel(String label)
   {
      if (alsocrossRefs == null) return false;

      for (int i = 0; i < alsocrossRefs.length; i++)
      {
         if (alsocrossRefs[i].equals(label))
         {
            return true;
         }
      }

      return false;
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

   public String getCrossRefTail()
   {
      if (crossRefTail != null)
      {// already obtained tail

         if (crossRefTail.isEmpty())
         {
            return null;
         }
         else
         {
            if (bib2gls.isDebuggingOn())
            {
               bib2gls.logAndPrintMessage(
                 bib2gls.getMessage("message.crossref.tail", getId(), crossRefTail));

               bib2gls.logAndPrintMessage("[ " + getId()+" > "+crossRefTail+" ]");
            }

            return crossRefTail;
         }
      }

      Vector<String> tailList = new Vector<String>();

      String tail = getCrossRefTail(tailList);

      if (tail != null && bib2gls.isDebuggingOn())
      {
         bib2gls.logAndPrintMessage(
           bib2gls.getMessage("message.crossref.tail", getId(), tail));

         bib2gls.logAndPrintMessageNoLn("[");

         String sep = " ";

         for (String id : tailList)
         {
            bib2gls.logAndPrintMessageNoLn(sep+id);
            sep = ", ";
         }

         bib2gls.logAndPrintMessage(" ]");

      }

      return tail;
   }

   private String getCrossRefTail(Vector<String> tailList)
   {
      if (crossRefTail != null)
      {// already obtained tail

         if (crossRefTail.isEmpty())
         {
            return null;
         }
         else
         {
            if (bib2gls.isDebuggingOn())
            {
               tailList.add(getId());
            }

            return crossRefTail;
         }
      }

      String tail = getFieldValue("alias");

      if (tail == null)
      {
         if (crossRefs != null && crossRefs.length == 1)
         {
            tail = crossRefs[0];
         }
      }
      else if (crossRefs != null && crossRefs.length > 0)
      {
         crossRefTail = "";
         return null;
      }

      if (tail == null)
      {
         if (alsocrossRefs != null && alsocrossRefs.length == 1)
         {
            tail = alsocrossRefs[0];
         }
      }
      else if (alsocrossRefs != null && alsocrossRefs.length > 0)
      {
         crossRefTail = "";
         return null;
      }

      if (tail != null)
      {
         String id = getId();
         String currentTail = null;

         if (!tailList.isEmpty())
         {
            currentTail = tailList.lastElement();

            if (tailList.contains(tail))
            {
               crossRefTail = currentTail;
               return currentTail;
            }
         }

         Bib2GlsEntry entry = resource.getEntry(tail);

         if (entry == null)
         {
            crossRefTail = (currentTail == null ? "" : currentTail);

            return currentTail;
         }

         tailList.add(id);

         String newTail = entry.getCrossRefTail(tailList);

         if (newTail != null && !id.equals(newTail))
         {
            tail = newTail;
         }

         if (id.equals(tail))
         {
            tail = null;
         }
      }

      crossRefTail = (tail == null ? "" : tail);

      return tail;
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

   /**
    * Adds a label found from parsing a field value to the list of dependencies.
    * The extra parameters allow for possible future options which may allow for
    * conditional appending for "gather-parsed-dependencies".
    * @param label the label to be added to the list of dependencies
    */
   protected void addParsedDependency(String label,
     String csname, GlsLike glslike, boolean mglslike)
    throws IOException
   {
      TeXParser parser = resource.getBibParser();

      if (!label.equals(getId()))
      {
         if (!deps.contains(label))
         { 
            deps.add(label);
         }

         String field = resource.getGatherParsedDependenciesField();

         if (field != null)
         {
            String strVal = getFieldValue(field);
            BibValueList bibList = null;
            TeXObjectList listVal = null;
            boolean changed = true;

            if (strVal == null)
            {
               bibList = getField(field);

               if (bibList != null)
               {
                  listVal = bibList.expand(parser);
                  strVal = listVal.toString(parser);
               }
            }

            if (strVal == null || strVal.isEmpty())
            {
               strVal = label;
               listVal = parser.getListener().createString(label);
            }
            else
            {
               TeXObject prefix = null;
               String labelList = strVal;

               if (strVal.startsWith("["))
               {
                  if (listVal == null)
                  {
                     if (bibList == null)
                     {
                        bibList = getField(field);
                     }

                     if (bibList != null)
                     {
                        listVal = bibList.expand(parser);
                     }
                  }

                  if (listVal == null)
                  {
                     int idx = strVal.lastIndexOf(']');

                     if (idx > 0)
                     {
                        labelList = strVal.substring(idx+1);
                        prefix = parser.getListener().createString(
                         strVal.substring(1, idx-1));
                     }
                  }
                  else
                  {
                     prefix = listVal.popArg(parser, '[', ']');
                     strVal = listVal.toString(parser);
                  }
               }

               String[] split = labelList.split(" *, *");

               for (String s : split)
               {
                  if (s.equals(label))
                  {
                     changed = false;
                     break;
                  }
               }

               if (changed)
               {
                  if (prefix == null)
                  {
                     strVal += "," + label;
                     listVal = parser.getListener().createString(strVal);
                  }
                  else
                  {
                     listVal = parser.getListener().createStack();
                     listVal.add(parser.getListener().getOther('['));
                     listVal.add(prefix, true);
                     listVal.add(parser.getListener().getOther(']'));
                     listVal.addAll(parser.getListener().createString(strVal+","+label));

                     strVal = "[" + prefix + "]" + strVal + "," + label;
                  }
               }
            }

            if (changed)
            {
               bibList = new BibValueList();
               bibList.add(new BibUserString(listVal));

               putField(field, strVal);
               putField(field, bibList);
            }
         }
      }
   }

   public void removeDependency(String label)
   {
      deps.remove(label);
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

      if (resource.getSavePrimaryLocationSetting()
                 == GlsResource.SAVE_PRIMARY_LOCATION_REMOVE)
      {
         n += getPrimaryRecordCount();
      }

      return n;
   }

   /**
    Determines if this entry only has ignored records and does not have any 
    dependencies.
   */
   public boolean isIgnoredEntry()
   {
      if (ignoredRecords == null
          || (records != null && !records.isEmpty())
          || (supplementalRecords != null && !supplementalRecords.isEmpty())
          || (primaryRecords != null && !primaryRecords.isEmpty())
          || (recordMap != null && !recordMap.isEmpty())
          || !deps.isEmpty()
          || (crossRefdBy != null && !crossRefdBy.isEmpty())
          || resource.hasEntryMglsRecords(getId()))
      {
         return false;
      }

      return !ignoredRecords.isEmpty();
   }

   public int mainRecordCount()
   {
      int n = 0;

      if (records != null)
      {
         n = records.size();
      }
      else
      {
         for (String counter : resource.getLocationCounters())
         {
            n += recordMap.get(counter).size();
         }
      }

      return n;
   }

   public int getPrimaryRecordCount()
   {
      int n = 0;

      if (primaryRecords != null)
      {
         n = primaryRecords.size();
      }
      else if (primaryRecordMap != null)
      {
         for (Iterator<String> it = primaryRecordMap.keySet().iterator();
              it.hasNext(); )
         {
            String counter = it.next();

            n += primaryRecordMap.get(counter).size();
         }
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
      if (recordCount() > 0)
      {
         return true;
      }

      if (resource.hasEntryMglsRecords(getId()))
      {
         return true;
      }

      return false;
   }

   public void addRecord(GlsSeeRecord rec)
   {
      // add as an ignored rec

      addIgnoredRecord(new GlsRecord(bib2gls, rec.getLabel(),
       "", "page", "glsignore", ""));

      StringBuilder builder = new StringBuilder();

      if (crossRefTag == null)
      {
         crossRefTag = rec.getTag();
      }

      if (crossRefTag != null)
      {
         builder.append(String.format("[%s]", crossRefTag));
      }

      if (crossRefs == null)
      {
         crossRefs = rec.getXrLabels();

         for (int i = 0; i < crossRefs.length; i++)
         {
            if (bib2gls.isVerbose())
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

         String[] newRefs = rec.getXrLabels();

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

            if (bib2gls.isVerbose())
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

   public void addRecord(GlsRecord rec)
   {
      addRecord(rec, false);
   }

   public void addRecord(GlsRecord rec, boolean onlyIfPrimary)
   {
      if (rec.getFormat().equals("glsignore"))
      {
         bib2gls.debugMessage("message.ignored.record", rec);
         addIgnoredRecord(rec);
         return;
      }

      if (recordIndex == -1)
      {
         setRecordIndex(rec.getIndex());
      }

      if (rec.getFormat().equals("glstriggerrecordformat"))
      {
         triggerRecordFound = true;
         bib2gls.debugMessage("message.ignored.record", rec);
         addIgnoredRecord(rec);
         return;
      }


      if (indexCounterRecord == null)
      {
         String indexCounter = resource.getSaveIndexCounter();

         if (indexCounter != null && rec.getCounter().equals("wrglossary"))
         {
            if (indexCounter.equals("true"))
            {
               indexCounterRecord = rec;
            }
            else if (rec.getFormat().equals(indexCounter))
            {
               indexCounterRecord = rec;
            }
         }
      }

      GlsRecord primary = null;
      int setting = GlsResource.SAVE_PRIMARY_LOCATION_OFF;
      boolean addRecord = !onlyIfPrimary;

      if (resource.isPrimaryLocation(rec.getFormat()))
      {
         primary = rec;

         int primaryLocCounterSetting = resource.getPrimaryLocationCountersSetting();

         if (records != null || primaryLocCounterSetting
              == GlsResource.PRIMARY_LOCATION_COUNTERS_COMBINE)
         {
            if (primaryRecords == null)
            {
               primaryRecords = new Vector<GlsRecord>();
            }

            bib2gls.debugMessage("message.adding.primary.record", primary,
                 getId());
            primaryRecords.add(primary);
         }
         else
         {
            if (primaryRecordMap == null)
            {
               primaryRecordMap = new HashMap<String,Vector<GlsRecord>>();

               if (primaryLocCounterSetting
                     != GlsResource.PRIMARY_LOCATION_COUNTERS_MATCH)
               {
                  primaryCounters = new Vector<String>();
               }
            }

            String counter = primary.getCounter();

            if (resource.isPrimaryLocationCounterAllowed(counter))
            {
               Vector<GlsRecord> list = primaryRecordMap.get(counter);

               if (list == null)
               {
                  list = new Vector<GlsRecord>();

                  primaryRecordMap.put(counter, list);

                  if (primaryCounters != null)
                  {
                     primaryCounters.add(counter);
                  }
               }

               if (!list.contains(primary))
               {
                  bib2gls.debugMessage("message.adding.counter.primary.record", primary,
                    getId(), counter);

                  list.add(primary);
               }
            }
         }

         setting = resource.getSavePrimaryLocationSetting();

         if (onlyIfPrimary)
         {
            addRecord = true;
         }
      }

      if (!addRecord)
      {
         return;
      }

      if (records != null)
      {
         if (!records.contains(rec))
         {
            bib2gls.debugMessage("message.adding.record", rec,
             getId());

            if (primary != null 
                 && setting == GlsResource.SAVE_PRIMARY_LOCATION_DEFAULT_FORMAT)
            {
               bib2gls.debugMessage("message.changing.primary_record.format",
                 rec, "glsnumberformat", setting);

               rec = (GlsRecord)rec.clone();
               rec.setFormat("glsnumberformat");
               records.add(rec);
            }
            else if (primary == null
                || setting == GlsResource.SAVE_PRIMARY_LOCATION_RETAIN)
            {
               records.add(rec);
            }
            else if (setting == GlsResource.SAVE_PRIMARY_LOCATION_START)
            {
               records.add(primaryRecords.size()-1, rec);
            }
         }
      }
      else if (primary == null
                || setting != GlsResource.SAVE_PRIMARY_LOCATION_REMOVE)
      {
         String counter = rec.getCounter();
         Vector<GlsRecord> list = recordMap.get(counter);

         if (list != null && !list.contains(rec))
         {
            bib2gls.debugMessage("message.adding.counter.record", rec,
             getId(), counter);

            if (primary != null
               && setting == GlsResource.SAVE_PRIMARY_LOCATION_DEFAULT_FORMAT)
            {
               bib2gls.debugMessage("message.changing.primary_record.format",
                 rec, "glsnumberformat", setting);

               rec = (GlsRecord)rec.clone();
               rec.setFormat("glsnumberformat");
               list.add(rec);
            }
            else if (primary == null
                  || setting == GlsResource.SAVE_PRIMARY_LOCATION_RETAIN)
            {
               list.add(rec);
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
                     list.add(i, rec);
                     done=true;
                     break;
                  }
               }

               if (!done)
               {
                  list.add(rec);
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

   public void addSupplementalRecord(GlsRecord rec)
   {
      if (supplementalRecords == null)
      {
         supplementalRecords = new Vector<GlsRecord>();
      }

      if (!bib2gls.isMultipleSupplementarySupported())
      {
         String fmt = rec.getFormat();

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

         rec.setFormat(fmt);
      }
      else if (rec instanceof SupplementalRecord)
      {
         if (supplementalRecordMap == null)
         {
            supplementalRecordMap = new HashMap<TeXPath,Vector<GlsRecord>>();
         }

         TeXPath source = ((SupplementalRecord)rec).getSource();

         Vector<GlsRecord> list = supplementalRecordMap.get(source);

         if (list == null)
         {
            list = new Vector<GlsRecord>();
            supplementalRecordMap.put(source, list);
         }

         if (!list.contains(rec))
         {
            list.add(rec);
         }
      }

      if (!supplementalRecords.contains(rec))
      {
         bib2gls.debugMessage("message.adding.supplemental.record", getId());
         supplementalRecords.add(rec);
      }
   }

   public void addIgnoredRecord(GlsRecord rec)
   {
      if (ignoredRecords == null)
      {
         ignoredRecords = new Vector<GlsRecord>();
      }

      if (!ignoredRecords.contains(rec))
      {
         ignoredRecords.add(rec);
      }
   }

   public static void insertRecord(GlsRecord rec, Vector<GlsRecord> list)
   {
      for (int i = 0, n = list.size(); i < n; i++)
      {
         GlsRecord r = list.get(i);

         if (r.locationMatch(rec))
         {
            return;
         }

         if (r.partialMatch(rec))
         {
            if (!r.resolveConflict(rec))
            {
               list.add(i, rec);
            }

            return;
         }

         int result = rec.compareTo(r);

         if (result <= 0)
         {
            list.add(i, rec);
            return;
         }
      }

      list.add(rec);
   }

   public void copyRecordsFrom(Bib2GlsEntry entry)
   {
      if (getId().equals(entry.getId()))
      {
         bib2gls.debugMessage("message.copying.self_record", getId());
         return;
      }

      if (entry.records != null)
      {
         for (GlsRecord rec : entry.records)
         {
            bib2gls.debugMessage(
               "message.copying.record", rec,
                 entry.getId(), getId());

            if (rec.getFormat().equals("glsignore")
              || rec.getFormat().equals("glstriggerrecordformat"))
            {
               addIgnoredRecord(rec.copy(getId()));
            }
            else
            {
               insertRecord(rec.copy(getId()), records);
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

               for (GlsRecord rec : list)
               {
                  bib2gls.debugMessage(
                     "message.copying.record", rec,
                       entry.getId(), getId());

                  if (rec.getFormat().equals("glsignore")
                    || rec.getFormat().equals("glstriggerrecordformat"))
                  {
                     addIgnoredRecord(rec.copy(getId()));
                  }
                  else
                  {
                     insertRecord(rec.copy(getId()), thisList);
                  }
               }
            }
         }
      }

      if (entry.primaryRecords != null)
      {
         if (primaryRecords == null)
         {
            primaryRecords = new Vector<GlsRecord>();
         }

         for (GlsRecord rec : entry.primaryRecords)
         {
            bib2gls.debugMessage(
               "message.copying.primary.record", rec,
                 entry.getId(), getId());

            primaryRecords.add(rec);
         }
      }
      else if (entry.primaryRecordMap != null)
      {
         if (primaryRecordMap == null)
         {
            primaryRecordMap = new HashMap<String, Vector<GlsRecord>>();
         }

         for (Iterator<String> it = entry.primaryRecordMap.keySet().iterator();
              it.hasNext(); )
         {
            String counter = it.next();

            Vector<GlsRecord> list = primaryRecordMap.get(counter);

            if (list == null)
            {
               list = new Vector<GlsRecord>();
               primaryRecordMap.put(counter, list);
            }

            Vector<GlsRecord> otherList = entry.primaryRecordMap.get(counter);

            for (GlsRecord rec : otherList)
            {
               bib2gls.debugMessage(
                 "message.copying.primary.record", rec,
                   entry.getId(), getId());

               list.add(rec);
            }
         }
      }

      if (entry.supplementalRecords != null)
      {
         for (GlsRecord rec : entry.supplementalRecords)
         {
            bib2gls.debugMessage(
               "message.copying.record", rec,
                 entry.getId(), getId());

            addSupplementalRecord(rec.copy(getId()));
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
      GlsRecord implicitStart = null;
      GlsRecord explicitRangeStart = null;
      GlsRecord explicitRangeEnd = null;

      int[] maxGap = new int[1];
      maxGap[0] = 0;

      boolean start=true;
      int compact = resource.getCompactRanges();

      GlsRecord rangeStart=null;
      String rangeFmt = null;

      int startRangeIdx = 0;

      for (int i = 0, n = recordList.size(); i < n; i++)
      {
         GlsRecord rec = recordList.get(i);
         String delimN = (i == n-1 ? "\\bibglslastDelimN " : "\\bibglsdelimN ");

         locationList.add(rec.getListTeXCode());
   
         Matcher m = RANGE_PATTERN.matcher(rec.getFormat());
   
         if (m.matches())
         {
            char paren = m.group(1).charAt(0);

            if (paren == '(')
            {
               if (rangeStart != null)
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                    "error.nested.range", rec, rangeStart));
               }
   
               rangeStart = rec;
               rangeFmt = m.group(2);
   
               if (resource.isMergeRangesOn())
               {
                  explicitRangeStart = (GlsRecord)rec.clone();
                  explicitRangeStart.setFormat(rangeFmt);

                  if (prev != null && explicitRangeStart.follows(prev, gap, maxGap))
                  {
                     mid.setLength(0);
                  }
                  else if (implicitStart == null)
                  {
                     implicitStart = rangeStart;

                     if (builder == null)
                     {
                        builder = new StringBuilder();
                     }
                     else if (!start)
                     {
                        builder.append(delimN);
                        startRangeIdx = builder.length();
                     }

                     builder.append(rec.getFmtTeXCode());
                  }
                  else
                  {
                     if (builder == null)
                     {
                        builder = new StringBuilder();
                     }

                     builder.append(mid);
                     mid.setLength(0);
                     implicitStart = rangeStart;
                     builder.append(delimN);
                     startRangeIdx = builder.length();
                     builder.append(rec.getFmtTeXCode());
                  }

                  count = minRange;
                  prev = explicitRangeStart;
               }
               else
               {
                  implicitStart = null;
                  count = 0;
                  mid.setLength(0);
   
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
                  builder.append(rec.getFmtTeXCode());
               }

               explicitRangeEnd = null;
            }
            else
            {
               if (rangeStart == null)
               {
                  throw new Bib2GlsException(bib2gls.getMessage(
                    "error.range.missing.start", rec));
               }

               if (resource.isMergeRangesOn())
               {
                  explicitRangeEnd = (GlsRecord)rec.clone();
                  explicitRangeEnd.setFormat(rangeFmt);
                  prev = explicitRangeEnd;
                  explicitRangeStart = null;
               }
               else
               {
                  implicitStart = null;
                  count = 0;
                  mid.setLength(0);

                  builder.append("\\delimR ");
                  builder.append(rec.getFmtTeXCode(rangeStart, compact));
                  builder.append("}");
               }

               rangeStart = null;
               rangeFmt = null;
            }
         }
         else if (rangeStart != null)
         {
             String recordFmt = rec.getFormat();

             if (!(rangeStart.getPrefix().equals(rec.getPrefix())
               &&  rangeStart.getCounter().equals(rec.getCounter())))
             {
                bib2gls.warningMessage(
                    "error.inconsistent.range", rec, rangeStart);

                String content = String.format("\\bibglsinterloper{%s}", 
                  rec.getFmtTeXCode());

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
                  rec, rangeStart);
             }
             else
             {
                if (rec.getFormat().equals("glsnumberformat")
                 || rangeFmt.isEmpty())
                {
                   bib2gls.verboseMessage(
                      "message.inconsistent.range", rec, rangeStart);
                }
                else
                {
                   bib2gls.warningMessage(
                      "error.inconsistent.range", rec, rangeStart);
                }

                String content = String.format("\\bibglsinterloper{%s}", 
                  rec.getFmtTeXCode());

                builder.insert(startRangeIdx, content);

                startRangeIdx += content.length();
             }
   
         }
         else if (explicitRangeEnd != null)
         {
            if (!rec.follows(explicitRangeEnd, gap, maxGap))
            {
               builder.append(mid);
               builder.append("\\delimR ");
               builder.append(explicitRangeEnd.getFmtTeXCode());

               if (maxGap[0] > 1)
               {
                  builder.append("\\bibglspassim ");
               }

               maxGap[0] = 0;

               builder.append(delimN);
               builder.append(rec.getFmtTeXCode());
               mid.setLength(0);
               count = 1;
               maxGap[0] = 0;
               implicitStart = null;
            }
            else
            {
               mid.append(delimN);
               mid.append(rec.getFmtTeXCode());
            }

            prev = rec;
            explicitRangeEnd = null;
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

            builder.append(rec.getFmtTeXCode());
         }
         else if (minRange < Integer.MAX_VALUE
                  && rec.follows(prev, gap, maxGap))
         {
            if (count == 1)
            {
               implicitStart = prev;
            }

            count++;

            mid.append(delimN);
            mid.append(rec.getFmtTeXCode());
         }
         else if (count==2 && suffixF != null)
         {
            builder.append(suffixF);
            builder.append(delimN);
            builder.append(rec.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
            maxGap[0] = 0;
            implicitStart = null;
         }
         else if (count > 2 && suffixFF != null)
         {
            builder.append(suffixFF);
            builder.append(delimN);
            builder.append(rec.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
            maxGap[0] = 0;
            implicitStart = null;
         }
         else if (count >= minRange)
         {
            builder.append("\\delimR ");
            builder.append(prev.getFmtTeXCode(implicitStart, compact));

            if (maxGap[0] > 1)
            {
               builder.append("\\bibglspassim ");
            }

            maxGap[0] = 0;

            builder.append(delimN);
            builder.append(rec.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
            implicitStart = null;
         }
         else
         {
            builder.append(mid);
            builder.append(delimN);
            builder.append(rec.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
            maxGap[0] = 0;
            implicitStart = null;
         }

         prev = rec;
         start = false;
      }

      if (explicitRangeEnd != null)
      {
         if (builder == null)
         {
            builder = new StringBuilder();
         }

         builder.append(mid);
         builder.append("\\delimR ");
         builder.append(explicitRangeEnd.getFmtTeXCode());
      }
      else if (rangeStart != null)
      {
         throw new Bib2GlsException(bib2gls.getMessage(
           "error.range.missing.end", rangeStart));
      }
      else if (prev != null && mid.length() > 0)
      {
         if (count >= minRange)
         {
            builder.append("\\delimR ");
            builder.append(prev.getFmtTeXCode(implicitStart, compact));

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
      if (nonumberlist || resource.isSaveLocationsOff())
      {
         return;
      }

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

      boolean incAliases = resource.isSaveAliasLocationsOn();
      boolean incSee = resource.isSaveSeeLocationsOn();
      boolean incSeeAlso = resource.isSaveSeeAlsoLocationsOn();

      boolean hasLocationList = (numRecords > 0
        && resource.isSaveAnyNonIgnoredLocationsOn());

      String alias = getAlias();

      if (aliasLocation == PRE_SEE && alias != null && incAliases)
      {
         builder = new StringBuilder();
         builder.append("\\bibglsusealias{");
         builder.append(getId());
         builder.append("}");

         if (hasLocationList)
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
      else if (seeLocation == PRE_SEE && crossRefs != null && incSee)
      {
         builder = new StringBuilder();
         builder.append("\\bibglsusesee{");
         builder.append(getId());
         builder.append("}");

         if (hasLocationList)
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

            listBuilder.append(crossRefs[i]);
         }

         listBuilder.append("}{}");

         locationList.add(listBuilder.toString());
      }
      else if (seealsoLocation == PRE_SEE && alsocrossRefs != null && incSeeAlso)
      {
         builder = new StringBuilder();
         builder.append("\\bibglsuseseealso{");
         builder.append(getId());
         builder.append("}");

         if (hasLocationList)
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

      if (aliasLocation == POST_SEE && alias != null && incAliases)
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
      else if (seeLocation == POST_SEE && crossRefs != null && incSee)
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

            listBuilder.append(crossRefs[i]);
         }

         listBuilder.append("}{}");

         locationList.add(listBuilder.toString());
      }
      else if (seealsoLocation == POST_SEE && alsocrossRefs != null && incSeeAlso)
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
      StringBuilder builder = new StringBuilder();

      if (locationList == null)
      {
         locationList = new Vector<String>();
      }

      int minRange = resource.getMinLocationRange();
      String suffixF = resource.getSuffixF();
      String suffixFF = resource.getSuffixFF();
      int gap = resource.getLocationGap();

      if (primaryRecords != null)
      {
         builder.append(String.format("\\bibglsprimary{%d}{",
              primaryRecords.size()));

         builder = updateLocationList(minRange, 
           suffixF, suffixFF, gap, primaryRecords, builder);

         builder.append("}");
      }
      else if (primaryRecordMap != null)
      {
         String sep = "";

         if (primaryCounters == null)
         {
            for (String counter : resource.getLocationCounters())
            {
               Vector<GlsRecord> list = primaryRecordMap.get(counter);

               if (list != null)
               {
                  builder.append(String.format(
                         "%s\\bibglsprimarylocationgroup{%s}{%s}{",
                   sep, list.size(), counter));


                  builder = updateLocationList(minRange, suffixF, suffixFF, gap,
                    list, builder);

                  builder.append("}");

                  sep = "\\bibglsprimarylocationgroupsep ";
               }
            }
         }
         else
         {
            for (String counter : primaryCounters)
            {
               Vector<GlsRecord> list = primaryRecordMap.get(counter);

               builder.append(String.format(
                      "%s\\bibglsprimarylocationgroup{%s}{%s}{",
                sep, list.size(), counter));


               builder = updateLocationList(minRange, suffixF, suffixFF, gap,
                 list, builder);

               builder.append("}");

               sep = "\\bibglsprimarylocationgroupsep ";
            }
         }
      }

      if (builder.length() > 0)
      {
         return builder.toString();
      }

      return null;
   }

   public void initAlias() throws IOException
   {
      TeXParser parser = resource.getBibParser();

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
         if (bib2gls.isVerbose())
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.field.not.set", "alias"));
         }
      }
      else
      {
         // is this a compound entry?
         CompoundEntry comp = bib2gls.getCompoundEntry(alias);

         if (comp == null)
         {
            alias = processLabel(alias);

            if (bib2gls.isVerbose())
            {
               bib2gls.logMessage(bib2gls.getMessage(
                  "message.crossref.found", getId(),
                  "alias", alias));
            }

            addDependency(alias);
         }
         else
         {
            if (bib2gls.isVerbose())
            {
               bib2gls.logMessage(bib2gls.getMessage(
                  "message.compoundcrossref.found", getId(),
                  "alias", alias));
            }

            String[] elements = comp.getElements();

            for (String elem : elements)
            {
               addDependency(elem);
            }
         }

         putField("alias", alias);

         resource.setAliases(true);
      }
   }

   public void initCrossRefs()
    throws IOException
   {
      if (bib2gls.isVerbose())
      {
         bib2gls.logMessage(bib2gls.getMessage(
            "message.checking.crossrefs", getId()));
      }

      initAlias();

      // Is there a 'see' field?
      BibValueList value = getField("see");

      BibValueList seeAlsoValue = getField("seealso");

      if (value == null)
      {// no 'see' field, is there a 'seealso' field?

         if (bib2gls.isVerbose())
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.field.not.set", "see"));
         }

         if (seeAlsoValue != null)
         {
            initAlsoCrossRefs(seeAlsoValue, getFieldValue("seealso"));
            return;
         }

         if (bib2gls.isVerbose())
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

      TeXParser parser = resource.getBibParser();

      TeXObjectList valList = value.expand(parser);

      StringBuilder builder = new StringBuilder();

      initSeeRef(valList, builder);
   }

   private void initSeeRef(TeXObjectList valList,
    StringBuilder builder)
    throws IOException
   {
      TeXParser parser = resource.getBibParser();

      boolean pruneDeadEnds = resource.isPruneSeeDeadEndsOn();

      if (valList instanceof Group)
      {
         valList = ((Group)valList).toList();
      }

      TeXObject opt = valList.popArg(parser, '[', ']');

      if (opt != null)
      {
         crossRefTag = opt.toString(parser);
         orgCrossRefTag = crossRefTag;

         builder.append('[');
         builder.append(crossRefTag);
         builder.append(']');
      }

      CsvList csvList = CsvList.getList(parser, valList);

      int n = csvList.size();

      if (n == 0) return;

      crossRefs = new String[n];
      int xrIdx = 0;

      if (pruneDeadEnds)
      {
         orgCrossRefs = new String[n];
      }

      for (int i = 0; i < n; i++)
      {
         boolean append = true;

         TeXObject xr = csvList.get(i);

         if (xr instanceof TeXObjectList)
         {
            xr = ((TeXObjectList)xr).trim();
         }

         String label = xr.toString(parser);

         // is this a compound entry?
         CompoundEntry comp = bib2gls.getCompoundEntry(label);

         if (comp == null)
         {
            label = processLabel(label);

            if (pruneDeadEnds && resource.isSeeDeadEnd(label))
            {
               append = false;
               bib2gls.verboseMessage("message.crossref.pruned", 
                getId(), "see", label);

               resource.prunedSee(label, this);
            }
            else
            {
               crossRefs[xrIdx++] = label;

               if (bib2gls.isVerbose())
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                     "message.crossref.found", getId(), "see", label));
               }

               addDependency(label);
            }
         }
         else
         {
            crossRefs[xrIdx++] = label;

            if (bib2gls.isVerbose())
            {
               bib2gls.logMessage(bib2gls.getMessage(
                  "message.compoundcrossref.found", getId(), "see", label));
            }

            String[] elements = comp.getElements();

            for (String elem : elements)
            {
               // don't process the label since it's provided in the
               // document not the bib files
               addDependency(elem);
            }
         }

         if (pruneDeadEnds)
         {
            orgCrossRefs[i] = label;
         }

         if (append)
         {
            if (xrIdx > 1)
            {
               builder.append(',');
            }

            builder.append(label);
         }
      }

      if (!pruneDeadEnds)
      {
         orgCrossRefs = crossRefs;
      }

      if (xrIdx == 0)
      {
         removeField("see");
         fieldValues.remove("see");
         crossRefTag = null;
         crossRefs = null;
      }
      else if (xrIdx < n)
      {
         String strList = builder.toString();

         // need to rebuild list

         TeXObjectList list = new TeXObjectList();

         list.addAll(parser.getListener().createString(strList));
         BibValueList bibList = new BibValueList();
         bibList.add(new BibUserString(list));

         putField("see", bibList);
         putField("see", strList);

         String[] array = new String[xrIdx];

         for (int i = 0; i < xrIdx; i++)
         {
            array[i] = crossRefs[i];
         }

         crossRefs = array;
      }
      else
      {
         putField("see", builder.toString());
      }
   }

   private void initAlsoCrossRefs(BibValueList value, 
     String strValue)
    throws IOException
   {
      if (strValue == null || strValue.isEmpty() 
           || !bib2gls.isKnownField("seealso"))
      {
         initAlsoCrossRefs(value);
      }
      else
      {
         TeXParser parser = resource.getBibParser();

         boolean pruneDeadEnds = resource.isPruneSeeAlsoDeadEndsOn();

         StringBuilder builder = new StringBuilder();
         String sep = "";

         alsocrossRefs = strValue.trim().split("\\s*,\\s*");
         orgAlsoCrossRefs = alsocrossRefs;

         Vector<String> xrList = null;

         if (pruneDeadEnds)
         {
            xrList = new Vector<String>();
         }

         for (String label : alsocrossRefs)
         {
            boolean append = true;

            // is this a compound entry?
            CompoundEntry comp = bib2gls.getCompoundEntry(label);

            if (comp == null)
            {
               label = processLabel(label);

               if (pruneDeadEnds && resource.isSeeAlsoDeadEnd(label))
               {
                  append = false;

                  bib2gls.verboseMessage("message.crossref.pruned", 
                   getId(), "seealso", label);

                  resource.prunedSeeAlso(label, this);
               }
               else
               {   
                  if (bib2gls.isVerbose())
                  {
                     bib2gls.logMessage(bib2gls.getMessage(
                        "message.crossref.found", getId(), "seealso", label));
                  }
   
                  addDependency(label);

                  if (xrList != null)
                  {
                     xrList.add(label);
                  }
               }
            }
            else
            {
               if (bib2gls.isVerbose())
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                     "message.compoundcrossref.found", getId(), "seealso", label));
               }

               String[] elements = comp.getElements();

               for (String elem : elements)
               {
                  addDependency(elem);
               }
            }

            if (append)
            {
               builder.append(sep);
               builder.append(label);
               sep = ",";
            }
         }

         if (xrList != null && xrList.size() < alsocrossRefs.length)
         {
            if (xrList.isEmpty())
            {
               removeField("seealso");
               removeFieldValue("seealso");
               alsocrossRefs = null;
            }
            else
            {
               String strList = builder.toString();

               alsocrossRefs = xrList.toArray(new String[xrList.size()]);

               // need to rebuild list

               TeXObjectList list = new TeXObjectList();

               list.addAll(parser.getListener().createString(strList));
               BibValueList bibList = new BibValueList();
               bibList.add(new BibUserString(list));

               putField("seealso", bibList);
               putField("seealso", strList);
            }
         }
         else
         {
            putField("seealso", builder.toString());
         }
      }
   }

   private void initAlsoCrossRefs(BibValueList value)
    throws IOException
   {
      TeXParser parser = resource.getBibParser();

      StringBuilder builder = new StringBuilder();

      TeXObjectList valList = value.expand(parser);

      if (!bib2gls.isKnownField("seealso"))
      {// seealso field not supported, so replicate see=[\seealsoname]

         bib2gls.warningMessage(
           "warning.field.unsupported", "seealso", "1.16");

         crossRefTag = "\\seealsoname ";
         builder.append("[\\seealsoname]");
         initSeeRef(valList, builder);

         return;
      }

      boolean pruneDeadEnds = resource.isPruneSeeAlsoDeadEndsOn();

      if (valList instanceof Group)
      {
         valList = ((Group)valList).toList();
      }

      CsvList csvList = CsvList.getList(parser, valList);

      int n = csvList.size();

      if (n == 0) return;

      alsocrossRefs = new String[n];
      int xrIdx = 0;

      if (pruneDeadEnds)
      {
         orgAlsoCrossRefs = new String[n];
      }

      for (int i = 0; i < n; i++)
      {
         boolean append = true;

         TeXObject xr = csvList.get(i);

         if (xr instanceof TeXObjectList)
         {
            xr = ((TeXObjectList)xr).trim();
         }

         String label = xr.toString(parser);

         // is this a compound entry?
         CompoundEntry comp = bib2gls.getCompoundEntry(label);

         if (pruneDeadEnds)
         {
            orgAlsoCrossRefs[i] = label;
         }

         if (comp == null)
         {
            label = processLabel(alsocrossRefs[i]);

            if (pruneDeadEnds && resource.isSeeAlsoDeadEnd(label))
            {
               append = false;
               bib2gls.verboseMessage("message.crossref.pruned", 
                getId(), "seealso", label);

               resource.prunedSeeAlso(label, this);
            }
            else
            {
               if (bib2gls.isVerbose())
               {
                  bib2gls.logMessage(bib2gls.getMessage(
                     "message.crossref.found", getId(), "seealso", label));
               }

               addDependency(label);
            }
         }
         else
         {
            if (bib2gls.isVerbose())
            {
               bib2gls.logMessage(bib2gls.getMessage(
                  "message.compoundcrossref.found", getId(), "seealso", label));
            }

            String[] elements = comp.getElements();

            for (String elem : elements)
            {
               addDependency(elem);
            }
         }

         if (append)
         {
            alsocrossRefs[xrIdx++] = label;

            if (xrIdx > 1)
            {
               builder.append(',');
            }

            builder.append(label);
         }
      }

      if (!pruneDeadEnds)
      {
         orgAlsoCrossRefs = alsocrossRefs;
      }

      if (xrIdx == 0)
      {
         removeField("seealso");
         removeFieldValue("seealso");
         alsocrossRefs = null;
      }
      else if (xrIdx < n)
      {
         String strList = builder.toString();

         // need to rebuild list

         TeXObjectList list = new TeXObjectList();

         list.addAll(parser.getListener().createString(strList));
         BibValueList bibList = new BibValueList();
         bibList.add(new BibUserString(list));

         putField("seealso", bibList);
         putField("seealso", strList);

         String[] array = new String[xrIdx];

         for (int i = 0; i < xrIdx; i++)
         {
            array[i] = alsocrossRefs[i];
         }

         alsocrossRefs = array;
      }
      else
      {
         putField("seealso", builder.toString());
      }
   }

   public void reprune()
   {
      TeXParser parser = resource.getBibParser();

      boolean modified = false;

      if (crossRefs != null)
      {
         Vector<String> newList = new Vector<String>();

         for (int i = 0; i < crossRefs.length; i++)
         {
            // is this a compound entry?
            CompoundEntry comp = bib2gls.getCompoundEntry(crossRefs[i]);

            if (comp == null && resource.isSeeDeadEnd(crossRefs[i]))
            {
               modified = true;
               removeDependency(crossRefs[i]);

               bib2gls.verboseMessage("message.crossref.pruned", 
                getId(), "see", crossRefs[i]);

               resource.prunedSee(crossRefs[i], this);
            }
            else
            {
               newList.add(crossRefs[i]);
            }
         }

         if (modified)
         {
            if (newList.isEmpty())
            {
               removeField("see");
               removeFieldValue("see");
               crossRefTag = null;
               crossRefs = null;
            }
            else
            {
               crossRefs = newList.toArray(new String[newList.size()]);

               StringBuilder builder = new StringBuilder();

               if (crossRefTag != null)
               {
                  builder.append('[');
                  builder.append(crossRefTag);
                  builder.append(']');
               }

               for (int i = 0; i < crossRefs.length; i++)
               {
                  if (i > 0)
                  {
                     builder.append(',');
                  }
         
                  builder.append(crossRefs[i]);
               }

               String strList = builder.toString();

               TeXObjectList list = new TeXObjectList();

               list.addAll(parser.getListener().createString(strList));
               BibValueList bibList = new BibValueList();
               bibList.add(new BibUserString(list));

               putField("see", bibList);
               putField("see", strList);
            }
         }

         modified = false;
      }

      if (alsocrossRefs != null)
      {
         Vector<String> newList = new Vector<String>();

         for (int i = 0; i < alsocrossRefs.length; i++)
         {
            // is this a compound entry?
            CompoundEntry comp = bib2gls.getCompoundEntry(alsocrossRefs[i]);

            if (comp == null && resource.isSeeAlsoDeadEnd(alsocrossRefs[i]))
            {
               modified = true;
               removeDependency(crossRefs[i]);

               bib2gls.verboseMessage("message.crossref.pruned", 
                getId(), "seealso", alsocrossRefs[i]);

               resource.prunedSeeAlso(alsocrossRefs[i], this);
            }
            else
            {
               newList.add(alsocrossRefs[i]);
            }
         }

         if (modified)
         {
            if (newList.isEmpty())
            {
               removeField("seealso");
               removeFieldValue("seealso");
               alsocrossRefs = null;
            }
            else
            {
               alsocrossRefs = newList.toArray(new String[newList.size()]);

               StringBuilder builder = new StringBuilder();

               for (int i = 0; i < alsocrossRefs.length; i++)
               {
                  if (i > 0)
                  {
                     builder.append(',');
                  }
         
                  builder.append(alsocrossRefs[i]);
               }

               String strList = builder.toString();

               TeXObjectList list = new TeXObjectList();

               list.addAll(parser.getListener().createString(strList));
               BibValueList bibList = new BibValueList();
               bibList.add(new BibUserString(list));

               putField("seealso", bibList);
               putField("seealso", strList);
            }
         }
      }
   }

   public void restorePrunedSee(String label)
   {
      if (crossRefs == null)
      {
         crossRefTag = orgCrossRefTag;
         crossRefs = new String[1];
         crossRefs[0] = label;
      }
      else
      {
         // restore the label according to its original relative position

         String[] newArray = new String[crossRefs.length+1];
         int i = 0;

         for (int j = 0; j < orgCrossRefs.length; j++)
         {
            if (orgCrossRefs[j].equals(label))
            {
               newArray[i] = label;
               break;
            }
            else if (isSeeLabel(orgCrossRefs[j]))
            {
               newArray[i++] = orgCrossRefs[j];
            }
         }

         for (; i < crossRefs.length; i++)
         {
            newArray[i+1] = crossRefs[i];
         }

         crossRefs = newArray;
      }

      StringBuilder builder = new StringBuilder();

      if (crossRefTag != null)
      {
         builder.append('[');
         builder.append(crossRefTag);
         builder.append(']');
      }

      for (int i = 0; i < crossRefs.length; i++)
      {
         if (i > 0)
         {
            builder.append(',');
         }

         builder.append(crossRefs[i]);
      }

      TeXParser parser = resource.getBibParser();

      String strList = builder.toString();

      TeXObjectList list = new TeXObjectList();

      list.addAll(parser.getListener().createString(strList));
      BibValueList bibList = new BibValueList();
      bibList.add(new BibUserString(list));

      putField("see", bibList);
      putField("see", strList);
   }

   public void restorePrunedSeeAlso(String label)
   {
      if (alsocrossRefs == null)
      {
         alsocrossRefs = new String[1];
         alsocrossRefs[0] = label;
      }
      else
      {
         // restore the label according to its original relative position

         String[] newArray = new String[alsocrossRefs.length+1];
         int i = 0;

         for (int j = 0; j < orgAlsoCrossRefs.length; j++)
         {
            if (orgAlsoCrossRefs[j].equals(label))
            {
               newArray[i] = label;
               break;
            }
            else if (isSeeAlsoLabel(orgAlsoCrossRefs[j]))
            {
               newArray[i++] = orgAlsoCrossRefs[j];
            }
         }

         for (; i < alsocrossRefs.length; i++)
         {
            newArray[i+1] = alsocrossRefs[i];
         }

         alsocrossRefs = newArray;
      }

      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < alsocrossRefs.length; i++)
      {
         if (i > 0)
         {
            builder.append(',');
         }

         builder.append(alsocrossRefs[i]);
      }

      TeXParser parser = resource.getBibParser();

      String strList = builder.toString();

      TeXObjectList list = new TeXObjectList();

      list.addAll(parser.getListener().createString(strList));
      BibValueList bibList = new BibValueList();
      bibList.add(new BibUserString(list));

      putField("seealso", bibList);
      putField("seealso", strList);
   }

   // User has identified that the given value is a list like "see"
   // so add dependencies but don't identify them as
   // cross-references.
   private void parseCustomDependencyList( 
    TeXObjectList valList, String field)
    throws IOException
   {
      TeXParser parser = resource.getBibParser();

      if (valList instanceof Group)
      {
         valList = ((Group)valList).toList();
      }

      StringBuilder builder = new StringBuilder();

      TeXObject opt = valList.popArg(parser, '[', ']');

      if (opt != null)
      {
         String depTag = opt.toString(parser);

         builder.append('[');
         builder.append(depTag);
         builder.append(']');
      }

      CsvList csvList = CsvList.getList(parser, valList);

      int n = csvList.size();

      if (n == 0) return;

      for (int i = 0; i < n; i++)
      {
         TeXObject xr = csvList.getValue(i);

         if (xr instanceof TeXObjectList)
         {
            xr = ((TeXObjectList)xr).trim();
         }

         String dep = xr.toString(parser);

         String label = processLabel(dep);

         if (bib2gls.isVerbose())
         {
            bib2gls.logMessage(bib2gls.getMessage(
               "message.custom.dep.found", field, getId(), field, label));
         }

         addDependency(label);
         builder.append(label);

         if (i != n-1)
         {
            builder.append(',');
         }
      }

      if (opt != null)
      {
         valList.push(parser.getListener().getOther(']'));
         valList.push(opt);
         valList.push(parser.getListener().getOther('['));
      }

      putField(field, builder.toString());
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

   public Bib2GlsEntry createParent()
   {
      TeXParser parser = resource.getBibParser();

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
      parentEntry.baseFile = baseFile;

      parentEntry.putField("name", orgParentValue);

      try
      {
         parentEntry.parseFields();
      }
      catch (TeXSyntaxException e)
      {
         bib2gls.error(bib2gls.getMessage( 
           "error.create.missing.parent.failed", orgLabel, getId(), 
             e.getMessage(bib2gls)));
         bib2gls.debug(e);
      }
      catch (Exception e)
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

   public Bib2GlsEntry getHierarchyRoot(Vector<Bib2GlsEntry> entries)
   {
      String parentId = getParent();

      if (parentId == null)
      {
         return this;
      }
      else
      {
         Bib2GlsEntry parent = getEntry(parentId, entries);

         if (parent == null)
         {
            return this;
         }
         else
         {
            return parent.getHierarchyRoot(entries);
         }
      }
   }

   public Bib2GlsEntry getHierarchyRoot()
   {
      String parentId = getParent();

      if (parentId == null)
      {
         return this;
      }
      else
      {
         Bib2GlsEntry parent = resource.getEntry(parentId);

         if (parent == null)
         {
            return this;
         }
         else
         {
            return parent.getHierarchyRoot();
         }
      }
   }

   public int getLevel(Vector<Bib2GlsEntry> entries)
   {
      if (sortLevel != -1 || entries == null)
      {
         return sortLevel;
      }

      sortLevel = 0;

      String parentId = getParent();

      if (parentId != null)
      {
         Bib2GlsEntry parent = getEntry(parentId, entries);

         if (parent != null)
         {
            sortLevel = parent.getLevel(entries)+1;
         }
      }

      return sortLevel;
   }

   public void moveUpHierarchy(Vector<Bib2GlsEntry> entries)
   {
      String parentId = getParent();
      String childId = getId();

      if (parentId == null)
      {
         return;
      }

      sortLevel = -1;

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
      entry.sortLevel = -1;

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
         bib2gls.debugMessage("message.adding.child", getId(), child.getId());
         children.add(child);
      }
   }

   public int getChildCount()
   {
      if (children == null)
      {
         resource.updateChildLists();
      }

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

   public void clearChildren()
   {
      children = null;
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

   @Override
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

   // Gets a minimal copy of this
   public Bib2GlsEntry getMinimalCopy()
   {
      Bib2GlsEntry entry = new Bib2GlsEntry(bib2gls, defIndex);

      entry.originalEntryType = originalEntryType;
      entry.records = records;
      entry.recordMap = recordMap;
      entry.base = base;
      entry.baseFile = baseFile;
      entry.labelPrefix = labelPrefix;
      entry.labelSuffix = labelSuffix;
      entry.setOriginalId(getOriginalId());
      entry.setRecordIndex(recordIndex);

      return entry;
   }

   private void setRecordIndex(long idx)
   {
      recordIndex = idx;

      if (recordIndex != -1)
      {
         String recordIndexField = resource.getUseIndexField();

         if (recordIndexField != null)
         {
            BibValueList val = new BibValueList();
            val.add(new BibNumber(new UserNumber((int)recordIndex)));
            putField(recordIndexField, val);
            putField(recordIndexField, ""+recordIndex);
         }
      }
   }

   public long getDefinitionIndex()
   {
      return defIndex;
   }

   public long getRecordIndex()
   {
      return recordIndex;
   }

   private Vector<GlsRecord> records;
   private HashMap<String,Vector<GlsRecord>> recordMap;
   private Vector<GlsRecord> ignoredRecords;

   private Vector<GlsRecord> supplementalRecords;
   private HashMap<TeXPath,Vector<GlsRecord>> supplementalRecordMap;

   private Vector<GlsRecord> primaryRecords = null;
   private HashMap<String,Vector<GlsRecord>> primaryRecordMap = null;
   private Vector<String> primaryCounters = null;

   private boolean selected = false;

   private boolean nonumberlist = false;

   private String base="";
   private File baseFile = null;

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

   private String orgCrossRefTag = null;
   private String[] orgCrossRefs = null;
   private String[] orgAlsoCrossRefs = null;

   public static final int NO_SEE=0, PRE_SEE=1, POST_SEE=2;

   protected Bib2Gls bib2gls;

   protected GlsResource resource;

   private CollationKey collationKey;

   private String groupId=null;

   private String labelPrefix = null, labelSuffix;

   private Bib2GlsEntry dual = null;

   private Number numericSort = null;

   private Object sortObject = null;

   private int sortLevel = -1;

   private boolean fieldsParsed = false;

   private boolean triggerRecordFound=false;

   private Vector<String> locationList = null;

   private GlsRecord indexCounterRecord = null;

   private long defIndex=0, recordIndex=-1;

   private String crossRefTail = null;

   private static long defIndexCount=0;

   private static final Pattern EXT_PREFIX_PATTERN = Pattern.compile(
     "ext(\\d+)\\.(.*)");

   private static final Pattern RANGE_PATTERN = Pattern.compile(
     "(\\(|\\))(.*)");
}
