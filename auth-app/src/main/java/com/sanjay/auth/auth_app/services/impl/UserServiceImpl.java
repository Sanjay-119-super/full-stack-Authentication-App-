package com.sanjay.auth.auth_app.services.impl;

import com.sanjay.auth.auth_app.dtos.UserDto;
import com.sanjay.auth.auth_app.entities.Provider;
import com.sanjay.auth.auth_app.entities.User;
import com.sanjay.auth.auth_app.exception.ResourceNotFoundException;
import com.sanjay.auth.auth_app.repository.UserRepository;
import com.sanjay.auth.auth_app.services.UserService;
import com.sanjay.auth.auth_app.utills.UserHelper;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if(userDto.getEmail()==null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email Already exists");
        }

        // here may have more checks as per required
        /*Here need to conversion Entity to Dto or Dto to Entity - Use model mapper dependency*/

        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);

        //role assign here to user for authorization
        //TODO:??
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser,UserDto.class);

    }

    @Override
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given email id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uuid = UserHelper.parseUUID(userId);
        User existingUser = userRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));

        if (userDto.getName() != null) existingUser.setName(userDto.getName());
        if (userDto.getImage() != null) existingUser.setImage(userDto.getImage());
        if (userDto.getProvider() != null) existingUser.setProvider(userDto.getProvider());
        //TODO: password save hashed next.....
        if (userDto.getPassword() != null) existingUser.setPassword(userDto.getPassword());
        existingUser.setEnable(userDto.isEnable());
        existingUser.setUpdatedAt(Instant.now());
        User updatedUser = userRepository.save(existingUser);
        return modelMapper.map(updatedUser,UserDto.class);

    }

    @Override
    public void deleteUser(String userId) {
        UUID uuid = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("user not found with given id"));
        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map((User user) -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}
