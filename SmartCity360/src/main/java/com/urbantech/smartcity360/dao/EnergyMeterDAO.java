package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.EnergyMeterEntity;
import com.urbantech.smartcity360.model.enums.OperationalStatus;

import java.util.List;
import java.util.Optional;

public class EnergyMeterDAO extends BaseDAO<EnergyMeterEntity> {

    public EnergyMeterDAO() {
        super(EnergyMeterEntity.class);
    }

    public List<EnergyMeterEntity> findAllWithZone() {
        return read(session -> session
                .createQuery("select m from EnergyMeterEntity m join fetch m.zone order by m.id", EnergyMeterEntity.class)
                .getResultList());
    }

    public List<EnergyMeterEntity> findActiveWithZone() {
        return read(session -> session
                .createQuery("select m from EnergyMeterEntity m join fetch m.zone where m.status = :status order by m.id",
                        EnergyMeterEntity.class)
                .setParameter("status", OperationalStatus.ACTIVE)
                .getResultList());
    }

    public Optional<EnergyMeterEntity> findByIdWithZone(Long id) {
        return read(session -> session
                .createQuery("select m from EnergyMeterEntity m join fetch m.zone where m.id = :id", EnergyMeterEntity.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    public boolean existsByMeterCode(String meterCode) {
        return read(session -> session
                .createQuery("select count(m) from EnergyMeterEntity m where m.meterCode = :code", Long.class)
                .setParameter("code", meterCode)
                .getSingleResult() > 0);
    }

    public long countByStatus(OperationalStatus status) {
        return read(session -> session
                .createQuery("select count(m) from EnergyMeterEntity m where m.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult());
    }
}