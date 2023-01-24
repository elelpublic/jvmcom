package com.infodesire.jvmcom.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class BufferUtils {

    /**
     * Read string from buffer until an end character was found.
     * If no end character was found, the buffers read index will be reset to the initial position.
     *
     * @param buf Buffer to read
     * @param end End character
     * @param maxLength Max length of line
     * @return Line of text including end character or null if nothing was found.
     */
    public static String readLineUntil( ByteBuf buf, char end, int maxLength ) {

        buf.markReaderIndex();

        StringBuffer line = new StringBuffer();

        while( buf.readableBytes() > 0 && line.length() <= maxLength ) {
            CharSequence c = buf.readCharSequence( 1, CharsetUtil.UTF_8 );
            line.append( c );
            if( c.charAt( 0 ) == end ) {
                return line.toString();
            }
        }

        buf.resetReaderIndex();
        return null;

    }

    /**
     * Peak at buffer content an return it as a string. Read position will not be changed afterwards.
     *
     * @param buf       Buffer to read
     * @param maxLength Max length of line
     * @return Line of text including end character or null if nothing was found.
     */
    public static StringBuffer peakLineUntil( ByteBuf buf, int maxLength ) {

        buf.markReaderIndex();
        StringBuffer line = new StringBuffer();

        try {

            while( buf.readableBytes() > 0 && line.length() <= maxLength ) {
                CharSequence c = buf.readCharSequence( 1, CharsetUtil.UTF_8 );
                if( c.charAt( 0 ) == '\n' ) {
                    break;
                }
                line.append( c );
            }

        }
        finally {
            buf.resetReaderIndex();
        }

        return line;

    }

}
