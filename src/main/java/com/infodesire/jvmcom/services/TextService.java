package com.infodesire.jvmcom.services;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.clientserver.TextHandler;
import com.infodesire.jvmcom.clientserver.TextServer;

import java.util.function.Supplier;

/**
 * A text service is a text based service (as opposed to binary service).
 *
 * It handles messages which can be expressed in text strings.
 *
 */
public abstract class TextService implements Service, Supplier<TextHandler> {

  private final TextServer server;
  private final ServerConfig serverConfig;
  private final String name;

  TextService( String name, ServerConfig serverConfig ) {
    this.name = name;
    this.serverConfig = serverConfig;
    server = new TextServer( serverConfig, this );
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getPort() {
    return serverConfig.port;
  }

}
