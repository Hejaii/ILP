package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a map with obstacles, restaurants, and specified areas.
 */
public class Map {

    private List<NamedRegion> obstacles;

    /**
     * Constructs a map with default parameters.
     *
     */
    public Map() {
        this.obstacles = new ArrayList<>();
    }

    public List<NamedRegion> getObstacles() {
        return obstacles;
    }

    /**
     * Adds an obstacle to the map.
     *
     * @param obstacle The obstacle coordinates to be added.
     */
    public void addObstacle(NamedRegion obstacle) {
        obstacles.add(obstacle);
    }

    /**
     * Checks if the given location contains an obstacle.
     *
     * @param location The location to check for obstacles.
     * @return True if the location is an obstacle, false otherwise.
     */
    public boolean isInObstacle(LngLat location) {
        for (NamedRegion obstacle : obstacles) {
            if(new LngLatHandler().isInRegion(location, obstacle)){
                return true;
            }
        }
        return false;
    }
}
