package com.gitlab.metrics.service;

import com.gitlab.metrics.entity.User;
import com.gitlab.metrics.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService单元测试
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Set<User.Role> testRoles;

    @Before
    public void setUp() {
        testUser = createMockUser();
        testRoles = new HashSet<>(Arrays.asList(User.Role.USER));
    }

    @Test
    public void testCreateUser_Success() {
        // Given
        String username = "testuser";
        String password = "password123";
        String email = "test@example.com";
        String fullName = "Test User";
        String encodedPassword = "encoded_password";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(username, password, email, fullName, testRoles);

        // Then
        assertNotNull(result);
        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertEquals(username, savedUser.getUsername());
        assertEquals(encodedPassword, savedUser.getPassword());
        assertEquals(email, savedUser.getEmail());
        assertEquals(fullName, savedUser.getFullName());
        assertEquals(testRoles, savedUser.getRoles());
        assertTrue(savedUser.isEnabled());
        assertTrue(savedUser.isAccountNonExpired());
        assertTrue(savedUser.isAccountNonLocked());
        assertTrue(savedUser.isCredentialsNonExpired());
    }

    @Test
    public void testCreateUser_UsernameExists() {
        // Given
        String username = "existinguser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When & Then
        try {
            userService.createUser(username, "password", "email@test.com", "Full Name", testRoles);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("用户名已存在: " + username, e.getMessage());
        }

        verify(userRepository).existsByUsername(username);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testCreateUser_EmailExists() {
        // Given
        String username = "newuser";
        String email = "existing@example.com";
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When & Then
        try {
            userService.createUser(username, "password", email, "Full Name", testRoles);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("邮箱已存在: " + email, e.getMessage());
        }

        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testCreateUser_NullEmail() {
        // Given
        String username = "testuser";
        String password = "password123";
        String email = null;
        String fullName = "Test User";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(username, password, email, fullName, testRoles);

        // Then
        assertNotNull(result);
        verify(userRepository).existsByUsername(username);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testFindByUsername() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByUsername(username);
    }

    @Test
    public void testFindByUsername_NotFound() {
        // Given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername(username);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByUsername(username);
    }

    @Test
    public void testFindByEmail() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail(email);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByEmail(email);
    }

    @Test
    public void testGetAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser, createMockUser());
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    public void testGetEnabledUsers() {
        // Given
        List<User> enabledUsers = Arrays.asList(testUser);
        when(userRepository.findByEnabledTrue()).thenReturn(enabledUsers);

        // When
        List<User> result = userService.getEnabledUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findByEnabledTrue();
    }

    @Test
    public void testGetUsersByRole() {
        // Given
        User.Role role = User.Role.ADMIN;
        List<User> adminUsers = Arrays.asList(testUser);
        when(userRepository.findByRole(role)).thenReturn(adminUsers);

        // When
        List<User> result = userService.getUsersByRole(role);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findByRole(role);
    }

    @Test
    public void testUpdateLastLogin() {
        // Given
        Long userId = 1L;
        LocalDateTime loginTime = LocalDateTime.now();
        String loginIp = "192.168.1.1";

        // When
        userService.updateLastLogin(userId, loginTime, loginIp);

        // Then
        verify(userRepository).updateLastLogin(userId, loginTime, loginIp);
    }

    @Test
    public void testEnableUser() {
        // Given
        Long userId = 1L;
        testUser.setEnabled(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.enableUser(userId);

        // Then
        assertTrue(testUser.isEnabled());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    public void testEnableUser_UserNotFound() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        userService.enableUser(userId);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testDisableUser() {
        // Given
        Long userId = 1L;
        testUser.setEnabled(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.disableUser(userId);

        // Then
        assertFalse(testUser.isEnabled());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    public void testLockUser() {
        // Given
        Long userId = 1L;
        testUser.setAccountNonLocked(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.lockUser(userId);

        // Then
        assertFalse(testUser.isAccountNonLocked());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    public void testUnlockUser() {
        // Given
        Long userId = 1L;
        testUser.setAccountNonLocked(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.unlockUser(userId);

        // Then
        assertTrue(testUser.isAccountNonLocked());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    public void testUpdatePassword() {
        // Given
        Long userId = 1L;
        String newPassword = "newpassword123";
        String encodedPassword = "encoded_new_password";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.updatePassword(userId, newPassword);

        // Then
        assertEquals(encodedPassword, testUser.getPassword());
        verify(userRepository).findById(userId);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    public void testUpdateUserRoles() {
        // Given
        Long userId = 1L;
        Set<User.Role> newRoles = new HashSet<>(Arrays.asList(User.Role.ADMIN, User.Role.USER));
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.updateUserRoles(userId, newRoles);

        // Then
        assertEquals(newRoles, testUser.getRoles());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    public void testDeleteUser() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }

    @Test
    public void testDeleteUser_UserNotFound() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    public void testExistsByUsername() {
        // Given
        String username = "testuser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When
        boolean result = userService.existsByUsername(username);

        // Then
        assertTrue(result);
        verify(userRepository).existsByUsername(username);
    }

    @Test
    public void testExistsByEmail() {
        // Given
        String email = "test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // When
        boolean result = userService.existsByEmail(email);

        // Then
        assertFalse(result);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    public void testGetRecentlyLoggedInUsers() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<User> recentUsers = Arrays.asList(testUser);
        when(userRepository.findRecentlyLoggedInUsers(since)).thenReturn(recentUsers);

        // When
        List<User> result = userService.getRecentlyLoggedInUsers(since);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findRecentlyLoggedInUsers(since);
    }

    @Test
    public void testGetInactiveUsers() {
        // Given
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<User> inactiveUsers = Arrays.asList(testUser);
        when(userRepository.findInactiveUsers(threshold)).thenReturn(inactiveUsers);

        // When
        List<User> result = userService.getInactiveUsers(threshold);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findInactiveUsers(threshold);
    }

    @Test
    public void testCountUsersByRole() {
        // Given
        List<Object[]> roleStats = Arrays.asList(
            new Object[]{User.Role.ADMIN, 2L},
            new Object[]{User.Role.USER, 10L}
        );
        when(userRepository.countUsersByRole()).thenReturn(roleStats);

        // When
        List<Object[]> result = userService.countUsersByRole();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).countUsersByRole();
    }

    // Helper methods

    private User createMockUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRoles(new HashSet<>(Arrays.asList(User.Role.USER)));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}