package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.pool.SocketPool;
import com.infodesire.jvmcom.services.ServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.infodesire.jvmcom.ConfigProperties.LEAVE_TIMEOUT_MS;

/**
 * A mesh node with a command line interface
 *
 */
public class CliNode extends Node implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger( "Mesh" );

  private final CompletableFuture<Void> background;

  public CliNode( Mesh mesh, NodeConfig config, SocketPool socketPool, Supplier<MessageHandler> messageHandlerFactory,
                  ServiceFactory serviceFactory ) {
    super( mesh, config, socketPool, messageHandlerFactory, serviceFactory );
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
        catch( IOException ex ) {
          ex.printStackTrace();
        }
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
          else if( input.startsWith( "ping" ) ) {
            String nodeName = input.substring( 4 );
            ping( nodeName );
          }
          else if( input.startsWith( "dm " ) ) {
            String nodeName = input.substring( 3 );
            int sep = nodeName.indexOf( " " );
            String msg = nodeName.substring( sep + 1 );
            nodeName = nodeName.substring( 0, sep );
            dm( nodeName, msg );
          }
          else if( input.startsWith( "cast" ) ) {
            String message = input.substring( 4 );
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
          else if( input.startsWith( "services" ) ) {
            String nodeName = input.substring( 8 );
            services( nodeName );
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

  private void services( String nodeName ) {
    NodeConfig nodeConfig = getNodeConfig( nodeName );
    if( nodeConfig != null ) {
      try( LineBufferClient client = new LineBufferClient( socketPool, nodeConfig.getAddress() ) ) {
        p( "Reply: " + services( client ) );
      }
      catch( Exception ex ) {
        p( "Error sending services request to " + nodeConfig.getAddress() );
        ex.printStackTrace();
      }
    }
  }

  private NodeConfig getNodeConfig( String nodeName ) {
    if( nodeName == null || nodeName.trim().length() == 0 ) {
      usage( "Missing node name" );
    }
    else {
      nodeName = nodeName.trim();
      NodeConfig nodeConfig = meshConfig.getNodeConfig( nodeName );
      if( nodeConfig == null ) {
        usage( "Node '" + nodeName + "' not found." );
      }
      else {
        return nodeConfig;
      }
    }
    return null;
  }

  private void ping( String nodeName ) {
    NodeConfig nodeConfig = getNodeConfig( nodeName );
    if( nodeConfig != null ) {
      try ( LineBufferClient client = new LineBufferClient( socketPool, nodeConfig.getAddress() ) ) {
        CharSequence reply = ping( client );
        p( "Reply: " + reply );
      }
      catch( Exception ex ) {
        ex.printStackTrace();
      }
    }
  }

  private void dm( String nodeName, String message ) {
    NodeConfig nodeConfig = meshConfig.getNodeConfig( nodeName );
    try ( LineBufferClient client = new LineBufferClient( socketPool, nodeConfig.getAddress() ) ) {
      CharSequence reply = dm( client, message );
      p( "Reply: " + reply );
    }
    catch( Exception ex ) {
      ex.printStackTrace();
    }
  }

  private void printStatus() {

    p( getStatusMessage() );

  }

  private static void p( CharSequence line ) {
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
