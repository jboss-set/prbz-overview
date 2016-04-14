package org.jboss.set.overview.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.overview.ejb.Aider;

@WebServlet(name = "PullRequestOverviewServlet", loadOnStartup = 1, urlPatterns = { "/pullrequestoverview" })
public class PullRequestOverviewServlet extends HttpServlet {

    public static Logger logger = Logger.getLogger(PullRequestOverviewServlet.class.getCanonicalName());

    private static final long serialVersionUID = -8119634403150269667L;

    private List<ProcessorData> pullRequestData = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @EJB
    private Aider aiderService;

    public PullRequestOverviewServlet() {
        super();
    }

    @Override
    public void init() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                logger.log(Level.INFO, "pull request data initialisation in Servlet init()");
                aiderService.generateData();
            }
        });
        executorService.shutdown();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Put the data list in request and let Freemarker paint it.
        pullRequestData = aiderService.getData();
        if (pullRequestData == null || pullRequestData.isEmpty()) {
            request.getRequestDispatcher("/error.html").forward(request, response);
        } else {
            request.setAttribute("rows", pullRequestData);
            request.getRequestDispatcher("/index.ftl").forward(request, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // do nothing
    }
}
