package com.rcpky.creational.abstractfactory.uilibrary.window;

import com.rcpky.creational.abstractfactory.uilibrary.UIFactory;
import com.rcpky.creational.abstractfactory.uilibrary.features.Button;
import com.rcpky.creational.abstractfactory.uilibrary.features.Checkbox;
import com.rcpky.creational.abstractfactory.uilibrary.features.TextField;
import com.rcpky.creational.abstractfactory.uilibrary.features.buttons.WindowButton;
import com.rcpky.creational.abstractfactory.uilibrary.features.checkboxes.WindowCheckbox;
import com.rcpky.creational.abstractfactory.uilibrary.features.textfields.WindowTextField;

public class WindowUIFactory implements UIFactory {

    @Override
    public Button createButton() {
        return new WindowButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new WindowCheckbox();
    }

    @Override
    public TextField createTextField() {
        return new WindowTextField();
    }
}
