package com.infodesire.jvmcom.services.value;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.clientserver.LineBufferClient;
import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class ValuesServerTest {


  @BeforeClass
  public static void beforeClass() {
    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "warn" );
  }


  @Test
  public void testStartupShutdown() throws IOException, InterruptedException {

    ServerConfig config = new ServerConfig();
    config.port = 0;
    config.threadCount = 3;
    config.serverThreadNamePattern = "Server Thread";
    config.workerThreadNamePattern = "Worker Thread";
    ValueServer server = new ValueServer( config );
    server.start();

    int port = server.getPort();

    try( LineBufferClient client = new LineBufferClient( "localhost", port ) ) {

      client.connect( false );
      TestCase.assertTrue( client.ping() );

      server.stop( 100 );
      server.start();

      assertEquals( port, server.getPort() );

      client.close();

    }

  }

  @Test
  public void testMapping() throws IOException {

    ServerConfig config = new ServerConfig();
    config.port = 0;
    config.threadCount = 3;
    config.serverThreadNamePattern = "Server Thread";
    config.workerThreadNamePattern = "Worker Thread";
    ValueServer server = new ValueServer( config );
    server.start();

    int port = server.getPort();

    ValueClient client = new ValueClient( "localhost", port );
    client.connect( false );

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
