package com.boutique.payment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

	List<PaymentEvent> findByPaymentIdOrderByIdAsc(Long paymentId);

}
