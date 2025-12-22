package zxylearn.bcnlserver.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("team_join_apply")
public class TeamJoinApply {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "applicant_id")
    private Long applicantId;

    @TableField(value = "team_id")
    private Long teamId;

    @TableField(value = "apply_time")
    private LocalDateTime applyTime;

    @TableField(value = "status")
    private Integer status; // 0 待审核 1 已通过 2 已拒绝
}
