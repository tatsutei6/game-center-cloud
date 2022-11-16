package org.tei.botrunner.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotPoolElement {
    Integer userId;
    String botCode;
    String input;
}
