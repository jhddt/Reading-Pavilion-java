package com.jhddt.config.init;

import com.jhddt.common.enums.UserRoles;
import com.jhddt.common.enums.UserStatus;
import com.jhddt.module.user.entity.UserEntity;
import com.jhddt.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时确保默认管理员账号存在。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        UserEntity existing = userService.getByUserName(DEFAULT_ADMIN_USERNAME);
        if (existing != null) {
            log.info("默认管理员账号已存在，跳过创建: {}", DEFAULT_ADMIN_USERNAME);
            return;
        }

        UserEntity admin = UserEntity.builder()
                .userName(DEFAULT_ADMIN_USERNAME)
                .password(DEFAULT_ADMIN_PASSWORD)
                .role(UserRoles.ADMIN.getCode())
                .status(UserStatus.ENABLE)
                .build();

        boolean created = userService.save(admin);
        if (created) {
            log.warn("已自动创建默认管理员账号: {}，请尽快修改默认密码", DEFAULT_ADMIN_USERNAME);
        } else {
            log.error("默认管理员账号创建失败: {}", DEFAULT_ADMIN_USERNAME);
        }
    }
}
