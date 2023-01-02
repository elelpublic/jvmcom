package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.pool.SocketPool;
import org.junit.After;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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

    class TestLoggingHandler extends LoggingHandlerImpl {
      public final List<String> lines = new ArrayList<>();
      public void setSender( InetSocketAddress senderAddress ) {}
      public HandlerReply process( String line ) {
        HandlerReply reply = super.process( line );
        lines.add( line );
        return reply;
      }
    }
    TestLoggingHandler testLoggingHandler = new TestLoggingHandler();

    try(
            LoggingService service = new LoggingService( 0, new Supplier<LoggingHandler>() {
              @Override
              public LoggingHandler get() {
                return testLoggingHandler;
              }
            } );
      ) {

      service.start();
      InetSocketAddress serviceAddress = new InetSocketAddress( "localhost", service.getPort() );
      LoggingClient client = new LoggingClient( new SocketPool(), serviceAddress, "test" );
      ressources.add( client );

      assertEquals( Level.INFO, client.getLevel() );

      client.log( Level.INFO, "Hello World" );

      int lineIndex = 0;
      assertEquals( "level", testLoggingHandler.lines.get( lineIndex++ ) );
      assertEquals( "log Hello World", testLoggingHandler.lines.get( lineIndex++ ) );

    }

  }

}