package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.netty.util.BufferUtils;
import com.infodesire.jvmcom.services.logging.Level;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;

import java.util.List;

public class LoggingRequestDecoder extends ByteToMessageDecoder {

    public static final Logger localLogger = LoggingServer.localLogger;

    @Override
    protected void decode( ChannelHandlerContext ctx, ByteBuf buf, List<Object> list ) throws Exception {

        while( buf.readableBytes() > 0 ) {

            boolean parseRequestOK = false;

            LoggingRequest loggingRequest = new LoggingRequest();

            String meta = BufferUtils.readLineUntil( buf, '\n', 100 );

            if( meta != null ) {

                int levelIndex = meta.indexOf( ' ' );
                if( levelIndex != -1 ) {

                    loggingRequest.category = meta.substring( 0, levelIndex ).trim();
                    meta = meta.substring( levelIndex ).trim();

                    int bytesIndex = meta.indexOf( ' ' );
                    if( bytesIndex != -1 ) {

                        String levelName = meta.substring( 0, bytesIndex ).trim();
                        try {
                            loggingRequest.level = Level.valueOf( levelName );
                            meta = meta.substring( bytesIndex ).trim();

                            int size = Integer.parseInt( meta );

                            if( buf.readableBytes() >= size ) {
                                loggingRequest.message = buf.readCharSequence( size, CharsetUtil.UTF_8 ).toString();
                                list.add( loggingRequest );
                                buf.markReaderIndex();
                                parseRequestOK = true;
                            }

                        }
                        catch( IllegalArgumentException ex ) {
                            localLogger.error( "Invalid log level " + levelName, ex );
                        }

                    }
                    localLogger.error( "No size information found in logging request" );

                }

            }

            if( !parseRequestOK ) {
                buf.resetReaderIndex();
                break;
            }

        }

    }

}
