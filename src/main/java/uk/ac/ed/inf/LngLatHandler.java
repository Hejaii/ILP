package uk.ac.ed.inf;


import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;
/**
 * A utility class for handling longitude and latitude operations.
 */
public class LngLatHandler implements LngLatHandling {

    /**
     * Calculates the Euclidean distance between two LngLat positions.
     *
     * @param startPosition The starting position.
     * @param endPosition   The ending position.
     * @return The Euclidean distance between the positions.
     */
    @Override
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        return Math.sqrt(Math.pow((startPosition.lat() - endPosition.lat()), 2)
                + Math.pow((startPosition.lng() - endPosition.lng()), 2));
    }

    /**
     * Checks if two LngLat positions are close to each other within a predefined distance.
     *
     * @param startPosition The starting position.
     * @param otherPosition  The position to check for proximity.
     * @return True if the positions are close; otherwise, false.
     */
    @Override
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        return distanceTo(startPosition, otherPosition) <= SystemConstants.DRONE_IS_CLOSE_DISTANCE;
    }

    /**
     * Checks if a given LngLat position is within a specified named region.
     *
     * @param position The position to check.
     * @param region   The named region.
     * @return True if the position is within the region; otherwise, false.
     */
    @Override
    public boolean isInRegion(LngLat position, NamedRegion region) {
        return isPointInPolygon(region.vertices(), position);
    }

    /**
     * Calculates the next LngLat position based on the given starting position and angle.
     *
     * @param startPosition The starting position.
     * @param angle          The angle of movement.
     * @return The next LngLat position.
     */
    @Override
    public LngLat nextPosition(LngLat startPosition, double angle) {
        final int NORTHEAST = 0;
        final int NORTHWEST = 1;
        final int SOUTHWEST = 2;
        final int SOUTHEAST = 3;
        final double DISTANCE = 0.00015;
        int direction = (int) angle / 90;
        double lng = 0;
        double lat = 0;
        double acuteAngle = Math.toRadians(angle - direction * 90);
        switch (direction) {
            case NORTHEAST:
                lng = startPosition.lng() + DISTANCE * Math.cos(acuteAngle);
                lat = startPosition.lat() + DISTANCE * Math.sin(acuteAngle);
                break;
            case NORTHWEST:
                lng = startPosition.lng() - DISTANCE * Math.sin(acuteAngle);
                lat = startPosition.lat() + DISTANCE * Math.cos(acuteAngle);
                break;
            case SOUTHWEST:
                lng = startPosition.lng() - DISTANCE * Math.cos(acuteAngle);
                lat = startPosition.lat() - DISTANCE * Math.sin(acuteAngle);
                break;
            case SOUTHEAST:
                lng = startPosition.lng() + DISTANCE * Math.sin(acuteAngle);
                lat = startPosition.lat() - DISTANCE * Math.cos(acuteAngle);
                break;
            default:
                break;
        }
        return new LngLat(lng, lat);
    }

    /**
     * Checks if a point is inside a polygon.
     *
     * @param polygon The vertices of the polygon.
     * @param p       The point to check.
     * @return True if the point is inside the polygon; otherwise, false.
     */
    private static boolean isPointInPolygon(LngLat[] polygon, LngLat p) {
        int count = 0;
        int size = polygon.length;
        double epsilon = 1e-10;

        for (int i = 0; i < size; i++) {
            LngLat p1 = polygon[i];
            LngLat p2 = polygon[(i + 1) % size];
            if (Double.compare(p1.lat(), p2.lat()) == 0) {
                continue;
            }
            if (p.lat() < Math.min(p1.lat(), p2.lat()) - epsilon || p.lat() >= Math.max(p1.lat(), p2.lat()) + epsilon) {
                continue;
            }
            double x = (p.lat() - p1.lat()) * (p2.lng() - p1.lng()) / (p2.lat() - p1.lat()) + p1.lng();
            if (Double.compare(Math.abs(x - p.lng()), epsilon) < 0 &&
                    Double.compare(p.lng(), Math.min(p1.lng(), p2.lng()) - epsilon) >= 0 &&
                    Double.compare(p.lng(), Math.max(p1.lng(), p2.lng()) + epsilon) < 0) {
                return true;
            }
            if (Double.compare(x, p.lng() + epsilon) > 0) {
                count++;
            }
        }
        return count % 2 == 1;
    }

    /**
     * Checks if a line segment intersects with a polygon.
     *
     * @param polygon The vertices of the polygon.
     * @param start   The starting point of the line segment.
     * @param end     The ending point of the line segment.
     * @return True if the line segment intersects with the polygon; otherwise, false.
     */
    public static boolean isSegmentIntersectPolygon(LngLat[] polygon, LngLat start, LngLat end) {
        int size = polygon.length;
        double distance = 1e-10;
        for (int i = 0; i < size; i++) {
            LngLat p1 = polygon[i];
            LngLat p2 = polygon[(i + 1) % size];
            return doSegmentsIntersect(start, end, p1, p2, distance);
        }
        return false;
    }

    /**
     * Checks if two line segments intersect.
     *
     * @param start1   The starting point of the first segment.
     * @param end1     The ending point of the first segment.
     * @param start2   The starting point of the second segment.
     * @param end2     The ending point of the second segment.
     * @param epsilon  A small value for numerical comparisons.
     * @return True if the segments intersect; otherwise, false.
     */
    public static boolean doSegmentsIntersect(LngLat start1, LngLat end1, LngLat start2, LngLat end2, double epsilon) {
        double x1 = start1.lng(), y1 = start1.lat();
        double x2 = end1.lng(), y2 = end1.lat();
        double x3 = start2.lng(), y3 = start2.lat();
        double x4 = end2.lng(), y4 = end2.lat();
        double den = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (Math.abs(den) < epsilon) {
            return false;
        }
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / den;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / den;
        return ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1;
    }
}
