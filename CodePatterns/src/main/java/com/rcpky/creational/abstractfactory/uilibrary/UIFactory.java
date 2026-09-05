package com.rcpky.creational.abstractfactory.uilibrary;

import com.rcpky.creational.abstractfactory.uilibrary.features.Button;
import com.rcpky.creational.abstractfactory.uilibrary.features.Checkbox;
import com.rcpky.creational.abstractfactory.uilibrary.features.TextField;

public interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
    TextField createTextField();
}
