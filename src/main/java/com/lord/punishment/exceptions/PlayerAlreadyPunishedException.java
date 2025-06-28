package com.lord.punishment.exceptions;

// Bu bir "checked exception" olacak, yani onu çağıran kodun bu durumu yönetmesini zorunlu kılacağız.
public class PlayerAlreadyPunishedException extends Exception {
    public PlayerAlreadyPunishedException(String message) {
        super(message);
    }
}