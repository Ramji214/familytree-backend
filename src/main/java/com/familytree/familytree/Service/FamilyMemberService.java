package com.familytree.familytree.Service;

import com.familytree.familytree.Model.FamilyMember;
import com.familytree.familytree.Repository.FamilyMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;

    public FamilyMemberService(FamilyMemberRepository familyMemberRepository) {
        this.familyMemberRepository = familyMemberRepository;
    }

    public FamilyMember addFamilyMember(FamilyMember familyMember) {
        return familyMemberRepository.save(familyMember);
    }

    public List<FamilyMember> getFamilyMembersByUser(Long userId) {
        return familyMemberRepository.findByUserId(userId);
    }

    public FamilyMember getFamilyMemberById(Long id) {
        return familyMemberRepository.findById(id).orElse(null);
    }

    public void deleteFamilyMember(Long id) {
        familyMemberRepository.deleteById(id);
    }
}