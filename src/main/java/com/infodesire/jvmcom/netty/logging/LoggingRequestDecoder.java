package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.netty.util.BufferUtils;
import com.infodesire.jvmcom.services.logging.Level;
import com.infodesire.jvmcom.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;

import java.util.List;

public class LoggingRequestDecoder extends ByteToMessageDecoder {

    public static final Logger localLogger = LoggingServer.localLogger;

    @Override
    protected void decode( ChannelHandlerContext ctx, ByteBuf buf, List<Object> list ) throws Exception {

        Attribute<String> clientName = ctx.attr( LoggingServer.CLIENT_NAME_ATTR );
        if( clientName.get() == null ) {
            String name = BufferUtils.readLineUntil( buf, '\n', 300 );
            if( name == null ) {
                name = "unknkown";
                localLogger.warn( "No client name received" );
            }
            else {
                if( name.startsWith( "CLIENT " ) ) {
                    name = name.substring( 7 ).trim();
                    clientName.set( name );
                }
                else {
                    localLogger.warn( "Client name should be submitted like this: CLIENT <name>, but found '" + name + "'" );
                }

            }
            clientName.set( name );
        }

        while( buf.readableBytes() > 0 ) {

            boolean parseRequestOK = false;

            LoggingRequest loggingRequest = new LoggingRequest();
            loggingRequest.clientName = clientName.get();

            String meta = BufferUtils.readLineUntil( buf, '\n', 100 );

            if( meta != null ) {

                int levelIndex = meta.indexOf( ' ' );
                if( levelIndex == -1 ) {
                    localLogger.error( "No category information found in logging request" );
                }
                else {

                    loggingRequest.category = meta.substring( 0, levelIndex ).trim();
                    meta = meta.substring( levelIndex ).trim();

                    int bytesIndex = meta.indexOf( ' ' );
                    if( bytesIndex == -1 ) {
                        localLogger.error( "No debug level information found in logging request" );
                    }
                    else {

                        String levelName = meta.substring( 0, bytesIndex ).trim();
                        try {
                            loggingRequest.level = Level.valueOf( levelName );
                            meta = meta.substring( bytesIndex ).trim();
                            if( StringUtils.isEmpty( meta ) ) {
                                localLogger.error( "No size information found in logging request" );
                            }
                            else {

                                int size = Integer.parseInt( meta );

                                if( buf.readableBytes() >= size ) {
                                    loggingRequest.message = buf.readCharSequence( size, CharsetUtil.UTF_8 ).toString();
                                    list.add( loggingRequest );
                                    buf.markReaderIndex();
                                    parseRequestOK = true;
                                }

                            }

                        }
                        catch( IllegalArgumentException ex ) {
                            localLogger.error( "Invalid log level " + levelName, ex );
                        }

                    }

                }

            }

            if( !parseRequestOK ) {
                buf.resetReaderIndex();
                break;
            }

        }

    }

}
