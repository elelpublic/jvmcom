package com.infodesire.jvmcom.netty.file;

import com.infodesire.jvmcom.netty.util.BufferUtils;
import com.infodesire.jvmcom.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Decoode meta data of a file which will be sent next by the server
 *
 */
public class MetaDataHandler extends ChannelInboundHandlerAdapter {

    public static Logger logger = FileClient.logger;

    @Override
    public void channelRead( ChannelHandlerContext ctx, Object msg ) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        Attribute<ReadMode> readModeAttribute = ctx.channel().attr( ReadMode.KEY );
        ReadMode readMode = readModeAttribute.get();
        if( readMode == null ) {
            readMode = ReadMode.META;
            readModeAttribute.set( readMode );
        }

        if( logger.isEnabledForLevel( Level.INFO ) ) {
            logger.info( "Received in mode " + readMode + ": " + BufferUtils.peakLineUntil( buf, 100 ) );
        }

        if( readMode == ReadMode.META ) {

            // check if a full line (ending with \n) is in the buffer
            String line = BufferUtils.readLineUntil( buf, '\n', 8192 );
            if( line != null ) {

                if( line.length() < 3 ) {
                    logger.error( "Cannot parse line: " + line );
                    buf.clear();
                }
                else {
                    String cmd = line.substring( 0, 3 ).toLowerCase();
                    if( cmd.equals( "hel" ) ) {
                        BufferUtils.readLineUntil( buf, '\n', 8196 );
                        logger.info( "Server welcome message: " + line );
                    }
                    else if( cmd.equals( "ok:" ) ) {
                        MetaData metaData = new MetaData();
                        line = line.substring( 3 ).trim();
                        int sep = line.indexOf( " " );
                        if( sep == -1 ) {
                            logger.error( "Cannot read file size in line: " + line );
                            buf.clear();
                        }
                        else {
                            metaData.size = Integer.parseInt( line.substring( 0, sep ) );
                            metaData.name = line.substring( sep ).trim();
                            if( StringUtils.isEmpty( metaData.name ) ) {
                                logger.error( "Cannot read file name in line: " + line );
                            }
                            else {
                                ctx.channel().attr( MetaData.KEY ).set( metaData );
                                readMode = ReadMode.FILE;
                                ctx.channel().attr( ReadMode.KEY ).set( readMode );
                            }
                        }
                    }
                    else if( cmd.equals( "err" ) ) {
                        logger.error( "Server replied with error message: " + line );
                        buf.clear();
                    }
                    else {
                        logger.error( "Cannot parse line: " + line );
                        buf.clear();
                    }
                }

            }
            // else line == null -> wait for more data to arrive

        }
        else {
            // else do nothing because data will be processed by binary file handler
            ctx.fireChannelRead( msg );
        }

    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) {
        logger.error( "Error in meta data handler", cause );
        ctx.close();
    }

}
