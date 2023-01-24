package com.infodesire.jvmcom.netty.file;

import io.netty.util.AttributeKey;

/**
 * Meta data of a file: size and name
 */
public class MetaData {

    /**
     * Key under which this meta data is stored in context attributes
     */
    public static final AttributeKey<MetaData> KEY = AttributeKey.newInstance( "META_DATA" );

    String name;

    int size;

}
