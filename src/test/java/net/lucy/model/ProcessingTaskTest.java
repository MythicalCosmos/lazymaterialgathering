package net.lucy.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessingTaskTest {

    @Test
    void inputRequiredRoundsUpToWholeProcessingOperations() {
        ProcessingTask task = new ProcessingTask(
                "minecraft:glass",
                "minecraft:sand",
                33L,
                1L,
                16L,
                RecipeType.SMELTING
        );

        assertEquals(3L, task.inputRequired());
    }

    @Test
    void inputRequiredDoesNotOverflowWhenRoundingLargeTargets() {
        ProcessingTask task = new ProcessingTask(
                "minecraft:example",
                "minecraft:input",
                Long.MAX_VALUE,
                1L,
                2L,
                RecipeType.SMELTING
        );

        assertEquals(Long.MAX_VALUE / 2L + 1L, task.inputRequired());
    }

    @Test
    void inputRequiredSaturatesWhenInputMultiplicationWouldOverflow() {
        ProcessingTask task = new ProcessingTask(
                "minecraft:example",
                "minecraft:input",
                Long.MAX_VALUE,
                2L,
                1L,
                RecipeType.SMELTING
        );

        assertEquals(Long.MAX_VALUE, task.inputRequired());
    }

    @Test
    void zeroOutputRequiresNoInput() {
        ProcessingTask task = new ProcessingTask(
                "minecraft:glass",
                "minecraft:sand",
                0L,
                1L,
                16L,
                RecipeType.SMELTING
        );

        assertEquals(0L, task.inputRequired());
    }
}
