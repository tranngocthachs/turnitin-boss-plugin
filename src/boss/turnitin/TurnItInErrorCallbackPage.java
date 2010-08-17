package boss.turnitin;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import uk.ac.warwick.dcs.boss.frontend.PageContext;
import uk.ac.warwick.dcs.boss.plugins.spi.pages.StaffPluginPageProvider;

public class TurnItInErrorCallbackPage extends StaffPluginPageProvider {

	@Override
	public String getPageName() {
		return "tii_error";
	}

	@Override
	public String getPageTemplate() {
		return "staff_tii_error";
	}

	@Override
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

	@Override
	public void handlePost(PageContext pageContext, Template template,
			VelocityContext templateContext) throws ServletException,
			IOException {
		throw new ServletException("Unexpected POST request");
	}

}
