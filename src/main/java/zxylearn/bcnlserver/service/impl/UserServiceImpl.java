package zxylearn.bcnlserver.service.impl;

import zxylearn.bcnlserver.pojo.DTO.UserSearchRequestDTO;
import zxylearn.bcnlserver.pojo.entity.User;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import zxylearn.bcnlserver.mapper.UserMapper;
import zxylearn.bcnlserver.service.UserService;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public boolean isExistUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .or()
                .eq(User::getEmail, username));
    }

    @Override
    public boolean isExistEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, email)
                .or()
                .eq(User::getEmail, email));
    }

    @Override
    public User getUserByUsernameOrEmail(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .or()
                .eq(User::getEmail, username));
    }

    @Override
    public List<User> searchUserList(UserSearchRequestDTO userSearchRequestDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        LocalDate now = LocalDate.now();

        if (userSearchRequestDTO.getMinAge() != null) {
            wrapper.le(User::getBirthday, now.minusYears(userSearchRequestDTO.getMinAge()));
        }
        if (userSearchRequestDTO.getMaxAge() != null) {
            wrapper.ge(User::getBirthday, now.minusYears(userSearchRequestDTO.getMaxAge() + 1).plusDays(1));
        }

        boolean hasCriteria = userSearchRequestDTO.getId() != null ||
                              userSearchRequestDTO.getUsername() != null ||
                              userSearchRequestDTO.getEmail() != null ||
                              userSearchRequestDTO.getName() != null ||
                              userSearchRequestDTO.getPrefixName() != null ||
                              userSearchRequestDTO.getSuffixName() != null ||
                              userSearchRequestDTO.getPhone() != null ||
                              userSearchRequestDTO.getGender() != null ||
                              userSearchRequestDTO.getAddress() != null ||
                              userSearchRequestDTO.getAdmin() != null;

        if (hasCriteria) {
            if (Boolean.TRUE.equals(userSearchRequestDTO.getMatchAll())) {
                wrapper.eq(userSearchRequestDTO.getId() != null, User::getId, userSearchRequestDTO.getId())
                       .eq(userSearchRequestDTO.getUsername() != null, User::getUsername, userSearchRequestDTO.getUsername())
                       .eq(userSearchRequestDTO.getEmail() != null, User::getEmail, userSearchRequestDTO.getEmail())
                       .like(userSearchRequestDTO.getName() != null, User::getName, userSearchRequestDTO.getName())
                       .likeRight(userSearchRequestDTO.getPrefixName() != null, User::getName, userSearchRequestDTO.getPrefixName())
                       .likeLeft(userSearchRequestDTO.getSuffixName() != null, User::getName, userSearchRequestDTO.getSuffixName())
                       .like(userSearchRequestDTO.getPhone() != null, User::getPhone, userSearchRequestDTO.getPhone())
                       .eq(userSearchRequestDTO.getGender() != null, User::getGender, userSearchRequestDTO.getGender())
                       .like(userSearchRequestDTO.getAddress() != null, User::getAddress, userSearchRequestDTO.getAddress())
                       .eq(userSearchRequestDTO.getAdmin() != null, User::getAdmin, userSearchRequestDTO.getAdmin());
            } else {
                wrapper.and(w -> w
                       .or(userSearchRequestDTO.getId() != null, i -> i.eq(User::getId, userSearchRequestDTO.getId()))
                       .or(userSearchRequestDTO.getUsername() != null, i -> i.eq(User::getUsername, userSearchRequestDTO.getUsername()))
                       .or(userSearchRequestDTO.getEmail() != null, i -> i.eq(User::getEmail, userSearchRequestDTO.getEmail()))
                       .or(userSearchRequestDTO.getName() != null, i -> i.like(User::getName, userSearchRequestDTO.getName()))
                       .or(userSearchRequestDTO.getPrefixName() != null, i -> i.likeRight(User::getName, userSearchRequestDTO.getPrefixName()))
                       .or(userSearchRequestDTO.getSuffixName() != null, i -> i.likeLeft(User::getName, userSearchRequestDTO.getSuffixName()))
                       .or(userSearchRequestDTO.getPhone() != null, i -> i.like(User::getPhone, userSearchRequestDTO.getPhone()))
                       .or(userSearchRequestDTO.getGender() != null, i -> i.eq(User::getGender, userSearchRequestDTO.getGender()))
                       .or(userSearchRequestDTO.getAddress() != null, i -> i.like(User::getAddress, userSearchRequestDTO.getAddress()))
                       .or(userSearchRequestDTO.getAdmin() != null, i -> i.eq(User::getAdmin, userSearchRequestDTO.getAdmin()))
                );
            }
        }

        wrapper.last("LIMIT " + userSearchRequestDTO.getOffset() + ", " + userSearchRequestDTO.getSize());
        List<User> userList = baseMapper.selectList(wrapper);
        userList.forEach(user -> user.setPasswordHash("---"));
        return userList;
    }

}