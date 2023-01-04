package com.infodesire.jvmcom.clientserver.binary;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.clientserver.ServerHandler;

import java.net.InetSocketAddress;

/**
 * Worker processing lines of text received on a socket
 */
public interface BinaryHandler extends ServerHandler {

  /**
   * Process a line of data from client
   *
   *
   * @param line Line of data
   * @return True: continue listening, false: close connection
   *
   */
  HandlerReply process( String line );

}
