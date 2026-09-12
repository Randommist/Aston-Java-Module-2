package org.example.userservice;

import org.example.userservice.console.ConsoleMenu;
import org.example.userservice.database.dao.UserDao;
import org.example.userservice.database.dao.impl.UserDaoImpl;
import org.example.userservice.mapper.UserMapper;
import org.example.userservice.service.UserService;
import org.example.userservice.util.HibernateUtil;
import org.hibernate.SessionFactory;

public class Main {
    public static void main(String[] args) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

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
