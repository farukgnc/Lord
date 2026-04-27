package com.lord.module;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleManagerTest {

    @Test
    void shouldEnableModulesInOrderAndDisableInReverseOrder() {
        ModuleManager manager = new ModuleManager();
        List<String> calls = new ArrayList<>();

        manager.registerModule(new TestModule("A", calls));
        manager.registerModule(new TestModule("B", calls));

        manager.enableModules();
        manager.disableModules();

        assertThat(calls).containsExactly("enable-A", "enable-B", "disable-B", "disable-A");
    }

    @Test
    void shouldContinueWhenOneModuleThrows() {
        ModuleManager manager = new ModuleManager();
        List<String> calls = new ArrayList<>();

        manager.registerModule(new TestModule("ok1", calls));
        manager.registerModule(new FailingModule("bad", calls));
        manager.registerModule(new TestModule("ok2", calls));

        manager.enableModules();

        assertThat(calls).contains("enable-ok1", "enable-bad", "enable-ok2");
    }

    private static class TestModule implements Module {
        private final String name;
        private final List<String> calls;

        private TestModule(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public void enable() {
            calls.add("enable-" + name);
        }

        @Override
        public void disable() {
            calls.add("disable-" + name);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static final class FailingModule extends TestModule {

        private FailingModule(String name, List<String> calls) {
            super(name, calls);
        }

        @Override
        public void enable() {
            super.enable();
            throw new RuntimeException("boom");
        }
    }
}
