package com.lord.module;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public void registerModule(Module module) {
        this.modules.add(module);
    }

    public void enableModules() {
        for (Module module : this.modules) {
            try {
                module.enable();
            } catch (Exception e) {
                System.err.println("An error occurred while enabling module " + module.getName());
                e.printStackTrace();
            }
        }
    }

    public void disableModules() {
        for (int i = this.modules.size() - 1; i >= 0; i--) {
            Module module = this.modules.get(i);
            try {
                module.disable();
            } catch (Exception e) {
                System.err.println("An error occurred while disabling module " + module.getName());
                e.printStackTrace();
            }
        }
    }
}