package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.netty.util.ServerUtils;
import com.infodesire.jvmcom.services.logging.Level;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.cert.CertificateException;

/**
 * Send log requests to a logging server in the form of:
 * <p>
 * CATEGORY LEVEL BYTES\r\n
 * MESSAGE
 * <p>
 * where CATEGORY is the name of the logger on the server,
 * LEVEL is the log level,
 * BYTES is the length of the log message,
 * MESSAGE is the log message.
 * <p>
 * The server will respond with:
 * <p>
 * OK CATEGORY LEVEL
 * <p>
 * where LEVEL is the current log level for the given logger on the server.
 *
 */
public class LoggingClient implements AutoCloseable {

    /**
     * Logger for local problems of the client (not the remote logger)
     */
    static final Logger localLogger = LoggerFactory.getLogger( "LoggingClient" );

    private static final ChannelHandler STRING_DECODER = new StringDecoder();
    private static final ChannelHandler LOGGING_REPLY_DECODER = new LoggingReplyDecoder();
    private static final ChannelHandler STRING_ENCODER = new StringEncoder();

    private final LoggingClientConfig config;
    private final SslContext sslCtx;
    private final NioEventLoopGroup group;
    private final Channel channel;
    private PMap<String, Level> levels = HashTreePMap.empty();

    public LoggingClient( LoggingClientConfig config ) throws CertificateException, SSLException, InterruptedException {

        this.config = config;

        sslCtx = config.useSSL ? ServerUtils.buildSslContext() : null;

        String threadBaseName = getClass().getSimpleName();
        if( config.workerThreadName != null ) {
            threadBaseName = config.workerThreadName;
        }

        group = new NioEventLoopGroup( new DefaultThreadFactory( threadBaseName ) );
        LoggingClientHandler loggingClient = new LoggingClientHandler( this, config );

        Bootstrap b = new Bootstrap();
        b.group( group )
                .channel( NioSocketChannel.class )
                .handler( new ChannelInitializer<SocketChannel>() {
                    protected void initChannel( SocketChannel ch ) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if( sslCtx != null ) {
                            pipeline.addLast( sslCtx.newHandler( ch.alloc(), config.host, config.port ) );
                        }
                        pipeline.addLast( STRING_ENCODER );
                        pipeline.addLast( STRING_DECODER );
                        pipeline.addLast( LOGGING_REPLY_DECODER );
                        pipeline.addLast( loggingClient );
                    }
                } );

        localLogger.info( "Connecting to server " + config.host + ":" + config.port );
        channel = b.connect( config.host, config.port ).sync().channel();

    }

    /**
     * Currently active log level on server for a given logger
     * <p>
     * The level requested from server on the first time a category
     * is logged and subsequently after each call to log(...).
     *
     * @param category Logger category (name of logger)
     * @return Currently active log level on server or null if level is not yet known
     *
     */
    public Level getLevel( String category ) {
        return levels.get( category );
    }

    /**
     * This methode is called by the handler, whenever a server reply arrives
     * containing the information about the current log level of one named logger (category).
     *
     * @param category Logger category (name of logger)
     * @param level Current log level
     *
     */
    protected void setLevel( String category, Level level ) {
        levels = levels.plus( category, level );
    }

    /**
     * Send a logging request to server
     *
     * @param category Category of message (=name of logger on server)
     * @param level Log level
     * @param message Log message
     * @param ex Exception causing this message
     *
     */
    public void log( String category, Level level, String message, Throwable ex ) throws InterruptedException {

        StringWriter stackTrace = new StringWriter();
        PrintWriter out = new PrintWriter( stackTrace );
        message = message + "\n" + stackTrace;
        log( category, level, message );

    }


    /**
     * Send a logging request to server
     *
     * @param category Category of message (=name of logger on server)
     * @param level Log level
     * @param message Log message
     */
    public void log( String category, Level level, String message ) throws InterruptedException {
        Level serverLevel = getLevel( category );
        if( message == null ) {
            message = "";
        }
        if( serverLevel == null || serverLevel.isAtLeast( level ) ) {
            ByteBuf buf = Unpooled.buffer();
            int size = message.getBytes( CharsetUtil.UTF_8 ).length;
            buf.writeCharSequence( category + " " + level + " " + size + "\n" + message, CharsetUtil.UTF_8 );
            channel.writeAndFlush( buf ).sync();
        }
    }

    @Override
    public void close() throws Exception {
        group.shutdownGracefully();
        channel.close().sync();
    }

    public static void main( String[] args ) throws Exception {

        LoggingClientConfig config = new LoggingClientConfig();

        config.host = "localhost";
        if( args.length > 0 ) {
            config.host = args[ 0 ];
        }

        config.port = 44000;
        if( args.length > 1 ) {
            config.port = Integer.parseInt( args[ 1 ] );
        }

        LoggingClient client = new LoggingClient( config );

        BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
        while( true ) {
            System.out.println( "Enter a logging request like this CATEGORY LEVEL message or EXIT to quit." );
            String line = in.readLine();
            if( line != null ) {
                line = line.trim();
                if( line.equalsIgnoreCase( "EXIT" ) ) {
                    client.close();
                    break;
                }
                else {
                    int index = line.indexOf( " " );
                    String category = line.substring( 0, index );
                    line = line.substring( index ).trim();
                    index = line.indexOf( " " );
                    String levelName = line.substring( 0, index );
                    line = line.substring( index ).trim();
                    Level level = Level.valueOf( levelName );
                    client.log( category, level, line );
                }
            }
        }

        System.out.println( "Bye" );

    }

}
