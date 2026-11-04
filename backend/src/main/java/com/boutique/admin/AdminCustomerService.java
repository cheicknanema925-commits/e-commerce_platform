package com.boutique.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boutique.common.LikePatterns;
import com.boutique.common.PageResponse;

@Service
@Transactional(readOnly = true)
public class AdminCustomerService {

	private final AdminCustomerRepository adminCustomerRepository;

	public AdminCustomerService(AdminCustomerRepository adminCustomerRepository) {
		this.adminCustomerRepository = adminCustomerRepository;
	}

	// Tri imposé (clients les plus récents d'abord) : un tri libre sur une colonne calculée (count, sum) échouerait
	public PageResponse<AdminCustomerResponse> findAll(String search, int page, int size) {
		String pattern = StringUtils.hasText(search) ? LikePatterns.contains(search) : null;
		PageRequest newestFirst = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
		return PageResponse.from(adminCustomerRepository.findCustomers(pattern, newestFirst)
			.map(AdminCustomerResponse::from));
	}

}
