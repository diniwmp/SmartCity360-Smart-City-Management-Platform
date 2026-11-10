package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.SensorEntity;
import com.urbantech.smartcity360.model.enums.OperationalStatus;
import com.urbantech.smartcity360.model.enums.SensorProtocol;

import java.util.List;
import java.util.Optional;

public class SensorDAO extends BaseDAO<SensorEntity> {

    private static final String SELECT_WITH_ZONE = "select s from SensorEntity s left join fetch s.zone";

    public SensorDAO() {
        super(SensorEntity.class);
    }

    public List<SensorEntity> findAllWithZone() {
        return read(session -> session
                .createQuery(SELECT_WITH_ZONE + " order by s.id", SensorEntity.class)
                .getResultList());
    }

    public List<SensorEntity> findActiveWithZone() {
        return read(session -> session
                .createQuery(SELECT_WITH_ZONE + " where s.status = :status order by s.id", SensorEntity.class)
                .setParameter("status", OperationalStatus.ACTIVE)
                .getResultList());
    }

    public Optional<SensorEntity> findByIdWithZone(Long id) {
        return read(session -> session
                .createQuery(SELECT_WITH_ZONE + " where s.id = :id", SensorEntity.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    public boolean existsBySensorCode(String code) {
        return read(session -> session
                .createQuery("select count(s) from SensorEntity s where s.sensorCode = :code", Long.class)
                .setParameter("code", code)
                .getSingleResult() > 0);
    }

    public long countByStatus(OperationalStatus status) {
        return read(session -> session
                .createQuery("select count(s) from SensorEntity s where s.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult());
    }

    public int updateProtocol(Long id, SensorProtocol protocol) {
        return write(session -> session
                .createMutationQuery("update SensorEntity s set s.protocol = :protocol where s.id = :id")
                .setParameter("protocol", protocol)
                .setParameter("id", id)
                .executeUpdate());
    }
}