package zxylearn.bcnlserver.redis;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import zxylearn.bcnlserver.utils.JwtUtil;

@Service
public class RedisService {

    private static String JWT_BLACK = "jwt:black:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    // 记录token黑名单
    public boolean addTokenToBlackList(String token) {
        String tokenId = jwtUtil.getTokenId(token);
        Date expiration = jwtUtil.getTokenExpiration(token);
        if(tokenId == null || expiration == null) {
            return false;
        }

        long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        redisTemplate.opsForValue().set(JWT_BLACK + tokenId, "black", ttl, TimeUnit.SECONDS);
        return true;
    }

    // 判断token是否在黑名单
    public boolean isTokenInBlackList(String token) {
        String tokenId = jwtUtil.getTokenId(token);
        if(tokenId == null) {
            return false;
        }
        
        return redisTemplate.hasKey(JWT_BLACK + tokenId);
    }

    // 添加图形验证码
    public boolean addImageCaptcha(String uuid, String code) {
        if(uuid == null || code == null) {
            return false;
        }
        String key = "image_captcha:" + uuid;
        redisTemplate.opsForValue().set(key, code, 180, TimeUnit.SECONDS);
        return true;
    }

    // 验证图形验证码
    public boolean verifyImageCaptcha(String uuid, String code) {
        if(uuid == null || code == null) {
            return false;
        }
        String key = "image_captcha:" + uuid;
        String cachedCode = (String) redisTemplate.opsForValue().get(key);
        if(cachedCode == null) {
            return false;
        }
        boolean result = cachedCode.equalsIgnoreCase(code);
        if(result) {
            redisTemplate.delete(key);
        }
        return result;
    }

    // 添加邮箱验证码
    public boolean addEmailCaptcha(String email, String code) {
        if(email == null || code == null) {
            return false;
        }
        String key = "email_captcha:" + email;
        redisTemplate.opsForValue().set(key, code, 300, TimeUnit.SECONDS);
        return true;
    }

    // 获取邮箱验证码TTL
    public Long getEmailCaptchaTTL(String email) {
        if(email == null) {
            return -2L;
        }
        String key = "email_captcha:" + email;
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null ? ttl : -2L;
    }

    // 验证邮箱验证码
    public boolean verifyEmailCaptcha(String email, String code) {
        if(email == null || code == null) {
            return false;
        }
        String key = "email_captcha:" + email;
        String cachedCode = (String) redisTemplate.opsForValue().get(key);
        if(cachedCode == null) {
            return false;
        }
        boolean result = cachedCode.equalsIgnoreCase(code);
        if(result) {
            redisTemplate.delete(key);
        }
        return result;
    }

}
