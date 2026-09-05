package org.example.userservice.console;

import org.example.userservice.database.entity.User;
import org.example.userservice.dto.CreateUserRq;
import org.example.userservice.dto.UpdateUserRq;
import org.example.userservice.service.UserService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class ConsoleMenu {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserService userService;
    private final Scanner scanner;

    public ConsoleMenu(UserService userService) {
        this(userService, new Scanner(System.in));
    }

    ConsoleMenu(UserService userService, Scanner scanner) {
        this.userService = userService;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("User Service");

        while (true) {
            printMenu();
            String command = scanner.nextLine().trim();

            try {
                switch (command) {
                    case "1" -> createUser();
                    case "2" -> findUserById();
                    case "3" -> showAllUsers();
                    case "4" -> updateUser();
                    case "5" -> deleteUser();
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Unknown command. Please choose 0-5.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Operation failed: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("1. Create user");
        System.out.println("2. Find user by id");
        System.out.println("3. Show all users");
        System.out.println("4. Update user");
        System.out.println("5. Delete user");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    private void createUser() {
        System.out.print("Name: ");
        String name = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        int age = readInt("Age: ");

        CreateUserRq request = new CreateUserRq(name, email, age);
        User user = userService.createUser(request);
        System.out.println("User created with id: " + user.getId());
    }

    private void findUserById() {
        long id = readLong("User id: ");

        userService.getUserById(id)
                .ifPresentOrElse(this::printUser,
                        () -> System.out.println("User not found"));
    }

    private void showAllUsers() {
        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users found");
            return;
        }

        users.forEach(this::printUser);
    }

    private void updateUser() {
        long id = readLong("User id: ");

        System.out.print("New name: ");
        String name = scanner.nextLine();

        System.out.print("New email: ");
        String email = scanner.nextLine();

        int age = readInt("New age: ");

        UpdateUserRq request = new UpdateUserRq(id, name, email, age);
        User user = userService.updateUser(request);
        System.out.println("User updated:");
        printUser(user);
    }

    private void deleteUser() {
        long id = readLong("User id: ");

        if (userService.deleteUser(id)) {
            System.out.println("User deleted");
        } else {
            System.out.println("User not found");
        }
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected an integer number");
        }
    }

    private long readLong(String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected an integer number");
        }
    }

    private void printUser(User user) {
        String createdAt = user.getCreatedAt() == null
                ? "-"
                : user.getCreatedAt().format(DATE_TIME_FORMATTER);

        System.out.printf(
                "id=%s, name=%s, email=%s, age=%s, createdAt=%s%n",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                createdAt
        );
    }
}