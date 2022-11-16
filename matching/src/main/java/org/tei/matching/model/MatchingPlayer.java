package org.tei.matching.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchingPlayer {
    private Integer userId;
    private Integer score;
    // 等待时间
    private Integer waitingTime;
    private Integer botId;
}
