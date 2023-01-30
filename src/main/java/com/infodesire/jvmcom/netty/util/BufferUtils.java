package com.infodesire.jvmcom.netty.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class BufferUtils {

    /**
     * Read string from buffer until an end character was found.
     * If no end character was found, the buffers read index will be reset to the initial position.
     *
     * @param buf       Buffer to read
     * @param end       End character
     * @param maxLength Max length of line
     * @return Line of text including end character or null if nothing was found.
     */
    public static String readLineUntil( ByteBuf buf, char end, int maxLength ) {

        buf.markReaderIndex();

        String line = null;

        try {
            line = buf.readCharSequence( Math.min( maxLength, buf.readableBytes() ),
                    CharsetUtil.UTF_8 ).toString();
        }
        catch( IndexOutOfBoundsException ex ) {
            buf.resetReaderIndex();
            return null;
        }

        if( line == null ) {
            buf.resetReaderIndex();
            return null;
        }

        int endIndex = line.indexOf( end );

        if( endIndex == -1 ) {
            buf.resetReaderIndex();
            return null;
        }

        return line.substring( 0, endIndex + 1 );

    }

    /**
     * Peak at buffer content and return it as a string. Read position will not be changed afterwards.
     *
     * @param buf           Buffer to read
     * @param maxLength     Max length of line
     * @param stopAtLineEnd Only return first line
     * @return Line of text including end character or null if nothing was found.
     */
    public static String peakLineUntil( ByteBuf buf, int maxLength, boolean stopAtLineEnd ) {

        String result = null;

        buf.markReaderIndex();

        try {

            result = buf.readCharSequence( Math.min( maxLength, buf.readableBytes() ),
                    CharsetUtil.UTF_8 ).toString();

            if( result != null ) {

                if( stopAtLineEnd ) {
                    int eol = result.indexOf( "\n" );
                    if( eol != -1 ) {
                        result = result.substring( 0, eol );
                    }
                }

            }

        }
        catch( IndexOutOfBoundsException ignored ) {
        }
        finally {
            buf.resetReaderIndex();
        }

        return result;

    }

}
