package com.slg.module.db.Service;
import com.slg.module.db.Dao.UserInfoDao;
import com.slg.module.db.entity.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserInfoDao dao;

    public UserService(UserInfoDao dao) {
        this.dao = dao;
    }

    @Transactional
    public UserInfo createUser(UserInfo user) {
        return dao.save(user);
    }

    public Page<UserInfo> getAllUsers(Pageable pageable) {
        return dao.findAll(pageable);
    }

}
