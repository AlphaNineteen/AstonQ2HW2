package com.example.userservice.dao;

import com.example.userservice.config.HibernateUtil;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DaoException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);


    private <T> T executeInTransaction(String operation, Function<Session, T> action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T result = action.apply(session);
                tx.commit();
                return result;
            } catch (Exception e) {
                log.error("Ошибка при '{}', откат транзакции: {}", operation, e.getMessage(), e);
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                throw translate(operation, e);
            }
        } catch (DaoException e) {
            throw e;
        } catch (HibernateException e) {
            log.error("HibernateException при '{}': {}", operation, e.getMessage(), e);
            throw translate(operation, e);
        }
    }

    private DaoException translate(String operation, Throwable e) {
        if (e instanceof ConstraintViolationException) {
            SQLException sql = ((ConstraintViolationException) e).getSQLException();

            if (sql != null && "23505".equals(sql.getSQLState())) {
                return new DaoException("Пользователь с таким email уже существует", e);
            }
            return new DaoException("Нарушение ограничения БД при '" + operation + "'", e);
        }
        if (e instanceof HibernateException) {
            return new DaoException("Ошибка Hibernate при операции '" + operation + "'", e);
        }
        return new DaoException("Неожиданная ошибка при '" + operation + "'", e);
    }

    @Override
    public User create(User user) {
        return executeInTransaction("создание пользователя", session -> {
            session.persist(user);
            log.info("Создан пользователь id={}", user.getId());
            return user;
        });
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(User.class, id));
        } catch (HibernateException e) {
            log.error("Ошибка при поиске id={}: {}", id, e.getMessage(), e);
            throw translate("поиск пользователя", e);
        }
    }

    @Override
    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User ORDER BY id", User.class).list();
        } catch (HibernateException e) {
            log.error("Ошибка при получении списка: {}", e.getMessage(), e);
            throw translate("получение списка", e);
        }
    }

    @Override
    public User update(User user) {
        return executeInTransaction("обновление пользователя", session -> {
            User updated = session.merge(user);
            log.info("Обновлён пользователь id={}", updated.getId());
            return updated;
        });
    }

    @Override
    public boolean deleteById(Long id) {
        return executeInTransaction("удаление пользователя", session -> {
            User user = session.get(User.class, id);
            if (user != null) {
                session.remove(user);
                log.info("Удалён пользователь id={}", id);
                return true;
            }
            return false;
        });
    }
}