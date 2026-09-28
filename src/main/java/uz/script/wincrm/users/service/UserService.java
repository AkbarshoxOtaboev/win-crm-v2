package uz.script.wincrm.users.service;

import uz.script.wincrm.users.dto.UserDTO;
import uz.script.wincrm.users.response.UserResponse;
import uz.script.wincrm.users.response.UserStatResponse;

import java.util.List;

public interface UserService {
    UserResponse create(UserDTO dto);
    UserResponse findById(Long id);
    List<UserResponse> fetchAllUsers();

    /** Minimal ro'yxat (id, username, fullName) — tanlash uchun, USER_VIEW talab qilinmaydi. */
    List<UserResponse> lookupUsers();
    UserResponse update(Long id, UserDTO dto);
    void delete(Long id);
    void activeOrDisabledUser(Long id);
    UserStatResponse getUserStats(Long userId);
}
