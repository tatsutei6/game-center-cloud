package org.tei.gamecenter.play.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.tei.gamecenter.play.mapper.GameRecordMapper;
import org.tei.gamecenter.play.mapper.UserMapper;
import org.tei.gamecenter.play.pojo.GameRecord;
import org.tei.gamecenter.play.pojo.User;
import org.tei.gamecenter.play.utils.Constants;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin
@RestController
@Slf4j
@RequestMapping("/play/record")
public class RecordController {
    private final GameRecordMapper gameRecordMapper;
    private final UserMapper userMapper;

    public RecordController(GameRecordMapper gameRecordMapper, UserMapper userMapper) {
        this.gameRecordMapper = gameRecordMapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/all/{page}")
    public RequestResult<JSONObject> my(@PathVariable("page") Integer page) {
        IPage<GameRecord> recordIPage = new Page<>(page, 10);
        QueryWrapper<GameRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        List<GameRecord> records = gameRecordMapper.selectPage(recordIPage, queryWrapper).getRecords();
        JSONObject response = new JSONObject();

        List<JSONObject> items = new ArrayList<>();


        for (GameRecord record : records) {
            User userA = userMapper.selectById(record.getAId());
            User userB = userMapper.selectById(record.getBId());
            JSONObject item = new JSONObject();
            item.put("content", record);
            JSONObject playerInfo = new JSONObject();

            playerInfo.put("aPhoto", userA.getAvatarUrl());
            playerInfo.put("aName", userA.getUsername());
            playerInfo.put("bPhoto", userB.getAvatarUrl());
            playerInfo.put("bName", userB.getUsername());
            String result = "引分";
            if ("A".equals(record.getLoser())) result = "B勝";
            else if ("B".equals(record.getLoser())) result = "A勝";
            playerInfo.put("result", result);
            item.put("playerInfo", playerInfo);
            items.add(item);
        }
        Long count = gameRecordMapper.selectCount(null);
        response.put("records", items);
        response.put("page", page);
        response.put("count", count);
        return new RequestResult<>(200, Constants.SUCCESS, response);
    }
}
