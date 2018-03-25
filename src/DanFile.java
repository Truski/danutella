public class DanFile {
  private String filename;
  private boolean owner;
  private int version;

  public DanFile(String filename, boolean owner, int version) {
    this.filename = filename;
    this.owner = owner;
    this.version = version;
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
}
