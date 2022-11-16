package org.tei.gamecenter.play.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tei.gamecenter.play.mapper.UserMapper;
import org.tei.gamecenter.play.pojo.User;
import org.tei.gamecenter.play.pojo.UserDetailsImpl;
import org.tei.gamecenter.play.utils.JwtUtil;

import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

    private final AuthenticationManager authenticationManager;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;


    public UserServiceImpl(AuthenticationManager authenticationManager, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public User register(String username, String password) {
        User user = new User();
        user.setUsername(username);
        QueryWrapper<User> queryWrapper = new QueryWrapper();
        queryWrapper.eq("username", username);
        List<User> queryResult = userMapper.selectList(queryWrapper);
        if (!queryResult.isEmpty()) {
            return null;
        }
        user.setPassword(passwordEncoder.encode(password));
        userMapper.insert(user);
        return user;
    }

    /**
     * ログイン
     *
     * @param username
     * @param password
     * @return　JWTトークン
     */
    @Override
    public String login(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);

        // ログイン失敗なら、自動的に例外が発生する
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticate.getPrincipal();
        User user = loginUser.getUser();
        return JwtUtil.createJWT(user.getId().toString());
    }

    @Override
    public User me() {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        UserDetailsImpl loginUser = (UserDetailsImpl) authentication.getPrincipal();
        return loginUser.getUser();
    }

}
