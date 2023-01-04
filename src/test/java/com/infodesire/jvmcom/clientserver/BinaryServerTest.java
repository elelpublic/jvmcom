package com.infodesire.jvmcom.clientserver;

import com.infodesire.jvmcom.clientserver.binary.Message;
import com.infodesire.jvmcom.util.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.infodesire.jvmcom.clientserver.binary.Status.OK;
import static org.junit.Assert.*;

public class BinaryServerTest {

    static {
        System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "info" );
    }

    private static final Logger logger = LoggerFactory.getLogger( "BinaryServerTest" );


    @Test
    public void testBinarySocketStream() throws Exception {

        List<Exception> insideFails = new ArrayList<>();

        try(
                ServerSocket serverSocket = new ServerSocket( 0 );
                Socket client = new Socket( "localhost", serverSocket.getLocalPort() );
        ) {

            Runnable s = () -> {
                try(
                        Socket server = serverSocket.accept();
                        InputStream serverIn = server.getInputStream();
                ) {
                    int b = serverIn.read();
                    while( b != -1 ) {
                        logger.debug( "r:" + b );
                        b = serverIn.read();
                    }
                    logger.debug( "Finished reading" );
                }
                catch( IOException ex ) {
                    insideFails.add( ex );
                }
            };

            Runnable c = () -> {
                try(
                        OutputStream clientOut = client.getOutputStream();
                ) {
                    for( int i = 0; i < 10; i++ ) {
                        logger.debug( "w:" + i );
                        clientOut.write( i );
                        clientOut.flush();
                        Thread.yield();
                    }
                    logger.debug( "Finished writing" );
                }
                catch( IOException ex ) {
                    insideFails.add( ex );
                }
            };

            ExecutorService exec = Executors.newFixedThreadPool( 2 );
            exec.submit( s );
            exec.submit( c );
            exec.shutdown();
            assertTrue( exec.awaitTermination( 2000, TimeUnit.MILLISECONDS ) );

        }
        catch( Exception ex ) {
            throw new RuntimeException( ex );
        }

        for( Exception ex : insideFails ) {
            throw ex;
        }

    }

    @Test
    public void testMessageSerializationViaByteArray() throws IOException, ClassNotFoundException {

        Message message = new Message();
        message.status = OK;
        message.fileName = "hello.txt";
        message.fileSize = 3096;
        message.directoryListing = "..\n.\nhello.txt";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( byteArrayOutputStream );
        out.writeObject( message );

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( byteArrayOutputStream.toByteArray() );
        ObjectInputStream in = new ObjectInputStream( byteArrayInputStream );

        // rule out defaults
        Message dummy = new Message();
        assertNotEquals( message.status, dummy.status );
        assertNotEquals( message.fileName, dummy.fileName );
        assertNotEquals( message.fileSize, dummy.fileSize );
        assertNotEquals( message.directoryListing, dummy.directoryListing );

        // test serialize-deserialize
        Message received = (Message) in.readObject();

        assertEquals( message.status, received.status );
        assertEquals( message.fileName, received.fileName );
        assertEquals( message.fileSize, received.fileSize );
        assertEquals( message.directoryListing, received.directoryListing );

    }


    @Test
    public void testMessageSerializationViaDataStream() throws IOException, ClassNotFoundException {

        Message message = new Message();
        message.status = OK;
        message.fileName = "hello.txt";
        message.fileSize = 3096;
        message.directoryListing = "..\n.\nhello.txt";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream( byteArrayOutputStream );
        message.serialize( out );

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( byteArrayOutputStream.toByteArray() );
        DataInputStream in = new DataInputStream( byteArrayInputStream );

        // rule out defaults
        Message dummy = new Message();
        assertNotEquals( message.status, dummy.status );
        assertNotEquals( message.fileName, dummy.fileName );
        assertNotEquals( message.fileSize, dummy.fileSize );
        assertNotEquals( message.directoryListing, dummy.directoryListing );

        // test serialize-deserialize
        Message received = Message.deserialize( in );

        assertEquals( message.status, received.status );
        assertEquals( message.fileName, received.fileName );
        assertEquals( message.fileSize, received.fileSize );
        assertEquals( message.directoryListing, received.directoryListing );

    }

    @Test
    public void testFileTransferOverSocket() throws Exception {

        List<Exception> insideFails = new ArrayList<>();

        final File sourceFile = new File( "src/test/ressources/HelloWorld.png" );
        final File targetFile = File.createTempFile( "HelloWorld-", ".png" );

        try(
                ServerSocket serverSocket = new ServerSocket( 0 );
                Socket client = new Socket( "localhost", serverSocket.getLocalPort() );
        ) {

            Runnable s = () -> {

                try(
                        Socket server = serverSocket.accept();
                        InputStream in = server.getInputStream();
                        FileOutputStream out = new FileOutputStream( targetFile );
                ) {

                    FileUtils.pipe( in, out );
                    logger.info( "Finished writing file " + targetFile.getAbsolutePath() );

                }
                catch( IOException ex ) {
                    insideFails.add( ex );
                }

            };

            Runnable c = () -> {

                try(
                        OutputStream out = client.getOutputStream();
                        FileInputStream in = new FileInputStream( sourceFile );
                ) {

                    FileUtils.pipe( in, out );
                    logger.info( "Finished sending file " + sourceFile.getAbsolutePath() );

                }
                catch( IOException ex ) {
                    insideFails.add( ex );
                }

            };

            ExecutorService exec = Executors.newFixedThreadPool( 2 );
            exec.submit( s );
            exec.submit( c );
            exec.shutdown();
            assertTrue( exec.awaitTermination( 2000, TimeUnit.MILLISECONDS ) );

        }
        catch( Exception ex ) {
            throw new RuntimeException( ex );
        }

        for( Exception ex : insideFails ) {
            throw ex;
        }

        String sourceChecksum = FileUtils.checksum( sourceFile );
        String targetChecksum = FileUtils.checksum( targetFile );
        assertEquals( sourceChecksum, targetChecksum );

        logger.info( "Equal checksum: " + targetChecksum );

    }

    //@Test
    public void testSendMultipleFileOverSameSocket() throws Exception {

        List<Exception> insideFails = new ArrayList<>();

        final File sourcePNG = new File( "src/test/ressources/HelloWorld.png" );
        final File sourcePDF = new File( "src/test/ressources/HelloWorld.pdf" );

        final File targetDir = File.createTempFile( "dummy-", ".txt" ).getParentFile();

        List<File> sourceFiles = new ArrayList<>();
        sourceFiles.add( sourcePNG );
        sourceFiles.add( sourcePDF );

        List<File> targetFiles = new ArrayList<>();

        try(
                ServerSocket serverSocket = new ServerSocket( 0 );
                Socket client = new Socket( "localhost", serverSocket.getLocalPort() );
        ) {

            Runnable s = () -> {

                try(
                        Socket server = serverSocket.accept();
                        InputStream in = server.getInputStream();
                        DataInputStream din = new DataInputStream( in );
                ) {

                    while( in.available() > 0 ) {

                        Message message = Message.deserialize( din );

                        File targetFile = new File( targetDir, message.fileName );
                        FileOutputStream out = new FileOutputStream( targetFile );
                        FileUtils.pipe( in, out, message.fileSize );
                        targetFiles.add( targetFile );

                        logger.info( "Finished writing file " + targetFile.getAbsolutePath() );

                    }


                }
                catch( IOException ex ) {
                    insideFails.add( ex );
                }

            };


            Runnable c = () -> {

                try(
                        OutputStream out = client.getOutputStream();
                        DataOutputStream dout = new DataOutputStream( out );
                ) {

                    for( File file : sourceFiles ) {

                        Message message = new Message( file );
                        message.serialize( dout );
                        FileInputStream in = new FileInputStream( file );
                        FileUtils.pipe( in, dout );
                        logger.info( "Finished sending file " + file.getAbsolutePath() );

                    }
                }
                catch( IOException ex ) {
                    insideFails.add( ex );
                }

            };

            ExecutorService exec = Executors.newFixedThreadPool( 2 );
            exec.submit( s );
            exec.submit( c );
            exec.shutdown();
            assertTrue( exec.awaitTermination( 2000, TimeUnit.MILLISECONDS ) );

        }
        catch( Exception ex ) {
            throw new RuntimeException( ex );
        }

        for( Exception ex : insideFails ) {
            throw ex;
        }

        for( int i = 0; i < sourceFiles.size(); i++ ) {

            File sourceFile = sourceFiles.get( i );
            File targetFile = targetFiles.get( i );

            String sourceChecksum = FileUtils.checksum( sourceFile );
            String targetChecksum = FileUtils.checksum( targetFile );
            assertEquals( sourceChecksum, targetChecksum );
            logger.info( "Equal checksum: " + targetChecksum );

        }

    }


}