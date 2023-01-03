package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

public class MeshConfig {

  public String name;
  private final SortedMap<String, NodeConfig> nodes = new TreeMap<>();

  public static MeshConfig loadFromProperties( Properties props ) {

    MeshConfig config = new MeshConfig();

    config.name = props.getProperty( "name" );

    for( String nodeName : props.getProperty( "nodes", "" ).split( "," ) ) {
      nodeName = nodeName.trim();
      String hostName = "localhost";
      int port = Integer.parseInt( props.getProperty( "nodes." + nodeName + ".port", "0" ) );
      InetSocketAddress inetAddress = new InetSocketAddress( hostName, port );
      NodeAddress nodeAddress = new NodeAddress( nodeName, inetAddress );
      NodeConfig nodeConfig = new NodeConfig( nodeAddress );
      nodeConfig.setAutojoin( Boolean.parseBoolean( props.getProperty( "nodes." + nodeName + ".autojoin", "false" ) ) );
      String serviceNames = props.getProperty( "nodes." + nodeName + ".services", "" );
      if( !StringUtils.isEmpty( serviceNames ) ) {
        for( String serviceName : serviceNames.split( "," ) ) {
          serviceName = serviceName.trim();
          int servicePort = Integer.parseInt( props.getProperty( "nodes." + nodeName + ".service." + serviceName + ".port", "0" ) );
          ServiceConfig serviceConfig = new ServiceConfig( serviceName, servicePort );
          nodeConfig.addService( serviceConfig );
        }
      }
      config.nodes.put( nodeName, nodeConfig );
    }

    return config;

  }

  public static MeshConfig loadFromFile( File file ) throws IOException {
    Properties properties = new Properties();
    properties.load( new FileReader( file ) );
    return loadFromProperties( properties );
  }

//  public SortedMap<String, NodeConfig> getNodes() {
//    return nodes;
//  }

  /**
   * @param name Name of node
   * @return Configuration of node
   *
   */
  public NodeConfig getNodeConfig( String name ) {
    return nodes.get( name );
  }

  public Collection<NodeConfig> getNodes() {
    return nodes.values();
  }

}
