package com.rcpky.singleton;

public class DclSingleton {

    // Why volatile ?
    // Volatile keyword ensures that multiple threads handle the unique instance of a singleton class correctly when it is initialized to the instance.
    private static volatile DclSingleton instance;

    private DclSingleton() {

    }

    public static DclSingleton getInstance() {
        if (instance == null) {
            synchronized (DclSingleton .class) {
                if (instance == null) {
                    instance = new DclSingleton();
                }
            }
        }
        return instance;
    }

    // private constructor and other methods...
}
