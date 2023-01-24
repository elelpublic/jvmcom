package com.infodesire.jvmcom.netty.file;

import com.infodesire.jvmcom.util.FileUtils;
import com.infodesire.jvmcom.util.JvmUtils;
import com.infodesire.jvmcom.util.SocketUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileClientTest {

    static {
        System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug" );
        System.setProperty( "org.slf4j.simpleLogger.log.io.netty", "warn" );
    }
    private static final Logger logger = LoggerFactory.getLogger( "FileClientTest" );

    File downloadDir;

    @Before
    public void setUp() throws Exception {
        downloadDir = FileUtils.createTempDir();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.rmdir( downloadDir );
    }

    @Test
    public void download() throws Exception {

        logger.info( "Process id: " + JvmUtils.getProcessId() );

        String host = "localhost";
        int port = SocketUtils.getFreePort();
        boolean useSsl = false;

        FileServer fileServer = null;
        FileClient fileClient = null;

        try {

            fileServer = new FileServer( port, useSsl );
            fileClient = new FileClient( host, port, useSsl, downloadDir.toString() );

            Thread.sleep( 100 );

            String sourceFile = "src/test/ressources/HelloWorld.pdf";
            fileClient.download( sourceFile );

            Thread.sleep( 100 );

            File file = new File( downloadDir, "HelloWorld.pdf" );
            assertTrue( file.exists() );
            assertEquals( FileUtils.checksum( new File( sourceFile ) ), FileUtils.checksum( file ) );

            assertTrue( true );

        }
        finally {
            if( fileClient != null ) {
                fileClient.close();
            }
            if( fileServer != null ) {
                fileServer.close();
            }
        }

    }

    @Test
    public void ssl() {

    }


}