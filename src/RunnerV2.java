package src;

import src.map.MapWithTtlV3;
import src.utilities.Common;

import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Runner programmed to load test the MapWithTtlV1 class.
 */
public class RunnerV2 {

    private static final Logger LOGGER;

    static {
        LOGGER = Common.getLogger(RunnerV2.class);
    }


    public static void main(String[] args) throws InterruptedException {
        MapWithTtlV3<Integer, String> testMap = new MapWithTtlV3<>();
        Scanner sc = new Scanner(System.in);
        LOGGER.warning("Enter the number of elements to be added to the map:");
        final int n = sc.nextInt();
        for (int i = 0; i < n; i++) {
            testMap.put(i, "Value" + i);
        }
        int i = 0;
        do {
            for (int j = 0; j < n; j++) {
                // Try getting it after TTL
                int finalJ = j;
                LOGGER.info(() -> String.format(
                        "Thread:%s => Key: %d, Value: %s",
                        Common.getThreadName(),
                        finalJ,
                        testMap.get(finalJ)
                ));
            }
            Thread.sleep(5000);
            i++;
        } while (i < 5);
        LOGGER.info("Exiting RunnerV2 ...");
    }
}
