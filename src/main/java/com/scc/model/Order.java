package com.scc.model;

public record Order(int id, int customerId, String serviceName, int quantity, double totalPrice, String status, String createdAt) {
}