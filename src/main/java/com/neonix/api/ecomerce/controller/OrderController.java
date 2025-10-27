package com.neonix.api.ecomerce.controller;

import com.neonix.api.ecomerce.models.Order;
import com.neonix.api.ecomerce.models.OrderDetail;
import com.neonix.api.ecomerce.models.User;
import com.neonix.api.ecomerce.repository.OrderRepository;
import com.neonix.api.ecomerce.repository.UserRepository;
import com.neonix.api.ecomerce.repository.AddressRepository;
import com.neonix.api.ecomerce.repository.PaymentMethodRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000") // permite conexión desde tu frontend (Next.js)
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    // ✅ Obtener todas las órdenes
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // ✅ Obtener una orden por ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Integer id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Crear una nueva orden con detalles incluidos
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        // 1️⃣ Validar que venga un usuario
        if (order.getUser() == null || order.getUser().getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<User> userOpt = userRepository.findById(order.getUser().getId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().build(); // Usuario no encontrado
        }

        order.setUser(userOpt.get());

        // 2️⃣ Validar dirección (opcional)
        if (order.getShippingAddress() != null && order.getShippingAddress().getId() != null) {
            addressRepository.findById(order.getShippingAddress().getId())
                    .ifPresent(order::setShippingAddress);
        }

        // 3️⃣ Validar método de pago (opcional)
        if (order.getPaymentMethod() != null && order.getPaymentMethod().getId() != null) {
            paymentMethodRepository.findById(order.getPaymentMethod().getId())
                    .ifPresent(order::setPaymentMethod);
        }

        // 4️⃣ Setear campos automáticos
        order.setOrderDate(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.pending);

        // 5️⃣ Asociar los detalles con la orden padre
        if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
            for (OrderDetail detail : order.getOrderDetails()) {
                detail.setOrder(order);
            }
        }

        // 6️⃣ Guardar la orden (en cascada guarda los detalles)
        Order savedOrder = orderRepository.save(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedOrder);
    }

    // ✅ Actualizar una orden existente
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Integer id, @RequestBody Order orderDetails) {
        Optional<Order> existingOrderOpt = orderRepository.findById(id);
        if (existingOrderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order existingOrder = existingOrderOpt.get();

        existingOrder.setShippingNumber(orderDetails.getShippingNumber());
        existingOrder.setDeliveryDate(orderDetails.getDeliveryDate());
        existingOrder.setTotalAmount(orderDetails.getTotalAmount());
        existingOrder.setStatus(orderDetails.getStatus());
        existingOrder.setUpdatedAt(LocalDateTime.now());

        // Actualizar dirección
        if (orderDetails.getShippingAddress() != null && orderDetails.getShippingAddress().getId() != null) {
            addressRepository.findById(orderDetails.getShippingAddress().getId())
                    .ifPresent(existingOrder::setShippingAddress);
        } else if (orderDetails.getShippingAddress() == null) {
            existingOrder.setShippingAddress(null);
        }

        // Actualizar método de pago
        if (orderDetails.getPaymentMethod() != null && orderDetails.getPaymentMethod().getId() != null) {
            paymentMethodRepository.findById(orderDetails.getPaymentMethod().getId())
                    .ifPresent(existingOrder::setPaymentMethod);
        } else if (orderDetails.getPaymentMethod() == null) {
            existingOrder.setPaymentMethod(null);
        }

        Order updatedOrder = orderRepository.save(existingOrder);
        return ResponseEntity.ok(updatedOrder);
    }

    // ✅ Eliminar una orden por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Integer id) {
        if (!orderRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        orderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Obtener todas las órdenes de un usuario
    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUserId(@PathVariable Integer userId) {
        return orderRepository.findByUserId(userId);
    }
}
