package zxylearn.bcnlserver.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import zxylearn.bcnlserver.redis.RedisService;
import zxylearn.bcnlserver.utils.EmailUtil;


@Tag(name = "权限验证模块")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private RedisService redisService;


    @Operation(summary = "获取图形验证码")
    @GetMapping("/getImageCaptcha")
    public ResponseEntity<?> getImageCaptcha() {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 6, 20);
        String uuid = IdUtil.simpleUUID();
        String code = lineCaptcha.getCode();
        redisService.addImageCaptcha(uuid, code);

        String base64Img = lineCaptcha.getImageBase64Data();
        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("base64Img", base64Img);

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "获取邮箱验证码")
    @GetMapping("/getEmailCaptcha")
    public ResponseEntity<?> getEmailCaptcha(
        @RequestParam("email") String email) {

        ///////////////////////////////////////
        /// 检查邮箱是否已经被注册 TODO
        ///////////////////////////////////////
        
        // 检查验证码是否频繁发送
        Long ttl = redisService.getEmailCaptchaTTL(email);
        if(ttl != null && ttl != -2L) {
            return ResponseEntity.status(429).body(Map.of("ttl", ttl));
        }

        // 生成验证码
        StringBuilder code = new StringBuilder();
        for(int i = 0; i < 6; i++) {
            int c = (int)(Math.random() * 10);
            code.append(c);
        }

        // 发送验证码
        if(!sendEmailCaptcha(email, code.toString())) {
            return ResponseEntity.status(400).body(Map.of("error", "验证码发送失败"));
        }

        // 存储验证码
        redisService.addEmailCaptcha(email, code.toString());

        return ResponseEntity.ok("");
    }



    ///////////////////////////////////////
    /// 私有辅助方法
    ///////////////////////////////////////
    
    // 发送邮箱验证码
    private Boolean sendEmailCaptcha(String email, String code) {
        if(email == null || code == null) {
            return false;
        }
        String subject = "编程能力提升验证码";
        String text = "您的验证码是：" + code + "\n该验证码有效时间为5分钟，请勿泄露给他人";
        return emailUtil.send(email, subject, text);
    }
}
