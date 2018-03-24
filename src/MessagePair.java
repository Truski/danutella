public class MessagePair {
  private MessageID messageID;
  private int upstream;

  public MessagePair(MessageID messageID, int upstream) {
    this.messageID = messageID;
    this.upstream = upstream;
  }

  public MessageID getMessageID() {
    return messageID;
  }

  public void setMessageID(MessageID messageID) {
    this.messageID = messageID;
  }

  public int getUpstream() {
    return upstream;
  }

  public void setUpstream(int upstream) {
    this.upstream = upstream;
  }

  @Override
  public boolean equals(Object obj) {
    MessagePair other = (MessagePair) obj;
    return messageID.equals(other.getMessageID()) && upstream == other.getUpstream();
  }
}
