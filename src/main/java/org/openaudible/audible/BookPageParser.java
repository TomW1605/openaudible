package org.openaudible.audible;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openaudible.books.Book;
import org.openaudible.books.BookElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public enum BookPageParser {
	instance;       // Singleton
	
	private static final Log LOG = LogFactory.getLog(BookPageParser.class);
	
	// audible uses a lot of cdata. It is useful.
	List<String> getCDATATags(String html) {
		ArrayList<String> list = new ArrayList<>();
		String startTag = "<![CDATA[";
		String endTag = "//]]>";
		
		int ch = 0;
		for (; ; ) {
			int start = html.indexOf(startTag, ch);
			if (start == -1) break;
			int end = html.indexOf(endTag, ch);
			assert (end != -1);
			if (end == -1) break;
			String cdata = html.substring(start + startTag.length(), end).trim();
			list.add(cdata);
			ch = end + endTag.length();
		}
		
		
		return list;
	}
	
	public boolean parseBookPage(HtmlPage page, Book b) {
		DomNode h = page;
		// HTMLUtil.debugNode(page, "book_info");
		String xml = page.asXml();
		//LOG.info(xml);
		List<String> cdataList = getCDATATags(xml);
		for (String cd : cdataList) {
			if (cd.startsWith("[")) {
				cd = cd.replace("\n", "");      // getting parse errors.
				
				try {
					JSONArray jsonArray = new JSONArray(cd);
					for (int x = 0; x < jsonArray.length(); x++) {
						JSONObject obj = jsonArray.getJSONObject(x);
						//LOG.info(obj);
						extractFromJSON(obj, b);
					}
				} catch (Throwable th) {
					LOG.info(cd);
					LOG.error("cdata json parse error", th);
				}
			}
			
		}
		
		
		return true;
	}
	
	
	// right now we only care about the @type AudioBook meta data.
	private void extractFromJSON(JSONObject obj, Book b) {
		String typ = obj.optString("@type");
		if (typ == null || typ.isEmpty())
			return;
		if (typ.equalsIgnoreCase("BreadcrumbList"))
			extractFromList(obj, b);
		if (!typ.equalsIgnoreCase("AudioBook")) // && !"Product".equalsIgnoreCase(typ))
			return;
		
		//LOG.info(obj.toString(2));
		
		for (String k : obj.keySet()) {
			Object value = obj.get(k);
			String str = value != null ? value.toString() : "";
			BookElement elem = null;
			
			switch (k) {
				case "description":
					elem = BookElement.summary;     // our summary is the description
					/*str = str.replaceAll("(?: ?<p> ?)(.+?)?(?: ?</p> ?)", "\n\n$1");
					str = str.replaceAll("(?:<i> ?)(.+?)(?: ?</i>)", "\"$1\"");
					str = str.replaceAll("^\\n+", "");*/
					break;
				case "sku":
					// elem = BookElement.product_id;
					break;
				
				case "duration":
					// format is like "PT11H19M" .. skipping for now.
					break;
				
				case "productID":
					elem = BookElement.asin;
					if (b.has(elem)) {
						assert (b.get(elem).equals(str));
					}
					break;
				case "datePublished":
					elem = BookElement.release_date;
					break;
				case "author":
					str = personToString(obj.getJSONArray(k));
					elem = BookElement.author;
					break;
				case "readBy":
					str = personToString(obj.getJSONArray(k));
					elem = BookElement.narratedBy;
					break;
				
				case "aggregateRating":
					JSONObject rating = obj.getJSONObject(k);
					double rvalue = rating.optDouble("ratingValue", 0);
					int rcount = rating.optInt("ratingCount", 0);
					if (rvalue > 0)
						b.setRating_average(rvalue);
					if (rcount > 0)
						b.setRating_count(rcount);
					break;
				case "name":
					elem = BookElement.shortTitle;
					break;
				case "publisher":
					elem = BookElement.publisher;
					break;
				default:
					//LOG.info("Skipping "+k+" = "+ str);
					break;
			}
			
			if (elem != null && !str.isEmpty()) {
				if (!str.equals(b.get(elem))) {
					LOG.info("set " + elem + " from " + b.get(elem) + " to " + str);
					b.set(elem, str);
				}
			}
		}
	}

	private void extractFromList(JSONObject obj, Book b) {
		for (String k : obj.keySet()) {
			Object array = obj.get(k);
			if (array.getClass()==JSONArray.class) {
				String genre = "", subGenre = "";
				for (Object val : (JSONArray)array) {
					if (val.getClass()==JSONObject.class) {
						JSONObject list = (JSONObject)val;
						int position = list.getInt("position");
						String str = list.getJSONObject("item").getString("name");

						switch (position) {
							case 2:
								genre = str;
								break;
							case 3:
								subGenre = str;
								break;
							default:
								break;
						}
					}
				}
				if (!genre.isEmpty() && !subGenre.isEmpty())
					b.setGenre(genre+"->"+subGenre);
			}
		}
	}


//      "author": [{
//        "@type": "Person",
//                "name": "Susan Smith"
//    }],
//            "readBy": [{
//        "@type": "Person",
//                "name": "Robin Racer"
//    }],
	
	// array of 'person' objects.
	private String personToString(JSONArray arr) {
		String out = "";
		for (int x = 0; x < arr.length(); x++) {
			JSONObject p = arr.getJSONObject(x);
			assert (p.getString("@type").equals("Person"));
			String name = p.optString("name", "");
			if (!name.isEmpty()) {
				if (!out.isEmpty())
					out += ",";
				out += name;
			}
		}
		return out;
	}
	
}
