package com.infodesire.jvmcom.util;

public class StringUtils {

    /**
     * Null safe empty check with whitespaces trimmed
     *
     * @param text Text to check
     * @return Text is not null and not empty
     *
     */
    public static boolean isEmpty( String text ) {
        if( text == null ) {
            return true;
        }
        return text.trim().length() == 0;
    }

}
