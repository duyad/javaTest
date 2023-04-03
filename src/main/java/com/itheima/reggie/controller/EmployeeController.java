package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 登录
     *
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {

        //1、将页面提交的密码password进行md5加密处
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2.根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //3.如果没有查询到则返回登灵失败结果
        if (emp == null) {
            return R.error(("登录失败！"));
        }

        //4.密码比对，如果不一致则返回登录失败结果
        if (!emp.getPassword().equals(password)) {
            return R.error("登录失败！");
        }

        //5、查看员工状态，如果为已蔡用状态，则返回员工已禁用结果
        if (emp.getStatus() == 0) {
            return R.error("该账户已被禁用！");
        }

        //6、登录成功，将员工id存入Session并返回登录成功结果
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp, "登录成功！");
    }

    /**
     * 登出
     *
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("employee");
        return R.success(null, "登出成功！");
    }


    /**
     * 新增
     * @param request
     * @param employee
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);
        employeeService.save(employee);
        return R.success(null, "新增成功！");

    }

    /**
     * 查询列表
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/list")
    public R<Page> page(int page, int pageSize, String name) {
        log.info("page={},pageSize = {},name={}", page, pageSize, name);
        //分页构造器
        Page pageInfo = new Page(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper();
        //添加一个过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        //添加排序
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        pageInfo = employeeService.page(pageInfo, queryWrapper);
        return R.success(pageInfo, null);
    }

    /**
     * 修改员工账号禁用状态
     *
     * @param request
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> updata(HttpServletRequest request, @RequestBody Employee employee) {
        Long empId = (Long) request.getSession().getAttribute("employee");
        employee.setUpdateUser(empId);
        employee.setUpdateTime(LocalDateTime.now());
        employeeService.updateById(employee);
        return R.success(null, "修改成功");
    }

    /**
     * 查看详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        if (employee != null) {
            return R.success(employee, null);
        }
        return R.error("没有查询到对应信息");
    }

    /**
     * 删除
     * @param request
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public R<String> delete(HttpServletRequest request, @PathVariable("id") Long id) {
        Long empId = (Long) request.getSession().getAttribute("employee");
        Employee employee = employeeService.getById(id);
        employee.setUpdateUser(empId);
        employee.setUpdateTime(LocalDateTime.now());
        employeeService.removeById(employee.getId());
        return R.success(null, "删除成功!");
    }


}
