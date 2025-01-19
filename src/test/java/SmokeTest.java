import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import junit.framework.TestCase;
import uk.ac.ed.inf.*;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.data.Restaurant;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SmokeTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        System.out.println("Set up");
    }

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
            OrderValidationCode.CARD_NUMBER_INVALID, OrderValidationCode.EXPIRY_DATE_INVALID,
            OrderValidationCode.CVV_INVALID, OrderValidationCode.TOTAL_INCORRECT,
            OrderValidationCode.PIZZA_NOT_DEFINED, OrderValidationCode.MAX_PIZZA_COUNT_EXCEEDED,
            OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR,
            OrderValidationCode.NO_ERROR, OrderValidationCode.NO_ERROR
        };

        try {
            ordersData = objectMapper1.readValue(new File("./smoke_test_data.json"), Order[].class);
            restaurantsData = objectMapper2.readValue(new File("./smoke_test_restaurant_data.json"), Restaurant[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ordersData != null) {
            for (int i = 0; i < ordersData.length; ++i) {
                toTest.validateOrder(ordersData[i], restaurantsData);
                org.junit.Assert.assertEquals(ordersData[i].getOrderValidationCode(), expected[i]);
            }
        }
        System.out.println("Test passed");
    }

    public void testPathFindingAlgo() {
        ObjectMapper objectMapper = new ObjectMapper();
        Restaurant[] restaurantsData = null;
        NamedRegion[] noFlyZones = null;
        try {
            noFlyZones = objectMapper.readValue(new File("./noflyzones.json"), NamedRegion[].class);
        } catch (IOException e) {
            e.printStackTrace();
            org.junit.Assert.fail("Failed to read test data files.");
        }

        Map map = new Map();
        for (NamedRegion zone : noFlyZones) {
            map.addObstacle(zone);
        }
        Point startPoint = new Point(-3.2025, 55.9433);
        Point endPoint = new Point(-3.1869, 55.9445);
        AStar pathFinder = new AStar(map, startPoint, endPoint);
        List<Point> path = pathFinder.findPath(false);

        Point lastPoint = path.get(path.size() - 1);
        org.junit.Assert.assertEquals("End longitude mismatch", endPoint.getLng(), lastPoint.getLng(), 1e-2);
        org.junit.Assert.assertEquals("End latitude mismatch", endPoint.getLat(), lastPoint.getLat(), 1e-2);

        System.out.println("Test passed");
    }
}
