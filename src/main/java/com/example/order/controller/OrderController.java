package com.example.order.controller;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.exception.ResourceNotFoundException;
import com.example.order.model.Order;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Order Management API and UI.
 *
 * <p>Provides both REST endpoints (/api/orders/*) and Thymeleaf UI flows (/, /orders/view).
 * Delegates business logic to {@link OrderService} and exposes stream-based summaries.</p>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    /**
     * Service layer for order operations.
     */
    private final OrderService orderService;

    /**
     * GET / — Renders create-order form page.
     *
     * @param model Thymeleaf model
     * @return index template
     */
    @GetMapping("/")
    public String home(Model model) {
        log.debug("Serving home page");
        return "index";
    }

    /**
     * POST /orders — Handles form submission to create an order.
     *
     * <p>Uses bean validation via {@code @Valid} on {@code @ModelAttribute}.
     * Validation failures are converted to specific, user-friendly flash messages.</p>
     *
     * @param request form-bound request DTO
     * @param bindingResult validation result
     * @param status optional initial status override
     * @param redirectAttributes flash attributes
     * @return redirect target
     */
    @PostMapping("/orders")
    public String createOrderFromForm(
            @Valid @ModelAttribute("orderRequest") OrderCreateRequest request,
            BindingResult bindingResult,
            @RequestParam(required = false) Order.Status status,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .distinct()
                    .collect(Collectors.joining(" | "));

            log.warn("Form validation failed: {}", message);
            redirectAttributes.addFlashAttribute("errorMessage", message);
            redirectAttributes.addFlashAttribute("submittedOrder", request);
            return "redirect:/";
        }

        Order saved = orderService.createOrder(request);

        if (status != null && status != Order.Status.NEW) {
            orderService.updateStatus(saved.getId(), status);
            log.debug("Order status overridden to {}", status);
        }

        log.info("Order created successfully from form: id={}", saved.getId());
        redirectAttributes.addFlashAttribute(
                "successMessage",
                String.format("Order #%d created successfully!", saved.getId())
        );
        return "redirect:/orders/view";
    }

    /**
     * GET /orders/view — Renders order listing page with optional single-order lookup.
     *
     * @param orderId optional order ID
     * @param model Thymeleaf model
     * @return orders template
     */
    @GetMapping("/orders/view")
    public String viewOrders(
            @RequestParam(required = false) Long orderId,
            Model model
    ) {
        List<Order> orders;

        if (orderId != null) {
            log.debug("Fetching single order: id={}", orderId);
            Order singleOrder = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("Order #%d not found", orderId)
                    ));
            orders = Collections.singletonList(singleOrder);
        } else {
            log.debug("Fetching all orders");
            orders = orderService.getAllOrders();
        }

        long totalOrders = orders.size();
        int totalQuantity = orders.stream()
                .map(Order::getQuantity)
                .filter(q -> q != null)
                .mapToInt(Integer::intValue)
                .sum();
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long completedOrders = orders.stream()
                .filter(o -> o.getStatus() == Order.Status.COMPLETED)
                .count();

        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("completedOrders", completedOrders);

        return "orders";
    }

    /**
     * POST /api/orders — Creates an order from JSON body.
     *
     * @param request validated JSON request
     * @return created order response
     */
    @PostMapping("/api/orders")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrderApi(@Valid @RequestBody OrderCreateRequest request) {
        log.info("API: Creating order for customer='{}'", request.getCustomerName());
        return OrderResponse.fromEntity(orderService.createOrder(request));
    }

    /**
     * GET /api/orders — Fetches all orders.
     *
     * @return list of order responses
     */
    @GetMapping("/api/orders")
    @ResponseBody
    public List<OrderResponse> getOrdersApi() {
        return orderService.getAllOrders().stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    /**
     * GET /api/orders/{id} — Fetches one order by ID.
     *
     * @param id order identifier
     * @return order response
     */
    @GetMapping("/api/orders/{id}")
    @ResponseBody
    public OrderResponse getOrderByIdApi(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(OrderResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Order #%d not found", id)
                ));
    }

    /**
     * GET /api/orders/search — Searches orders by customer name.
     *
     * @param customerName optional keyword
     * @return matching orders or all orders if empty
     */
    @GetMapping("/api/orders/search")
    @ResponseBody
    public List<OrderResponse> searchOrdersApi(@RequestParam(required = false) String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return getOrdersApi();
        }
        return orderService.searchByCustomerName(customerName).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    /**
     * PATCH /api/orders/{id}/status — Updates order status.
     *
     * @param id order identifier
     * @param status new status
     * @return updated order response
     */
    @PatchMapping("/api/orders/{id}/status")
    @ResponseBody
    public OrderResponse updateStatusApi(@PathVariable Long id, @RequestParam Order.Status status) {
        return orderService.updateStatus(id, status)
                .map(OrderResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Order #%d not found for status update", id)
                ));
    }

    /**
     * GET /api/orders/analytics/status-count — Returns status distribution.
     *
     * @return status-count map
     */
    @GetMapping("/api/orders/analytics/status-count")
    @ResponseBody
    public Object statusCountApi() {
        return orderService.countByStatus();
    }

    /**
     * GET /api/orders/analytics/completed-revenue — Returns completed orders revenue.
     *
     * @return revenue value
     */
    @GetMapping("/api/orders/analytics/completed-revenue")
    @ResponseBody
    public BigDecimal completedRevenueApi() {
        return orderService.calculateCompletedRevenue();
    }

    /**
     * Handles not-found exceptions for page routes.
     *
     * @param ex exception
     * @param model model for error page
     * @return error template
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("details", "The requested resource does not exist.");
        return "error";
    }

    /**
     * Handles unexpected exceptions for page routes.
     *
     * @param ex exception
     * @param model model for error page
     * @return error template
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model) {
        log.error("Unexpected exception in page handler", ex);
        model.addAttribute("message", "An unexpected error occurred.");
        model.addAttribute("details", ex.getMessage());
        return "error";
    }
}