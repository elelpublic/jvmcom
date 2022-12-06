package com.infodesire.jvmcom.mesh;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration of a mesh node
 *
 */
public class NodeConfig implements Comparable<NodeConfig> {

  private NodeAddress address;

  private Map<String, ServiceConfig> services = new HashMap<>();

  private boolean autojoin = false;

  public NodeConfig( NodeAddress address ) {
    this.address = address;
  }

  public NodeAddress getAddress() {
    return address;
  }

  public void setAddress( NodeAddress address ) {
    this.address = address;
  }

  public Collection<String> getServices() {
    return services.keySet();
  }

  public ServiceConfig getService( String name ) {
    return services.get( name );
  }

  public void addService( ServiceConfig serviceConfig ) {
    services.put( serviceConfig.getName(), serviceConfig );
  }

  public boolean getAutojoin() {
    return autojoin;
  }

  public void setAutojoin( boolean autojoin ) {
    this.autojoin = autojoin;
  }

  @Override
  public int compareTo( NodeConfig o ) {
    return address.compareTo( o.address );
  }

}
