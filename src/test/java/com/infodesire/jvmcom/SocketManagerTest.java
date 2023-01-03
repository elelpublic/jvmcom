package com.infodesire.jvmcom;

import org.junit.Test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SocketManagerTest {


  private static List<String> log;

  private static void log( String line ) {
    System.out.println( Thread.currentThread() + ": " + line );
    log.add( line );
  }

  @Test
  public void testCommunication() throws IOException {

    log = new ArrayList<>();

    String host = "localhost";

    Supplier<ServerWorker> workerFactory = new WorkerFactory();
    SocketManager manager = new SocketManager( 0, 3, workerFactory, "SERVER"  );
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
    String reply = log.get( linecounter++ );
    assertTrue( reply.startsWith( "request from " ) );
    assertTrue( reply.endsWith( ":HELO" ) );

    assertEquals( "reply:OK", log.get( linecounter ) );

  }

  @Test
  public void testLifecycleWithRestart() throws IOException, InterruptedException {

    log = new ArrayList<>();

    String host = "localhost";

    Supplier<ServerWorker> workerFactory = new WorkerFactory();
    SocketManager manager = new SocketManager( 0, 3, workerFactory, "SERVER" );
    int port = manager.getPort();

    Client client = new Client( host, port );
    assertEquals( "welcome", client.receive() );
    manager.stop( 1000 );
    assertFalse( client.ping() );

    manager = new SocketManager( port, 3, workerFactory, "SERVER" );
    client = new Client( host, port );
    assertEquals( "welcome", client.receive() );
    assertTrue( client.ping() );
    manager.stop( 1000 );
    assertFalse( client.ping() );

  }


  static class Client implements AutoCloseable {

    PrintWriter out;
    BufferedReader in;
    Socket clientSocket;

    Client( String host, int port ) throws IOException {
      clientSocket = new Socket( host, port );
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
    public boolean ping() {
      send( "ping" );
      try {
        String reply = receive();
        return reply != null && reply.equals( "OK" );
      }
      catch( IOException ex ) {
        return false;
      }
    }

    @Override
    public void close() throws Exception {
      clientSocket.close();
    }

  }

  class WorkerFactory implements Supplier<ServerWorker> {
    public ServerWorker get() {
      return new Worker();
    }
  }

  class Worker implements ServerWorker {
    private boolean stopRequest = false;
    private String sender;

    public void work( Socket socket ) {

      try {
        BufferedReader reader = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
        while( reader.ready() ) {
          reader.readLine();
        }
        PrintWriter writer = new PrintWriter( new OutputStreamWriter( socket.getOutputStream() ) );
        writer.println( "welcome" );
        writer.flush();
        while( !stopRequest ) {
          String line = null;
          try {
            line = reader.readLine();
            log( "request from " + sender + ":" + line );
          }
          catch( IOException ex ) {}
          if( line == null ) {
            stopRequest = true; // null means end of stream
          }
          writer.println( "OK" );
          writer.flush();
        }
      }
      catch( IOException ex ) {
        ex.printStackTrace();
      }
    }

    @Override
    public void requestStop() {
      stopRequest = true;
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


