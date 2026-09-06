package org.example.userservice;

import org.example.userservice.console.ConsoleMenu;
import org.example.userservice.database.dao.UserDao;
import org.example.userservice.database.dao.impl.UserDaoImpl;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.service.UserService;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class Main {
    public static void main(String[] args) {
        SessionFactory sessionFactory = new Configuration()
                .configure("hibernate.cfg.xml")
                .buildSessionFactory();
        try {
            UserDao userDao = new UserDaoImpl(sessionFactory);

            UserService userService = new UserService(userDao, new UserMapper());
            ConsoleMenu consoleMenu = new ConsoleMenu(userService);
            consoleMenu.run();
        } finally {
            sessionFactory.close();
        }
    }
}
