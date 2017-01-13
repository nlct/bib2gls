package com.dickimawbooks.bib2gls;

import java.io.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.bib.*;
import com.dickimawbooks.texparserlib.latex.CsvList;

public class Bib2GlsEntry extends BibEntry
{
   public Bib2GlsEntry()
   {
      this("entry");
   }

   public Bib2GlsEntry(String entryType)
   {
      super(entryType.toLowerCase());
      fieldValues = new HashMap<String,String>();
      deps = new Vector<String>();
      records = new Vector<GlsRecord>();
   }

   // does the control sequence given by csname have [options]{label}
   // syntax (with a * or + prefix)?
   private boolean isGlsCsOptLabel(Bib2Gls bib2gls, String csname)
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

   private void checkGlsCs(TeXParser parser, TeXObjectList list)
    throws IOException
   {
      Bib2Gls bib2gls = (Bib2Gls)parser.getListener().getTeXApp();

      for (int i = 0; i < list.size(); i++)
      {
         TeXObject object = list.get(i);

         if (object instanceof TeXCsRef)
         {
            String csname = ((TeXCsRef)object).getName().toLowerCase();

            boolean foundgls = false;

            try
            {
               if (csname.equals("glssee"))
               {// \glssee[tag]{label}{xr-label-list}

                  foundgls = (i==0);

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

                  addDependency(arg.toString(parser));

                  // get next argument

                  arg = list.get(++i);

                  while (arg instanceof Ignoreable)
                  {
                     arg = list.get(++i);
                  }

                  CsvList csvlist = CsvList.getList(parser, arg);

                  for (TeXObject obj : csvlist)
                  {
                     addDependency(obj.toString(parser));
                  }
               }
               else if (csname.equals("glsxtrp"))
               {// \glsxtrp{field}{label}

                  foundgls = (i==0);

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

                  addDependency(arg.toString(parser));
               }
               else if (isGlsCsOptLabel(bib2gls, csname))
               {
                  foundgls = (i==0);

                  TeXObject arg = list.get(++i);

                  while (arg instanceof Ignoreable)
                  {
                     arg = list.get(++i);
                  }

                  if (arg instanceof CharObject)
                  {
                     int code = ((CharObject)arg).getCharCode();

                     if (code == '*' || code == '+')
                     {
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

                  addDependency(arg.toString(parser));
               }
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
               bib2gls.warning(parser, 
                 String.format("Can't detect argument for \\%s", csname));
            }

            if (foundgls)
            {
               // found a problematic command at the start of a
               // field. Protect the field from first letter
               // upper casing by inserting an empty group.

               list.add(0, parser.getListener().createGroup());
               i++;
            }
         }
         else if (object instanceof TeXObjectList)
         {
            checkGlsCs(parser, (TeXObjectList)object);
         }
      }
   }

   public void parseContents(TeXParser parser,
    TeXObjectList contents, TeXObject endGroupChar)
     throws IOException
   {
      super.parseContents(parser, contents, endGroupChar);

      Bib2Gls bib2gls = (Bib2Gls)parser.getListener().getTeXApp();

      Vector<String> fields = bib2gls.getFields();

      for (String field : fields)
      {
         BibValueList value = getField(field);

         if (value != null)
         {
            TeXObjectList list = value.expand(parser);

            checkGlsCs(parser, list);

            fieldValues.put(field, list.toString(parser));
         }
      }

      if (fieldValues.get("sort") == null)
      {
         String sort = getDefaultSort();

         if (sort == null)
         {
            bib2gls.warning(parser, 
              String.format("can't determine sort value: %s", getId()));
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
      return fieldValues.get("name");
   }

   public String getFallbackField(String field)
   {
      if (field.equals("text"))
      {
         return fieldValues.get("name");
      }
      else if (field.equals("sort"))
      {
         return fieldValues.get("name");
      }
      else if (field.equals("first"))
      {
         return getFallbackField("text");
      }
      else if (field.equals("plural"))
      {
         return getFallbackField("text")+"s";
      }
      else if (field.equals("firstplural"))
      {
         return getFallbackField("first")+"s";
      }

      return null;
   }

   public void checkRequiredFields(TeXParser parser)
   {
      if (getField("name") == null)
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
       String.format("entry %s missing required field: %s",
        getId(), field));
   }

   public void writeBibEntry(PrintWriter writer)
   throws IOException
   {
      writer.format("\\bibglsnewentry{%s}%%%n{", getId());

      String description = "";
      String name = "";
      String sep = "";

      Set<String> keyset = getFieldSet();

      Iterator<String> it = keyset.iterator();

      while (it.hasNext())
      {
         String field = it.next();

         if (field.equals("description"))
         {
            description = fieldValues.get(field);
         }
         else if (field.equals("name"))
         {
            name = fieldValues.get(field);
         }
         else 
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, fieldValues.get(field));
         }
      }

      writer.println("}%");
      writer.println(String.format("{%s}%%", name));
      writer.println(String.format("{%s}", description));
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

   public static boolean inList(String label, Vector<Bib2GlsEntry> list)
   {
      for (Bib2GlsEntry entry : list)
      {
         if (entry.getId().equals(label)) return true;
      }

      return false;
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
      StringBuilder listBuilder = null;

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

         listBuilder = new StringBuilder();
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
         if (listBuilder == null)
         {
            listBuilder = new StringBuilder(record.getListTeXCode());
         }
         else
         {
            listBuilder.append("|");
            listBuilder.append(record.getListTeXCode());
         }

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
         else if (record.follows(prev))
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

         if (listBuilder == null)
         {
            listBuilder = new StringBuilder();
         }
         else
         {
            listBuilder.append('|');
         }

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
      }

      if (builder != null)
      {
         fieldValues.put("location", builder.toString());
      }

      if (listBuilder != null)
      {
         fieldValues.put("loclist", listBuilder.toString());
      }
   }

   public void initCrossRefs(TeXParser parser)
    throws IOException
   {
      BibValueList value = getField("see");

      if (value == null) return;

      TeXObjectList valList = value.expand(parser);

      if (valList instanceof Group)
      {
         valList = ((Group)valList).toList();
      }

      TeXObject opt = valList.popArg(parser, '[', ']');

      if (opt != null)
      {
         crossRefTag = opt.toString(parser);
      }

      CsvList csvList = CsvList.getList(parser, valList);

      int n = csvList.size();

      if (n == 0) return;

      crossRefs = new String[n];

      for (int i = 0; i < n; i++)
      {
         crossRefs[i] = csvList.get(i).toString(parser);

         addDependency(crossRefs[i]);
      }
   }

   private Vector<GlsRecord> records;

   private HashMap<String,String> fieldValues;

   private Vector<String> deps;

   private String crossRefTag = null;
   private String[] crossRefs = null;

   public static final int NO_SEE=0, PRE_SEE=1, POST_SEE=2;
}
