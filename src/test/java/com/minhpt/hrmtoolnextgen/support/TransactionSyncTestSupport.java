package com.minhpt.hrmtoolnextgen.support;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Helpers for unit-testing services that defer work via
 * {@link TransactionSynchronizationManager#registerSynchronization}.
 *
 * <p>Plain Mockito tests run outside any transaction, so {@code registerSynchronization}
 * throws {@code IllegalStateException: Transaction synchronization is not active}.
 * Call {@link #begin()} in {@code @BeforeEach} to open a synchronization scope,
 * {@link #triggerAfterCommit()} to run the callbacks the service registered, and
 * {@link #end()} in {@code @AfterEach} to leave no thread-local state behind.
 */
public final class TransactionSyncTestSupport {

    private TransactionSyncTestSupport() {}

    /** Opens a synchronization scope so registerSynchronization() succeeds. */
    public static void begin() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    /**
     * Runs {@code afterCommit()} on every synchronization registered so far,
     * simulating a successful commit.
     */
    public static void triggerAfterCommit() {
        List<TransactionSynchronization> syncs =
                List.copyOf(TransactionSynchronizationManager.getSynchronizations());
        syncs.forEach(TransactionSynchronization::afterCommit);
    }

    /** Clears the thread-local synchronization scope. Safe to call when inactive. */
    public static void end() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
