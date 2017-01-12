package com.dickimawbooks.bib2gls;

public class Bib2GlsException extends Exception
{
   public Bib2GlsException(String message)
   {
      super(message);
   }

   public Bib2GlsException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
