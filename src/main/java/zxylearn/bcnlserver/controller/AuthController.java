package zxylearn.bcnlserver.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import zxylearn.bcnlserver.OAuth2.OAuth2Service;
import zxylearn.bcnlserver.pojo.DTO.LoginRequestDTO;
import zxylearn.bcnlserver.pojo.DTO.LoginYNURequestDTO;
import zxylearn.bcnlserver.pojo.DTO.RegisterRequestDTO;
import zxylearn.bcnlserver.pojo.entity.User;
import zxylearn.bcnlserver.redis.RedisService;
import zxylearn.bcnlserver.service.UserService;
import zxylearn.bcnlserver.utils.EmailUtil;
import zxylearn.bcnlserver.utils.JwtUtil;

@Tag(name = "权限模块")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisService redisService;

    @Autowired
    private OAuth2Service oAuth2Service;

    @Autowired
    private UserService userService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("/get-image-captcha")
    public ResponseEntity<?> getImageCaptcha() {
        // 生成图形验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 6, 20);
        String uuid = IdUtil.simpleUUID();
        String code = lineCaptcha.getCode();

        // 缓存图形验证码
        redisService.addImageCaptcha(uuid, code);

        String base64Img = lineCaptcha.getImageBase64Data();
        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("base64Img", base64Img);

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "获取邮箱验证码")
    @GetMapping("/get-email-captcha")
    public ResponseEntity<?> getEmailCaptcha(
            @RequestParam("email") String email) {

        // 检查邮箱是否已被注册
        if (userService.isExistEmail(email)) {
            return ResponseEntity.status(400).body(Map.of("error", "邮箱已被注册"));
        }

        // 检查验证码是否频繁发送
        Long ttl = redisService.getEmailCaptchaTTL(email);
        if (ttl != null && ttl != -2L) {
            return ResponseEntity.status(429).body(Map.of("ttl", ttl));
        }

        // 生成邮箱验证码
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int c = (int) (Math.random() * 10);
            code.append(c);
        }

        // 发送邮箱验证码
        String subject = "编程能力提升验证码";
        String text = "您的验证码是：" + code + "\n该验证码有效时间为5分钟，请勿泄露给他人";
        if (!emailUtil.send(email, subject, text)) {
            return ResponseEntity.status(400).body(Map.of("error", "验证码发送失败"));
        }

        // 缓存邮箱验证码
        redisService.addEmailCaptcha(email, code.toString());

        return ResponseEntity.ok("");
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO registerRequestDTO) {

        String username = registerRequestDTO.getUsername();
        String password = registerRequestDTO.getPassword();
        String email = registerRequestDTO.getEmail();
        String code = registerRequestDTO.getCode();

        // 检查用户名、邮箱是否存在
        if (userService.isExistUsername(username) || userService.isExistEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "用户名或邮箱已存在"));
        }

        // 验证邮箱验证码
        if (!redisService.verifyEmailCaptcha(email, code)) {
            return ResponseEntity.status(403).body(Map.of("error", "邮箱验证码错误"));
        }

        // 注册用户
        String passwordHash = passwordEncoder.encode(password);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setEmail(email);
        if (!userService.save(user)) {
            return ResponseEntity.status(500).body(Map.of("error", "注册失败"));
        }

        return ResponseEntity.ok("");
    }

    @Operation(summary = "登陆")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequestDTO) {

        String username = loginRequestDTO.getUsername();
        String password = loginRequestDTO.getPassword();
        String uuid = loginRequestDTO.getUuid();
        String code = loginRequestDTO.getCode();

        // 验证图形验证码
        if (!redisService.verifyImageCaptcha(uuid, code)) {
            return ResponseEntity.status(400).body(Map.of("error", "验证码错误"));
        }

        // 验证用户信息
        User user = userService.getUserByUsernameOrEmail(username);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId().toString(),
                user.getAdmin() == 0 ? JwtUtil.USER : JwtUtil.ADMIN);

        // 返回登陆信息
        Map<String, Object> result = new HashMap<>();
        user.setPasswordHash("---");
        result.put("user", user);
        result.put("token", token);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "通过YNU登陆")
    @PostMapping("/login-ynu")
    public ResponseEntity<?> loginYNU(@RequestBody LoginYNURequestDTO loginYNURequestDTO) {

        String username = loginYNURequestDTO.getUsername();
        String password = loginYNURequestDTO.getPassword();
        String uuid = loginYNURequestDTO.getUuid();
        String code = loginYNURequestDTO.getCode();

        String email = loginYNURequestDTO.getEmail();
        String emailCode = loginYNURequestDTO.getEmailCode();

        // 验证图形验证码
        if (!redisService.verifyImageCaptcha(uuid, code)) {
            return ResponseEntity.status(400).body(Map.of("error", "验证码错误"));
        }

        // 第三方验证
        Map<String, String> ynuInfo = oAuth2Service.loginYNU(username, password);;
        if (ynuInfo == null) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }

        // 验证是否是使用第三方第一次登陆
        User user = userService.getUserByUsernameOrEmail(username + "@YNU");
        if (user == null) {
            // 验证邮箱验证码
            if (email == null || email.isEmpty() || email.isBlank() ||
                emailCode == null || emailCode.isEmpty() || emailCode.isBlank()) {
                return ResponseEntity.status(422).body(Map.of("error", "请绑定邮箱"));
            }
            if (!redisService.verifyEmailCaptcha(email, emailCode)) {
                return ResponseEntity.status(403).body(Map.of("error", "邮箱验证码错误"));
            }

            // 注册用户
            user = new User();
            user.setUsername(username + "@YNU");
            user.setPasswordHash("---");
            user.setEmail(email);
            user.setName(ynuInfo.get("name"));
            user.setAdmin(0);
            if (!userService.save(user)) {
                return ResponseEntity.status(500).body(Map.of("error", "注册失败"));
            }
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId().toString(),
                user.getAdmin() == 0 ? JwtUtil.USER : JwtUtil.ADMIN);

        // 返回登陆信息
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("token", token);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.replace("Bearer ", "");
        if (token != null) {
            redisService.addTokenToBlackList(token);
        }
        return ResponseEntity.ok("");
    }
}
