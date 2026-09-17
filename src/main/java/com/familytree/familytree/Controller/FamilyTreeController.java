package com.familytree.familytree.Controller;

import com.familytree.familytree.DTO.TreeNode;
import com.familytree.familytree.Service.FamilyTreeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/familytree")
@CrossOrigin(origins = "http://localhost:5173")
public class FamilyTreeController {

    private final FamilyTreeService familyTreeService;

    public FamilyTreeController(FamilyTreeService familyTreeService) {
        this.familyTreeService = familyTreeService;
    }

    @GetMapping("/{userId}")
    public TreeNode getFamilyTree(
            @PathVariable Long userId) {

        return familyTreeService.getFamilyTree(userId);
    }
}