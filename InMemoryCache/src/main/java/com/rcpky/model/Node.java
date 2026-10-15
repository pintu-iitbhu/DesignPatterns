package com.rcpky.model;

import lombok.Getter;

@Getter
public class Node<K> {
    K key;
    Node<K> next;
    Node<K> prev;

    Node(K key) {
        this.key = key;
        this.next = null;
        this.prev = null;
    }
}
