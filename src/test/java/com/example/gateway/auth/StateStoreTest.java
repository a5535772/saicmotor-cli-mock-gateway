package com.example.gateway.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateStoreTest {

    private final StateStore store = new StateStore(1);

    @Test
    void validatesAndConsumesStateOnce() {
        String state = store.create("http://localhost:3000/callback");
        assertTrue(store.validate(state, "http://localhost:3000/callback"));
        assertFalse(store.validate(state, "http://localhost:3000/callback")); // 已消费
    }

    @Test
    void rejectsWrongRedirect() {
        String state = store.create("http://localhost:3000/callback");
        assertFalse(store.validate(state, "http://localhost:3001/callback"));
    }

    @Test
    void rejectsExpired() throws Exception {
        StateStore tiny = new StateStore(0);
        String state = tiny.create("http://localhost:3000/callback");
        Thread.sleep(50);
        assertFalse(tiny.validate(state, "http://localhost:3000/callback"));
    }
}
