import java.io.Serializable;

/**
 * A PeerID is a combination of the peer's address and port number used for identification purposes.
 */
public class PeerID implements Serializable{
  private String address;
  private int port;

  // ****************
  // Boilerplate Code
  // ****************

  // Comments omitted because this is generic, boilerplate code

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
