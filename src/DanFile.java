import java.io.Serializable;

public class DanFile implements Serializable{

  public static final int VALID = 0, INVALID = 1, TTR_EXPIRED = 2;

  private String filename;
  private int version;
  private PeerID originServer;
  private int consistency;
  private long lastModifiedTime;
  private long TTR;

  public DanFile(String filename) {
    this.filename = filename;
  }

  public PeerID getOriginServer() {
    return originServer;
  }

  public void setOriginServer(PeerID originServer) {
    this.originServer = originServer;
  }

  public int getConsistency() {
    return consistency;
  }

  public void setConsistency(int consistency) {
    this.consistency = consistency;
  }

  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  public long getTTR() {
    return TTR;
  }

  public void setTTR(long TTR) {
    this.TTR = TTR;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public boolean isOwner(Peer peer) {
    return originServer.toString().equals(peer.getFullAddress());
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public void invalidate(){
    this.consistency = INVALID;
  }

  public boolean isValid() {
    return consistency == VALID;
  }
}
