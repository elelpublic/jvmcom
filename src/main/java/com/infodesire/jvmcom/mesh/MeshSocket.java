package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.clientserver.LineBufferClients;
import com.infodesire.jvmcom.clientserver.LineBufferHandler;
import com.infodesire.jvmcom.clientserver.LineBufferServer;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Server socket and client socket which can perform all the meshing protocol
 *
 */
public class MeshSocket {

  private final LineBufferServer server;
  private final NodeAddress myAddress;
  private final LineBufferClients clients;

  public MeshSocket( NodeAddress myAddress, Supplier<LineBufferHandler> handlerFactory ) throws IOException {
    this.myAddress = myAddress;
    ServerConfig config = new ServerConfig( myAddress.getInetSocketAddress().getPort() );
    server = new LineBufferServer( config, handlerFactory );
    server.start();
    clients = new LineBufferClients( config.maxClientLifetimeMs );
  }

  public LineBufferClient getClient( String hostName, int port ) throws IOException {
    LineBufferClient client = clients.getClient( hostName, port );
    if( !client.isConnected() ) {
      client.connect( false );
    }
    return client;
  }

  public void close() throws InterruptedException {
    server.stop( 1000 );
  }

}
