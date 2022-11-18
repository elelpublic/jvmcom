package com.infodesire.jvmcom;

import com.infodesire.jvmcom.modules.MappedValuesServer;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class LifecycleTest {


  @Test
  public void testStartupShutdown() throws IOException, InterruptedException {

    MappedValuesServer server = new MappedValuesServer( 0, 3 );
    server.start();

    int port = server.getPort();

    Client client = new Client( "localhost", port );
    client.connect( false );
    assertTrue( client.ping() );

    server.stop( 100 );
    server.start();

    assertEquals( port, server.getPort() );

    client.close();

  }

}
