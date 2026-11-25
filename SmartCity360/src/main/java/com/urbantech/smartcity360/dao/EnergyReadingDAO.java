package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.dto.MeterStats;
import com.urbantech.smartcity360.model.EnergyReadingEntity;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class EnergyReadingDAO extends BaseDAO<EnergyReadingEntity> {

    public EnergyReadingDAO() {
        super(EnergyReadingEntity.class);
    }

    public List<EnergyReadingEntity> findRecent(Long meterId, int limit) {
        String hql = "select r from EnergyReadingEntity r join fetch r.meter m join fetch m.zone"
                + (meterId != null ? " where m.id = :meterId" : "")
                + " order by r.readingTime desc, r.id desc";
        return read(session -> {
            Query<EnergyReadingEntity> query = session.createQuery(hql, EnergyReadingEntity.class).setMaxResults(limit);
            if (meterId != null) {
                query.setParameter("meterId", meterId);
            }
            return query.getResultList();
        });
    }

    public Optional<EnergyReadingEntity> findByIdWithMeter(Long id) {
        return read(session -> session
                .createQuery("select r from EnergyReadingEntity r join fetch r.meter m join fetch m.zone where r.id = :id",
                        EnergyReadingEntity.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    public List<MeterStats> summarizeByMeter() {
        return read(session -> session
                .createQuery("select new com.urbantech.smartcity360.dto.MeterStats("
                        + "r.meter.id, count(r), sum(r.kwhValue), max(r.readingTime)) "
                        + "from EnergyReadingEntity r group by r.meter.id", MeterStats.class)
                .getResultList());
    }

    public double sumKwh() {
        return read(session -> session
                .createQuery("select coalesce(sum(r.kwhValue), 0.0) from EnergyReadingEntity r", Double.class)
                .getSingleResult());
    }

    public long countSecured() {
        return read(session -> session
                .createQuery("select count(r) from EnergyReadingEntity r "
                        + "where r.layersApplied is not null and r.layersApplied <> ''", Long.class)
                .getSingleResult());
    }

    public int updateSecuredPayload(Long id, String payload) {
        return write(session -> session
                .createMutationQuery("update EnergyReadingEntity r set r.securedPayload = :payload where r.id = :id")
                .setParameter("payload", payload)
                .setParameter("id", id)
                .executeUpdate());
    }
}