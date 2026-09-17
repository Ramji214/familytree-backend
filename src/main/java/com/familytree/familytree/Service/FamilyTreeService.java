package com.familytree.familytree.Service;

import com.familytree.familytree.DTO.TreeNode;
import com.familytree.familytree.Model.FamilyMember;
import com.familytree.familytree.Model.Relationship;
import com.familytree.familytree.Repository.FamilyMemberRepository;
import com.familytree.familytree.Repository.RelationshipRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FAMILY TREE SERVICE
 *
 * ============================================================
 * WHAT WAS WRONG
 * ============================================================
 *
 * The old buildTree() passed a COPY of the visited set into every
 * branch:
 *
 *     buildTree(child, ..., new HashSet<>(visited));
 *
 * So the same person could be reached through two different paths
 * (child of father AND child of mother, child AND sibling, ...)
 * and was drawn twice.
 *
 * ============================================================
 * WHAT IS FIXED
 * ============================================================
 *
 * 1. ONE shared "placed" set for the whole tree.
 *    Every person is drawn exactly once, whichever path reaches them
 *    first.
 *
 * 2. A spouse is marked as placed too, so a wife is never drawn again
 *    as somebody's child.
 *
 * 3. Siblings are now FULL nodes: they carry their own spouse and
 *    their own children.
 *
 * 4. The "self" node is flagged so the UI can highlight you.
 */
@Service
public class FamilyTreeService {

    private final FamilyMemberRepository familyMemberRepository;
    private final RelationshipRepository relationshipRepository;

    public FamilyTreeService(
            FamilyMemberRepository familyMemberRepository,
            RelationshipRepository relationshipRepository) {

        this.familyMemberRepository = familyMemberRepository;
        this.relationshipRepository = relationshipRepository;
    }


    // ============================================================
    // GET FAMILY TREE
    // ============================================================

    public TreeNode getFamilyTree(Long userId) {

        List<FamilyMember> members =
                familyMemberRepository.findByUserId(userId);

        if (members == null || members.isEmpty()) {
            return null;
        }


        List<Relationship> relationships =
                relationshipRepository.findByPerson_User_Id(userId);

        if (relationships == null) {
            relationships = new ArrayList<>();
        }


        // --------------------------------------------------------
        // MEMBER MAP
        // --------------------------------------------------------

        Map<Long, FamilyMember> memberMap = new HashMap<>();

        for (FamilyMember member : members) {

            if (member != null && member.getId() != null) {
                memberMap.put(member.getId(), member);
            }
        }


        // --------------------------------------------------------
        // SELF
        //
        // If older data left more than one member flagged as self,
        // the oldest row wins. Never two.
        // --------------------------------------------------------

        FamilyMember self = findSelfMember(members);

        if (self == null) {
            self = members.get(0);
        }


        Long selfId = self.getId();


        // --------------------------------------------------------
        // HIGHEST ANCESTOR
        // --------------------------------------------------------

        FamilyMember root =
                findRoot(self, relationships, memberMap);


        // --------------------------------------------------------
        // BUILD
        //
        // "placed" is shared by the whole tree, so nobody is drawn
        // twice.
        // --------------------------------------------------------

        Set<Long> placed = new HashSet<>();

        return buildTree(
                root,
                relationships,
                memberMap,
                placed,
                selfId
        );
    }


    // ============================================================
    // FIND SELF
    // ============================================================

    private FamilyMember findSelfMember(List<FamilyMember> members) {

        FamilyMember found = null;

        for (FamilyMember member : members) {

            if (member == null || !member.isSelf()) {
                continue;
            }

            if (found == null ||
                    (member.getId() != null &&
                            found.getId() != null &&
                            member.getId() < found.getId())) {

                found = member;
            }
        }

        return found;
    }


    // ============================================================
    // FIND HIGHEST ANCESTOR
    // ============================================================

    private FamilyMember findRoot(
            FamilyMember person,
            List<Relationship> relationships,
            Map<Long, FamilyMember> memberMap) {

        if (person == null) {
            return null;
        }


        Set<Long> visited = new HashSet<>();

        FamilyMember current = person;


        while (current != null &&
                current.getId() != null &&
                !visited.contains(current.getId())) {

            visited.add(current.getId());


            FamilyMember parent =
                    findFatherOrParent(
                            current,
                            relationships,
                            memberMap
                    );


            if (parent == null ||
                    parent.getId() == null ||
                    visited.contains(parent.getId())) {

                break;
            }

            current = parent;
        }

        return current;
    }


    /**
     * Prefers the father, so the tree always climbs the same side and
     * does not jump between the father's and the mother's line.
     */
    private FamilyMember findFatherOrParent(
            FamilyMember person,
            List<Relationship> relationships,
            Map<Long, FamilyMember> memberMap) {

        FamilyMember anyParent = null;

        for (Relationship relationship : relationships) {

            if (relationship == null ||
                    relationship.getPerson() == null ||
                    relationship.getRelatedPerson() == null) {

                continue;
            }

            if (!"PARENT".equalsIgnoreCase(
                    relationship.getRelationshipType())) {

                continue;
            }

            Long childId = relationship.getPerson().getId();
            Long parentId = relationship.getRelatedPerson().getId();

            if (childId == null ||
                    parentId == null ||
                    !childId.equals(person.getId())) {

                continue;
            }

            FamilyMember parent = memberMap.get(parentId);

            if (parent == null) {
                continue;
            }

            if ("Male".equalsIgnoreCase(parent.getGender())) {
                return parent;
            }

            if (anyParent == null) {
                anyParent = parent;
            }
        }

        return anyParent;
    }


    // ============================================================
    // BUILD TREE
    // ============================================================

    private TreeNode buildTree(
            FamilyMember member,
            List<Relationship> relationships,
            Map<Long, FamilyMember> memberMap,
            Set<Long> placed,
            Long selfId) {

        if (member == null ||
                member.getId() == null ||
                placed.contains(member.getId())) {

            return null;
        }


        placed.add(member.getId());


        TreeNode node = new TreeNode(
                member.getId(),
                member.getName(),
                member.getGender(),
                member.getPhoto()
        );

        node.setSelf(
                selfId != null &&
                        selfId.equals(member.getId())
        );


        // --------------------------------------------------------
        // 1. SPOUSE
        //
        // Done first, so a wife can never be picked up later as
        // somebody's child.
        // --------------------------------------------------------

        for (FamilyMember spouse
                : relatedMembers(member, relationships, memberMap, "SPOUSE", true)) {

            if (placed.contains(spouse.getId())) {
                continue;
            }

            placed.add(spouse.getId());

            TreeNode spouseNode = createSimpleNode(spouse);

            spouseNode.setSelf(
                    selfId != null &&
                            selfId.equals(spouse.getId())
            );

            node.getSpouse().add(spouseNode);
        }


        // --------------------------------------------------------
        // 2. SIBLINGS
        //
        // Built as full nodes, so a sibling brings their own spouse
        // and their own children into the tree.
        // --------------------------------------------------------

        for (FamilyMember sibling
                : relatedMembers(member, relationships, memberMap, "SIBLING", true)) {

            TreeNode siblingNode =
                    buildTree(
                            sibling,
                            relationships,
                            memberMap,
                            placed,
                            selfId
                    );

            if (siblingNode != null) {
                node.getSiblings().add(siblingNode);
            }
        }


        // --------------------------------------------------------
        // 3. CHILDREN
        //
        // PARENT : person = child, relatedPerson = parent
        // CHILD  : person = parent, relatedPerson = child
        // --------------------------------------------------------

        for (FamilyMember child
                : childrenOf(member, relationships, memberMap)) {

            TreeNode childNode =
                    buildTree(
                            child,
                            relationships,
                            memberMap,
                            placed,
                            selfId
                    );

            if (childNode != null) {
                node.getChildren().add(childNode);
            }
        }


        return node;
    }


    // ============================================================
    // RELATED MEMBERS OF A GIVEN TYPE
    // ============================================================

    private List<FamilyMember> relatedMembers(
            FamilyMember member,
            List<Relationship> relationships,
            Map<Long, FamilyMember> memberMap,
            String type,
            boolean bothDirections) {

        List<FamilyMember> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (Relationship relationship : relationships) {

            if (relationship == null ||
                    relationship.getPerson() == null ||
                    relationship.getRelatedPerson() == null ||
                    relationship.getRelationshipType() == null) {

                continue;
            }

            if (!type.equalsIgnoreCase(
                    relationship.getRelationshipType())) {

                continue;
            }

            Long personId = relationship.getPerson().getId();
            Long relatedId = relationship.getRelatedPerson().getId();

            if (personId == null || relatedId == null) {
                continue;
            }

            Long otherId = null;

            if (personId.equals(member.getId())) {
                otherId = relatedId;
            } else if (bothDirections && relatedId.equals(member.getId())) {
                otherId = personId;
            }

            if (otherId == null ||
                    otherId.equals(member.getId())) {

                continue;
            }

            FamilyMember other = memberMap.get(otherId);

            if (other != null && seen.add(otherId)) {
                result.add(other);
            }
        }

        return result;
    }


    private List<FamilyMember> childrenOf(
            FamilyMember member,
            List<Relationship> relationships,
            Map<Long, FamilyMember> memberMap) {

        List<FamilyMember> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (Relationship relationship : relationships) {

            if (relationship == null ||
                    relationship.getPerson() == null ||
                    relationship.getRelatedPerson() == null ||
                    relationship.getRelationshipType() == null) {

                continue;
            }

            Long personId = relationship.getPerson().getId();
            Long relatedId = relationship.getRelatedPerson().getId();
            String type = relationship.getRelationshipType();

            if (personId == null || relatedId == null) {
                continue;
            }

            Long childId = null;

            if ("PARENT".equalsIgnoreCase(type) &&
                    relatedId.equals(member.getId())) {

                childId = personId;

            } else if ("CHILD".equalsIgnoreCase(type) &&
                    personId.equals(member.getId())) {

                childId = relatedId;
            }

            if (childId == null ||
                    childId.equals(member.getId())) {

                continue;
            }

            FamilyMember child = memberMap.get(childId);

            if (child != null && seen.add(childId)) {
                result.add(child);
            }
        }

        return result;
    }


    private TreeNode createSimpleNode(FamilyMember member) {

        if (member == null) {
            return null;
        }

        return new TreeNode(
                member.getId(),
                member.getName(),
                member.getGender(),
                member.getPhoto()
        );
    }
}