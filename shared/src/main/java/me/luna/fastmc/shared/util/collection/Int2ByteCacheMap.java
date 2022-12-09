package dev.fastmc.common.collection;

import dev.fastmc.common.UtilsKt;
import it.unimi.dsi.fastutil.bytes.ByteCollection;
import it.unimi.dsi.fastutil.ints.Int2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class Int2ByteCacheMap implements Int2ByteMap {
    private final int capacity;
    private final Int2ByteLinkedOpenHashMap backingMap;

    public Int2ByteCacheMap(int capacity, byte defaultReturnValue) {
        this.capacity = capacity;
        this.backingMap = new Int2ByteLinkedOpenHashMap(capacity, 0.9999999f) {
            @Override
            protected void rehash(int newN) {}
        };
        this.backingMap.defaultReturnValue(defaultReturnValue);
    }

    public Int2ByteCacheMap(int capacity) {
        this.capacity = capacity;
        this.backingMap = new Int2ByteLinkedOpenHashMap(capacity, 0.9999999f) {
            @Override
            protected void rehash(int newN) {}
        };
    }

    @NotNull
    @Override
    public ObjectSet<Map.Entry<Integer, Byte>> entrySet() {
        return backingMap.entrySet();
    }

    @Override
    public ObjectSet<Entry> int2ByteEntrySet() {
        return backingMap.int2ByteEntrySet();
    }

    @NotNull
    @Override
    public IntSet keySet() {
        return backingMap.keySet();
    }

    @NotNull
    @Override
    public ByteCollection values() {
        return backingMap.values();
    }

    @Override
    public boolean containsValue(byte value) {
        return backingMap.containsValue(value);
    }

    @Override
    public byte put(int key, byte value) {
        if (backingMap.size() >= capacity) {
            backingMap.removeLastByte();
        }
        return backingMap.putAndMoveToFirst(key, value);
    }

    public void put(int key, boolean value) {
        if (backingMap.size() >= capacity) {
            backingMap.removeLastByte();
        }
        backingMap.putAndMoveToFirst(key, value ? UtilsKt.BYTE_TRUE : UtilsKt.BYTE_FALSE);
    }

    @Override
    public byte get(int key) {
        return backingMap.getAndMoveToFirst(key);
    }

    @Override
    public byte remove(int key) {
        return backingMap.remove(key);
    }

    @Override
    public boolean containsKey(int key) {
        return backingMap.containsKey(key);
    }

    @Override
    public void defaultReturnValue(byte rv) {
        backingMap.defaultReturnValue(rv);
    }

    @Override
    public byte defaultReturnValue() {
        return backingMap.defaultReturnValue();
    }

    @Override
    public Byte put(Integer key, Byte value) {
        return this.put(key.intValue(), value.byteValue());
    }

    @Override
    public Byte get(Object key) {
        return this.get(((Integer) key).intValue());
    }

    @Override
    public boolean containsKey(Object key) {
        return this.containsKey(((Integer) key).intValue());
    }

    @Override
    public boolean containsValue(Object value) {
        return this.containsValue(((Byte) value).byteValue());
    }

    @Override
    public Byte remove(Object key) {
        return this.remove(((Integer) key).intValue());
    }

    @Override
    public void putAll(@NotNull Map<? extends Integer, ? extends Byte> m) {
        int mapSize = m.size();
        while (backingMap.size() + mapSize > capacity) {
            backingMap.removeLastByte();
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
