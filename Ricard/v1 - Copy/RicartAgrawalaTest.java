public class RicartAgrawalaTest {
  public static void main(String[] args) {
      // Create processes
      Process p1 = new Process(1);
      Process p2 = new Process(2);
      Process p3 = new Process(3);
      
      // Connect processes
      p1.addProcess(p2);
      p1.addProcess(p3);
      p2.addProcess(p1);
      p2.addProcess(p3);
      p3.addProcess(p1);
      p3.addProcess(p2);
      
      System.out.println("Starting Ricart-Agrawala mutual exclusion algorithm test");
      
      // Start threads for each process
      Thread t1 = new Thread(() -> {
          try {
              Thread.sleep(100);
              p1.requestCriticalSection();
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
      });
      
      Thread t2 = new Thread(() -> {
          try {
              Thread.sleep(200);
              p2.requestCriticalSection();
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
      });
      
      Thread t3 = new Thread(() -> {
          try {
              Thread.sleep(300);
              p3.requestCriticalSection();
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
      });
      
      t1.start();
      t2.start();
      t3.start();
      
      try {
          t1.join();
          t2.join();
          t3.join();
      } catch (InterruptedException e) {
          e.printStackTrace();
      }
      
      System.out.println("Test completed");
  }
}
