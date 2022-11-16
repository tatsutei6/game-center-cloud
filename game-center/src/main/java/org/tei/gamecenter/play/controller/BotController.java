package org.tei.gamecenter.play.controller;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.tei.gamecenter.play.mapper.BotMapper;
import org.tei.gamecenter.play.pojo.Bot;
import org.tei.gamecenter.play.utils.Constants;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@CrossOrigin
@RestController
@RequestMapping("/play/bot")
@Slf4j
public class BotController extends BaseController {
    private final BotMapper botMapper;

    public BotController(BotMapper botMapper) {
        this.botMapper = botMapper;
    }

    @PostMapping("/add")
    public RequestResult<Bot> add(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String description = request.get("description");
        String code = request.get("code");

        Integer userId = currentUserId();

        QueryWrapper<Bot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        Long botCount = botMapper.selectCount(queryWrapper);
        if (botCount >= 10) {
            return new RequestResult<>(200, "Bot数の限度を超えました", null);
        }

        if (title == null || title.length() == 0) {
            return new RequestResult<>(200, "タイトルは空です", null);
        }

        if (title.length() > 100) {
            return new RequestResult<>(200, "タイトルの長さの限度(100)を超えました", null);
        }

        if (description == null || description.length() == 0) {
            description = "これは私の対戦Botコードです！";
        }

        if (description.length() > 200) {
            return new RequestResult<>(200, "タイトルの長さの限度(200)を超えました", null);
        }

        if (code == null || code.length() == 0) {
            return new RequestResult<>(200, "Bot Codeは空です", null);

        }

        if (code.length() > 10000) {
            return new RequestResult<>(200, "Bot Codeの限度(10000)を超えました", null);
        }


        Bot bot = new Bot();
        bot.setTitle(title);
        bot.setDescription(description);
        bot.setCode(code);
        bot.setUserId(userId);
        Date now = new Date();
        bot.setCreateAt(now);
        bot.setUpdateAt(now);
        int result = botMapper.insert(bot);
        if (result < 1) {
            return new RequestResult<>(200, "Botを追加できませんでした", null);
        }
        return new RequestResult<>(200, Constants.SUCCESS, bot);
    }

    @PostMapping("/delete/{id}")
    public RequestResult<Bot> delete(@PathVariable("id") Integer id) {
        if (id == null || id <= 0) {
            return new RequestResult<>(200, "idは空です", null);
        }

        Integer userId = currentUserId();

        Bot bot = botMapper.selectById(id);
        if (bot == null) {
            return new RequestResult<>(200, "Botが見つかりません", null);
        }
        if (!Objects.equals(bot.getUserId(), userId)) {
            return new RequestResult<>(200, "削除権限がありません", null);
        }

        int result = botMapper.deleteById(id);

        if (result < 1) {
            return new RequestResult<>(200, "削除に失敗しました", null);
        }
        return new RequestResult<>(200, Constants.SUCCESS, null);
    }

    @PostMapping("/update/{id}")
    public RequestResult<Bot> update(@PathVariable("id") Integer id, @RequestBody Map<String, String> request) {
        String title = request.get("title");
        if (title == null || title.length() == 0) {
            return new RequestResult<>(200, "タイトルは空です", null);
        }
        String description = request.get("description");
        if (description == null || description.length() == 0) {
            return new RequestResult<>(200, "Descriptionは空です", null);
        }

        String code = request.get("code");
        if (code == null || code.length() == 0) {
            return new RequestResult<>(200, "Bot Codeは空です", null);
        }

        Bot bot = botMapper.selectById(id);
        if (bot == null) {
            return new RequestResult<>(200, "Botが見つかりません", null);
        }
        Integer userId = currentUserId();
        if (!Objects.equals(bot.getUserId(), userId)) {
            return new RequestResult<>(200, "更新権限がありません", null);
        }
        bot.setTitle(title);
        bot.setCode(code);
        bot.setDescription(description);
        bot.setUpdateAt(new Date());
        int result = botMapper.updateById(bot);
        if (result < 1) {
            return new RequestResult<>(200, "更新に失敗しました", null);
        }
        return new RequestResult<>(200, Constants.SUCCESS, bot);
    }

    @GetMapping("/my")
    public RequestResult<List<Bot>> getMyBotList() {
        Integer userId = currentUserId();
        QueryWrapper<Bot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<Bot> bots = botMapper.selectList(queryWrapper);
        return new RequestResult<>(200, Constants.SUCCESS, bots);
    }
}
