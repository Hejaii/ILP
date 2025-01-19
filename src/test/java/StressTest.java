import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import junit.framework.TestCase;
import org.junit.Assert;
import uk.ac.ed.inf.*;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StressTest extends TestCase {

    public void testSystemStress() throws IOException {
        int[] stressLevels = {10, 100, 1000, 10000, 100000};
        double[] elapsedTimes = new double[stressLevels.length];

        for (int i = 0; i < stressLevels.length; ++i) {
            long startTime = System.currentTimeMillis();
            String baseUrl = "https://ilp-rest.azurewebsites.net/";

            // Mock data preparation
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            SimpleModule module = new SimpleModule();
            module.addDeserializer(CreditCardInformation.class, new CardDeserializer());
            objectMapper.registerModule(module);

            Order[] ordersData;
            Restaurant[] restaurantsData;

            String jsonString = "{\n" +
                    "    \"orderNo\": \"6218488F\",\n" +
                    "    \"orderDate\": \"2023-09-01\",\n" +
                    "    \"orderStatus\": \"UNDEFINED\",\n" +
                    "    \"orderValidationCode\": \"UNDEFINED\",\n" +
                    "    \"priceTotalInPence\": 2600,\n" +
                    "    \"pizzasInOrder\": [\n" +
                    "      {\n" +
                    "        \"name\": \"R2: Meat Lover\",\n" +
                    "        \"priceInPence\": 1400\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"name\": \"R2: Vegan Delight\",\n" +
                    "        \"priceInPence\": 1100\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"creditCardInformation\": {\n" +
                    "      \"creditCardNumber\": \"4286860294655612\",\n" +
                    "      \"creditCardExpiry\": \"02/28\",\n" +
                    "      \"cvv\": \"937\"\n" +
                    "    }\n" +
                    "  }";

            // Write mock data to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("./stress_test_data.json"))) {
                writer.write('[');
                for (int j = 0; j < stressLevels[i]; j++) {
                    writer.write(jsonString);
                    if (j != stressLevels[i] - 1) writer.write(',');
                    writer.newLine();
                }
                writer.write(']');
            } catch (IOException e) {
                e.printStackTrace();
                Assert.fail("Failed to write stress test data to file.");
            }

            try {
                // Read mock data
                ordersData = objectMapper.readValue(new File("./stress_test_data.json"), Order[].class);
                restaurantsData = objectMapper.readValue(new File("./smoke_test_restaurant_data.json"), Restaurant[].class);

                // Execute system functions
                findOrdersByDate("2023-09-01", ordersData, restaurantsData);

                NamedRegion[] noFlyZones = null;
                try {
                    noFlyZones = objectMapper.readValue(new File("./noflyzones.json"), NamedRegion[].class);
                } catch (IOException e) {
                    e.printStackTrace();
                    Assert.fail("Failed to read test data files.");
                }
                Point startPoint = new Point(-3.186874, 55.944494);
                Point endPoint = new Point(-3.190874, 55.948494);
                Map map = new Map();
                for (NamedRegion zone : noFlyZones) {
                    map.addObstacle(zone);
                }
                AStar pathFinder = new AStar(map, startPoint, endPoint);
                List<Point> path = pathFinder.findPath(false);
                Assert.assertNotNull(path);

                long endTime = System.currentTimeMillis();
                elapsedTimes[i] = (endTime - startTime) / 1000.0; // Convert to seconds

            } catch (IOException e) {
                e.printStackTrace();
                Assert.fail("Failed during stress test execution.");
            }
        }

        // Print results
        for (int i = 0; i < stressLevels.length; ++i) {
            System.out.printf("StressLevel: %d, Time Elapsed (s): %.2f%n", stressLevels[i], elapsedTimes[i]);
        }
    }

    private static List<JsonOrder> findOrdersByDate(String date, Order[] orders, Restaurant[] restaurants) {
        // 解析日期字符串
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.parse(date, formatter);

        // 查找与日期匹配的订单
        List<JsonOrder> orderResults = new ArrayList<>();
        OrderValidator validator = new OrderValidator();
        for (Order order : orders) {
            if (order.getOrderDate().equals(today)) {
                // 验证订单
                Order validatedOrder = validator.validateOrder(order, restaurants);

                // 转换为 JsonOrder 对象
                orderResults.add(new JsonOrder(
                        validatedOrder.getOrderNo(),
                        validatedOrder.getOrderStatus(),
                        validatedOrder.getOrderValidationCode(),
                        validatedOrder.getPriceTotalInPence()
                ));
            }
        }
        return orderResults;
    }

}



