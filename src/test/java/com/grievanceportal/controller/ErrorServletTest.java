package com.grievanceportal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

class ErrorServletTest {

    @Test
    void notFoundAndForbiddenHaveTheirOwnWording() {
        assertEquals("Page not found", ErrorServlet.titleFor(404));
        assertEquals("Access denied", ErrorServlet.titleFor(403));
        assertTrue(ErrorServlet.messageFor(404).contains("could not find"));
        assertTrue(ErrorServlet.messageFor(403).contains("do not have access"));
        assertNotEquals(ErrorServlet.messageFor(404), ErrorServlet.messageFor(403));
    }

    @Test
    void serverErrorsUseOneGenericMessage() {
        assertEquals(ErrorServlet.messageFor(500), ErrorServlet.messageFor(503));
        assertEquals("Something went wrong", ErrorServlet.titleFor(500));
    }

    @Test
    void messagesNeverMentionExceptionsOrStackTraces() {
        for (int status : new int[] {400, 403, 404, 405, 500, 503}) {
            String text = ErrorServlet.titleFor(status) + " " + ErrorServlet.messageFor(status);
            assertFalse(text.contains("Exception"));
            assertFalse(text.contains("at com."));
            assertFalse(text.contains("Tomcat"));
        }
    }

    @Test
    void webXmlSendsTheCommonErrorsAndAllExceptionsToTheErrorPage() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File("src/main/webapp/WEB-INF/web.xml"));
        Set<String> handled = new HashSet<>();
        NodeList pages = doc.getElementsByTagName("error-page");
        for (int i = 0; i < pages.getLength(); i++) {
            org.w3c.dom.Element page = (org.w3c.dom.Element) pages.item(i);
            assertEquals("/error", page.getElementsByTagName("location").item(0).getTextContent().trim());
            NodeList codes = page.getElementsByTagName("error-code");
            NodeList types = page.getElementsByTagName("exception-type");
            if (codes.getLength() > 0) {
                handled.add(codes.item(0).getTextContent().trim());
            }
            if (types.getLength() > 0) {
                handled.add(types.item(0).getTextContent().trim());
            }
        }
        assertTrue(handled.contains("404"));
        assertTrue(handled.contains("403"));
        assertTrue(handled.contains("500"));
        assertTrue(handled.contains("java.lang.Throwable"));
    }
}
