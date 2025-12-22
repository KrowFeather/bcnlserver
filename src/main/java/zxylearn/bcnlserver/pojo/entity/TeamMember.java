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
@TableName("team_member")
public class TeamMember {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "team_id")
    private Long teamId;

    @TableField(value = "member_id")
    private Long memberId;

    @TableField(value = "position")
    private String position;

    @TableField(value = "join_time")
    private LocalDateTime joinTime;
}
