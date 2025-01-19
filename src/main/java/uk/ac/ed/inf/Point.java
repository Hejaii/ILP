package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;

/**
 * The Point class represents a geographical point with longitude and latitude coordinates.
 */
public class Point {
    private LngLat lngLat;      // The longitude and latitude coordinates of the point
    private double baseCost;    // The base cost associated with the point
    private Point parent;       // The parent point in the path
    private double cost;        // The total cost (including base cost and heuristic) to reach this point
    private String orderNo; // The order number associated with this point

    /**
     * Constructor to create a Point with the given longitude and latitude.
     *
     * @param longitude The longitude coordinate of the point.
     * @param latitude  The latitude coordinate of the point.
     */
    public Point(double longitude, double latitude) {
        this.lngLat = new LngLat(longitude, latitude);
    }

    /**
     * Get the order number associated with this point.
     *
     * @return The order number.
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * Set the order number associated with this point.
     *
     * @param orderNumber The order number to set.
     */
    public void setOrderNo(String orderNumber) {
        this.orderNo = orderNumber;
    }

    /**
     * Set the base cost for this point.
     *
     * @param cost The base cost to set.
     */
    public void setBaseCost(double cost) {
        this.baseCost = cost;
    }

    /**
     * Set the total cost for this point.
     *
     * @param cost The total cost to set.
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * Get the longitude coordinate of the point.
     *
     * @return The longitude coordinate.
     */
    public double getLng() {
        return lngLat.lng();
    }

    /**
     * Get the latitude coordinate of the point.
     *
     * @return The latitude coordinate.
     */
    public double getLat() {
        return lngLat.lat();
    }

    /**
     * Get the LngLat object representing the coordinates of the point.
     *
     * @return The LngLat object.
     */
    public LngLat getLngLat() {
        return lngLat;
    }

    /**
     * Get the total cost to reach this point.
     *
     * @return The total cost.
     */
    public double getCost() {
        return cost;
    }

    /**
     * Get the base cost associated with this point.
     *
     * @return The base cost.
     */
    public double getBaseCost() {
        return baseCost;
    }

    /**
     * Get the heuristic cost from this point to a given point.
     *
     * @param p The destination point.
     * @return The heuristic cost.
     */
    public double getHeuristicCost(LngLat p) {
        return new LngLatHandler().distanceTo(this.getLngLat(), p);
    }

    /**
     * Get the parent point in the path.
     *
     * @return The parent point.
     */
    public Point getParent() {
        return parent;
    }

    /**
     * Set the parent point in the path.
     *
     * @param parent The parent point to set.
     */
    public void setParent(Point parent) {
        this.parent = parent;
    }


}
