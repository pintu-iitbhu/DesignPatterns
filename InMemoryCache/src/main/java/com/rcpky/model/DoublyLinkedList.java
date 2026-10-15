package com.rcpky.model;

public class DoublyLinkedList<K> {
    private Node<K> head, tail;

    public DoublyLinkedList() {
        this.head = null;
        this.tail = null;
    }

    public Node<K> addLast(K key) {
        Node<K> node = new Node<>(key);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        return node;
    }

    public void moveToEnd(Node<K> node) {
        if (node == tail) return;

        if (node == head) {
            head = node.next;
        } else {
            node.prev.next = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        }

        node.prev = tail;
        node.next = null;
        tail.next = node;
        tail = node;
    }

    public Node<K> removeFirst() {
        if (head == null) return null;
        Node<K> node = head;
        head = head.next;
        if (head == null) tail = null;
        else head.prev = null;
        return node;
    }
}
