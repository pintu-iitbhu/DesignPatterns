package com.rcpky.abstractfactory.uilibrary;

import com.rcpky.abstractfactory.uilibrary.mac.MacUIFactory;
import com.rcpky.abstractfactory.uilibrary.window.WindowUIFactory;

public class OsFactory {

    public static UIFactory getFactory(String osType) {
        switch (osType.toLowerCase()) {
            case "mac" :   return new MacUIFactory();
            case "window": return new WindowUIFactory();
            default : throw new IllegalArgumentException("Unsupported OS: " + osType);
        }
    }
}
