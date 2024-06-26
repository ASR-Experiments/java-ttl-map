package src.map;

import src.utilities.Common;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class represents a Map with a Time-To-Live (TTL) feature.
 * Each key-value pair in the map will be automatically removed after a certain period of time.
 * The default TTL is 60000 milliseconds (1 minute).
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class MapWithTtlV3<K, V> implements Map<K, V> {

    private static final Logger LOGGER;

    static {
        LOGGER = Common.getLogger(MapWithTtlV3.class);
    }

    /**
     * The default TTL in milliseconds.
     */
    public static final int DEFAULT_TTL = 15000;

    /**
     * A record that holds the value and the thread associated with it.
     *
     * @param <V> the type of the value
     */
    private record Value<V>(V value, Future<?> t, Instant validTill) {
    }

    /**
     * The internal map that holds the keys and their associated values.
     */
    private final Map<K, Value<V>> internalMap = new HashMap<>();

    private final ScheduledExecutorService executor =
            new ScheduledThreadPoolExecutor(12);

    @Override
    public int size() {
        return internalMap.size();
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return internalMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return internalMap.values().stream()
                .anyMatch(mapValue -> mapValue.value == value);
    }

    @Override
    public V get(Object key) {
        Value<V> potentialValue = internalMap.get(key);
        if (potentialValue != null && Instant.now().isAfter(potentialValue.validTill)) {
            LOGGER.warning(() -> String.format(
                    "Thread:%s => Key: %s doesn't exist",
                    Common.getThreadName(),
                    key));
            return null;
        }
        return potentialValue == null ? null : potentialValue.value;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is replaced and the old thread is interrupted.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    @Override
    public V put(K key, V value) {
        Value<V> newValue = new Value<>(
                value,
                executor.schedule(
                        () -> removeKey(key),
                        DEFAULT_TTL,
                        TimeUnit.MILLISECONDS
                ),
                Instant.now().plusMillis(DEFAULT_TTL)
        );
        Value<V> originalValue = internalMap.put(key, newValue);
        if (originalValue != null) {
            originalValue.t.cancel(true);
            return originalValue.value;
        }
        return null;
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     * The thread associated with the key is also interrupted.
     *
     * @param key the key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    @Override
    public V remove(Object key) {
        Value<V> removedValue = internalMap.remove(key);
        if (removedValue != null) {
            removedValue.t.cancel(true);
            return removedValue.value;
        }
        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * The effect of this call is equivalent to that of calling put(k, v) on this map once for each mapping from key k to value v in the specified map.
     *
     * @param m mappings to be stored in this map
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Map<K, Value<V>> updatedMap = m.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Entry::getKey,
                                e -> new Value<>(
                                        e.getValue(),
                                        executor.schedule(
                                                () -> removeKey(e.getKey()),
                                                DEFAULT_TTL,
                                                TimeUnit.MILLISECONDS
                                        ),
                                        Instant.now().plusMillis(DEFAULT_TTL)
                                )
                        )
                );
        internalMap.putAll(updatedMap);
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     * All threads associated with the keys are also interrupted.
     */
    @Override
    public void clear() {
        List<Future<?>> threadList = new ArrayList<>();
        internalMap.values().stream()
                .map(vValue -> vValue.t)
                .forEach(threadList::add);
        internalMap.clear();
        threadList.forEach(o -> o.cancel(true));
    }

    @Override
    public Set<K> keySet() {
        return internalMap.entrySet().stream()
                .filter(vValue -> Instant.now().isBefore(vValue.getValue().validTill))
                .map(Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<V> values() {
        return internalMap.values().stream()
                .filter(vValue -> Instant.now().isBefore(vValue.validTill))
                .map(Value::value)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Map<K, V> transformedMap = internalMap.entrySet()
                .stream()
                .filter(vValue -> Instant.now().isBefore(vValue.getValue().validTill))
                .collect(Collectors.toMap(
                        Entry::getKey,
                        e -> e.getValue().value));
        return transformedMap.entrySet();
    }

    /**
     * A factory method to create a thread that sleeps for the default TTL and then removes the key from the map.
     *
     * @param key the key to be removed after the default TTL
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    private void ttlLogic(K key) throws InterruptedException {
        Thread.sleep(DEFAULT_TTL);
        removeKey(key);
    }

    private void removeKey(K key) {
        Instant start = Instant.now();
        consumeThread(1000, start);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        internalMap.remove(key); // ~ns
        LOGGER.info(() -> String.format(
                "Thread:%s => Key: %s removed due to TTL.",
                Common.getThreadName(),
                key
        ));
    }

    private static void consumeThread(long timeInMs, Instant start) {
        while (start.plusMillis(timeInMs).isAfter(Instant.now())) {
            // Do Nothing
        }
    }
}
