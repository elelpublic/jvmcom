package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.ServerConfig;
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

  public MeshSocket( NodeAddress myAddress, Supplier<LineBufferHandler> handlerFactory ) throws IOException {
    this.myAddress = myAddress;
    ServerConfig config = new ServerConfig( myAddress.getInetSocketAddress().getPort() );
    server = new LineBufferServer( config, handlerFactory );
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
