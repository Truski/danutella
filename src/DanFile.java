import java.io.Serializable;

/**
 * A DanFile represents a file in a shared drive. It contains helpers, getters, and setters to make tracking and sending
 * files over the network a breeze.
 */
public class DanFile implements Serializable{

  // The 3 states a file could be in
  public static final int VALID = 0, INVALID = 1, TTR_EXPIRED = 2;

  private String filename; // The text-based file name
  private int version; // The current version of the file that this peer knows about
  private PeerID originServer; // The master server that owns the file and can edit it
  private int consistency; // The current consistency state (Valid, Invalid, TTR_Expired)
  private long lastModifiedTime; // The last modified time in the system's nanoseconds of the file on disk
  private long lastPolledTime; // The last time this file was polled for pull-based consistency
  private long TTR; // The length of time at which to poll

  /**
   * Creates a DanFile with specified name
   * @param filename name of the file on disk
   */
  public DanFile(String filename) {
    this.filename = filename;
  }

  // **********************************************
  // Useful functions to manipulate and check files
  // **********************************************

  /**
   * Invalidates the file by setting its consistency
   */
  public void invalidate(){
    this.consistency = INVALID;
  }

  /**
   * Checks to see if the file is valid for uploading
   * @return True if the file is in a consistent state
   */
  public boolean isValid() {
    checkTTR();
    return consistency == VALID;
  }

  /**
   * Checks to see if the given peer is the owner of the file
   * @param peer Peer to check ownership
   * @return True if the peer is the owner, false otherwise
   */
  public boolean isOwner(Peer peer) {
    return originServer.toString().equals(peer.getFullAddress());
  }

  // *******************
  // Getters and Setters
  // *******************

  // Comments omitted because this is generic, boilerplate code

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

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public long getLastPolledTime() {
    return lastPolledTime;
  }

  public void setLastPolledTime(long lastPolledTime) {
    this.lastPolledTime = lastPolledTime;
  }

  public String print(Peer ref){
    String validation;
    switch(consistency){
      case VALID:
        validation = "VALID";
        break;
      case INVALID:
        validation = "INVALID";
        break;
      case TTR_EXPIRED:
        validation = "TTR_EXPIRED";
        break;
      default:
        validation = "UNKNOWN";
    }
    if(isOwner(ref)){
      return " +++ " + filename + " : { version: " + version + "; state: " + validation + " }";
    }
    return " - " + filename + " : { version: " + version + "; state: " + validation + " }";
  }

  public void checkTTR(){
    long time = System.nanoTime();
    if(isValid() && time > lastPolledTime + TTR){
      consistency = TTR_EXPIRED;
    }
  }

  public boolean isExpired(){
    return consistency == TTR_EXPIRED;
  }

  public void updateTTR(long newTTR){
    lastPolledTime = System.nanoTime();
    TTR = newTTR;
  }


}
