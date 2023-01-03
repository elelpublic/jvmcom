package com.infodesire.jvmcom.clientserver;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.pool.SocketPool;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class BinaryServerTest {


  @Test
  public void testNullReply() throws Exception {
    ServerConfig config = new ServerConfig();
    Supplier<BinaryHandler> handlerFactory = new EmptyHandlerFactory();
    BinaryServer server = new BinaryServer( config, handlerFactory );
    server.start();
    try( BinaryClient client = new BinaryClient( new SocketPool(), "localhost", server.getPort() ) ) {
      assertEquals( "", "" + client.send( "hello" ) );
    }
  }

  static class EmptyHandlerFactory implements Supplier<BinaryHandler> {
    public BinaryHandler get() {
      return new EmptyReply();
    }
  }

  static class EmptyReply implements BinaryHandler {
    public void setSender( InetSocketAddress senderAddress ) {}
    public HandlerReply process( String line ) {
      return null;
    }
  }

}