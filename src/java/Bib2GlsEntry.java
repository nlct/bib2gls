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
      records = new Vector<GlsRecord>();
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

         if (object instanceof TeXCsRef)
         {
            String csname = ((TeXCsRef)object).getName().toLowerCase();

            boolean found = false;

            try
            {
               if (csname.equals("glssee"))
               {// \glssee[tag]{label}{xr-label-list}

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

               bib2gls.warning(parser, 
                 bib2gls.getMessage("warning.uc.protecting",
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
               bib2gls.warning(parser, 
                 bib2gls.getMessage("warning.uc.protecting", 
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

      for (String field : fields)
      {
         if (resource.skipField(field))
         {
            continue;
         }

         BibValueList value = getField(field);

         if (value != null)
         {
            TeXObjectList list = value.expand(parser);

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

            fieldValues.put(field, list.toString(parser));
         }
      }

      if (fieldValues.get("sort") == null)
      {
         String sort = getDefaultSort();

         if (sort == null)
         {
            bib2gls.warning(parser, 
              bib2gls.getMessage("warning.no.default.sort", getId()));
            fieldValues.put("sort", getId());
         }
         else
         {
            fieldValues.put("sort", sort);
         }
      }

      checkRequiredFields(parser);
   }

   public String getDefaultSort()
   {
      String val = fieldValues.get("name");

      return val == null ? fieldValues.get("parent") : val;
   }

   public String getFallbackValue(String field)
   {
      if (field.equals("text"))
      {
         return fieldValues.get("name");
      }
      else if (field.equals("name"))
      {
         return fieldValues.get("parent");
      }
      else if (field.equals("sort"))
      {
         return fieldValues.get("name");
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
         return getField("parent");
      }
      else if (field.equals("sort"))
      {
         return getField("name");
      }
      else if (field.equals("first"))
      {
         return getFallbackContents("text");
      }
      else if (field.equals("plural"))
      {
         return getFallbackContents("text");
      }
      else if (field.equals("firstplural"))
      {
         return getFallbackContents("first");
      }

      return null;
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

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\bibglsnew%s{%s}%%%n{", getEntryType(), getId());

      String description = "";
      String name = "";
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
      if (value == null)
      {
         throw new NullPointerException();
      }

      return fieldValues.put(label, value);
   }

   public String getParent()
   {
      return fieldValues.get("parent");
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

   public boolean equals(Object other)
   {
      if (other == null || !(other instanceof Bib2GlsEntry)) return false;

      return getId().equals(((Bib2GlsEntry)other).getId());
   }

   public boolean hasRecords()
   {
      return records.size() > 0;
   }

   public void addRecord(GlsRecord record)
   {
      records.add(record);
   }

   public void updateLocationList(int minRange, String suffixF,
     String suffixFF, int seeLocation, boolean showLocationPrefix)
   {
      StringBuilder builder = null;

      locationList = new Vector<String>();

      GlsRecord prev = null;
      int count = 0;
      StringBuilder mid = new StringBuilder();

      boolean start=true;

      if (seeLocation == PRE_SEE && crossRefs != null)
      {
         builder = new StringBuilder();
         builder.append("\\glsxtrusesee{");
         builder.append(getId());
         builder.append("}");

         if (records.size() > 0)
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

      if (showLocationPrefix)
      {
         if (builder == null)
         {
            builder = new StringBuilder();
         }

         builder.append(String.format("\\bibglsprefix{%d}",
           records.size()));
      }

      for (GlsRecord record : records)
      {
         locationList.add(record.getListTeXCode());

         if (prev == null)
         {
            prev = record;
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
                  && record.follows(prev))
         {
            count++;
            prev = record;

            mid.append("\\delimN ");
            mid.append(record.getFmtTeXCode());
         }
         else if (count==2 && suffixF != null)
         {
            builder.append(suffixF);
            mid.setLength(0);
            count = 0;
            prev = null;
         }
         else if (count > 2 && suffixFF != null)
         {
            builder.append(suffixFF);
            mid.setLength(0);
            count = 0;
            prev = null;
         }
         else if (count >= minRange)
         {
            builder.append("\\delimR ");
            builder.append(record.getFmtTeXCode());
            mid.setLength(0);
            count = 0;
            prev = null;
         }
         else
         {
            builder.append(mid);
            builder.append("\\delimN ");
            builder.append(record.getFmtTeXCode());
            mid.setLength(0);
            count = 0;
            prev = null;
         }

         start = false;
      }

      if (prev != null && mid.length() > 0)
      {
         if (count >= minRange)
         {
            builder.append("\\delimR ");
            builder.append(prev.getFmtTeXCode());
         }
         else
         {
            builder.append(mid);
         }
      }

      if (seeLocation == POST_SEE && crossRefs != null)
      {
         if (builder == null)
         {
            builder = new StringBuilder();
         }

         if (records.size() > 0)
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

      if (builder != null)
      {
         fieldValues.put("location", builder.toString());
      }
   }

   public void initCrossRefs(TeXParser parser)
    throws IOException
   {
      if (resource.skipField("see"))
      {
         return;
      }

      BibValueList value = getField("see");

      if (value == null) return;

      TeXObjectList valList = value.expand(parser);

      StringBuilder builder = new StringBuilder();

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
         crossRefs[i] = csvList.get(i).toString(parser);

         String label = processLabel(crossRefs[i]);

         addDependency(label);
         builder.append(label);

         if (i != n-1)
         {
            builder.append(',');
         }
      }

      fieldValues.put("see", builder.toString());
   }

   public void setCollationKey(CollationKey key)
   {
      collationKey = key;
   }

   public CollationKey getCollationKey()
   {
      return collationKey;
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

      for (Bib2GlsEntry e : entries)
      {
         if (e.getId().equals(parentId))
         {
            addHierarchy(e, entries);
            return;
         }
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

   private Vector<GlsRecord> records;

   private HashMap<String,String> fieldValues;

   private Vector<String> deps;

   private Vector<Bib2GlsEntry> hierarchy;

   private String crossRefTag = null;
   private String[] crossRefs = null;

   public static final int NO_SEE=0, PRE_SEE=1, POST_SEE=2;

   protected Bib2Gls bib2gls;

   protected GlsResource resource;

   private CollationKey collationKey;

   private String labelPrefix = null;

   private Bib2GlsEntry dual = null;

   private Vector<String> locationList = null;

   private static final Pattern EXT_PREFIX_PATTERN = Pattern.compile(
     "ext(\\d+)\\.(.*)");
}
