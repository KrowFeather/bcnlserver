package zxylearn.bcnlserver.service.impl;

import zxylearn.bcnlserver.pojo.entity.TeamMember;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import zxylearn.bcnlserver.mapper.TeamMemberMapper;
import zxylearn.bcnlserver.service.TeamMemberService;

@Service
public class TeamMemberServiceImpl extends ServiceImpl<TeamMemberMapper, TeamMember> implements TeamMemberService {

    @Override
    public TeamMember getTeamMember(Long teamId, Long memberId) {
        if(teamId == null || memberId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getMemberId, memberId));
    }
}
