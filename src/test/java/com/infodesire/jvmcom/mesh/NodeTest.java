package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.util.SocketUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Properties;

import static org.junit.Assert.*;

public class NodeTest {

  @Before
  public void setUp() throws Exception {

  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testPing() throws IOException {

    File tempFile = File.createTempFile( "testPing-", ".mesh" );
    PrintWriter out = new PrintWriter( tempFile );
    out.println( "name=world" );
    out.println( "nodes=node1,node2,node3" );
    out.println( "nodes.node1.port=" + SocketUtils.getFreePort() );
    out.println( "nodes.node2.port=" + SocketUtils.getFreePort() );
    out.println( "nodes.node3.port=" + SocketUtils.getFreePort() );
    out.close();

    System.out.println( tempFile );

    Properties properties = new Properties();
    properties.load( new FileReader( tempFile ) );
    MeshConfig config = MeshConfig.loadFromProperties( properties );

    Node node1 = new Node( config, config.getMembers().get( "node1" ) );
    Node node2 = new Node( config, config.getMembers().get( "node2" ) );

    node1.join();
    node2.join();

    assertEquals( "node2", node1.ping( node2.getAddress() ) );
    assertEquals( "node1", node2.ping( node1.getAddress() ) );

  }

}