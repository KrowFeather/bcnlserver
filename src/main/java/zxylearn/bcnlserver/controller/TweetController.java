package zxylearn.bcnlserver.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import zxylearn.bcnlserver.ES.TweetDocService;
import zxylearn.bcnlserver.OSS.OssService;
import zxylearn.bcnlserver.common.UserContext;
import zxylearn.bcnlserver.hutool.IdGeneratorService;
import zxylearn.bcnlserver.pojo.DTO.TweetSendRequestDTO;
import zxylearn.bcnlserver.pojo.document.TweetDoc;
import zxylearn.bcnlserver.pojo.entity.Team;
import zxylearn.bcnlserver.service.TeamMemberService;
import zxylearn.bcnlserver.service.TeamService;
import zxylearn.bcnlserver.utils.JwtUtil;

@Tag(name = "推文模块")
@RestController
@RequestMapping("/tweet")
public class TweetController {

    @Autowired
    private TweetDocService tweetDocService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private IdGeneratorService idGeneratorService;

    @Autowired
    private OssService ossService;

    @Operation(summary = "发送推文")
    @PostMapping("/send")
    public ResponseEntity<?> sendTweet(@RequestBody TweetSendRequestDTO tweetSendRequestDTO) {

        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        // 判断推文发送合法性
        if(role.equals(JwtUtil.USER) && teamMemberService.getTeamMember(tweetSendRequestDTO.getTeamId(), userId) == null) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        // 管理员插入判断团队是否存在
        if(role.equals(JwtUtil.ADMIN) && teamService.getById(tweetSendRequestDTO.getTeamId()) == null) {
            return ResponseEntity.status(404).body(Map.of("error", "团队不存在"));
        }

        // 创建推文
        TweetDoc tweetDoc = TweetDoc.builder()
                .id(idGeneratorService.getId())
                .teamId(tweetSendRequestDTO.getTeamId())
                .senderId(userId)
                .createTime(LocalDateTime.now())
                .title(tweetSendRequestDTO.getTitle())
                .content(tweetSendRequestDTO.getContent())
                .images(tweetSendRequestDTO.getImages())
                .build();
        if(!tweetDocService.createTweet(tweetDoc)) {
            return ResponseEntity.status(500).body(Map.of("error", "推文发送失败"));
        }

        return ResponseEntity.ok(Map.of("tweet", tweetDoc));
    }

    @Operation(summary = "删除推文")
    @PostMapping("/delete")
    public ResponseEntity<?> deleteTweet(@RequestParam Long tweetId) {

        Long userId = Long.parseLong(UserContext.getUserId());
        String role = UserContext.getUserRole();

        // 判断推文删除合法性
        TweetDoc tweetDoc = tweetDocService.getTweetById(tweetId);
        if(tweetDoc == null) {
            return ResponseEntity.status(404).body(Map.of("error", "推文不存在"));
        }
        Team team = teamService.getById(tweetDoc.getTeamId());
        if(role.equals(JwtUtil.USER) && !tweetDoc.getSenderId().equals(userId) && !team.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作"));
        }

        // 删除推文
        if(!tweetDocService.removeTweet(tweetId)) {
            return ResponseEntity.status(500).body(Map.of("error", "推文删除失败"));
        }

        // 删除OSS图片
        for(String imageUrl: tweetDoc.getImages()) {
            ossService.deleteFile(imageUrl);
        }

        return ResponseEntity.ok(Map.of("tweet", tweetDoc));
    }

    @Operation(summary = "查询推文")
    @GetMapping("/search")
    public ResponseEntity<?> searchTweet(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long teamId) {

        return ResponseEntity.ok(Map.of("tweetList", tweetDocService.searchTweet(keyword, teamId)));
    }

}
