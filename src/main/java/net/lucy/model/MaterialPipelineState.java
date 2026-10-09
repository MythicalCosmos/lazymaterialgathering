package net.lucy.model;

/**
 * State of the complete automatic material pipeline.
 *
 * The pipeline always runs in this order:
 *
 *     GATHER -> PROCESS -> CRAFT
 *
 * A stage is not considered complete merely because its queue stopped.
 * The coordinator verifies the actual inventory before advancing.
 */
public enum MaterialPipelineState {
    IDLE,
    GATHERING,
    VERIFYING_GATHERING,
    PROCESSING,
    VERIFYING_PROCESSING,
    CRAFTING,
    VERIFYING_CRAFTING,
    COMPLETE,
    BLOCKED,
    PAUSED
}