package com.infodesire.jvmcom.mesh;

import java.net.InetSocketAddress;

/**
 * Address of a node in a mesh.
 *
 */
public class NodeAddress implements Comparable<NodeAddress> {

  private final String name;
  private final InetSocketAddress inetSocketAddress;
  private final String addressString;

  /**
   * Create address
   *
   * @param name Unique name of node within mesh
   * @param inetSocketAddress Socket address
   *
   */
  public NodeAddress( String name, InetSocketAddress inetSocketAddress ) {
    this.name = name;
    this.inetSocketAddress = inetSocketAddress;
    addressString = inetSocketAddress.getHostString() + ":"
      + inetSocketAddress.getPort();
  }

  public InetSocketAddress getInetSocketAddress() {
    return inetSocketAddress;
  }

  @Override
  public int compareTo( NodeAddress o ) {
    return name.compareTo( o.name );
  }

  public String toString() {
    return name + "(" + addressString + ")";
  }

  /**
   * @return Unique name of node within mesh
   */
  public String getName() {
    return name;
  }

}
