package zxylearn.bcnlserver.service;


import com.baomidou.mybatisplus.extension.service.IService;

import zxylearn.bcnlserver.pojo.entity.TeamJoinApply;

public interface TeamJoinApplyService extends IService<TeamJoinApply> {
    public TeamJoinApply getTeamJoinApply(Long teamId, Long applicantId);
}
