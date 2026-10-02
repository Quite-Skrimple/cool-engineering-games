package edu.arizona.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import edu.arizona.identity.model.entity.User;
import edu.arizona.identity.model.enums.UserRole;
import edu.arizona.identity.repository.UserRepository;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.PLAYER);
        return user;
    }

    @Test
    void savesAndFindsUserById() {
        User saved = userRepository.save(newUser("wilbur", "wilbur@arizona.edu"));

        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void findsUserByUsername() {
        userRepository.save(newUser("wilbur", "wilbur@arizona.edu"));

        assertThat(userRepository.findByUsername("wilbur")).isPresent();
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void findsUserByEmail() {
        userRepository.save(newUser("wilbur", "wilbur@arizona.edu"));

        assertThat(userRepository.findByEmail("wilbur@arizona.edu")).isPresent();
        assertThat(userRepository.findByEmail("nobody@arizona.edu")).isEmpty();
    }

    @Test
    void existsByUsernameAndEmailReflectSavedState() {
        userRepository.save(newUser("wilbur", "wilbur@arizona.edu"));

        assertThat(userRepository.existsByUsername("wilbur")).isTrue();
        assertThat(userRepository.existsByEmail("wilbur@arizona.edu")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }
}
