package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Pizza;
import uk.ac.ed.inf.ilp.data.Restaurant;
import uk.ac.ed.inf.ilp.interfaces.OrderValidation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Date;

public class OrderValidator implements OrderValidation{
    public static boolean isNumeric(String str) {
        // 遍历字符串中的每个字符
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // 检查字符是否是数字字符（0到9之间）
            if (c < '0' || c > '9') {
                return false; // 如果有一个字符不是数字字符，返回false
            }
        }
        return true; // 如果所有字符都是数字字符，返回true
    }

    public static boolean isLegalDate(String orderDate, String cardDate) {
        SimpleDateFormat orderDateSdf = new SimpleDateFormat("yy-MM-dd");
        orderDateSdf.setLenient(false);

        SimpleDateFormat orderSdf = new SimpleDateFormat("yy-MM");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/yy");
        orderSdf.setLenient(false);
        sdf.setLenient(false); // 设置严格的日期解析模式，不允许不合法日期
        try {
            Date ordDate = orderDateSdf.parse(orderDate);
            String formattedOrderDate = orderDateSdf.format(ordDate);

            Date ordWithoutDay = orderSdf.parse(formattedOrderDate);
            Date crdDate = sdf.parse(cardDate);

            return crdDate.after(ordWithoutDay) || crdDate.equals(ordWithoutDay);
        } catch (ParseException e) {
            return false; // 日期格式不正确
        }
    }

    public static boolean isRestaurantOpen(LocalDate date, Restaurant restaurant) {
        DayOfWeek[] openingDays = restaurant.openingDays();
        DayOfWeek curDay = date.getDayOfWeek();
        for (DayOfWeek day: openingDays) {
            if (day.equals(curDay)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Order validateOrder(Order orderToValidate, Restaurant[] definedRestaurants) {

        CreditCardInformation cardInfo = orderToValidate.getCreditCardInformation();

        // validate cardNumber
        String creditCardNumber = cardInfo.getCreditCardNumber();
        if (creditCardNumber.length() != 16 || !isNumeric(creditCardNumber)) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.CARD_NUMBER_INVALID);
            return orderToValidate;
        }

        // validate card expiry date
        String creditCardExpiry = cardInfo.getCreditCardExpiry();
        String orderDate = orderToValidate.getOrderDate().toString();
        if (!isLegalDate(orderDate, creditCardExpiry)) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.EXPIRY_DATE_INVALID);
            return orderToValidate;
        }

        // validate cvv
        String creditCardCvv = cardInfo.getCvv();
        if (creditCardCvv.length() != 3 || !isNumeric(creditCardCvv)) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.CVV_INVALID);
            return orderToValidate;
        }

        // 一个顾客最多4 个pizza，首先对于这些 pizza，
        // 1. 验证是否有餐厅做（名字是否正确）
        // 2. 且 pizza必须来自同一家餐厅
        Pizza[] pizzas = orderToValidate.getPizzasInOrder();

        Restaurant[] pizzasRestaurant = new Restaurant[pizzas.length];

        if (pizzas.length > 4) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED);
            return orderToValidate;
        }

//        System.out.println(1);

        // validate pizza in menu
        // MayBe TODO: reduce the complexity
//        System.out.println(2);
        int orderOfRestaurant = -1;
        for (int i = 0; i < pizzas.length; ++i) {
            Pizza p = pizzas[i];
            boolean inMenu = false;
            for (int j = 0; j < definedRestaurants.length; ++j) {
                Restaurant restaurant = definedRestaurants[j];
                Pizza[] pizzaInMenu = restaurant.menu();
                for (int k = 0; k < pizzaInMenu.length; ++k) {
                    if (p.equals(pizzaInMenu[k])) {
                        inMenu = true;
                        orderOfRestaurant = j;
                        pizzasRestaurant[i] = restaurant;
                        break;
                    }
                }
            }

            if (!inMenu) {
                orderToValidate.setOrderStatus(OrderStatus.INVALID);
                orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_NOT_DEFINED);
                return orderToValidate;
            }
        }

//        if (definedRestaurants[orderOfRestaurant].name().equals("Civerinos Slice")) {
//            System.out.println(orderToValidate.getOrderNo() + " " + definedRestaurants[orderOfRestaurant].name());
//        }

        // validate price
        int priceInTotal = orderToValidate.getPriceTotalInPence();
        int cntPrice = 100; // fee
        for (Pizza p : pizzas) {
            cntPrice += p.priceInPence();
        }
        if (cntPrice != priceInTotal) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.TOTAL_INCORRECT);
            return orderToValidate;
        }

        // validate from the same restaurant
        boolean fromSameRestaurant = true;
        for (int i = 1; i < pizzasRestaurant.length; ++i) {
            if (!pizzasRestaurant[i].equals(pizzasRestaurant[i - 1])) {
                fromSameRestaurant = false;
            }
        }
        if (!fromSameRestaurant) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS);
            return orderToValidate;
        }

        if ((pizzasRestaurant.length!=0)&& (!isRestaurantOpen(orderToValidate.getOrderDate(), pizzasRestaurant[0]))) {
            orderToValidate.setOrderStatus(OrderStatus.INVALID);
            orderToValidate.setOrderValidationCode(OrderValidationCode.RESTAURANT_CLOSED);
            return orderToValidate;
        }

        orderToValidate.setOrderStatus(OrderStatus.VALID_BUT_NOT_DELIVERED);
        orderToValidate.setOrderValidationCode(OrderValidationCode.NO_ERROR);

        return orderToValidate;
    }
}