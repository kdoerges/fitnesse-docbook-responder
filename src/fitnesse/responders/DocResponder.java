package fitnesse.responders;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import util.XmlUtil;
import util.XmlWriter;

import fitnesse.FitNesseContext;
import fitnesse.authentication.SecureOperation;
import fitnesse.authentication.SecureReadOperation;
import fitnesse.authentication.SecureResponder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.wiki.PageCrawler;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPagePath;

public class DocResponder implements SecureResponder {

	private WikiPage contextPage;
	private String resource;

	@Override
	public Response makeResponse(FitNesseContext context, Request request)
			throws Exception {

		contextPage = getContextPage(request, context);

		SimpleResponse response = new SimpleResponse();

		String title = "Test Dokumentation";
		String bookAbstract = "Dies ist eine Zusammenfassung der Tests." + "";
		Document docBook = buildDocBookHeader(title, bookAbstract);

		// Letzter Node oder sind Sub-Nodes vorhanden?
		// Erste Version: nur diese Seite verarbeiten
		docBook = addChapter(contextPage, docBook);

		byte[] bytes = toByteArray(docBook);
		response.setContent(bytes);
		response.setContentType("text/xml");

		return response;
	}

	private Document addChapter(WikiPage contextPage, Document docBook) {
		// XmlUtil.addTextNode(rssDocument, itemElement1, "title", title);

		Element itemChapter = docBook.createElement("chapter");
		try {
			itemChapter.setAttribute("id", "chapter_" + contextPage.getName());
			itemChapter.setIdAttribute("id", true);
		} catch (Exception e) {
			//
		}

		Element itemChapterTitle = docBook.createElement("title");
		try {
			itemChapterTitle.setTextContent(contextPage.getName());
		} catch (Exception e) {
			//
		}

		Element itemChapterPara = docBook.createElement("para");
		try {
			itemChapterPara.setTextContent(parseContent(contextPage.getData()
					.getContent()));
		} catch (Exception e) {
			//
		}

		// Und die Elemente einhaengen
		itemChapter.appendChild(itemChapterTitle);
		itemChapter.appendChild(itemChapterPara);
		Element rootElement = (Element) docBook.getElementsByTagName("book")
				.item(0);
		rootElement.appendChild(itemChapter);

		return docBook;
	}

	/**
	 * Erstellt das grundgeruest aus book Element und fuegt einen Titel hinzu.
	 * 
	 * @param title
	 * @return
	 * @throws Exception
	 */
	private Document buildDocBookHeader(String title, String bookabstract)
			throws Exception {
		Document docBook = XmlUtil.newDocument();
		Element docBookElement = docBook.createElement("book");

		Element itemTitle = docBook.createElement("title");
		itemTitle.setTextContent(title);
		docBookElement.appendChild(itemTitle);

		Element itemAbstract = docBook.createElement("abstract");

		Element itemAbstractPara = docBook.createElement("para");
		itemAbstractPara.setTextContent(bookabstract);
		itemAbstract.appendChild(itemAbstractPara);

		docBookElement.appendChild(itemAbstract);

		docBook.appendChild(docBookElement);

		return docBook;
	}

	private String parseContent(String content) {
		// Vereinbarung: der Text zwischen den ersten beiden " " ist der
		// Beschreibungstext und muss vor der ersten
		// Tabellendefinition erfolgen.
		String c2 = content.substring(0, content.indexOf("|"));

		StringBuffer result = new StringBuffer();

		Pattern p = Pattern.compile("\".*\"");
		Matcher m = p.matcher(c2);

		m.appendTail(result);
		return result.toString();
	}

	private byte[] toByteArray(Document docBook) throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XmlWriter writer = new XmlWriter(os);
		writer.write(docBook);
		writer.close();
		byte[] bytes = os.toByteArray();
		return bytes;
	}

	@Override
	public SecureOperation getSecureOperation() {
		return new SecureReadOperation();
	}

	private WikiPage getContextPage(Request request, FitNesseContext context)
			throws Exception {
		resource = request.getResource();
		PageCrawler pageCrawler = context.root.getPageCrawler();
		WikiPagePath resourcePath = PathParser.parse(resource);
		return pageCrawler.getPage(context.root, resourcePath);
	}

}