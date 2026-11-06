package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.ComplaintEntity;
import com.urbantech.smartcity360.model.ComplaintHistoryEntity;
import com.urbantech.smartcity360.model.enums.EscalationLevel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ComplaintDAO extends BaseDAO<ComplaintEntity> {

    private static final String SELECT_WITH_DETAILS = "select c from ComplaintEntity c join fetch c.citizen "
            + "left join fetch c.zone left join fetch c.assignedTo ";
    private static final String NEWEST_FIRST = " order by c.createdAt desc, c.id desc";

    public ComplaintDAO() {
        super(ComplaintEntity.class);
    }

    public List<ComplaintEntity> findAllWithDetails() {
        return read(session -> session
                .createQuery(SELECT_WITH_DETAILS + NEWEST_FIRST, ComplaintEntity.class)
                .getResultList());
    }

    public List<ComplaintEntity> findByCitizen(Long citizenId) {
        return read(session -> session
                .createQuery(SELECT_WITH_DETAILS + "where c.citizen.id = :citizenId" + NEWEST_FIRST, ComplaintEntity.class)
                .setParameter("citizenId", citizenId)
                .getResultList());
    }

    public List<ComplaintEntity> findByLevels(Collection<EscalationLevel> levels) {
        return read(session -> session
                .createQuery(SELECT_WITH_DETAILS + "where c.currentLevel in :levels" + NEWEST_FIRST, ComplaintEntity.class)
                .setParameterList("levels", levels)
                .getResultList());
    }

    public Optional<ComplaintEntity> findByIdWithDetails(Long id) {
        return read(session -> session
                .createQuery(SELECT_WITH_DETAILS + "where c.id = :id", ComplaintEntity.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    public List<ComplaintHistoryEntity> findHistory(Long complaintId) {
        return read(session -> session
                .createQuery("select h from ComplaintHistoryEntity h left join fetch h.handledBy "
                        + "where h.complaint.id = :id order by h.actionTime, h.id", ComplaintHistoryEntity.class)
                .setParameter("id", complaintId)
                .getResultList());
    }

    public boolean existsByReferenceNo(String referenceNo) {
        return read(session -> session
                .createQuery("select count(c) from ComplaintEntity c where c.referenceNo = :ref", Long.class)
                .setParameter("ref", referenceNo)
                .getSingleResult() > 0);
    }

    public ComplaintEntity saveWithHistory(ComplaintEntity complaint, List<ComplaintHistoryEntity> trail) {
        return write(session -> {
            session.persist(complaint);
            for (ComplaintHistoryEntity entry : trail) {
                entry.setComplaint(complaint);
                session.persist(entry);
            }
            return complaint;
        });
    }

    public ComplaintEntity updateWithHistory(ComplaintEntity complaint, List<ComplaintHistoryEntity> trail) {
        return write(session -> {
            ComplaintEntity managed = session.merge(complaint);
            for (ComplaintHistoryEntity entry : trail) {
                entry.setComplaint(managed);
                session.persist(entry);
            }
            return managed;
        });
    }
}