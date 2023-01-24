package com.infodesire.jvmcom.netty.file;

import com.infodesire.jvmcom.netty.util.ServerUtils;
import com.infodesire.jvmcom.util.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * Returns file content as bytes
 *
 */
public final class FileServer implements AutoCloseable {

    static final String DEFAULT_LOG_LEVEL = "warn";
    static {
        System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", DEFAULT_LOG_LEVEL );
    }

    static final Logger logger = LoggerFactory.getLogger( "FileServer" );

    static final int DEFAULT_PORT = 44000;
    static final boolean DEFAULT_USE_SSL = false;

    private final ChannelFuture channelFuture;
    private SslContext sslCtx;
    NioEventLoopGroup bossGroup;
    NioEventLoopGroup workerGroup;

    private static Options options;

    public static void main( String[] args ) throws Exception {

        options = createOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args );

        if( cmd.hasOption( "?" ) ) {
            showUsage( null );
            return;
        }

        String logLevel = DEFAULT_LOG_LEVEL;
        if( cmd.hasOption( "l" )  ) {
            logLevel = cmd.getOptionValue( "l" );
        }
        LogUtils.setSimpleLoggerLevel( logger, logLevel );

        int port = DEFAULT_PORT;

        if( cmd.hasOption( "p" ) ) {
            port = Integer.parseInt( cmd.getOptionValue( "p" ) );
        }

        boolean useSsl = DEFAULT_USE_SSL;
        if( cmd.hasOption( "s" ) ) {
            useSsl = true;
        }

        print( "Starting FileServer on port " + port );
        print( "For configuration options call FileServer -?" );

        FileServer fileServer = null;

        try {
            fileServer = new FileServer( port, useSsl );
        }
        finally {
            if( fileServer != null ) {
                fileServer.close();
            }
        }

    }

    public FileServer( int port, boolean useSsl ) throws CertificateException, SSLException, InterruptedException {

        if( useSsl ) {
            sslCtx = ServerUtils.buildSslContext();
        }

        bossGroup = new NioEventLoopGroup( 1, new DefaultThreadFactory( "!S-boss" ) );
        workerGroup = new NioEventLoopGroup( new DefaultThreadFactory( "!S-work" ) );
        ServerBootstrap b = new ServerBootstrap();
        b.group( bossGroup, workerGroup )
                .channel( NioServerSocketChannel.class )
                .option( ChannelOption.SO_BACKLOG, 100 )
                .handler( new LoggingHandler( LogLevel.INFO ) )
                .childHandler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel( SocketChannel ch ) {
                        ChannelPipeline p = ch.pipeline();
                        if( sslCtx != null ) {
                            p.addLast( sslCtx.newHandler( ch.alloc() ) );
                        }
                        p.addLast(
                                new StringEncoder( CharsetUtil.UTF_8 ),
                                new LineBasedFrameDecoder( 8192 ),
                                new StringDecoder( CharsetUtil.UTF_8 ),
                                new ChunkedWriteHandler(),
                                new FileServerHandler() );
                    }
                } );

        // start server
        channelFuture = b.bind( port ).sync();

    }

    @Override
    public void close() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        channelFuture.channel().close().sync();
    }

    private static Options createOptions() {

        // create Options object
        Options options = new Options();

        // add l option for lowercase
        //options.addOption( "l", false, "print lower case" );

        options.addOption(
                Option.builder()
                        .argName( "port" )
                        .option( "p" )
                        .hasArg()
                        .desc( "port, default is " + DEFAULT_PORT )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .argName( "ssl" )
                        .option( "s" )
                        .desc( "use ssl encryption - default: " + DEFAULT_USE_SSL )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .argName( "logLevel" )
                        .option( "l" )
                        .hasArg()
                        .desc( "log level (debug|info|warn|error) - default " + DEFAULT_LOG_LEVEL )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .argName( "help" )
                        .option( "?" )
                        .desc( "show help" )
                        .build()
        );

        return options;

    }

    private static void showUsage( String message ) {

        HelpFormatter formatter = new HelpFormatter();
        if( message != null && message.trim().length() > 0 ) {
            print( "####################################" );
            print( message );
            print( "####################################" );
            print( "" );
        }
        formatter.printHelp( "FileServer [options]", options );

    }

    private static void print( String line ) {
        System.out.println( line );
    }

}

