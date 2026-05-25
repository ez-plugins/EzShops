## Code Quality Rules for Minecraft Plugin Development

These rules are adapted for Java-based Minecraft plugins. The goal is not to write embedded-style code, but to keep plugin code simple, well split, testable, safe on live servers, and easy for maintainers or coding agents to reason about.

### 1. Keep control flow simple

Use straightforward Java control flow: `if`, `switch`, `for`, enhanced `for`, and small helper methods.

Avoid:

- Deeply nested conditionals.
- Clever stream chains with hidden side effects.
- Complex `CompletableFuture` chains inside gameplay logic.
- Recursion, unless there is a very strong reason and a clear maximum depth.
- Control flow hidden behind reflection, dynamic dispatch tricks, or global state.

Prefer early returns for validation and failure cases.

Good pattern:

- Command validates input.
- Command calls a service.
- Service returns a clear result.
- Command formats the result for the player.

Commands should not contain large business logic.

### 2. Every loop must have a clear and safe upper bound

Never write unbounded loops in runtime plugin logic.

Avoid:

- `while (true)` unless it is immediately and obviously guarded by a safe exit.
- Loops that depend on external state changing eventually.
- World scanning, chunk scanning, teleport searching, or database paging without a maximum limit.

Every loop that can run during gameplay must have an explicit upper bound.

Examples:

- RTP location search must use a configured max attempt count.
- Chunk scans must use a maximum radius.
- Pagination must use a maximum page size.
- Cleanup tasks must process a limited batch per tick/run.
- Retry logic must have a maximum retry count.

Configurable limits must be validated and clamped during config loading.

Do not trust raw config values directly inside loops.

### 3. Avoid unnecessary allocation in hot paths

Java uses dynamic memory, so allocation cannot be forbidden completely. Instead, avoid avoidable allocations in code that runs often.

Be careful with allocation inside:

- Event listeners.
- Scheduled tasks.
- Movement/chat/combat handlers.
- RTP search loops.
- Claim lookup logic.
- Placeholder expansions.
- Scoreboard or map rendering.
- Repeated command tab completion.

Prefer:

- Immutable config snapshots loaded during startup/reload.
- Reusing parsed config values instead of parsing every call.
- Caching expensive lookups where safe.
- Small value objects only when they improve clarity.
- Batch processing instead of creating many short-lived objects per tick.

Do not introduce memory caches without a clear invalidation strategy.

### 4. Keep methods short and focused

A method should usually fit on one screen.

Prefer a soft maximum of 40-60 lines per method.

Split large methods by responsibility:

- Validation.
- Permission checks.
- Config lookup.
- Business rule.
- Persistence call.
- Message formatting.
- Bukkit/Paper side effect.

Do not split methods randomly just to reduce line count. Each extracted method should have a clear name and purpose.

Classes should also stay focused. Avoid “god services” that handle commands, storage, config, messages, and listeners together.

### 5. Use explicit guards instead of Java `assert`

Java assertions are often disabled at runtime, so do not rely on `assert` for plugin correctness.

Use explicit validation and recovery:

- `Objects.requireNonNull(...)` for required constructor dependencies.
- Clear parameter checks for public/service methods.
- Return a typed result, `Optional`, or failure object when the caller can recover.
- Log and safely disable a feature when startup validation fails.
- Fail gracefully when optional integrations are missing.

Every public method, service method, repository method, command handler, and listener should validate assumptions that can realistically be broken.

Examples:

- Player may be offline.
- World may not exist.
- Config value may be invalid.
- Optional plugin may be missing.
- Database result may be empty.
- Economy provider may be unavailable.
- Chunk or location may not be safe.
- Faction/member/claim may no longer exist.

Do not throw generic runtime exceptions for normal gameplay failures.

### 6. Keep variables in the smallest useful scope

Declare variables as close as possible to where they are used.

Prefer `final` for local variables when the value should not change.

Avoid:

- Reusing one variable for multiple meanings.
- Large method-level variable blocks.
- Mutable class fields for temporary operation state.
- Static mutable state except for true constants.

Plugin lifecycle state should live in bootstrap/registry classes, not scattered globally.

### 7. Check return values and handle failure paths

Do not ignore meaningful return values.

Always handle:

- `Optional`.
- `Result` / custom outcome objects.
- Boolean success/failure returns.
- Repository save/update/delete results.
- Scheduler task creation failures where applicable.
- Integration lookup results.
- Economy transaction results.
- Permission and validation results.

If a method can fail, its name and return type should make that clear.

Avoid methods that return `null`. Prefer `Optional`, typed result objects, or clear exceptions only for programmer errors.

### 8. Validate all input at module boundaries

Validate data when it enters a layer.

Important boundaries:

- Commands.
- Event listeners.
- Config loading.
- Database/repository reads.
- PlaceholderAPI expansions.
- TeamsAPI/Vault/WorldGuard/WorldEdit/dynmap adapters.
- Public API methods.
- Scheduler tasks.
- Network/proxy messages.

Services should not assume commands already validated everything. Commands are not the only caller.

Repositories should not contain gameplay decisions. They should validate persistence-level requirements and return clear results.

### 9. Keep platform and integration code isolated

Do not spread direct optional-plugin calls throughout core logic.

Keep integrations behind adapters/services.

Examples:

- Vault logic belongs in an economy adapter/service.
- WorldGuard/WorldEdit logic belongs in a protection adapter/service.
- TeamsAPI logic belongs in an adapter.
- Paper/Folia scheduler differences belong in scheduler/platform abstraction code.
- PlaceholderAPI formatting belongs in a placeholder adapter.

The plugin must start safely when optional integrations are absent unless the feature is explicitly required.

Avoid static hard dependencies on optional plugins outside bootstrap or adapter code.

### 10. Keep Bukkit/Paper/Folia thread rules explicit

Do not block the main server thread.

Avoid on the main thread:

- Database queries.
- HTTP requests.
- File-heavy operations.
- Large chunk scans.
- Long RTP searches.
- Expensive biome/world lookups.
- Large cache rebuilds.

Only call Bukkit/Paper APIs from the correct thread/context.

For Folia-compatible code, use the correct scheduler abstraction instead of assuming one global main thread.

When moving between async work and Bukkit work, make the boundary obvious in code.

### 11. Make commands thin and services testable

Command classes should only handle:

- Argument parsing.
- Sender/player validation.
- Permission checks.
- Calling services.
- Sending messages.

Business logic belongs in services.

Persistence belongs in repositories.

Bukkit event reaction belongs in engines/listeners, but reusable decisions should still be moved into services.

This makes logic testable without starting a full Minecraft server.

### 12. Keep persistence code behind repositories

Do not query the database directly from commands, listeners, placeholders, or integrations.

Use repositories for database access.

Use services for business rules.

Repository methods should have clear names, for example:

- `findById`
- `findByName`
- `save`
- `delete`
- `findClaimsInWorld`
- `findMembersForFaction`

Services should decide what the result means for gameplay.

Tests should cover repository behavior when schema/model logic changes.

### 13. Keep configuration safe and typed

Do not read raw config values everywhere.

Prefer:

- Config loader classes.
- Typed config objects.
- Validation during startup/reload.
- Safe defaults.
- Clamped numeric values.
- Clear migration behavior for renamed keys.

When changing config:

- Keep backward compatibility where practical.
- Document migration impact.
- Update default config files.
- Update tests for config parsing if behavior changes.

Never allow config values to create unsafe loops, invalid worlds, negative cooldowns, impossible radii, or broken economy values.

### 14. Keep user-facing messages centralized

Do not hardcode player-facing text inside business logic.

Use message/config files where practical.

Preserve placeholder names when editing existing messages.

Command output should be consistent and testable.

Business services should return result objects, not preformatted chat messages, unless the service is specifically a message/formatting service.

### 15. Avoid hidden behavior through reflection or magic

Avoid reflection unless needed for version compatibility or optional integration support.

If reflection is required:

- Keep it in one adapter class.
- Document why it exists.
- Fail gracefully.
- Add tests around fallback behavior where practical.

Do not use reflection to avoid proper abstractions.

Do not use hidden global service lookups where constructor injection or registry access would be clearer.

### 16. Use lambdas and streams carefully

Lambdas and streams are allowed, but they must stay readable.

Avoid:

- Long stream chains.
- Streams with side effects.
- Nested lambdas.
- Async logic hidden inside lambdas.
- Stream use in hot paths when a simple loop is clearer or faster.

Prefer simple loops when the logic includes validation, branching, failure handling, logging, or side effects.

### 17. Prefer explicit result types for business operations

For non-trivial operations, avoid returning only `boolean`.

Prefer typed result objects or enums.

Examples:

- `TeleportResult`
- `ClaimResult`
- `FactionCreateResult`
- `EconomyChargeResult`
- `InviteResult`
- `CommandResult`

A good result should explain why an operation failed without requiring the caller to guess.

This improves command messages, tests, and future API usage.

### 18. Keep public APIs stable and boring

Public API classes should be small, documented, and hard to misuse.

Avoid exposing:

- Internal models directly when they may change.
- Mutable collections.
- Database objects.
- Bukkit implementation details unless required.
- Optional integration internals.

Prefer interfaces and simple DTOs for API surfaces.

If changing public API behavior, update docs and tests.

### 19. Compile cleanly and keep verification strict

Code must compile without warnings.

Before submitting changes, run the repository’s normal verification flow.

For EzRTP, use the Maven/Spotless flow already defined by the project.

For PvPIndex Factions, use the Maven test/package/verify flow and Checkstyle rules already defined by the project.

Minimum expectation:

- Compile succeeds.
- Tests pass.
- Formatting/checkstyle passes.
- No unrelated files are changed.
- No generated build output is committed.

If full verification is not possible, run the most relevant targeted tests and clearly document what was skipped.

### 20. Add or update tests for behavior changes

Every non-trivial behavior change should include tests.

Prioritize tests for:

- Services.
- Repositories.
- Config parsing.
- Command parsing and command results.
- Economy behavior.
- Claim/permission logic.
- RTP safety decisions.
- Scheduler boundary logic.
- Optional integration fallback behavior.

If Bukkit runtime behavior is hard to test directly, move decision logic behind a service and test that service.

Do not make code less testable just because the feature is Minecraft-specific.

### 21. Keep changes small and reviewable

Prefer focused patches.

Avoid mixing:

- Refactors.
- Formatting-only changes.
- Feature changes.
- Bug fixes.
- Config migrations.
- Large package moves.

A good change should be easy to review and easy to revert.

When refactoring legacy code, port one behavior slice at a time and leave a tested path behind.

### 22. Treat safety-critical gameplay as extra strict

Some plugin behavior can damage player trust or server state. Be extra conservative around:

- Teleport destination validation.
- Economy withdrawals/deposits.
- Faction claims/unclaims.
- Faction disbanding.
- Permission bypasses.
- Staff/admin commands.
- Database migrations.
- Cross-server/proxy behavior.
- Async save/load logic.
- Optional integration fallbacks.

For these areas:

- Validate inputs twice where needed.
- Prefer safe failure over risky success.
- Log important admin-impacting failures.
- Add tests for both success and failure paths.
- Do not silently change existing behavior.

### 23. Minecraft server compatibility comes first

Do not assume every server runs the same platform, version, plugin set, or configuration.

Code should degrade gracefully where possible.

For multi-platform support:

- Keep shared logic platform-neutral.
- Put platform-specific behavior in platform modules/adapters.
- Avoid using Paper-only APIs in common Bukkit/Spigot code.
- Keep Folia scheduling rules in mind.
- Do not break startup because an optional dependency is missing.

### 24. Agent-specific rules

When working as a coding agent:

- Read the existing architecture before adding new patterns.
- Follow the repository’s current service/repository/bootstrap/module layout.
- Do not introduce a new framework unless explicitly requested.
- Do not rewrite unrelated code.
- Do not remove legacy/reference files unless the task explicitly asks for cleanup.
- Do not silently change public commands, permissions, config keys, or message keys.
- Add tests where practical.
- Keep the final patch focused on the requested task.
- Mention verification commands run and any skipped checks.