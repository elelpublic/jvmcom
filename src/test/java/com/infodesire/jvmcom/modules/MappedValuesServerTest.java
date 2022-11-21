package com.infodesire.jvmcom.modules;

import com.infodesire.jvmcom.line.LineBufferClient;
import com.infodesire.jvmcom.modules.MappedValuesServer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class MappedValuesServerTest {


  @BeforeClass
  public static void beforeClass() {
    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "info" );
  }


  @Test
  public void testStartupShutdown() throws IOException, InterruptedException {

    MappedValuesServer server = new MappedValuesServer( 0, 3 );
    server.setServerThreadName( "Server Thread" );
    server.setWorkerThreadName( "Worker Thread" );
    server.start();

    int port = server.getPort();

    LineBufferClient client = new LineBufferClient( "localhost", port );
    client.connect( false );
    client.send( "help" );
    assertTrue( client.ping() );

    server.stop( 100 );
    server.start();

    assertEquals( port, server.getPort() );

    client.close();

  }

}
