package net.lucy.model;

public enum CraftingTaskState {
    IDLE,
    OPENING_INVENTORY,
    WAITING_FOR_CRAFTING_TABLE,
    FILLING_RECIPE,
    CRAFTING,
    VERIFYING,
    COMPLETE,
    BLOCKED,
    PAUSED
}