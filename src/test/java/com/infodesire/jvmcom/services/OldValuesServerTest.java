package com.infodesire.jvmcom.services;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class OldValuesServerTest {


  @BeforeClass
  public static void beforeClass() {
    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "warn" );
  }


  @Test
  public void testStartupShutdown() throws IOException, InterruptedException {

    OldValuesServer server = new OldValuesServer( 0, 3 );
    server.setServerThreadName( "Server Thread" );
    server.setWorkerThreadName( "Worker Thread" );
    server.start();

    int port = server.getPort();

    try( LineBufferClient client = new LineBufferClient( "localhost", port ) ) {

      client.connect( false );
      assertTrue( client.ping() );

      server.stop( 100 );
      server.start();

      assertEquals( port, server.getPort() );

      client.close();

    }

  }

}
