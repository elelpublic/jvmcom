package com.infodesire.jvmcom.clientserver.text;

import com.infodesire.jvmcom.ServerConfig;
import com.infodesire.jvmcom.ServerWorker;
import com.infodesire.jvmcom.SocketManager;
import com.infodesire.jvmcom.clientserver.HandlerReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

/**
 * Server socket with backed by thread pool of workers.
 * Workers will process client data on a line by line basis.
 *
 */
public class TextServer {

  private final Supplier<TextHandler> handlerFactory;
  private final ServerConfig config;
  private int localPort = 0;
  private SocketManager serverSocketManager;


  private static final Logger logger = LoggerFactory.getLogger( "TextServer" );


  /**
   * Create server. To find a random free port, pass 0 as port. You can get the port number AFTER
   * calling start using getPort().
   *
   * @param config Server settings and network configuration
   * @param handlerFactory Creates handlers for incoming socket connections
   *
   */
  public TextServer( ServerConfig config,
                     Supplier<TextHandler> handlerFactory ) {
    
    this.config = config;
    this.handlerFactory = handlerFactory;
    
  }


  public void start() throws IOException {

    localPort = localPort == 0 ? config.port : localPort;
    logger.info( "Trying to start server on port " + localPort );

    ServerSocket serverSocket = new ServerSocket( localPort );
    if( localPort == 0 ) {
      localPort = serverSocket.getLocalPort();
      logger.info( "Free port found " + localPort );
    }

    serverSocket.close();

    serverSocketManager = new SocketManager( localPort, config.threadCount,
            new WorkerFactory( config.workerThreadNamePattern, handlerFactory ), config.serverThreadNamePattern );

    logger.info( "Server started on port " + localPort + ". Waiting for requests." );

  }

  /**
   * @return Port on which server is listening. If 0 was configured, this will be the real local port found.
   *
   */
  public int getPort() {
    return localPort;
  }

  public void stop( long timeoutMs ) throws InterruptedException {
    if( serverSocketManager != null ) {
      serverSocketManager.stop( timeoutMs );
    }
  }

  public void waitForShutDown() throws InterruptedException {
    if( serverSocketManager != null ) {
      serverSocketManager.waitForShutDown();
    }
  }


  static class WorkerFactory implements Supplier<ServerWorker> {

    private final Supplier<TextHandler> handlerFactory;
    private final String threadName;

    WorkerFactory( String threadName, Supplier<TextHandler> handlerFactory ) {
      this.threadName = threadName;
      this.handlerFactory = handlerFactory;
    }

    @Override
    public ServerWorker get() {
      return new Worker( threadName, handlerFactory.get() );
    }

  }

  static class Worker implements ServerWorker {

    private final String threadName;
    private final TextHandler handler;
    private PrintWriter writer;
    private boolean stopRequest = false;

    Worker( String threadName, TextHandler lineBufferHandler ) {

      this.threadName = threadName;
      this.handler = lineBufferHandler;
    }

    @Override
    public void work( Socket socket ) {

      handler.setSender( (InetSocketAddress) socket.getRemoteSocketAddress() );

      if( threadName != null ) {
        Thread.currentThread().setName( threadName );
      }

      logger.debug( "Accepted new connection." );

      try(
              InputStream in = socket.getInputStream();
              OutputStream out = socket.getOutputStream()
      ) {
        BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
        writer = new PrintWriter( new OutputStreamWriter( out ) );
        send( "welcome" );
        while( !stopRequest ) {
          String line = null;
          try {
            line = reader.readLine();
            if( line != null ) {
              logger.info( "Client request: '" + line + "'" );
              HandlerReply reply = handler.process( line );
              if( reply != null ) {
                send( reply.replyText );
                stopRequest = !reply.continueProcessing;
              }
              else {
                send( "" );
              }
            }
          }
          catch( IOException ignored ) {
          }
          if( line == null ) {
            stopRequest = true;
            break; // null means end of stream
          }
        }
      }
      catch( IOException ex ) {
        ex.printStackTrace();
      }
      logger.debug( "Connection closed." );
    }

    private void send( String line ) {
      logger.debug( "Sending: " + line );
      writer.println( line );
      writer.flush();
    }

    @Override
    public void requestStop() {
      stopRequest = true;
    }

  }

}
