package com.rcpky.creational.abstractfactory.uilibrary;

import com.rcpky.creational.abstractfactory.uilibrary.mac.MacOs;
import com.rcpky.creational.abstractfactory.uilibrary.window.WindowOs;

public class Application {

    public static void main(String[] args) {
        System.out.println("=== Abstract Factory Demo ===\n");

        System.out.println("--- macOS UI ---");
        UIFactory macFactory = OsFactory.getFactory("mac");
        MacOs macApp = new MacOs(macFactory);
        macApp.show();
        macApp.minimize();
        macApp.maximize();
        macApp.close();

        System.out.println("\n--- Windows UI ---");
        UIFactory winFactory = OsFactory.getFactory("window");
        WindowOs winApp = new WindowOs(winFactory);
        winApp.show();
        winApp.minimize();
        winApp.maximize();
        winApp.close();
    }
}
