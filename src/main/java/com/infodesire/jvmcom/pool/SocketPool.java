package com.infodesire.jvmcom.pool;

import com.infodesire.jvmcom.mesh.NodeAddress;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.net.InetSocketAddress;
import java.net.Socket;


/**
 * Pool of connected sockets (because connect(...) is expensive)
 */
public class SocketPool {


  private KeyedObjectPool<NodeAddress, Socket> pool;

  public SocketPool() {
    SocketFactory factory = new SocketFactory();
    GenericKeyedObjectPoolConfig<Socket> config = new GenericKeyedObjectPoolConfig();
    pool = new GenericKeyedObjectPool<NodeAddress, Socket>( factory, config );
  }


  public Socket getSocket( NodeAddress nodeAddress ) throws Exception {
    return pool.borrowObject( nodeAddress );
  }

  public void returnSocket( NodeAddress nodeAddress, Socket socket ) throws Exception {
    pool.returnObject( nodeAddress, socket );
  }


  class SocketFactory implements KeyedPooledObjectFactory<NodeAddress, Socket> {

    @Override
    public void activateObject( NodeAddress nodeAddress, PooledObject<Socket> pooledObject ) throws Exception {
      Socket socket = pooledObject.getObject();
      if( !socket.isConnected() ) {
        socket.connect( new InetSocketAddress( nodeAddress.getInetSocketAddress().getHostName(), nodeAddress.getInetSocketAddress().getPort() ) );
      }
    }

    @Override
    public void destroyObject( NodeAddress nodeAddress, PooledObject<Socket> pooledObject ) throws Exception {
      Socket socket = pooledObject.getObject();
      if( socket != null && socket.isConnected() ) {
        socket.close();
      }
    }

    @Override
    public PooledObject<Socket> makeObject( NodeAddress nodeAddress ) throws Exception {
      Socket socket = new Socket( nodeAddress.getInetSocketAddress().getHostName(), nodeAddress.getInetSocketAddress().getPort() );
      return new DefaultPooledObject( socket );
    }

    @Override
    public void passivateObject( NodeAddress nodeAddress, PooledObject<Socket> pooledObject ) throws Exception {
    }

    @Override
    public boolean validateObject( NodeAddress nodeAddress, PooledObject<Socket> pooledObject ) {
      return true;
    }

  }


}
