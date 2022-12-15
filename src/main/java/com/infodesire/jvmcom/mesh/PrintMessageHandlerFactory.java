package com.infodesire.jvmcom.mesh;

import com.infodesire.jvmcom.mesh.MessageHandler;
import com.infodesire.jvmcom.mesh.PrintMessageHandler;

import java.util.function.Supplier;

/**
 * Create message handlers which print messages to standard out
 *
 */
public class PrintMessageHandlerFactory implements Supplier<MessageHandler> {

  @Override
  public MessageHandler get() {
    return new PrintMessageHandler();
  }

}

