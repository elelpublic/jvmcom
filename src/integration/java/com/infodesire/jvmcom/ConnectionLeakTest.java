package com.infodesire.jvmcom;

import com.infodesire.jvmcom.mesh.Mesh;
import com.infodesire.jvmcom.mesh.MeshConfig;
import com.infodesire.jvmcom.mesh.Node;
import com.infodesire.jvmcom.mesh.PrintMessageHandlerFactory;
import com.infodesire.jvmcom.pool.SocketPool;
import com.infodesire.jvmcom.services.DefaultServiceFactory;
import com.infodesire.jvmcom.util.FileUtils;
import com.infodesire.jvmcom.util.SocketUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;

public class ConnectionLeakTest {

    private Node node1, node2, node3;

    static {
        System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "warn" );
    }
    private static final Logger logger = LoggerFactory.getLogger( "ConnectionLeakTest" );

    @Before
    public void setUp() throws Exception {

        Iterator<Integer> freePorts = SocketUtils.getFreePorts( 3 ).iterator();

        File tempFile = File.createTempFile( "testPing-", ".mesh" );
        PrintWriter out = new PrintWriter( tempFile );
        out.println( "name=world" );
        out.println( "nodes=node1,node2,node3" );
        out.println( "nodes.node1.port=" + freePorts.next() );
        out.println( "nodes.node2.port=" + freePorts.next() );
        out.println( "nodes.node3.port=" + freePorts.next() );
        out.close();

        logger.debug( "Using mesh config file: " + tempFile );
        logger.debug( "-------------------------------------------------" );
        logger.debug( FileUtils.readFile( tempFile ) );
        logger.debug( "-------------------------------------------------" );

        Properties properties = new Properties();
        properties.load( new FileReader( tempFile ) );
        MeshConfig config = MeshConfig.loadFromProperties( properties );

        SocketPool socketPool = new SocketPool();

        Mesh mesh = new Mesh( config, socketPool, new PrintMessageHandlerFactory(), new DefaultServiceFactory() );

        node1 = mesh.get( "node1" );
        node2 = mesh.get( "node2" );
        node3 = mesh.get( "node3" );

    }

    @After
    public void tearDown() {
        try {
            node1.shutDown( 500 );
        }
        catch( Throwable ignored ) {
        }
        try {
            node2.shutDown( 500 );
        }
        catch( Throwable ignored ) {
        }
        try {
            node3.shutDown( 500 );
        }
        catch( Throwable ignored ) {
        }
    }

    @Test(timeout = 10000)
    public void testMultipleJoinLeave() throws IOException, InterruptedException {

        int CONNECTION_COUNT = 2000;

        long timeout = 500;

        node1.join();
        node2.join();

        for( int i = 0; i < CONNECTION_COUNT; i++ ) {
            Thread.yield();
            logger.debug( "BEFORE LEAVE " + i );
            node1.leave( timeout );
            Thread.yield();
            logger.debug( "BEFORE JOIN " + i );
            node1.join();
        }

        node1.leave( timeout );
        node2.leave( timeout );

    }

}