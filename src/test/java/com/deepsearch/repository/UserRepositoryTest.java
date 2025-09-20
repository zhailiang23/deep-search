package com.deepsearch.repository;

import com.deepsearch.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository 单元测试
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$hashed_password");
        testUser.setRole(User.Role.USER);
    }

    @Test
    void testSaveUser() {
        // Given
        // testUser is already set up in @BeforeEach

        // When
        User savedUser = userRepository.save(testUser);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getRole()).isEqualTo(User.Role.USER);
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindByUsername() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testFindByUsernameNotFound() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void testFindByEmail() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testFindByEmailNotFound() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> foundUser = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void testExistsByUsername() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Boolean exists = userRepository.existsByUsername("testuser");
        Boolean notExists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testExistsByEmail() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Boolean exists = userRepository.existsByEmail("test@example.com");
        Boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void testDeleteUser() {
        // Given
        User persistedUser = entityManager.persistAndFlush(testUser);
        Long userId = persistedUser.getId();

        // When
        userRepository.deleteById(userId);
        entityManager.flush();

        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void testUpdateUser() {
        // Given
        User persistedUser = entityManager.persistAndFlush(testUser);

        // When
        persistedUser.setEmail("updated@example.com");
        persistedUser.setRole(User.Role.ADMIN);
        User updatedUser = userRepository.save(persistedUser);
        entityManager.flush();

        // Then
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(updatedUser.getUpdatedAt()).isAfter(updatedUser.getCreatedAt());
    }

    @Test
    void testFindAll() {
        // Given
        User user2 = new User();
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");
        user2.setPasswordHash("$2a$10$hashed_password2");
        user2.setRole(User.Role.ADMIN);

        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(user2);

        // When
        Iterable<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(2);
    }

    @Test
    void testUniqueConstraints() {
        // Given
        User duplicateUsernameUser = new User();
        duplicateUsernameUser.setUsername("testuser"); // Same username
        duplicateUsernameUser.setEmail("different@example.com");
        duplicateUsernameUser.setPasswordHash("$2a$10$hashed_password");

        User duplicateEmailUser = new User();
        duplicateEmailUser.setUsername("differentuser");
        duplicateEmailUser.setEmail("test@example.com"); // Same email
        duplicateEmailUser.setPasswordHash("$2a$10$hashed_password");

        entityManager.persistAndFlush(testUser);

        // When & Then - Username uniqueness
        try {
            entityManager.persistAndFlush(duplicateUsernameUser);
            assertThat(false).as("Expected DataIntegrityViolationException for duplicate username").isTrue();
        } catch (Exception e) {
            // Expected exception for duplicate username
            assertThat(e.getMessage().toLowerCase()).contains("constraint");
        }

        // When & Then - Email uniqueness
        try {
            entityManager.persistAndFlush(duplicateEmailUser);
            assertThat(false).as("Expected DataIntegrityViolationException for duplicate email").isTrue();
        } catch (Exception e) {
            // Expected exception for duplicate email
            assertThat(e.getMessage().toLowerCase()).contains("constraint");
        }
    }
}