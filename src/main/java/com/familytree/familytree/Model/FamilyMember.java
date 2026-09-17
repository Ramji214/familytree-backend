package com.familytree.familytree.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "family_members")
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String gender;

    /*
     * Photos are stored as Base64 strings.
     * Base64 images can be much larger than a normal VARCHAR column.
     *
     * @Lob + LONGTEXT allows large images to be stored.
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photo;

    @Column(nullable = false)
    private boolean self = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public FamilyMember() {
    }


    // ============================================================
    // GET ID
    // ============================================================

    public Long getId() {
        return id;
    }


    // ============================================================
    // NAME
    // ============================================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // ============================================================
    // GENDER
    // ============================================================

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }


    // ============================================================
    // PHOTO
    // ============================================================

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }


    // ============================================================
    // SELF
    // ============================================================

    public boolean isSelf() {
        return self;
    }

    public void setSelf(boolean self) {
        this.self = self;
    }


    // ============================================================
    // USER
    // ============================================================

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}