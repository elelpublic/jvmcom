package com.infodesire.jvmcom.netty.file;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import org.slf4j.Logger;

import java.io.File;
import java.io.RandomAccessFile;

public class FileServerHandler extends SimpleChannelInboundHandler<String> {

    static Logger logger = FileServer.logger;

    @Override
    public void channelActive( ChannelHandlerContext ctx ) {
        logger.info( "New channel was opened. Sending welcome message." );
        ctx.writeAndFlush( "HELLO: Type the path of the file to retrieve.\n" );
    }

    @Override
    public void channelRead0( ChannelHandlerContext ctx, String msg ) throws Exception {

        logger.info( "Received request for file: '" + msg + "'" );

        RandomAccessFile raf = null;
        File file = new File( msg );
        long length = -1;
        try {
            raf = new RandomAccessFile( file, "r" );
            length = raf.length();
        }
        catch( Exception ex ) {
            ctx.writeAndFlush( "ERR: " + ex.getClass().getSimpleName() + ": " + ex.getMessage() + '\n' );
            return;
        }
        finally {
            if( length < 0 && raf != null ) {
                raf.close();
            }
        }

        String replyWithLength = "OK: " + raf.length() + " " + file.getName() + "\n";
        logger.debug( "Sending file size: " + replyWithLength );
        ctx.writeAndFlush( replyWithLength );

        if( ctx.pipeline().get( SslHandler.class ) == null ) {
            // SSL not enabled - can use zero-copy file transfer.
            logger.info( "Sending file data " + file );
            ctx.write( new DefaultFileRegion( raf.getChannel(), 0, length ) );
        }
        else {
            // SSL enabled - cannot use zero-copy file transfer.
            logger.info( "Sending file data in chunks " + file );
            ctx.write( new ChunkedFile( raf ) );
        }
        logger.info( "Finished sending file " + file );
        ctx.writeAndFlush( "\n" );
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) {

        logger.error( "Error in FileServer", cause );

        if( ctx.channel().isActive() ) {
            ctx.writeAndFlush( "ERR: " +
                    cause.getClass().getSimpleName() + ": " +
                    cause.getMessage() + '\n' ).addListener( ChannelFutureListener.CLOSE );
        }
    }
}

