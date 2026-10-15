package com.rcpky.evictionpolicy;

import com.rcpky.interfaces.EvictionPolicy;
import com.rcpky.model.DoublyLinkedList;
import com.rcpky.model.Node;

import java.util.HashMap;
import java.util.Map;

public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {
    private final Map<K, Node<K>> keyMap= new HashMap<>();
    private final DoublyLinkedList<K> dll = new DoublyLinkedList<>();

    public LRUEvictionPolicy() {}

    @Override
    public K evictKey() {
        Node<K> node = dll.removeFirst();
        if (node == null) return null;
        keyMap.remove(node.getKey());
        return node.getKey();
    }

    @Override
    public void keyAccessed(K key) {
        if (keyMap.containsKey(key)) {
            dll.moveToEnd(keyMap.get(key));
        }else {
            Node<K> node= dll.addLast(key);
            keyMap.put(key, node);
        }
    }
}
