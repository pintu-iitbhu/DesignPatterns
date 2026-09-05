package com.rcpky.creational.abstractfactory.uilibrary;

import com.rcpky.creational.abstractfactory.uilibrary.mac.MacUIFactory;
import com.rcpky.creational.abstractfactory.uilibrary.window.WindowUIFactory;

public class OsFactory {

    public static UIFactory getFactory(String osType) {
        switch (osType.toLowerCase()) {
            case "mac" :   return new MacUIFactory();
            case "window": return new WindowUIFactory();
            default : throw new IllegalArgumentException("Unsupported OS: " + osType);
        }
    }
}
