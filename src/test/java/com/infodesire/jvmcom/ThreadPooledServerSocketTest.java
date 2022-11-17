package com.infodesire.jvmcom;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ThreadPooledServerSocketTest {


  private static List<String> log;

  private static void log( String line ) {
    System.out.println( Thread.currentThread() + ": " + line );
    log.add( line );
  }

  @Test
  public void testCommunication() throws IOException {

    log = new ArrayList<>();

    String host = "localhost";

    Supplier<Consumer<Socket>> workerFactory = new WorkerFactory();
    ThreadPooledServerSocket manager = new ThreadPooledServerSocket( 0, 3, workerFactory, "SERVER"  );
    int port = manager.getPort();

    Client client = new Client( host, port );

    String line = client.receive();
    log( line );

    client.send( "HELO" );

    line = client.receive();
    log( "reply:" + line );

    int linecounter = 0;
    assertEquals( 3, log.size() );
    assertEquals( "welcome", log.get( linecounter++ ) );
    assertEquals( "request:HELO", log.get( linecounter++ ) );
    assertEquals( "reply:OK", log.get( linecounter ) );

  }

  @Test
  public void testLifecycle() throws IOException, InterruptedException {

    log = new ArrayList<>();

    String host = "localhost";

    Supplier<Consumer<Socket>> workerFactory = new WorkerFactory();
    ThreadPooledServerSocket manager = new ThreadPooledServerSocket( 0, 3, workerFactory, "SERVER" );
    int port = manager.getPort();

    assertEquals( "welcome", new Client( host, port ).receive() );
    manager.stop( 1000 );
    assertFalse( ping( host, port ) );

    manager = new ThreadPooledServerSocket( port, 3, workerFactory, "SERVER" );
    assertEquals( "welcome", new Client( host, port ).receive() );
    assertTrue( ping( host, port ) );
    manager.stop( 1000 );
    assertFalse( ping( host, port ) );

  }

  private boolean ping( String host, int port ) {
    try {
      new Socket( host, port ).close();
      return true;
    }
    catch( IOException ex ) {
      return false;
    }
  }

  class Client {

    PrintWriter out;
    BufferedReader in;
    Client( String host, int port ) throws IOException {

      Socket clientSocket = new Socket( host, port );
      out = new PrintWriter( new OutputStreamWriter( clientSocket.getOutputStream() ) );
      in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );
    }
    String receive() throws IOException {
      return in.readLine();
    }
    void send( String line ) {
      out.println( line );
      out.flush();
    }
  }

  class WorkerFactory implements Supplier<Consumer<Socket>> {
    public Consumer<Socket> get() {
      return new Worker();
    }
  }

  class Worker implements Consumer<Socket> {
    private boolean stop = false;
    public void accept( Socket socket ) {
      work( socket );
    }
    private void work( Socket socket ) {
      try {
        BufferedReader reader = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
        while( reader.ready() ) {
          reader.readLine();
        }
        PrintWriter writer = new PrintWriter( new OutputStreamWriter( socket.getOutputStream() ) );
        writer.println( "welcome" );
        writer.flush();
        while( !stop ) {
          String line = reader.readLine();
          if( line == null ) {
            stop = true; // null means end of stream
          }
          log( "request:" + line );
          writer.println( "OK" );
          writer.flush();
        }
      }
      catch( IOException ex ) {
        //ex.printStackTrace();
      }
    }
  }


//  @Test
//  public void explainSockets() throws IOException, InterruptedException {
//
//    ServerSocket serverSocket = new ServerSocket( 0 );
//    int port = serverSocket.getLocalPort();
//
//    ExecutorService background = Executors.newFixedThreadPool( 2 );
//
//    // server
//    background.submit( () -> {
//      try {
//        Socket socket = serverSocket.accept(); // wait for connection
//        BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
//        PrintWriter out = new PrintWriter( new OutputStreamWriter( socket.getOutputStream() ) );
//        while( !background.isShutdown() ) {
//          String line = in.readLine();
//          System.out.println( "server.received:" + line );
//          out.println( line );
//          out.flush();
//        }
//      }
//      catch( IOException ex ) {
//        ex.printStackTrace();
//      }
//    });
//
//    // client
//    background.submit( () -> {
//      try {
//        Socket socket = new Socket( "localhost", port );
//        BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
//        PrintWriter out = new PrintWriter( new OutputStreamWriter( socket.getOutputStream() ) );
//        int counter = 0;
//        while( !background.isShutdown() ) {
//          out.println( "Hi Server. Request #" + counter++ );
//          out.flush();
//          String line = in.readLine();
//          System.out.println( "client.received:" + line );
//        }
//      }
//      catch( IOException ex ) {
//        ex.printStackTrace();
//      }
//
//    });
//
//    Thread.sleep( 50 );
//
//  }


}


