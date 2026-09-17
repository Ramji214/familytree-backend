package com.familytree.familytree.Repository;

import com.familytree.familytree.Model.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelationshipRepository
        extends JpaRepository<Relationship, Long> {

    List<Relationship> findByPersonId(Long personId);

    List<Relationship> findByRelatedPersonId(Long relatedPersonId);

    List<Relationship> findByPerson_User_Id(Long userId);
}