package com.familytree.familytree.Service;

import com.familytree.familytree.DTO.FamilySetupRequest;
import com.familytree.familytree.Model.FamilyMember;
import com.familytree.familytree.Model.Relationship;
import com.familytree.familytree.Model.User;
import com.familytree.familytree.Repository.FamilyMemberRepository;
import com.familytree.familytree.Repository.RelationshipRepository;
import com.familytree.familytree.Repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FAMILY SETUP SERVICE
 *
 * ============================================================
 * WHY YOUR NAME APPEARED TWICE
 * ============================================================
 *
 * The old saveFamily() always CREATED brand new members.
 *
 * So whenever the form was submitted a second time in "create" mode
 * (for example: save once, then log in again before profileCompleted
 * was refreshed, or submit the form twice), a SECOND "self" member,
 * a second father, a second mother and so on were inserted.
 *
 * The tree then legitimately contained two people called "ramji".
 *
 * ============================================================
 * THE FIX
 * ============================================================
 *
 * 1. saveFamily() and updateFamily() now both call ONE method:
 *    saveOrUpdateFamily(). It is idempotent - saving the same form
 *    ten times produces exactly the same single family.
 *
 * 2. Exactly one member can be self = true.
 *
 * 3. A person with no id is matched against an existing member with
 *    the same name + gender before a new row is created.
 *
 * 4. removeDuplicateMembers() cleans up the duplicates ALREADY in your
 *    database. It runs on every save, so your existing double "ramji"
 *    disappears the next time you press Save / Update.
 *
 * 5. Siblings are now real family members:
 *      - linked to your father and mother  (so they sit in YOUR row)
 *      - their spouse is saved
 *      - their children are saved (recursively)
 */
@Service
public class FamilySetupService {

    private final FamilyMemberRepository familyMemberRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    public FamilySetupService(
            FamilyMemberRepository familyMemberRepository,
            RelationshipRepository relationshipRepository,
            UserRepository userRepository) {

        this.familyMemberRepository = familyMemberRepository;
        this.relationshipRepository = relationshipRepository;
        this.userRepository = userRepository;
    }


    // ============================================================
    // PUBLIC ENTRY POINTS
    // ============================================================

    /**
     * First time save. Safe to call more than once.
     */
    @Transactional
    public void saveFamily(Long userId, FamilySetupRequest request) {
        saveOrUpdateFamily(userId, request);
    }


    /**
     * Edit family. Same logic as save.
     */
    @Transactional
    public void updateFamily(Long userId, FamilySetupRequest request) {
        saveOrUpdateFamily(userId, request);
    }


    /**
     * Delete everything for this user and start again.
     *
     * Useful once, if you want a completely clean tree.
     */
    @Transactional
    public void resetFamily(Long userId) {

        User user = getUser(userId);

        List<FamilyMember> members =
                familyMemberRepository.findByUserId(userId);

        if (members != null) {

            for (FamilyMember member : members) {

                if (member == null || member.getId() == null) {
                    continue;
                }

                deleteRelationshipsOf(member.getId());
            }

            familyMemberRepository.deleteAll(members);
        }

        user.setProfileCompleted(false);

        userRepository.save(user);
    }


    // ============================================================
    // SAVE OR UPDATE  (single source of truth)
    // ============================================================

    private void saveOrUpdateFamily(
            Long userId,
            FamilySetupRequest request) {

        User user = getUser(userId);

        validateRequest(request);


        // --------------------------------------------------------
        // STEP 1 : clean duplicates created by older versions
        // --------------------------------------------------------

        removeDuplicateMembers(userId);
        removeDuplicateRelationships(userId);


        // --------------------------------------------------------
        // STEP 2 : load current members
        // --------------------------------------------------------

        List<FamilyMember> existingMembers =
                new ArrayList<>();

        List<FamilyMember> loaded =
                familyMemberRepository.findByUserId(userId);

        if (loaded != null) {
            existingMembers.addAll(loaded);
        }


        Set<Long> usedMemberIds = new HashSet<>();


        // --------------------------------------------------------
        // STEP 3 : SELF  (never duplicated)
        // --------------------------------------------------------

        FamilyMember self =
                findSelfMember(
                        existingMembers,
                        request.getSelfId()
                );


        if (self == null) {

            self = createMember(
                    request.getName(),
                    request.getGender(),
                    request.getPhoto(),
                    user,
                    true,
                    existingMembers
            );

        } else {

            updateMember(
                    self,
                    request.getName(),
                    request.getGender(),
                    request.getPhoto()
            );
        }


        // Only ONE member may be flagged as self.

        for (FamilyMember member : existingMembers) {

            if (member == null || member.getId() == null) {
                continue;
            }

            if (member.isSelf() &&
                    !member.getId().equals(self.getId())) {

                member.setSelf(false);

                familyMemberRepository.save(member);
            }
        }


        if (self.getId() != null) {
            usedMemberIds.add(self.getId());
        }


        // --------------------------------------------------------
        // STEP 4 : ANCESTORS
        // --------------------------------------------------------

        FamilyMember father =
                updateOrCreate(request.getFather(), user, existingMembers, usedMemberIds);

        FamilyMember mother =
                updateOrCreate(request.getMother(), user, existingMembers, usedMemberIds);

        FamilyMember grandfather =
                updateOrCreate(request.getGrandfather(), user, existingMembers, usedMemberIds);

        FamilyMember grandmother =
                updateOrCreate(request.getGrandmother(), user, existingMembers, usedMemberIds);

        FamilyMember greatGrandfather =
                updateOrCreate(request.getGreatGrandfather(), user, existingMembers, usedMemberIds);

        FamilyMember greatGrandmother =
                updateOrCreate(request.getGreatGrandmother(), user, existingMembers, usedMemberIds);

        FamilyMember greatGreatGrandfather =
                updateOrCreate(request.getGreatGreatGrandfather(), user, existingMembers, usedMemberIds);

        FamilyMember greatGreatGrandmother =
                updateOrCreate(request.getGreatGreatGrandmother(), user, existingMembers, usedMemberIds);

        FamilyMember spouse =
                updateOrCreate(request.getSpouse(), user, existingMembers, usedMemberIds);


        // --------------------------------------------------------
        // STEP 5 : RELATIONSHIPS FOR THE MAIN LINE
        // --------------------------------------------------------

        addParentRelationship(self, father);
        addParentRelationship(self, mother);

        addSpouseRelationship(father, mother);

        addParentRelationship(father, grandfather);
        addParentRelationship(father, grandmother);

        addSpouseRelationship(grandfather, grandmother);

        addParentRelationship(grandfather, greatGrandfather);
        addParentRelationship(grandfather, greatGrandmother);

        addSpouseRelationship(greatGrandfather, greatGrandmother);

        addParentRelationship(greatGrandfather, greatGreatGrandfather);
        addParentRelationship(greatGrandfather, greatGreatGrandmother);

        addSpouseRelationship(greatGreatGrandfather, greatGreatGrandmother);

        addSpouseRelationship(self, spouse);


        // --------------------------------------------------------
        // STEP 6 : SIBLINGS
        //
        // A sibling is:
        //
        //   - your brother / sister          -> SIBLING both ways
        //   - a child of your parents        -> PARENT (so the tree
        //                                       puts them in your row)
        //   - allowed a spouse
        //   - allowed children
        // --------------------------------------------------------

        if (request.getSiblings() != null) {

            for (FamilySetupRequest.PersonRequest siblingRequest
                    : request.getSiblings()) {

                FamilyMember sibling =
                        updateOrCreate(
                                siblingRequest,
                                user,
                                existingMembers,
                                usedMemberIds
                        );

                if (sibling == null) {
                    continue;
                }


                // Never let you become your own sibling.

                if (sameId(sibling, self)) {
                    continue;
                }


                addSiblingRelationship(self, sibling);


                // Same parents as you.

                addParentRelationship(sibling, father);
                addParentRelationship(sibling, mother);


                // Sibling's spouse.

                FamilyMember siblingSpouse =
                        updateOrCreate(
                                siblingRequest.getSpouse(),
                                user,
                                existingMembers,
                                usedMemberIds
                        );

                addSpouseRelationship(sibling, siblingSpouse);


                // Sibling's children (and their children, recursively).

                saveDescendants(
                        sibling,
                        siblingRequest.getChildren(),
                        user,
                        existingMembers,
                        usedMemberIds
                );
            }
        }


        // --------------------------------------------------------
        // STEP 7 : YOUR CHILDREN
        // --------------------------------------------------------

        saveDescendants(
                self,
                request.getChildren(),
                user,
                existingMembers,
                usedMemberIds
        );


        // --------------------------------------------------------
        // STEP 8 : PROFILE COMPLETED
        // --------------------------------------------------------

        user.setProfileCompleted(true);

        userRepository.save(user);
    }


    // ============================================================
    // SAVE CHILDREN RECURSIVELY
    // ============================================================

    private void saveDescendants(
            FamilyMember parent,
            List<FamilySetupRequest.PersonRequest> childRequests,
            User user,
            List<FamilyMember> existingMembers,
            Set<Long> usedMemberIds) {

        if (parent == null || childRequests == null) {
            return;
        }


        for (FamilySetupRequest.PersonRequest childRequest : childRequests) {

            FamilyMember child =
                    updateOrCreate(
                            childRequest,
                            user,
                            existingMembers,
                            usedMemberIds
                    );

            if (child == null || sameId(child, parent)) {
                continue;
            }


            addChildRelationship(parent, child);


            // The child's spouse.

            FamilyMember childSpouse =
                    updateOrCreate(
                            childRequest.getSpouse(),
                            user,
                            existingMembers,
                            usedMemberIds
                    );

            addSpouseRelationship(child, childSpouse);


            // Grandchildren.

            saveDescendants(
                    child,
                    childRequest.getChildren(),
                    user,
                    existingMembers,
                    usedMemberIds
            );
        }
    }


    // ============================================================
    // GET FAMILY FOR EDIT
    // ============================================================

    @Transactional(readOnly = true)
    public FamilySetupRequest getFamilyForEdit(Long userId) {

        getUser(userId);

        List<FamilyMember> members =
                familyMemberRepository.findByUserId(userId);

        if (members == null || members.isEmpty()) {
            throw new RuntimeException("Family details not found");
        }


        FamilyMember self = null;

        for (FamilyMember member : members) {

            if (member != null && member.isSelf()) {

                if (self == null ||
                        (member.getId() != null &&
                                self.getId() != null &&
                                member.getId() < self.getId())) {

                    self = member;
                }
            }
        }

        if (self == null) {
            throw new RuntimeException("Your family profile was not found");
        }


        List<Relationship> relationships =
                relationshipRepository.findByPerson_User_Id(userId);

        if (relationships == null) {
            relationships = new ArrayList<>();
        }


        FamilySetupRequest response = new FamilySetupRequest();

        response.setSelfId(self.getId());
        response.setName(self.getName());
        response.setGender(self.getGender());
        response.setPhoto(self.getPhoto());


        // --------------------------------------------------------
        // FATHER / MOTHER
        // --------------------------------------------------------

        FamilyMember father = null;
        FamilyMember mother = null;

        for (FamilyMember parent : parentsOf(self, relationships)) {

            if (father == null && isMale(parent)) {
                father = parent;
            } else if (mother == null && isFemale(parent)) {
                mother = parent;
            }
        }

        if (father != null) {
            response.setFather(toPersonRequest(father));
        }

        if (mother != null) {
            response.setMother(toPersonRequest(mother));
        }


        // --------------------------------------------------------
        // SPOUSE
        // --------------------------------------------------------

        FamilyMember selfSpouse =
                spouseOf(self, relationships);

        if (selfSpouse != null) {
            response.setSpouse(toPersonRequest(selfSpouse));
        }


        // --------------------------------------------------------
        // SIBLINGS  (with spouse + children)
        // --------------------------------------------------------

        List<FamilySetupRequest.PersonRequest> siblings =
                new ArrayList<>();

        Set<Long> siblingIds = new HashSet<>();

        for (Relationship relationship : relationships) {

            if (!isRelationship(relationship, "SIBLING")) {
                continue;
            }

            FamilyMember sibling = null;

            if (sameId(relationship.getPerson(), self)) {
                sibling = relationship.getRelatedPerson();
            } else if (sameId(relationship.getRelatedPerson(), self)) {
                sibling = relationship.getPerson();
            }

            if (sibling == null ||
                    sibling.getId() == null ||
                    sameId(sibling, self)) {

                continue;
            }

            if (siblingIds.add(sibling.getId())) {

                FamilySetupRequest.PersonRequest siblingRequest =
                        toPersonRequest(sibling);

                FamilyMember siblingSpouse =
                        spouseOf(sibling, relationships);

                if (siblingSpouse != null) {
                    siblingRequest.setSpouse(
                            toPersonRequest(siblingSpouse)
                    );
                }

                siblingRequest.setChildren(
                        childrenRequestsOf(
                                sibling,
                                relationships,
                                new HashSet<>()
                        )
                );

                siblings.add(siblingRequest);
            }
        }

        response.setSiblings(siblings);


        // --------------------------------------------------------
        // CHILDREN  (with spouse + grandchildren)
        // --------------------------------------------------------

        response.setChildren(
                childrenRequestsOf(
                        self,
                        relationships,
                        new HashSet<>()
                )
        );


        // --------------------------------------------------------
        // GRANDPARENTS
        // --------------------------------------------------------

        FamilyMember grandfather = null;
        FamilyMember grandmother = null;

        FamilyMember parentForGrand =
                father != null ? father : mother;

        if (parentForGrand != null) {

            for (FamilyMember gp : parentsOf(parentForGrand, relationships)) {

                if (grandfather == null && isMale(gp)) {
                    grandfather = gp;
                } else if (grandmother == null && isFemale(gp)) {
                    grandmother = gp;
                }
            }
        }

        if (grandfather != null) {
            response.setGrandfather(toPersonRequest(grandfather));
        }

        if (grandmother != null) {
            response.setGrandmother(toPersonRequest(grandmother));
        }


        // --------------------------------------------------------
        // GREAT GRANDPARENTS
        // --------------------------------------------------------

        FamilyMember greatGrandfather = null;
        FamilyMember greatGrandmother = null;

        FamilyMember grandForGreat =
                grandfather != null ? grandfather : grandmother;

        if (grandForGreat != null) {

            for (FamilyMember great : parentsOf(grandForGreat, relationships)) {

                if (greatGrandfather == null && isMale(great)) {
                    greatGrandfather = great;
                } else if (greatGrandmother == null && isFemale(great)) {
                    greatGrandmother = great;
                }
            }
        }

        if (greatGrandfather != null) {
            response.setGreatGrandfather(toPersonRequest(greatGrandfather));
        }

        if (greatGrandmother != null) {
            response.setGreatGrandmother(toPersonRequest(greatGrandmother));
        }


        // --------------------------------------------------------
        // GREAT GREAT GRANDPARENTS
        // --------------------------------------------------------

        FamilyMember greatGreatGrandfather = null;
        FamilyMember greatGreatGrandmother = null;

        FamilyMember greatForGreatGreat =
                greatGrandfather != null ? greatGrandfather : greatGrandmother;

        if (greatForGreatGreat != null) {

            for (FamilyMember greatGreat
                    : parentsOf(greatForGreatGreat, relationships)) {

                if (greatGreatGrandfather == null && isMale(greatGreat)) {
                    greatGreatGrandfather = greatGreat;
                } else if (greatGreatGrandmother == null && isFemale(greatGreat)) {
                    greatGreatGrandmother = greatGreat;
                }
            }
        }

        if (greatGreatGrandfather != null) {
            response.setGreatGreatGrandfather(
                    toPersonRequest(greatGreatGrandfather)
            );
        }

        if (greatGreatGrandmother != null) {
            response.setGreatGreatGrandmother(
                    toPersonRequest(greatGreatGrandmother)
            );
        }


        return response;
    }


    // ============================================================
    // READ HELPERS
    // ============================================================

    private List<FamilyMember> parentsOf(
            FamilyMember person,
            List<Relationship> relationships) {

        List<FamilyMember> parents = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        if (person == null) {
            return parents;
        }

        for (Relationship relationship : relationships) {

            if (!isRelationship(relationship, "PARENT")) {
                continue;
            }

            if (!sameId(relationship.getPerson(), person)) {
                continue;
            }

            FamilyMember parent = relationship.getRelatedPerson();

            if (parent != null &&
                    parent.getId() != null &&
                    seen.add(parent.getId())) {

                parents.add(parent);
            }
        }

        // Also support CHILD stored the other way round.

        for (Relationship relationship : relationships) {

            if (!isRelationship(relationship, "CHILD")) {
                continue;
            }

            if (!sameId(relationship.getRelatedPerson(), person)) {
                continue;
            }

            FamilyMember parent = relationship.getPerson();

            if (parent != null &&
                    parent.getId() != null &&
                    seen.add(parent.getId())) {

                parents.add(parent);
            }
        }

        return parents;
    }


    private FamilyMember spouseOf(
            FamilyMember person,
            List<Relationship> relationships) {

        if (person == null) {
            return null;
        }

        for (Relationship relationship : relationships) {

            if (!isRelationship(relationship, "SPOUSE")) {
                continue;
            }

            if (sameId(relationship.getPerson(), person)) {
                return relationship.getRelatedPerson();
            }

            if (sameId(relationship.getRelatedPerson(), person)) {
                return relationship.getPerson();
            }
        }

        return null;
    }


    private List<FamilySetupRequest.PersonRequest> childrenRequestsOf(
            FamilyMember parent,
            List<Relationship> relationships,
            Set<Long> visited) {

        List<FamilySetupRequest.PersonRequest> result =
                new ArrayList<>();

        if (parent == null ||
                parent.getId() == null ||
                !visited.add(parent.getId())) {

            return result;
        }


        Set<Long> childIds = new HashSet<>();

        List<FamilyMember> children = new ArrayList<>();

        for (Relationship relationship : relationships) {

            FamilyMember child = null;

            if (isRelationship(relationship, "CHILD") &&
                    sameId(relationship.getPerson(), parent)) {

                child = relationship.getRelatedPerson();

            } else if (isRelationship(relationship, "PARENT") &&
                    sameId(relationship.getRelatedPerson(), parent)) {

                child = relationship.getPerson();
            }

            if (child == null || child.getId() == null) {
                continue;
            }

            /*
             * Your siblings are also children of your parents.
             * They must NOT come back as "children" of the person
             * we are reading, otherwise the form would show them twice.
             *
             * We only exclude them at the top level, which is why
             * the caller passes a fresh visited set.
             */

            if (childIds.add(child.getId())) {
                children.add(child);
            }
        }


        for (FamilyMember child : children) {

            FamilySetupRequest.PersonRequest childRequest =
                    toPersonRequest(child);

            FamilyMember childSpouse =
                    spouseOf(child, relationships);

            if (childSpouse != null) {
                childRequest.setSpouse(toPersonRequest(childSpouse));
            }

            childRequest.setChildren(
                    childrenRequestsOf(
                            child,
                            relationships,
                            visited
                    )
            );

            result.add(childRequest);
        }

        return result;
    }


    private boolean isMale(FamilyMember member) {
        return member != null &&
                "Male".equalsIgnoreCase(member.getGender());
    }


    private boolean isFemale(FamilyMember member) {
        return member != null &&
                "Female".equalsIgnoreCase(member.getGender());
    }


    private FamilySetupRequest.PersonRequest toPersonRequest(
            FamilyMember member) {

        if (member == null) {
            return null;
        }

        FamilySetupRequest.PersonRequest request =
                new FamilySetupRequest.PersonRequest();

        request.setId(member.getId());
        request.setName(member.getName());
        request.setGender(member.getGender());
        request.setPhoto(member.getPhoto());

        return request;
    }


    // ============================================================
    // DUPLICATE CLEAN UP
    // ============================================================

    /**
     * Merges members of the same user that have the same name + gender.
     *
     * The oldest row (lowest id) wins. Every relationship pointing at a
     * duplicate is moved to the surviving member, then the duplicate row
     * is deleted.
     *
     * This is what removes the second "ramji" already in your database.
     */
    private void removeDuplicateMembers(Long userId) {

        List<FamilyMember> members =
                familyMemberRepository.findByUserId(userId);

        if (members == null || members.size() < 2) {
            return;
        }


        Map<String, FamilyMember> keepers =
                new LinkedHashMap<>();

        List<FamilyMember> duplicates =
                new ArrayList<>();


        for (FamilyMember member : members) {

            if (member == null ||
                    member.getId() == null ||
                    member.getName() == null ||
                    member.getName().trim().isEmpty()) {

                continue;
            }


            String key =
                    member.getName().trim().toLowerCase()
                            + "|"
                            + (member.getGender() == null
                            ? ""
                            : member.getGender().trim().toLowerCase());


            FamilyMember keeper = keepers.get(key);


            if (keeper == null) {

                keepers.put(key, member);
                continue;
            }


            // Decide which row survives: the lower id.

            FamilyMember survivor = keeper;
            FamilyMember duplicate = member;

            if (member.getId() < keeper.getId()) {
                survivor = member;
                duplicate = keeper;
                keepers.put(key, survivor);
            }


            // Carry over anything useful before deleting.

            if (duplicate.isSelf()) {
                survivor.setSelf(true);
            }

            if ((survivor.getPhoto() == null ||
                    survivor.getPhoto().trim().isEmpty()) &&
                    duplicate.getPhoto() != null) {

                survivor.setPhoto(duplicate.getPhoto());
            }

            familyMemberRepository.save(survivor);

            repointRelationships(duplicate, survivor);

            duplicates.add(duplicate);
        }


        if (!duplicates.isEmpty()) {

            familyMemberRepository.deleteAll(duplicates);

            removeDuplicateRelationships(userId);
        }
    }


    private void repointRelationships(
            FamilyMember duplicate,
            FamilyMember survivor) {

        if (duplicate == null ||
                survivor == null ||
                duplicate.getId() == null) {

            return;
        }


        List<Relationship> asPerson =
                relationshipRepository.findByPersonId(duplicate.getId());

        if (asPerson != null) {

            for (Relationship relationship : asPerson) {

                if (relationship == null) {
                    continue;
                }

                relationship.setPerson(survivor);

                relationshipRepository.save(relationship);
            }
        }


        List<Relationship> asRelated =
                relationshipRepository.findByRelatedPersonId(duplicate.getId());

        if (asRelated != null) {

            for (Relationship relationship : asRelated) {

                if (relationship == null) {
                    continue;
                }

                relationship.setRelatedPerson(survivor);

                relationshipRepository.save(relationship);
            }
        }
    }


    /**
     * Deletes repeated rows and self-pointing rows such as
     * "ramji SPOUSE ramji", which can appear after a merge.
     */
    private void removeDuplicateRelationships(Long userId) {

        List<Relationship> relationships =
                relationshipRepository.findByPerson_User_Id(userId);

        if (relationships == null || relationships.isEmpty()) {
            return;
        }


        Set<String> seen = new HashSet<>();

        List<Relationship> toDelete = new ArrayList<>();


        for (Relationship relationship : relationships) {

            if (relationship == null ||
                    relationship.getPerson() == null ||
                    relationship.getRelatedPerson() == null ||
                    relationship.getPerson().getId() == null ||
                    relationship.getRelatedPerson().getId() == null ||
                    relationship.getRelationshipType() == null) {

                if (relationship != null) {
                    toDelete.add(relationship);
                }

                continue;
            }


            Long personId = relationship.getPerson().getId();
            Long relatedId = relationship.getRelatedPerson().getId();


            // Nobody is their own parent / spouse / sibling.

            if (personId.equals(relatedId)) {
                toDelete.add(relationship);
                continue;
            }


            String key =
                    personId + "|" + relatedId + "|"
                            + relationship.getRelationshipType()
                            .trim().toUpperCase();


            if (!seen.add(key)) {
                toDelete.add(relationship);
            }
        }


        if (!toDelete.isEmpty()) {
            relationshipRepository.deleteAll(toDelete);
        }
    }


    private void deleteRelationshipsOf(Long memberId) {

        List<Relationship> asPerson =
                relationshipRepository.findByPersonId(memberId);

        if (asPerson != null && !asPerson.isEmpty()) {
            relationshipRepository.deleteAll(asPerson);
        }

        List<Relationship> asRelated =
                relationshipRepository.findByRelatedPersonId(memberId);

        if (asRelated != null && !asRelated.isEmpty()) {
            relationshipRepository.deleteAll(asRelated);
        }
    }


    // ============================================================
    // FIND SELF
    // ============================================================

    private FamilyMember findSelfMember(
            List<FamilyMember> members,
            Long selfId) {

        if (members == null || members.isEmpty()) {
            return null;
        }


        if (selfId != null) {

            FamilyMember byId = findMemberById(members, selfId);

            if (byId != null) {
                byId.setSelf(true);
                return byId;
            }
        }


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


    private FamilyMember findMemberById(
            List<FamilyMember> members,
            Long id) {

        if (id == null || members == null) {
            return null;
        }

        for (FamilyMember member : members) {

            if (member != null &&
                    member.getId() != null &&
                    member.getId().equals(id)) {

                return member;
            }
        }

        return null;
    }


    /**
     * Matches a person that has no id against someone already stored.
     *
     * Without this, pressing Save twice would create
     * "manavalan", "manavalan", "manavalan"...
     */
    private FamilyMember findMemberByName(
            List<FamilyMember> members,
            String name,
            String gender,
            Set<Long> usedMemberIds) {

        if (members == null ||
                name == null ||
                name.trim().isEmpty()) {

            return null;
        }


        String wantedName = name.trim().toLowerCase();

        String wantedGender =
                gender == null ? "" : gender.trim().toLowerCase();


        for (FamilyMember member : members) {

            if (member == null ||
                    member.getId() == null ||
                    member.getName() == null) {

                continue;
            }


            if (usedMemberIds.contains(member.getId())) {
                continue;
            }


            String memberGender =
                    member.getGender() == null
                            ? ""
                            : member.getGender().trim().toLowerCase();


            boolean nameMatches =
                    member.getName().trim().toLowerCase()
                            .equals(wantedName);

            boolean genderMatches =
                    wantedGender.isEmpty() ||
                            memberGender.isEmpty() ||
                            memberGender.equals(wantedGender);


            if (nameMatches && genderMatches) {
                return member;
            }
        }

        return null;
    }


    // ============================================================
    // UPDATE OR CREATE
    // ============================================================

    private FamilyMember updateOrCreate(
            FamilySetupRequest.PersonRequest request,
            User user,
            List<FamilyMember> existingMembers,
            Set<Long> usedMemberIds) {

        if (request == null ||
                request.getName() == null ||
                request.getName().trim().isEmpty()) {

            return null;
        }


        // --------------------------------------------------------
        // EXISTING MEMBER BY ID
        // --------------------------------------------------------

        if (request.getId() != null) {

            FamilyMember existingMember =
                    findMemberById(existingMembers, request.getId());

            if (existingMember != null) {

                if (existingMember.getUser() == null ||
                        existingMember.getUser().getId() == null ||
                        !existingMember.getUser().getId().equals(user.getId())) {

                    throw new RuntimeException("Invalid family member");
                }

                updateMember(
                        existingMember,
                        request.getName(),
                        request.getGender(),
                        request.getPhoto()
                );

                usedMemberIds.add(existingMember.getId());

                return existingMember;
            }
        }


        // --------------------------------------------------------
        // EXISTING MEMBER BY NAME  (stops duplicates)
        // --------------------------------------------------------

        FamilyMember byName =
                findMemberByName(
                        existingMembers,
                        request.getName(),
                        request.getGender(),
                        usedMemberIds
                );

        if (byName != null) {

            updateMember(
                    byName,
                    request.getName(),
                    request.getGender(),
                    request.getPhoto()
            );

            usedMemberIds.add(byName.getId());

            return byName;
        }


        // --------------------------------------------------------
        // NEW MEMBER
        // --------------------------------------------------------

        FamilyMember newMember =
                createMember(
                        request.getName(),
                        request.getGender(),
                        request.getPhoto(),
                        user,
                        false,
                        existingMembers
                );

        if (newMember.getId() != null) {
            usedMemberIds.add(newMember.getId());
        }

        return newMember;
    }


    private void updateMember(
            FamilyMember member,
            String name,
            String gender,
            String photo) {

        if (member == null) {
            throw new RuntimeException("Family member not found");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name is required");
        }

        member.setName(name.trim());

        if (gender != null && !gender.trim().isEmpty()) {
            member.setGender(gender);
        }

        // Keep the old photo when no new photo was uploaded.

        if (photo != null && !photo.trim().isEmpty()) {
            member.setPhoto(photo);
        }

        familyMemberRepository.save(member);
    }


    private FamilyMember createMember(
            String name,
            String gender,
            String photo,
            User user,
            boolean self,
            List<FamilyMember> existingMembers) {

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Name is required");
        }

        if (user == null) {
            throw new RuntimeException("User is required");
        }


        FamilyMember member = new FamilyMember();

        member.setName(name.trim());
        member.setGender(gender);
        member.setPhoto(photo);
        member.setUser(user);
        member.setSelf(self);


        FamilyMember saved =
                familyMemberRepository.save(member);


        // Keep the in-memory list in sync so later lookups find it.

        if (existingMembers != null) {
            existingMembers.add(saved);
        }

        return saved;
    }


    // ============================================================
    // USER / VALIDATION
    // ============================================================

    private User getUser(Long userId) {

        if (userId == null) {
            throw new RuntimeException("User ID is required");
        }

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );
    }


    private void validateRequest(FamilySetupRequest request) {

        if (request == null) {
            throw new RuntimeException("Family details are required");
        }

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new RuntimeException("Your name is required");
        }
    }


    // ============================================================
    // RELATIONSHIP HELPERS
    // ============================================================

    private void addParentRelationship(
            FamilyMember child,
            FamilyMember parent) {

        if (child == null || parent == null || sameId(child, parent)) {
            return;
        }

        addRelationship(child, parent, "PARENT");
    }


    private void addSpouseRelationship(
            FamilyMember person,
            FamilyMember spouse) {

        if (person == null || spouse == null || sameId(person, spouse)) {
            return;
        }

        addRelationship(person, spouse, "SPOUSE");
        addRelationship(spouse, person, "SPOUSE");
    }


    private void addSiblingRelationship(
            FamilyMember person,
            FamilyMember sibling) {

        if (person == null || sibling == null || sameId(person, sibling)) {
            return;
        }

        addRelationship(person, sibling, "SIBLING");
        addRelationship(sibling, person, "SIBLING");
    }


    private void addChildRelationship(
            FamilyMember parent,
            FamilyMember child) {

        if (parent == null || child == null || sameId(parent, child)) {
            return;
        }

        addRelationship(parent, child, "CHILD");
    }


    private void addRelationship(
            FamilyMember person,
            FamilyMember relatedPerson,
            String type) {

        if (person == null ||
                relatedPerson == null ||
                person.getId() == null ||
                relatedPerson.getId() == null ||
                type == null ||
                person.getId().equals(relatedPerson.getId())) {

            return;
        }


        // --------------------------------------------------------
        // PREVENT DUPLICATE ROWS
        // --------------------------------------------------------

        List<Relationship> existing =
                relationshipRepository.findByPersonId(person.getId());

        if (existing != null) {

            for (Relationship relationship : existing) {

                if (relationship == null ||
                        relationship.getRelatedPerson() == null ||
                        relationship.getRelatedPerson().getId() == null ||
                        relationship.getRelationshipType() == null) {

                    continue;
                }

                boolean samePerson =
                        relationship.getRelatedPerson().getId()
                                .equals(relatedPerson.getId());

                boolean sameType =
                        type.equalsIgnoreCase(
                                relationship.getRelationshipType()
                        );

                if (samePerson && sameType) {
                    return;
                }
            }
        }


        Relationship relationship = new Relationship();

        relationship.setPerson(person);
        relationship.setRelatedPerson(relatedPerson);
        relationship.setRelationshipType(type);

        relationshipRepository.save(relationship);
    }


    private boolean isRelationship(
            Relationship relationship,
            String type) {

        if (relationship == null || type == null) {
            return false;
        }

        if (relationship.getPerson() == null ||
                relationship.getRelatedPerson() == null ||
                relationship.getRelationshipType() == null) {

            return false;
        }

        return type.equalsIgnoreCase(
                relationship.getRelationshipType()
        );
    }


    private boolean sameId(FamilyMember first, FamilyMember second) {

        if (first == null || second == null) {
            return false;
        }

        if (first.getId() == null || second.getId() == null) {
            return false;
        }

        return first.getId().equals(second.getId());
    }
}