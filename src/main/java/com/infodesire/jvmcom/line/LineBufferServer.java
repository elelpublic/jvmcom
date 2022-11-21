package com.infodesire.jvmcom.line;

import com.infodesire.jvmcom.ServerWorker;
import com.infodesire.jvmcom.SocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

/**
 * Server socket with backed by thread pool of workers.
 * Workers will process client data on a line by line basis.
 *
 */
public class LineBufferServer {

  private final Supplier<LineBufferHandler> handlerFactory;
  private int port;
  private int threadCount;
  private SocketManager serverSocketManager;


  /**
   * Make threads names easier to read for debugging
   */
  private String serverThreadName, workerThreadName;

  private static Logger logger = LoggerFactory.getLogger( "Server" );


  /**
   * Create server. To find a random free port, pass 0 as port. You can get the port number AFTER
   * calling start using getPort().
   *
   * @param port Listening port - 0 for random free port
   *
   */
  public LineBufferServer( int port, int threadCount, 
                           Supplier<LineBufferHandler> handlerFactory ) {
    
    this.port = port;
    this.threadCount = threadCount;
    this.handlerFactory = handlerFactory;
    
  }


  public void start() throws IOException {

    logger.info( "Trying to start server on port " + port );

    ServerSocket serverSocket = new ServerSocket( port );
    if( port == 0 ) {
      port = serverSocket.getLocalPort();
      logger.info( "Free port found " + port );
    }

    serverSocket.close();

    serverSocketManager = new SocketManager( port, threadCount,
      new WorkerFactory( workerThreadName, handlerFactory ), serverThreadName );

  }

  public int getPort() {
    return port;
  }

  public void stop( long timeoutMs ) throws InterruptedException {
    serverSocketManager.stop( timeoutMs );
  }

  public void setServerThreadName( String serverThreadName ) {
    this.serverThreadName = serverThreadName;
  }

  public void setWorkerThreadName( String workerThreadName ) {
    this.workerThreadName = workerThreadName;
  }

  public void waitForShutDown() throws InterruptedException {
    serverSocketManager.waitForShutDown();
  }


  class WorkerFactory implements Supplier<ServerWorker> {

    private final Supplier<LineBufferHandler> handlerFactory;
    private String threadName;

    WorkerFactory( String threadName, Supplier<LineBufferHandler> handlerFactory ) {
      this.threadName = threadName;
      this.handlerFactory = handlerFactory;
    }

    @Override
    public ServerWorker get() {
      return new Worker( threadName, handlerFactory.get() );
    }

  }

  class Worker implements ServerWorker {

    private final String threadName;
    private final LineBufferHandler handler;
    private PrintWriter writer;
    private boolean stopRequest = false;

    Worker( String threadName, LineBufferHandler lineBufferHandler ) {

      this.threadName = threadName;
      this.handler = lineBufferHandler;
    }

    @Override
    public void accept( Socket socket ) {

      if( threadName != null ) {
        Thread.currentThread().setName( threadName );
      }
      logger.info( "Accepted new connection." );
      InputStream in = null;
      OutputStream out = null;
      try {
        in = socket.getInputStream();
        out = socket.getOutputStream();
        BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
        writer = new PrintWriter( new OutputStreamWriter( out ) );
        send( "Welcome!%n" );
        while( !stopRequest ) {
          String line = reader.readLine();
          if( line == null ) {
            break; // null means end of stream
          }
          logger.info( "Client request: '" + line + "'" );
          stopRequest = !handler.process( line );
        }
      }
      catch( IOException ex ) {
        ex.printStackTrace();
      }
      finally {
        if( in != null ) {
          try {
            in.close();
          }
          catch( IOException ex ) {
          }
        }
        if( out != null ) {
          try {
            out.close();
          }
          catch( IOException ex ) {
          }
        }
      }
      logger.info( "Connection closed." );
    }

    private void send( String line, Object... parames ) {

      String reply = String.format( line, parames );
      logger.debug( "Sending: " + reply.trim() );
      writer.print( reply );
      writer.flush();
    }

    @Override
    public void requestStop() {
      stopRequest = true;
    }

  }

}
