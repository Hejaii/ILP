import org.junit.Assert;
import org.junit.Test;
import uk.ac.ed.inf.*;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// A* Pathfinding Boundary Conditions Test
public class BoundaryTest {

    @Test
    public void testFileWritingBoundaryConditions() throws IOException {

        try {
            App.writeDirections(new ArrayList<>(), null);
            Assert.fail("Expected NullPointerException for null date.");
        } catch (NullPointerException e) {
            Assert.assertTrue(true);
        }
    }
    @Test(timeout = 5000)
    public void testPathfindingBoundaryConditions() {
        Map map = new Map();
        LngLatHandler handler = new LngLatHandler();

        // Add a simple obstacle
        NamedRegion obstacle = new NamedRegion("Obstacle", new LngLat[]{
                new LngLat(-3.19, 55.94),
                new LngLat(-3.18, 55.94),
                new LngLat(-3.18, 55.95),
                new LngLat(-3.19, 55.95)
        });
        map.addObstacle(obstacle);

        // Test 1: Start and end points are the same
        Point startAndEnd = new Point(-3.186874, 55.944494);
        AStar pathFinder1 = new AStar(map, startAndEnd, startAndEnd);
        List<Point> path1 = pathFinder1.findPath(false);
        Assert.assertEquals(1, path1.size());

        // Test 2: Start point inside an obstacle
        Point startInObstacle = new Point(-3.185, 55.945);
        Point endOutside = new Point(-3.186874, 55.944494);
        AStar pathFinder2 = new AStar(map, startInObstacle, endOutside);
        List<Point> path2 = pathFinder2.findPath(false);
        Assert.assertTrue("Path should be empty if start point is in obstacle.", path2.isEmpty());

        // Test 3: End point inside an obstacle
        Point endInObstacle = new Point(-3.185, 55.945);
        AStar pathFinder3 = new AStar(map, startAndEnd, endInObstacle);
        List<Point> path3 = pathFinder3.findPath(false);
        Assert.assertTrue("Path should be empty if end point is in obstacle.", path3.isEmpty());

        // Test 4: Start and end points out of bounds
        Point startOutOfBounds = new Point(-10, 60);
        Point endOutOfBounds = new Point(10, -60);
        AStar pathFinder4 = new AStar(map, startOutOfBounds, endOutOfBounds);
        List<Point> path4 = pathFinder4.findPath(false);
        Assert.assertTrue("Path should be empty if points are out of bounds.", path4.isEmpty());
    }



}
