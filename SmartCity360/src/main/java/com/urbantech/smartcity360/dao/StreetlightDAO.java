package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.dto.StreetlightInsertRow;
import com.urbantech.smartcity360.model.StreetlightEntity;
import com.urbantech.smartcity360.model.enums.DeviceStatus;
import org.hibernate.query.Query;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class StreetlightDAO extends BaseDAO<StreetlightEntity> {

    private static final String TABLE_NAME = "streetlights";
    private static final List<String> INSERT_COLUMNS = List.of(
            "spec_id", "zone_id", "latitude", "longitude", "status", "brightness_percent", "installed_date");
    private static final String PAGE_BASE = "select l from StreetlightEntity l join fetch l.spec left join fetch l.zone";

    private volatile String cachedInsertSql;

    public StreetlightDAO() {
        super(StreetlightEntity.class);
    }


    public List<StreetlightEntity> findPage(DeviceStatus status, Long zoneId, int offset, int limit) {
        String hql = PAGE_BASE + whereClause(status, zoneId) + " order by l.id";
        return read(session -> {
            Query<StreetlightEntity> query = session.createQuery(hql, StreetlightEntity.class)
                    .setFirstResult(offset)
                    .setMaxResults(limit);
            bindFilters(query, status, zoneId);
            return query.getResultList();
        });
    }

    public long countFiltered(DeviceStatus status, Long zoneId) {
        String hql = "select count(l) from StreetlightEntity l" + whereClause(status, zoneId);
        return read(session -> {
            Query<Long> query = session.createQuery(hql, Long.class);
            bindFilters(query, status, zoneId);
            return query.getSingleResult();
        });
    }

    public Map<DeviceStatus, Long> countByStatus() {
        return read(session -> {
            Map<DeviceStatus, Long> counts = new EnumMap<>(DeviceStatus.class);
            session.createQuery("select l.status, count(l) from StreetlightEntity l group by l.status", Object[].class)
                    .getResultList()
                    .forEach(row -> counts.put((DeviceStatus) row[0], (Long) row[1]));
            return counts;
        });
    }

    public double sumPowerWatts() {
        return read(session -> session
                .createQuery("select coalesce(sum(s.wattage * l.brightnessPercent / 100.0), 0.0) "
                        + "from StreetlightEntity l join l.spec s where l.status in :lit", Number.class)
                .setParameterList("lit", List.of(DeviceStatus.ON, DeviceStatus.DIM))
                .getSingleResult()
                .doubleValue());
    }

    private String whereClause(DeviceStatus status, Long zoneId) {
        List<String> conditions = new ArrayList<>();
        if (status != null) {
            conditions.add("l.status = :status");
        }
        if (zoneId != null) {
            conditions.add("l.zone.id = :zoneId");
        }
        return conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
    }

    private void bindFilters(Query<?> query, DeviceStatus status, Long zoneId) {
        if (status != null) {
            query.setParameter("status", status);
        }
        if (zoneId != null) {
            query.setParameter("zoneId", zoneId);
        }
    }



    public int insertBatch(List<StreetlightInsertRow> rows) {
        return write(session -> {
            session.doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(insertSql(connection))) {
                    Date today = Date.valueOf(LocalDate.now());
                    for (StreetlightInsertRow row : rows) {
                        statement.setLong(1, row.specId());
                        statement.setLong(2, row.zoneId());
                        statement.setDouble(3, row.latitude());
                        statement.setDouble(4, row.longitude());
                        statement.setString(5, row.status().name());
                        statement.setInt(6, row.brightnessPercent());
                        statement.setDate(7, today);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            });
            return rows.size();
        });
    }

    private String insertSql(Connection connection) throws SQLException {
        if (cachedInsertSql != null) {
            return cachedInsertSql;
        }
        Map<String, String> actualColumns = new HashMap<>();
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, TABLE_NAME, null)) {
            while (columns.next()) {
                String name = columns.getString("COLUMN_NAME");
                actualColumns.put(normalize(name), name);
            }
        }

        List<String> names = new ArrayList<>();
        for (String logicalName : INSERT_COLUMNS) {
            String actual = actualColumns.get(normalize(logicalName));
            if (actual == null) {
                throw new SQLException("Column '" + logicalName + "' was not found in table " + TABLE_NAME);
            }
            names.add("`" + actual + "`");
        }
        cachedInsertSql = "insert into " + TABLE_NAME + " (" + String.join(", ", names)
                + ") values (?, ?, ?, ?, ?, ?, ?)";
        return cachedInsertSql;
    }

    private String normalize(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    public int updateStatus(Long id, DeviceStatus status, int brightness) {
        return write(session -> session
                .createMutationQuery("update StreetlightEntity l set l.status = :status, "
                        + "l.brightnessPercent = :brightness where l.id = :id")
                .setParameter("status", status)
                .setParameter("brightness", brightness)
                .setParameter("id", id)
                .executeUpdate());
    }

    public int updateStatusByZone(Long zoneId, DeviceStatus status, int brightness) {
        return write(session -> session
                .createMutationQuery("update StreetlightEntity l set l.status = :status, "
                        + "l.brightnessPercent = :brightness where l.zone.id = :zoneId and l.status <> :faulty")
                .setParameter("status", status)
                .setParameter("brightness", brightness)
                .setParameter("zoneId", zoneId)
                .setParameter("faulty", DeviceStatus.FAULTY)
                .executeUpdate());
    }
}