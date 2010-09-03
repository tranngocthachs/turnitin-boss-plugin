package boss.turnitin;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import boss.plugins.spi.pages.IStaffPluginPage;

import uk.ac.warwick.dcs.boss.frontend.PageContext;

public class TurnItInErrorCallbackPage extends IStaffPluginPage {

	@Override
	public String getPageName() {
		return "tii_error";
	}

	@Override
	public String getPageTemplate() {
		return "staff_tii_error";
	}

	public void handleGet(PageContext pageContext, Template template,
			VelocityContext templateContext) throws ServletException,
			IOException {
		String rcode = pageContext.getParameter("ec");
		if (rcode != null)
			templateContext.put("rcode", rcode);
		String rmessage = pageContext.getParameter("em");
		if (rmessage != null)
			templateContext.put("rmessage", rmessage);
		pageContext.renderTemplate(template, templateContext);
	}

	public void handlePost(PageContext pageContext, Template template,
			VelocityContext templateContext) throws ServletException,
			IOException {
		throw new ServletException("Unexpected POST request");
	}
}
