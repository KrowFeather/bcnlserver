package zxylearn.bcnlserver.service.impl;

import zxylearn.bcnlserver.pojo.entity.TeamJoinApply;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import zxylearn.bcnlserver.mapper.TeamJoinApplyMapper;
import zxylearn.bcnlserver.service.TeamJoinApplyService;

@Service
public class TeamJoinApplyServiceImpl extends ServiceImpl<TeamJoinApplyMapper, TeamJoinApply> implements TeamJoinApplyService {

    @Override
    public TeamJoinApply getTeamJoinApply(Long teamId, Long applicantId) {
        if(teamId == null || applicantId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<TeamJoinApply>()
                .eq(TeamJoinApply::getTeamId, teamId)
                .eq(TeamJoinApply::getApplicantId, applicantId)
        );
    }
}
