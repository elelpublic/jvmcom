package com.infodesire.jvmcom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A server socket manager which uses a thread pool for workers serving requests
 * and cleans up (closed) all sockets in an orderly fashion when the thread pool ist stopped.
 *
 * Idea found here: https://stackoverflow.com/questions/25528634/reliably-closing-client-sockets-in-a-thread-pool-on-shutdown
 *
 */
public class SocketManager {


  private final ServerThread serverThread;
  private ThreadPoolExecutor pool;


  private Set<Worker> workers = new HashSet<>();


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
    Supplier<Consumer<Socket>> workerFactory, String serverThreadName ) throws IOException {

    ServerSocket serverSocket = new ServerSocket( port );

    pool = new ThreadPoolExecutor( poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>() )  {

      protected void afterExecute(Runnable r, Throwable t) {
        // The future returns the Worker object!
        Worker worker;
        try {
          worker = (Worker)((FutureTask)r).get();
          // Remove it from list of known clients
          workers.remove( worker );
        }
        catch( InterruptedException | ExecutionException ex ) {}
      }

    };

    serverThread = new ServerThread( pool, serverSocket, workerFactory );
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
    for( Worker worker : workers ) {
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
    private final Supplier<Consumer<Socket>> workerFactory;
    private boolean shutdown = false;
    private final AtomicLong ids = new AtomicLong();

    ServerThread( ThreadPoolExecutor pool, ServerSocket serverSocket, Supplier<Consumer<Socket>> workerFactory ) {
      this.serverSocket = serverSocket;
      this.workerFactory = workerFactory;
    }

    public void run() {
      while( !shutdown ) {
        try {
          Socket socket = serverSocket.accept();
          Worker worker = new Worker( ids.incrementAndGet(), socket, workerFactory.get() );
          pool.submit( worker );
          workers.add( worker );
        }
        catch( IOException ex ) {
          ex.printStackTrace();
        }
      }
    }

    public void shutdown() {
      shutdown = true;
      try {
        serverSocket.close();
      }
      catch( IOException ex ) {}
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


  class Worker implements Runnable {

    private final Object id;
    private final Socket socket;
    private final Consumer<Socket> worker;

    Worker( Object id, Socket socket, Consumer<Socket> worker ) {
      this.id = id;
      this.socket = socket;
      this.worker = worker;
    }

    @Override
    public void run() {
      worker.accept( socket );
    }
    
    public int hashCode() {
      return id.hashCode();
    }

    public boolean equals( Object other ) {
      return other instanceof Worker && id.equals( ((Worker) other).id );
    }

    public void stop() {
      try {
        socket.close();
      }
      catch( IOException ex ) {}
    }

  }

}
