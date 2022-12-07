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

  public CliNode( Mesh mesh, NodeConfig config, SocketPool socketPool ) throws IOException {
    super( mesh, config, socketPool );

    background = CompletableFuture.runAsync( this );

  }

  @Override
  public void run() {
    BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
    p( "----------------------------------------------------------" );
    p( "Mesh Node CLI." );
    printStatus();
    usage();
    boolean shutDown = false;
    while( !shutDown ) {
      System.out.print( myAddress.getName() + " > " );
      try {
        String input = null;
        try {
          input = in.readLine();
        }
        catch( IOException ex ) {}
        if( input == null ) {
          leave( LEAVE_TIMEOUT_MS );
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
            String nodeName = input.substring( 5 );
            ping( nodeName );
          }
          else if( input.startsWith( "dm " ) ) {
            String nodeName = input.substring( 3 );
            int sep = nodeName.indexOf( " " );
            String msg = nodeName.substring( sep + 1 );
            nodeName = nodeName.substring( 0, sep );
            dm( nodeName, msg );
          }
          else if( input.startsWith( "cast " ) ) {
            String message = input.substring( 5 );
            String replies = cast( message );
            p( "Replies:" );
            p( replies );
          }
          else if( input.equals( "in" ) || input.equals( "join" ) ) {
            try {
              join();
            }
            catch( IOException ex ) {
              logger.error( "Error joining mesh.", ex );
            }
            printStatus();
          }
          else if( input.startsWith( "services " ) ) {
            String nodeName = input.substring( 9 );
            String services = services( nodeName );
            p( services );
          }
          else if( input.equals( "out" ) || input.equals( "leave" ) ) {
            leave( LEAVE_TIMEOUT_MS );
            printStatus();
          }
          else if( input.equals( "up" ) ) {
            updateActiveMembers();
            printStatus();
          }
          else if( input.equals( "quit" ) ) {
            leave( LEAVE_TIMEOUT_MS );
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

  private String services( String nodeName ) {
    if( nodeName.equals( myName ) ) {
      return handleServices().replyText;
    }
    else {
      NodeConfig nodeConfig = meshConfig.getNodeConfig( nodeName );
      try {
        LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeConfig.getAddress() ) );
        return services( client );
      }
      catch( Exception ex ) {
        p( "Error sending services request to " + nodeConfig.getAddress() );
        ex.printStackTrace();
      }
    }
    return "";
  }

  private void ping( String nodeName ) {
    try {
      NodeConfig nodeConfig = meshConfig.getNodeConfig( nodeName );
      LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeConfig.getAddress() ) );
      String reply = ping( client );
      p( "Reply: " + reply );

    }
    catch( Exception ex ) {
      ex.printStackTrace();
    }
  }

  private void dm( String nodeName, String message ) {
    try {
      NodeConfig nodeConfig = meshConfig.getNodeConfig( nodeName );
      LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeConfig.getAddress() ) );
      String reply = dm( client, message );
      p( "Reply: " + reply );
    }
    catch( Exception ex ) {
      ex.printStackTrace();
    }
  }

  private void printStatus() {

    p( "----------------------------------------------------------" );
    p( "Mesh name    : " + meshConfig.name );
    p( "Node name    : " + myAddress.getName() );
    p( "Node address : " + myAddress.getInetSocketAddress() );
    p( "Node joined  : " + ( isIn() ? "yes" : "no" ) );
    p( "----------------------------------------------------------" );

    for( NodeConfig nodeConfig : meshConfig.getNodes() ) {
      String nodeStatus = isIn() ? activeMembers.contains( nodeConfig.getAddress() ) ? "in" : "out"
        : ( nodeConfig.getAddress().equals( myAddress ) ? "out" : "???" );
      if( myAddress.equals( nodeConfig.getAddress() ) ) {
        nodeStatus += " (this node)";
      }
      p( "Node '" + nodeConfig.getAddress().getName() + "' " + nodeConfig.getAddress().getInetSocketAddress() + " " + nodeStatus );
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
    p( "help .............. show this list if commands" );
    p( "ls ................ list nodes and show status" );
    p( "in / join ......... join the mesh" );
    p( "out / leave ....... leave the mesh" );
    p( "up ................ update mesh status (ping all nodes)" );
    p( "ping ID ........... ping node with ID" );
    p( "dm id mesg ........ send a direct message to a node ID" );
    p( "cast mesg ......... broadcast a message to all active nodes" );
    p( "services id ....... show services of node ID" );
    p( "quit .............. quit CLI" );
    p( "" );
  }

  public void waitForShutDown() {
    background.join();
  }

}
