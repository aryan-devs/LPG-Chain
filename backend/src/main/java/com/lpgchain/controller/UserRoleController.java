package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.entity.UserRole;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UserRoleController — REST controller for managing user-role mappings.
 */
@RestController
@RequestMapping("/api/user-roles")
@Tag(name = "User Roles", description = "User-to-role assignment and mapping management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @GetMapping
    @Operation(summary = "Get All User Roles", description = "Retrieves all user-role assignments. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of user-role mappings",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserRole.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserRole>> getAllUserRoles() {
        return ResponseEntity.ok(userRoleService.getAllUserRoles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User Role by ID", description = "Retrieves a user-role mapping by its ID. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User-role mapping found",
                    content = @Content(schema = @Schema(implementation = UserRole.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User-role mapping not found")
    })
    public ResponseEntity<UserRole> getUserRoleById(
            @Parameter(description = "UserRole mapping ID", example = "1") @PathVariable Long id) {
        return userRoleService.getUserRoleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Assign Role to User", description = "Creates a new user-role assignment. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User-role mapping created",
                    content = @Content(schema = @Schema(implementation = UserRole.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserRole> createUserRole(@RequestBody UserRole userRole) {
        UserRole savedUserRole = userRoleService.saveUserRole(userRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUserRole);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete User Role Assignment", description = "Deletes a user-role mapping by ID. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User-role mapping deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUserRole(
            @Parameter(description = "UserRole mapping ID", example = "1") @PathVariable Long id) {
        userRoleService.deleteUserRole(id);
        return ResponseEntity.noContent().build();
    }
}
