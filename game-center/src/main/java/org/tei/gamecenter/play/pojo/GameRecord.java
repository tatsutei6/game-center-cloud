package org.tei.gamecenter.play.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_record")
public class GameRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @JsonProperty("aId")
    private Integer aId;
    @JsonProperty("aBotId")
    private Integer aBotId;
    @JsonProperty("aRow")
    private Integer aRow;
    @JsonProperty("aColumn")
    private Integer aColumn;
    @JsonProperty("bId")
    private Integer bId;
    @JsonProperty("bBotId")
    private Integer bBotId;
    @JsonProperty("bRow")
    private Integer bRow;
    @JsonProperty("bColumn")
    private Integer bColumn;
    @JsonProperty("aSteps")
    private String aSteps;
    @JsonProperty("bSteps")
    private String bSteps;
    private String map;
    private String loser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Tokyo")
    private Date createAt;
}
