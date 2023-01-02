package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.clientserver.LineBufferHandler;
import com.infodesire.jvmcom.pool.SocketPool;
import com.infodesire.jvmcom.services.Service;
import com.infodesire.jvmcom.services.ServiceFactory;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PSortedSet;
import org.pcollections.TreePSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.infodesire.jvmcom.ConfigProperties.LEAVE_TIMEOUT_MS;

/**
 * A mesh node
 *
 */
public class Node {

  private static final Logger logger = LoggerFactory.getLogger( "Mesh" );

  protected final MeshConfig meshConfig;
  final SocketPool socketPool;
  protected final Mesh mesh;
  protected final NodeConfig config;
  protected final NodeAddress myAddress;
  protected final String myName;
  private final Supplier<MessageHandler> messageHandlerFactory;
  private final ServiceFactory serviceFactory;
  protected PSortedSet<NodeAddress> activeMembers = TreePSet.empty();
  private MeshSocket meshSocket;
  private PMap<String, Service> services = HashTreePMap.empty();

  public Node( Mesh mesh, NodeConfig config, SocketPool socketPool, Supplier<MessageHandler> messageHandlerFactory
          , ServiceFactory serviceFactory ) {
    this.mesh = mesh;
    this.meshConfig = mesh.getConfig();
    this.config = config;
    myAddress = config.getAddress();
    myName = myAddress.getName();
    this.socketPool = socketPool;
    this.messageHandlerFactory = messageHandlerFactory;
    this.serviceFactory = serviceFactory;
    logger.info( "Create mesh node on " + myAddress );
    if( config.getAutojoin() ) {
      try {
        join();
      }
      catch( IOException ex ) {
        throw new RuntimeException( "Error on autojoin", ex );
      }
    }
  }

  /**
   * Join the mesh
   *
   */
  public void join() throws IOException {

    if( meshSocket != null ) {
      throw new RuntimeException( "Already joined." );
    }

    meshSocket = new MeshSocket( myAddress, new HandlerFactory( mesh, messageHandlerFactory ) );
    
    updateActiveMembers();
    Set<NodeAddress> lostNodes = new HashSet<>();
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !nodeAddress.equals( myAddress ) ) {
        try( LineBufferClient client = new LineBufferClient( socketPool, nodeAddress.getInetSocketAddress() ) ) {
          notifyJoin( client );
        }
        catch( Exception ex ) {
          Mesh.logger.error( "Error sending in request to " + nodeAddress, ex );
          lostNodes.add( nodeAddress );
        }
      }
    }

    startServices();

    notifyLost( lostNodes );

  }

  private void startServices() {

    for( String serviceName : config.getServices() ) {

      ServiceConfig serviceConfig = config.getService( serviceName );
      Service service = serviceFactory.create( serviceConfig.getPort(), serviceName );
      
      if( service != null ) {
        services = services.plus( serviceName, service );
      }
      else {
        logger.error( "Unknown service: " + serviceName );
      }

    }

  }

  private void stopServices( long timeoutMs ) {

    for( Service service : services.values() ) {
      try {
        service.stop( timeoutMs );
      }
      catch( InterruptedException ex ) {
        logger.error( "Error stopping service " + service.getName() + " on node " + myAddress );
      }
    }

  }


  private void notifyLost( Set<NodeAddress> lostNodes ) {

    for( NodeAddress lostNode : lostNodes ) {
      activeMembers = activeMembers.minus( lostNode );
    }
    for( NodeAddress nodeAddress : activeMembers ) {
      if( !lostNodes.contains( nodeAddress ) && !nodeAddress.equals( myAddress ) ) {
        for( NodeAddress lostNode : lostNodes ) {
          try( LineBufferClient client = new LineBufferClient( socketPool, nodeAddress )  ) {
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
        try( LineBufferClient client = new LineBufferClient( socketPool, nodeAddress ) )  {
          notifyLeave( client );
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

    stopServices( timeoutMs );

  }

  /**
   * Notify a node of this node joining the mesh
   *
   * @param client Client connected to the node to be notified
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyJoin( LineBufferClient client ) throws IOException {
    client.send( "join " + myName );
  }

  /**
   * Notify a node of this node leaving the mesh
   *
   * @param client Client connected to the node to be notified
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public void notifyLeave( LineBufferClient client ) throws IOException {
    client.send( "leave " + myName );
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
    client.send( "lost " + lostNode.getName() );
  }

  /**
   * Send ping request
   *
   * @param client Client connected to the node to be pinged
   * @return The id string returned by the target node (should be the id of that node)
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public CharSequence ping( LineBufferClient client ) throws IOException {
    CharSequence reply = client.send( "ping" );
    return reply == null ? "" : reply;
  }

  /**
   * Ask a node for its list if services
   *
   * @param client Client connected to the node to be asked
   * @return List of spaces separated service:port entries showing the service name and port offered
   * @throws IOException when node could not be notified for network reasons
   *
   */
  public CharSequence services( LineBufferClient client ) throws IOException {
    CharSequence reply = client.send( "services" );
    return reply == null ? "" : reply;
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
  public CharSequence dm( LineBufferClient client, String message ) throws IOException {
    CharSequence reply = client.send( "dm " + message );
    return reply == null ? "" : reply;
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
        try( LineBufferClient client = new LineBufferClient( socketPool, nodeAddress ) ) {
          CharSequence reply = client.send( "cast " + message );
          replies.add( nodeAddress.getName() + ": " + ( reply == null ? "" : reply.toString() ) );
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
    for( NodeConfig nodeConfig : meshConfig.getNodes() ) {
      NodeAddress nodeAddress = nodeConfig.getAddress();
      if( !nodeAddress.equals( myAddress ) ) {
        try( LineBufferClient client = new LineBufferClient( socketPool, nodeAddress ) ) {
          String replyId = "" + ping( client );
          if( !replyId.equals( nodeAddress.getName() ) ) {
            Mesh.logger.error( "Node " + nodeAddress + " replies with wrong id '" + replyId + "'. Will ignore this node." );
          }
          else {
            activeMembers = activeMembers.plus( nodeAddress );
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

    private final Mesh mesh;
    private final Supplier<MessageHandler> messageHandlerFactory;

    public HandlerFactory( Mesh mesh, Supplier<MessageHandler> messageHandlerFactory ) {
      this.mesh = mesh;
      this.messageHandlerFactory = messageHandlerFactory;
    }

    @Override
    public LineBufferHandler get() {
      return new RequestHandler( messageHandlerFactory.get() );
    }

  }

  /**
   * Handles incoming meshing requests
   */
  class RequestHandler implements LineBufferHandler {

    private final MessageHandler messageHandler;
    private InetSocketAddress senderAddress;

    public RequestHandler( MessageHandler messageHandler ) {
      this.messageHandler = messageHandler;
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
        else if( line.startsWith( "join " ) ) {
          return handleJoin( line.substring( 5 ) );
        }
        else if( line.startsWith( "leave " ) ) {
          return handleLeave( line.substring( 6 ) );
        }
        else if( line.startsWith( "dm " ) ) {
          return messageHandler.handleDm( "" + senderAddress, line.substring( 3 ) );
        }
        else if( line.startsWith( "cast " ) ) {
          return messageHandler.handleCast( "" + senderAddress, line.substring( 5 ) );
        }
        else if( line.equals( "services" ) ) {
          return handleServices();
        }
        else return new HandlerReply( MeshError.UNKNOWN_COMMAND );
      }
      else { // null request
        return new HandlerReply( MeshError.NULL_COMMAND );
      }
    }


  }

  private HandlerReply handleJoin( String nodeId ) {
    NodeConfig nodeConfig = meshConfig.getNodeConfig( nodeId );
    if( nodeConfig != null ) {
      activeMembers = activeMembers.plus( nodeConfig.getAddress() );
      return handleActive();
    }
    else {
      return new HandlerReply( MeshError.UNKNOWN_NODE_ID );
    }
  }

  private HandlerReply handleLeave( String nodeName ) {
    NodeConfig nodeConfig = meshConfig.getNodeConfig( nodeName );
    if( nodeConfig != null ) {
      activeMembers = activeMembers.minus( nodeConfig.getAddress() );
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
    return new HandlerReply( myAddress.getName() );
  }

  /**
   * Handle "ping" request
   *
   * @return Id of this node
   *
   */
  protected HandlerReply handleServices() {
    StringJoiner reply = new StringJoiner( " " );
    for( Service service : services.values() ) {
      reply.add( service.getName() + ":" + service.getPort() );
    }
    return new HandlerReply( myAddress.getName() + ": " + reply );
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
      .map( NodeAddress::getName )
      .collect( Collectors.joining( " " ) );

    return new HandlerReply( nodeList );

  }

  public boolean isIn() {
    return meshSocket != null;
  }

  protected void finalize() {
    leave( LEAVE_TIMEOUT_MS );
  }

  public CharSequence getStatusMessage() {

    StringBuilder result = new StringBuilder();

    p( result, "----------------------------------------------------------" );
    p( result, "Mesh name    : " + meshConfig.name );
    p( result, "Node name    : " + myAddress.getName() );
    p( result, "Node address : " + myAddress.getInetSocketAddress() );
    p( result, "Node joined  : " + ( isIn() ? "yes" : "no" ) );
    p( result, "----------------------------------------------------------" );

    for( NodeConfig nodeConfig : meshConfig.getNodes() ) {
      String nodeStatus = isIn() ? activeMembers.contains( nodeConfig.getAddress() ) ? "in" : "out"
        : ( nodeConfig.getAddress().equals( myAddress ) ? "out" : "???" );
      if( myAddress.equals( nodeConfig.getAddress() ) ) {
        nodeStatus += " (this node)";
      }
      p( result, "Node '" + nodeConfig.getAddress().getName() + "' " + nodeConfig.getAddress().getInetSocketAddress() + " " + nodeStatus );
    }
    p( result, "----------------------------------------------------------" );

    return result;

  }

  private void p( StringBuilder result, CharSequence line ) {
    result.append( line );
    result.append( "\n" );
  }

  public String toString() {
    return "" + myAddress;
  }

}
