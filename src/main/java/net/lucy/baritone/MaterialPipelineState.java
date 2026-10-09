package net.lucy.baritone;

public enum MaterialPipelineState {
    IDLE,
    GATHERING,
    VERIFYING_GATHERING,
    PROCESSING,
    VERIFYING_PROCESSING,
    CRAFTING,
    COMPLETE,
    BLOCKED,
    PAUSED
}