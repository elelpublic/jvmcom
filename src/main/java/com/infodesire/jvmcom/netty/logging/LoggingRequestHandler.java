package com.infodesire.jvmcom.netty.logging;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Do something with incoming logging requests
 */
public class LoggingRequestHandler extends SimpleChannelInboundHandler<LoggingRequest> {

    @Override
    protected void channelRead0( ChannelHandlerContext ctx, LoggingRequest loggingRequest ) throws Exception {

        Attribute<String> clientName = ctx.attr( LoggingServer.CLIENT_NAME_ATTR );

        Logger logger = LoggerFactory.getLogger( loggingRequest.category );
        logger.atLevel( loggingRequest.level.asSlf4jLevel() ).log( "[" + clientName.get() + "] "
                + loggingRequest.message );

    }

}
