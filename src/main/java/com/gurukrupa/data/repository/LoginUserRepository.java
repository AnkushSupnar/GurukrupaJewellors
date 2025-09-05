package com.gurukrupa.data.repository;



import com.gurukrupa.data.entities.LoginUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginUserRepository extends JpaRepository<LoginUser, Long> {

    // Additional method to find by username if needed
    LoginUser findByUsername(String username);

    @Query("select l.username from LoginUser  l")
    List<String> getUserNames();

    @Query("select case when count(l) > 0 then true else false end from LoginUser l where l.username = :username and l.password = :password")
    boolean existsByUsernameAndPassword(@Param("username") String username, @Param("password") String password);
}

