package com.infodesire.jvmcom;

import java.net.Socket;
import java.util.function.Consumer;

public interface ServerWorker extends Consumer<Socket> {

  void requestStop();

}
