package com.lord.punishment;

import lombok.Getter;

@Getter
public enum PunishmentType {
    BAN("banned"),
    MUTE("muted"),
    KICK("kicked"),
    WARN("warned");

    private final String pastTense;

    PunishmentType(String pastTense) {
        this.pastTense = pastTense;
    }
}