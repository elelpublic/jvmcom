package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.pool.SocketPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Container for a set of nodes in a mesh. Not synchronized.
 *
 */
public class Mesh {

  public static Logger logger = LoggerFactory.getLogger( "Server" );
  private final MeshConfig config;
  private final SocketPool socketPool;
  private Map<String, Node> nodes = new HashMap<>();

  public Mesh( MeshConfig config, SocketPool socketPool ) {
    this.config = config;
    this.socketPool = socketPool;
  }

  public MeshConfig getConfig() {
    return config;
  }

  /**
   * @param nodeId Id of node
   * @return Address of node or null if no such node is registered
   *
   */
  public NodeAddress getAddress( String nodeId ) {
    return config.getMembers().get( nodeId );
  }

  /**
   * Get node, create if new
   *
   * @param nodeId Id of node
   * @return Node
   *
   */
  public Node get( String nodeId ) {
    NodeAddress nodeAddress = getAddress( nodeId );
    if( nodeAddress == null ) {
      throw new RuntimeException( "No such node id is registered: " + nodeId );
    }
    Node node = nodes.get( nodeId );
    if( node == null ) {
      node = new Node( this, nodeAddress, socketPool );
      nodes.put( nodeId, node );
    }
    return node;
  }

}
