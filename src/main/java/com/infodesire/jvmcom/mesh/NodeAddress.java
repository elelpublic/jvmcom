package com.infodesire.jvmcom.mesh;

import java.net.InetSocketAddress;

/**
 * Address of a node in a mesh.
 *
 */
public class NodeAddress implements Comparable<NodeAddress> {

  private final String id;
  private final InetSocketAddress inetSocketAddress;
  private final String addressString;

  /**
   * Create address
   *
   * @param id Unique id of node within mesh
   * @param inetSocketAddress Socket address
   *
   */
  public NodeAddress( String id, InetSocketAddress inetSocketAddress ) {
    this.id = id;
    this.inetSocketAddress = inetSocketAddress;
    addressString = inetSocketAddress.getHostString() + ":"
      + inetSocketAddress.getPort();
  }

  public InetSocketAddress getInetSocketAddress() {
    return inetSocketAddress;
  }

  @Override
  public int compareTo( NodeAddress o ) {
    return id.compareTo( o.id );
  }

  public String toString() {
    return id + "(" + addressString + ")";
  }

  /**
   * @return Unique id of node within mesh
   */
  public String getId() {
    return id;
  }

}
