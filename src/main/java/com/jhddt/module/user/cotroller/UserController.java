package com.jhddt.module.user.cotroller;

import com.jhddt.common.result.Result;
import com.jhddt.module.user.dto.LoginRequest;
import com.jhddt.module.user.entity.UserEntity;
import com.jhddt.module.user.vo.LoginResponse;
import com.jhddt.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户管理", description = "用户注册、登录、信息管理等接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "用户登录", 
            description = "使用用户名和密码登录系统，登录成功后返回 JWT Token，后续请求需在 Header 中携带 Token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "登录成功，返回用户信息和 Token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "操作成功",
                                      "data": {
                                        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                        "userId": 1,
                                        "userName": "zhangsan",
                                        "realName": "张三",
                                        "role": 1
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "登录失败，用户名或密码错误")
    })
    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public Result<LoginResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "登录请求参数",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "userName": "zhangsan",
                                      "password": "123456"
                                    }
                                    """)
                    )
            )
            @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = userService.login(loginRequest);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(
            summary = "根据ID查询用户", 
            description = "根据用户ID查询用户详细信息，包括用户名、真实姓名、角色等"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = UserEntity.class))
            ),
            @ApiResponse(responseCode = "500", description = "用户不存在")
    })
    @GetMapping("/{id}")
    public Result<UserEntity> getUser(
            @Parameter(description = "用户ID", required = true, example = "1") 
            @PathVariable Long id) {
        UserEntity user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @Operation(
            summary = "添加用户（注册）", 
            description = "注册新用户，密码会自动使用 BCrypt 加密存储。用户名必须唯一，不能重复"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "500", description = "注册失败，用户名已存在或其他错误")
    })
    @PostMapping(value = "/add", consumes = "application/json", produces = "application/json")
    public Result<Void> addUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户信息（id、createTime、updateTime、status 由系统自动生成，无需填写）",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserEntity.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "userName": "lisi",
                                      "password": "123456",
                                      "realName": "李四",
                                      "role": 1
                                    }
                                    """)
                    )
            )
            @RequestBody UserEntity user) {
        if (userService.existsByUserName(user.getUserName())) {
            return Result.error("用户名已存在");
        }
        boolean success = userService.save(user);
        if (success) {
            return Result.success();
        }
        return Result.error("添加用户失败");
    }

    @Operation(
            summary = "删除用户", 
            description = "根据用户ID逻辑删除用户（软删除，数据仍保留在数据库中）"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID", required = true, example = "1") 
            @PathVariable Long id) {
        boolean success = userService.removeById(id);
        if (success) {
            return Result.success();
        }
        return Result.error("删除用户失败");
    }

    @Operation(
            summary = "修改用户信息", 
            description = "根据用户ID修改用户信息。如果修改用户名，会检查新用户名是否已被其他用户使用"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "500", description = "修改失败，用户名已存在或其他错误")
    })
    @PutMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
    public Result<Void> updateUser(
            @Parameter(description = "用户ID", required = true, example = "1") 
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户信息（id、createTime、updateTime 无需填写）",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserEntity.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "userName": "zhangsan",
                                      "realName": "张三三",
                                      "role": 1,
                                      "status": 0
                                    }
                                    """)
                    )
            )
            @RequestBody UserEntity user) {
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

    @Operation(
            summary = "查询所有用户", 
            description = "查询系统中所有用户列表（不包括已删除的用户）"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", 
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = UserEntity.class))
            )
    })
    @GetMapping("/selectAll")
    public Result<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.list();
        return Result.success(users);
    }
}
