package com.study.controller;

import com.study.pojo.*;
import com.study.service.admin.AdminService;
import com.study.service.depart.DepartService;
import com.study.service.doctor.DoctorService;
import com.study.service.user.UserService;
import com.study.utils.encryption.EncryptUtil;
import com.study.utils.result.Result;
import com.study.utils.result.ResultCode;
import com.study.utils.token.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @program: college-health
 * @author: Shixin.Duan
 * @Time: 2021/7/1  9:34
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private AdminService adminService;
    private DoctorService doctorService;
    private UserService userService;
    private DepartService departService;

    @Autowired
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }
    @Autowired
    public void setDoctorService(DoctorService doctorService) {
        this.doctorService = doctorService;
    }
    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    @Autowired
    public void setDepartService(DepartService departService) {
        this.departService = departService;
    }

    @PostMapping("/login")
    public Result login(String username, String password, HttpServletRequest request,
                        HttpSession session) throws ParseException {

        System.out.println(EncryptUtil.encrypt(password));
        Admin admin = adminService.login(username, EncryptUtil.encrypt(password));
        if (admin != null) {
            //设置登录用户类型 1代表管理员
            String userType = "1";
            String token = TokenUtil.sign(username,userType);
            String loginIp = request.getHeader("x-forwarded-for");
            if (loginIp == null || loginIp.length() == 0 || "unknown".equalsIgnoreCase(loginIp)) {
                loginIp = request.getHeader("Proxy-Client-IP");//获取代理的IP
            }
            if (loginIp == null || loginIp.length() == 0 || "unknown".equalsIgnoreCase(loginIp)) {
                loginIp = request.getHeader("WL-Proxy-Client-IP");//获取代理的IP
            }
            if (loginIp == null || loginIp.length() == 0 || "unknown".equalsIgnoreCase(loginIp)) {
                loginIp = request.getRemoteAddr();
            }
            Date date = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String nowTime = simpleDateFormat.format(date);
            Date loginTime = simpleDateFormat.parse(nowTime);
            Date logoutTime = loginTime;
            Integer isSafeExit = 0;
            adminService.insAdminLog(username, loginIp, loginTime, logoutTime, isSafeExit);
            //保存登录时间
            session.setAttribute("adminLoginTime",loginTime);
            //返回数据给前端
            Object[] objects = {admin,token,userType};
            return new Result(ResultCode.SUCCESS,objects);
        }
        return new Result(ResultCode.USERNAMEPASSWORDERROR);
    }

    //获取登录日志
    @RequestMapping("/getAdminLogList")
    public Result getAdminLogList(Integer page, Integer limit)
            throws ParseException {
        return adminService.getAdminLogList(page, limit);
    }

    @RequestMapping("/getAllUserList")
    public Result getAllUserList(Integer page, Integer limit, UserSearch search) {
       return userService.getAllUserList(page, limit, search);
    }

    @RequestMapping("/doctorList")
    public Result doctorList(HttpSession session) {
        List<Depart> departs = departService.getAllDeparts();
        session.setAttribute("departs", departs);
        return new Result(ResultCode.SUCCESS,departs);
    }

    @RequestMapping("/getAllDoctorList")
    public Result getAllDoctorList(Integer page, Integer limit, DoctorSearch search,HttpSession session) {
        System.out.println(session.getAttribute("adminLoginTime"));
        return doctorService.getAllDoctorList(page, limit, search);
    }

    @RequestMapping("/deleteDoctorById/{doctor_id}")
    public Result deleteDoctorById(@PathVariable("doctor_id") int doctor_id) {
        int flag = doctorService.deleteDoctorById(doctor_id);
        if (flag > 0) {
            return new Result(ResultCode.SUCCESS);
        }
        return new Result(ResultCode.FAIL);
    }

    @RequestMapping("/deleteUserById/{user_id}")
    public Result deleteUserById(@PathVariable("user_id") int user_id) {
        int flag = userService.deleteUserById(user_id);
        if (flag > 0) {
            return new Result(ResultCode.SUCCESS);
        }
        return new Result(ResultCode.FAIL);
    }

    //获取角色列表 带分页
    @RequestMapping("/getRoleList")
    public Result getRoleList(Integer page, Integer limit) {
        return adminService.getRoles(page, limit);
    }

    //删除一个角色
    @RequestMapping("/delRole/{roleId}")
    public Result delRole(@PathVariable("roleId") Long roleId) {
        int flag = adminService.delRole(roleId);
        if (flag > 0) {
            return new Result(ResultCode.SUCCESS);
        }
        return new Result(ResultCode.FAIL);
    }

    // 检查角色是否唯一
    @RequestMapping("/checkRoleName/{roleName}/{roleId}")
    public Result checkRoleName(@PathVariable("roleName") String roleName,
                                    @PathVariable("roleId") Long roleId) {
        Role role = adminService.getRoleByRoleName(roleName);
        if (role == null)
        {
            return new Result(ResultCode.SUCCESS);
        } else if (role.getRoleId() == roleId) //已经有这个角色名 并且就是这个id 也可以
        {
            return new Result(ResultCode.SUCCESS);
        } else  //此角色名已存在 别的roleId
        {
            return new Result(ResultCode.EXIST);
        }
    }

    // 检查角色是否唯一 添加新角色的时候用这个函数
    @RequestMapping("/checkAddRoleName/{roleName}")
    public Result checkRoleName(@PathVariable("roleName") String roleName) {
        Role role = adminService.getRoleByRoleName(roleName);
        if (role == null)//没有这个角色名 可以
        {
            return new Result(ResultCode.SUCCESS);
        } else  //此角色名已存在
        {
            return new Result(ResultCode.EXIST);
        }
    }

    //更新角色
    @RequestMapping("/updateRole")
    public void updateRole(Role role, String m) {
        adminService.updRole(role, m);
    }

    //添加角色
    @RequestMapping("/insRole")
    public Result insertRole(Role role, String m) {
        adminService.insRole(role, m);
        return new Result(ResultCode.SUCCESS);
    }

    //查看管理员的个人信息
    @RequestMapping("/personalData")
    public Result personalDate(String username, HttpSession session) {
        Admin admin = adminService.getAdminByUsername(username);
        session.setAttribute("admin", admin);
        return new Result(ResultCode.SUCCESS,admin);
    }

    @RequestMapping("/getAdminList")
    //获取所有管理员列表 带分页
    public Result getAdminList(Integer page, Integer limit) {
        // Tomcat Localhost Log 会输出错误信息 如果下面的sql语句有问题
        return adminService.getAdminList(page, limit);
    }

    //更新管理员
    @RequestMapping("/updateAdmin")
    public Result updateAdmin(Admin admin) {
        int flag = adminService.updAdmin(admin);
        if (flag > 0) {
            return new Result(ResultCode.SUCCESS);
        }
        return new Result(ResultCode.FAIL);
    }

    //修改密码
    @RequestMapping("/changeAdminPassword")
    public Result changeAdminPassword(String password, String newPassword, String username) {

        Admin admin = adminService.getAdminByUsername(username);
        if (admin != null) {
            if (admin.getPassword().equals(EncryptUtil.encrypt(password))) {
                admin.setPassword(EncryptUtil.encrypt(newPassword));
                int flag = adminService.updAdmin(admin);
                if (flag > 0) {
                    return new Result(ResultCode.SUCCESS);
                }
            } else {
                return new Result(ResultCode.USERNAMEPASSWORDERROR);
            }
        }
        return new Result(ResultCode.FAIL);
    }

    //删除一个管理员
    @RequestMapping("/delAdminById/{id}")
    public Result delAdminById(@PathVariable("id") Long id) {
        if (id == 1) {
            return new Result(ResultCode.FAIL);
        }
        int flag = adminService.delAdminById(id);
        if (flag > 0) {
            return new Result(ResultCode.SUCCESS);
        }
        return new Result(ResultCode.FAIL);

    }

    //检查是否已经存在此用户名
    @RequestMapping("/checkAdminName/{username}")
    public Result checkAdminName(@PathVariable("username") String username) {
        Admin admin = adminService.getAdminByUsername(username);
        if (admin != null) {
            return new Result(ResultCode.EXIST);
        }
        return new Result(ResultCode.SUCCESS);
    }

    //添加新管理员
    @RequestMapping("/insAdmin")
    public Result insAdmin(Admin admin) {
        int flag = adminService.insAdmin(admin);
        if (flag > 0) {
            return new Result(ResultCode.SUCCESS);
        }
        return new Result(ResultCode.FAIL);
    }

    @RequestMapping("/logout")
    public Result loginOut(HttpSession session)
            throws ParseException {

        Date loginTime = (Date) session.getAttribute("adminLoginTime");
        AdminLog adminLog = adminService.getAdminLogByLoginTime(loginTime);
        adminLog.setIsSafeExit(1);
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String nowTime = simpleDateFormat.format(date);
        Date logoutTime = simpleDateFormat.parse(nowTime);
        adminLog.setLogoutTime(logoutTime);
        adminService.updateAdminLog(adminLog);
        session.invalidate();
        return new Result(ResultCode.SUCCESS);
    }

}
