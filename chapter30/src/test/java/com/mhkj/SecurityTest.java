package com.mhkj;

import com.mhkj.entity.*;
import com.mhkj.repository.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.List;

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
