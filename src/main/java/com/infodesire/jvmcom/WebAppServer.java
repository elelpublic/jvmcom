package com.infodesire.jvmcom;

import com.infodesire.jvmcom.modules.MappedValuesServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The web app version of client / server
 *
 * This is a singleton, because it handles the communication of exactly one JVM.
 *
 */
public class WebAppServer {

  private static Logger logger = LoggerFactory.getLogger( "WebAppServer" );

  private static boolean isClientConnected = false;
  private static int port;
  private static MappedValuesServer server;
  private static final int THREAD_COUNT = Integer.parseInt( System.getProperty( "com.infodesire.jvmcom.threadCount", "10" ) );
  private static String host;
  private static int serverPort;

  public static boolean isServerRunning() {
    return server != null;
  }

  public static boolean isClientConnected() {
    return isClientConnected;
  }

  public static void startServer() {
    if( !isServerRunning() ) {
      try {
        server = new MappedValuesServer( port, THREAD_COUNT );
        server.setServerThreadName( "SERVER" );
        server.setWorkerThreadName( "WORKER" );
        server.start();
      }
      catch( IOException ex ) {
        logger.error( "Error starting server", ex );
        stopImpl();
      }
    }
  }

  public static void stopServer() {
    if( isServerRunning() ) {
      stopImpl();
    }
  }

  private static void stopImpl() {
    if( server != null ) {
      logger.info( "Stopping server." );
      try {
        server.stop( 1000 );
      }
      catch( Exception ex ) {
        logger.error( "Error stopping server", ex );
      }
      finally {
        server = null;
      }
    }
  }

  public static void connectClient() {
    isClientConnected = true;
  }

  public static void disconnectClient() {
    isClientConnected = false;
  }

  public static void setPort( int port ) {
    WebAppServer.port = port;
  }

  public static void setHost( String host ) {
    WebAppServer.host = host;
  }

  public static void setServerPort( int serverPort ) {
    WebAppServer.serverPort = serverPort;
  }

}
