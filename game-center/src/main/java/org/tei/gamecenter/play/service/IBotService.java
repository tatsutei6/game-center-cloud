package org.tei.gamecenter.play.service;

import org.tei.gamecenter.play.pojo.Bot;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface IBotService {
    Bot addBot(Bot bot);
    int updateBot(Bot bot);
    int deleteBot(Integer botId);
    List<Bot> getBot(Map<String,String> params);

    Bot getBotById(Serializable id);
}
