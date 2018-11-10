package org.openaudible.desktop.swt.manager.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.openaudible.Audible;
import org.openaudible.books.Book;
import org.openaudible.books.BookElement;
import org.openaudible.books.BookListener;
import org.openaudible.books.BookNotifier;
import org.openaudible.desktop.swt.gui.SWTAsync;
import org.openaudible.desktop.swt.i8n.Translate;
import org.openaudible.desktop.swt.manager.AudibleGUI;
import org.openaudible.desktop.swt.util.shop.FontShop;
import org.openaudible.desktop.swt.util.shop.PaintShop;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BookInfoPanel extends GridComposite implements BookListener {
	
	private static final Log LOG = LogFactory.getLog(BookInfoPanel.class);
	
	//    , BookElement.codec,  BookElement.genre, BookElement.asin, BookElement.infoLink, , BookElement.summary, BookElement.description,  BookElement.format, BookElement.rating_average, BookElement.rating_count, BookElement.genre, BookElement.shortTitle, BookElement.copyright, BookElement.user_id, BookElement.cust_id };
	final Image cover = PaintShop.getImage("images/cover.png");
	BookElement elems[] = {
			BookElement.shortTitle,
			BookElement.fullTitle,
			BookElement.genre,
			BookElement.author,
			BookElement.narratedBy,
			BookElement.duration,
			BookElement.purchase_date,
			BookElement.release_date,
			BookElement.publisher,
			BookElement.asin,
	};
	//static final BookElement elems[] = { BookElement.fullTitle, BookElement.author, BookElement.release_date, BookElement.publisher, BookElement.asin, BookElement.product_id };
	Label stats[] = new Label[BookElement.values().length];
	Label task;     // progress task info, or what actions can be done.
	
	int imageSize = 200;
	Book curBook = null;
	Label imageLabel;
	AtomicInteger cache = new AtomicInteger();
	
	BookInfoPanel(Composite c) {
		super(c, SWT.NONE);
		initLayout(2, false, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		// this.debugLayout();
		this.noMargins();
		
		GridComposite statsView = createStatsView(this);
		createImageLabel(this);
		BookNotifier.getInstance().addListener(this);
	}
	
	public static String getName(BookElement n) {
		return n.displayName();
	}
	
	public static String getValue(BookElement n, Book b) {
		String out = b.get(n);
		
		out = out.replace("#169;", "\u00a9");
		out = out.replace("(C)", "\u00a9");
		out = out.replace("(P)", "\u2117");
		out = out.replace("&amp;", "&&");      // two of them for text items
		out = out.replace("&quot;", "\"");
		out = out.replace("&apos;", "\u0027");
		out = out.replace("&lt;", "<");
		out = out.replace("&gt;", ">");
		out = out.replace("&", "&&");
		return out;
	}
	
	private void createImageLabel(Composite parent) {
		imageLabel = new Label(parent, SWT.BORDER_SOLID);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = imageSize;
		gd.heightHint = imageSize;
		imageLabel.setLayoutData(gd);
		clearCoverArt();
	}
	
	private GridComposite createStatsView(GridComposite parent) {
		GridComposite c = null;
		if (true) {
			c = new GridComposite(parent, SWT.BORDER_DOT);
			
			c.initLayout(2, false, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
			c.noMargins();
			
			
			c.getGridData().horizontalIndent = 0;
			c.getGridData().verticalIndent = 0;
			// c.debugLayout(SWT.COLOR_BLUE);
			
		} else {
			c = parent;
		}
		
		MouseAdapter linkClickListener = new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent mouseEvent) {
				super.mouseUp(mouseEvent);
				Label l = (Label) mouseEvent.widget;
				linkClicked(l);
			}
		};
		
		for (BookElement s : elems) {
			String labelName = getName(s);
			Label l = c.newLabel();
			l.setText(Translate.getInstance().labelName(labelName) + ": ");
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			l.setFont(FontShop.instance.tableFontBold());
			l.setBackground(bgColor);
			
			Label d = c.newLabel();
			d.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			d.setFont(FontShop.instance.tableFont());
			d.setBackground(bgColor);
			d.setData(s);
			stats[s.ordinal()] = d;
			d.addMouseListener(linkClickListener);
		}
		
		
		if (true) {
			// Task Status:
			Label l = c.newLabel();
			l.setText(Translate.getInstance().labelName("Task") + ": ");
			l.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			l.setFont(FontShop.instance.tableFontBold());
			l.setBackground(bgColor);
			
			Label d = c.newLabel();
			d.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			d.setFont(FontShop.instance.tableFont());
			d.setBackground(bgColor);
			//d.setData(s);
			task = d;
		}
		
		
		return c;
	}
	
	private void linkClicked(Label s) {
		if (curBook != null) {
			BookElement e = (BookElement) s.getData();
			String link = getLink(curBook, e);
			if (!link.isEmpty()) {
				AudibleGUI.instance.browse(link);
			}
		}
		
	}
	
	private void update(Book b) {
		curBook = b;
		
		setThumbnailImage(b);
		
		for (Label s : stats) {
			if (s == null) continue;
			BookElement e = (BookElement) s.getData();
			String value = "";
			if (b != null) {
				value = getValue(e, b);
			}
			s.setText(value);
			
			String link = getLink(b, e);
			int color = link.isEmpty() ? SWT.COLOR_BLACK : SWT.COLOR_BLUE;
			s.setForeground(Display.getCurrent().getSystemColor(color));
			
			
		}
		
		String taskMsg = AudibleGUI.instance.getTaskString(curBook);
		task.setText(taskMsg);
		
		
		if (b != null) {
			if (LOG.isTraceEnabled())
				LOG.trace(b.inspect("\n"));
		}
	}
	
	private String getLink(Book b, BookElement e) {
		String out = "";
		String prefix = "";
		String suffix = "";
		
		if (b != null) {
			switch (e) {
				case fullTitle:
				case shortTitle:
					out = b.getInfoLink();
					break;
				case narratedBy:
					prefix = "https://www.audible.com.au/search?searchNarrator=";
					suffix = b.getNarratedBy();
					break;
				
				case publisher:
					prefix = "https://www.audible.com.au/search?searchProvider=";
					suffix = b.getPublisher();
					break;
				
				case author:
					out = b.getAuthorLink();
					break;
				default:
					break;
			}
		}
		
		if (!prefix.isEmpty() && !suffix.isEmpty()) {
			try {
				suffix = URLEncoder.encode(suffix, "utf-8");
			} catch (Exception ignore) {
			}
			out = prefix + suffix;
		}
		
		return out;
	}
	
	private void clearCoverArt() {
		Image i = imageLabel.getImage();
		if (i != null && i != cover) {
			imageLabel.setImage(null);
			i.dispose();
		}
		
		imageLabel.setImage(cover);
		
	}
	
	private void setThumbnailImage(Book b) {
		clearCoverArt();
		if (b == null) return;
		
		File imgFile = Audible.instance.getImageFileDest(b);
		if (imgFile.exists()) {
			try {
				try (FileInputStream fis = new FileInputStream(imgFile)) {
					Image i = new Image(Display.getCurrent(), fis);
					
					int width = i.getBounds().width;
					int height = i.getBounds().height;
					
					Image out = new Image(Display.getCurrent(), imageSize, imageSize);
					GC gc = new GC(out);
					
					int x = 0;
					int y = 0;
					int w = imageSize;
					int h = imageSize;
					if (width != height) {
						if (width > height) {
							// width will be imageSize, height imageSize*ratio
							float ratio = height / (float) width;
							assert (ratio >= 0 && ratio <= 1);
							h = Math.round(h * ratio);
							y = (imageSize - h) / 2;
							
						} else {
							float ratio = width / (float) height;
							assert (ratio >= 0 && ratio <= 1);
							
							w = Math.round(h * ratio);
							x = (imageSize - w) / 2;
						}
						assert (x >= 0 && x <= imageSize);
						assert (y >= 0 && y <= imageSize);
						assert (w <= imageSize);
						assert (h <= imageSize);
						
					}
					
					gc.drawImage(i, 0, 0, i.getBounds().width, i.getBounds().height, x, y, w, h);
					
					Image thumb = PaintShop.resizeImage(i, imageSize, imageSize);
					i.dispose();
					
					imageLabel.setImage(thumb);
				}
			} catch (Throwable th) {
				assert (false);
			}
		}
		
	}
	
	private void refresh() {
		refresh(curBook);
	}
	
	private void refresh(final Book b) {
		if (cache.getAndIncrement() == 0) {
			SWTAsync.run(new SWTAsync("refresh") {
				@Override
				public void task() {
					cache.set(0);
					update(b);
				}
			});
		}
	}
	
	@Override
	public void booksSelected(final List<Book> list) {
		SWTAsync.run(new SWTAsync("update") {
			@Override
			public void task() {
				
				switch (list.size()) {
					case 0:
						update(null);
						break;
					case 1:
						update(list.get(0));
						break;
					default:
						update(null);
						break;
				}
				
			}
		});
		
	}
	
	@Override
	public void bookAdded(Book book) {
	}
	
	@Override
	public void bookUpdated(Book book) {
		//if (book.equals(curBook))
		{
			refresh();
		}
	}
	
	@Override
	public void booksUpdated() {
		refresh();
	}
	
	@Override
	public void bookProgress(final Book book, final String msg) {
		if (book.equals(curBook))
		{
			SWTAsync.run(new SWTAsync("bookProgress") {
				@Override
				public void task() {
					task.setText(msg);
				}
			});
		}
	}
	
	
}
