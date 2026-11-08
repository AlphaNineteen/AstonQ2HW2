package com.example.userservice;

import com.example.userservice.config.HibernateUtil;
import com.example.userservice.dao.UserDao;
import com.example.userservice.dao.UserDaoImpl;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final UserDao userDao = new UserDaoImpl();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        log.info("Запуск user-service");
        System.out.println("=== User Service (Hibernate + PostgreSQL) ===");

        try {
            runMenu();
        } finally {
            HibernateUtil.shutdown();
            log.info("Приложение завершено");
        }
    }

    private static void runMenu() {
        while (true) {
            printMenu();
            System.out.print("Выберите действие: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> create();
                    case "2" -> findById();
                    case "3" -> findAll();
                    case "4" -> update();
                    case "5" -> delete();
                    case "0" -> {
                        System.out.println("Выход.");
                        return;
                    }
                    default -> System.out.println("Неизвестная команда.");
                }
            } catch (DaoException e) {
                System.err.println("Ошибка БД: " + e.getMessage());
            } catch (Exception e) {
                log.error("Непредвиденная ошибка", e);
                System.err.println("Непредвиденная ошибка: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("""
                
                --- Меню ---
                1. Создать пользователя
                2. Найти пользователя по ID
                3. Показать всех пользователей
                4. Обновить пользователя
                5. Удалить пользователя
                0. Выход
                """);
    }

    private static void create() {
        User user = readUserData(null);
        User saved = userDao.create(user);
        System.out.println("Создан пользователь: " + saved);
    }

    private static void findById() {
        Long id = readId();
        userDao.findById(id)
                .ifPresentOrElse(System.out::println,
                        () -> System.out.println("Пользователь id=" + id + " не найден"));
    }

    private static void findAll() {
        List<User> users = userDao.findAll();
        if (users.isEmpty()) {
            System.out.println("Пользователей нет.");
        } else {
            users.forEach(System.out::println);
        }
    }

    private static void update() {
        Long id = readId();
        User existing = userDao.findById(id)
                .orElseThrow(() -> new DaoException("Пользователь id=" + id + " не найден", null));
        System.out.println("Текущие данные: " + existing);
        User updated = readUserData(existing);
        System.out.println("Обновлено: " + userDao.update(updated));
    }

    private static void delete() {
        Long id = readId();
        System.out.println(userDao.deleteById(id)
                ? "Пользователь удалён." : "Пользователь id=" + id + " не найден.");
    }


    private static User readUserData(User existing) {
        User user = existing != null ? existing : new User();

        System.out.print("Имя" + (existing != null ? " [" + existing.getName() + "]" : "") + ": ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) user.setName(name);

        System.out.print("Email" + (existing != null ? " [" + existing.getEmail() + "]" : "") + ": ");
        String email = scanner.nextLine().trim();
        if (!email.isEmpty()) user.setEmail(email);

        while (true) {
            String prompt = "Возраст" + (existing != null && existing.getAge() != null
                    ? " [" + existing.getAge() + "]" : "") + ": ";
            System.out.print(prompt);
            String ageInput = scanner.nextLine().trim();
            if (ageInput.isEmpty()) break;
            try {
                int age = Integer.parseInt(ageInput);
                if (age < 0 || age > 150) {
                    System.out.println("Возраст должен быть от 0 до 150.");
                    continue;
                }
                user.setAge(age);
                break;
            } catch (NumberFormatException e) {
                System.out.println("Введите целое число.");
            }
        }
        return user;
    }

    private static Long readId() {
        while (true) {
            System.out.print("Введите ID: ");
            try {
                return Long.parseLong(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("ID должен быть числом.");
            }
        }
    }
}