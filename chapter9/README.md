全局异常处理统一格式输出
------------------------

在 [第八章：整合hibernate-validator优雅表单校验](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter8)，
我们使用了 hibernate-validator 对参数进行校验，
如果校验失败，Spring 会使用默认的异常处理器对校验失败抛出的异常进行处理，并返回给调用端，
但如果希望返回的格式是自定义的格式，则需要自行设置全局异常处理器

### 课程目标

对 SpringBoot 项目中的所有异常进行响应，处理成统一的格式进行输出。

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

整体依赖如下所示

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-tomcat</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-undertow</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 编码

1. 构建统一输出类，作为项目所有接口的输出对象

```java
@Getter
@Setter
@NoArgsConstructor
public class RestData<T> {

    private static final String SUCCESS_INFO = "操作成功";
    private static final String FAIL_INFO = "操作失败";

    // 接口调用状态，true表示调用成功，false表示调用失败
    private boolean status;
    // 提示信息，如果没有设置，则根据status值使用默认提示
    private String info;
    // 返回数据
    private T data;

    private RestData(boolean status, String info) {
        this.status = status;
        this.info = info;
    }

    /**
     * 操作是否成功
     */
    public boolean isSuccess() {
        return this.status;
    }

    /**
     * 操作成功，组装返回数据
     */
    public RestData<T> success() {
        return this.success(null);
    }

    /**
     * 操作成功，组装返回数据
     */
    public RestData<T> success(String info) {
        return success(info, null);
    }

    /**
     * 操作成功，组装返回数据
     */
    public RestData<T> success(String info, T data) {
        if (info != null && !info.isEmpty()) {
            this.status = true;
            this.info = info;
            this.data = data;
            return this;
        } else {
            return success(SUCCESS_INFO, data);
        }
    }

    /**
     * 操作成功，组装返回数据
     */
    public RestData<T> error() {
        return this.error(null);
    }

    /**
     * 操作成功，组装返回数据
     */
    public RestData<T> error(String info) {
        return error(info, null);
    }

    /**
     * 操作成功，组装返回数据
     */
    public RestData<T> error(String info, T data) {
        if (info != null && !info.isEmpty()) {
            this.status = false;
            this.info = info;
            this.data = data;
            return this;
        } else {
            return error(FAIL_INFO, data);
        }
    }

}
```

2. 全局异常处理器

- 类上添加 `@RestControllerAdvice` 注解，是 `@ResponseBody` 与 `@ControllerAdvice` 的结合，其中 `@ControllerAdvice` 用于注册异常处理器，而 `@ResponseBody` 用于标记该类中所有方法返回类型为 JSON。
- 方法上添加 `@ExceptionHandler` 注解，用于标记该方法用于处理何种异常。
- 该类中可以同时编写多个方法，用于处理多种异常，Spring 会自行根据异常类型选择执行相应的方法。

```java
package com.mhkj.config;

import com.mhkj.utils.RestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * 校验错误拦截处理
     * 使用 @RequestBody 接收入参时，校验失败抛 MethodArgumentNotValidException 异常
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public RestData handler(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException handler", e);
        BindingResult bindingResult = e.getBindingResult();
        if (bindingResult.hasFieldErrors()) {
            return new RestData().error(bindingResult.getFieldError().getDefaultMessage());
        }
        return new RestData().error("parameter is not valid");
    }

    /**
     * 校验错误拦截处理
     * 使用 @RequestBody 接收入参时，数据类型转换失败抛 HttpMessageConversionException 异常
     */
    @ExceptionHandler(value = HttpMessageConversionException.class)
    public RestData handler(HttpMessageConversionException e) {
        log.error("HttpMessageConversionException handler", e);
        return new RestData().error(e.getMessage());
    }

    /**
     * 全局异常处理
     */
    @ExceptionHandler(value = Exception.class)
    public RestData handler(Exception e) {
        log.error("exception handler", e);
        return new RestData().error(e.getMessage());
    }

}
```

3. Controller 层代码

```java
@RestController
public class UserController {

    @RequestMapping("/register")
    public String doRegister(@Valid @RequestBody UserBO bo) {
        return "success";
    }

}
```

4. 启动类

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```

### 验证结果

编写测试用例

```java
@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = Application.class)
public class UserTest {

    private MockMvc mvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void test1() throws Exception {
        MvcResult mvcResult = mvc.perform(
                MockMvcRequestBuilders
                .post("/user/add")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"name\":\"user1\",\"sex\":1,\"birthday\":\"2030-05-21\"}")
        )
        .andDo(MockMvcResultHandlers.print())
        .andReturn();

        Assert.assertEquals(200, mvcResult.getResponse().getStatus());
    }

}
```

调用测试用例可以看到输出如下所示，因为入参不符合校验规则，所以 hiberenate-validator 返回了一个异常，该异常被全局异常处理器捕获，处理后返回调用方

```
{"status":false,"info":"用户等级不能为空","data":null,"success":false}
```

### 源码地址

本章源码 : [https://github.com/lizhengdan/spring-boot-tutorial.git](https://github.com/lizhengdan/spring-boot-tutorial.git)

### 结束语

通过全局异常处理，可以保证系统在出现异常的情况下，出参结构是统一的，结合接口正常情况下的返回结构，就可以保证整个系统的出参结构一致，这种方式不管是跟前端交互还是跟其它系统交互，都很重要。
