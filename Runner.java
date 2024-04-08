public class Runner {
    public static void main(String[] args) throws InterruptedException {
        MapWithTtlV1<Integer, String> testMap = new MapWithTtlV1<>();
        testMap.put(1, "First");
        // Try getting it before TTL
        System.out.printf("Key: %d, Value: %s\n", 1, testMap.get(1));
        // Wait for TTL
        Thread.sleep(MapWithTtlV1.DEFAULT_TTL + 1);
        // Try getting it after TTL
        System.out.printf("Key: %d, Value: %s\n", 1, testMap.get(1));
    }
}
