---
name: accessibility
description: Guidelines for implementing W3C WCAG accessibility standards in Android
  apps using Jetpack Compose. Focuses on Perceivable, Operable, Understandable, and
  Robust principles to ensure the app is usable by everyone, including users of TalkBack,
  Switch Access, and other assistive technologies.
license: Complete terms in LICENSE.txt
metadata:
  author: Cestou Team
  last-updated: '2026-07-31'
  keywords:
  - Accessibility
  - WCAG
  - TalkBack
  - Semantics
  - ContentDescription
  - Touch Targets
---

## Perceivable

Ensure that information and user interface components are presentable to users in ways they can perceive.

### 1. Text Alternatives
- Every interactive `Icon` or `IconButton` MUST have a meaningful `contentDescription`.
- Decorative images SHOULD have `contentDescription = null` to be ignored by screen readers.
- Use `stringResource` for all descriptions to support localization.

### 2. Semantics
- Use `Modifier.semantics { heading() }` for screen titles and section headers.
- Use `Modifier.semantics(mergeDescendants = true)` to group related information (e.g., a list item with name, price, and status) so it's read as a single block.

### 3. Color Contrast
- Ensure a contrast ratio of at least 4.5:1 for normal text and 3:1 for large text.
- Don't rely on color alone to convey information (e.g., use icons + colors for error states).

## Operable

User interface components and navigation must be operable.

### 1. Touch Targets
- Interactive elements MUST have a minimum size of 48x48dp.
- Use `Modifier.padding` or `Box` with `sizeIn(minWidth = 48.dp, minHeight = 48.dp)` to expand small targets.

### 2. Focus Management
- Ensure a logical focus order (usually top-to-bottom, left-to-right).
- Use `FocusRequester` to move focus to new elements (like a newly opened dialog or a revealed search bar).

## Understandable

Information and the operation of the user interface must be understandable.

### 1. Labeling
- Form fields MUST have clear labels (`label` parameter in `TextField`).
- Buttons SHOULD have clear action verbs (e.g., "Save", "Delete", "Add item").

### 2. State Feedback
- Communicate state changes clearly. For example, use `Modifier.semantics { stateDescription = ... }` to announce if a toggle is "Active" or "Paused".

## Step-by-Step Implementation Workflow

### Step 1: Audit Interactive Elements
1. Search for `IconButton`, `Icon` within `clickable` modifiers, and `Switch`.
2. Verify if `contentDescription` is present and localized.

### Step 2: Grouping and Flow
1. Identify list items or cards that contain multiple pieces of information.
2. Apply `mergeDescendants = true` to the root modifier of these items.
3. Test with TalkBack to ensure the reading flow is natural.

### Step 3: Headings and Landmarks
1. Mark top-level titles in Scaffolds or Headers as `heading()`.
2. Ensure the first element of a screen gives context about where the user is.

### Step 4: Validation
1. Use the **Layout Inspector** to verify the semantic tree.
2. Enable **TalkBack** on a physical device or emulator and navigate the entire flow.
3. Use the **Accessibility Scanner** app from Google to find common issues.
