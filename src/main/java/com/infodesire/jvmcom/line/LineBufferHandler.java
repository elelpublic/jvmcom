package com.infodesire.jvmcom.line;

/**
 * Worker processing lines of text received on a socket
 */
public interface LineBufferHandler {

  /**
   * Process a line of data from client
   *
   * @param line Line of data
   * @return True: continue listening, false: close connection
   *
   */
  HandlerReply process( String line );


}
