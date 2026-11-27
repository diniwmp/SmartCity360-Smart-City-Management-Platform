package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.NotificationEntity;
import com.urbantech.smartcity360.model.enums.AlertType;
import com.urbantech.smartcity360.model.enums.NotificationChannel;
import com.urbantech.smartcity360.model.enums.SeverityLevel;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDAO extends BaseDAO<NotificationEntity> {

    public NotificationDAO() {
        super(NotificationEntity.class);
    }


    public List<NotificationEntity> findInbox(Long userId, boolean unreadOnly, int limit) {
        String hql = "from NotificationEntity n where n.recipient.id = :userId and n.channel = :channel"
                + (unreadOnly ? " and n.read = false" : "")
                + " order by n.createdAt desc, n.id desc";
        return read(session -> session.createQuery(hql, NotificationEntity.class)
                .setParameter("userId", userId)
                .setParameter("channel", NotificationChannel.DASHBOARD)
                .setMaxResults(limit)
                .getResultList());
    }

    public long countInbox(Long userId, boolean unreadOnly, SeverityLevel severity) {
        String hql = "select count(n) from NotificationEntity n where n.recipient.id = :userId and n.channel = :channel"
                + (unreadOnly ? " and n.read = false" : "")
                + (severity != null ? " and n.severity = :severity" : "");
        return read(session -> {
            Query<Long> query = session.createQuery(hql, Long.class)
                    .setParameter("userId", userId)
                    .setParameter("channel", NotificationChannel.DASHBOARD);
            if (severity != null) {
                query.setParameter("severity", severity);
            }
            return query.getSingleResult();
        });
    }

    public List<NotificationEntity> findDeliveries(int limit) {
        return read(session -> session
                .createQuery("select n from NotificationEntity n join fetch n.recipient where n.channel <> :dashboard "
                        + "order by n.createdAt desc, n.id desc", NotificationEntity.class)
                .setParameter("dashboard", NotificationChannel.DASHBOARD)
                .setMaxResults(limit)
                .getResultList());
    }

    public long countDeliveries() {
        return read(session -> session
                .createQuery("select count(n) from NotificationEntity n where n.channel <> :dashboard", Long.class)
                .setParameter("dashboard", NotificationChannel.DASHBOARD)
                .getSingleResult());
    }

    public boolean existsRecent(AlertType type, String sourceRef, LocalDateTime since) {
        return read(session -> session
                .createQuery("select count(n) from NotificationEntity n where n.alertType = :type "
                        + "and n.sourceRef = :ref and n.createdAt >= :since", Long.class)
                .setParameter("type", type)
                .setParameter("ref", sourceRef)
                .setParameter("since", since)
                .getSingleResult() > 0);
    }


    public int markRead(Long id, Long userId) {
        return write(session -> session
                .createMutationQuery("update NotificationEntity n set n.read = true "
                        + "where n.id = :id and n.recipient.id = :userId")
                .setParameter("id", id)
                .setParameter("userId", userId)
                .executeUpdate());
    }

    public int markAllRead(Long userId) {
        return write(session -> session
                .createMutationQuery("update NotificationEntity n set n.read = true "
                        + "where n.recipient.id = :userId and n.channel = :channel and n.read = false")
                .setParameter("userId", userId)
                .setParameter("channel", NotificationChannel.DASHBOARD)
                .executeUpdate());
    }
}