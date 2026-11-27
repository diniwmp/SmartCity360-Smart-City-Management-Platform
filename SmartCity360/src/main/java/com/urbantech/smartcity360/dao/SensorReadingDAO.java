package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.SensorReadingEntity;
import org.hibernate.query.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorReadingDAO extends BaseDAO<SensorReadingEntity> {

    public SensorReadingDAO() {
        super(SensorReadingEntity.class);
    }

    public List<SensorReadingEntity> findRecent(Long sensorId, int limit) {
        String hql = "select r from SensorReadingEntity r join fetch r.sensor s left join fetch s.zone"
                + (sensorId != null ? " where s.id = :sensorId" : "")
                + " order by r.readingTime desc, r.id desc";
        return read(session -> {
            Query<SensorReadingEntity> query = session.createQuery(hql, SensorReadingEntity.class).setMaxResults(limit);
            if (sensorId != null) {
                query.setParameter("sensorId", sensorId);
            }
            return query.getResultList();
        });
    }

    public Map<Long, SensorReadingEntity> latestPerSensor() {
        return read(session -> {
            Map<Long, SensorReadingEntity> latest = new HashMap<>();
            session.createQuery("select r from SensorReadingEntity r join fetch r.sensor where r.id in "
                                    + "(select max(r2.id) from SensorReadingEntity r2 group by r2.sensor.id)",
                            SensorReadingEntity.class)
                    .getResultList()
                    .forEach(reading -> latest.put(reading.getSensor().getId(), reading));
            return latest;
        });
    }
}