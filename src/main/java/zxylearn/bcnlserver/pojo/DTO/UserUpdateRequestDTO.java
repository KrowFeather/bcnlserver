package zxylearn.bcnlserver.pojo.DTO;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequestDTO {
    private Long userId;
    private String name;
    private String avatar;
    private LocalDate birthday;
    private String phone;
    private Integer gender;
    private String address;

}
