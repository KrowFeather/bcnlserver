package zxylearn.bcnlserver.pojo.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TweetSendRequestDTO {
    private Long teamId;
    private String title;
    private String content;
    private List<String> images;
}
