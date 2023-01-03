package com.infodesire.jvmcom.services.logging;

import com.infodesire.jvmcom.clientserver.HandlerReply;
import com.infodesire.jvmcom.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class LoggingHandlerImpl implements LoggingHandler {

  private static final Logger logger = LoggerFactory.getLogger( "Server" );

  private InetSocketAddress senderAddress;

  @Override
  public void setSender( InetSocketAddress senderAddress ) {
    this.senderAddress = senderAddress;
  }

  @Override
  public HandlerReply process( String line ) {
    if( !StringUtils.isEmpty( line ) ) {
      if( line.startsWith( "log " ) ) {
        logger.debug( line.substring( 4 ) );
      }
    }
    return new HandlerReply( "" + LoggingUtils.getLevel( logger ) );
  }

}
