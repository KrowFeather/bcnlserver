package zxylearn.bcnlserver.pojo.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSearchRequestDTO {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String prefixName;
    private String suffixName;
    private Integer minAge;
    private Integer maxAge;
    private String phone;
    private Integer gender;
    private String address;
    private Integer admin;

    @NotNull
    private Boolean matchAll;

    @NotNull
    @Min(value = 0)
    private Long offset;

    @NotNull
    @Min(value = 1)
    @Max(value = 500)
    private Long size;
}
