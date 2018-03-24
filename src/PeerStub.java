import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PeerStub {
  private String address = Peer.ADDRESS;
  private int port;

  public PeerStub(int port){
    this.port = port;
  }

  public void query(int upstream, MessageID messageID, int TTL, String filename){
    String rpc = "query";
    try {
      Socket socket = new Socket(address, port);

      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      os.writeInt(upstream);
      os.writeObject(messageID);
      os.writeInt(TTL);
      os.writeObject(filename);

      socket.close();

    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public void hitQuery(MessageID messageID, int TTL, String filename, String address){
    String rpc = "hitQuery";
    try {
      Socket socket = new Socket(address, port);

      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      os.writeObject(messageID);
      os.writeInt(TTL);
      os.writeObject(filename);
      os.writeObject(address);

      socket.close();

    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public boolean obtain(String filename){
    String rpc = "obtain";
    try {
      Socket socket = new Socket(address, port);

      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      os.writeObject(filename);

      InputStream is = socket.getInputStream();
      FileOutputStream fos = new FileOutputStream(Peer.OTHER_FILES_DIR + filename);

      int count;
      byte[] buffer = new byte[4096];
      while((count = is.read(buffer)) > 0){
        fos.write(buffer, 0, count);
      }

      socket.close();
      fos.close();

      return true;

    } catch (Exception e){
      e.printStackTrace();
    }

    return false;
  }

  public String getFullAddress(){
    return address + ":" + port;
  }

}
