package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.clientserver.LineBufferHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class LoggingHandler implements LineBufferHandler {

  private static Logger logger = LoggerFactory.getLogger( "Server" );

  private InetSocketAddress senderAddress;

  @Override
  public void setSender( InetSocketAddress senderAddress ) {
    this.senderAddress = senderAddress;
  }

  @Override
  public HandlerReply process( String line ) {
    logger.debug( line );
    return null;
  }

}
