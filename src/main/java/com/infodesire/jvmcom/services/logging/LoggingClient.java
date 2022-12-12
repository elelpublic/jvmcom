package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A client for logging onto a remove logging service
 *
 */
public class LoggingClient implements AutoCloseable {

  private static Logger localLog = LoggerFactory.getLogger( "Logging" );
  private final LineBufferClient client;
  private Level level = Level.INFO;


  /**
   * Create logger which is a client to a remote logging service
   *
   * @param serverAddress Address of remote logging server
   * @param log Name of logging category
   *
   */
  public LoggingClient( InetSocketAddress serverAddress, String log ) throws IOException {
    this( new Socket( serverAddress.getHostName(), serverAddress.getPort() ), log );
  }

  /**
   * Create logger which is a client to a remote logging service
   *
   * @param socket Connection to remote logging server
   * @param log Name of logging category
   *
   */
  public LoggingClient( Socket socket, String log ) throws IOException {
    client = new LineBufferClient( socket );
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
