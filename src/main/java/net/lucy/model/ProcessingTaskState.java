package net.lucy.model;

public enum ProcessingTaskState {
    IDLE,
    FINDING_PROCESSOR,
    OPENING_PROCESSOR,
    LOADING_INPUT,
    LOADING_FUEL,
    PROCESSING,
    COLLECTING,
    VERIFYING,
    COMPLETE,
    BLOCKED,
    PAUSED
}