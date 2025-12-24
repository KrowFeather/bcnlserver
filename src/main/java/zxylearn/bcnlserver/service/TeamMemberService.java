package zxylearn.bcnlserver.service;


import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import zxylearn.bcnlserver.pojo.DTO.TeamMemberVO;
import zxylearn.bcnlserver.pojo.entity.TeamMember;

public interface TeamMemberService extends IService<TeamMember> {
    public TeamMember getTeamMember(Long teamId, Long memberId);
    public List<TeamMemberVO> getTeamMemberList(Long teamId);
    public List<Long> getTeamIdsByMemberId(Long memberId);
}
