package com.jhddt.module.user.cotroller;

import com.jhddt.common.security.CurrentUser;
import com.jhddt.common.enums.UserRoles;
import com.jhddt.common.enums.UserStatus;
import com.jhddt.common.result.Result;
import com.jhddt.module.user.dto.LoginRequest;
import com.jhddt.module.user.dto.AdminBatchImportResult;
import com.jhddt.module.user.dto.UpdateCurrentUserRequest;
import com.jhddt.module.user.entity.UserEntity;
import com.jhddt.module.user.vo.LoginResponse;
import com.jhddt.module.user.vo.UserProfileResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "用户管理", description = "用户注册、登录、信息管理等接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

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
        LoginResponse response = userService.login(loginRequest);
        return Result.success(response);
    }

    @Operation(
            summary = "查询当前登录用户信息",
            description = "根据当前请求中的 JWT Token 查询当前登录用户的个人信息"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "500", description = "用户不存在")
    })
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<UserProfileResponse> getCurrentUser(Authentication authentication) {
        Long userId = currentUser.id(authentication);
        UserProfileResponse profile = userService.getCurrentUserProfile(userId);
        return Result.success(profile);
    }

    @Operation(
            summary = "修改当前登录用户个人信息",
            description = "当前登录用户可修改自己的用户名，并在校验当前密码后修改密码；头像请通过上传接口单独处理"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "500", description = "修改失败，用户名已存在、当前密码错误等")
    })
    @PutMapping(value = "/me", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<Void> updateCurrentUser(
            Authentication authentication,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "个人信息修改请求。修改密码时需要同时传 currentPassword 和 newPassword",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateCurrentUserRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "userName": "zck_new",
                                      "currentPassword": "123456",
                                      "newPassword": "654321"
                                    }
                                    """)
                    )
            )
            @RequestBody UpdateCurrentUserRequest request) {
        Long userId = currentUser.id(authentication);
        userService.updateCurrentUserProfile(userId, request);
        return Result.success();
    }

    @Operation(
            summary = "上传当前登录用户头像",
            description = "上传头像图片文件，后端保存文件地址到 avatar_url，并返回更新后的用户信息"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 Token 无效"),
            @ApiResponse(responseCode = "500", description = "上传失败，文件为空或存储失败")
    })
    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data", produces = "application/json")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public Result<UserProfileResponse> uploadCurrentUserAvatar(
            Authentication authentication,
            @Parameter(description = "头像图片文件", required = true)
            @RequestParam("file") MultipartFile file) {
        Long userId = currentUser.id(authentication);
        UserProfileResponse profile = userService.uploadAvatar(userId, file);
        return Result.success("头像上传成功", profile);
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
    @PreAuthorize("hasRole('ADMIN')")
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
        if (user == null || user.getUserName() == null || user.getUserName().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            return Result.error("用户名和密码不能为空");
        }
        if (userService.existsByUserName(user.getUserName())) {
            return Result.error("用户名已存在");
        }
        // 公开注册接口只允许创建学生账号，避免越权注册教师/管理员
        UserEntity toCreate = UserEntity.builder()
                .userName(user.getUserName().trim())
                .password(user.getPassword())
                .role(UserRoles.STUDENT.getCode())
                .status(UserStatus.ENABLE)
                .build();
        boolean success = userService.save(toCreate);
        if (success) {
            return Result.success();
        }
        return Result.error("添加用户失败");
    }

    @Operation(summary = "管理员创建用户", description = "管理员可创建学生/教师/管理员账号")
    @PostMapping(value = "/admin/add", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> addUserByAdmin(@RequestBody UserEntity user) {
        if (user == null || user.getUserName() == null || user.getUserName().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            return Result.error("用户名和密码不能为空");
        }
        if (user.getRole() == null) {
            return Result.error("角色不能为空");
        }
        try {
            UserRoles.fromCode(user.getRole());
        } catch (IllegalArgumentException ex) {
            return Result.error("角色非法");
        }
        if (userService.existsByUserName(user.getUserName())) {
            return Result.error("用户名已存在");
        }
        UserEntity toCreate = UserEntity.builder()
                .userName(user.getUserName().trim())
                .password(user.getPassword())
                .role(user.getRole())
                .status(user.getStatus() == null ? UserStatus.ENABLE : user.getStatus())
                .build();
        boolean success = userService.save(toCreate);
        return success ? Result.success() : Result.error("添加用户失败");
    }

    @Operation(summary = "管理员批量导入用户", description = "通过 CSV 批量导入用户，列：userName,password,role,status")
    @PostMapping(value = "/admin/import", consumes = "multipart/form-data", produces = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AdminBatchImportResult> importUsersByAdmin(
            @Parameter(description = "CSV 文件（UTF-8）", required = true)
            @RequestParam("file") MultipartFile file) {
        return Result.success("导入完成", userService.batchImportUsers(file));
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.list();
        return Result.success(users);
    }
}
