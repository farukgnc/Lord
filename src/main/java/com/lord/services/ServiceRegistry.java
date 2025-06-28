package com.lord.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceRegistry {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    public <T> void register(Class<T> serviceClass, T serviceInstance) {
        this.services.put(serviceClass, serviceInstance);
    }

    public <T> void unregister(Class<T> serviceClass) {
        this.services.remove(serviceClass);
    }

    public <T> T get(Class<T> serviceClass) {
        Object serviceInstance = this.services.get(serviceClass);
        if (serviceInstance == null) {
            throw new IllegalStateException("Service not found: " + serviceClass.getName());
        }
        return serviceClass.cast(serviceInstance);
    }
}