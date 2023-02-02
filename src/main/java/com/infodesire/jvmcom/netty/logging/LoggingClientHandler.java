package com.infodesire.jvmcom.netty.logging;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;

/**
 * Handle responses from logging server
 * <p>
 * The server will respond to each log request with a message "OK LEVEL" where
 * LEVEL is the current level of the requested logger.
 *
 */
public class LoggingClientHandler extends SimpleChannelInboundHandler<LoggingReply> {

    private final LoggingClient loggingClient;
    private static final Logger localLogger = LoggingClient.localLogger;
    private final LoggingClientConfig config;

    public LoggingClientHandler( LoggingClient loggingClient, LoggingClientConfig config ) {
        this.loggingClient = loggingClient;
        this.config = config;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush( Unpooled.copiedBuffer( "CLIENT " + config.clientName + "\n",
                CharsetUtil.UTF_8 ) );
    }

    @Override
    protected void channelRead0( ChannelHandlerContext ctx, LoggingReply msg ) throws Exception {
        loggingClient.setLevel( msg.category, msg.level );
    }

}
