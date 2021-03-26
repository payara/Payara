package fish.payara.samples.grpc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.grpc.servlet.ServletAdapter;
import io.grpc.servlet.ServletAdapterBuilder;

@WebServlet(value = "/*", asyncSupported = true)
public class GrpcServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private ServletAdapter adapter = new ServletAdapterBuilder() //
            .addService(new PayaraService()) //
            .buildServletAdapter();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        adapter.doPost(req, resp);
    }

    @Override
    public void destroy() {
        adapter.destroy();
        super.destroy();
    }

}
