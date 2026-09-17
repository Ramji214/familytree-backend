package com.familytree.familytree.Controller;

import com.familytree.familytree.DTO.FamilySetupRequest;
import com.familytree.familytree.Service.FamilySetupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/familytree")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class FamilySetupController {

    private final FamilySetupService familySetupService;


    public FamilySetupController(
            FamilySetupService familySetupService) {

        this.familySetupService = familySetupService;
    }


    // ============================================================
    // SAVE FAMILY
    //
    // Safe to call more than once. If the family already exists it
    // is updated instead of being created a second time.
    // ============================================================

    @PostMapping("/setup/{userId}")
    public ResponseEntity<String> saveFamily(
            @PathVariable Long userId,
            @RequestBody FamilySetupRequest request) {

        try {

            familySetupService.saveFamily(userId, request);

            return ResponseEntity.ok(
                    "Family details saved successfully"
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }


    // ============================================================
    // GET EXISTING FAMILY - EDIT FORM
    // ============================================================

    @GetMapping("/edit/{userId}")
    public ResponseEntity<?> getFamilyForEdit(
            @PathVariable Long userId) {

        try {

            FamilySetupRequest family =
                    familySetupService.getFamilyForEdit(userId);

            return ResponseEntity.ok(family);

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }


    // ============================================================
    // UPDATE EXISTING FAMILY
    // ============================================================

    @PutMapping("/edit/{userId}")
    public ResponseEntity<String> updateFamily(
            @PathVariable Long userId,
            @RequestBody FamilySetupRequest request) {

        try {

            familySetupService.updateFamily(userId, request);

            return ResponseEntity.ok(
                    "Family details updated successfully"
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }


    // ============================================================
    // RESET FAMILY
    //
    // Deletes every member and relationship for this user.
    // Use it once if you would rather start from an empty tree than
    // let the duplicate cleanup merge the old rows.
    // ============================================================

    @DeleteMapping("/reset/{userId}")
    public ResponseEntity<String> resetFamily(
            @PathVariable Long userId) {

        try {

            familySetupService.resetFamily(userId);

            return ResponseEntity.ok(
                    "Family details cleared"
            );

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}