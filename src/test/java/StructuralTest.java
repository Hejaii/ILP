import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ed.inf.*;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StructuralTest {

    /**
     * Test system-level functionality: validate the entire workflow from processing orders
     * to generating flight paths and output files.
     */
    @Test
    public void testSystemLevel() {
        String[] testDates = {"2023-11-15",  "2023-11-17"};

        for (String testDate : testDates) {
            String baseUrl = "https://ilp-rest-2024.azurewebsites.net"; // Replace with your test base URL
            String[] args = {testDate, baseUrl};
            App.main(args); // Execute the main application workflow

            File flightPathFile = new File("./resultfiles/flightpath-" +testDate + ".json");
            JsonMove[] flightPaths;
            try {
                // 读取文件内容
                String jsonContent = Files.readString(Paths.get(flightPathFile.getPath()), StandardCharsets.US_ASCII);

                // 替换未知枚举值为默认值
                jsonContent = jsonContent.replace("\"PRICE_FOR_PIZZA_INVALID\"", "\"UNDEFINED\"");

                // 配置 ObjectMapper
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

                // 反序列化
                flightPaths = objectMapper.readValue(jsonContent, JsonMove[].class);

                // 验证反序列化结果
                double lastLng = 0, lastLat = 0;
                String currentOrder = "";

                for (JsonMove path : flightPaths) {
                    if (!path.orderNo().equals(currentOrder)) {
                        if (!currentOrder.isEmpty()) {
                            Assert.assertEquals("End point longitude mismatch", lastLng, path.fromLongitude(), 1e-6);
                            Assert.assertEquals("End point latitude mismatch", lastLat, path.fromLatitude(), 1e-6);
                        }
                        currentOrder = path.orderNo();
                    }
                    lastLng = path.toLongitude();
                    lastLat = path.toLatitude();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Assert.fail("Failed to read flight path file for date: " + testDate);
            }

        }
        System.out.println("System-level test passed.");
    }

    /**
     * Test module-level functionality: validate the pathfinding algorithm with given start and end points.
     */
    @Test
    public void testPathFindingAlgo() {
        double[] startLng = {-3.192785, -3.183496, -3.202130, -3.197598,
                -3.192642, -3.205881, -3.206784, -3.182572};
        double[] startLat = {55.942891, 55.935584, 55.946064, 55.941246,
                55.941042, 55.937646, 55.935953, 55.937077};
        double[] endLng = {-3.201733, -3.191808, -3.203827, -3.192392,
                -3.186334, -3.201121, -3.204187, -3.195669};
        double[] endLat = {55.945131, 55.942063, 55.940247, 55.943826,
                55.937428, 55.939588, 55.937671, 55.944693};

        ObjectMapper objectMapper = new ObjectMapper();
        NamedRegion[] noFlyZones = null;
        try {
            noFlyZones = objectMapper.readValue(new File("./noflyzones.json"), NamedRegion[].class);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Failed to read test data files.");
        }

        Map map = new Map();
        for (NamedRegion zone : noFlyZones) {
            map.addObstacle(zone);
        }

        for (int i = 0; i < startLng.length; i++) {
            Point startPoint = new Point(startLng[i], startLat[i]);
            Point endPoint = new Point(endLng[i], endLat[i]);

            AStar pathFinder = new AStar(map, startPoint, endPoint);
            List<Point> path = pathFinder.findPath(false);

            Point lastPoint = path.get(path.size() - 1);
            Assert.assertEquals("End longitude mismatch", endPoint.getLng(), lastPoint.getLng(), 1e-2);
            Assert.assertEquals("End latitude mismatch", endPoint.getLat(), lastPoint.getLat(), 1e-2);
        }
        System.out.println("Pathfinding algorithm test passed.");
    }

    /**
     * Test unit-level functionality: validate the order validator with various test cases.
     */
    @Test
    public void testOrderValidator() {
        OrderValidator toTest = new OrderValidator();

        // Read order from the rest service
        ObjectMapper objectMapper1 = new ObjectMapper();
        objectMapper1.registerModule(new JavaTimeModule());
        ObjectMapper objectMapper2 = new ObjectMapper();

        // Register a custom deserializer for CreditCardInformation
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CreditCardInformation.class, new CardDeserializer());
        objectMapper1.registerModule(module);

        Order[] ordersData = null;
        Restaurant[] restaurantsData = null;

        OrderValidationCode[] expected = {
                OrderValidationCode.CARD_NUMBER_INVALID, OrderValidationCode.EXPIRY_DATE_INVALID, OrderValidationCode.CVV_INVALID,
                OrderValidationCode.TOTAL_INCORRECT, OrderValidationCode.PIZZA_NOT_DEFINED,
                OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED, OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS, OrderValidationCode.RESTAURANT_CLOSED,
                OrderValidationCode.CARD_NUMBER_INVALID, OrderValidationCode.EXPIRY_DATE_INVALID, OrderValidationCode.CVV_INVALID,
                OrderValidationCode.TOTAL_INCORRECT, OrderValidationCode.PIZZA_NOT_DEFINED,
                OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED, OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS, OrderValidationCode.RESTAURANT_CLOSED,
                OrderValidationCode.CARD_NUMBER_INVALID, OrderValidationCode.EXPIRY_DATE_INVALID, OrderValidationCode.CVV_INVALID,
                OrderValidationCode.TOTAL_INCORRECT, OrderValidationCode.PIZZA_NOT_DEFINED,
                OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED, OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS,
                OrderValidationCode.RESTAURANT_CLOSED, OrderValidationCode.CARD_NUMBER_INVALID,
                OrderValidationCode.EXPIRY_DATE_INVALID, OrderValidationCode.CVV_INVALID, OrderValidationCode.TOTAL_INCORRECT,
                OrderValidationCode.PIZZA_NOT_DEFINED, OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED,
                OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS, OrderValidationCode.RESTAURANT_CLOSED,
                OrderValidationCode.CARD_NUMBER_INVALID, OrderValidationCode.EXPIRY_DATE_INVALID,
                OrderValidationCode.CVV_INVALID, OrderValidationCode.TOTAL_INCORRECT, OrderValidationCode.PIZZA_NOT_DEFINED,
                OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED, OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS,
                OrderValidationCode.RESTAURANT_CLOSED, OrderValidationCode.CARD_NUMBER_INVALID, OrderValidationCode.EXPIRY_DATE_INVALID,
                OrderValidationCode.CVV_INVALID, OrderValidationCode.TOTAL_INCORRECT,
                OrderValidationCode.PIZZA_NOT_DEFINED, OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED,
                OrderValidationCode.PIZZA_FROM_MULTIPLE_RESTAURANTS, OrderValidationCode.RESTAURANT_CLOSED,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
                OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR
        };

        try {
            ordersData = objectMapper1.readValue(new File("./functional_test_data.json"), Order[].class);
            restaurantsData = objectMapper2.readValue(new File("./smoke_test_restaurant_data.json"), Restaurant[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ordersData != null) {
            for (int i = 0; i < ordersData.length; ++i) {
                toTest.validateOrder(ordersData[i], restaurantsData);
                Assert.assertEquals(ordersData[i].getOrderValidationCode(), expected[i]);
            }
        }
        System.out.println("Test passed");
    }



    @Test
    public void testLngLatHandler() {
        LngLatHandler toTest = new LngLatHandler();

        LngLat start = new LngLat(29.745907488250266, 89.49656456882596);
        LngLat end = new LngLat(52.63486667376833, 51.93695619937847);

        double expectedDistance = 43.984413528687426;
        double actualDistance = toTest.distanceTo(start, end);
        Assert.assertEquals(expectedDistance, actualDistance, 1e-2);

        boolean isClose = toTest.isCloseTo(start, end);
        Assert.assertFalse(isClose);

        double angle = 20.0;
        LngLat nextPosition = toTest.nextPosition(start, angle);
        LngLat expectedPosition = new LngLat(29.745907, 89.496565);
        Assert.assertEquals(expectedPosition.lat(), nextPosition.lat(), 1e-2);
        Assert.assertEquals(expectedPosition.lng(), nextPosition.lng(), 1e-2);

        System.out.println("TestLngLatHandler passed.");
    }

    @Test
    public void testFetchData() throws IOException {
        String testUrl = "https://ilp-rest-2024.azurewebsites.net/orders";
        Order[] orders = App.fetchData(testUrl, Order[].class);
        Assert.assertNotNull(orders);
        Assert.assertTrue(orders.length > 0);  // 确保至少获取一条数据
    }
    @Test
    public void testLoadAndValidateResults() throws IOException {
        // 文件路径
        String deliveriesFilePath = "/Users/leojiang/Downloads/PizzaDronz-2/resultfiles/deliveries-2023-10-18.json";
        String flightPathFilePath = "/Users/leojiang/Downloads/PizzaDronz-2/resultfiles/flightpath-2023-10-18.json";

        // 加载数据
        ObjectMapper mapper = new ObjectMapper();

        // 从文件加载 validatedOrders
        Order[] validatedOrders = mapper.readValue(new File(deliveriesFilePath), Order[].class);
        Assert.assertNotNull(validatedOrders); // 确保数据不为空
        Assert.assertTrue(validatedOrders.length > 0); // 确保有订单数据

        // 验证数据的特定属性（示例）
        Assert.assertTrue(validatedOrders[0].getOrderNo() != null); // 确保订单号存在
    }


    @Test
    public void testIntegrationLevel() throws IOException {
        // 定义 REST 服务的基础 URL
        String baseUrl = "https://ilp-rest-2024.azurewebsites.net/";

        // 调用方法获取订单、餐厅和禁飞区数据
        Order[] orderResults = App.fetchData(baseUrl + "orders", Order[].class);
        Assert.assertNotNull(orderResults);
        Assert.assertTrue(orderResults.length > 0); // 确保至少有一个订单

        Restaurant[] restaurantResults = App.fetchData(baseUrl + "restaurants", Restaurant[].class);
        Assert.assertNotNull(restaurantResults);
        Assert.assertTrue(restaurantResults.length > 0); // 确保至少有一个餐厅

        NamedRegion[] noFlyZones = App.fetchData(baseUrl + "noFlyZones", NamedRegion[].class);
        Assert.assertNotNull(noFlyZones);
        Assert.assertTrue(noFlyZones.length > 0); // 确保至少有一个禁飞区

        // 准备输出订单数据
        List<Order> orderResultsList = new ArrayList<>();
        orderResultsList.add(new Order());
        orderResultsList.add(new Order());
        LocalDate date = LocalDate.of(2022, 12, 31);
        App.writeDeliveries(orderResultsList, date);

        // 验证订单结果文件是否生成
        File deliveriesFile = new File("./resultfiles/deliveries-2022-12-31.json");
        Assert.assertTrue(deliveriesFile.exists());

        // 准备输出飞行路径数据
        List<Point> flightPathsResults = new ArrayList<>();
        flightPathsResults.add(new Point(-3.186874, 55.944494));
        flightPathsResults.add(new Point(-3.186800, 55.944400));

        App.writeDirections(flightPathsResults, date);

        // 验证飞行路径文件是否生成
        File flightPathFile = new File("./resultfiles/flightpath-2022-12-31.json");
        Assert.assertTrue(flightPathFile.exists());

        // 准备输出 GeoJSON 数据
        App.writeCoordinates(flightPathsResults, date);

        // 验证 GeoJSON 文件是否生成
        File geoJsonFile = new File("./resultfiles/drone-2022-12-31.geojson");
        Assert.assertTrue(geoJsonFile.exists());

        // 输出测试通过日志
        System.out.println("TestIntegrationLevel passed.");
    }


}

