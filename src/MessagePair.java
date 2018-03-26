public class MessagePair {
  private MessageID messageID;
  private PeerID upstream;

  public MessagePair(MessageID messageID, PeerID upstream) {
    this.messageID = messageID;
    this.upstream = upstream;
  }

  public MessageID getMessageID() {
    return messageID;
  }

  public void setMessageID(MessageID messageID) {
    this.messageID = messageID;
  }

  public PeerID getUpstream() {
    return upstream;
  }

  public void setUpstream(PeerID upstream) {
    this.upstream = upstream;
  }

  @Override
  public boolean equals(Object obj) {
    MessagePair other = (MessagePair) obj;
    return messageID.equals(other.getMessageID()) && upstream == other.getUpstream();
  }
}
