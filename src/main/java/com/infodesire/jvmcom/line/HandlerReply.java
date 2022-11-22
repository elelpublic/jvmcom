package com.infodesire.jvmcom.line;

/**
 * Reply from a LineBufferHandler
 */
public class HandlerReply {

  /**
   * True: go on with processing, false: close connection
   */
  boolean continueProcessing = true;

  /**
   * Reply text to be sent to client
   */
  String replyText;

  /**
   * Reply with text an continue = true
   *
   * @param replyText Reply text to be sent to client
   */
  public HandlerReply( String replyText ) {
    this.replyText = replyText;
  }

  /**
   * @param continueProcessing True: go on with processing, false: close connection
   * @param replyText Reply text to be sent to client
   */
  public HandlerReply( boolean continueProcessing, String replyText ) {
    this.continueProcessing = continueProcessing;
    this.replyText = replyText;
  }

  /**
   * @param continueProcessing True: go on with processing, false: close connection
   */
  public HandlerReply( boolean continueProcessing ) {
    this.continueProcessing = continueProcessing;
  }

}
