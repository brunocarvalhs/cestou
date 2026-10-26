# CLAUDE.md

Project-level instructions for any Claude Code session (main or subagent) working in this repo.

## Code comment policy

Comments in source files (`.kt`, `.kts`, and friends) are **structural/technical only** — they
document what a declaration is and does, not how it came to exist.

**Allowed**: KDoc on public API, Google's Kotlin style
(https://developer.android.com/kotlin/style-guide#kdoc) — a one-line summary, `@param`/`@return`/
`@throws` as needed, and inline comments only where the *why* is genuinely non-obvious (a hidden
constraint, a workaround for a specific platform bug, a subtle invariant).

**Never put in a source comment**:
- References to an AI agent, model, session, or tool ("per the agent's analysis", "Claude added
  this", "/model opus", "this subagent decided").
- References to a spec requirement ID, task ID, PR number, or Gxx roadmap item (`IAA-02`, `PR #67`,
  `G17`, `AD-013`). That context belongs in the commit message and `.specs/`, where it can be
  cross-referenced and doesn't rot in place when the spec changes.
- Narration of the current task/fix/session ("added for the Y flow", "fixes the bug found in
  review", "handles the case from issue #123").

If you're about to write a comment explaining *why a decision was made* rather than *what the code
does*, put that reasoning in the commit message or the relevant `.specs/features/*/spec.md` /
`.specs/STATE.md` entry instead — not in the file.

**Example**:
```kotlin
// Bad — process/agent-referential, will rot, says nothing about the code itself
// Checked before save per spec item-add-authorship P2, IAA-02 (added by pm session 2026-10-23)
val duplicate = duplicateCheckUseCase(product.name, shopping.id)

// Bad — narrates the task instead of documenting the method
// Fix for G21: now records a real total when finishing from the home screen
fun finishPurchase(total: Double) { ... }

// Good — KDoc states what the function does and its contract, nothing about why/who/when
/**
 * Marks [shopping] as finished with the given [total] and [establishment], recording
 * [Shopping.finishedAt] as the current time.
 *
 * @return [Result.failure] if the user is not a member of the list.
 */
suspend fun finishPurchase(shopping: Shopping, total: Double, establishment: String?): Result<Unit>
```

This applies to every source file this repo's agents touch, regardless of which agent or session
wrote it.
