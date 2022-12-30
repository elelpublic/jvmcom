package com.infodesire.jvmcom.services;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.pool.SocketPool;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class OldValuesServerTest {


  @BeforeClass
  public static void beforeClass() {
    System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "warn" );
  }


  @Test
  public void testStartupShutdown() throws Exception {

    OldValuesServer server = new OldValuesServer( 0, 3 );
    server.setServerThreadName( "Server Thread" );
    server.setWorkerThreadName( "Worker Thread" );
    server.start();

    int port = server.getPort();

    try( LineBufferClient client = new LineBufferClient( new SocketPool(), "localhost", port ) ) {

      assertTrue( client.ping() );

      server.stop( 100 );
      server.start();

      assertEquals( port, server.getPort() );

      client.close();

    }

  }

}
