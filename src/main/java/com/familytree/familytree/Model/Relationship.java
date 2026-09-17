package com.familytree.familytree.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "relationships")
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private FamilyMember person;

    @ManyToOne
    @JoinColumn(name = "related_person_id")
    private FamilyMember relatedPerson;

    private String relationshipType;

    public Relationship() {
    }

    public Long getId() {
        return id;
    }

    public FamilyMember getPerson() {
        return person;
    }

    public void setPerson(FamilyMember person) {
        this.person = person;
    }

    public FamilyMember getRelatedPerson() {
        return relatedPerson;
    }

    public void setRelatedPerson(FamilyMember relatedPerson) {
        this.relatedPerson = relatedPerson;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }
}