package com.infodesire.jvmcom.util;

import java.util.Collections;

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

    /**
     * Null safe string compare. Empty and null strings are considered equal.
     *
     * @param text1 Text 1
     * @param text2 Text 2
     * @return Texts are equal
     *
     */
    public static boolean equals( String text1, String text2 ) {
        if( isEmpty( text1 ) ) {
            return isEmpty( text2 );
        }
        else if( isEmpty( text2 ) ) {
            return isEmpty( text1 );
        }
        else {
            return text1.equals( text2 );
        }
    }

    /**
     * Repeat string s n times
     *
     * @param s Input string
     * @param n Number of repeats
     * @return s repeated n times
     *
     */
    public static String repeat( String s, int n ) {
        return String.join("", Collections.nCopies(n, s));
    }

}
