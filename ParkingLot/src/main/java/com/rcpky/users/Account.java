package com.rcpky.users;

import com.rcpky.enums.AccountStatus;

public abstract class Account {
    private String userName;
    private String password;
    private Person person;
    private AccountStatus status;
    public abstract boolean resetPassword();
}
