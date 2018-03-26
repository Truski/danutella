public class DanFile {

  public static final int VALID = 0, INVALID = 1, TTR_EXPIRED = 2;

  private String filename;
  private boolean owner;
  private int version;
  private String originServer;
  private int consistency;
  private long lastModifiedTime;
  private long TTR;

  public DanFile(String filename, boolean owner, int version, String originServer, long lastModifiedTime, long TTR) {
    this.filename = filename;
    this.owner = owner;
    this.version = version;
    this.originServer = originServer;
    this.consistency = VALID;
    this.lastModifiedTime = lastModifiedTime;
    this.TTR = TTR;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public boolean isOwner() {
    return owner;
  }

  public void setOwner(boolean owner) {
    this.owner = owner;
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
