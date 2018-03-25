public class DanFile {
  private String filename;
  private boolean owner;

  public DanFile(String filename, boolean owner) {
    this.filename = filename;
    this.owner = owner;
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
}
