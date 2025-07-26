import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledCommandExecutor {
    private static final String COMMANDS_FILE_PATH = System.getProperty("os.name").toLowerCase().contains("windows")
            ? "C:\\tmp\\commands.txt"
            : "/tmp/commands.txt";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private static final List<OneTimeCommand> oneTimeCommands = new ArrayList<>();
    private static final List<RecurringCommand> recurringCommands = new ArrayList<>();

    public static void main(String[] args) {
        ScheduledCommandExecutor executor = new ScheduledCommandExecutor();
        executor.loadAndExecuteCommands();

        // Keep the main thread alive to allow scheduled tasks to run
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down scheduler...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }));
    }

    public void loadAndExecuteCommands() {
        loadCommandsFromFile();
        scheduleOneTimeCommands();
        scheduleRecurringCommands();
    }

    private void loadCommandsFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(COMMANDS_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("*/")) {
                    // Recurring command
                    parseAndAddRecurringCommand(line);
                } else {
                    // One-time command
                    parseAndAddOneTimeCommand(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading commands file: " + e.getMessage());
            System.err.println("Please ensure the file exists at: " + COMMANDS_FILE_PATH);
        }
    }

    private void parseAndAddRecurringCommand(String line) {
        try {
            // Format: */n <user command>
            int spaceIndex = line.indexOf(' ', 2);
            if (spaceIndex == -1) {
                System.err.println("Invalid recurring command format: " + line);
                return;
            }

            String intervalStr = line.substring(2, spaceIndex);
            int interval = Integer.parseInt(intervalStr);
            String command = line.substring(spaceIndex + 1).trim();

            // Validate interval
            int[] validIntervals = {1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60};
            boolean isValid = false;
            for (int validInterval : validIntervals) {
                if (interval == validInterval) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) {
                System.err.println("Invalid interval for recurring command: " + interval);
                return;
            }

            recurringCommands.add(new RecurringCommand(interval, command));
            System.out.println("Parsed recurring command: every " + interval + " minutes - " + command);
        } catch (NumberFormatException e) {
            System.err.println("Invalid interval format in recurring command: " + line);
        }
    }

    private void parseAndAddOneTimeCommand(String line) {
        try {
            String[] parts = line.split("\\s+", 6);
            if (parts.length < 6) {
                System.err.println("Invalid one-time command format (need 6 parts): " + line);
                System.err.println("Expected format: Minute Hour Day Month Year <command>");
                return;
            }

            int minute = Integer.parseInt(parts[0]);
            int hour = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            int month = Integer.parseInt(parts[3]);
            int year = Integer.parseInt(parts[4]);
            String command = parts[5];

            // Validate date/time values
            if (minute < 0 || minute > 59 ||
                    hour < 0 || hour > 23 ||
                    day < 1 || day > 31 ||
                    month < 1 || month > 12 ||
                    year < 1900 || year > 2100) {
                System.err.println("Invalid date/time values in command: " + line);
                return;
            }

            oneTimeCommands.add(new OneTimeCommand(minute, hour, day, month, year, command));
            System.out.println("Parsed one-time command: " + minute + " " + hour + " " + day + " " +
                    month + " " + year + " - " + command);
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in one-time command: " + line);
            System.err.println("Make sure first 5 parts are valid numbers");
        }
    }

    private void scheduleOneTimeCommands() {
        LocalDateTime now = LocalDateTime.now();

        for (OneTimeCommand command : oneTimeCommands) {
            LocalDateTime executionTime = LocalDateTime.of(
                    command.year(), command.month(), command.day(), command.hour(), command.minute());

            if (executionTime.isAfter(now)) {
                long delay = java.time.Duration.between(now, executionTime).getSeconds();
                scheduler.schedule(() -> executeCommand(command.command()), delay, TimeUnit.SECONDS);
                System.out.println("Scheduled one-time command [" + command.command() + "] for " + executionTime);
            } else {
                System.out.println("Skipping past one-time command: " + command.command());
            }
        }
    }

    private void scheduleRecurringCommands() {
        for (RecurringCommand command : recurringCommands) {
            // Schedule initial execution after first interval, then repeat
            scheduler.scheduleAtFixedRate(
                    () -> executeCommand(command.command()),
                    command.interval(), // Initial delay
                    command.interval(), // Period
                    TimeUnit.MINUTES
            );
            System.out.println("Scheduled recurring command every " + command.interval() +
                    " minutes: " + command.command());
        }
    }

    private void executeCommand(String command) {
        try {
            System.out.println("[" + java.time.LocalTime.now() + "] Executing command: " + command);
            ProcessBuilder processBuilder = new ProcessBuilder();

            // Handle shell commands properly
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }

            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            System.out.println("[" + java.time.LocalTime.now() + "] Command completed with exit code: " + exitCode);
        } catch (Exception e) {
            System.err.println("[" + java.time.LocalTime.now() + "] Error executing command: " + command + " - " + e.getMessage());
        }
    }

    record OneTimeCommand(int minute, int hour, int day, int month, int year, String command) {}

    record RecurringCommand(int interval, String command) {}
}