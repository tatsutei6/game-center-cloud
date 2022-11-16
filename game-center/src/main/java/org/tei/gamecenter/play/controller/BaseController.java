package org.tei.gamecenter.play.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.tei.gamecenter.play.pojo.User;
import org.tei.gamecenter.play.pojo.UserDetailsImpl;

public class BaseController {
    public User currentUser() {
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        // ログイン失敗なら、自動的に例外が発生する
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        return loginUser.getUser();
    }

    public User getUserByUsernameAndPassword(AuthenticationManager authenticationManager, String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        // ログイン失敗なら、自動的に例外が発生する
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticate.getPrincipal();
        return loginUser.getUser();
    }

    public Integer currentUserId() {
        return currentUser().getId();
    }
}
