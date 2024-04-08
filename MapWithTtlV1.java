import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MapWithTtlV1<K, V> implements Map<K, V> {

    public static final int DEFAULT_TTL = 60000;

    private record Value<V>(V value, Thread t) {
    }

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

    @Override
    public V put(K key, V value) {
        Value<V> newValue = new Value<>(value, new Thread(() -> {
            try {
                threadFactory(key);
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

    @Override
    public V remove(Object key) {
        Value<V> removedValue = internalMap.remove(key);
        if (removedValue != null) {
            removedValue.t.interrupt();
            return removedValue.value;
        }
        return null;
    }

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

    private void threadFactory(K key) throws InterruptedException {
        Thread.sleep(DEFAULT_TTL);
        internalMap.remove(key);
        System.out.printf(
                "Thread:%s (%s) => Key: %s removed\n",
                Thread.currentThread().getName(),
                new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                key
        );
    }
}
