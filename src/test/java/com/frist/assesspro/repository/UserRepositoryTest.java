package com.frist.assesspro.repository;

import com.frist.assesspro.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User admin;
    private User creator;
    private User tester;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        admin = new User();
        admin.setUsername("admin");
        admin.setPassword("pass");
        admin.setRole(User.Roles.ADMIN);
        admin.setFirstName("Admin");
        admin.setLastName("Adminov");
        admin.setIsActive(true);
        admin.setIsProfileComplete(true);
        admin.setCreatedAt(LocalDateTime.now());

        creator = new User();
        creator.setUsername("creator");
        creator.setPassword("pass");
        creator.setRole(User.Roles.CREATOR);
        creator.setFirstName("Creator");
        creator.setLastName("Creatorsky");
        creator.setIsActive(true);
        creator.setIsProfileComplete(false);
        creator.setCreatedAt(LocalDateTime.now());

        tester = new User();
        tester.setUsername("tester");
        tester.setPassword("pass");
        tester.setRole(User.Roles.TESTER);
        tester.setFirstName("Tester");
        tester.setLastName("Testovich");
        tester.setIsActive(false);
        tester.setIsProfileComplete(true);
        tester.setCreatedAt(LocalDateTime.now());

        userRepository.saveAll(List.of(admin, creator, tester));
    }

    @Test
    void findByUsername_ShouldReturnUser() {
        Optional<User> found = userRepository.findByUsername("creator");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("creator");
    }

    @Test
    void existsByUsername_ShouldReturnTrueForExisting() {
        assertThat(userRepository.existsByUsername("admin")).isTrue();
        assertThat(userRepository.existsByUsername("none")).isFalse();
    }

    @Test
    void countByRole_ShouldReturnCorrectCount() {
        assertThat(userRepository.countByRole(User.Roles.ADMIN)).isEqualTo(1L);
        assertThat(userRepository.countByRole(User.Roles.CREATOR)).isEqualTo(1L);
        assertThat(userRepository.countByRole(User.Roles.TESTER)).isEqualTo(1L);
    }

    @Test
    void findByRole_ShouldReturnListOfUsersWithRole() {
        List<User> admins = userRepository.findByRole(User.Roles.ADMIN);
        assertThat(admins).hasSize(1).extracting(User::getUsername).containsExactly("admin");
    }

    @Test
    void findByProfileNotComplete_ShouldReturnUsersWithFalseIsProfileComplete() {
        List<User> incomplete = userRepository.findByProfileNotComplete();
        assertThat(incomplete).hasSize(1).extracting(User::getUsername).containsExactly("creator");
    }

    @Test
    void findByCreatedAtAfter_ShouldReturnUsersCreatedAfterGivenDate() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        List<User> recent = userRepository.findByCreatedAtAfter(before);
        assertThat(recent).hasSize(3);
    }

    @Test
    void countAllUsers_ShouldReturnTotalCount() {
        assertThat(userRepository.countAllUsers()).isEqualTo(3L);
    }

    @Test
    void countByIsActive_ShouldReturnCorrectCounts() {
        assertThat(userRepository.countByIsActive(true)).isEqualTo(2L);
        assertThat(userRepository.countByIsActive(false)).isEqualTo(1L);
    }

    @Test
    void findUsersWithFilters_ShouldFilterByRole() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = userRepository.findUsersWithFilters(User.Roles.CREATOR, null, null, pageable);
        assertThat(page.getContent()).hasSize(1).extracting(User::getUsername).containsExactly("creator");
    }

    @Test
    void findUsersWithFilters_ShouldFilterByActive() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = userRepository.findUsersWithFilters(null, true, null, pageable);
        assertThat(page.getContent()).hasSize(2).extracting(User::getUsername).containsExactlyInAnyOrder("admin", "creator");
    }

    @Test
    void findUsersWithFilters_ShouldSearchByUsername() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = userRepository.findUsersWithFilters(null, null, "admin", pageable);
        assertThat(page.getContent()).hasSize(1).extracting(User::getUsername).containsExactly("admin");
    }

    @Test
    void searchTesters_ShouldReturnMatchingTesters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = userRepository.searchTesters("test", pageable);
        assertThat(page.getContent()).hasSize(1).extracting(User::getUsername).containsExactly("tester");
    }

    @Test
    void findAllCreators_ShouldReturnAllCreators() {
        List<User> creators = userRepository.findAllCreators();
        assertThat(creators).hasSize(1).extracting(User::getUsername).containsExactly("creator");
    }
}