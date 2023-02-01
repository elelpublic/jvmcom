package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.netty.ServerConfig;
import io.netty.channel.SimpleChannelInboundHandler;

public class LoggingServerConfig extends ServerConfig {

    /**
     * Handler for incoming logging requests. If null a standard handler will be used.
     */
    SimpleChannelInboundHandler<LoggingRequest> loggingRequestHandler;

}
