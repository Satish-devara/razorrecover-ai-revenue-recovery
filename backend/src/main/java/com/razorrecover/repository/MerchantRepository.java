package com.razorrecover.repository;

import com.razorrecover.domain.Merchant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
}
