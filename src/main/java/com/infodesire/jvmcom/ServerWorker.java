package com.infodesire.jvmcom;

import java.net.InetSocketAddress;
import java.net.Socket;

public interface ServerWorker {

  void work( Socket socket );

  void requestStop();

}
