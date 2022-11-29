package com.infodesire.jvmcom.pool;

import com.infodesire.jvmcom.mesh.NodeAddress;
import com.infodesire.jvmcom.util.SocketUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;

public class SocketPoolTest {


  @Test
  public void testPooling() throws Exception {

    SocketPool pool = new SocketPool();

    NodeAddress serverAddress = new NodeAddress( "main",
      new InetSocketAddress( "localhost", SocketUtils.getFreePort() ) );
    ServerSocket serverSocket = new ServerSocket( serverAddress.getInetSocketAddress().getPort() );

    Socket socket = pool.getSocket( serverAddress );
    assertTrue( socket.isConnected() );
    assertFalse( socket.isClosed() );

    pool.returnSocket( serverAddress, socket );

    Socket pooledSocket = pool.getSocket( serverAddress );
    assertTrue( pooledSocket.isConnected() );
    assertFalse( pooledSocket.isClosed() );
    assertSame( socket, pooledSocket );

    // closed sockets will be evicted an a new one will be created
    pooledSocket.close();

    pool.returnSocket( serverAddress, pooledSocket );

    Socket newSocket = pool.getSocket( serverAddress );
    assertTrue( newSocket.isConnected() );
    assertFalse( newSocket.isClosed() );
    assertNotSame( newSocket, pooledSocket );
    assertNotSame( newSocket, socket );

    // a second socket
    Socket secondSocket = pool.getSocket( serverAddress );
    assertNotSame( newSocket, secondSocket );


  }


}