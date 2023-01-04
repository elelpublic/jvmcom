package com.infodesire.jvmcom.services.value;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.clientserver.text.TextClient;
import com.infodesire.jvmcom.pool.SocketPool;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class ValuesServerTest {


  @BeforeClass
  public static void beforeClass() {
    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "warn" );
  }


  @Test
  public void testStartupShutdown() throws Exception {

    ServerConfig config = new ServerConfig();
    config.port = 0;
    config.threadCount = 3;
    config.serverThreadNamePattern = "Server Thread";
    config.workerThreadNamePattern = "Worker Thread";
    ValueServer server = new ValueServer( config );
    server.start();

    int port = server.getPort();

    try( TextClient client = new TextClient( new SocketPool(), "localhost", port ) ) {

      TestCase.assertTrue( client.ping() );

      server.stop( 100 );
      server.start();

      assertEquals( port, server.getPort() );

      client.close();

    }

  }

  @Test
  public void testMapping() throws Exception {

    ServerConfig config = new ServerConfig();
    config.port = 0;
    config.threadCount = 3;
    config.serverThreadNamePattern = "Server Thread";
    config.workerThreadNamePattern = "Worker Thread";
    ValueServer server = new ValueServer( config );
    server.start();

    int port = server.getPort();

    ValueClient client = new ValueClient( new SocketPool(), new InetSocketAddress( "localhost", port ) );

    assertFalse( client.has( "main", "version" ) );
    assertEquals( 0, client.size( "main" ) );
    client.put( "main", "version", "1.0" );
    assertEquals( 1, client.size( "main" ) );
    assertEquals( "1.0", client.get( "main", "version" ) );
    client.clear( "main" );
    TestCase.assertNull( client.get( "main", "version" ) );
    assertEquals( 0, client.size( "main" ) );

    TestCase.assertNull( client.get( "dummy", "hello" ) );
    assertEquals( 0, client.size( "dummy" ) );

  }

}
