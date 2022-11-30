package com.infodesire.jvmcom.mesh;

public class Message {

  private final String text;

  private final String senderId;

  private final MessageType type;

  public Message( MessageType type, String senderId, String text ) {
    this.type = type;
    this.senderId = senderId;
    this.text = text;
  }

  public String toString() {
    return type + " from " + senderId + ": " + text;
  }

  public String getText() {
    return text;
  }

  public String getSenderId() {
    return senderId;
  }

  public MessageType getType() {
    return type;
  }

}
