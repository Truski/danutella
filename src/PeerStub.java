import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PeerStub {
  private PeerID ID;

  public PeerStub(PeerID ID){
    this.ID = ID;
  }

  public void query(PeerID upstream, MessageID messageID, int TTL, String filename){
    String rpc = "query";
    try {
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      os.writeObject(upstream);
      os.writeObject(messageID);
      os.writeInt(TTL);
      os.writeObject(filename);

      socket.close();

    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public void hitQuery(MessageID messageID, int TTL, String filename, PeerID address){
    String rpc = "hitQuery";
    try {
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

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

  public DanFile obtain(String filename){
    String rpc = "obtain";
    try {
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      os.writeObject(filename);

      InputStream is = socket.getInputStream();

      // Read in DanFile
      ObjectInputStream objectInputStream = new ObjectInputStream(is);
      DanFile danFile = (DanFile) objectInputStream.readObject();

      FileOutputStream fos = new FileOutputStream(Peer.OTHER_FILES_DIR + filename);

      int count;
      byte[] buffer = new byte[4096];
      while((count = is.read(buffer)) > 0){
        fos.write(buffer, 0, count);
      }

      socket.close();
      fos.close();

      return danFile;

    } catch (Exception e){
      e.printStackTrace();
    }

    return null;
  }

  public void invalidate(MessageID messageID, PeerID originServer, String filename, int version){
    String rpc = "invalidate";
    try {
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      os.writeObject(messageID);
      os.writeObject(originServer);
      os.writeObject(filename);
      os.writeInt(version);

      socket.close();

    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public String getFullAddress(){
    return ID.toString();
  }

}
