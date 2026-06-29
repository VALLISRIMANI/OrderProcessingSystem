package src.controller;

import src.dao.ItemDAO;
import src.model.Item;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemDAO itemDAO;

    public ItemController(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    @GetMapping
    public ResponseEntity<?> getAllItems() {
        try {
            return ResponseEntity.ok(itemDAO.getAllItems());
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> addItem(@RequestBody Item item) {
        try {
            itemDAO.addItem(item);
            return ResponseEntity.ok(Map.of("message", "Item added successfully"));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchByName(@RequestParam String name) {
        try {
            return ResponseEntity.ok(itemDAO.searchByName(name));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/price-range")
    public ResponseEntity<?> searchByPriceRange(@RequestParam double min, @RequestParam double max) {
        try {
            return ResponseEntity.ok(itemDAO.searchByPriceRange(min, max));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/reorder")
    public ResponseEntity<?> getReorderItems() {
        try {
            return ResponseEntity.ok(itemDAO.getItemsNeedingReorder());
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}