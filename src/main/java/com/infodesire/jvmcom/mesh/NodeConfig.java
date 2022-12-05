package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.ServerConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration of a mesh node
 *
 */
public class NodeConfig {

  private NodeAddress address;

  private Map<String, ServiceConfig> services = new HashMap<>();

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

}
