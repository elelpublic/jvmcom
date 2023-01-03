package com.infodesire.jvmcom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * A server socket manager which uses a thread pool for workers serving requests
 * and cleans up (closed) all sockets in an orderly fashion when the thread pool ist stopped.
 * <p>
 * Idea found here: <a href="https://stackoverflow.com/questions/25528634/reliably-closing-client-sockets-in-a-thread-pool-on-shutdown">stackoverflow</a>
 *
 */
public class SocketManager {


  private final ServerThread serverThread;
  private final ThreadPoolExecutor pool;


  private final Set<WorkerThread> workers = new HashSet<>();


  /**
   * Create pooled server socket manager
   *
   * @param port The port to listen for new connections on
   * @param poolSize Size of thread pool (i.e. max concurrent workers)
   * @param workerFactory Should create workers which handle requests sent via the socket
   * @param serverThreadName Optional name to be set to server thread
   *
   */
  public SocketManager( int port, int poolSize,
    Supplier<ServerWorker> workerFactory, String serverThreadName ) throws IOException {

    ServerSocket serverSocket = new ServerSocket( port );

    pool = new ThreadPoolExecutor( poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>() )  {

      protected void afterExecute(Runnable r, Throwable t) {
        // The future returns the Worker object!
        WorkerThread worker;
        try {
          worker = (WorkerThread)((FutureTask<?>)r).get();
          if( worker != null ) {
            // Remove it from list of known clients
            worker.requestStop();
            workers.remove( worker );
          }
        }
        catch( InterruptedException | ExecutionException ignored ) {}
      }

    };

    serverThread = new ServerThread( serverSocket, workerFactory );
    if( serverThreadName != null ) {
      serverThread.setName( serverThreadName );
    }
    serverThread.start();

  }


  /**
   * Close all sockets, stop workers and shutdown pool
   *
   */
  public void stop( long timeoutMs ) throws InterruptedException {
    for( WorkerThread worker : workers ) {
      worker.stop();
    }
    pool.shutdown();
    pool.awaitTermination( timeoutMs, TimeUnit.MILLISECONDS );
    serverThread.shutdown();
  }


  /**
   * Get the port. This is useful if the original port was 0. Then the
   * system will find and use the next free port.
   *
   * @return Real port used by the server socket
   *
   */
  public int getPort() {
    return serverThread.getPort();
  }

  public void waitForShutDown() throws InterruptedException {
    serverThread.join();
  }


  class ServerThread extends Thread {

    private final ServerSocket serverSocket;
    private final Supplier<ServerWorker> workerFactory;
    private boolean shutdown = false;
    private final AtomicLong ids = new AtomicLong();

    ServerThread( ServerSocket serverSocket, Supplier<ServerWorker> workerFactory ) {
      this.serverSocket = serverSocket;
      this.workerFactory = workerFactory;
    }

    public void run() {
      while( !shutdown ) {
        try {
          Socket socket = serverSocket.accept();
          WorkerThread worker = new WorkerThread( ids.incrementAndGet(), socket, workerFactory.get() );
          pool.submit( worker );
          workers.add( worker );
        }
        catch( IOException ex ) {
          if( !shutdown ) {
            ex.printStackTrace();
          }
        }
      }
    }

    public void shutdown() {
      shutdown = true;
      try {
        serverSocket.close();
      }
      catch( IOException ignored ) {}
      interrupt();
    }

    /**
     * Get the port. This is useful if the original port was 0. Then the
     * system will find and use the next free port.
     *
     * @return Real port used by the server socket
     *
     */
    public int getPort() {
      return serverSocket.getLocalPort();
    }

  }


  static class WorkerThread implements Runnable {

    private final Object id;
    private final Socket socket;
    private final ServerWorker worker;

    WorkerThread( Object id, Socket socket, ServerWorker worker ) {
      this.id = id;
      this.socket = socket;
      this.worker = worker;
    }

    @Override
    public void run() {
      worker.work( socket );
    }
    
    public int hashCode() {
      return id.hashCode();
    }

    public boolean equals( Object other ) {
      return other instanceof WorkerThread && id.equals( ((WorkerThread) other).id );
    }

    public void stop() {
      try {
        socket.close();
      }
      catch( IOException ignored ) {}
    }

    public void requestStop() {
      worker.requestStop();
    }

  }

}
