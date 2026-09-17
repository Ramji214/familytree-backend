package com.familytree.familytree.Controller;

import com.familytree.familytree.Model.FamilyMember;
import com.familytree.familytree.Service.FamilyMemberService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-members")
@CrossOrigin(origins = "http://localhost:5173")
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    public FamilyMemberController(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    @PostMapping
    public FamilyMember addFamilyMember(@RequestBody FamilyMember familyMember) {
        return familyMemberService.addFamilyMember(familyMember);
    }

    @GetMapping("/user/{userId}")
    public List<FamilyMember> getFamilyMembers(@PathVariable Long userId) {
        return familyMemberService.getFamilyMembersByUser(userId);
    }

    @GetMapping("/{id}")
    public FamilyMember getFamilyMember(@PathVariable Long id) {
        return familyMemberService.getFamilyMemberById(id);
    }

    @DeleteMapping("/{id}")
    public String deleteFamilyMember(@PathVariable Long id) {
        familyMemberService.deleteFamilyMember(id);
        return "Family member deleted successfully";
    }
}