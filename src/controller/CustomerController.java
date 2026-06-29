package src.controller;

import src.dao.CustomerDAO;
import src.model.Customer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerDAO customerDAO;

    public CustomerController(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    @GetMapping
    public ResponseEntity<?> getAllCustomers() {
        try {
            return ResponseEntity.ok(customerDAO.getAllCustomers());
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> addCustomer(@RequestBody Customer customer) {
        try {
            customerDAO.addCustomer(customer);
            return ResponseEntity.ok(Map.of("message", "Customer added successfully"));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}