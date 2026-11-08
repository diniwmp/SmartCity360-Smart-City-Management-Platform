package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.CollectionRouteEntity;
import com.urbantech.smartcity360.model.WasteBinEntity;
import com.urbantech.smartcity360.model.enums.BinStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CollectionRouteDAO extends BaseDAO<CollectionRouteEntity> {

    public CollectionRouteDAO() {
        super(CollectionRouteEntity.class);
    }

    public List<CollectionRouteEntity> findAllWithDetails() {
        return read(session -> session
                .createQuery("select distinct r from CollectionRouteEntity r left join fetch r.assignedStaff "
                        + "left join fetch r.bins order by r.id desc", CollectionRouteEntity.class)
                .getResultList());
    }


    public Optional<Integer> completeRoute(Long routeId, LocalDateTime when) {
        return write(session -> {
            CollectionRouteEntity route = session
                    .createQuery("select r from CollectionRouteEntity r left join fetch r.bins where r.id = :id",
                            CollectionRouteEntity.class)
                    .setParameter("id", routeId)
                    .uniqueResult();
            if (route == null || route.isCompleted()) {
                return Optional.<Integer>empty();
            }
            for (WasteBinEntity bin : route.getBins()) {
                bin.setFillLevelPercent(0);
                bin.setStatus(BinStatus.NORMAL);
                bin.setLastCollectedAt(when);
            }
            route.setCompleted(true);
            return Optional.of(route.getBins().size());
        });
    }
}