import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.ed.inf.*;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FunctionalTest extends TestCase {




    // testPathFinding
    // startPoint or Endpoint in the obstacles
    // cause endless while
    @Test
    public void testPathFinding() {
        // 初始化简单地图
        Map map = new Map();

        // 定义起点和终点
        Point startPoint = new Point(-3.192473, 55.943484); // 起点：某个坐标
        Point endPoint = new Point(-3.187300, 55.944800);   // 终点：某个坐标

        // 初始化 A* 路径规划器
        AStar pathFinder = new AStar(map, startPoint, endPoint);

        // 执行路径规划
        List<Point> path = pathFinder.findPath(false);

        // 验证路径规划结果
        Assert.assertNotNull("Path should not be null", path); // 路径不为空
        Assert.assertFalse("Path should contain points", path.isEmpty()); // 路径应包含点
        Assert.assertEquals("Path should start with startPoint", startPoint.getLat(), path.get(0).getLat()); // 起点匹配
        Assert.assertEquals("Path should start with startPoint", startPoint.getLng(), path.get(0).getLng()); // 起点匹配
        Assert.assertEquals("Path should end with endPoint", endPoint.getLat(), path.get(path.size() - 1).getLat(), 1e-3); // 终点匹配
        Assert.assertEquals("Path should end with endPoint", endPoint.getLng(), path.get(path.size() - 1).getLng(), 1e-3); // 终点匹配

        System.out.println("Test passed");
    }



    // order date not match the pattern
    public void testFindValidateOrders() {
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
                OrderValidationCode.CARD_NUMBER_INVALID, OrderValidationCode.EXPIRY_DATE_INVALID
        };

        try {
            ordersData = objectMapper1.readValue(new File("./structural_test_data.json"), Order[].class);
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
    public void testLagLatHandler() {
        LngLatHandler toTest = new LngLatHandler();

        LngLat[] startPositions = {
                new LngLat(29.745907488250266, 89.49656456882596), // 0
                new LngLat(-3.1884309389133922, 55.94378009065508)  // 1
        };

        LngLat[] vertices = {
                new LngLat(-3.190578818321228, 55.94402412577528),
                new LngLat(-3.1899887323379517, 55.94284650540911),
                new LngLat(-3.187097311019897, 55.94328811724263),
                new LngLat(-3.187682032585144, 55.944477740393744),
                new LngLat(-3.190578818321228, 55.94402412577528)
        };
        NamedRegion testRegion = new NamedRegion("George Square Area", vertices);
        boolean result = toTest.isInRegion(startPositions[0], testRegion);
        Assert.assertFalse(result);
        System.out.println("Test passed");
    }
}
