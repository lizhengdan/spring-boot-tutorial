整合SpringSecurity之基于SpEL表达式实现动态方法鉴权
---

通过前面的文章，我们已经实现了基于数据进行登录鉴权及基于注解的方式进行方法鉴权

 - [第二十四章：整合SpringSecurity之最简登录及方法鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter24)
 - [第二十五章：整合SpringSecurity之基于数据库实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter25)
 - [第二十六章：整合SpringSecurity之前后端分离使用JSON格式交互](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter26)
 - [第二十七章：整合SpringSecurity之前后端分离使用Token实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter27)
 - [第二十八章：整合SpringSecurity之前后端分离使用JWT实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter28)

注解方式的方法鉴权：
通过 `@EnableGlobalMethodSecurity` 注解来开启方法鉴权。
 - securedEnabled：开启 @Secured 注解
    - 单个角色：@Secured("ROLE_USER")
    - 多个角色任意一个：@Secured({"ROLE_USER","ROLE_ADMIN"})
 - prePostEnabled：开启 @PreAuthorize 及 @PostAuthorize 注解，分别适用于进入方法前后进行鉴权，支持表达式
    - 允许所有访问：@PreAuthorize("true")
    - 拒绝所有访问：@PreAuthorize("false")
    - 单个角色：@PreAuthorize("hasRole('ROLE_USER')")
    - 多个角色与条件：@PreAuthorize("hasRole('ROLE_USER') AND hasRole('ROLE_ADMIN')")
    - 多个角色或条件：@PreAuthorize("hasRole('ROLE_USER') OR hasRole('ROLE_ADMIN')")
 - jsr250Enabled：开启 JSR-250 相关注解
    - 允许所有访问：@PermitAll
    - 拒绝所有访问：@DenyAll
    - 多个角色任意一个：@RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})

虽然非常灵活，但是毕竟是硬编码，不符合实际的生产需求，在项目中，每个角色的可访问权限必须是可调整的，一般情况下使用数据库进行持久化。

### 目标
整合 SpringSecurity 及 MybatisPlus 实现使用读取数据库数据进行方法鉴权

### 思路
使用配置类的 HttpSecurity 提供的 access 方法，通过扩展SpEL表达式，实现自定义鉴权
```
.access("@authService.canAccess(request, authentication)")
```
其中 authService 是 Spring 容器中的 Bean，canAccess 是其中的一个方法。
```java
@Service
public class AuthService {
    public boolean canAccess(HttpServletRequest request, Authentication authentication) {
        //在这里编写校验代码…
        return true;
    }
}
```

### 准备工作
创建用户表 `user`、角色表 `role`、用户角色关系表 `user_role`，资源表 `resource`，资源角色关系表 `role_resource`

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
  UNIQUE KEY (`user_id`,`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

CREATE TABLE `resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `url` varchar(128) NOT NULL COMMENT '请求路径',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`url`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COMMENT='资源';

CREATE TABLE `role_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`resource_id`,`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='资源角色关系表';
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
用户登录相关代码请参考 [第二十五章：整合SpringSecurity之使用数据库实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter25)，这里不再粘贴。

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
资源实体
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("resource")
public class Resource {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;

}
```
资源角色关系实体
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("role_resource")
public class RoleResource {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
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
public interface ResourceRepository extends BaseMapper<Resource> {
}
@Mapper
public interface RoleResourceRepository extends BaseMapper<RoleResource> {
}
```
##### 实现自定义方法鉴权
```java
@Service
@AllArgsConstructor
public class AuthService {

    private ResourceRepository resourceRepository;
    private RoleResourceRepository roleResourceRepository;
    private RoleRepository roleRepository;

    public boolean canAccess(HttpServletRequest request, Authentication authentication) {
        String uri = request.getRequestURI();
        List<Role> requestRoles = getRolesForResource(uri);
        if (requestRoles != null && !requestRoles.isEmpty()) {
            for (Role requestRole : requestRoles) {
                for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
                    if (requestRole.getAuthority().equals(grantedAuthority.getAuthority())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<Role> getRolesForResource(String uri) {
        if (StringUtils.isEmpty(uri)) {
            return Collections.emptyList();
        }
        Resource resource = resourceRepository.selectOne(
                new QueryWrapper<Resource>().lambda().eq(Resource::getUrl, uri));
        if (resource == null) {
            return Collections.emptyList();
        }
        List<RoleResource> roleResources = roleResourceRepository.selectList(
                new QueryWrapper<RoleResource>().lambda().eq(RoleResource::getResourceId, resource.getId()));
        if (roleResources == null || roleResources.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = roleResources.stream().map(RoleResource::getRoleId).collect(Collectors.toList());
        return roleRepository.selectList(
                new QueryWrapper<Role>().lambda().in(Role::getId, roleIds));
    }

}
```

##### 注册配置
不用再声明 `@EnableGlobalMethodSecurity` 注解，注册自定义鉴权方法 `authService.canAccess`。
```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserService userService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .anyRequest().access("@authService.canAccess(request, authentication)")
//            .anyRequest().authenticated()
            .and()
            .formLogin().and().httpBasic();
    }
}
```

##### 去掉原来的方法鉴权相关注解
```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

//    @Secured("ROLE_USER")
    @GetMapping("/secure")
    public String secure() {
        return "Hello Security";
    }

//    @PreAuthorize("true")
    @GetMapping("/authorized")
    public String authorized() {
        return "Hello World";
    }

//    @PreAuthorize("false")
    @GetMapping("/denied")
    public String denied() {
        return "Goodbye World";
    }

}
```

##### 启动类
```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```
#### 验证结果
##### 初始化数据
执行测试用例进行初始化数据
```java
@Slf4j
@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = Application.class)
@AllArgsConstructor
public class SecurityTest {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleRepository roleRepository;
    private ResourceRepository resourceRepository;
    private RoleResourceRepository roleResourceRepository;

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

        List<Resource> resourceList = new ArrayList<>();
        resourceList.add(new Resource(1L, "/hello"));
        resourceList.add(new Resource(2L, "/secure"));

        List<RoleResource> rrList = new ArrayList<>();
        rrList.add(new RoleResource(1L, 1L, 1L));
        rrList.add(new RoleResource(1L, 2L, 1L));
        rrList.add(new RoleResource(1L, 1L, 2L));

        userList.forEach(userRepository::insert);
        roleList.forEach(roleRepository::insert);
        urList.forEach(userRoleRepository::insert);
        resourceList.forEach(resourceRepository::insert);
        rrList.forEach(roleResourceRepository::insert);
    }

}
```

##### 校验
使用 `admin` 登录可以访问 `/hello` 及 `/secure`，使用 `user` 登录则只能访问 `/hello`

### 源码地址

本章源码 : <https://gitee.com/gongm_24/spring-boot-tutorial.git>

### 参考

[249.Spring Boot+Spring Security：基于URL动态权限：扩展access()的SpEL表达式](https://mp.weixin.qq.com/s?__biz=MzA4ODIyMzEwMg==&mid=2447533899&idx=1&sn=fa4272d828147ef2a6b5246acb724283&chksm=843bbb5ab34c324cf7392f28bd79fcf2bbd06b697f5123ca83104903c64b92b06abaaa349808&scene=21#wechat_redirect)