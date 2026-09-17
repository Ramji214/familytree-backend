package com.familytree.familytree.Repository;

import com.familytree.familytree.Model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findByUserId(Long userId);
}