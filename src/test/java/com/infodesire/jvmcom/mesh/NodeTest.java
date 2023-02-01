package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.text.TextClient;
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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NodeTest {

  private SocketPool socketPool;
  private Node node1, node2, node3;

  static {
    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug" );
  }
  private static final Logger logger = LoggerFactory.getLogger( "NodeTest" );

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

    socketPool = new SocketPool();

    Mesh mesh = new Mesh( config, socketPool, new PrintMessageHandlerFactory(), new DefaultServiceFactory() );
    Thread.yield();

    node1 = mesh.get( "node1" );
    node2 = mesh.get( "node2" );
    node3 = mesh.get( "node3" );

  }

  @After
  public void tearDown() {
    try {
      node1.shutDown( 500 );
      Thread.yield();
    }
    catch( Throwable ignored ) {}
    try {
      node2.shutDown( 500 );
      Thread.yield();
    }
    catch( Throwable ignored ) {}
    try {
      node3.shutDown( 500 );
      Thread.yield();
    }
    catch( Throwable ignored ) {}
  }

  @Test( timeout = 2000 )
  public void testPing() throws Exception {

    node1.join();
    Thread.yield();
    node2.join();
    Thread.yield();

    try(
            TextClient client1 = new TextClient( socketPool, node2.getAddress() );
            TextClient client2 = new TextClient( socketPool, node1.getAddress() )
    ) {

      assertEquals( "node2", "" + node1.ping( client1 ) );
      assertEquals( "node1", "" + node2.ping( client2 ) );

    }

    node1.shutDown( 100 );
    Thread.yield();
    node2.shutDown( 100 );
    Thread.yield();

  }


  @Test( timeout = 10000 )
  public void testJoinLeave() throws IOException {

    assertFalse( node1.isIn() );
    assertFalse( node2.isIn() );

    node1.join();
    Thread.yield();

    assertTrue( node1.isIn() );
    assertFalse( node2.isIn() );

    assertEquals( 1, node1.getActiveMembers().size() );
    assertEquals( node1.getAddress(), node1.getActiveMembers().iterator().next() );

    node2.join();
    Thread.yield();

    assertTrue( node1.isIn() );
    assertTrue( node2.isIn() );

    assertEquals( 2, node1.getActiveMembers().size() );

    assertTrue( node1.getActiveMembers().contains( node1.getAddress() ) );
    assertTrue( node1.getActiveMembers().contains( node2.getAddress() ) );

    assertTrue( node2.getActiveMembers().contains( node1.getAddress() ) );
    assertTrue( node2.getActiveMembers().contains( node2.getAddress() ) );

    node1.shutDown( 1000 );
    Thread.yield();

    assertEquals( 1, node2.getActiveMembers().size() );
    assertFalse( node2.getActiveMembers().contains( node1.getAddress() ) );
    assertTrue( node2.getActiveMembers().contains( node2.getAddress() ) );

    node2.shutDown( 1000 );
    Thread.yield();

    node3.join();
    Thread.yield();

    assertEquals( 1, node3.getActiveMembers().size() );
    assertTrue( node3.getActiveMembers().contains( node3.getAddress() ) );

    node3.shutDown( 1000 );
    Thread.yield();

  }


  @Test( timeout = 2000 )
  public void testMessages() throws Exception {

    node1.join();
    Thread.yield();
    node2.join();
    Thread.yield();
    node3.join();
    Thread.yield();

    TextClient client = new TextClient( socketPool, node2.getAddress() );
    assertEquals( "OK", "" + node1.dm( client, "hi" ) );
    assertEquals( "", "node2: OK\nnode3: OK", node1.cast( "hello all" ) );

  }


}