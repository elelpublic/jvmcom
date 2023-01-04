package com.infodesire.jvmcom.util;

import java.io.*;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {

    /**
     * Read text file into string
     *
     * @param file Text file
     * @return String with content of text file
     * @throws IOException error reading or finding file
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

    public static String checksum( File file ) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance( "SHA-1" );
        try(
                InputStream in = Files.newInputStream( file.toPath() );
                DigestInputStream dis = new DigestInputStream( in, sha )
        ) {
            byte[] bytes = new byte[ 16 * 1024 ];
            int count;
            while( ( count = in.read( bytes ) ) > 0 ) {
            }
        }
        byte[] digest = sha.digest();
        StringBuilder result = new StringBuilder();
        for( byte b : digest ) {
            result.append( String.format( "%02x", b ) );
        }
        return result.toString();
    }


    /**
     * Pipe all data from an input source to an output stream
     *
     * @param in Input stream
     * @param out Output stream
     * @throws IOException on underlying IO error
     *
     */
    public static void pipe( InputStream in, OutputStream out ) throws IOException {

        byte[] bytes = new byte[ 16 * 1024 ];
        int count;
        while( ( count = in.read( bytes ) ) > 0 ) {
            out.write( bytes, 0, count );
        }

    }

    /**
     * Pipe all data from an input source to an output stream
     *
     * @param in Input stream
     * @param out Output stream
     * @param length Number of bytes to read
     * @throws IOException on underlying IO error
     *
     */
    public static void pipe( InputStream in, OutputStream out, int length ) throws IOException {

        int bufferSize = 16 * 1024;
        byte[] bytes = new byte[ bufferSize ];
        int count;
        int remaining = length;
        while( ( count = in.read( bytes, 0, Math.min( bufferSize, remaining ) ) ) > 0 ) {
            out.write( bytes, 0, count );
        }

    }

}

