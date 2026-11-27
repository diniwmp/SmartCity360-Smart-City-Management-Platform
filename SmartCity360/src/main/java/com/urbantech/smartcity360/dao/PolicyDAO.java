package com.urbantech.smartcity360.dao;

import com.urbantech.smartcity360.model.PolicyEntity;
import com.urbantech.smartcity360.model.PolicyVersionEntity;

import java.util.List;
import java.util.Optional;

public class PolicyDAO extends BaseDAO<PolicyEntity> {

    public PolicyDAO() {
        super(PolicyEntity.class);
    }

    public List<PolicyEntity> findAllWithCreator() {
        return read(session -> session
                .createQuery("select p from PolicyEntity p left join fetch p.createdBy order by p.id", PolicyEntity.class)
                .getResultList());
    }

    public List<PolicyEntity> findActive() {
        return read(session -> session
                .createQuery("from PolicyEntity p where p.active = true order by p.id", PolicyEntity.class)
                .getResultList());
    }

    public Optional<PolicyEntity> findByIdWithCreator(Long id) {
        return read(session -> session
                .createQuery("select p from PolicyEntity p left join fetch p.createdBy where p.id = :id", PolicyEntity.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    public PolicyEntity saveWithVersion(PolicyEntity policy, PolicyVersionEntity version) {
        return write(session -> {
            session.persist(policy);
            version.setPolicy(policy);
            session.persist(version);
            return policy;
        });
    }

    public PolicyEntity updateWithVersion(PolicyEntity policy, PolicyVersionEntity version) {
        return write(session -> {
            PolicyEntity managed = session.merge(policy);
            if (version != null) {
                version.setPolicy(managed);
                session.persist(version);
            }
            return managed;
        });
    }

    public int updateActive(Long id, boolean active) {
        return write(session -> session
                .createMutationQuery("update PolicyEntity p set p.active = :active where p.id = :id")
                .setParameter("active", active)
                .setParameter("id", id)
                .executeUpdate());
    }
}