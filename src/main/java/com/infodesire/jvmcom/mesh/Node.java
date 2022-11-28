package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.clientserver.LineBufferHandler;
import com.infodesire.jvmcom.pool.SocketPool;
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
  private final SocketPool socketPool;
  private PSortedSet<NodeAddress> activeMembers = TreePSet.empty();
  private MeshSocket meshSocket;

  public Node( MeshConfig config, NodeAddress myAddress, SocketPool socketPool ) {
    this.config = config;
    this.myAddress = myAddress;
    this.socketPool = socketPool;
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
          LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
          notifyJoin( client );
        }
        catch( Exception ex ) {
          Mesh.logger.error( "Error sending join request to " + nodeAddress, ex );
          lostNodes.add( nodeAddress );
        }
      }
    }

    notifyLost( lostNodes );

  }

  private void notifyLost( Set<NodeAddress> lostNodes ) {

    for( NodeAddress lostNode : lostNodes ) {
      activeMembers = activeMembers.minus( lostNode );
    }
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !lostNodes.contains( nodeAddress ) && !nodeAddress.equals( myAddress ) ) {
        for( NodeAddress lostNode : lostNodes ) {
          try {
            LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
            notifyLost( client, lostNode );
          }
          catch( Exception ex ) {
            Mesh.logger.error( "Error sending lost request to " + nodeAddress, ex );
          }
        }
      }
    }

  }

  /**
   * Leave the mesh
   *
   * @param timeoutMs Number of ms to wait for orderly leave
   *
   */
  public void leave( long timeoutMs ) {

    updateActiveMembers();
    Set<NodeAddress> lostNodes = new HashSet<>();
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !nodeAddress.equals( myAddress ) ) {
        try {
          LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
          notifyLeave( client );
        }
        catch( Exception ex ) {
          Mesh.logger.error( "Error sending leave request to " + nodeAddress, ex );
          lostNodes.add( nodeAddress );
        }
      }
    }

    notifyLost( lostNodes );

    try {
      meshSocket.close( timeoutMs );
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
   * @param client Client connected to the node to be notified
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyJoin( LineBufferClient client ) throws IOException {
    client.send( "join " + myAddress.getId() );
  }

  /**
   * Notify a node of this node leaving the mesh
   *
   * @param client Client connected to the node to be notified
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyLeave( LineBufferClient client ) throws IOException {
    client.send( "leave " + myAddress.getId() );
  }

  /**
   * Notify a node of a node being lost from the mesh
   *
   * @param client Client connected to the node to be notified
   * @param lostNode The node to which the connection was lost
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyLost( LineBufferClient client, NodeAddress lostNode ) throws IOException {
    client.send( "lost " + lostNode.getId() );
  }

  /**
   * Send ping request
   *
   * @param client Client connected to the node to be pinged
   * @return The id string returned by the target node (should be the id of that node)
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public String ping( LineBufferClient client ) throws IOException {
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
          LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
          String replyId = ping( client );
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
        catch( Exception ex ) {
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
   * Leave mesh and shut down node
   *
   * @param timeoutMs Number of ms to wait for orderly leave
   *
   */
  public void shutDown( long timeoutMs ) {
    leave( timeoutMs );
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
        else if( line.startsWith( "join" ) ) {
          return handleJoin( line.substring( 5 ) );
        }
        else if( line.startsWith( "leave" ) ) {
          return handleLeave( line.substring( 6 ) );
        }
        else return new HandlerReply( MeshError.UNKNOWN_COMMAND );
      }
      else { // null request
        return new HandlerReply( MeshError.NULL_COMMAND );
      }
    }


  }

  private HandlerReply handleJoin( String nodeId ) {
    NodeAddress nodeAddress = config.getMembers().get( nodeId );
    if( nodeAddress != null ) {
      activeMembers = activeMembers.plus( nodeAddress );
      return handleActive();
    }
    else {
      return new HandlerReply( MeshError.UNKNOWN_NODE_ID );
    }
  }

  private HandlerReply handleLeave( String nodeId ) {
    NodeAddress nodeAddress = config.getMembers().get( nodeId );
    if( nodeAddress != null ) {
      activeMembers = activeMembers.minus( nodeAddress );
      return handleActive();
    }
    else {
      return new HandlerReply( MeshError.UNKNOWN_NODE_ID );
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
