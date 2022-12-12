package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.clientserver.LineBufferHandler;
import com.infodesire.jvmcom.clientserver.LineBufferServer;
import com.infodesire.jvmcom.services.Service;

import java.io.IOException;
import java.util.function.Supplier;

public class LoggingService implements Service, Supplier<LineBufferHandler>, AutoCloseable {

  private final LineBufferServer server;

  public LoggingService( int port ) {
    ServerConfig config = new ServerConfig( port );
    server = new LineBufferServer( config, this );
  }

  @Override
  public String getName() {
    return "logging";
  }

  @Override
  public int getPort() {
    return server.getPort();
  }

  @Override
  public void start() throws IOException {
    server.start();
  }

  @Override
  public void stop( long timeoutMs ) throws InterruptedException {
    server.stop( timeoutMs );
  }

  @Override
  public LineBufferHandler get() {
    return new LoggingHandler();
  }

  @Override
  public void close() throws Exception {
    stop( 1000 );
  }

}
