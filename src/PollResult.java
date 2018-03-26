import java.io.Serializable;

/**
 * This class is used for Polling messages. It is a self explanatory container used to convey if a file is out of date
 * and if not, provide a new TTR for the file to update on.
 */
public class PollResult implements Serializable{
  private boolean outOfDate;
  private long newTTR;

  // ****************
  // Boilerplate Code
  // ****************

  // Comments omitted because this is generic, boilerplate code

  public PollResult(boolean outOfDate, long newTTR) {
    this.outOfDate = outOfDate;
    this.newTTR = newTTR;
  }

  public boolean isOutOfDate() {
    return outOfDate;
  }

  public void setOutOfDate(boolean outOfDate) {
    this.outOfDate = outOfDate;
  }

  public long getNewTTR() {
    return newTTR;
  }

  public void setNewTTR(long newTTR) {
    this.newTTR = newTTR;
  }
}
