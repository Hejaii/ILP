package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.*;

import static uk.ac.ed.inf.App.centralArea;

/**
 * A* algorithm implementation for pathfinding on a map.
 */
public class AStar {

    public static final int NUBER_OF_DIRECTIONS = 16;
    private Map map;
    private Point startPoint;
    private Point endPoint;
    private PriorityQueue<Point> openList;
    private List<Point> closeList;
    private LngLatHandler handler;

    /**
     * Constructs an AStar object with the given map, start point, and end point.
     *
     * @param map        The map on which pathfinding is performed.
     * @param startPoint The starting point for the pathfinding.
     * @param endPoint   The destination point for the pathfinding.
     */
    public AStar(Map map, Point startPoint, Point endPoint) {
        this.map = map;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.openList = new PriorityQueue<>(Comparator.comparingDouble(Point::getCost));
        this.closeList = new ArrayList<>();
        this.handler = new LngLatHandler();
    }

    /**
     * Calculates the total cost of a point, including base cost and heuristic cost.
     *
     * @param p The point for which to calculate the total cost.
     * @return The total cost of the point.
     */
    private double totalCost(Point p) {
        return p.getBaseCost() + p.getHeuristicCost(endPoint.getLngLat())*7.23;
    }

    /**
     * Checks if a point is accessible, i.e., not in an obstacle on the map.
     *
     * @param p The point to check for accessibility.
     * @return True if the point is accessible, false otherwise.
     */
    private boolean isAccessiblePoint(Point p) {
        LngLat point = p.getLngLat();
        return !map.isInObstacle(point);
    }

    /**
     * Checks if a point is in a given list of points.
     *
     * @param p          The point to check for.
     * @param pointList  The list of points to search.
     * @return True if the point is in the list, false otherwise.
     */
    private boolean isInPointList(Point p, List<Point> pointList) {
        return pointList.contains(p);
    }

    /**
     * Checks if a point is in the close list.
     *
     * @param p The point to check for.
     * @return True if the point is in the close list, false otherwise.
     */
    private boolean isInCloseList(Point p) {
        return isInPointList(p, this.closeList);
    }

    /**
     * Checks if a point is the starting point.
     *
     * @param p The point to check.
     * @return True if the point is the starting point, false otherwise.
     */
    private boolean isStartPoint(Point p) {
        return p.getLngLat().equals(startPoint.getLngLat());
    }

    /**
     * Checks if a point is near the end point.
     *
     * @param p The point to check.
     * @return True if the point is near the end point, false otherwise.
     */
    private boolean isNearEndPoint(Point p) {
        return handler.isCloseTo(p.getLngLat(), endPoint.getLngLat());
    }

    /**
     * Checks if a point is in central area.
     *
     * @param p The point to check.
     * @return True if the point is in the central area, false otherwise.
     */
    private boolean isInCentralArea(Point p){return  handler.isInRegion(p.getLngLat(), centralArea);}


    /**
     * Processes a point during the pathfinding algorithm.
     *
     * @param q The point to be processed.
     * @param p The parent point from which the path is extended.
     */
    private void processPoint(Point q, Point p) {
        if (!isAccessiblePoint(q)) {
            return;
        }
        if (isInCloseList(q)) {
            return;
        }

        for (NamedRegion noFlyZone: map.getObstacles()) {
            if(LngLatHandler.isSegmentIntersectPolygon(noFlyZone.vertices(),q.getLngLat(), p.getLngLat())) {
                return;
            }
        }

        q.setParent(p);
        q.setBaseCost(p.getBaseCost() +0.00015);
        q.setCost(totalCost(q));
        Iterator<Point> iterator = openList.iterator();
        while (iterator.hasNext()) {
            Point r = iterator.next();
            if (r.equals(q)) {
                if (Double.compare(r.getCost(),q.getCost())>0) {
                    iterator.remove();
                } else {
                    return;
                }
            }
        }
        this.openList.add(q);
    }

    /**
     * Builds the path from the destination point to the starting point.
     *
     * @param p The destination point.
     * @return The list of points representing the path.
     */
    private List<Point> buildPath(Point p) {
        List<Point> path = new ArrayList<>();
        while (!isStartPoint(p)) {
            path.add(0, p);
            p = p.getParent();
        }
        path.add(0, p);
        return path;
    }

    /**
     * Finds the surrounding points of a given center point based on specified angles.
     *
     * @param center The center point.
     * @return An array of surrounding points.
     */
    public Point[] surroundingPoints(Point center) {
        double[] angles = {0, 22.5, 45, 67.5, 90, 112.5, 135, 157.5, 180, 202.5, 225, 247.5, 270, 292.5, 315, 337.5};
        Point[] surroundingPoints = new Point[16];

        for (int i = 0; i < NUBER_OF_DIRECTIONS; i++) {
            double angle = angles[i];
            LngLat nextPosition = handler.nextPosition(center.getLngLat(), angle);
            Point point = new Point(nextPosition.lng(), nextPosition.lat());
            surroundingPoints[i] = point;
        }

        return surroundingPoints;
    }

    /**
     * Finds the path from the start point to the end point using the A* algorithm.
     *
     * @return The list of LngLat points representing the path.
     */
    public List<Point> findPath(boolean toSchool) {
        Point start = new Point(startPoint.getLng(), startPoint.getLat());
        start.setBaseCost(0);
        start.setCost(0);
        this.openList.add(start);

        while (!this.openList.isEmpty()) {
            Point p = this.openList.poll();

            if (isNearEndPoint(p)) {
                return buildPath(p);
            }
            this.closeList.add(p);

            for (Point q : surroundingPoints(p)) {
                if(toSchool && isInCentralArea(p) && !isInCentralArea(q)) {
                    continue;
                }
                processPoint(q, p);
            }
        }
        return new ArrayList<>();
    }
}
