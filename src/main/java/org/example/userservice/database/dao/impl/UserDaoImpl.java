package org.example.userservice.database.dao.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.database.dao.UserDao;
import org.example.userservice.database.entity.User;
import org.example.userservice.exception.UserServiceException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.example.userservice.constant.ErrorCode.DB_ERROR;

@Slf4j
@AllArgsConstructor
public class UserDaoImpl implements UserDao {

    private final SessionFactory sessionFactory;

    @Override
    public User create(User user) {
        return executeInTransaction(session -> {
            session.persist(user);
            return user;
        });
    }

    @Override
    public Optional<User> findById(Long id) {
        return executeInTransaction(session -> Optional.ofNullable(session.find(User.class, id)));
    }

    @Override
    public List<User> findAll() {
        return executeInTransaction(session -> session.createQuery("from User", User.class).list());
    }

    @Override
    public User update(User user) {
        return executeInTransaction(session -> session.merge(user));
    }

    @Override
    public void deleteById(Long id) {
        executeInTransaction(session -> {
            User user = session.find(User.class, id);
            if (user != null) {
                session.remove(user);
            }
            return null;
        });
    }

    /**
     * Выполняет действие в рамках транзакции: открывает сессию, стартует транзакцию,
     * выполняет переданное действие и коммитит. При ошибке транзакция откатывается,
     * а исключение Hibernate оборачивается в {@link UserServiceException}.
     */
    private <T> T executeInTransaction(Function<Session, T> action) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T result = action.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                try {
                    if (transaction.isActive()) {
                        transaction.rollback();
                    }
                } catch (RuntimeException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        } catch (RuntimeException e) {
            log.error("Transaction failed", e);
            throw new UserServiceException(DB_ERROR, DB_ERROR.getMessage());
        }
    }
}

