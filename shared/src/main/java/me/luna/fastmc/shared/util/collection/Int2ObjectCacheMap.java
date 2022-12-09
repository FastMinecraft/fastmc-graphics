package dev.fastmc.common.collection;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class Int2ObjectCacheMap<V> implements Int2ObjectMap<V> {
    private final int capacity;
    private final Int2ObjectLinkedOpenHashMap<V> backingMap;

    public Int2ObjectCacheMap(int capacity) {
        this.capacity = capacity;
        this.backingMap = new Int2ObjectLinkedOpenHashMap<V>(capacity, 0.9999999f) {
            @Override
            protected void rehash(int newN) {}
        };
    }

    @NotNull
    @Override
    public ObjectSet<Map.Entry<Integer, V>> entrySet() {
        return backingMap.entrySet();
    }

    @Override
    public ObjectSet<Entry<V>> int2ObjectEntrySet() {
        return backingMap.int2ObjectEntrySet();
    }

    @NotNull
    @Override
    public IntSet keySet() {
        return backingMap.keySet();
    }

    @NotNull
    @Override
    public ObjectCollection<V> values() {
        return backingMap.values();
    }

    @Override
    public V put(int key, V value) {
        if (backingMap.size() >= capacity) {
            backingMap.removeLast();
        }
        return backingMap.putAndMoveToFirst(key, value);
    }

    @Override
    public V get(int key) {
        return backingMap.getAndMoveToFirst(key);
    }

    @Override
    public V remove(int key) {
        return backingMap.remove(key);
    }

    @Override
    public boolean containsKey(int key) {
        return backingMap.containsKey(key);
    }

    @Override
    public void defaultReturnValue(V rv) {
        backingMap.defaultReturnValue(rv);
    }

    @Override
    public V defaultReturnValue() {
        return backingMap.defaultReturnValue();
    }

    @Override
    public V put(Integer key, V value) {
        return this.put(key.intValue(), value);
    }

    @Override
    public V get(Object key) {
        return this.get(((Integer) key).intValue());
    }

    @Override
    public boolean containsKey(Object key) {
        return this.containsKey(((Integer) key).intValue());
    }

    @Override
    public boolean containsValue(Object value) {
        return backingMap.containsValue(value);
    }

    @Override
    public V remove(Object key) {
        return this.remove(((Integer) key).intValue());
    }

    @Override
    public void putAll(@NotNull Map<? extends Integer, ? extends V> m) {
        int mapSize = m.size();
        while (backingMap.size() + mapSize > capacity) {
            backingMap.removeLast();
        }
        backingMap.putAll(m);
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public void clear() {
        backingMap.clear();
    }

    public int getCapacity() {
        return capacity;
    }
}
