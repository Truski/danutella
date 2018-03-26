import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * This class acts as a network interface for the server part of the peer. It listens for incoming peer requests and
 * delegates the commands up to the reference of the peer.
 */

public class PeerSkeleton {

  private Peer peer;

  /**
   * Creates a Peer Skeleton that will listen to incoming connections for the given Peer.
   *
   * @param p The Peer that the skeleton is to provide interface for
   */
  public PeerSkeleton(Peer p){
    this.peer = p;
  }

  /**
   * Listens for incoming connections. Should be called on a separate thread so that a peer could also make its own
   * requests.
   */
  public void listen(){
    try {
      // Create a ServerSocket with the given port number
      ServerSocket socket = new ServerSocket(peer.getPort());
      // Infinitely listen to requests
      while(true){
        // Accept a new connection
        Socket s = socket.accept();

        // Read the RPC
        ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
        String rpc = (String) inputStream.readObject();

        // Start a new Thread to complete the RPC
        new Thread(){
          @Override
          public void run() {
            // Check which operation is requested and complete the desired action
            if(rpc.equals("query")){
              query(s, inputStream);
            } else if(rpc.equals("hitQuery")){
              hitQuery(s, inputStream);
            } else if(rpc.equals("obtain")){
              obtain(s, inputStream);
            } else if(rpc.equals("invalidate")){
              invalidate(s, inputStream);
            }
          }
        }.start();
      }
    } catch (Exception e){
      // An error occurred in the connection
      e.printStackTrace();
    }
  }

  /**
   * Reads the parameters from the input stream for a query and call the peer's query method
   * @param s socket to close when done
   * @param inputStream InputStream to read over the network
   */
  public void query(Socket s, ObjectInputStream inputStream) {
    try {
      // Receive parameters
      PeerID upstream = (PeerID) inputStream.readObject(); // Upstream
      MessageID messageID = (MessageID) inputStream.readObject(); // MessageID
      int TTL = inputStream.readInt(); // TTL
      String filename = (String) inputStream.readObject(); // Filename

      // Release resources
      s.close();

      // Send the operation to the Peer object
      peer.query(upstream, messageID, TTL, filename);

    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }
  }

  /**
   * Reads the parameters from the input stream for a hitQuery and call the peer's hitQuery method
   * @param s socket to close when done
   * @param inputStream InputStream to read over the network
   */
  public void hitQuery(Socket s, ObjectInputStream inputStream){
    try {
      // Receive parameters
      MessageID messageID = (MessageID) inputStream.readObject(); // MessageID
      int TTL = inputStream.readInt(); // TTL
      String filename = (String) inputStream.readObject(); // Name of the file
      PeerID address = (PeerID) inputStream.readObject(); // Address that has the file

      // Release resources
      s.close();

      // Send the operation to the Peer object
      peer.hitQuery(messageID, TTL, filename, address);

    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }
  }


  /**
   * Services request by the Peer on the other side of the connection of the socket. Sends the requested file over
   * the network. Slightly different from Napster in that we also send the DanFile
   * @param s Connection to listen for file name ane send file over
   * @param inputStream InputStream to read over the network
   */
  private void obtain(Socket s, ObjectInputStream inputStream){
    try {
      // Receive filename
      String filename = (String) inputStream.readObject();

      // Open output stream
      OutputStream os = s.getOutputStream(); // File uploading stream

      // Send file information
      DanFile danFile = peer.getFile(filename); // Get DanFile with the given name
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(os); // Create object output stream over network
      objectOutputStream.writeObject(danFile); // Send DanFile over the network

      // Send file
      FileInputStream fis = peer.obtain(filename); // File reading stream

      int count; // Number of bytes read from file
      byte[] buffer = new byte[4096]; // Buffer to store file in
      while ((count = fis.read(buffer)) > 0){ // Read from file into buffer
        os.write(buffer, 0, count); // Write to Peer from Buffer
      }

      // Release resources
      s.close();
      fis.close();

    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }
  }

  /**
   * Reads the parameters from the input stream for an invalidate message and call the peer's invalidate method
   * @param s socket to close when done
   * @param inputStream InputStream to read over the network
   */
  public void invalidate(Socket s, ObjectInputStream inputStream){
    try {
      // Receive parameters
      MessageID messageID = (MessageID) inputStream.readObject(); // MessageID
      PeerID originServer = (PeerID) inputStream.readObject(); // Origin Server
      String filename = (String) inputStream.readObject(); // Name of the file
      int version = inputStream.readInt(); // New Version of the file

      // Release resources
      s.close();

      // Send the operation to the Peer object
      peer.invalidate(messageID, originServer, filename, version);

    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }
  }
}
