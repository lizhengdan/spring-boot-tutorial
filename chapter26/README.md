整合SpringSecurity之前后端分离使用JSON格式交互
----------------------------------------------

在前端的文章中，我们实现了使用 SpringSecurity 实现登录鉴权，并使用数据库存储用户信息，实现登录鉴权

- [第二十四章：整合SpringSecurity之最简登录及方法鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter24)
- [第二十五章：整合SpringSecurity之基于数据库实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter25)

登录页都是使用 SpringSecurity 提供的默认登录，入参为 username 及 password，前端通过 form 表单提交请求，后台使用 request.getParameter() 获取填写数据。

目前，在实际项目中，更多的企业选择使用前后端分离的项目架构，前后端数据交互选择使用 application/json 方式进行传递，同时，在实际的项目中，拥有更多形式的登录，比如手机号/短信验证码，不能使用单一的 usernamepassword 结构进行处理，需要自行定义入参结构。

### 目标

本章将整合 SpringSecurity 实现使用自定义格式进行登录，并使用 json 方式进行前后端交互。

### 准备工作

创建用户表 `user`、角色表 `role`、用户角色关系表 `user_role`

```mysql
CREATE TABLE `role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `rolename` varchar(32) NOT NULL COMMENT '角色名',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE `user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`,`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';
```

### 操作步骤

#### 添加依赖

引入 Spring Boot Starter 父工程

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.0.5.RELEASE</version>
</parent>
```

添加 `springSecurity` 及 `mybatisPlus` 的依赖，添加后的整体依赖如下

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.2.0</version>
    </dependency>

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
</dependencies>
```

#### 配置

配置一下数据源

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/test?characterEncoding=utf8&useSSL=false
    username: app
    password: 123456
```

#### 编码

##### 实体类

角色实体类 Role，实现权限接口 GrantedAuthority

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("role")
public class Role implements GrantedAuthority {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String rolename;

    @Override
    public String getAuthority() {
        return this.rolename;
    }
}
```

用户实体类 user，实现权限接口 UserDetails，主要方法是 getAuthorities，用于获取用户的角色列表

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User implements UserDetails {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    @TableField(exist = false)
    private List<Role> roleList;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleList;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
```

用户角色关系实体

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_role")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;

}
```

##### Repository 层

分别为三个实体类添加 Mapper

```java
@Mapper
public interface RoleRepository extends BaseMapper<Role> {
}
@Mapper
public interface UserRepository extends BaseMapper<User> {
}
@Mapper
public interface UserRoleRepository extends BaseMapper<UserRole> {
}
```

#### 权限配置

##### 实现 UserDetailsService 接口

UserDetailsService 是 SpringSecurity 提供的登陆时用于根据用户名获取用户信息的接口

```java
@AllArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isEmpty()) {
            throw new UsernameNotFoundException("用户名不能为空");
        }
        User user = userRepository.selectOne(new QueryWrapper<User>().lambda().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        List<UserRole> userRoles = userRoleRepository.selectList(new QueryWrapper<UserRole>().lambda().eq(UserRole::getUserId, user.getId()));
        if (userRoles != null && !userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            List<Role> roles = roleRepository.selectList(new QueryWrapper<Role>().lambda().in(Role::getId, roleIds));
            user.setRoleList(roles);
        }
        return user;
    }

}
```

##### 自定义登录参数格式

```java
@Data
public class LoginDto {

    private String mobile;
    private String password;
    private String dycode;

}
```

##### 自定义登录过滤器

继承 SpringSecurity 提供的 AbstractAuthenticationProcessingFilter 类，实现 attemptAuthentication 方法，用于登录校验。
本例中，模拟前端使用 json 格式传递参数，所以通过 objectMapper.readValue 的方式从流中获取入参，之后借用了用户名密码登录的校验，并返回权限对象

```java
@Data
public class JsonAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private ObjectMapper objectMapper;

    public JsonAuthenticationFilter() {
        super(new AntPathRequestMatcher("/login", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        // 从输入流中获取到登录的信息
        try {
            LoginDto loginUser = objectMapper.readValue(request.getInputStream(), LoginDto.class);
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(loginUser.getMobile(), loginUser.getPassword())
            );
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
```

##### 自定义登陆成功后处理

实现 SpringSecurity 提供的 AuthenticationSuccessHandler 接口，使用 JSON 格式返回

```java
@AllArgsConstructor
public class JsonLoginSuccessHandler implements AuthenticationSuccessHandler {

    private ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(authentication));
    }

}
```

##### 自定义登陆失败后处理

实现 SpringSecurity 提供的 AuthenticationFailureHandler 接口，使用 JSON 格式返回

```java
public class JsonLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, 
                                        AuthenticationException exception) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getWriter().write("{\"message\":\"" + exception.getMessage() + "\"}");
    }

}
```

##### 自定义权限校验失败后处理

登陆成功之后，访问接口之前 SpringSecurity 会进行鉴权，如果没有访问权限，需要对返回进行处理。
实现 SpringSecurity 提供的 AccessDeniedHandler 接口，使用 JSON 格式返回

```java
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getWriter().write("{\"message\":\"" + exception.getMessage() + "\"}");
    }

}
```

##### 自定义未登录后处理

实现 SpringSecurity 提供的 AuthenticationEntryPoint 接口，使用 JSON 格式返回

```java
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getWriter().write("{\"message\":\"" + exception.getMessage() + "\"}");
    }

}
```

##### 注册

在 configure 方法中调用 addFilterAfter 方法，将自定义的 jsonAuthenticationFilter 注册进 SpringSecurity 的过滤器链中。

```java
@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(new BCryptPasswordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().anyRequest().authenticated()
            .and().csrf().disable()
            .addFilterAfter(jsonAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling()
            .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
            .accessDeniedHandler(new JsonAccessDeniedHandler());
    }

    @Bean
    public JsonAuthenticationFilter jsonAuthenticationFilter() throws Exception {
        JsonAuthenticationFilter filter = new JsonAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager());
        filter.setObjectMapper(this.objectMapper);
        filter.setAuthenticationSuccessHandler(jsonLoginSuccessHandler());
        filter.setAuthenticationFailureHandler(new JsonLoginFailureHandler());
        return filter;
    }

    @Bean
    public JsonLoginSuccessHandler jsonLoginSuccessHandler() {
        return new JsonLoginSuccessHandler(objectMapper);
    }

}
```

#### 启动类

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```

### 验证结果

#### 初始化数据

执行测试用例进行初始化数据

```java
@Slf4j
@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = Application.class)
public class SecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void initData() {
        List<User> userList = new ArrayList<>();
        userList.add(new User(1L, "admin", new BCryptPasswordEncoder().encode("123456"), null));
        userList.add(new User(2L, "user", new BCryptPasswordEncoder().encode("123456"), null));

        List<Role> roleList = new ArrayList<>();
        roleList.add(new Role(1L, "ROLE_ADMIN"));
        roleList.add(new Role(2L, "ROLE_USER"));

        List<UserRole> urList = new ArrayList<>();
        urList.add(new UserRole(1L, 1L, 1L));
        urList.add(new UserRole(2L, 1L, 2L));
        urList.add(new UserRole(3L, 2L, 2L));

        userList.forEach(userRepository::insert);
        roleList.forEach(roleRepository::insert);
        urList.forEach(userRoleRepository::insert);
    }

}
```

### 源码地址

本章源码 : [https://github.com/lizhengdan/spring-boot-tutorial.git](https://github.com/lizhengdan/spring-boot-tutorial.git)

### 扩展

前面的示例只是使用了一个自定义结构去接收前端表单数据，但是处理还是使用的 `username/password` 那一套，
调用 `getAuthenticationManager().authenticate()` 去进行入参的校验，
`AuthenticationManager` 中维护一个 `AuthenticationProvider` 列表，
每一个 `AuthenticationProvider` 会支持一个 `Token` 类型，
`username/password` 则是使用的 `UsernamePasswordAuthenticationToken`，
并为之提供了 `AbstractUserDetailsAuthenticationProvider` 进行登录校验，
`AbstractUserDetailsAuthenticationProvider` 的实现类就是 `DaoAuthenticationProvider`，
注册的时候通过 `auth.userDetailsService(userService)` 方法调用，
会将 `DaoAuthenticationProvider` 加入至 `AuthenticationManager` 中维护的 `AuthenticationProvider` 列表中。

如果需要完全自定义入参结构，按如下操作即可
1、自定义 `AbstractAuthenticationToken` 实现类
2、自定义 `AuthenticationProvider` 实现类，并支持自定义的 `AbstractAuthenticationToken` 实现类，具体实现可以参考 `AbstractUserDetailsAuthenticationProvider`
3、自定义 `AuthenticationProcessingFilter` 实现类，需要定义请求地址，接收入参，组装 `Token`，调用 `getAuthenticationManager().authenticate()`
4、将自定义 `AuthenticationProvider`实现类加入至 `AuthenticationManager` 中维护的 `AuthenticationProvider` 列表中。
5、将自定义 `AuthenticationProcessingFilter` 实现类加入至过滤器列表中，置于 `UsernamePasswordAuthenticationFilter` 之后。
