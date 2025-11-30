# AI Agent Instruction Keywords Specification

## Purpose
This document defines a set of keywords for providing clear, unambiguous instructions to AI agents. These keywords are inspired by RFC 2119 and adapted for AI-specific contexts.

## Interpretive Statement
The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in AI agent instructions are to be interpreted as described below.

## Core Requirement Keywords

### 1. MUST / REQUIRED / SHALL
**Definition**: This indicates an absolute requirement. The AI agent has no discretion and must follow this instruction exactly.
**Use case**: Critical safety requirements, legal compliance, or core functionality.
**Example**: "The AI agent MUST verify user authentication before accessing sensitive data."

### 2. MUST NOT / SHALL NOT
**Definition**: This indicates an absolute prohibition. The AI agent is forbidden from performing the specified action under any circumstances.
**Use case**: Preventing harmful actions, ensuring security boundaries.
**Example**: "The AI agent MUST NOT generate or execute malicious code."

### 3. SHOULD / RECOMMENDED
**Definition**: This indicates a strong preference that should be followed unless there are valid, documented reasons to deviate.
**Use case**: Best practices, optimal approaches.
**Example**: "The AI agent SHOULD use existing files rather than creating new ones."

### 4. SHOULD NOT / NOT RECOMMENDED
**Definition**: This indicates behavior that is generally discouraged but may be acceptable in specific, justified circumstances.
**Use case**: Practices to avoid unless necessary.
**Example**: "The AI agent SHOULD NOT include unnecessary comments in generated code."

### 5. MAY / OPTIONAL
**Definition**: This indicates truly optional behavior. The AI agent can choose whether to perform this action based on context.
**Use case**: Additional features, enhancements.
**Example**: "The AI agent MAY provide additional context if it improves clarity."

## AI-Specific Keywords

### 6. INFER
**Definition**: The AI agent should use context and reasoning to determine appropriate values or actions when not explicitly specified.
**Use case**: Handling ambiguous instructions intelligently.
**Example**: "The AI agent SHALL INFER the programming language from file extensions."

### 7. VERIFY
**Definition**: The AI agent must check and confirm before proceeding with potentially impactful actions.
**Use case**: Preventing errors, ensuring accuracy.
**Example**: "The AI agent MUST VERIFY file existence before attempting edits."

### 8. PRIORITIZE
**Definition**: When multiple valid options exist, the AI agent should rank and select based on specified criteria.
**Use case**: Decision-making with multiple constraints.
**Example**: "The AI agent SHALL PRIORITIZE security over performance optimization."

### 9. PRESERVE
**Definition**: The AI agent must maintain existing structures, styles, or patterns unless explicitly instructed otherwise.
**Use case**: Maintaining code consistency, respecting existing work.
**Example**: "The AI agent MUST PRESERVE the existing code formatting style."

### 10. CLARIFY
**Definition**: The AI agent should request additional information when instructions are ambiguous or critical information is missing.
**Use case**: Preventing misunderstandings, ensuring accuracy.
**Example**: "The AI agent SHOULD CLARIFY the target file path if multiple matches exist."

## Contextual Modifiers

### ALWAYS
**Definition**: Applies in every instance without exception throughout the entire session.
**Example**: "ALWAYS use absolute file paths."

### NEVER
**Definition**: Prohibits an action in every instance without exception throughout the entire session.
**Example**: "NEVER commit changes without explicit user request."

### ONLY
**Definition**: Restricts action to specific conditions, excluding all others.
**Example**: "ONLY create documentation files when explicitly requested."

### IMMEDIATELY
**Definition**: Requires action without delay or intermediate steps.
**Example**: "IMMEDIATELY stop execution if a security violation is detected."

## Usage Guidelines

1. Use keywords sparingly and only where necessary for clarity
2. Apply keywords consistently throughout instructions
3. Combine keywords when needed: "The AI agent MUST IMMEDIATELY VERIFY..."
4. Document any session-specific interpretations
5. Consider the principle of least surprise - instructions should be predictable

## Examples in Context

```
# File Operations
The AI agent MUST read files before editing them.
The AI agent SHOULD PRESERVE existing code style.
The AI agent MUST NOT create files unless REQUIRED by the task.
The AI agent MAY INFER file locations from context.

# Code Generation
The AI agent SHALL follow language-specific best practices.
The AI agent SHOULD NOT add comments unless requested.
The AI agent MUST VERIFY syntax correctness.
The AI agent SHALL PRIORITIZE readability over brevity.

# User Interaction
The AI agent MUST CLARIFY ambiguous instructions.
The AI agent SHOULD provide concise responses.
The AI agent MAY offer additional suggestions when relevant.
The AI agent MUST NOT proceed with destructive operations without confirmation.
```

## Implementation Notes

- Keywords in uppercase indicate requirement levels
- Context and task requirements may influence interpretation
- When conflicts arise, more restrictive keywords take precedence
- Document any domain-specific keyword additions
