package src.controller;

import src.dao.CustomerDAO;
import src.dao.ItemDAO;
import src.dao.OrderDAO;
import src.model.Order;
import src.model.OrderItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderDAO orderDAO;
    private final CustomerDAO customerDAO;
    private final ItemDAO itemDAO;

    public OrderController(OrderDAO orderDAO, CustomerDAO customerDAO, ItemDAO itemDAO) {
        this.orderDAO = orderDAO;
        this.customerDAO = customerDAO;
        this.itemDAO = itemDAO;
    }

    // Request body: { "customerId": 1, "items": [{ "itemId": 1, "quantity": 2 }] }
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody Map<String, Object> body) {
        try {
            int customerId = (int) body.get("customerId");
            if (customerDAO.getCustomerById(customerId) == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Customer not found"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) body.get("items");

            List<OrderItem> orderItems = new java.util.ArrayList<>();
            for (Map<String, Object> entry : itemsList) {
                int itemId = (int) entry.get("itemId");
                int qty = (int) entry.get("quantity");
                var item = itemDAO.getItemById(itemId);
                if (item == null)
                    return ResponseEntity.badRequest().body(Map.of("error", "Item " + itemId + " not found"));
                orderItems.add(new OrderItem(itemId, qty, item.getPrice()));
            }

            int orderId = orderDAO.placeOrder(customerId, orderItems);
            Order placed = orderDAO.getOrderById(orderId);
            return ResponseEntity.ok(placed);

        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable int id) {
        try {
            Order order = orderDAO.getOrderById(id);
            if (order == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(order);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/report/customers-purchased")
    public ResponseEntity<?> customersWhoPurchased() {
        try {
            return ResponseEntity.ok(orderDAO.getCustomersWhoPurchased());
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/report/recent")
    public ResponseEntity<?> recentOrders(@RequestParam(defaultValue = "7") int days) {
        try {
            return ResponseEntity.ok(orderDAO.getOrdersWithinDays(days));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/report/extreme")
    public ResponseEntity<?> extremeOrders() {
        try {
            Order highest = orderDAO.getHighestOrder();
            Order lowest = orderDAO.getLowestOrder();
            return ResponseEntity.ok(Map.of("highest", highest, "lowest", lowest));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}