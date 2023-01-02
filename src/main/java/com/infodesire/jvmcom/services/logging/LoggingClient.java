package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.clientserver.TextClient;
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
  private final TextClient client;
  private Level remoteLevel = Level.INFO;
  private int logCounter = 0;

  /**
   * Create logger which is a client to a remote logging service
   *
   * @param socketPool Pool of sockets
   * @param inetSocketAddress Address of server
   * @param log Name of logging category
   *
   */
  public LoggingClient( SocketPool socketPool, InetSocketAddress inetSocketAddress, String log ) throws Exception {
    client = new TextClient( socketPool, inetSocketAddress );
    updateLevel();
  }

  /**
   * Ask for current log level of the remote server
   */
  public void updateLevel() {
    try {
      CharSequence levelName = client.send( "level" );
      if( levelName != null ) {
        try {
          remoteLevel = Level.valueOf( "" + levelName );
        }
        catch( Exception ex ) {
          localLog.error( "Invalid level sent from server: " + levelName );
        }
      }
    }
    catch( IOException ex ) {
      localLog.error( "Error requesting log level via " + client.toString() );
    }
  }

  /**
   * @return Log level at the loggging server
   */
  public Level getLevel() {
    return remoteLevel;
  }

  @Override
  public void close() throws Exception {
    client.close();
  }

  public void log( Level level, String line ) {
    if( level.isAtLeast( remoteLevel ) ) {
      try {
        CharSequence levelName = client.send( "log " + line );
        logCounter = 0;
        if( levelName != null ) {
          try {
            remoteLevel = Level.valueOf( "" + levelName );
          }
          catch( Exception ex ) {
            localLog.error( "Invalid level sent from server: " + levelName );
          }
        }
      }
      catch( IOException ex ) {
        localLog.error( "Error sending log message to remote logger.", ex );
      }
    }
    else {
      // after some time not logging, because log level to low, get an update on the log level from server
      if( logCounter++ > 1000 ) {
        logCounter = 0;
        updateLevel();
      }
    }
  }

}
