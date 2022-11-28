package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NodeTest {

  private MeshConfig config;
  private SocketPool socketPool;

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

  }

  @After
  public void tearDown() {
  }

  @Test
  public void testPing() throws Exception {

    Node node1 = new Node( config, config.getMembers().get( "node1" ), socketPool );
    Node node2 = new Node( config, config.getMembers().get( "node2" ), socketPool );

    node1.join();
    node2.join();

    assertEquals( "node2", node1.ping( new LineBufferClient( socketPool.getSocket( node2.getAddress() ) ) ) );
    assertEquals( "node1", node2.ping( new LineBufferClient( socketPool.getSocket( node1.getAddress() ) ) ) );

    node1.shutDown( 100 );
    node2.shutDown( 100 );

  }


  @Test
  public void testJoinLeave() throws IOException {

    Node node1 = new Node( config, config.getMembers().get( "node1" ), socketPool );
    Node node2 = new Node( config, config.getMembers().get( "node2" ), socketPool );

    assertFalse( node1.hasJoined() );
    assertFalse( node2.hasJoined() );

    node1.join();

    assertTrue( node1.hasJoined() );
    assertFalse( node2.hasJoined() );

    assertEquals( 1, node1.getActiveMembers().size() );
    assertEquals( node1.getAddress(), node1.getActiveMembers().iterator().next() );

    node2.join();

    assertTrue( node1.hasJoined() );
    assertTrue( node2.hasJoined() );

    assertEquals( 2, node1.getActiveMembers().size() );

    assertTrue( node1.getActiveMembers().contains( node1.getAddress() ) );
    assertTrue( node1.getActiveMembers().contains( node2.getAddress() ) );

    assertTrue( node2.getActiveMembers().contains( node1.getAddress() ) );
    assertTrue( node2.getActiveMembers().contains( node2.getAddress() ) );

    node1.shutDown( 1000 );

    assertEquals( 1, node2.getActiveMembers().size() );
    assertFalse( node2.getActiveMembers().contains( node1.getAddress() ) );
    assertTrue( node2.getActiveMembers().contains( node2.getAddress() ) );

    node2.shutDown( 1000 );

    Node node3 = new Node( config, config.getMembers().get( "node3" ), socketPool );
    node3.join();

    assertEquals( 1, node3.getActiveMembers().size() );
    assertTrue( node3.getActiveMembers().contains( node3.getAddress() ) );

    node3.shutDown( 1000 );

  }


}