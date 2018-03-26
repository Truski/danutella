/**
 * A messagePair links a messageID to an upstream peer. Instead of using a hashtable, a short arraylist is created so
 * that we can control the maximum amount of associative mappings we are caching.
 */
public class MessagePair {
  private MessageID messageID;
  private PeerID upstream;

  // ****************
  // Boilerplate Code
  // ****************

  // Comments omitted because this is generic, boilerplate code

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
