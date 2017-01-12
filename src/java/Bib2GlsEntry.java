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

      for (int i = 0, n = list.size(); i < n; i++)
      {
         TeXObject object = list.get(i);

         if (object instanceof TeXCsRef)
         {
            String csname = ((TeXCsRef)object).getName().toLowerCase();

            try
            {
               if (csname.equals("glssee"))
               {// \glssee[tag]{label}{xr-label-list}

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
                        for (; i < n; i++)
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
                        for (; i < n; i++)
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
      writer.format("\\longnewglossaryentry{%s}{", getId());

      String description = "";
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
         else
         {
            writer.format("%s", sep);

            sep = String.format(",%n");

            writer.format("%s={%s}", field, fieldValues.get(field));
         }
      }

      writer.println(String.format("}{%s}", description));
   }

   public Set<String> getFieldSet()
   {
      return fieldValues.keySet();
   }

   public String getFieldValue(String field)
   {
      return fieldValues.get(field);
   }

   public void addDependency(String label)
   {
      if (!deps.contains(label))
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

   public void addRecord(GlsRecord record)
   {
      records.add(record);
   }

   public void updateLocationList(int minRange, String suffixF,
     String suffixFF)
   {
      StringBuilder builder = null;
      StringBuilder listBuilder = null;

      GlsRecord prev = null;
      int count = 0;
      StringBuilder mid = new StringBuilder();

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
            else
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
      }

      if (prev != null && mid.length() > 0)
      {
System.out.println("count: "+count);
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

      fieldValues.put("location", builder.toString());

      fieldValues.put("loclist", listBuilder.toString());
   }

   private Vector<GlsRecord> records;

   private HashMap<String,String> fieldValues;

   private Vector<String> deps;
}
