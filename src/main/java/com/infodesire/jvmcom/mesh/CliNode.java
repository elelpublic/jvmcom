package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.pool.SocketPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

import static com.infodesire.jvmcom.ConfigProperties.LEAVE_TIMEOUT_MS;

/**
 * A mesh node with a command line interface
 *
 */
public class CliNode extends Node implements Runnable {

  private static Logger logger = LoggerFactory.getLogger( "Mesh" );

  private final CompletableFuture<Void> background;

  public CliNode( Mesh mesh, NodeAddress myAddress, SocketPool socketPool ) throws IOException {
    super( mesh, myAddress, socketPool );

    background = CompletableFuture.runAsync( this );

  }

  @Override
  public void run() {
    BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
    p( "----------------------------------------------------------" );
    p( "Mesh Node CLI." );
    printStatus();
    p( "Enter 'help' for a list of commands." );
    boolean shutDown = false;
    while( !shutDown ) {
      System.out.print( myAddress.getId() + " > " );
      try {
        String input = null;
        try {
          input = in.readLine();
        }
        catch( IOException ex ) {}
        if( input == null ) {
          out( LEAVE_TIMEOUT_MS );
          shutDown = true;
        }
        else {
          if( input.equals( "help" ) ) {
            usage();
          }
          else if( input.equals( "ls" ) ) {
            printStatus();
          }
          else if( input.startsWith( "ping " ) ) {
            String nodeId = input.substring( 5 );
            ping( nodeId );
          }
          else if( input.startsWith( "dm " ) ) {
            String nodeId = input.substring( 3 );
            int sep = nodeId.indexOf( " " );
            String msg = nodeId.substring( sep + 1 );
            nodeId = nodeId.substring( 0, sep );
            dm( nodeId, msg );
          }
          else if( input.startsWith( "cast " ) ) {
            String message = input.substring( 5 );
            String replies = cast( message );
            p( "Replies:" );
            p( replies );
          }
          else if( input.equals( "in" ) ) {
            try {
              in();
            }
            catch( IOException ex ) {
              logger.error( "Error joining mesh.", ex );
            }
            printStatus();
          }
          else if( input.equals( "out" ) ) {
            out( LEAVE_TIMEOUT_MS );
            printStatus();
          }
          else if( input.equals( "up" ) ) {
            updateActiveMembers();
            printStatus();
          }
          else if( input.equals( "quit" ) ) {
            out( LEAVE_TIMEOUT_MS );
            printStatus();
            shutDown = true;
          }
          else {
            usage( "Unknown command: " + input );
          }
        }
      }
      catch( Exception ex ) {
        ex.printStackTrace();
      }
    }
    p( "Bye." );
  }

  private void ping( String nodeId ) {
    try {
      NodeAddress nodeAddress = config.getMembers().get( nodeId );
      LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
      String reply = ping( client );
      p( "Reply: " + reply );

    }
    catch( Exception ex ) {
      ex.printStackTrace();
    }
  }

  private void dm( String nodeId, String message ) {
    try {
      NodeAddress nodeAddress = config.getMembers().get( nodeId );
      LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
      String reply = dm( client, message );
      p( "Reply: " + reply );
    }
    catch( Exception ex ) {
      ex.printStackTrace();
    }
  }

  private void printStatus() {

    p( "----------------------------------------------------------" );
    p( "Mesh name    : " + config.name );
    p( "Node name    : " + myAddress.getId() );
    p( "Node address : " + myAddress.getInetSocketAddress() );
    p( "Node joined  : " + ( isIn() ? "yes" : "no" ) );
    p( "----------------------------------------------------------" );

    for( NodeAddress nodeAddress : config.getMembers().values() ) {
      String nodeStatus = isIn() ? activeMembers.contains( nodeAddress ) ? "in" : "out" : ( nodeAddress.equals( myAddress ) ? "out" : "???" );
      if( myAddress.equals( nodeAddress ) ) {
        nodeStatus += " (this node)";
      }
      p( "Node '" + nodeAddress.getId() + "' " + nodeAddress.getInetSocketAddress() + " " + nodeStatus );
    }
    p( "----------------------------------------------------------" );

  }

  private void p( String line ) {
    System.out.println( line );
  }

  private void usage() {
    usage( null );
  }

  private void usage( String message ) {
    if( message != null ) {
      p( "###########################################" );
      p( message );
      p( "###########################################" );
    }
    p( "Commands:" );
    p( "" );
    p( "ls ................ list nodes and show status" );
    p( "in ................ join the mesh" );
    p( "out ............... leave the mesh" );
    p( "up ................ update mesh status (ping all nodes)" );
    p( "ping id ........... ping node" );
    p( "dm id mesg ........ send a direct message to a node" );
    p( "cast mesg ......... broadcast a message to all active nodes" );
    p( "quit .............. quit CLI" );
    p( "" );
  }

  public void waitForShutDown() {
    background.join();
  }

}
