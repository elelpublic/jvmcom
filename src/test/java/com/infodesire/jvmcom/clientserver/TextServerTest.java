package com.infodesire.jvmcom.clientserver;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.pool.SocketPool;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class TextServerTest {


  @Test
  public void testNullReply() throws Exception {
    ServerConfig config = new ServerConfig();
    Supplier<TextHandler> handlerFactory = new EmptyHandlerFactory();
    TextServer server = new TextServer( config, handlerFactory );
    server.start();
    try( TextClient client = new TextClient( new SocketPool(), "localhost", server.getPort() ) ) {
      assertEquals( "", "" + client.send( "hello" ) );
    }
  }

  @Test
  public void testEchoReply() throws Exception {
    ServerConfig config = new ServerConfig();
    Supplier<TextHandler> handlerFactory = new EchoHandlerFactory();
    TextServer server = new TextServer( config, handlerFactory );
    server.start();
    try( TextClient client = new TextClient( new SocketPool(), "localhost", server.getPort() ) ) {
      assertEquals( "Hello World!", "" + client.send( "Hello World!" ) );
    }
  }


  static class EmptyHandlerFactory implements Supplier<TextHandler> {
    public TextHandler get() {
      return new EmptyReply();
    }
  }

  static class EchoHandlerFactory implements Supplier<TextHandler> {
    public TextHandler get() {
      return new EchoReply();
    }
  }

  static class EmptyReply implements TextHandler {
    public void setSender( InetSocketAddress senderAddress ) {}
    public HandlerReply process( String line ) {
      return null;
    }
  }

  static class EchoReply implements TextHandler {
    public void setSender( InetSocketAddress senderAddress ) {}
    public HandlerReply process( String line ) {
      return new HandlerReply( line );
    }
  }

}