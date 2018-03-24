import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class PeerSkeleton {

  private Peer peer;

  public PeerSkeleton(Peer p){
    this.peer = p;
  }

  public void listen(){
    try {
      ServerSocket socket = new ServerSocket(peer.getPort());
      while(true){
        Socket s = socket.accept();
        ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
        String rpc = (String) inputStream.readObject();
        new Thread(){
          @Override
          public void run() {
            if(rpc.equals("query")){
              query(s, inputStream);
            } else if(rpc.equals("hitQuery")){
              hitQuery(s, inputStream);
            } else if(rpc.equals("obtain")){
              obtain(s, inputStream);
            }
          }
        }.start();
      }
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public void query(Socket s, ObjectInputStream inputStream){

  }

  public void hitQuery(Socket s, ObjectInputStream inputStream){

  }


  /**
   * Services request by the Peer on the other side of the connection of the socket. Sends the requested file over
   * the network.
   * @param s Connection to listen for file name ane send file over
   */
  private void obtain(Socket s, ObjectInputStream inputStream){
    try {
      // Receive filename
      String filename = (String) inputStream.readObject();

      // Send file
      FileInputStream fis = peer.obtain(filename); // File reading stream
      OutputStream os = s.getOutputStream(); // File uploading stream

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
}
