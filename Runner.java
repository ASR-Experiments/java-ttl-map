import java.text.SimpleDateFormat;
import java.time.Instant;

public class Runner {
    public static void main(String[] args) throws InterruptedException {
        MapWithTtlV1<Integer, String> testMap = new MapWithTtlV1<>();
        testMap.put(1, "First");
        testMap.put(2, "Second");
        int i = 0;
        do {
            // Try getting it after TTL
            System.out.printf(
                    "Thread:%s at (%s) => Key: %d, Value: %s\n",
                    Thread.currentThread().getName(),
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()),
                    1,
                    testMap.get(1)
            );
            System.out.printf(
                    "Thread:%s at (%s) => Key: %d, Value: %s\n",
                    Thread.currentThread().getName(),
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()),
                    2,
                    testMap.get(2)
            );
            // Wait for TTL
            Thread.sleep(10000);
            i++;
        } while (i <= 6);
    }
}
