package com.infodesire.jvmcom.clientserver;

import com.infodesire.jvmcom.ServerConfig;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class LineBufferServerTest {


  @Test
  public void testNullReply() throws IOException {
    ServerConfig config = new ServerConfig();
    Supplier<LineBufferHandler> handlerFactory = new EmptyHandlerFactory();
    LineBufferServer server = new LineBufferServer( config, handlerFactory );
    server.start();
    try( LineBufferClient client = new LineBufferClient( "localhost", server.getPort() ) ) {
      client.connect( false );
      assertEquals( "", "" + client.send( "hello" ) );
    }
  }

  @Test
  public void testEchoReply() throws IOException {
    ServerConfig config = new ServerConfig();
    Supplier<LineBufferHandler> handlerFactory = new EchoHandlerFactory();
    LineBufferServer server = new LineBufferServer( config, handlerFactory );
    server.start();
    try( LineBufferClient client = new LineBufferClient( "localhost", server.getPort() ) ) {
      client.connect( false );
      assertEquals( "Hello World!", "" + client.send( "Hello World!" ) );
    }
  }


  class EmptyHandlerFactory implements Supplier<LineBufferHandler> {
    public LineBufferHandler get() {
      return new EmptyReply();
    }
  }

  class EchoHandlerFactory implements Supplier<LineBufferHandler> {
    public LineBufferHandler get() {
      return new EchoReply();
    }
  }

  class EmptyReply implements LineBufferHandler {
    public void setSender( InetSocketAddress senderAddress ) {}
    public HandlerReply process( String line ) {
      return null;
    }
  }

  class EchoReply implements LineBufferHandler {
    public void setSender( InetSocketAddress senderAddress ) {}
    public HandlerReply process( String line ) {
      return new HandlerReply( line );
    }
  }

}