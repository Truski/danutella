import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class Peer {

  public static final String configFile = "config.cfg";
  public static final String myFilesDir = "myfiles/";
  public static final String otherFilesDir = "otherfiles/";

  private String address = "localhost";
  private int port;

  private ArrayList<Peer> neighbors;

  public static void main(String[] args){
    // Read port numbers from config file
    // Line 1: This server's port
    // Remaining lines: Peer ports

    Peer peer;

    try {
      // Open config file
      File file = new File("config.cfg");
      BufferedReader br = new BufferedReader(new FileReader(file));

      // Read this peer's port number
      String line = br.readLine();
      int thisPort = Integer.parseInt(line);

      // Read neighboring peers line by line
      ArrayList<Peer> peers = new ArrayList<Peer>(); // Create empty list of peers
      while((line = br.readLine()) != null){ // Read line
        peers.add(new Peer(Integer.parseInt(line))); // Parse integer, create peer, add to neighbors
      }

      // Create this peer object
      peer = new Peer(thisPort, peers);

    } catch (Exception e){
      System.out.println("Error reading config file");
      e.printStackTrace();
      return;
    }

    // Run the command line interface
    runCLI(peer);
  }

  private static void runCLI(Peer peer) {
    System.out.println("Printing out all neighbors!");
    for(Peer p : peer.getNeighbors()){
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
        break; // Exit out of loop, ending program
      } else if (function.equals("get")){
        if(command.length == 2){
          String filename = command[1];
          peer.get(filename);
          continue;
        }
      }

      System.out.println("Invalid Command!");

    }

    System.out.println("Shutting down peer. Goodbye!");
  }

  // Constructors

  public Peer(int port){
    this.port = port;
  }

  public Peer(int port, ArrayList<Peer> peers){
    this.port = port;
    this.neighbors = peers;
  }


  // Command Line Options
  public void get(String filename){
    // For each neighbor, query it.


  }

  // Inner Workings

  public void query(String filename){

  }

  /**
   * Obtains a FileStream for the requested file.
   *
   * @param filename Name of the file to obtain
   * @return Returns the stream to the file, null if no file found
   */
  public FileInputStream obtain(String filename) {
    FileInputStream is = null;

    try {
      is = new FileInputStream(myFilesDir + filename); // Open file and grab stream
    } catch (Exception e){
      e.printStackTrace(); // An error occurred
    }

    return is;
  }

  // Getters and Setters

  public String getAddress(){
    return this.address;
  }

  public int getPort(){
    return this.port;
  }

  public String getFullAddress(){
    return address + ":" + this.port;
  }

  public ArrayList<Peer> getNeighbors(){
    return this.neighbors;
  }
}
