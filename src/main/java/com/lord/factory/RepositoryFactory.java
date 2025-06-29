package com.lord.factory;

import java.util.concurrent.CompletableFuture;

// Bu arayüz, tüm repository'lerimizi üreten bir fabrikanın kontratıdır.
public interface RepositoryFactory {

    CompletableFuture<Boolean> connect();

    void createRepositories();

    void disconnect();
}