package com.jhddt.module.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 文件实体类
 */
@Schema(description = "文件实体")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("file")
public class FileEntity {

    @Schema(description = "文件ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableId(value = "file_id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "上传者ID", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField("user_id")
    private Long userId;

    @Schema(description = "所属作文ID")
    @TableField("essay_id")
    private Long essayId;

    @Schema(description = "原始文件名", example = "作文.jpg")
    @TableField("file_name")
    private String fileName;

    @Schema(description = "文件类型", example = "image/jpeg")
    @TableField("file_type")
    private String fileType;

    @Schema(description = "存储路径", example = "essay/images/xxx.jpg")
    @TableField("file_path")
    private String filePath;

    @Schema(description = "文件大小（字节）", example = "102400")
    @TableField("file_size")
    private Long fileSize;

    @Schema(description = "上传时间", accessMode = Schema.AccessMode.READ_ONLY)
    @TableField(value = "upload_time", fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;

    @Schema(description = "逻辑删除标记", hidden = true)
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
