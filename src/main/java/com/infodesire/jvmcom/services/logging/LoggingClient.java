package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.pool.SocketPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * A client for logging onto a remove logging service
 *
 */
public class LoggingClient implements AutoCloseable {

  private static final Logger localLog = LoggerFactory.getLogger( "Logging" );
  private final LineBufferClient client;
  private final Level level = Level.INFO;

  /**
   * Create logger which is a client to a remote logging service
   *
   * @param socketPool Pool of sockets
   * @param inetSocketAddress Address of server
   * @param log Name of logging category
   *
   */
  public LoggingClient( SocketPool socketPool, InetSocketAddress inetSocketAddress, String log ) throws Exception {
    client = new LineBufferClient( socketPool, inetSocketAddress );
    updateLevel();
  }

  /**
   * Ask for current log level of the remote server
   */
  public void updateLevel() {

    try {
      client.send( "level" );
    }
    catch( IOException ex ) {
      localLog.error( "Error requesting log level via " + client.toString() );
    }
  }

  /**
   * @return Log level at the loggging server
   */
  public Level getLevel() {
    return level;
  }

  @Override
  public void close() throws Exception {
    client.close();
  }

}
