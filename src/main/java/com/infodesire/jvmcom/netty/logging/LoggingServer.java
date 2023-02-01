package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.netty.util.ServerUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * Handles request for logging into the servers log system
 *
 */
public class LoggingServer implements AutoCloseable {

    /**
     * Logger for local problems of the server (not the remote logger)
     */
    public static final Logger localLogger = LoggerFactory.getLogger( "LoggingServer" );

    private final ChannelFuture channelFuture;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private static final LoggingRequestDecoder decoder = new LoggingRequestDecoder();

    public LoggingServer( LoggingServerConfig config ) throws CertificateException, SSLException, InterruptedException {

        final SslContext sslCtx = config.useSSL ? ServerUtils.buildSslContext() : null;

        String threadName = "boss-" + getClass().getSimpleName();
        if( config.bossThreadName != null ) {
            threadName = config.bossThreadName;
        }
        bossGroup = new NioEventLoopGroup( 1,new DefaultThreadFactory( threadName ) );

        threadName = "work-" + getClass().getSimpleName();
        if( config.workerThreadName != null ) {
            threadName = config.workerThreadName;
        }
        workerGroup = new NioEventLoopGroup( new DefaultThreadFactory( threadName ) );

        SimpleChannelInboundHandler<LoggingRequest> handler = config.loggingRequestHandler != null
                ? config.loggingRequestHandler : new LoggingRequestHandler();

        ServerBootstrap b = new ServerBootstrap();
        b.group( bossGroup, workerGroup )
                .channel( NioServerSocketChannel.class )
                .option( ChannelOption.SO_BACKLOG, 100 )
                //.handler(new LoggingHandler( LogLevel.DEBUG))
                .childHandler( new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel( SocketChannel ch ) {

                        ChannelPipeline pipeline = ch.pipeline();

                        if (sslCtx != null) {
                            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                        }

                        //pipeline.addLast(new DelimiterBasedFrameDecoder( MAX_MESSAGE_LENGTH, Delimiters.lineDelimiter()));

                        pipeline.addLast( new StringEncoder() );

                        pipeline.addLast( decoder );
                        pipeline.addLast( handler );

                    }
                } );

        channelFuture = b.bind( config.port ).sync();

    }

    @Override
    public void close() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        channelFuture.channel().close().sync();
    }

    public static void main( String[] args ) throws Exception {

        int port = 44000;
        if( args.length > 0 ) {
            port = Integer.parseInt( args[ 0 ] );
        }

        LoggingServerConfig config = new LoggingServerConfig();
        config.port = port;

        new LoggingServer( config );

    }

}

