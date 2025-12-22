package zxylearn.bcnlserver.pojo.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamCreateRequestDTO {
    private String name;
    private String banner;
    private String logo;
    private String description;
}
