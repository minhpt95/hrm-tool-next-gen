package com.minhpt.hrmtoolnextgen.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Unit tests for {@link LoggingUtils} — the MDC-backed structured-logging context.
 *
 * <p>MDC is thread-local, so every test clears it before and after to stay isolated.
 */
class LoggingUtilsTest {

    @BeforeEach
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    // -------------------------------------------------------------------------
    // module / operation
    // -------------------------------------------------------------------------

    @Test
    void setModule_isReadBackByGetModule() {
        LoggingUtils.setModule("UserService");
        assertEquals("UserService", LoggingUtils.getModule());
    }

    @Test
    void setOperation_isReadBackByGetOperation() {
        LoggingUtils.setOperation("createUser");
        assertEquals("createUser", LoggingUtils.getOperation());
    }

    @Test
    void getModuleAndOperation_whenUnset_returnNull() {
        assertNull(LoggingUtils.getModule());
        assertNull(LoggingUtils.getOperation());
    }

    // -------------------------------------------------------------------------
    // context serialisation
    // -------------------------------------------------------------------------

    @Test
    void setContext_serialisesToJsonAndReadsBack() {
        LoggingUtils.setContext(Map.of("userId", 42));

        Map<String, Object> context = LoggingUtils.getContext();

        assertEquals(1, context.size());
        assertEquals(42, context.get("userId"));
    }

    @Test
    void setContext_null_isIgnored() {
        LoggingUtils.setContext(null);
        assertTrue(LoggingUtils.getContext().isEmpty());
    }

    @Test
    void setContext_empty_isIgnored() {
        LoggingUtils.setContext(Map.of());
        assertTrue(LoggingUtils.getContext().isEmpty());
    }

    @Test
    void getContext_whenUnset_returnsEmptyMap() {
        assertTrue(LoggingUtils.getContext().isEmpty());
    }

    @Test
    void getContext_whenMdcHoldsUnparseableJson_returnsEmptyMap() {
        MDC.put("log.context", "{not valid json");
        assertTrue(LoggingUtils.getContext().isEmpty());
    }

    @Test
    void setContext_unserialisableValue_fallsBackToToString() {
        Map<String, Object> context = new LinkedHashMap<>();
        // A self-referencing map cannot be serialised by Jackson.
        context.put("self", context);

        LoggingUtils.setContext(context);

        // The fallback stores context.toString(), which is not valid JSON,
        // so reading it back yields an empty map rather than throwing.
        assertTrue(LoggingUtils.getContext().isEmpty());
    }

    // -------------------------------------------------------------------------
    // addContext — merges into whatever is already there
    // -------------------------------------------------------------------------

    @Test
    void addContext_addsToEmptyContext() {
        LoggingUtils.addContext("requestId", "abc-123");

        assertEquals("abc-123", LoggingUtils.getContext().get("requestId"));
    }

    @Test
    void addContext_mergesWithExistingEntries() {
        LoggingUtils.setContext(Map.of("first", 1));
        LoggingUtils.addContext("second", 2);

        Map<String, Object> context = LoggingUtils.getContext();

        assertEquals(2, context.size());
        assertEquals(1, context.get("first"));
        assertEquals(2, context.get("second"));
    }

    @Test
    void addContext_overwritesExistingKey() {
        LoggingUtils.setContext(Map.of("key", "old"));
        LoggingUtils.addContext("key", "new");

        assertEquals("new", LoggingUtils.getContext().get("key"));
    }

    // -------------------------------------------------------------------------
    // clearContext
    // -------------------------------------------------------------------------

    @Test
    void clearContext_removesModuleOperationAndContext() {
        LoggingUtils.setModule("M");
        LoggingUtils.setOperation("O");
        LoggingUtils.setContext(Map.of("k", "v"));

        LoggingUtils.clearContext();

        assertNull(LoggingUtils.getModule());
        assertNull(LoggingUtils.getOperation());
        assertTrue(LoggingUtils.getContext().isEmpty());
    }

    // -------------------------------------------------------------------------
    // withContext — sets, runs, and always clears
    // -------------------------------------------------------------------------

    @Test
    void withContext_setsContextForTheDurationOfTheBlock() {
        LoggingUtils.withContext("BatchJob", "run", Map.of("size", 10), () -> {
            assertEquals("BatchJob", LoggingUtils.getModule());
            assertEquals("run", LoggingUtils.getOperation());
            assertEquals(10, LoggingUtils.getContext().get("size"));
        });

        assertNull(LoggingUtils.getModule());
        assertNull(LoggingUtils.getOperation());
        assertTrue(LoggingUtils.getContext().isEmpty());
    }

    @Test
    void withContext_clearsContextEvenWhenTheBlockThrows() {
        assertThrows(IllegalStateException.class, () ->
                LoggingUtils.withContext("M", "O", Map.of("k", "v"), () -> {
                    throw new IllegalStateException("boom");
                }));

        assertNull(LoggingUtils.getModule());
        assertNull(LoggingUtils.getOperation());
        assertTrue(LoggingUtils.getContext().isEmpty());
    }
}
