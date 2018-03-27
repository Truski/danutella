/**
 * THe LazyPoller only is created and run on a separate thread when PULL mode is activated. It calls the peer's
 * lazy poll method every period of time, which is a provided constant in this file.
 */
public class LazyPoller {
  private static final int LAZY_POLL_FREQUENCY = 1000 * 15; // How often to poll, defaulted at every 15 seconds
  private Peer peer;

  public LazyPoller(Peer peer) {
    this.peer = peer;
  }

  /**
   * The method that a thread will run. Runs the peer's lazy-polling feature and then sleeps.
   */
  public void autoPoll(){
    while(true){
      // Sleep first - no need to instantly poll
      try {
        Thread.sleep(LAZY_POLL_FREQUENCY);
      } catch (Exception e){
        e.printStackTrace();
      }

      System.out.println("Automatically polling...");
      peer.lazyPoll();
    }
  }
}
