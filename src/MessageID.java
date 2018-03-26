import java.io.Serializable;

public class MessageID implements Serializable {
  private PeerID peerID;
  private int sequenceNumber;

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
