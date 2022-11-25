package com.infodesire.jvmcom.clientserver;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * To keep connections open to multiple clients, this class caches them for later reuse.
 *
 */
public class LineBufferClients {

  private static Logger logger = LoggerFactory.getLogger( "Client" );

  private final long maxLifetimeMs;
  private PMap<String, LineBufferClient> clients = HashTreePMap.empty();
  private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);

  /**
   * Create cache list of clients to different servers
   *
   * @param maxLifetimeMs Max lifetime of a client before auto closing them.
   *
   */
  public LineBufferClients( long maxLifetimeMs ) {

    this.maxLifetimeMs = maxLifetimeMs;

    // check after each minute

    int cleanupEverySeconds = 60;
    try {
      cleanupEverySeconds = Integer.parseInt( System.getProperty( "com.infodesire.jvmcom.connections.autoCloseSeconds", "60" ) );
    }
    catch( Exception ex ) {
      logger.error( "Error parsing com.infodesire.jvmcom.connections.autoCloseSeconds. Will use 60 seconds", ex );
    }
    cleanupScheduler.scheduleAtFixedRate( () -> {
      long time = System.currentTimeMillis();
      for( String key : clients.keySet() ) {
        LineBufferClient client = clients.get( key );
        if( time - client.getCreatedTime() > maxLifetimeMs ) {
          try {
            if( client.isConnected() ) {
              client.close();
            }
          }
          catch( IOException ex ) {
            logger.warn( "Error closing client", ex );
          }
          clients = clients.minus( key );
        }
      }
    }, cleanupEverySeconds, cleanupEverySeconds, TimeUnit.SECONDS );

  }

  /**
   * Get cached client or create new one
   *
   * @param hostName
   * @param port
   * @return
   */
  public LineBufferClient getClient( String hostName, int port ) throws IOException {
    String key = getKey( hostName, port );
    LineBufferClient client = clients.get( key );
    if( client == null ) {
      client = new LineBufferClient( hostName, port );
      clients = clients.plus( key, client );
      client.connect( false );
    }
    return client;
  }

  private String getKey( String hostName, int port ) {
    return hostName + ":" + port;
  }

}
