package zxylearn.bcnlserver.service;


import com.baomidou.mybatisplus.extension.service.IService;

import zxylearn.bcnlserver.pojo.entity.TeamMember;

public interface TeamMemberService extends IService<TeamMember> {
    public TeamMember getTeamMember(Long teamId, Long memberId);
}
