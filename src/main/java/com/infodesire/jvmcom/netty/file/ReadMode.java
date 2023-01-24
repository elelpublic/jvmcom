package com.infodesire.jvmcom.netty.file;

import io.netty.util.AttributeKey;

/**
 * Reading mode if file client channel.
 * <p>
 * The channel can be either reading meta data of a file
 * or reading the binary file data.
 *
 */
public enum ReadMode {

    /**
     * Reading meta data before file data
     */
    META,

    /**
     * Reading binary file data
     */
    FILE;

    public static AttributeKey<ReadMode> KEY = AttributeKey.newInstance( "READ_MODE" );

}
