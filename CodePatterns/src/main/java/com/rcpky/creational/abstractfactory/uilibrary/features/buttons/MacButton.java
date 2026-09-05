package com.rcpky.creational.abstractfactory.uilibrary.features.buttons;

import com.rcpky.creational.abstractfactory.uilibrary.features.Button;

public class MacButton implements Button {

    @Override
    public void render() {
        System.out.println("[Mac] Rendering rounded button with Aqua style");
    }

    @Override
    public void onClick() {
        System.out.println("[Mac] Button clicked with haptic feedback");
    }

    @Override
    public void disable() {
        System.out.println("[Mac] Button disabled — greyed out");
    }

    @Override
    public void enable() {
        System.out.println("[Mac] Button enabled — Aqua highlight restored");
    }
}
