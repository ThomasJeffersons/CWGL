package com.example.cwgl.dao;


import com.example.cwgl.entity.House;
import com.example.cwgl.entity.Role;
import com.example.cwgl.entity.User;
import com.example.cwgl.utils.PageModel;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInfoMapper {

    /**
     * 获取单个用户信息，可用于：
     * 1.登录
     * 2.通过用户某一部分信息获取用户完整信息
     * @param userInfo
     * @return
     */
    User getUserInfo(User userInfo);

    /**
     * 注册
     * @param userInfo
     * @return
     */
    int addUser(User userInfo);

    /**
     * 通过username判断该用户是否存在
     * @param userInfo
     * @return
     */
    int userIsExisted(User userInfo);

    /**
     * 通过条件获取符合条件的优化信息 -- 分页
     * @param model
     * @return
     */
    List<User> getUsersByWhere(PageModel<User> model);

    int getToatlByWhere(PageModel<User> model);

    int add(User userInfo);

    int update(User userInfo);

    int delete(String id);

    List<Role> getAllRoles();

    int addRole(Role role);

    int updateRole(Role role);

    int deleteRole(String id);

    Role getRoleById(String id);

    int addHouseId(House house);
}
