package com.infodesire.jvmcom.netty.file;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BinaryFileDataHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = FileClient.logger;
    private final File downloadDir;
    private FileOutputStream fileOutputStream;
    private int remainingBytes;

    public BinaryFileDataHandler( File downloadDir ) {
        this.downloadDir = downloadDir;
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) {
        logger.error( "Error in binary file data handler", cause );
        ctx.close();
    }

    @Override
    public void channelRead( ChannelHandlerContext ctx, Object msg ) {

        boolean close = false;

        Attribute<ReadMode> readModeAttribute = ctx.channel().attr( ReadMode.KEY );
        ReadMode readMode = readModeAttribute.get();

        if( readMode == ReadMode.FILE ) {

            ByteBuf buf = (ByteBuf) msg;

            try {

                MetaData metaData = ctx.channel().attr( MetaData.KEY ).get();
                File file = new File( downloadDir, metaData.name );

                logger.debug( "Reading file data" );

                try {

                    if( fileOutputStream == null ) {
                        logger.info( "Opening file " + file );
                        fileOutputStream = new FileOutputStream( file );
                        remainingBytes = metaData.size;
                    }

                    int numBytes = buf.readableBytes();
                    if( numBytes > remainingBytes ) {
                        numBytes = remainingBytes;
                    }

                    buf.getBytes( 0, fileOutputStream, numBytes );
                    remainingBytes -= numBytes;

                    if( remainingBytes <= 0 ) {
                        fileOutputStream.close();
                        fileOutputStream = null;
                        readModeAttribute.set( ReadMode.META );
                        logger.info( "Closing file after writing " + file );
                    }

                }
                catch( IOException ex ) {
                    logger.error( "Error reading file data", ex );
                    close = true;
                }

                if( close ) {
                    logger.info( "Closing connection" );
                    ctx.close();
                }

            }
            finally {
                buf.release();
            }

        }
        
    }

}
