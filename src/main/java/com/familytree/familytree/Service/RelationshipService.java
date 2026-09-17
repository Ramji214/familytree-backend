package com.familytree.familytree.Service;

import com.familytree.familytree.Model.Relationship;
import com.familytree.familytree.Repository.RelationshipRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RelationshipService {

    private final RelationshipRepository relationshipRepository;

    public RelationshipService(RelationshipRepository relationshipRepository) {
        this.relationshipRepository = relationshipRepository;
    }

    public Relationship addRelationship(Relationship relationship) {
        return relationshipRepository.save(relationship);
    }

    public List<Relationship> getRelationshipsByPerson(Long personId) {
        return relationshipRepository.findByPersonId(personId);
    }

    public List<Relationship> getRelationshipsByRelatedPerson(Long relatedPersonId) {
        return relationshipRepository.findByRelatedPersonId(relatedPersonId);
    }

    public void deleteRelationship(Long id) {
        relationshipRepository.deleteById(id);
    }
}