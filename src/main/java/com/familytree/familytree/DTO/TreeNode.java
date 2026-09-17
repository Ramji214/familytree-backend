package com.familytree.familytree.DTO;

import java.util.ArrayList;
import java.util.List;

/**
 * TREE NODE
 *
 * WHAT CHANGED
 *
 * 1. "self" flag added, so the frontend can mark the logged-in person.
 *
 * 2. Nodes inside "siblings" are now FULL nodes.
 *    A sibling therefore carries their own spouse and their own children.
 */
public class TreeNode {

    private Long id;
    private String name;
    private String gender;
    private String photo;

    /*
     * true only for the logged-in person's own card.
     */
    private boolean self;

    private List<TreeNode> children = new ArrayList<>();
    private List<TreeNode> siblings = new ArrayList<>();
    private List<TreeNode> spouse = new ArrayList<>();


    public TreeNode() {
    }


    public TreeNode(Long id, String name, String gender, String photo) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.photo = photo;
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


    public boolean isSelf() {
        return self;
    }

    public void setSelf(boolean self) {
        this.self = self;
    }


    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }


    public List<TreeNode> getSiblings() {
        return siblings;
    }

    public void setSiblings(List<TreeNode> siblings) {
        this.siblings = siblings;
    }


    public List<TreeNode> getSpouse() {
        return spouse;
    }

    public void setSpouse(List<TreeNode> spouse) {
        this.spouse = spouse;
    }
}