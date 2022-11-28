package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.clientserver.LineBufferHandler;
import org.pcollections.PSortedSet;
import org.pcollections.TreePSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A mesh node
 *
 */
public class Node {

  private static Logger logger = LoggerFactory.getLogger( "Mesh" );

  private final MeshConfig config;
  private final NodeAddress myAddress;
  private PSortedSet<NodeAddress> activeMembers = TreePSet.empty();
  private MeshSocket meshSocket;

  public Node( MeshConfig config, NodeAddress myAddress ) throws IOException {
    this.config = config;
    this.myAddress = myAddress;
    logger.info( "Create mesh node on " + myAddress );
  }

  /**
   * Join the mesh
   *
   */
  public void join() throws IOException {

    if( meshSocket != null ) {
      throw new RuntimeException( "Already joined." );
    }

    meshSocket = new MeshSocket( myAddress, new HandlerFactory() );

    updateActiveMembers();
    Set<NodeAddress> lostNodes = new HashSet<>();
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !nodeAddress.equals( myAddress ) ) {
        try {
          notifyJoin( nodeAddress );
        }
        catch( IOException ex ) {
          Mesh.logger.error( "Error sending join request to " + nodeAddress, ex );
          lostNodes.add( nodeAddress );
        }
      }
    }
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !lostNodes.contains( nodeAddress ) && !nodeAddress.equals( myAddress ) ) {
        for( NodeAddress lostNode : lostNodes ) {
          try {
            notifyLost( nodeAddress, lostNode );
          }
          catch( IOException ex ) {
            Mesh.logger.error( "Error sending lost request to " + nodeAddress, ex );
          }
        }
      }
    }
    for( NodeAddress lostNode : lostNodes ) {
      activeMembers = activeMembers.minus( lostNode );
    }

  }

  /**
   * Leave the mesh
   *
   */
  public void leave() {

    updateActiveMembers();
    Set<NodeAddress> lostNodes = new HashSet<>();
    for( NodeAddress nodeAddress : activeMembers ) {
      try {
        notifyLeave( nodeAddress );
      }
      catch( IOException ex ) {
        Mesh.logger.error( "Error sending leave request to " + nodeAddress, ex );
        lostNodes.add( nodeAddress );
      }
    }
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !lostNodes.contains( nodeAddress ) ) {
        for( NodeAddress lostNode : lostNodes ) {
          try {
            notifyLost( nodeAddress, lostNode );
          }
          catch( IOException ex ) {
            Mesh.logger.error( "Error sending lost request to " + nodeAddress, ex );
          }
        }
      }
    }
    for( NodeAddress lostNode : lostNodes ) {
      activeMembers = activeMembers.minus( lostNode );
    }

    try {
      meshSocket.close();
    }
    catch( InterruptedException ex ) {
      logger.error( "Error closing node connections." );
    }
    finally {
      meshSocket = null;
    }

  }

  /**
   * Notify a node of this node joining the mesh
   *
   * @param nodeAddress Node to be notified
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyJoin( NodeAddress nodeAddress ) throws IOException {
    LineBufferClient client = meshSocket.getClient( nodeAddress.getInetSocketAddress().getHostName(),
      nodeAddress.getInetSocketAddress().getPort() );
    client.send( "join " + myAddress.getId() );
  }

  /**
   * Notify a node of this node leaving the mesh
   *
   * @param nodeAddress Node to be notified
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyLeave( NodeAddress nodeAddress ) throws IOException {
    LineBufferClient client = meshSocket.getClient( nodeAddress.getInetSocketAddress().getHostName(),
      nodeAddress.getInetSocketAddress().getPort() );
    client.send( "leave " + myAddress.getId() );
  }

  /**
   * Notify a node of a node being lost from the mesh
   *
   * @param nodeAddress Node to be notified
   * @param lostNode The node to which the connection was lost
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyLost( NodeAddress nodeAddress, NodeAddress lostNode ) throws IOException {
    LineBufferClient client = meshSocket.getClient( nodeAddress.getInetSocketAddress().getHostName(),
      nodeAddress.getInetSocketAddress().getPort() );
    client.send( "lost " + lostNode.getId() );
  }

  /**
   * Send ping request
   *
   * @param nodeAddress Node to send ping request to
   * @return The id string returned by the target node (should be the id of that node)
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public String ping( NodeAddress nodeAddress ) throws IOException {
    LineBufferClient client = meshSocket.getClient( nodeAddress.getInetSocketAddress().getHostName(),
      nodeAddress.getInetSocketAddress().getPort() );
    StringBuffer reply = client.send( "ping" );
    return reply == null ? "" : reply.toString();
  }

  private void updateActiveMembers() {
    activeMembers = TreePSet.empty();
    if( hasJoined() ) {
      activeMembers = activeMembers.plus( myAddress );
    }
    for( NodeAddress nodeAddress : config.getMembers().values() ) {
      if( !nodeAddress.equals( myAddress ) ) {
        try {
          String replyId = ping( nodeAddress );
          if( replyId != null ) {
            if( !replyId.equals( nodeAddress.getId() ) ) {
              Mesh.logger.error( "Node " + nodeAddress + " replies with wrong id '" + replyId + "'. Will ignore this node." );
            }
            else {
              activeMembers = activeMembers.plus( nodeAddress );
            }
          }
          else {
            Mesh.logger.debug( "No reply from " + nodeAddress );
          }
        }
        catch( IOException ex ) {
          Mesh.logger.debug( "No reply from " + nodeAddress );
        }
      }
    }
  }

  public NodeAddress getAddress() {
    return myAddress;
  }

  public Collection<NodeAddress> getActiveMembers() {
    return activeMembers;
  }

  /**
   * Creates handler for incoming requests
   */
  class HandlerFactory implements Supplier<LineBufferHandler> {

    @Override
    public LineBufferHandler get() {
      return new RequestHandler();
    }

  }

  /**
   * Handles incoming meshing requests
   */
  class RequestHandler implements LineBufferHandler {

    @Override
    public HandlerReply process( String line ) {
      if( line != null ) {
        if( line.equals( "ping" ) ) {
          return handlePing();
        }
        else if( line.equals( "active" ) ) {
          return handleActive();
        }
        else return new HandlerReply( MeshError.UNKNOWN_COMMAND );
      }
      else { // null request
        return new HandlerReply( MeshError.NULL_COMMAND );
      }
    }

  }

  /**
   * Handle "ping" request
   *
   * @return Id of this node
   *
   */
  protected HandlerReply handlePing() {
    return new HandlerReply( myAddress.getId() );
  }

  /**
   * Handle "active" request
   *
   * @return List of ids of active nodes
   *
   */
  protected HandlerReply handleActive() {

    String nodeList = activeMembers
      .stream()
      .map( nodeAddress -> nodeAddress.getId() )
      .collect( Collectors.joining( " " ) );

    return new HandlerReply( nodeList );

  }

  public boolean hasJoined() {
    return meshSocket != null;
  }

}
