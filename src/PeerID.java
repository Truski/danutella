public class PeerID {
  private String address;
  private int port;

  public PeerID(int port) {
    this.address = Peer.ADDRESS;
    this.port = port;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String toString(){
    return address + ":" + port;
  }

  @Override
  public boolean equals(Object other){
    PeerID otherID = (PeerID) other;
    return address.equals(otherID.getAddress()) && port == otherID.getPort();
  }
}
