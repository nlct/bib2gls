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
import java.text.CollationKey;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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

   public String getId()
   {
      return labelPrefix == null ? super.getId() : labelPrefix+super.getId();
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

   public String processLabel(String label)
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
            String csname = ((TeXCsRef)object).getName().toLowerCase();

            boolean found = false;

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

                  String newLabel = processLabel(label);

                  if (!newLabel.equals(label))
                  {
                     label = newLabel;

                     list.set(i, parser.getListener().createGroup(label));
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

                     label = processLabel(obj.toString(parser));

                     grp.add(parser.getListener().createString(label));

                     if (j < m)
                     {
                        grp.add(parser.getListener().getOther(','));
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
                  String newLabel = processLabel(label);

                  if (!label.equals(newLabel))
                  {
                     label = newLabel;
                     list.set(i, parser.getListener().createGroup(label));
                  }

                  addDependency(label);
               }
               else if (isGlsCsOptLabel(csname))
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

                  String newLabel = processLabel(label);

                  if (!label.equals(newLabel))
                  {
                     label = newLabel;

                     for ( ; i > start; i--)
                     {
                        list.remove(i);
                     }

                     list.set(i, parser.getListener().createGroup(label));
                  }

                  addDependency(label);

                  if (bib2gls.checkNestedLinkTextField(fieldName)
                   && !csname.equals("glsps") && !csname.equals("glspt"))
                  {
                     bib2gls.warning(parser, 
                       bib2gls.getMessage("warning.potential.nested.link",
                       getId(), fieldName,
                       String.format("\\%s%s%s", csname, pre, opt),
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

   public void parseContents(TeXParser parser,
    TeXObjectList contents, TeXObject endGroupChar)
     throws IOException
   {
      super.parseContents(parser, contents, endGroupChar);

      Vector<String> fields = bib2gls.getFields();

      boolean mfirstucProtect = bib2gls.mfirstucProtection();
      String[] protectFields = bib2gls.mfirstucProtectionFields();

      if (resource.changeShortCase())
      {
         BibValueList value = getField("short");

         if (value != null)
         {
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

      if (resource.hasSkippedFields())
      {
         String[] skip = resource.getSkipFields();

         for (String field : skip)
         {
            removeField(field);
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

         if (value != null)
         {
            TeXObjectList list = BibValueList.stripDelim(value.expand(parser));

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

            putField(field, list.toString(parser));
         }
      }

      checkRequiredFields(parser);

      // the name can't have its case changed until it's been
      // checked and been assigned a fallback if not present.

      if (resource.changeNameCase())
      {
         BibValueList value = getField("name");

         if (value != null)
         {
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
      }

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

   public void checkRequiredFields(TeXParser parser)
   {
      if (getField("name") == null && getField("parent") == null)
      {
         missingFieldWarning(parser, "name");
      }

      if (getField("description") == null)
      {
         missingFieldWarning(parser, "description");
      }
   }

   protected void missingFieldWarning(TeXParser parser, String field)
   {
      parser.getListener().getTeXApp().warning(parser, 
       bib2gls.getMessage("warning.missing.field", getId(), field));
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
         else
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, value);
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

   public boolean hasCrossRefs()
   {
      return (crossRefs != null && crossRefs.length > 0)
         || (alsocrossRefs != null && alsocrossRefs.length > 0);
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

   public void addRecord(GlsRecord record)
   {
      if (record.getFormat().equals("glsignore"))
      {
         bib2gls.debug(bib2gls.getMessage("message.ignored.record", record));
         addIgnoredRecord(record);
      }
      else if (records != null)
      {
         if (!records.contains(record))
         {
            records.add(record);
         }
      }
      else
      {
         Vector<GlsRecord> list = recordMap.get(record.getCounter());

         if (list != null && !list.contains(record))
         {
            list.add(record);
         }
      }
   }

   public void addSupplementalRecord(GlsRecord record)
   {
      if (supplementalRecords == null)
      {
         supplementalRecords = new Vector<GlsRecord>();
      }

      record.setLabel(getId());
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

      if (!supplementalRecords.contains(record))
      {
         supplementalRecords.add(record);
      }
   }

   public void addIgnoredRecord(GlsRecord record)
   {
      if (ignoredRecords == null)
      {
         ignoredRecords = new Vector<GlsRecord>();
      }

      record.setLabel(getId());

      if (!ignoredRecords.contains(record))
      {
         ignoredRecords.add(record);
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

      for (GlsRecord record : recordList)
      {
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
                  builder.append("\\delimN ");
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
                bib2gls.warning(bib2gls.getMessage(
                    "error.inconsistent.range", record, rangeStart));

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
                bib2gls.debug(bib2gls.getMessage("message.merge.range",
                  record, rangeStart));
             }
             else
             {
                if (record.getFormat().equals("glsnumberformat")
                 || rangeFmt.isEmpty())
                {
                   bib2gls.verbose(bib2gls.getMessage(
                      "message.inconsistent.range", record, rangeStart));
                }
                else
                {
                   bib2gls.warning(bib2gls.getMessage(
                      "error.inconsistent.range", record, rangeStart));
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
               builder.append("\\delimN ");
            }

            builder.append(record.getFmtTeXCode());
         }
         else if (minRange < Integer.MAX_VALUE
                  && record.follows(prev, gap, maxGap))
         {
            count++;

            mid.append("\\delimN ");
            mid.append(record.getFmtTeXCode());
         }
         else if (count==2 && suffixF != null)
         {
            builder.append(suffixF);
            builder.append("\\delimN ");
            builder.append(record.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
            maxGap[0] = 0;
         }
         else if (count > 2 && suffixFF != null)
         {
            builder.append(suffixFF);
            builder.append("\\delimN ");
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

            builder.append("\\delimN ");
            builder.append(record.getFmtTeXCode());
            mid.setLength(0);
            count = 1;
         }
         else
         {
            builder.append(mid);
            builder.append("\\delimN ");
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

   public void updateLocationList(int minRange, String suffixF,
     String suffixFF, int seeLocation, int seealsoLocation,
     boolean showLocationPrefix, 
     boolean showLocationSuffix, int gap)
   throws Bib2GlsException
   {
      StringBuilder builder = null;

      locationList = new Vector<String>();

      int numRecords = recordCount();

      if (seeLocation == PRE_SEE && crossRefs != null)
      {
         builder = new StringBuilder();
         builder.append("\\glsxtrusesee{");
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
         builder.append("\\glsxtruseseealso{");
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

      if (getField("alias") != null 
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

            builder = updateLocationList(minRange, suffixF, suffixFF, gap,
              supplementalRecords, builder);

            builder.append("}");
         }
      }

      if (seeLocation == POST_SEE && crossRefs != null)
      {
         if (builder == null)
         {
            builder = new StringBuilder();
         }

         if (hasLocationList)
         {
            builder.append("\\bibglsseesep ");
         }

         builder.append("\\glsxtrusesee{");
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

         builder.append("\\glsxtruseseealso{");
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

   public void initAlias(TeXParser parser) throws IOException
   {
      // Is there an 'alias' field?
      BibValueList value = getField("alias");
      String alias = null;

      if (value != null)
      {
         alias = value.expand(parser).toString(parser);
         addDependency(processLabel(alias));
         putField("alias", alias);

         resource.setAliases(true);

         // Is there a 'see' field?

         if (getField("see") == null)
         {
            putField("see", value);
         }
      }
   }

   public void initCrossRefs(TeXParser parser)
    throws IOException
   {
      // Is there a 'see' field?
      BibValueList value = getField("see");
      TeXObjectList valList = null;

      BibValueList seeAlsoValue = getField("seealso");

      if (value == null)
      {// no 'see' field, is there a 'seealso' field?

         if (seeAlsoValue != null)
         {
            initAlsoCrossRefs(parser, seeAlsoValue);
            return;
         }

         // no 'seealso' field, so check for \glssee records
         // (see field overrides any instances of \glssee)

         GlsSeeRecord record = bib2gls.getSeeRecord(getId());

         if (record == null)
         {
            return;
         }

         TeXObject valObj = record.getValue();

         if (valObj instanceof TeXObjectList)
         {
            valList = (TeXObjectList)valObj;
         }
         else
         {
            valList = new TeXObjectList();
            valList.add(valObj);
         }
      }

      if (seeAlsoValue != null)
      {
         bib2gls.warning(bib2gls.getMessage("warning.field.clash",
           "see", "seealso"));
      }

      if (valList == null)
      {
         valList = value.expand(parser);
      }

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

         addDependency(label);
         builder.append(label);

         if (i != n-1)
         {
            builder.append(',');
         }
      }

      putField("see", builder.toString());
   }

   private void initAlsoCrossRefs(TeXParser parser, BibValueList value)
    throws IOException
   {
      StringBuilder builder = new StringBuilder();

      TeXObjectList valList = value.expand(parser);

      if (!bib2gls.isKnownField("seealso"))
      {// seealso field not supported, so replicate see=[\seealsoname]

         bib2gls.warning(bib2gls.getMessage(
           "warning.field.unsupported", "seealso", "1.16"));

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

      if (parent != null)
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

   private Vector<GlsRecord> records;
   private HashMap<String,Vector<GlsRecord>> recordMap;
   private Vector<GlsRecord> supplementalRecords;
   private Vector<GlsRecord> ignoredRecords;

   private Vector<Bib2GlsEntry> children;

   private HashMap<String,String> fieldValues;

   private Vector<String> deps;

   private Vector<Bib2GlsEntry> hierarchy;

   private Vector<Bib2GlsEntry> crossRefdBy;

   private String crossRefTag = null;
   private String[] crossRefs = null;
   private String[] alsocrossRefs = null;

   public static final int NO_SEE=0, PRE_SEE=1, POST_SEE=2;

   protected Bib2Gls bib2gls;

   protected GlsResource resource;

   private CollationKey collationKey;

   private String groupId=null;

   private String labelPrefix = null;

   private Bib2GlsEntry dual = null;

   private Number numericSort = null;

   private Vector<String> locationList = null;

   private static final Pattern EXT_PREFIX_PATTERN = Pattern.compile(
     "ext(\\d+)\\.(.*)");

   private static final Pattern RANGE_PATTERN = Pattern.compile(
     "(\\(|\\))(.*)");
}
