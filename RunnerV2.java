import java.text.SimpleDateFormat;
import java.util.Scanner;

/**
 * Runner programmed to load test the MapWithTtlV1 class.
 */
public class RunnerV2 {
    public static void main(String[] args) throws InterruptedException {
        MapWithTtlV1<Integer, String> testMap = new MapWithTtlV1<>();
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the number of elements to be added to the map:");
        final int n = sc.nextInt();
        for (int i = 0; i < n; i++) {
            testMap.put(i, "Value" + i);
        }
        int i = 0;
        do {
            for (int j = 0; j < n; j++) {
                // Try getting it after TTL
                System.out.printf(
                        "Thread:%s at (%s) => Key: %d, Value: %s\n",
                        Thread.currentThread().getName(),
                        new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()),
                        j,
                        testMap.get(j)
                );
            }
            Thread.sleep(5000);
            i++;
        } while (i < 5);
        System.out.println("Exiting RunnerV2 ...");
    }
}
