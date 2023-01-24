package com.infodesire.jvmcom.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class FileUtilsTest {

    @Test
    public void checksum() throws IOException, NoSuchAlgorithmException {

        File file1 = new File( "src/test/ressources/HelloWorld.pdf" );
        File file2 = new File( "src/test/ressources/HelloWorld.png" );

        assertEquals( FileUtils.checksum( file1 ), FileUtils.checksum( file1 ) );
        assertNotEquals( FileUtils.checksum( file1 ), FileUtils.checksum( file2 ) );

    }

    @Test
    public void tempDir() throws IOException {

        File tempDir = null;
        try {
            tempDir = FileUtils.createTempDir();
            assertTrue( tempDir.exists() );
            assertTrue( tempDir.isDirectory() );
        }
        finally {
            if( tempDir != null ) {
                FileUtils.rmdir( tempDir );
            }
        }

    }

    @Test
    public void rmdir() throws IOException {

        File tempDir = FileUtils.createTempDir();
        File hello = new File( tempDir, "hello.txt" );
        PrintWriter out = new PrintWriter( hello );
        out.println( "Hello World!" );
        out.close();
        File subDir = new File( tempDir, "sub" );
        subDir.mkdirs();
        File subText = new File( tempDir, "subtext.txt" );
        out = new PrintWriter( subText );
        out.println( "Hello Underworld!" );
        out.close();

        assertTrue( tempDir.exists() );
        assertTrue( tempDir.isDirectory() );
        assertTrue( hello.exists() );
        assertTrue( hello.isFile() );
        assertTrue( hello.length() > 0 );
        assertTrue( subDir.exists() );
        assertTrue( subText.exists() );
        assertTrue( subText.isFile() );
        assertTrue( subText.length() > 0 );

        FileUtils.rmdir( tempDir );

        assertFalse( tempDir.exists() );
        assertFalse( hello.exists() );
        assertFalse( subDir.exists() );
        assertFalse( subText.exists() );

    }

}