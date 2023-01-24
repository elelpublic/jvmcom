package com.infodesire.jvmcom.util;

import java.io.*;
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

        FileInputStream fis = new FileInputStream( file );

        byte[] byteArray = new byte[ 1024 * 4 ];
        int bytesCount = 0;

        //Read file data and update in message digest
        while( ( bytesCount = fis.read( byteArray ) ) != -1 ) {
            sha.update( byteArray, 0, bytesCount );
        }

        fis.close();

        byte[] bytes = sha.digest();

        // convert to hex
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < bytes.length; i++ ) {
            sb.append( Integer.toString( ( bytes[ i ] & 0xff ) + 0x100, 16 ).substring( 1 ) );
        }
        return sb.toString();

    }


    /**
     * Pipe all data from an input source to an output stream
     *
     * @param in  Input stream
     * @param out Output stream
     * @throws IOException on underlying IO error
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
     * @param in     Input stream
     * @param out    Output stream
     * @param length Number of bytes to read
     * @throws IOException on underlying IO error
     */
    public static void pipe( InputStream in, OutputStream out, int length ) throws IOException {

        int bufferSize = 16 * 1024;
        byte[] bytes = new byte[ bufferSize ];
        int count;
        int remaining = length;
        while( ( count = in.read( bytes, 0, Math.min( bufferSize, remaining ) ) ) > 0 ) {
            out.write( bytes, 0, count );
            remaining -= count;
        }

    }

    /**
     * @return Newly created directory in the place where temporary files are stored
     * @throws IOException Not able to create a temp dir
     *
     */
    public static File createTempDir() throws IOException {
        return createTempDir( "tmp", null );
    }

    /**
     * @param prefix Prefix of dir name (must be at least 3 chars)
     * @param suffix Optional suffix of dir name (can be null)
     * @return Newly created directory in the place where temporary files are stored
     * @throws IOException Not able to create a temp dir
     *
     */
    public static File createTempDir( String prefix, String suffix ) throws IOException {
        File tmpFile = File.createTempFile( prefix, suffix );
        File tmpDir = new File( tmpFile.getParentFile(), tmpFile.getName() );
        tmpFile.delete();
        tmpDir.mkdirs();
        return tmpDir;
    }

    /**
     * Delete a directory recursively
     *
     * @param dir Directory to delete
     */
    public static void rmdir( File dir ) {
        if( dir.exists() ) {
            File parent = dir.getParentFile();
            for( File file : dir.listFiles() ) {
                if( file.isDirectory() ) {
                    if( !file.equals( dir ) && !file.equals( parent ) ) {
                        rmdir( file );
                    }
                }
                else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

}

