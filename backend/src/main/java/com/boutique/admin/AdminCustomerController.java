package com.boutique.admin;

import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.common.PageResponse;

@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

	private final AdminCustomerService adminCustomerService;

	public AdminCustomerController(AdminCustomerService adminCustomerService) {
		this.adminCustomerService = adminCustomerService;
	}

	@GetMapping
	public PageResponse<AdminCustomerResponse> findAll(@RequestParam(required = false) @Size(max = 100) String q,
			@PageableDefault(size = 20) Pageable pageable) {
		return adminCustomerService.findAll(q, pageable.getPageNumber(), pageable.getPageSize());
	}

}
