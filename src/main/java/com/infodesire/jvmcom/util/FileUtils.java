package com.infodesire.jvmcom.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class FileUtils {

  /**
   * Read text file into string
   *
   * @param file Text file
   * @return String with content of text file
   * @throws IOException error reading or finding file
   *
   */
  public static String readFile( File file ) throws IOException {

    BufferedReader in = new BufferedReader( new FileReader( file ) );
    StringWriter stringWriter = new StringWriter();
    PrintWriter out = new PrintWriter( stringWriter );
    String line;
    while( ( line = in.readLine() ) != null ) {
      if( in.ready() ) {
        out.println( line );
      }
      else {
        out.print( line );
      }
    }
    in.close();
    out.close();
    return stringWriter.toString();

  }

}
