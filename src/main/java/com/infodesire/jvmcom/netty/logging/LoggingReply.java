package com.infodesire.jvmcom.netty.logging;

import com.infodesire.jvmcom.services.logging.Level;
import com.infodesire.jvmcom.util.StringUtils;

/**
 * Reply from logging server
 */
public class LoggingReply {

    String category;

    Level level;

    public LoggingReply( String category, Level level ) {
        this.category = category;
        this.level = level;
    }

    public boolean equals( Object o ) {
        LoggingReply other = (LoggingReply) o;
        return StringUtils.equals( category, other.category )
                && level == other.level;
    }

    public String toString() {
        return category + " " + level;
    }

}
