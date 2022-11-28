package com.infodesire.jvmcom.mesh;

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

  public CliNode( MeshConfig config, NodeAddress myAddress ) throws IOException {
    super( config, myAddress );

    background = CompletableFuture.runAsync( this );

  }

  @Override
  public void run() {
    BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
    System.out.println( "Node CLI. Enter help for a list of commands." );
    while( true ) {
      System.out.print( getAddress().getId() + " > " );
      try {
        String input = in.readLine();
        if( input != null ) {
          if( input.equals( "help" ) ) {
            usage();
          }
          else if( input.equals( "status" ) ) {
            printStatus();
          }
          else if( input.equals( "join" ) ) {
            try {
              join();
            }
            catch( IOException ex ) {
              logger.error( "Error joining mesh.", ex );
            }
            printStatus();
          }
          else if( input.equals( "leave" ) ) {
            leave();
            printStatus();
          }
          else if( input.equals( "active" ) ) {
            printStatus();
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
  }

  private void printStatus() {

    System.out.println( "Node has" + (hasJoined() ? " " : " not ") + "joined." );

    Collection<NodeAddress> all = getActiveMembers();

    String text = all.stream()
      .map( nodeAddress -> nodeAddress.getId() )
      .collect( Collectors.joining( " " ) );

    System.out.println( all.size() + " nodes: " + text );

  }

  private void usage() {
    usage( null );
  }

  private void usage( String message ) {
    if( message != null ) {
      System.out.println( message );
    }
    System.out.println( "help .............. show list of commands" );
    System.out.println( "status ............ show if this node has joined" );
    System.out.println( "join .............. join the mesh" );
    System.out.println( "leave ............. leave the mesh" );
    System.out.println( "active ............ show nodes in mesh" );
    System.out.println( "ping host:ip ...... ping a node, will return node id" );
  }

  public void waitForShutDown() {
    background.join();
  }

}
