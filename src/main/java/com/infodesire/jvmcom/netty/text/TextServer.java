package com.infodesire.jvmcom.netty.text;

import com.infodesire.jvmcom.netty.util.ServerUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

/**
 * Simple server for telnet like communication
 */
public class TextServer {

    private final int port;

    public TextServer( int port ) {
        this.port = port;
    }

    public void run() throws Exception {

        //System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug" );

        final SslContext sslCtx = ServerUtils.buildSslContext();

        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group( bossGroup, workerGroup )
                    .channel( NioServerSocketChannel.class ) // (3)
                    .handler(new LoggingHandler( LogLevel.DEBUG))
                    .childHandler( new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel( SocketChannel ch ) throws Exception {

                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast( new LoggingHandler(LogLevel.DEBUG) );

                            if (sslCtx != null) {
                                pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                            }

                            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));

                            pipeline.addLast( new StringDecoder() );
                            pipeline.addLast( new StringEncoder() );

                            pipeline.addLast( new TextServerHandler() );

                        }
                    } );

            // start server and wait for shutdown
            ChannelFuture f = b.bind( port ).sync();

            f.channel().closeFuture().sync();

        }
        finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }

    }

    public static void main( String[] args ) throws Exception {
        int port = 44000;
        if( args.length > 0 ) {
            port = Integer.parseInt( args[ 0 ] );
        }

        new TextServer( port ).run();
    }
}