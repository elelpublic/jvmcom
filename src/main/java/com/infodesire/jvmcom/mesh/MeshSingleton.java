package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.pool.SocketPool;
import com.infodesire.jvmcom.services.ServiceFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * If you need exactly one mesh, this is a way to create and access it
 */
public class MeshSingleton {

    private static Mesh mesh;

    public static void initialize( File meshConfigFile, Supplier<MessageHandler> messageHandlerFactory,
                                   ServiceFactory serviceFactory ) throws IOException {
        if( mesh != null ) {
            throw new RuntimeException( "Mesh is already initialized" );
        }
        MeshConfig config = MeshConfig.loadFromFile(meshConfigFile);
        SocketPool socketPool = new SocketPool();
        initialize( config, socketPool, messageHandlerFactory, serviceFactory );
    }

    public static void initialize( MeshConfig config, SocketPool socketPool,
                                   Supplier<MessageHandler> messageHandlerFactory,
                                   ServiceFactory serviceFactory ) {
        if( mesh != null ) {
            throw new RuntimeException( "Mesh is already initialized" );
        }
        mesh = new Mesh( config, socketPool, messageHandlerFactory, serviceFactory );
    }

    /**
     * @return Single mesh instance if initialized, null otherwise (call initialize first)
     */
    public static Mesh get() {
        return mesh;
    }


}
