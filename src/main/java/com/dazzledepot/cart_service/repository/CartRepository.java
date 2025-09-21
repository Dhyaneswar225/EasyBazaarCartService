package com.dazzledepot.cart_service.repository;

import com.dazzledepot.cart_service.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartRepository extends MongoRepository<Cart, String> {
}