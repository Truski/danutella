import java.io.Serializable;

/**
 * A MessageID is the combination of a PeerID and a sequenceNumber. It is used so that a message is not received twice
 * by the same peer, and so that we can link them to Upstream PeerIDs as a trace-back mechanism.
 */
public class MessageID implements Serializable {
  private PeerID peerID;
  private int sequenceNumber;

  // ****************
  // Boilerplate Code
  // ****************

  // Comments omitted because this is generic, boilerplate code

  public MessageID(PeerID peerID, int sequenceNumber) {
    this.peerID = peerID;
    this.sequenceNumber = sequenceNumber;
  }

  public PeerID getPeerID() {
    return peerID;
  }

  public void setPeerID(PeerID peerID) {
    this.peerID = peerID;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public void setSequenceNumber(int sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  @Override
  public boolean equals(Object obj) {
    MessageID other = (MessageID) obj;
    return peerID.equals(other.getPeerID()) && sequenceNumber == other.getSequenceNumber();
  }
}
