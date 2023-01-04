package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.clientserver.text.TextHandler;
import com.infodesire.jvmcom.clientserver.text.TextServer;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Server socket and client socket which can perform all the meshing protocol
 *
 */
public class MeshSocket {

  private final TextServer server;

  public MeshSocket( NodeAddress myAddress, Supplier<TextHandler> handlerFactory ) throws IOException {
    ServerConfig config = new ServerConfig( myAddress.getInetSocketAddress().getPort() );
    server = new TextServer( config, handlerFactory );
    server.start();
  }

  /**
   * Close server and all client sockets
   *
   * @param timeoutMs Number of ms to wait for orderly leave
   * @throws InterruptedException
   */
  public void close( long timeoutMs ) throws InterruptedException {
    server.stop( timeoutMs );
  }

}
