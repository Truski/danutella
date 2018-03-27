import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

/**
 * The Peer class is where most of the Gnutella implementation is located, because no central indexing server
 * exists. It also servers as an entry point for peers. A peer listens for incoming connections via a PeerSkeleton
 * and makes outgoing requests via PeerStubs.
 */

public class Peer {

  // Constants chosen for easy maintainability

  // Boolean constants
  public static final boolean DEBUG = true; // Set this flag to true to have 2-second pauses when a peer receives a message

  // String Constants
  public static final String CONFIG_FILE = "config.cfg"; // Name of the config file for the peer
  public static final String MY_FILES_DIR = "myfiles/"; // Name of directory where a peer's own files are kept
  public static final String OTHER_FILES_DIR = "otherfiles/"; // Name of directory where a peer's downloaded files are kept
  public static final String ADDRESS = "localhost"; // Default address to run on a single machine

  // Numeric Constants
  public static final int DEFAULT_TTL = 5; // Default number of hops for messages
  public static final int MESSAGE_CACHE = 10; // Default size of associative array for messages
  public static final long TTR = 1000 * 60; // Default length of TTR (time to refresh/poll)

  // Program configuration parameters
  public static final int PUSH = 0, PULL = 1;
  public static int MODE = PUSH;

  // Instance variables
  private PeerID ID; // The ID of the peer

  private ArrayList<PeerStub> neighbors; // Neighboring peers to connect with
  private ArrayList<DanFile> files; // List of files this peer contains
  private LinkedList<MessagePair> messages; // Associative mapping from messageID to upstream
  private LinkedList<MessageID> fileRequests; // List of files the peer is still waiting to get a hitQuery for

  private int sequenceNumber = 0; // This peer's current sequence number for messages
  private int TTL = DEFAULT_TTL; // The TTL this peers' files have for pull-based consistency

  /**
   * Entry point for Peer. Initializes the peer by reading the config file, then
   * starts listening for incoming requests and runs the command line interface
   * for users to enter commands.
   * @param args
   */
  public static void main(String[] args){

    // Read command line arguments if there are any (push vs pull)
    if(args.length == 0){
      System.out.println("No consistency configuration parameter provided. Using push-based consistency.");
    } else {
      if(args[0].equals("push")){
        MODE = PUSH;
      } else if(args[0].equals("pull")){
        MODE = PULL;
      } else {
        System.out.println("Invalid command line arguments. Defaulting to push-based consistency.");
      }
    }

    // Initialize the peer
    Peer peer = initializePeer();


    // Exit if the peer failed to initialize
    if(peer == null){
      return;
    }

    // Create a PeerSkeleton to service other peers through
    PeerSkeleton peerSkeleton = new PeerSkeleton(peer);

    // Start listening for incoming connections on a separate thread
    new Thread(peerSkeleton::listen).start();

    // If it's set to PULL mode create and deploy a lazy polling thread
    if(MODE == PULL){
      LazyPoller lazyPoller = new LazyPoller(peer);
      new Thread(lazyPoller::autoPoll).start();
    }

    // Run the command line interface
    runCLI(peer);
  }

  /**
   * Function to run the command line interface. Options include:
   * - get {filename} - starts a query through the network to download a file
   * - refresh - refreshes all files that are expired (pull-based)
   * - refresh {filename} - specifically refresh this file
   * @param peer
   */
  private static void runCLI(Peer peer) {

    // Create prompt meesage which contains the peer's poort
    String prompt = String.format(Locale.getDefault(), "danutella-%d> ", peer.getPort());

    // Create scanner to scan user input
    Scanner in = new Scanner(System.in);

    // Display options for peer
    System.out.println("Please enter a command: get {filename}, files, edit {filename}, refresh, ? for help");

    // Keep asking for commands until user types exit
    while(true){
      // Print out prompt
      System.out.print(prompt);

      // Parse command
      String[] command = in.nextLine().split(" ");
      String function = command[0];

      // Check if user input is a valid command
      if(function.equals("exit")){
        // Shut down the peer by informing the user and ending all threads
        System.out.println("Shutting down peer. Goodbye!");
        System.exit(0); // End all threads
      } else if (function.equals("get")){
        if(command.length == 2){
          // Parse filename from command and query neighbors for filename
          String filename = command[1];
          peer.get(filename);
          continue;
        }
      } else if (function.equals("edit")){
        if(command.length == 2){
          // Parse filename from command and query neighbors for filename
          String filename = command[1];
          peer.edit(filename);
          continue;
        }
      } else if(function.equals("files")){
        // List all files
        peer.listFiles();
        continue;
      } else if(function.equals("refresh")){
        // Refresh invalid files
        peer.refresh();
        continue;
      } else if(function.equals("?")){
        // Display help message
        System.out.println("Please enter a command: get {filename}, files, edit {filename}, refresh, ? for help");
        continue;
      }

      // Print error message for invalid command
      System.out.println("Invalid Command!");
    }
  }

  /**
   * Simulates an edit on a file
   * @param filename the name of the file to virtually edit
   */
  private void edit(String filename) {
    // Get the file
    DanFile danFile = getDanFile(filename);

    // Make sure that this peer owns it
    if(danFile != null && danFile.isOwner(this)){

      // Increment the version and set a new modified time
      danFile.setVersion(danFile.getVersion()+1);
      danFile.setLastModifiedTime(System.currentTimeMillis());

      // Check if we're in PUSH mode
      if(MODE == PUSH){
        // Show user that it is being invalidated
        System.out.println("Invalidating " + filename + "; new version: " + danFile.getVersion());

        // Broadcast an invalidation message to all neighboring peers
        MessageID messageID = new MessageID(ID, sequenceNumber++);
        for(PeerStub peerStub : neighbors){
          peerStub.invalidate(messageID, ID, filename, danFile.getVersion());
        }
      }

    } else {
      // Inform user he cannot edit a file he doesn't own
      System.out.println("You cannot edit" + filename + "; you must have and own the file.");
    }
  }

  /**
   * Lists all files that a peer has access to
   */
  private void listFiles() {
    System.out.println("List of Files:");
    // Loops through each file
    for(DanFile danFile : files){
      // Prints out its details
      System.out.println(danFile.print(this));
    }
  }

  private void refresh() {
    // Update Invalid files directly
    for(DanFile df : files){
      // Check if it is invalid
      if(df.getConsistency() == DanFile.INVALID){
        // Create PeerStub to interact with the Peer that owns the file
        PeerStub origin = new PeerStub(df.getOriginServer());

        // Download file from that peer
        DanFile danFile = origin.obtain(df.getFilename());

        if(danFile != null){
          // Since we already had it, update parameters
          df.setVersion(danFile.getVersion());
          df.setLastModifiedTime(danFile.getLastModifiedTime());
          df.setConsistency(danFile.getConsistency());
          df.setTTR(danFile.getTTR());
          df.setLastPolledTime(System.currentTimeMillis());

          System.out.println("Successfully refreshed " + df.getFilename());


          // Print file contents if small file
          try {
            long length = new File(OTHER_FILES_DIR + df.getFilename()).length();
            System.out.println("File size: " + length + " bytes.");
            if(length > 1024){
              System.out.println("File is larger than 1K - will not display.");
            } else {
              FileInputStream fileInputStream = new FileInputStream(OTHER_FILES_DIR + df.getFilename());
              byte[] buffer = new byte[1024];
              fileInputStream.read(buffer, 0, 1024);
              System.out.println("<==|== Start of File Contents ==||==>");
              System.out.println(new String(buffer));
              System.out.println("<==|==  End of File Contents  ==||==>");
            }
          } catch (Exception e){
            e.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Initializes a Peer by reading the config file. The config file contains the Peer's port as well as its
   * neighboring ports. Also, check master directory for files this peer owns.
   * @return The initialized Peer
   */
  private static Peer initializePeer(){

    try {
      // Open config file and setup reader
      //   Config file format:
      //     Line 1: This server's port
      //     Remaining lines: Peer ports
      File file = new File("config.cfg");
      BufferedReader br = new BufferedReader(new FileReader(file));

      // Read this peer's port number
      String line = br.readLine();
      int thisPort = Integer.parseInt(line);

      // Create this Peer's PeerID
      PeerID ID = new PeerID(thisPort);

      ArrayList<PeerStub> peers = new ArrayList<PeerStub>(); // Create empty list of neighboring peers

      // Read neighboring peers line by line
      while((line = br.readLine()) != null){ // Read line
        int port = Integer.parseInt(line); // Parse integer port
        PeerID id = new PeerID(port); // Create peer ID based on port
        peers.add(new PeerStub(id)); // Add new PeerStub to neighbor list
      }


      // Load list of files it contains
      File folder = new File(MY_FILES_DIR); // Open my files directory
      File[] listOfFiles = folder.listFiles(); // Get list of files in directory

      ArrayList<DanFile> files = new ArrayList<DanFile>(); // Create empty list of files

      // Add each of files to list
      for (int i = 0; i < listOfFiles.length; i++) {

        // Create File and set proper attributes
        DanFile danFile = new DanFile(listOfFiles[i].getName()); // Create file with name
        danFile.setOriginServer(ID); // Set the origin server to be this peer
        danFile.setVersion(0); // Original version is 0
        danFile.setConsistency(DanFile.VALID); // File is valid when created
        danFile.setLastModifiedTime(System.currentTimeMillis()); // Now is the last modified time
        danFile.setLastPolledTime(-1); // A negative last polled time means this is the master file
        danFile.setTTR(Peer.TTR); // Set the time to refresh as default time to refresh

        // Add file to list of files
        files.add(danFile);
      }

      // Create this peer object with given ID, peers, and own files
      return new Peer(ID, peers, files);

    } catch (Exception e){
      // Warn user of incorrect configuration and return null
      System.out.println("Error reading config file");
      return null;
    }

  }

  // ************
  // Constructors
  // ************

  /**
   * Creates a Peer with the given ID, neighbors, and own files. Used in Peer Initialization
   * @param ID The ID of the Peer
   * @param peers The neighbor stubs the peer can connect to
   * @param files The list of the peer's own initial files
   */
  public Peer(PeerID ID, ArrayList<PeerStub> peers, ArrayList<DanFile> files){
    this.ID = ID;
    this.neighbors = peers;
    this.files = files;
    this.messages = new LinkedList<MessagePair>();
    this.fileRequests = new LinkedList<MessageID>();
  }

  // ********************
  // Command Line Options
  // ********************

  /**
   * This function is called by the user via the command line. The user intends to download a file, so
   * the Peer must contact all neighbors to see if they have the file. Also, the messageID is added
   * to the list of outgoing file requests so that only one copy of the file is downloaded.
   * @param filename Name of the file this peer is requesting
   */
  public synchronized void get(String filename){

    // Create the message ID that will be propagated through the network
    MessageID messageID = new MessageID(ID, sequenceNumber++); // Increment sequence number for uniqueness

    // Add this message to list of ongoing file requests
    fileRequests.add(messageID);

    // For each neighbor, query it
    for(PeerStub neighbor : neighbors){
      neighbor.query(this.ID, messageID, TTL, filename);
    }

  }

  // **********************************************
  // Peer Remote Procedure Calls / Message Handlers
  // **********************************************

  /**
   * This method is called when a query message is received by this peer. First it must check that it's
   * not receiving its own message or if it has already seen this message before. Then, it checks if it contains
   * the requested file. If it does, it sends a hitQuery to the upstream. Regardless, it sends the query outwards to its
   * own peers.
   * @param upstream The ID of the peer where to send hitQueries if we have the file
   * @param messageID The messageID for tracking and mapping purposes
   * @param TTL The remaining time to live for the message
   * @param filename The name of the file being searched for
   */
  public synchronized void query(PeerID upstream, MessageID messageID, int TTL, String filename){

    // Check if this peer is the origin of the message.
    if(messageID.getPeerID().equals(this.ID)){
      // If so, ignore it
      return;
    }

    // Check to see if the peer has already received this message
    if(!seenQuery(messageID)){
      // If not, add it to the associate cache

      // If the cache is too large, delete the first-in message
      if(messages.size() >= MESSAGE_CACHE){
        messages.removeFirst();
      }

      // Create the mapping and at it to the cache
      MessagePair mp = new MessagePair(messageID, upstream);
      messages.addLast(mp);
    } else {
      // If this peer has already seen this message, ignore it
      return;
    }

    // Inform the user that a query has been received
    System.out.println("Query from " + upstream + " for " + filename + ".");
    sleep();

    // Check to see if this peer is able to share this file
    if(this.hasFile(filename)){
      // If so, create and set a hitQuery upstream
      System.out.println("Found it! Sending hitQuery to " + upstream);
      PeerStub peerStub = new PeerStub(upstream);
      peerStub.hitQuery(messageID, this.TTL, filename, ID);
    }

    // If the TTL is not zero, propagate query forward
    if(TTL > 0){
      // For each neighbor, query it
      for(PeerStub neighbor : neighbors){
        neighbor.query(ID, messageID, TTL-1, filename); // Reduce TTL by 1
      }
    }
  }

  /**
   * This method is called when the peer receives a hitQuery message. There are two possibilities:
   *   If the hitQuery is for a file this peer requested, download the file from the peer.
   *   If the hitQuery is for a file this peer DID NOT request, propogate it upstream.
   *   If the hitQuery is for a file this peer requested BUT has already received, ignore it.
   * @param messageID The messageID that the query issuer created
   * @param TTL The remaining hops for this message
   * @param filename The name of the file wanting to be downloaded
   * @param address The location where the file can be downloaded from
   */
  public synchronized void hitQuery(MessageID messageID, int TTL, String filename, PeerID address) {
    // Announce that a new hitquery has come in
    System.out.println("Received hitQuery from " + address + " meant for " + messageID.getPeerID() + ".");
    sleep();

    // First, check if this hitQuery is to me
    if(messageID.getPeerID().equals(ID)){

      // Check to see if this peer has already downloaded this file
      if(!fileRequests.contains(messageID)){
        // Ignore the message if the file is no longer wanted
        System.out.println("Receieved hitQuery from " + address + ", but I already got " + filename + ".");
        return;
      }

      // Remove this file from the list of files this peer is waiting for
      fileRequests.remove(messageID);

      // Create PeerStub to interact with the Peer that has the file
      PeerStub origin = new PeerStub(address);

      // Download file from that peer
      DanFile danFile = origin.obtain(filename);

      if(danFile != null){
        // Add this file to this peer's list of files

        // First, check if we already had this file
        DanFile old = getDanFile(filename);
        if(old != null){
          // If we already had it, update parameters
          old.setVersion(danFile.getVersion());
          old.setLastModifiedTime(danFile.getLastModifiedTime());
          old.setConsistency(danFile.getConsistency());
          old.setTTR(danFile.getTTR());
          old.setLastPolledTime(danFile.getLastPolledTime());
          danFile = old;
        } else {
          // Otherwise, add it to the list of files
          files.add(danFile);
        }

        // If this file was downloaded from the origin server, it counts as if it was just polled.
        if(danFile.getOriginServer().equals(address)){
          danFile.setLastPolledTime(System.currentTimeMillis());
        }

        System.out.println("Successfully downloaded " + filename + " from " + address + ".");

        // Print file contents if small file
        try {
          long length = new File(OTHER_FILES_DIR + filename).length();
          System.out.println("File size: " + length + " bytes.");
          if(length > 1024){
            System.out.println("File is larger than 1K - will not display.");
          } else {
            FileInputStream fileInputStream = new FileInputStream(OTHER_FILES_DIR + filename);
            byte[] buffer = new byte[1024];
            fileInputStream.read(buffer, 0, 1024);
            System.out.println("<==|== Start of File Contents ==||==>");
            System.out.println(new String(buffer));
            System.out.println("<==|==  End of File Contents  ==||==>");
          }
        } catch (Exception e){
          e.printStackTrace();
        }
      }
    } else {
      // If this is not a hitQuery to this Peer, send it upstream

      // Get upstream from the associative list
      PeerID upstreamPort = getUpstream(messageID);

      // If there was a mapping in the associative list and the TTL is above zero, propagate it upstream
      if(upstreamPort != null && TTL > 0){
        // Create PeerStub and send hitQuery message
        new PeerStub(upstreamPort).hitQuery(messageID, TTL-1, filename, address); // Decrement TTL
      }
    }
  }

  /**
   * Obtains a FileStream for the requested file.
   *
   * @param filename Name of the file to obtain
   * @return Returns the stream to the file, null if no file found
   */
  public synchronized FileInputStream obtain(String filename) {
    // Announce sending file
    System.out.println("Sending file " + filename + " directly over the network.");
    sleep();

    FileInputStream is = null; // The stream to be returned

    // Set directory based on if this peer is the owner of the file or not
    String dir = this.isOwner(filename) ? MY_FILES_DIR : OTHER_FILES_DIR;

    try {
      is = new FileInputStream(dir + filename); // Open file and grab stream
    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }

    // Return the input stream for file uploading
    return is;
  }

  /**
   * This method is called when the peer receives an invalidate message. Propagates just like a query message, except that
   * there is no TTL to ensure utmost consistency.
   * @param messageID The ID of the message to disable infinite propagation
   * @param originServer The server that originally sent the invalidation message
   * @param filename The name of the file that is now invalid
   * @param version The new version number of the file
   */
  public synchronized void invalidate(MessageID messageID, PeerID originServer, String filename, int version){

    // Ignore the message if this peer was the creator of it
    if(messageID.getPeerID().equals(ID)){
      return;
    }

    // Check to see if this peer has already seen this message
    if(!seenQuery(messageID)){
      // If not, add it to the associate cache

      // If the cache is too large, remove the first-come message pair
      if(messages.size() >= MESSAGE_CACHE){
        messages.removeFirst();
      }

      // Create the message pair and add it to the cache
      MessagePair mp = new MessagePair(messageID, null); // Upstream not important in invalidation
      messages.addLast(mp);
    } else {
      // If so, ignore it
      return;
    }

    // Announce that invalidation message was received
    System.out.println("Invalidation received! " + filename + " version is now " + version);
    sleep();

    // Check if this peer has this file
    DanFile file = this.getDanFile(filename);
    if(file != null){
      System.out.println("Invalidation successful.");
      // If the new version > current version, invalidate the file (don't discard it though as it could still be wanted by the user
      if(file.getVersion() < version){
        file.invalidate();
      }
    }

    // Propagate invalidation message
    // For each neighbor, query it.
    for(PeerStub neighbor : neighbors){
      neighbor.invalidate(messageID, originServer, filename, version);
    }
  }

  /**
   * When a peer receives a poll request, it checks to see if the current version is newer and returns a reply to update
   * the TTR if necessary or to invalidate the file.
   * @param version the version that the other peer has the file of
   * @param filename the name of the file to check
   * @return a PollResult object for the other peer to process
   */
  public synchronized PollResult poll(int version, String filename){
    // Get the DanFile representation of the file with the given name
    DanFile danFile = getDanFile(filename);

    // Check to see if the current version is newer
    if(danFile.getVersion() > version){
      // If it is newer, return an outOfDate = True result
      return new PollResult(true, 0);
    } else {
      // If they have the same version, give it a new TTR
      return new PollResult(false, TTR);
    }
  }


  /**
   * Gets the DanFile from the list of files with the given name
   * @param filename name of the file to get the DanFile for
   * @return The DanFile to work with, null if the name doesn't exist
   */
  public DanFile getDanFile(String filename){
    // Loop through all dan files
    for(DanFile df : files){
      if(df.getFilename().equals(filename)){
        // If the name matches, return it
        return df;
      }
    }

    // DanFile with that name doesn't exist
    return null;
  }

  /**
   * This function is used for the Poll-based approach to consistency. Every so often the lazy poller will call this
   * method. It checks each file to see if its ttr is expired. If it is expired, it will poll the origin server. If
   * it is out of date, the file will be invalidated. Otherwise, it will receive a new TTR.
   */
  public synchronized void lazyPoll(){
    // Loop through every file on this peer
    for(DanFile danFile : files){

      // Do not poll for own file
      if(danFile.isOwner(this)) continue;

      // Update the TTR status of the file
      danFile.checkTTR();

      // Check if the TTR is expired
      if(danFile.isExpired()){
        // If it is expired, poll the origin server
        PeerStub originServer = new PeerStub(danFile.getOriginServer()); // Create a stub to connect to
        PollResult result = originServer.poll(danFile.getVersion(), danFile.getFilename()); // poll the origin server

        // If the result is out of date, invalidate the file
        if(result.isOutOfDate()){
          System.out.println("Invalidating " + danFile.getFilename() + ". The poll gave back a newer version.");
          danFile.invalidate();
        } else {
          // Otherwise, just update it with the new TTR
          danFile.updateTTR(result.getNewTTR());
          // Update it to valid once again
          danFile.setConsistency(DanFile.VALID);
        }
      }
    }
  }


  // **********************
  // Private Helper Methods
  // **********************


  /**
   * Checks if this message has already been seen to limit infinite talk-back
   * @param messageID The messageID to check
   * @return True if we've already seen the message
   */
  private boolean seenQuery(MessageID messageID){
    // Loop through all message-upstream pairs
    for(MessagePair mp : messages){
      if(mp.getMessageID().equals(messageID)){
        return true;
      }
    }

    return false;
  }

  /**
   * Sleeps 2 seconds to visibly show trace of messages. Only does anything if debug mode is enabled
   */
  private void sleep(){
    if(!DEBUG){
      return; // Do nothing in non-debug mode
    }
    try {
      Thread.sleep(2000); // Sleep 2 seconds in debug mode
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  /**
   * Checks to see if the given file is available for download from this peer.
   * @param filename Name of the file to check if available
   * @return True if the peer has a valid copy of the file
   */
  private boolean hasFile(String filename){
    // Loop through all files in list of files
    for(DanFile f : files){
      // Check that it has the right name
      if(f.getFilename().equals((filename))){
        // Check to see if this file is valid
        if(f.isValid()){
          return true; // If so, return true
        } else {
          return false; // Invalid file, return false
        }
      }
    }

    return false; // This peer doesn't have the requested file
  }

  /**
   * Check to see if this peer is the owner of the file with the given name
   * @param filename Name of file to check
   * @return True if this peer is the owner
   */
  private boolean isOwner(String filename) {
    // Loop through all the files the peer has
    for(DanFile f : files){
      // Check if the file has the right name
      if(f.getFilename().equals(filename)){
        return f.isOwner(this); // Check whether this file is specifically own by this peer and return result
      }
    }

    return false; // This peer doesn't even have the file
  }

  /**
   * Given a messageID, provide the upstream PeerID for back-tracing
   * @param messageID The message to check the associative list for
   * @return The upstream peer, or null if the message isn't in the cache
   */
  private PeerID getUpstream(MessageID messageID) {
    for(MessagePair mp : messages){
      if(mp.getMessageID().equals(messageID)){
        return mp.getUpstream();
      }
    }
    return null;
  }

  // *******************
  // Getters and Setters
  // *******************

  // Comments omitted because this is generic, boilerplate code

  public String getAddress(){
    return this.ID.getAddress();
  }

  public int getPort(){
    return this.ID.getPort();
  }

  public String getFullAddress(){
    return ID.toString();
  }

  public ArrayList<PeerStub> getNeighbors(){
    return this.neighbors;
  }
}
