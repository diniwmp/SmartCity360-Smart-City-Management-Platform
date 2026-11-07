package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.ZoneEntity;
import com.urbantech.smartcity360.model.enums.PowerStatus;
import com.urbantech.smartcity360.model.enums.ZoneType;

import java.util.Collection;
import java.util.List;

public class ZoneDAO extends BaseDAO<ZoneEntity> {

    public ZoneDAO() {
        super(ZoneEntity.class);
    }

    public List<ZoneEntity> findAllWithParent() {
        return read(session -> session
                .createQuery("select z from ZoneEntity z left join fetch z.parent order by z.id", ZoneEntity.class)
                .getResultList());
    }

    public boolean existsByCode(String code) {
        return read(session -> session
                .createQuery("select count(z) from ZoneEntity z where z.code = :code", Long.class)
                .setParameter("code", code)
                .getSingleResult() > 0);
    }

    public List<ZoneEntity> findByType(ZoneType type) {
        return read(session -> session
                .createQuery("from ZoneEntity z where z.zoneType = :type order by z.name", ZoneEntity.class)
                .setParameter("type", type)
                .getResultList());
    }

    public int updatePowerStatus(Collection<Long> zoneIds, PowerStatus status) {
        if (zoneIds.isEmpty()) {
            return 0;
        }
        return write(session -> session
                .createMutationQuery("update ZoneEntity z set z.powerStatus = :status where z.id in :ids")
                .setParameter("status", status)
                .setParameterList("ids", zoneIds)
                .executeUpdate());
    }
}