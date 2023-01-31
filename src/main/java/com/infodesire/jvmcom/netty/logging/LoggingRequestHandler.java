package com.infodesire.jvmcom.netty.logging;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Do something with incoming logging requests
 */
public class LoggingRequestHandler extends SimpleChannelInboundHandler<LoggingRequest> {

    @Override
    protected void channelRead0( ChannelHandlerContext channelHandlerContext, LoggingRequest loggingRequest ) throws Exception {

        Logger logger = LoggerFactory.getLogger( loggingRequest.category );
        logger.atLevel( loggingRequest.level.asSlf4jLevel() ).log( loggingRequest.message );

    }

}
