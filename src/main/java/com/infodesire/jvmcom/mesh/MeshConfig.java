package com.infodesire.jvmcom.mesh;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class MeshConfig {

  public String name;
  private SortedMap<String, NodeAddress> members = new TreeMap<>();

  public static MeshConfig loadFromProperties( Properties props ) {

    MeshConfig config = new MeshConfig();

    config.name = props.getProperty( "name" );

    for( String nodeName : props.getProperty( "nodes", "" ).split( "," ) ) {
      nodeName = nodeName.trim();
      String hostName = "localhost";
      int port = Integer.parseInt( props.getProperty( "nodes." + nodeName + ".port", "0" ) );
      InetSocketAddress inetAddress = new InetSocketAddress( hostName, port );
      NodeAddress nodeAddress = new NodeAddress( nodeName, inetAddress );
      config.members.put( nodeName, nodeAddress );
    }

    return config;

  }

  public SortedMap<String, NodeAddress> getMembers() {
    return members;
  }

}
