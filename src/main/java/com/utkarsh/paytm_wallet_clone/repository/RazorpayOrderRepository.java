package com.utkarsh.paytm_wallet_clone.repository;

import com.utkarsh.paytm_wallet_clone.model.RazorpayOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RazorpayOrderRepository extends JpaRepository<RazorpayOrder, Long> {

    Optional<RazorpayOrder> findByRazorpayOrderId(String razorpayOrderId);

    List<RazorpayOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}