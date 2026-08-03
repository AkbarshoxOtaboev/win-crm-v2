package uz.script.wincrm.users.service;

import uz.script.wincrm.users.dto.UserDTO;
import uz.script.wincrm.users.response.UserResponse;
import uz.script.wincrm.users.response.UserStatResponse;

import java.util.List;

public interface UserService {
    UserResponse create(UserDTO dto);
    UserResponse findById(Long id);
    List<UserResponse> fetchAllUsers();
    UserResponse update(Long id, UserDTO dto);
    void delete(Long id);
    void activeOrDisabledUser(Long id);
    UserStatResponse getUserStats(Long userId);
}
