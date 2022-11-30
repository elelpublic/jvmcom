package com.infodesire.jvmcom;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

public interface ServerWorker extends Consumer<Socket> {

  void setSender( InetSocketAddress sender );

  void requestStop();

}
