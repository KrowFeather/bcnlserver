package zxylearn.bcnlserver.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import zxylearn.bcnlserver.common.UserContext;
import zxylearn.bcnlserver.pojo.DTO.TeamCreateRequestDTO;
import zxylearn.bcnlserver.pojo.DTO.TeamUpdateRequestDTO;
import zxylearn.bcnlserver.pojo.entity.Team;
import zxylearn.bcnlserver.pojo.entity.TeamJoinApply;
import zxylearn.bcnlserver.pojo.entity.TeamMember;
import zxylearn.bcnlserver.service.TeamJoinApplyService;
import zxylearn.bcnlserver.service.TeamMemberService;
import zxylearn.bcnlserver.service.TeamService;
import zxylearn.bcnlserver.utils.JwtUtil;

@Tag(name = "团队模块")
@RestController
@RequestMapping("/team")
@Transactional
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private TeamJoinApplyService teamJoinApplyService;


    @Operation(summary = "申请创建团队")
    @PostMapping("/apply-create")
    public ResponseEntity<?> applyCreateTeam(@RequestBody TeamCreateRequestDTO teamCreateRequestDTO) {

        Long userId = Long.parseLong(UserContext.getUserId());

        Team team = Team.builder()
                .name(teamCreateRequestDTO.getName())
                .banner(teamCreateRequestDTO.getBanner())
                .logo(teamCreateRequestDTO.getLogo())
                .description(teamCreateRequestDTO.getDescription())
                .ownerId(userId)
                .status(0) // 0 待审核
                .build();

        if(!teamService.save(team)) {
            return ResponseEntity.status(500).body(Map.of("error", "创建团队失败"));
        }

        return ResponseEntity.ok(Map.of("team", team));
    }

    @Operation(summary = "同意创建团队")
    @PostMapping("/approve-create")
    public ResponseEntity<?> approveCreateTeam(@RequestParam Long teamId) {

        String role = UserContext.getUserRole();
        if(!role.equals(JwtUtil.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        Team team = teamService.getById(teamId);
        if(team == null) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在"));
        }

        // 将创建者加入团队
        TeamMember teamMember = TeamMember.builder()
                .memberId(team.getOwnerId())
                .teamId(team.getId())
                .position("团队创建者")
                .joinTime(LocalDateTime.now())
                .build();

        if(!teamMemberService.save(teamMember)) {
            return ResponseEntity.status(500).body(Map.of("error", "添加团队创建者为成员失败"));
        }

        team.setStatus(1); // 1 已通过
        if(!teamService.updateById(team)) {
            return ResponseEntity.status(500).body(Map.of("error", "同意创建团队失败"));
        }

        return ResponseEntity.ok(Map.of("team", team));
    }

    @Operation(summary = "拒绝创建团队")
    @PostMapping("/reject-create")
    public ResponseEntity<?> rejectCreateTeam(@RequestParam Long teamId) {

        String role = UserContext.getUserRole();
        if(!role.equals(JwtUtil.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        Team team = teamService.getById(teamId);
        if(team == null) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在"));
        }

        team.setStatus(2); // 2 已拒绝
        if(!teamService.updateById(team)) {
            return ResponseEntity.status(500).body(Map.of("error", "拒绝创建团队失败"));
        }

        return ResponseEntity.ok(Map.of("team", team));
    }

    @Operation(summary = "删除团队")
    @PostMapping("/delete")
    public ResponseEntity<?> deleteTeam(@RequestParam Long teamId) {

        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        Team team = teamService.getById(teamId);
        if(team == null || !team.getStatus().equals(1)) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在"));
        }

        if(team.getStatus().equals(3)) {
            return ResponseEntity.status(400).body(Map.of("error", "团队已被删除"));
        }

        if(role.equals(JwtUtil.USER) && !userId.equals(team.getOwnerId())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        team.setStatus(3); // 3 已删除
        if(!teamService.updateById(team)) {
            return ResponseEntity.status(500).body(Map.of("error", "删除团队失败"));
        }

        return ResponseEntity.ok(Map.of("team", team));
    }

    @Operation(summary = "申请加入团队")
    @PostMapping("/apply-join")
    public ResponseEntity<?> applyJoinTeam(@RequestParam Long teamId) {

        Long userId = Long.parseLong(UserContext.getUserId());
        
        // 团队合法性检验
        Team team = teamService.getById(teamId);
        if(team == null || team.getStatus() != 1) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        // 已是成员检验
        if(teamMemberService.getTeamMember(team.getId(), userId) != null) {
            return ResponseEntity.status(400).body(Map.of("error", "已是团队成员"));
        }

        // 是否已提交申请检验
        TeamJoinApply teamJoinApply = teamJoinApplyService.getTeamJoinApply(teamId, userId);
        if(teamJoinApply != null && teamJoinApply.getStatus() == 0) {
            return ResponseEntity.status(400).body(Map.of("error", "已提交加入团队申请，正在审核中"));
        }

        teamJoinApply = TeamJoinApply.builder()
                .applicantId(userId)
                .teamId(teamId)
                .applyTime(LocalDateTime.now())
                .status(0) // 0 待审核
                .build();

        if(!teamJoinApplyService.save(teamJoinApply)) {
            return ResponseEntity.status(500).body(Map.of("error", "申请加入团队失败"));
        }

        return ResponseEntity.ok(Map.of("teamJoinApply", teamJoinApply));
    }

    @Operation(summary = "同意加入团队申请")
    @PostMapping("/approve-join-apply")
    public ResponseEntity<?> approveJoinTeamApplication(@RequestParam Long teamJoinApplyId) {
        
        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        // 申请合法性检验
        TeamJoinApply teamJoinApply = teamJoinApplyService.getById(teamJoinApplyId);
        if(teamJoinApply == null || teamJoinApply.getStatus() != 0) {
            return ResponseEntity.status(404).body(Map.of("error", "加入团队申请不存在或已被处理"));
        }

        // 已是成员检验
        if(teamMemberService.getTeamMember(teamJoinApply.getTeamId(), teamJoinApply.getApplicantId()) != null) {
            return ResponseEntity.status(400).body(Map.of("error", "已是团队成员"));
        }

        // 团队合法性检验
        Team team = teamService.getById(teamJoinApply.getTeamId());
        if(team == null || team.getStatus() != 1) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        if(role.equals(JwtUtil.USER) && !userId.equals(team.getOwnerId())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        // 更新申请状态
        teamJoinApply.setStatus(1); // 1 已同意
        if(!teamJoinApplyService.updateById(teamJoinApply)) {
            return ResponseEntity.status(500).body(Map.of("error", "更新加入团队申请状态失败"));
        }

        // 添加成员
        TeamMember teamMember = TeamMember.builder()
                .memberId(teamJoinApply.getApplicantId())
                .teamId(teamJoinApply.getTeamId())
                .position("普通成员")
                .joinTime(LocalDateTime.now())
                .build();

        if(!teamMemberService.save(teamMember)) {
            return ResponseEntity.status(500).body(Map.of("error", "添加团队成员失败"));
        }

        return ResponseEntity.ok(Map.of("teamJoinApply", teamJoinApply));
    }

    @Operation(summary = "拒绝加入团队申请")
    @PostMapping("/reject-join-apply")
    public ResponseEntity<?> rejectJoinTeamApplication(@RequestParam Long teamJoinApplyId) {

        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        // 申请合法性检验
        TeamJoinApply teamJoinApply = teamJoinApplyService.getById(teamJoinApplyId);
        if(teamJoinApply == null || teamJoinApply.getStatus() != 0) {
            return ResponseEntity.status(404).body(Map.of("error", "加入团队申请不存在或已被处理"));
        }

        // 团队合法性检验
        Team team = teamService.getById(teamJoinApply.getTeamId());
        if(team == null || team.getStatus() != 1) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        if(role.equals(JwtUtil.USER) && !userId.equals(team.getOwnerId())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        // 更新申请状态
        teamJoinApply.setStatus(2); // 2 已拒绝
        if(!teamJoinApplyService.updateById(teamJoinApply)) {
            return ResponseEntity.status(500).body(Map.of("error", "更新加入团队申请状态失败"));
        }

        return ResponseEntity.ok(Map.of("teamJoinApply", teamJoinApply));
    }

    @Operation(summary = "删除团队成员")
    @PostMapping("/remove-member")
    public ResponseEntity<?> removeTeamMember(@RequestParam Long teamId, @RequestParam Long memberId) {

        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        Team team = teamService.getById(teamId);
        if(team == null || team.getStatus() != 1) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        TeamMember teamMember = teamMemberService.getTeamMember(teamId, memberId);
        if(teamMember == null) {
            return ResponseEntity.status(404).body(Map.of("error", "团队成员不存在"));
        }

        if(team.getOwnerId().equals(memberId)) {
            return ResponseEntity.status(400).body(Map.of("error", "无法删除团队创建者"));
        }

        if(role.equals(JwtUtil.USER) && !userId.equals(team.getOwnerId())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        // 删除成员
        if(!teamMemberService.removeById(teamMember.getId())) {
            return ResponseEntity.status(500).body(Map.of("error", "删除团队成员失败"));
        }

        return ResponseEntity.ok(Map.of("teamMember", teamMember));
    }

    @Operation(summary = "退出团队")
    @PostMapping("/leave")
    public ResponseEntity<?> leaveTeam(@RequestParam Long teamId) {

        Long userId = Long.parseLong(UserContext.getUserId());

        Team team = teamService.getById(teamId);
        if(team == null || team.getStatus() != 1) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        TeamMember teamMember = teamMemberService.getTeamMember(teamId, userId);
        if(teamMember == null) {
            return ResponseEntity.status(404).body(Map.of("error", "你不是团队成员"));
        }

        // 退出团队
        if(!teamMemberService.removeById(teamMember.getId())) {
            return ResponseEntity.status(500).body(Map.of("error", "退出团队失败"));
        }

        return ResponseEntity.ok(Map.of("teamMember", teamMember));
    }

    @Operation(summary = "更新团队信息")
    @PutMapping("/update-info")
    public ResponseEntity<?> updateTeamInfo(@RequestBody TeamUpdateRequestDTO teamUpdateRequestDTO) {

        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        Team team = teamService.getById(teamUpdateRequestDTO.getId());
        if(team == null || !team.getStatus().equals(1)) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        if(role.equals(JwtUtil.USER) && !userId.equals(team.getOwnerId())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        team.setName(teamUpdateRequestDTO.getName() != null && !teamUpdateRequestDTO.getName().isEmpty() ? teamUpdateRequestDTO.getName() : team.getName());
        team.setBanner(teamUpdateRequestDTO.getBanner() != null && !teamUpdateRequestDTO.getBanner().isEmpty() ? teamUpdateRequestDTO.getBanner() : team.getBanner());
        team.setLogo(teamUpdateRequestDTO.getLogo() != null && !teamUpdateRequestDTO.getLogo().isEmpty() ? teamUpdateRequestDTO.getLogo() : team.getLogo());
        team.setDescription(teamUpdateRequestDTO.getDescription() != null && !teamUpdateRequestDTO.getDescription().isEmpty() ? teamUpdateRequestDTO.getDescription() : team.getDescription());

        if(!teamService.updateById(team)) {
            return ResponseEntity.status(500).body(Map.of("error", "更新团队信息失败"));
        }


        return ResponseEntity.ok(Map.of("team", team));
    }

    @Operation(summary = "更新团队成员职务")
    @PutMapping("/update-member-position")
    public ResponseEntity<?> updateTeamMemberPosition(@RequestParam Long teamId, @RequestParam Long memberId, @RequestParam String position) {

        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        Team team = teamService.getById(teamId);
        if(team == null || team.getStatus() != 1) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        if(role.equals(JwtUtil.USER) && !userId.equals(team.getOwnerId())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        TeamMember teamMember = teamMemberService.getTeamMember(teamId, memberId);
        if(teamMember == null) {
            return ResponseEntity.status(404).body(Map.of("error", "团队成员不存在"));
        }

        teamMember.setPosition(position);
        if(!teamMemberService.updateById(teamMember)) {
            return ResponseEntity.status(500).body(Map.of("error", "更新团队成员职务失败"));
        }

        return ResponseEntity.ok(Map.of("teamMember", teamMember));
    }

    @Operation(summary = "获取团队列表")
    @PostMapping("/get-list")
    public ResponseEntity<?> getTeamList(@RequestParam(required = false) Integer status) {
        return ResponseEntity.ok(Map.of("teamList", teamService.getTeamListByStatus(status)));
    }

    @Operation(summary = "获取团队信息")
    @PostMapping("/get-info")
    public ResponseEntity<?> getTeamInfo(@RequestParam Long teamId) {
        Team team = teamService.getById(teamId);
        if(team == null || !team.getStatus().equals(1)) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        return ResponseEntity.ok(Map.of("team", team));
    }

    @Operation(summary = "获取团队成员列表")
    @PostMapping("/get-members")
    public ResponseEntity<?> getTeamMembers(@RequestParam Long teamId) {

        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        // 检查是否是团队成员
        if(role.equals(JwtUtil.USER) && teamMemberService.getTeamMember(teamId, userId) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        return ResponseEntity.ok(Map.of("teamMemberList", teamMemberService.getTeamMemberList(teamId)));
    }

    @Operation(summary = "获取团队加入申请列表")
    @PostMapping("/get-join-applications")
    public ResponseEntity<?> getTeamJoinApplications(@RequestParam Long teamId) {
        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        Team team = teamService.getById(teamId);
        if(team == null || team.getStatus() != 1) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在或已被删除"));
        }

        if(role.equals(JwtUtil.USER) && !userId.equals(team.getOwnerId())) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        return ResponseEntity.ok(Map.of("teamJoinApplyList", teamJoinApplyService.getTeamJoinApplyList(teamId)));
    }

    @Operation(summary = "获取当前用户已加入的团队列表")
    @PostMapping("/get-my-teams")
    public ResponseEntity<?> getMyTeams() {
        Long userId = Long.parseLong(UserContext.getUserId());
        List<Long> teamIds = teamMemberService.getTeamIdsByMemberId(userId);
        List<Team> teams = teamIds.stream()
                .map(teamId -> teamService.getById(teamId))
                .filter(team -> team != null && team.getStatus() == 1)
                .toList();
        return ResponseEntity.ok(Map.of("teamList", teams));
    }
}
