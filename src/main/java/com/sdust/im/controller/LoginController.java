package com.sdust.im.controller;

import com.sdust.im.common.ServerResponse;
import com.sdust.im.domin.dao.User;
import com.sdust.im.service.LoginService;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.session.SessionProperties.Redis;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/*
 * @author luhao
 * @date 2018/5/11 17:16
 *
 */
@RestController
public class LoginController {

    private final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private LoginService loginService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户登陆
     * @param user
     * @return
     */
    @RequestMapping("login")
    public ServerResponse login(@RequestBody User user, HttpSession session){
        String accountId = user.getAccountId();
        String password = user.getPassword();
        logger.info("用户账号为【{}】，密码为【{}】",accountId , password);
        if(null != accountId && null != password){
            ServerResponse response = loginService.login(user);
            if (response.isSuccess()){
                session.setAttribute("isLogin", true);
            }
            return response;
        }
        return ServerResponse.createByError();
    }

    /**
     * 用户注册
     * @param user
     * @return
     */
    @RequestMapping("register")
    public ServerResponse register(@RequestBody User user, HttpSession session){

        if(null != user){
            String birth = user.getBirth();
            birth = birth.substring(0, birth.indexOf("T"));
            user.setBirth(birth);
            List<String> homeplace = user.getHomeplace();
            user.setProvince(homeplace.get(0));
            user.setCity(homeplace.get(1));
            user.setCounty(homeplace.get(2));
            ServerResponse response = loginService.register(user);
            return response;
        }else{
            return ServerResponse.createByError();

        }
    }

    /**
     * 用户登出
     * @param id
     * @return
     */
    @RequestMapping("logout/{id}")
    public ServerResponse logout(@PathVariable(value = "id") String id, HttpSession session){
        logger.info("本次登出用户id【{}】", id);
        return ServerResponse.createBySuccess();
    }

}
