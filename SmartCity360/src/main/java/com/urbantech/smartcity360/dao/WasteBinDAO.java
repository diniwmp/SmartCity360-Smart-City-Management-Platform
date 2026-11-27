package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.WasteBinEntity;
import com.urbantech.smartcity360.model.enums.BinStatus;
import org.hibernate.query.Query;

import java.time.LocalDateTime;
import java.util.*;

public class WasteBinDAO extends BaseDAO<WasteBinEntity> {

    private static final String SELECT_WITH_ZONE = "select b from WasteBinEntity b left join fetch b.zone";
    private static final String FULLEST_FIRST = " order by b.fillLevelPercent desc, b.id";

    public WasteBinDAO() {
        super(WasteBinEntity.class);
    }


    public List<WasteBinEntity> findPage(Collection<BinStatus> statuses, Long zoneId, int offset, int limit) {
        String hql = SELECT_WITH_ZONE + whereClause(statuses, zoneId) + FULLEST_FIRST;
        return read(session -> {
            Query<WasteBinEntity> query = session.createQuery(hql, WasteBinEntity.class)
                    .setFirstResult(offset)
                    .setMaxResults(limit);
            bindFilters(query, statuses, zoneId);
            return query.getResultList();
        });
    }

    public long countFiltered(Collection<BinStatus> statuses, Long zoneId) {
        String hql = "select count(b) from WasteBinEntity b" + whereClause(statuses, zoneId);
        return read(session -> {
            Query<Long> query = session.createQuery(hql, Long.class);
            bindFilters(query, statuses, zoneId);
            return query.getSingleResult();
        });
    }

    public Map<BinStatus, Long> countByStatus() {
        return read(session -> {
            Map<BinStatus, Long> counts = new EnumMap<>(BinStatus.class);
            session.createQuery("select b.status, count(b) from WasteBinEntity b group by b.status", Object[].class)
                    .getResultList()
                    .forEach(row -> counts.put((BinStatus) row[0], (Long) row[1]));
            return counts;
        });
    }

    public boolean existsByBinCode(String code) {
        return read(session -> session
                .createQuery("select count(b) from WasteBinEntity b where b.binCode = :code", Long.class)
                .setParameter("code", code)
                .getSingleResult() > 0);
    }

    public List<WasteBinEntity> findNeedingCollectionNotRouted() {
        return read(session -> session
                .createQuery("select b from WasteBinEntity b where b.status in :statuses and b.id not in "
                        + "(select rb.id from CollectionRouteEntity r join r.bins rb where r.completed = false)"
                        + FULLEST_FIRST, WasteBinEntity.class)
                .setParameterList("statuses", List.of(BinStatus.NEAR_FULL, BinStatus.OVERFLOW))
                .getResultList());
    }

    private String whereClause(Collection<BinStatus> statuses, Long zoneId) {
        List<String> conditions = new ArrayList<>();
        if (!statuses.isEmpty()) {
            conditions.add("b.status in :statuses");
        }
        if (zoneId != null) {
            conditions.add("b.zone.id = :zoneId");
        }
        return conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
    }

    private void bindFilters(Query<?> query, Collection<BinStatus> statuses, Long zoneId) {
        if (!statuses.isEmpty()) {
            query.setParameterList("statuses", statuses);
        }
        if (zoneId != null) {
            query.setParameter("zoneId", zoneId);
        }
    }


    public int saveAll(List<WasteBinEntity> bins) {
        return write(session -> {
            bins.forEach(session::persist);
            return bins.size();
        });
    }

    public int updateAll(List<WasteBinEntity> bins) {
        return write(session -> {
            bins.forEach(session::merge);
            return bins.size();
        });
    }

    public int collect(Long id, LocalDateTime when) {
        return write(session -> session
                .createMutationQuery("update WasteBinEntity b set b.fillLevelPercent = 0, b.status = :normal, "
                        + "b.lastCollectedAt = :when where b.id = :id")
                .setParameter("normal", BinStatus.NORMAL)
                .setParameter("when", when)
                .setParameter("id", id)
                .executeUpdate());
    }
}