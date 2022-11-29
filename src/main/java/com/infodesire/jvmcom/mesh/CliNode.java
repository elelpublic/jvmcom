package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.pool.SocketPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A mesh node with a command line interface
 *
 */
public class CliNode extends Node implements Runnable {

  private static Logger logger = LoggerFactory.getLogger( "Mesh" );

  private final CompletableFuture<Void> background;

  private long leaveTimeoutMs = Long.parseLong( System.getProperty( "com.infodesire.jvmcom.mesh.leaveTimeoutMs", "1000" ) );

  public CliNode( MeshConfig config, NodeAddress myAddress, SocketPool socketPool ) throws IOException {
    super( config, myAddress, socketPool );

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
        String input = in.readLine();
        if( input != null ) {
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
          else if( input.equals( "in" ) ) {
            try {
              join();
            }
            catch( IOException ex ) {
              logger.error( "Error joining mesh.", ex );
            }
            printStatus();
          }
          else if( input.equals( "out" ) ) {
            leave( leaveTimeoutMs );
            printStatus();
          }
          else if( input.equals( "quit" ) ) {
            leave( leaveTimeoutMs );
            printStatus();
            shutDown = true;
          }
          else {
            usage( "Unknown command: " + input );
          }
        }
      }
      catch( IOException ex ) {
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

  private void printStatus() {

    p( "----------------------------------------------------------" );
    p( "Mesh name    : " + config.name );
    p( "Node name    : " + myAddress.getId() );
    p( "Node address : " + myAddress.getInetSocketAddress() );
    p( "Node joined  : " + ( hasJoined() ? "yes" : "no" ) );
    p( "----------------------------------------------------------" );

    for( NodeAddress nodeAddress : config.getMembers().values() ) {
      String nodeStatus = hasJoined() ? activeMembers.contains( nodeAddress ) ? "in" : "out" : ( nodeAddress.equals( myAddress ) ? "out" : "???" );
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
    p( "ping id ........... ping node" );
    p( "quit .............. quit CLI" );
    p( "" );
  }

  public void waitForShutDown() {
    background.join();
  }

}
