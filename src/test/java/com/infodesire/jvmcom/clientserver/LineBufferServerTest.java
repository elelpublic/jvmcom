package com.infodesire.jvmcom.clientserver;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.pool.SocketPool;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class LineBufferServerTest {


  @Test
  public void testNullReply() throws Exception {
    ServerConfig config = new ServerConfig();
    Supplier<LineBufferHandler> handlerFactory = new EmptyHandlerFactory();
    LineBufferServer server = new LineBufferServer( config, handlerFactory );
    server.start();
    try( LineBufferClient client = new LineBufferClient( new SocketPool(), "localhost", server.getPort() ) ) {
      assertEquals( "", "" + client.send( "hello" ) );
    }
  }

  @Test
  public void testEchoReply() throws Exception {
    ServerConfig config = new ServerConfig();
    Supplier<LineBufferHandler> handlerFactory = new EchoHandlerFactory();
    LineBufferServer server = new LineBufferServer( config, handlerFactory );
    server.start();
    try( LineBufferClient client = new LineBufferClient( new SocketPool(), "localhost", server.getPort() ) ) {
      assertEquals( "Hello World!", "" + client.send( "Hello World!" ) );
    }
  }


  static class EmptyHandlerFactory implements Supplier<LineBufferHandler> {
    public LineBufferHandler get() {
      return new EmptyReply();
    }
  }

  static class EchoHandlerFactory implements Supplier<LineBufferHandler> {
    public LineBufferHandler get() {
      return new EchoReply();
    }
  }

  static class EmptyReply implements LineBufferHandler {
    public void setSender( InetSocketAddress senderAddress ) {}
    public HandlerReply process( String line ) {
      return null;
    }
  }

  static class EchoReply implements LineBufferHandler {
    public void setSender( InetSocketAddress senderAddress ) {}
    public HandlerReply process( String line ) {
      return new HandlerReply( line );
    }
  }

}