package com.dazzledepot.cart_service.controller;

import com.dazzledepot.cart_service.model.Cart;
import com.mongodb.client.result.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;
import com.dazzledepot.cart_service.repository.CartRepository;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("/add")
    public Cart addItem(@RequestBody Cart.CartItem item) {
        try {
            String userId = item.getUserId(); // Extract userId from the request body
            logger.info("Adding item to cart for userId: {}", userId);
            Query query = new Query(Criteria.where("userId").is(userId));
            Update update = new Update().push("items", item);
            UpdateResult result = mongoTemplate.upsert(query, update, Cart.class);
            if (result.getModifiedCount() == 0 && result.getUpsertedId() == null) {
                Cart cart = new Cart(userId);
                cart.getItems().add(item);
                return cartRepo.save(cart);
            }
            return getCart(userId);
        } catch (Exception e) {
            logger.error("Error adding item to cart: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{userId}")
    public Cart getCart(@PathVariable String userId) {
        try {
            logger.info("Fetching cart for userId: {}", userId);
            return cartRepo.findById(userId).orElse(new Cart(userId));
        } catch (Exception e) {
            logger.error("Error fetching cart: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/remove")
    public Cart removeItem(@RequestBody Cart.RemoveRequest request) {
        try {
            String userId = request.getUserId();
            logger.info("Removing items from cart for userId: {}", userId);
            Cart cart = cartRepo.findById(userId).orElse(new Cart(userId));
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                for (Cart.CartItem item : request.getItems()) {
                    if (item.getProductId() != null) { // Check for null productId
                        cart.getItems().removeIf(i -> i.getProductId().equals(item.getProductId()));
                    } else {
                        logger.warn("Skipping removal: productId is null for item: {}", item);
                    }
                }
            } else {
                cart.setItems(new ArrayList<>());
            }
            return cartRepo.save(cart);
        } catch (Exception e) {
            logger.error("Error removing items from cart: {}", e.getMessage(), e);
            throw e;
        }
    }
    @PostMapping("/update")
    public Cart updateQuantity(@RequestBody Cart.CartItem item) {
        try {
            String userId = item.getUserId();
            logger.info("Updating quantity for productId: {} in cart for userId: {}", item.getProductId(), userId);
            Query query = new Query(Criteria.where("userId").is(userId));
            Cart cart = cartRepo.findById(userId).orElse(new Cart(userId));

            // Find and update the existing item
            boolean itemUpdated = false;
            for (Cart.CartItem existingItem : cart.getItems()) {
                if (existingItem.getProductId() != null && existingItem.getProductId().equals(item.getProductId())) {
                    existingItem.setQuantity(Math.max(1, item.getQuantity())); // Ensure quantity doesn't go below 1
                    itemUpdated = true;
                    break;
                }
            }
            if (!itemUpdated && item.getProductId() != null) {
                cart.getItems().add(item); // Add new item if not found
            }

            return cartRepo.save(cart);
        } catch (Exception e) {
            logger.error("Error updating quantity in cart: {}", e.getMessage(), e);
            throw e;
        }
    }

}
