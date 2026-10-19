package net.lucy.model;

/**
 * One furnace-family processing operation.
 *
 * required represents the number of output items wanted.
 */
public final class ProcessingTask {

    private final String output;
    private final String input;
    private final long required;
    private final long inputPerOutput;
    private final long outputPerOperation;
    private final RecipeType type;

    public ProcessingTask(
            String output,
            String input,
            long required,
            long inputPerOutput,
            RecipeType type
    ) {
        this(output, input, required, inputPerOutput, 1L, type);
    }

    public ProcessingTask(
            String output,
            String input,
            long required,
            long inputPerOutput,
            long outputPerOperation,
            RecipeType type
    ) {
        this.output = output;
        this.input = input;
        this.required = Math.max(0L, required);
        this.inputPerOutput = Math.max(1L, inputPerOutput);
        this.outputPerOperation = Math.max(1L, outputPerOperation);
        this.type = type == null
                ? RecipeType.OTHER
                : type;
    }

    public String output() {
        return output;
    }

    public String input() {
        return input;
    }

    public long required() {
        return required;
    }

    public long inputPerOutput() {
        return inputPerOutput;
    }

    public long outputPerOperation() {
        return outputPerOperation;
    }

    public RecipeType type() {
        return type;
    }

    public long inputRequired() {
        long operations = (required + outputPerOperation - 1L) / outputPerOperation;
        return operations * inputPerOutput;
    }
}