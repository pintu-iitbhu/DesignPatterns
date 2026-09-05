package com.rcpky.creational.singleton;

public class Book {

    private  static Book instance;

    private Book() {

    }

    ///  we have synchronization problem here
    public static Book getInstance() {
        if (instance == null) {
            instance = new Book();
        }
        return instance;
    }

    // this is thread safe but it is not lazy loading
    //Despite this class being thread-safe, we can see that there’s a clear performance drawback: each time we want to get the instance of our singleton, we need to acquire a potentially unnecessary lock.
    public static synchronized Book getInstance2() {
        if (instance == null) {
            instance = new Book();
        }
        return  instance;
    }
}
