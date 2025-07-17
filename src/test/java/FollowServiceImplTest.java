import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mengnankk.dto.Result;
import com.mengnankk.dto.UserDTO;
import com.mengnankk.entity.Blog;
import com.mengnankk.entity.User;
import com.mengnankk.mapper.BlogMapper;
import com.mengnankk.service.BlogService;
import com.mengnankk.service.FollowService;
import com.mengnankk.service.UserService;
import com.mengnankk.service.Impl.BlogServiceImpl;
import com.mengnankk.utils.AuthContextHolder;
import com.mengnankk.utils.LayeredBlogCache;
import com.mengnankk.utils.RedisConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.data.redis.core.*;

public class FollowServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private FollowService followService;
    @Mock
    private LayeredBlogCache layeredBlogCache;
    @Mock
    private BlogMapper blogMapper; // Mock BlogMapper
    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<User>> userQueryCaptor;


    @InjectMocks
    private BlogServiceImpl blogService;

    private Blog mockBlog;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // 初始化 Mockito
        mockBlog = new Blog();
        mockBlog.setId(1L);
        mockBlog.setUserId(2L);
        mockBlog.setContent("testContent");
    }

    @Test
    void queryBlogById_BlogExistsInDb_ReturnsBlog() throws JsonProcessingException {
        // Arrange
        Long blogId = 1L;
        User user = new User();
        user.setId(2L);
        user.setUsername("testUser");
        String blogKey = "blog:id:" + blogId;

        when(redisTemplate.opsForValue().get(blogKey)).thenReturn(null); // 模拟缓存未命中
        when(blogMapper.selectById(blogId)).thenReturn(mockBlog); // 模拟数据库返回 Blog
        when(userService.getById(mockBlog.getUserId())).thenReturn(user);     // 模拟数据库返回 User
        when(objectMapper.writeValueAsString(mockBlog)).thenReturn("{\"id\":1,\"userId\":2,\"content\":\"testContent\"}");  // Mock 序列化动作

        // Act
        Result result = blogService.queryBlogById(blogId);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getData());
        Blog blog = (Blog) result.getData();
        assertEquals(blogId, blog.getId());
        assertEquals("testUser", blog.getName());
        verify(redisTemplate.opsForValue(), times(1)).set(eq(blogKey), anyString());
    }

    @Test
    void queryBlogLikes_CacheHit_ReturnsCachedUsers() {
        // Arrange
        Long blogId = 1L;
        List<UserDTO> cachedUsers = Collections.singletonList(new UserDTO());
        when(layeredBlogCache.getCachedLikedUsers(blogId)).thenReturn(cachedUsers);

        // Act
        Result<List<UserDTO>> result = blogService.queryBlogLikes(blogId);

        // Assert
        assertNotNull(result);
        assertEquals(cachedUsers, result.getData());
        verify(redisTemplate, never()).execute(any(RedisCallback.class));
    }

    @Test
    void queryBlogLikes_CacheMiss_ReturnsDbUsers() {
        // Arrange
        Long blogId = 1L;
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        User user = new User();
        user.setId(1L);

        Set<String> topNuserids = new HashSet<>();
        topNuserids.add("1");
        when(layeredBlogCache.getCachedLikedUsers(blogId)).thenReturn(null);
        when(redisTemplate.execute((RedisCallback<Object>) any())).thenReturn(Collections.singletonList(userDTO));
        when(userService.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(user));

        // Act
        Result<List<UserDTO>> result = blogService.queryBlogLikes(blogId);

        // Assert
        assertNotNull(result);
        List<UserDTO> userDTOList = (List<UserDTO>) result.getData();
        assertFalse(userDTOList.isEmpty());

        verify(layeredBlogCache,times(2)).getCachedLikedUsers(blogId);
        verify(redisTemplate,times(1)).execute((RedisCallback<Object>) any());
    }
    // ... 更多测试函数
}
