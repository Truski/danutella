import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

public class Peer {

  public static final String CONFIG_FILE = "config.cfg";
  public static final String MY_FILES_DIR = "myfiles/";
  public static final String OTHER_FILES_DIR = "otherfiles/";
  public static final String ADDRESS = "localhost";
  public static final int DEFAULT_TTL = 5;
  public static final int MESSAGE_CACHE = 10;

  private String address = Peer.ADDRESS;
  private int port;

  private ArrayList<PeerStub> neighbors;
  private ArrayList<DanFile> files;
  private LinkedList<MessagePair> messages;
  private LinkedList<MessageID> fileRequests;

  private int sequenceNumber = 0;
  private int TTL = DEFAULT_TTL;

  public static void main(String[] args){

    // Initialize the peer
    Peer peer = initializePeer();

    PeerSkeleton peerSkeleton = new PeerSkeleton(peer);

    new Thread(peerSkeleton::listen).start();

    if(peer == null){
      return;
    }

    // Run the command line interface
    runCLI(peer);
  }

  private static void runCLI(Peer peer) {
    System.out.println("Printing out all neighbors!");
    for(PeerStub p : peer.getNeighbors()){
      System.out.println(p.getFullAddress());
    }

    String prompt = String.format(Locale.getDefault(), "danutella-%d> ", peer.getPort());

    Scanner in = new Scanner(System.in);
    while(true){
      System.out.println("Please enter a command: get {filename}");
      System.out.print(prompt);

      String[] command = in.nextLine().split(" ");
      String function = command[0];

      if(function.equals("exit")){
        System.out.println("Shutting down peer. Goodbye!");
        System.exit(0); // End all threads
      } else if (function.equals("get")){
        if(command.length == 2){
          String filename = command[1];
          peer.get(filename);
          continue;
        }
      }

      System.out.println("Invalid Command!");
    }
  }

  private static Peer initializePeer(){

    // Read port numbers from config file
    // Line 1: This server's port
    // Remaining lines: Peer ports

    try {
      // Open config file
      File file = new File("config.cfg");
      BufferedReader br = new BufferedReader(new FileReader(file));

      // Read this peer's port number
      String line = br.readLine();
      int thisPort = Integer.parseInt(line);

      // Read neighboring peers line by line
      ArrayList<PeerStub> peers = new ArrayList<PeerStub>(); // Create empty list of peers
      while((line = br.readLine()) != null){ // Read line
        peers.add(new PeerStub(Integer.parseInt(line))); // Parse integer, create peer, add to neighbors
      }


      // Load list of files it contains

      File folder = new File("myfiles");
      File[] listOfFiles = folder.listFiles();

      ArrayList<DanFile> files = new ArrayList<DanFile>();
      for (int i = 0; i < listOfFiles.length; i++) {
        files.add(new DanFile(listOfFiles[i].getName(), true));
      }

      // Create this peer object
      return new Peer(thisPort, peers, files);

    } catch (Exception e){
      System.out.println("Error reading config file");
      e.printStackTrace();
      return null;
    }

  }

  // Constructors

  public Peer(int port, ArrayList<PeerStub> peers, ArrayList<DanFile> files){
    this.port = port;
    this.neighbors = peers;
    this.files = files;
    this.messages = new LinkedList<MessagePair>();
    this.fileRequests = new LinkedList<MessageID>();
  }


  // Command Line Options
  public void get(String filename){

    MessageID messageID = new MessageID(this.getFullAddress(), sequenceNumber++);

    // For each neighbor, query it.
    for(PeerStub neighbor : neighbors){
      neighbor.query(this.port, messageID, TTL, filename);
    }

  }

  // Inner Workings

  // When this peer GETS queried:
  public void query(int upstream, MessageID messageID, int TTL, String filename){
    if(messageID.getPeerID().equals(this.getFullAddress())){
      return;
    }

    MessagePair mp = new MessagePair(messageID, upstream);
    if(!messages.contains(mp)){
      if(messages.size() > MESSAGE_CACHE){
        messages.removeFirst();
      }
      messages.addLast(mp);
    } else {
      return;
    }

    if(this.hasFile(filename)){
      // Send hitQuery upstream
      PeerStub peerStub = new PeerStub(upstream);
      peerStub.hitQuery(messageID, DEFAULT_TTL, filename, this.getFullAddress());
    }

    // Propagate query
    // For each neighbor, query it.
    if(TTL > 0){
      for(PeerStub neighbor : neighbors){
        neighbor.query(this.port, messageID, TTL-1, filename);
      }
    }

  }

  public void hitQuery(MessageID messageID, int TTL, String filename, String address) {
    // First, check if this hitQuery is to me
    if(messageID.getPeerID().equals(this.getFullAddress())){
      int split = address.indexOf(':');
      int stubPort = Integer.parseInt(address.substring(split+2));
      PeerStub origin = new PeerStub(stubPort);
      if(origin.obtain(filename)){
        files.add(new DanFile(filename, false));
        System.out.println("Successfully downloaded " + filename + " from " + address);
      }
    } else {
      // Not mine, push upstream
      int upstreamPort = getUpstream(messageID);
      if(upstreamPort != -1 && TTL > 0){
        new PeerStub(upstreamPort).hitQuery(messageID, TTL-1, filename, address);
      }
    }
  }

  private int getUpstream(MessageID messageID) {
    for(MessagePair mp : messages){
      if(mp.getMessageID().equals(messageID)){
        return mp.getUpstream();
      }
    }
    return -1;
  }

  /**
   * Obtains a FileStream for the requested file.
   *
   * @param filename Name of the file to obtain
   * @return Returns the stream to the file, null if no file found
   */
  public FileInputStream obtain(String filename) {
    FileInputStream is = null;

    String dir = this.isOwner(filename) ? MY_FILES_DIR : OTHER_FILES_DIR;

    try {
      is = new FileInputStream(MY_FILES_DIR + filename); // Open file and grab stream
    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }

    return is;
  }

  // Private helpers

  private boolean hasFile(String filename){

    for(DanFile f : files){
      if(f.getFilename().equals((filename))){
        return true;
      }
    }

    return false;
  }

  private boolean isOwner(String filename) {

    for(DanFile f : files){
      if(f.getFilename().equals(filename)){
        return f.isOwner();
      }
    }

    return false;
  }

  // Getters and Setters

  public String getAddress(){
    return this.ADDRESS;
  }

  public int getPort(){
    return this.port;
  }

  public String getFullAddress(){
    return ADDRESS + ":" + this.port;
  }

  public ArrayList<PeerStub> getNeighbors(){
    return this.neighbors;
  }
}
