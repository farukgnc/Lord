package com.lord.punishment.exceptions;

// Bu bir "checked exception" olacak, yani onu çağıran kodun bu durumu yönetmesini zorunlu kılacağız.
public class CannotPunishSelfException extends Exception {
    public CannotPunishSelfException(String message) {
        super(message);
    }
}