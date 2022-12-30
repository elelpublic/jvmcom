package com.infodesire.jvmcom.clientserver;

import com.infodesire.jvmcom.mesh.NodeAddress;
import com.infodesire.jvmcom.pool.SocketPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Client to a LineBufferServer, sending one line of text at a time.
 *
 */
public class LineBufferClient implements AutoCloseable {


  private static final Logger logger = LoggerFactory.getLogger( "Client" );
  private final SocketPool socketPool;
  private final InetSocketAddress inetSocketAddress;
  private PrintWriter serverOut;
  private Socket socket;
  private BufferedReader in;
  private final long createdTime = System.currentTimeMillis();


  public LineBufferClient( SocketPool socketPool, String host, int port ) throws Exception {
    this( socketPool, new InetSocketAddress( host, port ) );
  }


  public LineBufferClient( SocketPool socketPool, NodeAddress nodeAddress ) throws Exception {
    this( socketPool, nodeAddress.getInetSocketAddress() );
  }

  public LineBufferClient( SocketPool socketPool, InetSocketAddress inetSocketAddress ) throws Exception {
    this.socketPool = socketPool;
    this.inetSocketAddress = inetSocketAddress;
    connect();
  }

  private void welcome() throws IOException {
    serverOut = new PrintWriter( socket.getOutputStream() );
    in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
    logger.info( "Server reply: " + getReply() );
  }

  private void connect() throws Exception {
    logger.info( "Connecting to " + inetSocketAddress );
    socket = socketPool.getSocket( inetSocketAddress );
    welcome();
  }

  public void enterInteractiveMode() throws IOException {

    logger.info( "Connected. Enter text to send now." );

    BufferedReader console = new BufferedReader( new InputStreamReader( System.in ) );
    System.out.print( "> " );
    String line = console.readLine();
    while( line != null ) {
      if( line.equals( "exit" ) ) {
        Runtime.getRuntime().halt( 0 );
      }
      sendImpl( line );
      System.out.println( "< " + getReply() );
      System.out.print( "< " );
      line = console.readLine();
    }

    close();

  }

  /**
   * Send command to server and return reply
   *
   * @param line Command line
   * @return Server reply
   *
   */
  public CharSequence send( String line ) throws IOException {
    sendImpl( line );
    StringBuffer reply = getReply();
    if( logger.isDebugEnabled() ) {
      logger.debug( reply.toString() );
    }
    return reply;
  }

  private StringBuffer getReply() throws IOException {

    while( !in.ready() ) {
      Thread.yield(); // wait for answer
    }

    StringBuffer result = new StringBuffer();

    String newline = "";
    while( in.ready() ) {
      String line = in.readLine();
      result.append( newline );
      result.append( line );
      newline = "\n";
    }

    return result;

  }

  private void sendImpl( String line ) {
    logger.info( "Sending: " + line );
    serverOut.println( line );
    serverOut.flush();
  }

  public void close() throws IOException {
    logger.info( "Closing connection." );
    if( socket != null ) {
      socket.close();
      socket = null;
    }
  }

  /**
   * Test if server is running
   *
   * @return Server replied
   *
   */
  public boolean ping() throws IOException {
    String reply = send( "ping" ).toString();
    logger.info( "Ping reply: " + reply );
    return reply.equals( "OK" );
  }

  public boolean isConnected() {
    return in != null;
  }

  public long getCreatedTime() {
    return createdTime;
  }

  public String toString() {
    if( isConnected() ) {
      return "client connected to " + inetSocketAddress;
    }
    else {
      return "unconnected client";
    }
  }

}

