import com.mengnankk.entity.User;
import com.mengnankk.entity.Role;
import com.mengnankk.exception.AuthException;
import com.mengnankk.mapper.UserMapper;
import com.mengnankk.mapper.RoleMapper;
import com.mengnankk.service.BloomFilterService;
import com.mengnankk.service.Impl.AliyunSmsService;
import com.mengnankk.service.Impl.UserServiceImpl;
import com.mengnankk.untils.JwtTokenUntils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceExtraTests {
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenUntils jwtUtils;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private AliyunSmsService aliyunSmsService;
    @Mock
    private BloomFilterService userBloomFilterService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(userService, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    void testRegisterUserExistsByPhoneNumber() {
        String username = "newuser";
        String phoneNumber = "13812345678";
        String password = "password123";
        String smsCode = "123456";

        when(userMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.existsByPhoneNumber(phoneNumber)).thenReturn(true);
        doNothing().when(aliyunSmsService).verifyRegisterSmsCode(phoneNumber, smsCode);

        AuthException thrown = assertThrows(AuthException.class, () -> {
            userService.register(username, phoneNumber, password, smsCode);
        });

        assertEquals("手机号已注册", thrown.getMessage());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void testRegisterUserSmsCodeInvalid() {
        String username = "testuser";
        String phoneNumber = "13812345678";
        String password = "password123";
        String smsCode = "wrongcode";

        when(userMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        doThrow(new AuthException("验证码错误")).when(aliyunSmsService).verifyRegisterSmsCode(phoneNumber, smsCode);

        AuthException thrown = assertThrows(AuthException.class, () -> {
            userService.register(username, phoneNumber, password, smsCode);
        });

        assertEquals("验证码错误", thrown.getMessage());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void testLoginPasswordIncorrect() {
        String identifier = "testuser";
        String password = "wrongpassword";
        User user = new User();
        user.setId(1L);
        user.setUsername(identifier);
        user.setPhoneNumber("13812345678");
        user.setPassword("hashedPassword");
        user.setStatus("ACTIVE");

        when(userMapper.findByUsername(identifier)).thenReturn(user);
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(false);

        AuthException thrown = assertThrows(AuthException.class, () -> {
            userService.login(identifier, password);
        });

        assertEquals("用户名/手机号或密码不正确", thrown.getMessage());
        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyList(), anyList());
    }

    @Test
    void testLoginUserDisabled() {
        String identifier = "testuser";
        String password = "password123";
        User user = new User();
        user.setId(1L);
        user.setUsername(identifier);
        user.setPhoneNumber("13812345678");
        user.setPassword("hashedPassword");
        user.setStatus("DISABLED");

        when(userMapper.findByUsername(identifier)).thenReturn(user);
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

        AuthException thrown = assertThrows(AuthException.class, () -> {
            userService.login(identifier, password);
        });

        assertEquals("用户已被禁用", thrown.getMessage());
        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyList(), anyList());
    }

    @Test
    void testLogoutNoRefreshToken() {
        Long userId = 1L;
        when(valueOperations.get(eq("refresh_token:" + userId))).thenReturn(null);

        assertDoesNotThrow(() -> userService.logout(userId));
        verify(redisTemplate, never()).delete(anyString());
        verify(valueOperations, never()).set(startsWith("blacklist:at:"), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void testLogoutInvalidRefreshToken() {
        Long userId = 1L;
        String mockRefreshToken = "mockRefreshToken";

        when(valueOperations.get(eq("refresh_token:" + userId))).thenReturn(mockRefreshToken);
        when(jwtUtils.getClaimsFromToken(mockRefreshToken)).thenReturn(null);

        assertDoesNotThrow(() -> userService.logout(userId));
        verify(redisTemplate).delete(eq("refresh_token:" + userId));
        verify(valueOperations, never()).set(startsWith("blacklist:at:"), anyString(), anyLong(), any(TimeUnit.class));
    }
} 