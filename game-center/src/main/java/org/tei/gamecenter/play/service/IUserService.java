package org.tei.gamecenter.play.service;

import org.tei.gamecenter.play.pojo.User;

public interface IUserService {
    User register(String username, String password);

    String login(String username, String password);

    User me();
}
