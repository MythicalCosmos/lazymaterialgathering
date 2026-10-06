package net.lucy.model;

/**
 * Lifecycle state of the gathering coordinator.
 */
public enum GatheringTaskState {
    IDLE,
    MOVING_TO_RESOURCE,
    EXPLORING,
    MINING,
    VERIFYING,
    RETURNING_TO_DEPOSIT,
    BLOCKED,
    COMPLETE,
    PAUSED
}