package org.tei.gamecenter.play.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.tei.gamecenter.play.mapper.UserMapper;
import org.tei.gamecenter.play.pojo.User;
import org.tei.gamecenter.play.utils.Constants;
import org.tei.gamecenter.play.utils.JwtUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@CrossOrigin
@RestController
@RequestMapping("/play/user")
public class UserController extends BaseController {
    private final UserMapper userMapper;

    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder;


    public UserController(UserMapper userMapper, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public RequestResult<String> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        if (username == null || password == null) {
            return new RequestResult<>(200, "ユーザー名かパスワードは空です", null);
        }
        username = username.trim();
        if ("".equals(username) || "".equals(password)) {
            return new RequestResult<>(200, "ユーザー名かパスワードは空です", null);
        }

        User user = getUserByUsernameAndPassword(authenticationManager, username, password);
        String jwt = JwtUtil.createJWT(user.getId().toString());
        return new RequestResult<>(200, Constants.SUCCESS, jwt);
    }


    @GetMapping("/me")
    public RequestResult<User> me() {
        return new RequestResult<>(200, Constants.SUCCESS, currentUser());
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Integer id) {
        return userMapper.selectById(id);
    }

    @PostMapping("/register")
    public RequestResult<User> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String confirmPassword = request.get("confirmPassword");
        if (username == null || password == null || confirmPassword == null) {
            return new RequestResult<>(200, "ユーザー名かパスワードは空です", null);
        }

        username = username.trim();
        if ("".equals(username) || "".equals(password) || "".equals(confirmPassword)) {
            return new RequestResult<>(200, "ユーザー名かパスワードは空です", null);
        }
        if (username.length() > 30 || password.length() > 30 || confirmPassword.length() > 30) {
            return new RequestResult<>(200, "ユーザー名かパスワードの長さは限度を超えた", null);
        }

        if (!Objects.equals(password, confirmPassword)) {
            return new RequestResult<>(200, "確認パスワードが一致しない", null);
        }
        User user = new User();
        user.setUsername(username);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        List<User> queryResult = userMapper.selectList(queryWrapper);
        if (!queryResult.isEmpty()) {
            return new RequestResult<>(200, "ユーザー名が重複しています", null);
        }

        user.setPassword(passwordEncoder.encode(password));
        int result = userMapper.insert(user);
        if (result < 1) {
            return new RequestResult<>(200, Constants.SUCCESS, user);
        } else {
            return new RequestResult<>(200, "ユーザー登録失敗", null);
        }
    }
}
