package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.clientserver.LineBufferHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * A mesh node
 *
 */
public class Node {

  private final MeshConfig config;
  private final NodeAddress myAddress;
  private final SortedSet<NodeAddress> activeMembers = new TreeSet<>();
  private boolean hasJoined = false;
  private final MeshSocket meshSocket;

  public Node( MeshConfig config, NodeAddress myAddress ) throws IOException {
    this.config = config;
    this.myAddress = myAddress;
    meshSocket = new MeshSocket( myAddress, new HandlerFactory() );
  }

  /**
   * Join the mesh
   *
   */
  public void join() {

    updateActiveMembers();
    Set<NodeAddress> lostNodes = new HashSet<>();
    for( NodeAddress nodeAddress : activeMembers ) {
      try {
        notifyJoin( nodeAddress );
      }
      catch( IOException ex ) {
        Mesh.logger.error( "Error sending join request to " + nodeAddress, ex );
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
      activeMembers.remove( lostNode );
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
    activeMembers.clear();
    for( NodeAddress nodeAddress : config.getMembers().values() ) {
      try {
        String replyId = ping( nodeAddress );
        if( replyId != null ) {
          if( !replyId.equals( nodeAddress.getId() ) ) {
            Mesh.logger.error( "Node " + nodeAddress + " replies with wrong id '" + replyId + "'. Will ignore this node." );
          }
          else {
            activeMembers.add( nodeAddress );
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
          return new HandlerReply( myAddress.getId() );
        }
        else return new HandlerReply( MeshError.UNKNOWN_COMMAND );
      }
      else { // null request
        return new HandlerReply( MeshError.NULL_COMMAND );
      }
    }

  }


}
