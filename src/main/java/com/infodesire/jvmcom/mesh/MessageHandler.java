package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.HandlerReply;

/**
 * Handles broadcast and direct messages between nodes
 *
 */
public interface MessageHandler {

  /**
   * Handle a direct message
   *
   * @param senderAddress Address of sender
   * @param message Text of message
   * @return Reply to sender and information on how to proceed with operations
   *
   */
  HandlerReply handleDm( String senderAddress, String message );

  /**
   * Handle a direct message
   *
   * @param senderAddress Address of sender
   * @param message Text of message
   * @return Reply to sender and information on how to proceed with operations
   *
   */
  HandlerReply handleCast( String senderAddress, String message );

}
