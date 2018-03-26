import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This class is the client-side interface of a Peer server. When connecting to and requesting files from other peers,
 * a peer must instantiate a PeerStub with a Peer it gets from the Server.
 */

public class PeerStub {
  private PeerID ID;

  /**
   * Creates a Peer Stub that will connect to the given PeerID
   *
   * @param ID The ID of the peer to connect to
   */
  public PeerStub(PeerID ID){
    this.ID = ID;
  }


  /**
   * Packs parameters of the query method and sends them to the peer
   * @param upstream Where to send back
   * @param messageID The messageID of this message
   * @param TTL How many hops it has left
   * @param filename The name of the file being queried
   */
  public void query(PeerID upstream, MessageID messageID, int TTL, String filename){
    // Name the RPC
    String rpc = "query";
    try {
      // Connect to the Peer
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

      // Send over the RPC
      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      // Send over all the parameters
      os.writeObject(upstream);
      os.writeObject(messageID);
      os.writeInt(TTL);
      os.writeObject(filename);

      // Clean up resources
      socket.close();

    } catch (Exception e){
      // An error occurred on the connection
      e.printStackTrace();
    }
  }

  /**
   * Packs parameters and sends a hitQuery message to the peer
   * @param messageID the ID of this message
   * @param TTL how many hops it has left
   * @param filename the name of the file that was found
   * @param address the address where the final can be found
   */
  public void hitQuery(MessageID messageID, int TTL, String filename, PeerID address){
    // Name the RPC
    String rpc = "hitQuery";
    try {
      // Connect to the Peer
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

      // Send over the RPC
      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      // Send over all the parameters
      os.writeObject(messageID);
      os.writeInt(TTL);
      os.writeObject(filename);
      os.writeObject(address);

      // Clean up socket
      socket.close();

    } catch (Exception e){
      // A network error occurred
      e.printStackTrace();
    }
  }


  /**
   * Downloads the the given File from the Peer to the shared directory. Also gets a DanFile object to store in list of
   * files
   *
   * @param filename Name of the file to download
   * @return The DanFile representation of the file. Null if error or no file found
   */
  public DanFile obtain(String filename){
    // Name the RPC
    String rpc = "obtain";
    try {
      // Connect over the network to the peer
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

      // Send the RPC
      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      // Send filename
      os.writeObject(filename);

      // Get input over network
      InputStream is = socket.getInputStream();

      // Read in DanFile
      ObjectInputStream objectInputStream = new ObjectInputStream(is);
      DanFile danFile = (DanFile) objectInputStream.readObject();

      // Open file-writing stream
      FileOutputStream fos = new FileOutputStream(Peer.OTHER_FILES_DIR + filename);

      int count; // Number of bytes read
      byte[] buffer = new byte[4096]; // Buffer to store file in
      while((count = is.read(buffer)) > 0 ){ // Read from Peer into buffer
        fos.write(buffer, 0, count); // Write to file from buffer
      }

      // Release resources
      socket.close();
      fos.close();

      // Return the DanFile representation
      return danFile;

    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }

    return null; // Failed to download. Something went wrong
  }

  /**
   * Packs parameters and sends an invalidation message to the peer
   * @param messageID the messageID of the message
   * @param originServer the origin of the file
   * @param filename the name of the file to invalidate
   * @param version the latest version of the file
   */
  public void invalidate(MessageID messageID, PeerID originServer, String filename, int version){
    // Name the RPC
    String rpc = "invalidate";
    try {
      // Connect to the peer
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

      // Send the name of the RPC
      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      // Send over parameters one by one
      os.writeObject(messageID);
      os.writeObject(originServer);
      os.writeObject(filename);
      os.writeInt(version);
      os.writeObject("endoffile"); // End of stream padding

      // Clean up resources and close connection
      socket.close();

    } catch (Exception e){
      // An error occurred over the network
      e.printStackTrace();
    }
  }

  /**
   * Polls the origin server to see if the file must be updated or not. Packs the parameters
   * and returns a PollResult object for the Peer to make decisions on
   *
   * @param version the version to check for
   * @param filename Name of the file to check
   * @return A PollResult object containing TTR and expiration information from the origin server
   */
  public PollResult poll(int version, String filename) {
    // Name the RPC
    String rpc = "poll";
    try {
      // Connect over the network to the peer
      Socket socket = new Socket(ID.getAddress(), ID.getPort());

      // Send the RPC
      ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
      os.writeObject(rpc);

      // Send version
      os.writeInt(version);
      // Send filename
      os.writeObject(filename);

      // Get input over network
      InputStream is = socket.getInputStream();
      // Read in PollResult
      ObjectInputStream objectInputStream = new ObjectInputStream(is);
      PollResult result = (PollResult) objectInputStream.readObject();

      // Release resources
      socket.close();

      // Return the poll result
      return result;

    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }

    return null; // Failed to poll. Something went wrong
  }
}
