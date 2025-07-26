---
title: Scheduled Command Executor
description: A Java utility to execute scheduled shell commands based on time-based patterns.
---

# Scheduled Command Executor

Reads a list of scheduled commands from a file and executes them based on defined one-time or recurring intervals.

## Key Assumptions

- **Commands file path**:
  - Unix/Linux/macOS: `/tmp/commands.txt`
  - Windows: `C:\tmp\commands.txt`
- **Times** are interpreted using the **system's time zone**.
- Commands are **OS-compatible** and executed with necessary permissions.
- One-time past commands are **skipped**.
- Allowed recurring intervals: `1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60` minutes.
- Logs errors; invalid or failed commands are **skipped without crashing**.

## Command Formats

### For Unix/Linux/macOS
```bash
*/1 date && echo "Amex motto is 'Don't live life without it!'"
*/2 uptime && echo "Amex was founded in 1850."
*/5 ls -la
30 17 30 12 2025 echo "This is a one-time command for future date"
```

### For Windows

```cmd
*/1 date /t && echo Amex motto is 'Don't live life without it!'
*/2 time /t && echo Amex was founded in 1850.
*/5 dir C:\
30 17 30 12 2025 echo This is a one-time command for future date
```

## Requirements

- Java 16 or higher (for `record` feature)
- Read and execute permissions for command file and shell

## Usage

1. Create the commands file:
   - **Unix**: `/tmp/commands.txt`
   - **Windows**: `C:\tmp\commands.txt`

2. Add commands using the supported formats.

3. Compile and run:
   ```bash
   javac ScheduledCommandExecutor.java
   java ScheduledCommandExecutor
   ```
