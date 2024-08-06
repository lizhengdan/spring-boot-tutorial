整合SpringSecurity之Swagger单元测试传递Token
---

通过前面的文章，我们一步步实现了前后端分离模式下的基于token实现系统权限验证

 - [第二十四章：整合SpringSecurity之最简登录及方法鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter24)
 - [第二十五章：整合SpringSecurity之基于数据库实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter25)
 - [第二十六章：整合SpringSecurity之前后端分离使用JSON格式交互](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter26)
 - [第二十七章：整合SpringSecurity之前后端分离使用Token实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter27)
 - [第二十八章：整合SpringSecurity之前后端分离使用JWT实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter28)

也实现了对 Swagger 的集成
 - [第十一章：整合Swagger2自动生成API文档](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter11)

通过 Swagger 提供的网页，我们可以直接对后台接口实现单元测试，
但是在前后端分离的项目中，请求时需要附带鉴权使用的 token，否则会被系统拒绝访问。
所以本文要解决的就是，使用 Swagger2 进行单元测试时，怎么传递 Token 的问题。

### 目标
整合 SpringSecurity 实现使用 Swagger2 文档对后台接口进行测试，并传入Token。

### 操作步骤

#### 方案一
##### 配置 Swagger2
通过 `globalOperationParameters` 方法为接口添加参数
```java
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        ParameterBuilder builder = new ParameterBuilder();
        builder.name("Authorization").description("认证token")
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .required(false)
                .build();

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .globalOperationParameters(Collections.singletonList(builder.build()));
    }

    /**
     * 创建该API的基本信息（这些基本信息会展现在文档页面中）
     * 访问地址：http://项目实际地址/swagger-ui.html
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("接口文档")
                .version("1.0.0-SNAPSHOT")
                .contact(new Contact("Bruce.Gong", "", ""))
                .build();
    }

}
```

##### 配置 SpringSecurity
SpringSecurity 默认会将 Swagger-UI 的网页拦截，所以要访问 Swagger2 的接口文档，需要去掉拦截。
```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
                "/v2/api-docs",
                "/swagger-resources/configuration/ui",
                "/swagger-resources",
                "/swagger-resources/configuration/security",
                "/swagger-ui.html");
    }
    
}
```

##### 编写接口
```java
@RestController
@Api("测试组")
public class TestController {

    @ApiOperation(value = "测试", notes = "测试")
    @GetMapping("/testGet")
    public String testGet() {
        return "测试成功";
    }

}
```

#### 验证
通过地址 `http://localhost:8080/swagger-ui.html` 访问 Swagger2 接口文档，
选择上一步编写的测试接口，点击 `Try it out`，
如下图所示，可以看到 `Authorization` 变成了可输入项。
![](https://img-blog.csdnimg.cn/20200310181205381.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2dvbmdtMjQ=,size_16,color_FFFFFF,t_70)

我们什么也不输入，直接点击 `Execute` 按钮接交请求，结果返回如下，
请求被 `AuthenticationEntryPoint` 拦截，说明当前用户未登录。
```json
{
  "message": "Full authentication is required to access this resource"
}
```

使用 postman 访问 `http://localhost:8080/login` 进行登录，登录成功后会返回 token，将 token 的值填入 Authorization 输入项，再次接交，结果显示 `测试成功`，请求被放行。

#### 升级方案
按上面的方法，测试一个接口没有问题，但是如果要测试很多接口，则需要在每一次测试时，手动填上 token 值，非常麻烦，有没有办法填写一次，所有接口都可以共用呢？

##### 修改 Swagger2 注册
```java
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2).
                useDefaultResponseMessages(false)
                .select()
                .apis(RequestHandlerSelectors.any())
                .build()
                .securitySchemes(Collections.singletonList(securityScheme()))
                .securityContexts(Collections.singletonList(securityContext()));
    }

    private SecurityScheme securityScheme() {
        return new ApiKey("Authorization", "Authorization", "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .forPaths(PathSelectors.regex("^(?!auth).*$"))
                        .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Collections.singletonList(
                new SecurityReference("Authorization", authorizationScopes));
    }

    /**
     * 创建该API的基本信息（这些基本信息会展现在文档页面中）
     * 访问地址：http://项目实际地址/swagger-ui.html
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("接口文档")
                .version("1.0.0-SNAPSHOT")
                .contact(new Contact("Bruce.Gong", "", ""))
                .build();
    }
}
```

#### 验证
通过地址 `http://localhost:8080/swagger-ui.html` 访问 Swagger2 接口文档，
如下图所示，原来的 `Authorization` 输入框消失了，出现的是右上角的 `Authorize` 按钮，
![](https://img-blog.csdnimg.cn/20200310181237211.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2dvbmdtMjQ=,size_16,color_FFFFFF,t_70)

点击按钮会出现一个弹窗，弹窗内可输入 `Authorization`
![](https://img-blog.csdnimg.cn/20200310181247746.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2dvbmdtMjQ=,size_16,color_FFFFFF,t_70)

### 源码地址

本章源码 : <https://gitee.com/gongm_24/spring-boot-tutorial.git>

