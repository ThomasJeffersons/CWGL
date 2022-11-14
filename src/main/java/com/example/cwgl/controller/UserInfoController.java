package com.example.cwgl.controller;


import com.example.cwgl.entity.*;
import com.example.cwgl.service.PrivilegeService;
import com.example.cwgl.service.UserInfoService;
import com.example.cwgl.utils.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
* description: TODO
* @author zhangsihai
* @date 2020/3/24 15:24
*/
@Controller
public class UserInfoController {

    @Resource
    private UserInfoService userInfoService;
    @Resource
    private PrivilegeService privilegeService;

    @Resource
    private JDBC13 jdbc13;
    @RequestMapping("regist.html")
    public String regist(HttpServletRequest request, HttpServletResponse response){
        return "regist";
    }
    @RequestMapping(value = {"/", "login.html"})
    public String toLogin(HttpServletRequest request, HttpServletResponse response){
        HttpSession session = request.getSession();
        String requestURI = request.getRequestURI();
        if(session.getAttribute(Config.CURRENT_USERNAME)==null){
            return "login";
        }else {
            try {
                response.sendRedirect("/pages/index");
            } catch (IOException e) {
                e.printStackTrace();
                return "login";
            }
            return null;
        }

    }
    @RequestMapping("regist")
    @ResponseBody
    public JSONData regist(User userInfo, HttpServletRequest request, HttpServletResponse response){
        User user  = jdbc13.selOneObj(Table.User,"username=?",userInfo.getUsername());
        if(user!=null){
            return JSONData.fail("用户名已存在");
        }
        return JSONData.result(jdbc13.insert(Table.User,userInfo,null));
    }

//    @RequestMapping(value = "/login.do",method = RequestMethod.POST)
    @RequestMapping(value = "/login.do")
    @ResponseBody
    public Result getUserInfo(User userInfo, HttpServletRequest request, HttpServletResponse response){
        boolean userIsExisted = userInfoService.userIsExisted(userInfo);
        System.out.println(userIsExisted + " - " + request.getHeader("token"));
        userInfo = getUserInfo(userInfo);
        if("client".equals(request.getHeader("token")) && !userIsExisted){
            //用户不存在
            return  ResultUtil.success(-1);
        }
        if (userIsExisted && userInfo == null){
            return  ResultUtil.unSuccess("用户名或密码错误！");
        }else {
            //将用户信息存入session
            userInfo = setSessionUserInfo(userInfo,request.getSession());
            //将当前用户信息存入cookie
            setCookieUser(request,response);
            return ResultUtil.success("登录成功", userInfo);
        }
    }

    @RequestMapping("/users/getUsersByWhere/{pageNo}/{pageSize}")
    public @ResponseBody Result getUsersByWhere(User userInfo, @PathVariable int pageNo, @PathVariable int pageSize, HttpSession session){
        if ("".equals(userInfo.getHouseid())){
            userInfo.setHouseid(null);
        }
        if (userInfo.getRoleid() == -1){
            userInfo.setRoleid(Config.getSessionUser(session).getRoleid());
        }
        Utils.log(userInfo.toString());
        PageModel model = new PageModel<>(pageNo,userInfo);
        model.setPageSize(pageSize);
        return userInfoService.getUsersByWhere(model);
    }

    @RequestMapping("/users/getWorkByWhere/{pageNo}/{pageSize}")
    public @ResponseBody JSONData getUsersByWhere(Work work, @PathVariable int pageNo, @PathVariable int pageSize, HttpSession session){
        Integer roleid = Config.getSessionUser(session).getRoleid();
        if(roleid==3){
            work.setUserid(Config.getSessionUser(session).getId());
        }
        work.setHouseid(Config.getSessionUser(session).getHouseid());
        return JSONData.ok(jdbc13.pageSelByObj(Table.Work,work,pageNo,pageSize,",=,=,like,,like"));
    }
    @RequestMapping("/users/getInvestByWhere/{pageNo}/{pageSize}")
    public @ResponseBody JSONData getInvestByWhere(Investment work, @PathVariable int pageNo, @PathVariable int pageSize, HttpSession session){
        Integer roleid = Config.getSessionUser(session).getRoleid();
        if(roleid==3){
            work.setUserid(Config.getSessionUser(session).getId());
        }
        work.setHouseid(Config.getSessionUser(session).getHouseid());
        return JSONData.ok(jdbc13.pageSelByObj(Table.Investment,work,pageNo,pageSize,",=,=,like,like"));
    }

    @RequestMapping("/user/add")
    public @ResponseBody Result addUser(User userInfo){
        System.out.println(userInfo);
        try {
            int num = userInfoService.add(userInfo);
            if(num>0){
                return ResultUtil.success();
            }else {
                return ResultUtil.unSuccess();
            }
        }catch (Exception e){
            return ResultUtil.error(e);
        }
    }
    @RequestMapping("/user/workadd")
    public @ResponseBody JSONData workadd(Work work, HttpSession session){
        work.setHouseid(Config.getSessionUser(session).getHouseid());
        work.setRealname(jdbc13.selOneBC("realname",Table.User,"id=?",work.getUserid()));
        return JSONData.result(jdbc13.insert(Table.Work,work,null));
    }
    @RequestMapping("/user/investadd")
    public @ResponseBody JSONData investadd(Investment work, HttpSession session){
        work.setHouseid(Config.getSessionUser(session).getHouseid());
        work.setRealname(jdbc13.selOneBC("realname",Table.User,"id=?",work.getUserid()));
        return JSONData.result(jdbc13.insert(Table.Investment,work,null));
    }

    @RequestMapping("/user/update")
    public @ResponseBody Result updateUser(User userInfo){
        try {
            int num = userInfoService.update(userInfo);
            if(num>0){
                return ResultUtil.success();
            }else {
                return ResultUtil.unSuccess();
            }
        }catch (Exception e){
            return ResultUtil.error(e);
        }
    }
    @RequestMapping("/user/workupdate")
    public @ResponseBody JSONData workupdate(Work work){
        return JSONData.result(jdbc13.update(Table.Work,work,"id"));
    }
    @RequestMapping("/user/investupdate")
    public @ResponseBody JSONData investupdate(Investment work){
        return JSONData.result(jdbc13.update(Table.Investment,work,"id"));
    }

    @RequestMapping("/user/del/{id}")
    public @ResponseBody Result deleteUser(@PathVariable String id){
        try {
            int num = userInfoService.delete(id);
            if(num>0){
                return ResultUtil.success();
            }else {
                return ResultUtil.unSuccess();
            }
        }catch (Exception e){
            return ResultUtil.error(e);
        }
    }
    @RequestMapping("/user/workdel/{id}")
    public @ResponseBody JSONData workdel(@PathVariable String id){
        return JSONData.result(jdbc13.deleteById(Table.Work,"id",id));
    }
    @RequestMapping("/user/investdel/{id}")
    public @ResponseBody JSONData investdel(@PathVariable String id){
        return JSONData.result(jdbc13.deleteById(Table.Investment,"id",id));
    }
    @RequestMapping("/getSessionUser")
    @ResponseBody
    public User getSessionUser(HttpSession session){
        User sessionUser = (User) session.getAttribute(Config.CURRENT_USERNAME);
        sessionUser.setPassword(null);
        return sessionUser;
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response){
        delCookieUser(request, response);
        request.getSession().removeAttribute(Config.CURRENT_USERNAME);
        return "login";
    }

    @RequestMapping("/getAllRoles")
    public @ResponseBody Result<Role> getAllRoles(){
        try {
            List<Role> roles = userInfoService.getAllRoles();
            if (roles.size()>0){
                return ResultUtil.success(roles);
            }else {
                return ResultUtil.unSuccess();
            }
        }catch (Exception e){
            return ResultUtil.error(e);
        }
    }

    @RequestMapping("/getAllUser")
    public @ResponseBody JSONData getAllUser(HttpSession session){
        int i = Config.getSessionUser(session).getHouseid();
        User user = new User();
        user.setHouseid(i);
        if(i==3){
            user.setId(Config.getSessionUser(session).getId());
        }
        return JSONData.ok(jdbc13.selByObj(Table.User,user,"=,,,,,="));
    }

    @RequestMapping("/role/add")
    public @ResponseBody Result addRole(Role role){
        try {
            int num = userInfoService.addRole(role);
            if(num>0){
                privilegeService.addDefaultPrivilegesWhenAddRole(role.getRoleid().toString());
                return ResultUtil.success();
            }else {
                return ResultUtil.unSuccess();
            }
        }catch (Exception e){
            return ResultUtil.error(e);
        }
    }

    @RequestMapping("/role/update")
    public @ResponseBody Result updateRole(Role role){
        try {
            int num = userInfoService.updateRole(role);
            if(num>0){
                return ResultUtil.success();
            }else {
                return ResultUtil.unSuccess();
            }
        }catch (Exception e){
            return ResultUtil.error(e);
        }
    }

    @RequestMapping("/role/del/{roleid}")
    public @ResponseBody Result deleteRole(@PathVariable String roleid){
        try {
            privilegeService.delPrivilegesWenDelRole(roleid);
            int num = userInfoService.deleteRole(roleid);
            if(num>0){
                return ResultUtil.success();
            }else {
                privilegeService.addDefaultPrivilegesWhenAddRole(roleid);
                return ResultUtil.unSuccess();
            }
        }catch (Exception e){
            return ResultUtil.error(e);
        }
    }

    @RequestMapping("/getRole/{id}")
    public @ResponseBody Result getRoleById(@PathVariable String id){
        try {
            Role role = userInfoService.getRoleById(id);
            if(role != null){
                return ResultUtil.success(role);
            }else {
                return ResultUtil.unSuccess();
            }
        }catch (Exception e){
            return ResultUtil.error(e);
        }
    }

    /**
     * 登录时将用户信息加入cookie中
     * @param response
     */
    private void setCookieUser(HttpServletRequest request, HttpServletResponse response){
        User user = getSessionUser(request.getSession());
        Cookie cookie = new Cookie(Config.CURRENT_USERNAME,user.getUsername()+"_"+user.getId());
        //cookie 保存7天
        cookie.setMaxAge(60*60*24*7);
        response.addCookie(cookie);
    }

    /**
     * 注销时删除cookie信息
     * @param request
     * @param response
     */
    private void delCookieUser(HttpServletRequest request, HttpServletResponse response){
        User user = getSessionUser(request.getSession());
        Cookie cookie = new Cookie(Config.CURRENT_USERNAME,user.getUsername()+"_"+user.getId());
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
    }

    /**
     * 通过用户信息获取用户权限信息，并存入session中
     * @param userInfo
     * @param session
     * @return
     */
    public User setSessionUserInfo(User userInfo, HttpSession session){
        List<Privilege> privileges = privilegeService.getPrivilegeByRoleid(userInfo.getRoleid());
        userInfo.setPrivileges(privileges);
        session.setAttribute(Config.CURRENT_USERNAME,userInfo);
        return userInfo;

    }

    public User getUserInfo(User userInfo){
       return userInfoService.getUserInfo(userInfo);
    }
}
