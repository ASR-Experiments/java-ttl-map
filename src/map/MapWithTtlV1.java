package src.map;

import src.utilities.Common;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
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
public class MapWithTtlV1<K, V> implements Map<K, V> {

    private static final Logger LOGGER;

    static {
        LOGGER = Common.getLogger(MapWithTtlV1.class);
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
    private record Value<V>(V value, Thread t) {
    }

    /**
     * The internal map that holds the keys and their associated values.
     */
    private final Map<K, Value<V>> internalMap = new HashMap<>();

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
        Value<V> newValue = new Value<>(value, new Thread(() -> {
            try {
                MapWithTtlV1.this.threadFactory(key);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
        Value<V> originalValue = internalMap.put(key, newValue);
        newValue.t.start();
        if (originalValue != null) {
            originalValue.t.interrupt();
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
            removedValue.t.interrupt();
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
                .collect(Collectors.toMap(
                        Entry::getKey,
                        e -> new Value<>(e.getValue(), new Thread(() -> {
                            try {
                                threadFactory(e.getKey());
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }))));
        internalMap.putAll(updatedMap);
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     * All threads associated with the keys are also interrupted.
     */
    @Override
    public void clear() {
        List<Thread> threadList = new ArrayList<>();
        internalMap.values().stream()
                .map(vValue -> vValue.t)
                .forEach(threadList::add);
        internalMap.clear();
        threadList.forEach(Thread::interrupt);
    }

    @Override
    public Set<K> keySet() {
        return internalMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return internalMap.values().stream()
                .map(Value::value).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Map<K, V> transformedMap = internalMap.entrySet()
                .stream()
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
    private void threadFactory(K key) throws InterruptedException {
        Thread.sleep(DEFAULT_TTL);
        internalMap.remove(key);
        LOGGER.info(() -> String.format(
                "Thread:%s (%s) => Key: %s removed",
                Common.getThreadName(),
                new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                key
        ));
    }

    private static void consumeThread(long timeInMs) {
        while (Instant.now().plusMillis(timeInMs).isAfter(Instant.now())) {
            // Do Nothing
        }
    }
}
