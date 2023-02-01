package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.netty.util.BufferUtils;
import com.infodesire.jvmcom.services.logging.Level;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Decode a reply from logging server
 * <p>
 * The message looks like this
 * <p>
 * OK CATEGORY LEVEL\n
 */
public class LoggingReplyDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode( ChannelHandlerContext channelHandlerContext, ByteBuf buf, List<Object> list ) throws Exception {
        String line = BufferUtils.readLineUntil( buf, '\n', 8196 );
        if( line != null ) {
            if( line.startsWith( "OK " ) ) {
                line = line.substring( 3 ).trim();
                int sep = line.indexOf( " " );
                if( sep != -1 ) {
                    String category = line.substring( 0, sep );
                    line = line.substring( sep ).trim();
                    try {
                        Level level = Level.valueOf( line );
                        LoggingReply reply = new LoggingReply( category, level );
                        list.add( reply );
                    }
                    catch( IllegalArgumentException ignored ) {}
                }
            }
        }
    }

}

