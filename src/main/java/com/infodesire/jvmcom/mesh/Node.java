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
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.infodesire.jvmcom.ConfigProperties.LEAVE_TIMEOUT_MS;
import static com.infodesire.jvmcom.mesh.MessageType.BROADCAST;
import static com.infodesire.jvmcom.mesh.MessageType.DM;

/**
 * A mesh node
 *
 */
public class Node {

  private static Logger logger = LoggerFactory.getLogger( "Mesh" );

  protected final MeshConfig config;
  final NodeAddress myAddress;
  final SocketPool socketPool;
  protected final Mesh mesh;
  PSortedSet<NodeAddress> activeMembers = TreePSet.empty();
  private MeshSocket meshSocket;
  private Queue<Message> dms = new LinkedBlockingQueue<>();
  protected Queue<Message> broadcasts = new LinkedBlockingQueue<>();

  public Node( Mesh mesh, NodeAddress myAddress, SocketPool socketPool ) {
    this.mesh = mesh;
    this.config = mesh.getConfig();
    this.myAddress = myAddress;
    this.socketPool = socketPool;
    logger.info( "Create mesh node on " + myAddress );
  }

  /**
   * Join the mesh
   *
   */
  public void in() throws IOException {

    if( meshSocket != null ) {
      throw new RuntimeException( "Already joined." );
    }

    meshSocket = new MeshSocket( myAddress, new HandlerFactory( mesh ) );
    
    updateActiveMembers();
    Set<NodeAddress> lostNodes = new HashSet<>();
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !nodeAddress.equals( myAddress ) ) {
        try {
          LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
          notifyIn( client );
        }
        catch( Exception ex ) {
          Mesh.logger.error( "Error sending in request to " + nodeAddress, ex );
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
  public void out( long timeoutMs ) {

    updateActiveMembers();
    Set<NodeAddress> lostNodes = new HashSet<>();
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !nodeAddress.equals( myAddress ) ) {
        try {
          LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
          notifyOut( client );
        }
        catch( Exception ex ) {
          Mesh.logger.error( "Error sending out request to " + nodeAddress, ex );
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
  public void notifyIn( LineBufferClient client ) throws IOException {
    client.send( "in " + myAddress.getId() );
  }

  /**
   * Notify a node of this node leaving the mesh
   *
   * @param client Client connected to the node to be notified
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyOut( LineBufferClient client ) throws IOException {
    client.send( "out " + myAddress.getId() );
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

  /**
   * Send direct request to another node
   *
   * @param client Client connected to the node to sent message to
   * @param message Message to be sent
   * @return The id string returned by the target node (should be the id of that node)
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public String dm( LineBufferClient client, String message ) throws IOException {
    StringBuffer reply = client.send( "dm " + message );
    return reply == null ? "" : reply.toString();
  }

  /**
   * Send broadcast message request to all active nodes
   *
   * @param message Message to be sent
   * @return The id string returned by the target node (should be the id of that node)
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public String cast( String message ) throws IOException {
    StringJoiner replies = new StringJoiner( "\n" );
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !nodeAddress.equals( myAddress ) ) {
        try {
          LineBufferClient client = new LineBufferClient( socketPool.getSocket( nodeAddress ) );
          StringBuffer reply = client.send( "cast " + message );
          replies.add( nodeAddress.getId() + ": " + ( reply == null ? "" : reply.toString() ) );
        }
        catch( Exception ex ) {
          ex.printStackTrace();
        }
      }
    }
    return replies.toString();
  }

  protected void updateActiveMembers() {
    activeMembers = TreePSet.empty();
    if( isIn() ) {
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
    out( timeoutMs );
  }

  /**
   * Creates handler for incoming requests
   */
  class HandlerFactory implements Supplier<LineBufferHandler> {

    private final Mesh mesh;

    public HandlerFactory( Mesh mesh ) {
      this.mesh = mesh;
    }

    @Override
    public LineBufferHandler get() {
      return new RequestHandler( mesh );
    }

  }

  /**
   * Handles incoming meshing requests
   */
  class RequestHandler implements LineBufferHandler {

    private final Mesh mesh;
    private InetSocketAddress senderAddress;

    public RequestHandler( Mesh mesh ) {
      this.mesh = mesh;
    }

    @Override
    public void setSender( InetSocketAddress senderAddress ) {
      this.senderAddress = senderAddress;
    }

    @Override
    public HandlerReply process( String line ) {
      if( line != null ) {
        if( line.equals( "ping" ) ) {
          return handlePing();
        }
        else if( line.equals( "active" ) ) {
          return handleActive();
        }
        else if( line.startsWith( "in " ) ) {
          return handleIn( line.substring( 3 ) );
        }
        else if( line.startsWith( "out " ) ) {
          return handleOut( line.substring( 4 ) );
        }
        else if( line.startsWith( "dm " ) ) {
          return handleDm( "" + senderAddress, line.substring( 3 ) );
        }
        else if( line.startsWith( "cast " ) ) {
          return handleCast( "" + senderAddress, line.substring( 5 ) );
        }
        else return new HandlerReply( MeshError.UNKNOWN_COMMAND );
      }
      else { // null request
        return new HandlerReply( MeshError.NULL_COMMAND );
      }
    }


  }

  /**
   * Handle direct message
   *
   * @param sender Name of sender
   * @param message Message
   * @return Reply
   */
  private HandlerReply handleDm( String sender, String message ) {
    Message messageObject = new Message( DM, sender, message );
    logger.info( "Received: " + messageObject );
    dms.offer( new Message( DM, sender, message ) );
    return new HandlerReply( "OK" );
  }

  /**
   * Handle broadcast message
   *
   * @param sender Name of sender
   * @param message Message
   * @return Reply
   */
  private HandlerReply handleCast( String sender, String message ) {
    Message messageObject = new Message( BROADCAST, sender, message );
    logger.info( "Received: " + messageObject );
    broadcasts.offer( messageObject );
    return new HandlerReply( "OK" );
  }

  private HandlerReply handleIn( String nodeId ) {
    NodeAddress nodeAddress = config.getMembers().get( nodeId );
    if( nodeAddress != null ) {
      activeMembers = activeMembers.plus( nodeAddress );
      return handleActive();
    }
    else {
      return new HandlerReply( MeshError.UNKNOWN_NODE_ID );
    }
  }

  private HandlerReply handleOut( String nodeId ) {
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

  public boolean isIn() {
    return meshSocket != null;
  }

  public void finalize() {
    out( LEAVE_TIMEOUT_MS );
  }

}
