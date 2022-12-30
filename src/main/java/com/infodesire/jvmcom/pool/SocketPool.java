package com.infodesire.jvmcom.pool;

import com.infodesire.jvmcom.mesh.NodeAddress;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;


/**
 * Pool of connected sockets (because connect(...) is expensive)
 *
 * See here: https://commons.apache.org/proper/commons-pool/apidocs/org/apache/commons/pool2/PooledObjectFactory.html
 *
 * makeObject() is called whenever a new instance is needed.
 * activateObject(org.apache.commons.pool2.PooledObject<T>) is invoked on every instance that has been passivated before it is borrowed from the pool.
 * validateObject(org.apache.commons.pool2.PooledObject<T>) may be invoked on activated instances to make sure they can be borrowed from the pool. validateObject(org.apache.commons.pool2.PooledObject<T>) may also be used to test an instance being returned to the pool before it is passivated. It will only be invoked on an activated instance.
 * passivateObject(org.apache.commons.pool2.PooledObject<T>) is invoked on every instance when it is returned to the pool.
 * destroyObject(org.apache.commons.pool2.PooledObject<T>) is invoked on every instance when it is being "dropped" from the pool (whether due to the response from validateObject(org.apache.commons.pool2.PooledObject<T>), or for reasons specific to the pool implementation.) There is no guarantee that the instance being destroyed will be considered active, passive or in a generally consistent state.
 *
 *
 */
public class SocketPool {

  private static Logger logger = LoggerFactory.getLogger( "SocketPool" );

  private GenericKeyedObjectPool<NodeAddress, Socket> pool;

  public SocketPool() {
    SocketFactory factory = new SocketFactory();
    GenericKeyedObjectPoolConfig<Socket> config = new GenericKeyedObjectPoolConfig();
    config.setTestOnBorrow( true );
    config.setTestOnReturn( true );
    config.setJmxEnabled( false );
    //config.setMaxTotal( 1000 );
    config.setMaxTotalPerKey( 2000 );
    pool = new GenericKeyedObjectPool<NodeAddress, Socket>( factory, config );
  }


  public Socket getSocket( NodeAddress nodeAddress ) throws Exception {
    logger.error( "borrow" );
    return pool.borrowObject( nodeAddress, 2000 );
  }

  public void returnSocket( NodeAddress nodeAddress, Socket socket ) throws Exception {
    logger.error( "return" );
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
      Socket socket = pooledObject.getObject();
      return !socket.isClosed() && socket.isConnected();
    }

  }


}
