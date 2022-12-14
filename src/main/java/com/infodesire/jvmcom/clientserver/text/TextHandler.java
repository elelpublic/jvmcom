package com.infodesire.jvmcom.clientserver.text;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.clientserver.ServerHandler;

/**
 * Worker processing lines of text received on a socket
 */
public interface TextHandler extends ServerHandler {

  /**
   * Process a line of data from client
   *
   * @param line Line of data
   * @return True: continue listening, false: close connection
   *
   */
  HandlerReply process( String line );

}
