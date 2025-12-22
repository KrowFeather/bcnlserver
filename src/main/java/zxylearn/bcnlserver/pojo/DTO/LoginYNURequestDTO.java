package zxylearn.bcnlserver.pojo.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginYNURequestDTO {
    private String username;
    private String password;
    private String uuid;
    private String code;

    // 第一次登陆需要邮箱验证码
    private String email;
    private String emailCode;
}
