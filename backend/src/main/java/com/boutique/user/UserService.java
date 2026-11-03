package com.boutique.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserResponse getProfile(Long userId) {
		return userRepository.findById(userId).map(UserResponse::from).orElseThrow(() -> new UserNotFoundException(userId));
	}

}
