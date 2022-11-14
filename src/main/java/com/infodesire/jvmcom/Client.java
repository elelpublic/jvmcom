package com.infodesire.jvmcom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {


  private static Logger logger = LoggerFactory.getLogger( "Client" );


  private final String host;
  private final int port;
  private PrintWriter serverOut;
  private Socket socket;
  private BufferedReader in;


  public Client( String host, int port ) {
    this.host = host;
    this.port = port;
  }


  public void connect( boolean interactive ) throws IOException {

    logger.info( "Connecting to " + host + ":" + port );
    socket = new Socket( host, port );
    serverOut = new PrintWriter( socket.getOutputStream() );
    in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
    logger.info( "Server reply: " + getReply() );

    if( interactive ) {

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

  }

  /**
   * Send command to server and return reply
   *
   * @param line Command line
   * @return Server reply
   *
   */
  public StringBuffer send( String line ) throws IOException {
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
      result.append( newline + line );
      newline = "\n";
    }

    return result;

  }

  private void sendImpl( String line ) {
    serverOut.println( line );
    serverOut.flush();
  }

  public void close() throws IOException {
    logger.info( "Closing connection." );
    socket.close();
  }

}

