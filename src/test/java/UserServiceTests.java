import cn.hutool.jwt.Claims;
import com.mengnankk.dto.TokenResponse;
import com.mengnankk.entity.Role;
import com.mengnankk.entity.User;
import com.mengnankk.exception.AuthException;
import com.mengnankk.mapper.RoleMapper;
import com.mengnankk.mapper.UserMapper;
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

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

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
        ReflectionTestUtils.setField(userService, "refreshTokenExpirationMs", 604800000L); // 7天
    }

    @Test
    void testRegisterUserSuccess() {
        String username = "testuser";
        String phoneNumber = "13812345678";
        String password = "password123";
        String smsCode = "123456";

        when(userMapper.existsByUsername(username)).thenReturn(false);
        when(userMapper.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("hashedPassword");
        when(roleMapper.findByName("USER")).thenReturn(new Role() {{
            setId(1L);
            setRoleName("USER");
        }});
        doNothing().when(userMapper).insertUser(any(User.class));
        doNothing().when(userMapper).insertUserRole(anyLong(), anyLong());
        doNothing().when(aliyunSmsService).verifyRegisterSmsCode(phoneNumber, smsCode);
        // 这里模拟 BloomFilterService 的 put 方法，传入用户ID（Long），返回 true 表示成功放入
        doNothing().when(userBloomFilterService).put(anyLong());

        User registeredUser = userService.register(username, phoneNumber, password, smsCode);

        assertNotNull(registeredUser);
        assertEquals(username, registeredUser.getUsername());
        verify(aliyunSmsService).verifyRegisterSmsCode(phoneNumber, smsCode);
        verify(userMapper).insertUser(any(User.class));
        verify(userMapper).insertUserRole(anyLong(), eq(1L));
        verify(userBloomFilterService).put(anyLong());
    }

    @Test
    void testRegisterUserExistsByUsername() {
        String username = "existinguser";
        String phoneNumber = "13812345678";
        String password = "password123";
        String smsCode = "123456";

        when(userMapper.existsByUsername(username)).thenReturn(true);
        doNothing().when(aliyunSmsService).verifyRegisterSmsCode(phoneNumber, smsCode);

        AuthException thrown = assertThrows(AuthException.class, () -> {
            userService.register(username, phoneNumber, password, smsCode);
        });

        assertEquals("用户名已存在", thrown.getMessage());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        String identifier = "testuser";
        String password = "password123";
        User user = new User();
        user.setId(1L);
        user.setUsername(identifier);
        user.setPhoneNumber("13812345678");
        user.setPassword("hashedPassword");
        user.setStatus("ACTIVE");

        when(userMapper.findByUsername(identifier)).thenReturn(user);
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
        when(userMapper.findUserRoles(user.getId())).thenReturn(Arrays.asList("USER"));
        when(userMapper.findUserPermissions(user.getId())).thenReturn(Arrays.asList("user:read_profile"));
        when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyList(), anyList())).thenReturn("mockAccessToken");
        when(jwtUtils.generateRefreshToken(anyLong())).thenReturn("mockRefreshToken");
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        TokenResponse response = userService.login(identifier, password);

        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        verify(userMapper).findByUsername(identifier);
        verify(passwordEncoder).matches(password, user.getPassword());
        verify(valueOperations).set(eq("refresh_token:1"), eq("mockRefreshToken"), eq(604800000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void testLoginInvalidCredentials() {
        String identifier = "nonexistent";
        String password = "wrongpassword";

        when(userMapper.findByUsername(identifier)).thenReturn(null);

        AuthException thrown = assertThrows(AuthException.class, () -> {
            userService.login(identifier, password);
        });

        assertEquals("用户名/手机号或密码不正确", thrown.getMessage());
        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyList(), anyList());
    }

    @Test
    void testLogoutSuccess() {
        Long userId = 1L;
        String mockRefreshToken = "mockRefreshToken";
        String mockJti = "mockJti";

        when(valueOperations.get(eq("refresh_token:" + userId))).thenReturn(mockRefreshToken);
        // 模拟jwtUtils.getClaimsFromToken 返回的Claims对象及其方法
        io.jsonwebtoken.Claims mockClaims = mock(io.jsonwebtoken.Claims.class);
        when(jwtUtils.getClaimsFromToken(mockRefreshToken)).thenReturn(mockClaims);
        when(mockClaims.getId()).thenReturn(mockJti);
        when(mockClaims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 100000));
        doNothing().when(redisTemplate).delete(eq("refresh_token:" + userId));
        doNothing().when(valueOperations).set(eq("blacklist:at:" + mockJti), eq("true"), anyLong(), any(TimeUnit.class));

        userService.logout(userId);

        verify(redisTemplate).delete(eq("refresh_token:" + userId));
        verify(valueOperations).set(eq("blacklist:at:" + mockJti), eq("true"), anyLong(), any(TimeUnit.class));
    }
}
