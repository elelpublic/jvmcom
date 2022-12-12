package com.infodesire.jvmcom.services.logging;

import org.junit.After;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LoggingServiceTest {

  List<AutoCloseable> ressources = new ArrayList<>();

  @After
  public void cleanup() {
    for( AutoCloseable closeable : ressources ) {
      try {
        closeable.close();
      }
      catch( Exception ex ) {
        ex.printStackTrace();
      }
    }
  }

  @Test
  public void testRemoteLogging() throws Exception {

    try(
      LoggingService service = new LoggingService( 0 );
      ) {

      service.start();
      InetSocketAddress serviceAddress = new InetSocketAddress( "localhost", service.getPort() );
      LoggingClient client = new LoggingClient( serviceAddress, "test" );
      ressources.add( client );

      assertEquals( Level.INFO, client.getLevel() );

    }

  }

}