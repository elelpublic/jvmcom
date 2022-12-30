package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.LineBufferClient;
import com.infodesire.jvmcom.pool.SocketPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Container for a set of nodes in a mesh. Not synchronized.
 *
 */
public class Mesh {

  public static Logger logger = LoggerFactory.getLogger( "Server" );
  private final MeshConfig config;
  private final SocketPool socketPool;
  private final Supplier<MessageHandler> messageHandlerFactory;
  private final Map<String, Node> nodes = new HashMap<>();

  public Mesh( MeshConfig config, SocketPool socketPool, Supplier<MessageHandler> messageHandlerFactory ) {
    this.config = config;
    this.socketPool = socketPool;
    this.messageHandlerFactory = messageHandlerFactory;
  }

  public MeshConfig getConfig() {
    return config;
  }

  /**
   * Get node, create if new
   *
   * @param nodeId Id of node
   * @return Node
   *
   */
  public Node get( String nodeId ) {
    NodeConfig nodeConfig = config.getNodeConfig( nodeId );
    if( nodeConfig == null ) {
      throw new RuntimeException( "No such node id is registered: " + nodeId );
    }
    Node node = nodes.get( nodeId );
    if( node == null ) {
      node = new Node( this, nodeConfig, socketPool, messageHandlerFactory );
      nodes.put( nodeId, node );
    }
    return node;
  }


  /**
   * @param nodeId Id of node to communicate with
   * @return Client to communicate with remote node
   *
   */
  public LineBufferClient getClient( String nodeId ) throws Exception {
    NodeConfig nodeConfig = config.getNodeConfig( nodeId );
    if( nodeConfig == null ) {
      throw new RuntimeException( "No node found for node id " + nodeId );
    }
    return new LineBufferClient( socketPool.getSocket( nodeConfig.getAddress().getInetSocketAddress() ) );
  }


}
