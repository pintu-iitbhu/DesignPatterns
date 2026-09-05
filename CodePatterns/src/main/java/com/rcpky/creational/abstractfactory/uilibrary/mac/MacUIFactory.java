package com.rcpky.creational.abstractfactory.uilibrary.mac;

import com.rcpky.creational.abstractfactory.uilibrary.UIFactory;
import com.rcpky.creational.abstractfactory.uilibrary.features.Button;
import com.rcpky.creational.abstractfactory.uilibrary.features.Checkbox;
import com.rcpky.creational.abstractfactory.uilibrary.features.TextField;
import com.rcpky.creational.abstractfactory.uilibrary.features.buttons.MacButton;
import com.rcpky.creational.abstractfactory.uilibrary.features.checkboxes.MacCheckbox;
import com.rcpky.creational.abstractfactory.uilibrary.features.textfields.MacTextField;

public class MacUIFactory implements UIFactory {

    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }

    @Override
    public TextField createTextField() {
        return new MacTextField();
    }
}
