package com.familytree.familytree.DTO;

import java.util.List;

/**
 * FAMILY SETUP REQUEST
 *
 * WHAT CHANGED
 *
 * PersonRequest now carries its own spouse and its own children.
 *
 * That means a sibling can have:
 *
 *     sibling
 *        |__ spouse
 *        |__ children  (and each child can also have spouse / children)
 *
 * Previously the form collected the sibling's spouse and the sibling's
 * children, but the DTO had nowhere to put them, so the backend
 * silently dropped that data.
 */
public class FamilySetupRequest {

    // ============================================================
    // YOUR DETAILS
    // ============================================================

    private Long selfId;

    private String name;
    private String gender;
    private String photo;


    // ============================================================
    // PARENTS
    // ============================================================

    private PersonRequest father;
    private PersonRequest mother;


    // ============================================================
    // GRANDPARENTS
    // ============================================================

    private PersonRequest grandfather;
    private PersonRequest grandmother;


    // ============================================================
    // GREAT GRANDPARENTS
    // ============================================================

    private PersonRequest greatGrandfather;
    private PersonRequest greatGrandmother;


    // ============================================================
    // GREAT-GREAT GRANDPARENTS
    // ============================================================

    private PersonRequest greatGreatGrandfather;
    private PersonRequest greatGreatGrandmother;


    // ============================================================
    // SPOUSE
    // ============================================================

    private PersonRequest spouse;


    // ============================================================
    // SIBLINGS
    // ============================================================

    private List<PersonRequest> siblings;


    // ============================================================
    // CHILDREN
    // ============================================================

    private List<PersonRequest> children;


    // ============================================================
    // GETTERS / SETTERS
    // ============================================================

    public Long getSelfId() {
        return selfId;
    }

    public void setSelfId(Long selfId) {
        this.selfId = selfId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }


    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }


    public PersonRequest getFather() {
        return father;
    }

    public void setFather(PersonRequest father) {
        this.father = father;
    }


    public PersonRequest getMother() {
        return mother;
    }

    public void setMother(PersonRequest mother) {
        this.mother = mother;
    }


    public PersonRequest getGrandfather() {
        return grandfather;
    }

    public void setGrandfather(PersonRequest grandfather) {
        this.grandfather = grandfather;
    }


    public PersonRequest getGrandmother() {
        return grandmother;
    }

    public void setGrandmother(PersonRequest grandmother) {
        this.grandmother = grandmother;
    }


    public PersonRequest getGreatGrandfather() {
        return greatGrandfather;
    }

    public void setGreatGrandfather(PersonRequest greatGrandfather) {
        this.greatGrandfather = greatGrandfather;
    }


    public PersonRequest getGreatGrandmother() {
        return greatGrandmother;
    }

    public void setGreatGrandmother(PersonRequest greatGrandmother) {
        this.greatGrandmother = greatGrandmother;
    }


    public PersonRequest getGreatGreatGrandfather() {
        return greatGreatGrandfather;
    }

    public void setGreatGreatGrandfather(PersonRequest greatGreatGrandfather) {
        this.greatGreatGrandfather = greatGreatGrandfather;
    }


    public PersonRequest getGreatGreatGrandmother() {
        return greatGreatGrandmother;
    }

    public void setGreatGreatGrandmother(PersonRequest greatGreatGrandmother) {
        this.greatGreatGrandmother = greatGreatGrandmother;
    }


    public PersonRequest getSpouse() {
        return spouse;
    }

    public void setSpouse(PersonRequest spouse) {
        this.spouse = spouse;
    }


    public List<PersonRequest> getSiblings() {
        return siblings;
    }

    public void setSiblings(List<PersonRequest> siblings) {
        this.siblings = siblings;
    }


    public List<PersonRequest> getChildren() {
        return children;
    }

    public void setChildren(List<PersonRequest> children) {
        this.children = children;
    }


    // ============================================================
    // PERSON REQUEST
    // ============================================================

    public static class PersonRequest {

        /*
         * Existing person : id = database ID   -> UPDATE
         * New person      : id = null          -> CREATE
         */
        private Long id;

        private String name;
        private String gender;
        private String photo;

        /*
         * NEW
         *
         * A person can now carry their own spouse and children.
         * Used for siblings and for your own children.
         */
        private PersonRequest spouse;

        private List<PersonRequest> children;


        public PersonRequest() {
        }


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }


        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }


        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }


        public PersonRequest getSpouse() {
            return spouse;
        }

        public void setSpouse(PersonRequest spouse) {
            this.spouse = spouse;
        }


        public List<PersonRequest> getChildren() {
            return children;
        }

        public void setChildren(List<PersonRequest> children) {
            this.children = children;
        }
    }
}