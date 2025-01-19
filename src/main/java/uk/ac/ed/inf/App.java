package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.data.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The main application class for processing orders and generating delivery information.
 */
public class App {


    public static final int NUMBERPLATE = 2;
    private static final String RESTAURANT_URL = "restaurants";
    private static final String ORDER_URL = "orders";
    private static final String CENTRAL_AREA_URL = "centralArea";
    private static final String NO_FLY_ZONES_URL = "noFlyZones";
    private static final boolean TO_SCHOOL = true;
    private static final boolean TO_RESTAURANT = false;

    private static final Point APPLETONTOWER = new Point(-3.186874, 55.944494);
    public static final int HOVER = 999;
    public static  NamedRegion centralArea = null;
    static SimpleModule module = new SimpleModule();

    private static final double FULL_CIRCLE = 360.0;
    private static final double ANGLE_STEP = 22.5;

    /**
     * The main entry point for the application.
     *
     * @param args Command line arguments. Requires the base URL and specified date.
     * @return
     */
    public static int main(String[] args) {
        try {
            validateArguments(args);
            String baseUrl = args[1];
            String specifiedDate = args[0];
            LocalDate localDate = LocalDate.parse(specifiedDate);


            // Ensure the URL ends with a slash
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            Restaurant[] restaurants = fetchData(baseUrl + RESTAURANT_URL, Restaurant[].class);
            Order[] orders = fetchData(baseUrl + ORDER_URL, Order[].class);
            NamedRegion[] noFlyZones = fetchData(baseUrl + NO_FLY_ZONES_URL, NamedRegion[].class);
            centralArea = fetchData(baseUrl + CENTRAL_AREA_URL, NamedRegion.class);

            Map map = createMapWithNoFlyZones(noFlyZones);
            Point startPoint = APPLETONTOWER;

            List<Order> validatedOrders = processOrders(orders, restaurants, map, startPoint);
            List<Point> pathPoints = generatePathPoints(validatedOrders, restaurants, map, startPoint);

            writeResults(validatedOrders, pathPoints, localDate);

        } catch (MalformedURLException e) {
            handleMalformedURLException(e);
        } catch (IOException e) {
            handleIOException(e);
        }
        return 0;
    }

    /**
     * Validates the command-line arguments.
     *
     * @param args The command-line arguments.
     * @throws IllegalArgumentException If the number of arguments is invalid.
     */
    private static void validateArguments(String[] args) {
        if (args.length != NUMBERPLATE) {
            printUsage();
            throw new IllegalArgumentException("Invalid number of arguments.");
        }
    }

    /**
     * Fetches data from a specified URL using the provided value type.
     *
     * @param url       The URL to fetch data from.
     * @param valueType The class type of the value to be fetched.
     * @param <T>       The generic type of the value.
     * @return The fetched data.
     * @throws IOException If an IO error occurs during data fetching.
     */
    public static <T> T fetchData(String url, Class<T> valueType) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // 配置 ObjectMapper 忽略未知枚举值
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

        // 添加自定义反序列化器（如果有需要）
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CreditCardInformation.class, new CardDeserializer());
        mapper.registerModule(module);

        // 注册 JavaTimeModule 处理日期时间类型
        mapper.registerModule(new JavaTimeModule());

        // 读取和反序列化数据
        return mapper.readValue(new URL(url), valueType);
    }


    /**
     * Creates a map with no-fly zones based on the provided array of named regions.
     *
     * @param noFlyZones An array of named regions representing no-fly zones.
     * @return The created map with no-fly zones.
     */
    private static Map createMapWithNoFlyZones(NamedRegion[] noFlyZones) {
        Map map = new Map();
        for (NamedRegion n : noFlyZones) {
            map.addObstacle(n);
        }
        return map;
    }

    /**
     * Processes the orders, validating them and returning a list of validated orders.
     *
     * @param orders      An array of orders to be processed.
     * @param restaurants An array of restaurants.
     * @param map         The map with no-fly zones.
     * @param startPoint  The starting point for processing orders.
     * @return The list of validated orders.
     */
    private static List<Order> processOrders(Order[] orders, Restaurant[] restaurants, Map map, Point startPoint) {
        List<Order> validatedOrders = new ArrayList<>();
        for (Order order : orders) {
            OrderValidator orderValidator = new OrderValidator();
            Order validatedOrder = orderValidator.validateOrder(order, restaurants);
            validatedOrders.add(validatedOrder);
        }
        return validatedOrders;
    }

    /**
     * Generates path points for the given validated orders, restaurants, map, and starting point.
     *
     * @param validatedOrders An array of validated orders.
     * @param restaurants     An array of restaurants.
     * @param map             The map with no-fly zones.
     * @param startPoint      The starting point for generating path points.
     * @return The list of generated path points.
     */
    private static List<Point> generatePathPoints(List<Order> validatedOrders, Restaurant[] restaurants, Map map, Point startPoint) {
        List<Point> pathPoints = new ArrayList<>();
        for (Order order : validatedOrders) {
            System.out.println(order.getOrderDate());
            if (order.getOrderStatus().equals(OrderStatus.VALID_BUT_NOT_DELIVERED)) {
                Restaurant restaurantOfOrder = findRestaurantForOrder(order, restaurants);
                if (restaurantOfOrder != null) {
                    Point restaurantPoint = new Point(restaurantOfOrder.location().lng(), restaurantOfOrder.location().lat());
                    AStar toRestaurant = new AStar(map, startPoint, restaurantPoint);
                    List<Point> toRestaurantPath = toRestaurant.findPath(TO_RESTAURANT);

                    // Skip the order if the restaurant is in an obstacle
                    if(map.isInObstacle(restaurantOfOrder.location())) {continue;}

                    for (Point p : toRestaurantPath) {
                        p.setOrderNo(order.getOrderNo());
                    }
                    pathPoints.addAll(toRestaurantPath);
                    startPoint = toRestaurantPath.get(toRestaurantPath.size() - 1);
                    AStar toSchool = new AStar(map, startPoint, APPLETONTOWER);
                    List<Point> toSchoolPath = toSchool.findPath(TO_SCHOOL);
                    for (Point p : toSchoolPath) {
                        p.setOrderNo(order.getOrderNo());
                    }
                    pathPoints.addAll(toSchoolPath);
                    startPoint = toSchoolPath.get(toSchoolPath.size() - 1);
                }
            }
        }
        pathPoints.add(pathPoints.get(pathPoints.size() - 1));
        return pathPoints;
    }

    /**
     * Writes results, including deliveries, directions, and coordinates, to the output files.
     *
     * @param validatedOrders An array of validated orders.
     * @param pathPoints      The list of generated path points.
     * @param localDate       The specified date for the data.
     * @throws IOException If an IO error occurs while writing results.
     */
    public static void writeResults(List<Order> validatedOrders, List<Point> pathPoints, LocalDate localDate) throws IOException {
        File resultFolder = new File("resultfiles");

        new App().writeDeliveries(validatedOrders, localDate);
        new App().writeDirections(pathPoints, localDate);
        new App().writeCoordinates(buildFlightPathList(pathPoints), localDate);
    }

    /**
     * Prints the application usage instructions.
     */
    private static void printUsage() {
        System.err.println("Usage: App Base-URL Specified-Date");
        System.err.println("You must supply the base address of the ILP REST Service\n" +
                "e.g., http://restservice.somewhere and the specified date");
    }

    /**
     * Handles a malformed URL exception.
     *
     * @param e The MalformedURLException to handle.
     */
    private static void handleMalformedURLException(MalformedURLException e) {
        System.err.println("Malformed URL. Please check the provided URL.");
        e.printStackTrace();
    }

    /**
     * Handles an IO exception.
     *
     * @param e The IOException to handle.
     */
    private static void handleIOException(IOException e) {
        System.err.println("An IO error occurred while processing the request.");
        e.printStackTrace();
    }


    /**
     * Writes the delivery information to a JSON file.
     *
     * @param validatedOrder List of validated orders.
     * @param date           The date for which deliveries are processed.
     * @throws IOException If an I/O error occurs during writing.
     */
    public static void writeDeliveries(List<Order> validatedOrder, LocalDate date) throws IOException {
        List<JsonOrder> deliverList = validatedOrder.stream().map(
                order -> new JsonOrder(order.getOrderNo(),
                        order.getOrderStatus(),
                        order.getOrderValidationCode(),
                        order.getPriceTotalInPence())).toList();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            String format = date.toString();
            String name = "deliveries-" + format + ".json";
            objectMapper.writeValue(new File("resultfiles/" + name), deliverList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the directions (flight path) to a JSON file.
     *
     * @param points List of points representing the flight path.
     * @param date   The date for which directions are generated.
     * @throws IOException If an I/O error occurs during writing.
     */
    public static void writeDirections(List<Point> points, LocalDate date) throws IOException {
        List<JsonMove> path = IntStream.range(0, points.size() - 1)
                .mapToObj(i -> {
                    Point currentPoint = points.get(i);
                    Point nextPoint = points.get(i + 1);

                    return new JsonMove(currentPoint.getOrderNo(),
                            currentPoint.getLng(),
                            currentPoint.getLat(),
                            findAngle(currentPoint, nextPoint),
                            nextPoint.getLng(),
                            nextPoint.getLat());
                }).toList();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            String format = date.toString();
            String name = "flightpath-" + format + ".json";
            objectMapper.writeValue(new File("resultfiles/" + name), path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a list of points to a list of coordinates.
     *
     * @param points List of points.
     * @return List of coordinate lists.
     */
    public static List<List<Double>> toCoordinateList(List<Point> points) {
        List<List<Double>> coordinateList = new ArrayList<>();

        for (Point p : points) {
            List<Double> coordinates = new ArrayList<>();
            coordinates.add(p.getLng());
            coordinates.add(p.getLat());
            coordinateList.add(coordinates);
        }

        return coordinateList;
    }

    /**
     * Writes the coordinates (flight path) to a GeoJSON file.
     *
     * @param pathPoints List of points representing the flight path.
     * @param date       The date for which coordinates are generated.
     * @throws IOException If an I/O error occurs during writing.
     */
    public static void writeCoordinates(List<Point> pathPoints, LocalDate date) throws IOException {
        GeoPoint pointList = new GeoPoint("LineString", toCoordinateList(pathPoints));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            String format = date.toString();
            String name = "drone-" + format + ".geojson";
            objectMapper.writeValue(new File("resultfiles/" + name), pointList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds the restaurant associated with a given order.
     *
     * @param order       The order for which to find the associated restaurant.
     * @param restaurants Array of restaurants.
     * @return The restaurant associated with the order, or {@code null} if not found.
     */
    private static Restaurant findRestaurantForOrder(Order order, Restaurant[] restaurants) {
        for (Pizza pizza : order.getPizzasInOrder()) {
            for (Restaurant restaurant : restaurants) {
                if (Arrays.asList(restaurant.menu()).contains(pizza)) {
                    return restaurant;
                }
            }
        }
        return null;
    }

    /**
     * Calculates the angle between two points.
     *
     * @param p The first point.
     * @param q The second point.
     * @return The angle between the two points.
     */
    private static double findAngle(Point p, Point q) {
        double deltaLng = p.getLng() - q.getLng();
        double deltaLat = p.getLat() - q.getLat();

        double degreeAngle = Math.toDegrees(Math.atan2(deltaLat, deltaLng));

        if (deltaLat == 0 && deltaLng == 0) {
            return HOVER;
        }
        if (degreeAngle < 0) {
            degreeAngle += FULL_CIRCLE;
        }

        double angle = 0;
        while (angle <= FULL_CIRCLE){
            if (Math.abs(angle - degreeAngle) <= (ANGLE_STEP / 2)) {
                return angle;
            }
            angle += ANGLE_STEP;
        }
        return angle;
    }


    /**
     * Builds a new list of points by removing consecutive duplicate points.
     *
     * @param points The list of points to process.
     * @return A new list with consecutive duplicate points removed.
     */
    private static List<Point> buildFlightPathList(List<Point> points) {
        List<Point> newPoints = new ArrayList<>(points);
        int i = 0;
        while (i < points.size()-1){
            if(points.get(i) == points.get(i+1)){
                points.remove(i);
            }
            i++;
        }
        return newPoints;
    }
}

