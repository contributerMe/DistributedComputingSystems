
public class LogicalClock {
  private int value = 0;
  
  public synchronized int getValue() {
      return value;
  }
  
  public synchronized int increment() {
      return ++value;
  }
  
  public synchronized void update(int receivedTimestamp) {
      value = Math.max(value, receivedTimestamp) + 1;
  }
}
