package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.clientserver.text.TextHandler;
import com.infodesire.jvmcom.clientserver.text.TextServer;
import com.infodesire.jvmcom.services.Service;

import java.io.IOException;
import java.util.function.Supplier;

public class LoggingService implements Service, AutoCloseable, Supplier<TextHandler> {

  private final TextServer server;
  private final Supplier<LoggingHandler> handlerFactory;

  public LoggingService( int port, Supplier<LoggingHandler> handlerFactory ) {
    ServerConfig config = new ServerConfig( port );
    this.handlerFactory = handlerFactory;
    server = new TextServer( config, this );
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
  public TextHandler get() {
    return handlerFactory.get();
  }

  @Override
  public void close() throws Exception {
    stop( 1000 );
  }

}
