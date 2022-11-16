package org.tei.gamecenter.play.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.tei.gamecenter.play.pojo.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
