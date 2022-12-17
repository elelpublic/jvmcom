package com.infodesire.jvmcom;

import com.infodesire.jvmcom.mesh.Mesh;
import com.infodesire.jvmcom.mesh.MeshConfig;
import com.infodesire.jvmcom.mesh.Node;
import com.infodesire.jvmcom.mesh.PrintMessageHandlerFactory;
import com.infodesire.jvmcom.pool.SocketPool;
import com.infodesire.jvmcom.util.FileUtils;
import com.infodesire.jvmcom.util.SocketUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;

public class ConnectionLeakFinder {

    private MeshConfig config;
    private SocketPool socketPool;
    private Node node1, node2, node3;
    private Mesh mesh;

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

        System.out.println( "Using mesh config file: " + tempFile );
        System.out.println( "-------------------------------------------------" );
        System.out.println( FileUtils.readFile( tempFile ) );
        System.out.println( "-------------------------------------------------" );

        Properties properties = new Properties();
        properties.load( new FileReader( tempFile ) );
        config = MeshConfig.loadFromProperties( properties );

        socketPool = new SocketPool();

        mesh = new Mesh( config, socketPool, new PrintMessageHandlerFactory() );

        node1 = mesh.get( "node1" );
        node2 = mesh.get( "node2" );
        node3 = mesh.get( "node3" );

    }

    @After
    public void tearDown() {
        try {
            node1.shutDown( 500 );
        }
        catch( Throwable ex ) {}
        try {
            node2.shutDown( 500 );
        }
        catch( Throwable ex ) {}
        try {
            node3.shutDown( 500 );
        }
        catch( Throwable ex ) {}
    }

    @Test( timeout = 2000 )
    public void testMultipleJoinLeave() throws IOException {

        long timeout = 500;

        node1.join();
        node2.join();

        for( int i = 0; i < 100; i++ ) {
            System.out.println("BEFORE LEAVE " + i);
            node1.leave( timeout );
            System.out.println("BEFORE JOIN " + i);
            node1.join();
        }

        node1.leave( timeout );

        node2.leave( timeout );

    }

}