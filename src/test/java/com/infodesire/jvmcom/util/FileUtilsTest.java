package com.infodesire.jvmcom.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
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

}