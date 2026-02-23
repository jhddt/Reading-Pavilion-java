package com.jhddt.cotroller;

import com.jhddt.common.Result;
import com.jhddt.domain.entity.UserEntity;
import com.jhddt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //根据id查询用户信息
    @GetMapping("/{id}")
    public Result<UserEntity> getUser(@PathVariable Long id) {
        UserEntity user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    //添加用户信息
    @PostMapping("/add")
    public Result<Void> addUser(@RequestBody UserEntity user) {
        if (userService.existsByUserName(user.getUserName())) {
            return Result.error("用户名已存在");
        }
        boolean success = userService.save(user);
        if (success) {
            return Result.success();
        }
        return Result.error("添加用户失败");
    }

    //根据id删除用户信息
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        if (success) {
            return Result.success();
        }
        return Result.error("删除用户失败");
    }

    //根据id修改用户信息
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody UserEntity user) {
        if (userService.existsByUserNameAndIdNot(user.getUserName(), id)) {
            return Result.error("用户名已存在");
        }
        user.setId(id);
        boolean success = userService.updateById(user);
        if (success) {
            return Result.success();
        }
        return Result.error("修改用户失败");
    }

    //查询全部用户信息
    @GetMapping("/selectAll")
    public Result<java.util.List<UserEntity>> getAllUsers() {
        java.util.List<UserEntity> users = userService.list();
        return Result.success(users);
    }
}