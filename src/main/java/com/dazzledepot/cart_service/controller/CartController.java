package com.dazzledepot.cart_service.controller;

import com.dazzledepot.cart_service.model.Cart;
import com.dazzledepot.cart_service.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;

import com.mongodb.client.result.UpdateResult;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("/add")
    public Cart addItem(@RequestBody Cart.CartItem item, @RequestParam String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        Update update = new Update().push("items", item);
        UpdateResult result = mongoTemplate.upsert(query, update, Cart.class);
        if (result.getModifiedCount() == 0 && result.getUpsertedId() == null) {
            // If no cart was created or updated, create a new one
            Cart cart = new Cart(userId);
            cart.getItems().add(item);
            return cartRepo.save(cart);
        }
        return getCart(userId);
    }

    @GetMapping("/{userId}")
    public Cart getCart(@PathVariable String userId) {
        return cartRepo.findById(userId).orElse(new Cart(userId));
    }

    @PostMapping("/remove")
    public Cart removeItem(@RequestBody Cart.CartItem item, @RequestParam String userId) {
        Cart cart = cartRepo.findById(userId).orElse(new Cart(userId));
        cart.getItems().removeIf(i -> i.getProductId().equals(item.getProductId()));
        return cartRepo.save(cart);
    }
}