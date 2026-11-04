package com.boutique.admin;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.boutique.user.User;

// Un 2e repository sur l'entité User, propre au back-office : Repository (et non JpaRepository) n'expose QUE
// les méthodes déclarées ici, en lecture seule. Le LEFT JOIN garde aussi les clients sans commande.
interface AdminCustomerRepository extends Repository<User, Long> {

	@Query(value = """
			select u.id as id, u.fullName as fullName, u.email as email, u.createdAt as createdAt,
			       count(o.id) as orderCount, coalesce(sum(o.totalAmount), 0) as totalSpent,
			       max(o.createdAt) as lastOrderAt
			from User u
			left join Order o on o.user = u and o.status <> com.boutique.order.OrderStatus.CANCELLED
			where u.role = com.boutique.user.Role.CUSTOMER
			  and (:pattern is null or lower(u.email) like :pattern escape '\\' or lower(u.fullName) like :pattern escape '\\')
			group by u.id, u.fullName, u.email, u.createdAt
			""", countQuery = """
			select count(u) from User u
			where u.role = com.boutique.user.Role.CUSTOMER
			  and (:pattern is null or lower(u.email) like :pattern escape '\\' or lower(u.fullName) like :pattern escape '\\')
			""")
	Page<CustomerSummary> findCustomers(String pattern, Pageable pageable);

	interface CustomerSummary {

		Long getId();

		String getFullName();

		String getEmail();

		Instant getCreatedAt();

		long getOrderCount();

		BigDecimal getTotalSpent();

		Instant getLastOrderAt();

	}

}
