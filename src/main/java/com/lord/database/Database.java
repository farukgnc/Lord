package com.lord.database;

import java.util.concurrent.CompletableFuture;

public interface Database {

    CompletableFuture<Boolean> connect();

    void disconnect();

}
