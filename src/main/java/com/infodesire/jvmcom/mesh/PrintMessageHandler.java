package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.clientserver.HandlerReply;

/**
 * Handle messages by printing them to standard out
 *
 */
public class PrintMessageHandler implements MessageHandler {

  @Override
  public HandlerReply handleDm( String senderAddress, String message ) {
    System.out.println( "Direct message from " + senderAddress + ": " + message );
    return new HandlerReply( "OK" );
  }

  @Override
  public HandlerReply handleCast( String senderAddress, String message ) {
    System.out.println( "Broadcast message from " + senderAddress + ": " + message );
    return new HandlerReply( "OK" );
  }

}
