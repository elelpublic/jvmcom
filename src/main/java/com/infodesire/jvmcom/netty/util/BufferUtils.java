package com.infodesire.jvmcom.netty.util;

import com.infodesire.jvmcom.util.StringUtils;
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

        int oldReaderIndex = buf.readerIndex();
        buf.markReaderIndex();

        String line = null;

        try {
            CharSequence chars = buf.readCharSequence( Math.min( maxLength, buf.readableBytes() ),
                    CharsetUtil.UTF_8 );
            if( chars == null ) {
                buf.resetReaderIndex();
                return null;
            }
            line = chars.toString();
        }
        catch( IndexOutOfBoundsException ex ) {
            buf.resetReaderIndex();
            return null;
        }

        int endIndex = line.indexOf( end );

        if( endIndex == -1 ) {
            buf.resetReaderIndex();
            return null;
        }

        String result = line.substring( 0, endIndex + 1 );
        buf.readerIndex( oldReaderIndex + result.getBytes( CharsetUtil.UTF_8 ).length );
        return result;

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

            CharSequence chars = buf.readCharSequence( Math.min( maxLength, buf.readableBytes() ),
                    CharsetUtil.UTF_8 );

            if( chars != null ) {

                result = chars.toString();

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

    /**
     * Create a visual display of a buffer containing text with markers
     *
     * @param buf Buffer containing text
     * @return Visual representation of a text buffer and its state variables
     *
     */
    public static String debugTextBuffer( ByteBuf buf ) {

        buf.markReaderIndex();
        buf.readerIndex( 0 );
        String string = buf.readCharSequence( buf.readableBytes(), CharsetUtil.UTF_8 ).toString();
        buf.resetReaderIndex();
        if( string == null ) {
            string = "";
        }

        String result = "\"" + string.replace( '\r', '_' ).replace( '\n', '_' ).replace( '\t', '_' ) + "\"\n";
        result += "|" + StringUtils.repeat( "-", buf.readerIndex() ) + "^R" + StringUtils.repeat( " ", buf.readableBytes() + 1 )
                + "(" + buf.readerIndex() + ") (" + buf.readableBytes() + " more)\n";
        result += "|" + StringUtils.repeat( "-", buf.writerIndex() ) + "^W (" + buf.writerIndex() + ")";

        return result;

    }

}
