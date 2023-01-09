package com.infodesire.jvmcom.netty.time;

import java.util.Date;

/**
 * The pojo used in pojo time server
 */
public class Time {

    private final long value;

    public Time() {
        this( System.currentTimeMillis() / 1000L + 2208988800L );
    }

    public Time( long value ) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public String toString() {
        return new Date( ( value() - 2208988800L ) * 1000L ).toString();
    }

}


