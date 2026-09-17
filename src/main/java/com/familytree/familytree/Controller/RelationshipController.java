package com.familytree.familytree.Controller;

import com.familytree.familytree.Model.Relationship;
import com.familytree.familytree.Service.RelationshipService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relationships")
@CrossOrigin(origins = "http://localhost:5173")
public class RelationshipController {

    private final RelationshipService relationshipService;

    public RelationshipController(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @PostMapping
    public Relationship addRelationship(@RequestBody Relationship relationship) {
        return relationshipService.addRelationship(relationship);
    }

    @GetMapping("/person/{personId}")
    public List<Relationship> getByPerson(@PathVariable Long personId) {
        return relationshipService.getRelationshipsByPerson(personId);
    }

    @GetMapping("/related/{relatedPersonId}")
    public List<Relationship> getByRelatedPerson(
            @PathVariable Long relatedPersonId) {
        return relationshipService
                .getRelationshipsByRelatedPerson(relatedPersonId);
    }

    @DeleteMapping("/{id}")
    public String deleteRelationship(@PathVariable Long id) {
        relationshipService.deleteRelationship(id);
        return "Relationship deleted successfully";
    }
}