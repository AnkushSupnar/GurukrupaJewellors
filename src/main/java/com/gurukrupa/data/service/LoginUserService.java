package com.gurukrupa.data.service;


import com.gurukrupa.data.entities.LoginUser;
import com.gurukrupa.data.repository.LoginUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginUserService {
    @Autowired
    private LoginUserRepository loginUserRepository;

    public LoginUser saveLoginUser(LoginUser loginUser){
        return loginUserRepository.save(loginUser);
    }
    public List<String> getAllUserName(){
        return loginUserRepository.getUserNames();
    }

    public boolean authenticate(String userName,String password){
        return loginUserRepository.existsByUsernameAndPassword(userName,password);
    }
}