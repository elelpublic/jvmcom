package com.infodesire.jvmcom.mesh;

/**
 * Configuration of a node service
 *
 */
public class ServiceConfig {

  private String name;

  private int port;

  public ServiceConfig( String name, int port ) {
    this.name = name;
    this.port = port;
  }

  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public int getPort() {
    return port;
  }

  public void setPort( int port ) {
    this.port = port;
  }

}
