package com.infodesire.jvmcom.clientserver;

import java.net.InetSocketAddress;

/**
 * Worker processing lines of text received on a socket
 */
public interface LineBufferHandler {

  /**
   * Set sender of requests
   *
   * @param senderAddress Sender of requests
   *
   */
  void setSender( InetSocketAddress senderAddress );

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
