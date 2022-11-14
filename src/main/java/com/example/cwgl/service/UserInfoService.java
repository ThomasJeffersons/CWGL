package com.example.cwgl.service;


import com.example.cwgl.entity.Role;
import com.example.cwgl.entity.User;
import com.example.cwgl.utils.PageModel;
import com.example.cwgl.utils.Result;

import java.util.List;

public interface UserInfoService {

    int add(User userInfo);

    int update(User userInfo);

    boolean userIsExisted(User userInfo);

    int delete(String id);

    User getUserInfo(User userInfo);

    Result getUsersByWhere(PageModel<User> model);

    List<Role> getAllRoles();

    int addRole(Role role);

    int updateRole(Role role);

    int deleteRole(String id);

    Role getRoleById(String id);

}
