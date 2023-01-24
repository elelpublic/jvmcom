package com.infodesire.jvmcom.netty.file;

import com.infodesire.jvmcom.netty.util.ServerUtils;
import com.infodesire.jvmcom.util.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;

/**
 * Client to the FileServer in the same package
 */
public final class FileClient implements AutoCloseable {

    static final String DEFAULT_LOG_LEVEL = "warn";
    static {
        System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", DEFAULT_LOG_LEVEL );
    }

    static final Logger logger = LoggerFactory.getLogger( "FileClient" );

    static final int DEFAULT_PORT = 44000;
    static final String DEFAULT_HOST = "localhost";
    static final String DEFAULT_DOWNLOAD_DIR = ".";
    static final boolean DEFAULT_USE_SSL = false;

    private final NioEventLoopGroup group;
    private final Channel ch;
    private static Options options;
    private SslContext sslCtx = null;

    public static void main( String[] args ) throws Exception {

        System.out.println( "File client" );

        options = createOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args );

        String logLevel = DEFAULT_LOG_LEVEL;
        if( cmd.hasOption( "l" )  ) {
            logLevel = cmd.getOptionValue( "l" );
        }
        LogUtils.setSimpleLoggerLevel( logger, logLevel );

        if( !( cmd.hasOption( "f" ) || cmd.hasOption( "i" ) ) ) {
            showUsage( "Error: need one of -f or -i options" );
            return;
        }

        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        String downloadDir = DEFAULT_DOWNLOAD_DIR;

        if( cmd.hasOption( "h" ) ) {
            host = cmd.getOptionValue( "h" );
        }

        if( cmd.hasOption( "p" ) ) {
            port = Integer.parseInt( cmd.getOptionValue( "p" ) );
        }

        if( cmd.hasOption( "d" ) ) {
            downloadDir = cmd.getOptionValue( "d" );
            logger.debug( "Download dir: " + downloadDir );
        }

        boolean useSsl = DEFAULT_USE_SSL;
        if( cmd.hasOption( "s" ) ) {
            useSsl = true;
        }

        FileClient fileClient = new FileClient( host, port, useSsl, downloadDir );

        if( cmd.hasOption( "f" ) ) {
            fileClient.download( cmd.getOptionValue( "f" ) );
        }
        else if( cmd.hasOption( "i" ) ) {
            fileClient.startCommandLine();
        }

    }

    public FileClient( String host, int port, boolean useSsl, String downloadDirName ) throws CertificateException, IOException, InterruptedException {

        Thread.currentThread().setName( "_C-main-0-0" );

        // Configure SSL.
        if( useSsl ) {
            sslCtx = ServerUtils.buildSslContext();
        }

        group = new NioEventLoopGroup( new DefaultThreadFactory( "_C-asyn" ) );
        //FileClientHandler fileClientHandler = new FileClientHandler( new File( downloadDirName ) );
        BinaryFileDataHandler binaryFileDataHandler = new BinaryFileDataHandler( new File( downloadDirName ) );
        Bootstrap b = new Bootstrap();
        b.group( group )
                .channel( NioSocketChannel.class )
                .handler( new ChannelInitializer<SocketChannel>() {
                    protected void initChannel( SocketChannel ch ) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if( sslCtx != null ) {
                            pipeline.addLast( sslCtx.newHandler( ch.alloc(), host, port ) );
                        }
                        pipeline.addLast( new StringEncoder() );
                        pipeline.addLast( new MetaDataHandler() );
                        pipeline.addLast( binaryFileDataHandler );
                    }
                } );

        logger.info( "Connecting to server " + host + ":" + port );
        ch = b.connect( host, port ).sync().channel();

    }

    public void startCommandLine() {

        print( "FileClient interactive mode" );

        try {

            // read commands from console
            BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
            while( ch.isActive() ) {

                print( "Enter file name: " );
                String line = in.readLine();
                if( line == null || !ch.isActive() ) {
                    break;
                }

                if( "bye".equalsIgnoreCase( line ) ) {
                    ch.closeFuture().sync();
                    break;
                }

                download( line.trim() );

            }

        }
        catch( IOException | InterruptedException ex ) {
            throw new RuntimeException( ex );
        }

    }

    /**
     * Request a file from server. File will be stored with the same name (not path)
     * in the download directory.
     *
     * @param filePath Path of file
     * @throws InterruptedException when download was interrupted
     */
    public void download( String filePath ) throws InterruptedException {
        logger.info( "Sending request for file '" + filePath + "'" );
        ch.writeAndFlush( filePath + "\r\n" ).sync();
    }

    @Override
    public void close() throws Exception {
        group.shutdownGracefully();
    }

    private static Options createOptions() {

        // create Options object
        Options options = new Options();

        // add l option for lowercase
        //options.addOption( "l", false, "print lower case" );

        options.addOption(
                Option.builder()
                        .argName( "host" )
                        .option( "h" )
                        .hasArg()
                        .desc( "host name, default is " + DEFAULT_HOST )
                        .build()
        );

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
                        .argName( "file" )
                        .option( "f" )
                        .hasArg()
                        .desc( "name of file to download (path to file on server)" )
                        .build()
        );

        options.addOption(
                Option.builder()
                        .argName( "interactive" )
                        .option( "i" )
                        .desc( "start file client in interactive mode" )
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
                        .argName( "downloadDir" )
                        .option( "d" )
                        .hasArg()
                        .desc( "directory where to store downloaded files - default " + DEFAULT_DOWNLOAD_DIR )
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
        formatter.printHelp( "FileClient [options]", options );

    }

    private static void print( String line ) {
        System.out.println( line );
    }

}


