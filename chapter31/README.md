整合SpringSecurity之自定义MD5加密
---------------------------------

在前面的文章中，我们已经基本完成对 SpringSecurity 的整合

- [第二十四章：整合SpringSecurity之最简登录及方法鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter24)
- [第二十五章：整合SpringSecurity之使用数据库实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter25)
- [第二十六章：整合SpringSecurity之JSON格式前后端交互](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter26)
- [第二十七章：整合SpringSecurity之前后端分离使用Token实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter27)
- [第二十八章：整合SpringSecurity之前后端分离使用JWT实现登录鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter28)
- [第二十九章：整合SpringSecurity之Swagger文档传递Token](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter29)
- [第三十章：整合SpringSecurity之基于SpEL表达式实现动态方法鉴权](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter30)

### 目标

使用MD5加密方式进行登录鉴权

### 操作步骤

#### 自定义加密算法

自定义一个MD5加密类，需要实现 `PasswordEncoder` 接口，这里的加密引用了 `commons-codec` 的实现，需要在 pom 文件中添加对 `commons-codec` 的依赖

```java
public class MD5PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return DigestUtils.md5Hex((String) rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encodedPassword.equals(encode(rawPassword));
    }

}
```

#### 注册加密类

简单地声明 Bean 即可

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new MD5PasswordEncoder();
}
```

### 源码地址

本章源码 : [https://github.com/lizhengdan/spring-boot-tutorial.git](https://github.com/lizhengdan/spring-boot-tutorial.git)
