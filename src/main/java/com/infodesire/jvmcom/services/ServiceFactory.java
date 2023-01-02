package com.infodesire.jvmcom.services;

/**
 * Create service by name
 */
public interface ServiceFactory {

    /**
     * @param port
     * @param name Name of service
     * @return New instance
     */
    Service create( int port, String name );

}
