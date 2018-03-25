import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class MasterCopyGenerator {
  public static void main(String[] args){
    int peerMin = 0;
    int peerMax = 9;

    int fileMin = 1;
    int fileMax = 10;

    // Generate a folder and 10 files per folder for each peer
    for(int i = peerMin; i <= peerMax; i++){
      String dirname = "peer" + i + "/myfiles";
      String otherdirname = "peer" + i + "/otherfiles";
      new File(dirname).mkdir();
      new File(otherdirname).mkdir();
      for(int j = fileMin; j <= fileMax; j++){
        String line = "This is peer " + i + "'s file of size " + j + "K. ";
        int size = line.length();
        int iterations = j * 1024 / size;

        try {
          String filename = dirname + "/" + "p" + i + "-" + j + "k.txt";
          BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
          for(int k = 0; k < iterations; k++){
            writer.write(line);
          }
          writer.close();
        } catch (Exception e){
          System.out.println("Unable to create " + i + " - " + j);
          e.printStackTrace();
        }

      }
    }
  }
}
