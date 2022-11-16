package org.tei.gamecenter.play.service;

import org.springframework.stereotype.Service;
import org.tei.gamecenter.play.mapper.BotMapper;
import org.tei.gamecenter.play.pojo.Bot;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Service
public class BotServiceImpl implements IBotService {
    private final BotMapper botMapper;

    public BotServiceImpl(BotMapper botMapper) {
        this.botMapper = botMapper;
    }

    @Override
    public Bot addBot(Bot bot) {
        botMapper.insert(bot);
        return bot;
    }

    @Override
    public int updateBot(Bot bot) {
        return botMapper.updateById(bot);
    }

    @Override
    public int deleteBot(Integer botId) {
        return botMapper.deleteById(botId);
    }

    @Override
    public List<Bot> getBot(Map<String, String> params) {
        return null;
    }

    @Override
    public Bot getBotById(Serializable id) {
        return botMapper.selectById(id);
    }
}
