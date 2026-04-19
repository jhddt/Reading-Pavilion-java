package com.jhddt.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jhddt.common.util.JwtUtil;
import com.jhddt.config.security.UserDetailsServiceImpl;
import com.jhddt.common.enums.UserRoles;
import com.jhddt.common.enums.UserStatus;
import com.jhddt.module.essay.service.FileStorageService;
import com.jhddt.module.user.dto.AdminBatchImportResult;
import com.jhddt.module.user.dto.LoginRequest;
import com.jhddt.module.user.dto.UpdateCurrentUserRequest;
import com.jhddt.module.user.entity.UserEntity;
import com.jhddt.module.user.vo.LoginResponse;
import com.jhddt.module.user.mapper.UserMapper;
import com.jhddt.module.user.service.UserService;
import com.jhddt.module.user.vo.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final FileStorageService fileStorageService;

    @Override
    public boolean save(UserEntity entity) {
        // 保存用户时，对密码进行加密
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        return super.save(entity);
    }

    @Override
    public boolean updateById(UserEntity entity) {
        // 更新用户时，如果传了密码，则重新加密
        if (entity.getPassword() != null && !entity.getPassword().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        return super.updateById(entity);
    }

    @Override
    public UserEntity getByUserName(String userName) {
        return this.getOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserName, userName));
    }

    @Override
    public boolean existsByUserName(String userName) {
        return this.count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserName, userName)) > 0;
    }

    @Override
    public boolean existsByUserNameAndIdNot(String userName, Long id) {
        return this.count(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserName, userName)
                .ne(UserEntity::getId, id)) > 0;
    }

    /**
     * 用户登录（使用 Spring Security 标准认证流程）
     *
     * @param loginRequest 登录请求参数
     * @return 登录响应（包含 Token）
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("开始登录，用户名: {}", loginRequest.getUserName());
        try {
            // 1. 创建认证令牌（包含用户名和密码）
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUserName(),
                            loginRequest.getPassword()
                    );
            log.info("创建认证令牌成功");

            // 2. 调用 AuthenticationManager 进行认证
            // 它会自动调用 UserDetailsService.loadUserByUsername() 加载用户
            // 然后使用 PasswordEncoder.matches() 验证密码
            log.info("开始调用 AuthenticationManager 进行认证");
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            log.info("认证成功");

            // 3. 认证成功，获取用户名
            String username = authentication.getName();
            log.info("获取用户名: {}", username);

            // 4. 从数据库获取完整的用户信息（用于返回）
            UserEntity user = userDetailsService.getUserEntityByUsername(username);
            log.info("获取用户信息: {}", user);

            // 5. 生成 JWT Token
            String token = jwtUtil.generateToken(user.getId(), user.getUserName(), user.getRole());
            log.info("生成 JWT Token 成功");

            // 6. 返回登录响应
            return new LoginResponse(token, user.getId(), user.getUserName(), user.getRole());

        } catch (AuthenticationException e) {
            // 认证失败（用户名不存在或密码错误）
            log.error("认证失败: ", e);
            throw new RuntimeException("用户名或密码错误");
        } catch (Exception e) {
            log.error("登录过程发生异常: ", e);
            throw e;
        }
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(Long userId) {
        UserEntity user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String avatarPreviewUrl = null;
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
            avatarPreviewUrl = fileStorageService.getFileUrl(user.getAvatarUrl(), 604800);
        }

        return UserProfileResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .avatarPreviewUrl(avatarPreviewUrl)
                .status(user.getStatus() != null ? user.getStatus().getCode() : null)
                .avatarUpdateTime(user.getAvatarUpdateTime())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }

    @Override
    public void updateCurrentUserProfile(Long userId, UpdateCurrentUserRequest request) {
        UserEntity user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (request.getUserName() != null && !request.getUserName().trim().isEmpty()) {
            String newUserName = request.getUserName().trim();
            if (existsByUserNameAndIdNot(newUserName, userId)) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUserName(newUserName);
        }

        boolean wantsToChangePassword = request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty();
        if (wantsToChangePassword) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                throw new RuntimeException("修改密码时必须提供当前密码");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("当前密码不正确");
            }
            user.setPassword(request.getNewPassword().trim());
        }

        boolean success = this.updateById(user);
        if (!success) {
            throw new RuntimeException("修改个人信息失败");
        }
    }

    @Override
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        UserEntity user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("头像文件不能为空");
        }

        String oldAvatarPath = user.getAvatarUrl();
        String avatarPath = fileStorageService.storeImage(file, userId);
        user.setAvatarUrl(avatarPath);

        boolean success = this.updateById(user);
        if (!success) {
            throw new RuntimeException("头像保存失败");
        }

        if (oldAvatarPath != null && !oldAvatarPath.trim().isEmpty() && !oldAvatarPath.equals(avatarPath)) {
            try {
                fileStorageService.deleteFile(oldAvatarPath);
            } catch (Exception ignored) {
            }
        }

        return getCurrentUserProfile(userId);
    }

    @Override
    public AdminBatchImportResult batchImportUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("导入文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!originalName.endsWith(".csv")) {
            throw new RuntimeException("仅支持 CSV 文件导入");
        }

        int totalRows = 0;
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.toLowerCase().contains("username")) {
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                totalRows++;

                String[] columns = line.split(",", -1);
                if (columns.length < 3) {
                    errors.add("第" + lineNo + "行格式错误，至少需要3列：userName,password,role");
                    continue;
                }

                String userName = columns[0].trim();
                String password = columns[1].trim();
                String roleText = columns[2].trim();
                String statusText = columns.length >= 4 ? columns[3].trim() : "1";

                if (userName.isEmpty() || password.isEmpty()) {
                    errors.add("第" + lineNo + "行失败：用户名或密码为空");
                    continue;
                }
                if (existsByUserName(userName)) {
                    errors.add("第" + lineNo + "行失败：用户名已存在 - " + userName);
                    continue;
                }

                Integer role;
                try {
                    role = Integer.valueOf(roleText);
                    UserRoles.fromCode(role);
                } catch (Exception ex) {
                    errors.add("第" + lineNo + "行失败：角色非法（仅允许1/2/3）");
                    continue;
                }

                UserStatus status;
                try {
                    Integer statusCode = Integer.valueOf(statusText);
                    status = UserStatus.fromCode(statusCode);
                } catch (Exception ex) {
                    errors.add("第" + lineNo + "行失败：状态非法（仅允许0/1）");
                    continue;
                }

                UserEntity toCreate = UserEntity.builder()
                        .userName(userName)
                        .password(password)
                        .role(role)
                        .status(status)
                        .build();
                boolean ok = save(toCreate);
                if (ok) {
                    successCount++;
                } else {
                    errors.add("第" + lineNo + "行失败：数据库写入失败");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("批量导入失败: " + e.getMessage(), e);
        }

        return AdminBatchImportResult.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failCount(totalRows - successCount)
                .errors(errors)
                .build();
    }
}
