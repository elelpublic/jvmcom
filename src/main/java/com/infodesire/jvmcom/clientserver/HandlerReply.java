package com.infodesire.jvmcom.clientserver;

import com.infodesire.jvmcom.mesh.MeshError;

/**
 * Reply from a LineBufferHandler
 */
public class HandlerReply {

  /**
   * True: go on with processing, false: close connection
   */
  public boolean continueProcessing = true;

  /**
   * Reply text to be sent to client
   */
  public String replyText;

  /**
   * Reply with text an continue = true
   *
   * @param replyText Reply text to be sent to client
   */
  public HandlerReply( String replyText ) {
    this.replyText = replyText;
  }

  /**
   * Reply with text an continue = true
   *
   * @param error Reply text to be sent to client
   */
  public HandlerReply( MeshError error ) {
    this.replyText = error.name();
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
