package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.exception.DataAccessException;
import com.urbantech.smartcity360.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public abstract class BaseDAO<T> {

    private final Class<T> entityClass;

    protected BaseDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected <R> R read(Function<Session, R> query) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return query.apply(session);
        } catch (RuntimeException e) {
            throw new DataAccessException("Read failed for " + entityClass.getSimpleName(), e);
        }
    }

    protected <R> R write(Function<Session, R> action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                R result = action.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        } catch (RuntimeException e) {
            throw new DataAccessException("Write failed for " + entityClass.getSimpleName(), e);
        }
    }

    public T save(T entity) {
        return write(session -> {
            session.persist(entity);
            return entity;
        });
    }

    public T update(T entity) {
        return write(session -> session.merge(entity));
    }

    public void delete(T entity) {
        write(session -> {
            session.remove(session.contains(entity) ? entity : session.merge(entity));
            return null;
        });
    }

    public Optional<T> findById(Long id) {
        return read(session -> Optional.ofNullable(session.get(entityClass, id)));
    }

    public List<T> findAll() {
        return read(session -> session
                .createQuery("from " + entityClass.getSimpleName(), entityClass)
                .getResultList());
    }

    public long count() {
        return read(session -> session
                .createQuery("select count(e) from " + entityClass.getSimpleName() + " e", Long.class)
                .getSingleResult());
    }
}