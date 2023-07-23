package com.design.hub.resource.impl

import com.design.hub.configuration.SecurityProperties
import com.design.hub.domain.user.UserDomain
import com.design.hub.exception.ExceptionDetails
import com.design.hub.exception.ResourceNotFoundException
import com.design.hub.payload.user.converter.UserConverter
import com.design.hub.payload.user.request.UserCreateRequest
import com.design.hub.payload.user.response.UserCreateResponse
import com.design.hub.resource.UserResource
import com.design.hub.resource.UserResource.Companion.LOGGER
import com.design.hub.service.UserService
import com.design.hub.utils.I18n
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Optional
import java.util.UUID

@Validated
@RestController
@RequestMapping("users")
@Tag(name = "Users", description = "Resources for managing users")
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ExceptionDetails::class)
                )
            ]
        )
    ]
)
class UserResourceImpl(
    private val userService: UserService,
    private val userConverter: UserConverter,
    private val securityProperties: SecurityProperties
) : UserResource {

    @PostMapping
    override fun create(
        @RequestBody @Valid
        entity: UserCreateRequest
    ): ResponseEntity<UserCreateResponse> {
        LOGGER.info("TOKEN: ${this.securityProperties.tokenExpiration}")
        LOGGER.info("[create] Creating user")

        val userDomain = this.userConverter.toCreateDomain(entity)
        val createdEntity: UserDomain = this.userService.create(userDomain)
        LOGGER.info("[create] User created successfully: ${createdEntity.id}")

        return ResponseEntity(this.userConverter.toResponse(createdEntity), HttpStatus.CREATED)
    }

    @GetMapping("/{id}")
    override fun findById(@PathVariable("id") id: UUID): ResponseEntity<UserCreateResponse> {
        LOGGER.info("[findById] Fetching entity by ID: $id")

        val optUser: Optional<UserDomain> = this.userService.findById(id)

        return if (optUser.isEmpty) {
            LOGGER.info("[findById] Entity not found for ID: $id")
            throw ResourceNotFoundException(I18n.HTTP_4XX_404_NOT_FOUND)
        } else {
            LOGGER.info("[findById] Entity found: ${optUser.get().id}")
            ResponseEntity(this.userConverter.toResponse(optUser.get()), HttpStatus.OK)
        }
    }

    @GetMapping
    @Operation(summary = "Get all users")
    override fun findAll(@ParameterObject pageable: Pageable): ResponseEntity<Page<UserCreateResponse>> {
        LOGGER.info("[findAll] Fetching all entities")

        val entities = this.userService.findAll(pageable).map { this.userConverter.toResponse(it) }
        LOGGER.info("[findAll] Total entities found: ${entities.size}")

        return ResponseEntity(entities, HttpStatus.OK)
    }

    @PutMapping("/{id}")
    override fun update(
        @PathVariable("id") id: UUID,
        @RequestBody entity: UserCreateRequest
    ): ResponseEntity<UserCreateResponse> {
        LOGGER.info("[update] Updating entity with ID: $id")
        val userDomain: UserDomain = this.userService.update(id, this.userConverter.toCreateDomain(entity))

        return run {
            LOGGER.info("[update] Entity updated successfully: ${userDomain.id}")
            ResponseEntity(this.userConverter.toResponse(userDomain), HttpStatus.OK)
        }
    }

    @DeleteMapping("/{id}")
    override fun delete(@PathVariable("id") id: UUID): ResponseEntity<Unit> {
        LOGGER.info("[delete] Deleting entity with ID: $id")

        val isDeleted = this.userService.delete(id)
        return if (isDeleted) {
            LOGGER.info("[delete] Entity deleted successfully with ID: $id")
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            LOGGER.info("[delete] Entity not found for ID: $id")
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}