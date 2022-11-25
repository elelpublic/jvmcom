package com.infodesire.jvmcom.util;

import java.io.IOException;
import java.net.ServerSocket;

public class SocketUtils {

  public static int getFreePort() throws IOException {
    ServerSocket serverSocket = new ServerSocket( 0 );
    int port = serverSocket.getLocalPort();
    serverSocket.close();
    return port;
  }

}
