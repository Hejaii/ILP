import org.junit.Test;
import uk.ac.ed.inf.App;
import uk.ac.ed.inf.ilp.data.Order;

import java.io.IOException;
import java.net.MalformedURLException;

public class ExceptionTest {

    @Test
    public void testPrintUsage() {
        // 提供无效的命令行参数，触发 printUsage 方法
        String[] invalidArgs = {};
        try {
            App.main(invalidArgs);
            org.junit.Assert.fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            // 捕获到异常后，验证并通过测试
            org.junit.Assert.assertTrue("printUsage test passed.", true);
        }
    }

    @Test
    public void testMalformedURLException() {
        // 提供一个无效 URL，触发 handleMalformedURLException
        String invalidUrl = "htp://invalid-url";
        try {
            App.fetchData(invalidUrl, Order[].class);
            org.junit.Assert.fail("Expected MalformedURLException to be thrown");
        } catch (MalformedURLException e) {
            try {
                // 通过 main 方法触发异常处理
                App.main(new String[]{"2023-01-01", invalidUrl});
                org.junit.Assert.assertTrue("MalformedURLException test passed.", true);
            } catch (Exception ex) {
                org.junit.Assert.fail("Unexpected exception during MalformedURLException test: " + ex.getMessage());
            }
        } catch (IOException e) {
            org.junit.Assert.fail("Unexpected IOException during MalformedURLException test.");
        }
    }

    @Test
    public void testIOException() {
        // 提供一个不可访问的 URL，触发 handleIOException
        String validUrlButUnavailable = "https://ilp-rest-2024.azurewebsites.net/unavailable-endpoint";
        try {
            App.main(new String[]{"2023-01-01", validUrlButUnavailable});
            org.junit.Assert.assertTrue("IOException test passed.", true);
        } catch (Exception e) {
            org.junit.Assert.fail("Unexpected exception during IOException test: " + e.getMessage());
        }
    }

}

