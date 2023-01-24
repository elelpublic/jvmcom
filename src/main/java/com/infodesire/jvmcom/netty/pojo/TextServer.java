package com.infodesire.jvmcom.netty.pojo;

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
 * Echoes any incoming data.
 */
public class TextServer {

    private final int port;

    public TextServer( int port ) {
        this.port = port;
    }

    public void run() throws Exception {

        //System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", "debug" );

        SslContext sslCtx = ServerUtils.buildSslContext();

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
                            pipeline.addLast(sslCtx.newHandler(ch.alloc()));

                            // Add the text line codec combination first,
                            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));

                            // the encoder and decoder are static as these are sharable
                            pipeline.addLast( new StringDecoder() );
                            pipeline.addLast( new StringEncoder() );

                            // and then business logic.
                            pipeline.addLast( new TextServerHandler() );

                        }
                    } );

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind( port ).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
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