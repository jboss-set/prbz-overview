package org.jboss.set.overview.servlet;

import java.io.IOException;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.set.assistant.data.ProcessorData;
import org.jboss.set.overview.ejb.Aider;

public class PullRequestOverviewServlet extends HttpServlet {

    private static final long serialVersionUID = -8119634403150269667L;

    @EJB
    private Aider aiderService;
    private List<ProcessorData> data;

    public PullRequestOverviewServlet() {
        super();
    }

    @Override
    public void init() {
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Put the data list in request and let Freemarker paint it.
        data = aiderService.getData();
        if (data == null || data.isEmpty()) {
            request.getRequestDispatcher("/error.html").forward(request, response);
        } else {
            request.setAttribute("rows", data);
            request.getRequestDispatcher("/index.ftl").forward(request, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // do nothing
    }
}
