package com.mhkj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mhkj.entity.Resource;
import com.mhkj.entity.Role;
import com.mhkj.entity.RoleResource;
import com.mhkj.repository.ResourceRepository;
import com.mhkj.repository.RoleRepository;
import com.mhkj.repository.RoleResourceRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
