package com.rcpky.creational.abstractfactory.uilibrary.features.buttons;

import com.rcpky.creational.abstractfactory.uilibrary.features.Button;

public class WindowButton implements Button {

    @Override
    public void render() {
        System.out.println("[Windows] Rendering flat button with Fluent Design");
    }

    @Override
    public void onClick() {
        System.out.println("[Windows] Button clicked with ripple effect");
    }

    @Override
    public void disable() {
        System.out.println("[Windows] Button disabled — faded appearance");
    }

    @Override
    public void enable() {
        System.out.println("[Windows] Button enabled — accent color restored");
    }
}
