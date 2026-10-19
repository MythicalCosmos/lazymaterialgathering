# End-to-End Validation Plan

This checklist is for validating LazyMaterialGathering in Minecraft after the code-level audit and fixes are complete. A successful Gradle build does not replace these runtime checks.

## 1. Planning and dependency accounting

- [ ] Request one item already in inventory; no gathering or crafting should be scheduled unnecessarily.
- [ ] Request multiple items that share an ingredient; the planner should reserve shared inventory once and account for the combined demand.
- [ ] Request an item that requires crafting from gathered ingredients; dependencies should run in a valid order.
- [ ] Request an item that requires furnace processing before crafting; inputs, fuel, processed output, and final crafting demand should all be accounted for.
- [ ] Include an item with multiple possible recipes; the selected recipe should be feasible with the available or obtainable ingredients.
- [ ] Include an unavailable or unsupported item; the plan should report the exact missing item and quantity rather than loop forever.

## 2. Gathering and navigation

- [ ] Gather a nearby common block and verify the requested quantity is reached.
- [ ] Gather a resource that requires exploration; verify the search makes progress and stops with a useful reason if no target can be found.
- [ ] Make a target inaccessible or remove it while en route; verify the target is rejected and the plan recovers.
- [ ] Cause a route to stall or end early; verify bounded retries and an actionable failure instead of an infinite loop.
- [ ] Fill the inventory during gathering; verify the mod does not discard collected items or claim the target was satisfied.
- [ ] Return to deposit locations with a partially full inventory; verify collected materials remain accounted for even if a deposit route fails.

## 3. Inventory and deposits

- [ ] Test partial stacks, multiple stacks, and quantities larger than one stack.
- [ ] Verify item counts across the hotbar and main inventory.
- [ ] Verify full inventory behavior during gathering, processing, and crafting.
- [ ] Verify that depositing materials does not cause the pipeline to forget those materials or gather them again unnecessarily.
- [ ] Change inventory contents manually while a plan is active; verify the next planning pass uses the current inventory.

## 4. Processing

- [ ] Process a smelting recipe using a furnace, a blasting recipe using a blast furnace, and a smoking recipe using a smoker.
- [ ] Test a recipe requiring multiple processing batches.
- [ ] Test insufficient fuel, unavailable processing input, and an unavailable workstation.
- [ ] Verify input transfer, fuel use, output collection, and completion counts against the actual inventory.
- [ ] Close a workstation screen or interrupt the operation mid-task; verify recovery or a clear blocked reason.

## 5. Crafting

- [ ] Craft a recipe using the player inventory crafting grid and a recipe requiring a crafting table.
- [ ] Test multiple output batches and recipes with repeated ingredients.
- [ ] Test unavailable ingredients and a crafting table that cannot be reached.
- [ ] Interrupt crafting by closing the screen or changing inventory contents.
- [ ] Verify output counts and ensure the task is not marked complete before the output is actually present.

## 6. Lifecycle and final completion

- [ ] Stop and restart a plan; verify stale tasks and navigation state do not leak into the new run.
- [ ] Change the requested schematic/targets while work is queued; verify the active plan is refreshed.
- [ ] Disconnect and reconnect, then restart the client/world where applicable; verify persisted world knowledge loads safely.
- [ ] Complete a multi-material plan containing gathering, processing, and crafting dependencies.
- [ ] Verify final success only when every requested target quantity exists in inventory.
- [ ] Verify a permanently impossible plan ends in a clear blocked state with missing items and quantities.

## Results log

For each run, record the Minecraft version, mod version/commit, target items and quantities, expected behavior, actual behavior, and any relevant log output. Mark a test passed only after observing the behavior in-game.
