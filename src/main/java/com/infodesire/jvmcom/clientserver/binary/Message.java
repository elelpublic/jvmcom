package com.infodesire.jvmcom.clientserver.binary;

import java.io.*;

/**
 * Message passed between binary client and server
 * <p>
 * Objects can be used on ObjectOutputStream/ObjectInputStream through serialization
 * or
 *
 */
public class Message implements Serializable {

    public Status status;

    public String fileName;

    public int fileSize;

    public String directoryListing;

    public Message() {
    }

    public Message( File file ) {
        status = Status.OK;
        fileName = file.getName();
        if( file.length() > Integer.MAX_VALUE ) {
            throw new RuntimeException( "Files larger than " + Integer.MAX_VALUE + " bytes are not supported." );
        }
        fileSize = (int) file.length();
    }

    public void serialize( DataOutputStream out ) throws IOException {
        out.writeInt( status.ordinal() );
        out.writeUTF( fileName );
        out.writeInt( fileSize );
        out.writeUTF( directoryListing == null ? "" : directoryListing );
        out.flush();
    }

    /**
     * Read message from stream
     *
     * @param in Data input stream
     * @return Message read or null if end of stream was reached.
     * @throws IOException
     */
    public static Message deserialize( DataInputStream in ) throws IOException {
        try {
            Message message = new Message();
            message.status = Status.values()[ in.readInt() ];
            message.fileName = in.readUTF();
            message.fileSize = in.readInt();
            message.directoryListing = in.readUTF();
            return message;
        }
        catch( EOFException ex ) {
            return null;
        }
    }

}
