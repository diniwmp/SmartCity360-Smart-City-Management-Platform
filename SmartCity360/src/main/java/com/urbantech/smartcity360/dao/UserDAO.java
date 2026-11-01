package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.User;
import com.urbantech.smartcity360.model.enums.UserRole;

import java.util.Optional;

public class UserDAO extends BaseDAO<User> {

    public UserDAO() {
        super(User.class);
    }

    public Optional<User> findByUsername(String username) {
        return read(session -> session
                .createQuery("from User u where u.username = :username", User.class)
                .setParameter("username", username)
                .uniqueResultOptional());
    }

    public boolean existsByUsername(String username) {
        return existsByField("username", username);
    }

    public boolean existsByEmail(String email) {
        return existsByField("email", email);
    }

    /** field නම පැමිණෙන්නේ ඉහත public methods වලින් පමණි (user input නොවේ). */
    private boolean existsByField(String field, String value) {
        return read(session -> session
                .createQuery("select count(u) from User u where u." + field + " = :value", Long.class)
                .setParameter("value", value)
                .getSingleResult() > 0);
    }

    // ---- dao/UserDAO.java : මේ method එක එකතු කරන්න (import: UserRole)
    /** Role එකට අදාළ පළමු active user (complaint assignee ලෙස). */
    public Optional<User> findFirstActiveByRole(UserRole role) {
        return read(session -> session
                .createQuery("from User u where u.role = :role and u.active = true order by u.id", User.class)
                .setParameter("role", role)
                .setMaxResults(1)
                .uniqueResultOptional());
    }
}