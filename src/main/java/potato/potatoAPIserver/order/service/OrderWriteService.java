package potato.potatoAPIserver.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import potato.potatoAPIserver.cart.domain.Cart;
import potato.potatoAPIserver.cart.domain.CartProduct;
import potato.potatoAPIserver.cart.repository.CartProductRepository;
import potato.potatoAPIserver.cart.service.CartReadService;
import potato.potatoAPIserver.common.CustomException;
import potato.potatoAPIserver.common.ResultCode;
import potato.potatoAPIserver.order.domain.Order;
import potato.potatoAPIserver.order.dto.request.OrderCreateRequest;
import potato.potatoAPIserver.order.repository.OrderRepository;
import potato.potatoAPIserver.user.domain.User;
import potato.potatoAPIserver.user.service.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderWriteService {

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final OrderProductWriteService orderProductWriteService;
    private final CartReadService cartReadService;
    private final CartProductRepository cartProductRepository;

    public Long createOrderWithCart(Long userId, OrderCreateRequest orderCreateRequest) {
        Cart cart = cartReadService.findCart(userId).orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, ResultCode.CART_NOT_FOUND));

        List<Long> cartProductIdList = orderCreateRequest.getCartProductIdList();
        List<CartProduct> cartProductList = cartProductRepository.findAllByCartId(cart.getId());

        if (!cartProductList.stream().map(CartProduct::getId).collect(Collectors.toSet()).containsAll(cartProductIdList)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, ResultCode.CART_PRODUCT_NOT_FOUND);
        }

        List<CartProduct> selectedCartProductList = cartProductList.stream()
                .filter(cartProduct -> cartProductIdList.contains(cartProduct.getId()))
                .toList();

        BigDecimal totalPrice = getTotalPrice(selectedCartProductList);

        User user = userService.getUserById(userId);

        Order order = Order.builder()
                .user(user)
                .orderPrice(totalPrice)
                .build();

        Order savedOrder = orderRepository.save(order);

        orderProductWriteService.createOrderProductWithCart(userId, savedOrder, selectedCartProductList);

        return savedOrder.getId();
    }

    private static BigDecimal getTotalPrice(List<CartProduct> selectedCartProductList) {
        return selectedCartProductList.stream()
                .map(cartProduct -> cartProduct.getProduct().getPrice().multiply(BigDecimal.valueOf(cartProduct.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
