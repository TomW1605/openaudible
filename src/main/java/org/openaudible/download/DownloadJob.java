package org.openaudible.download;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp4.Mp4FileReader;
import org.jaudiotagger.audio.mp4.Mp4FileWriter;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.jaudiotagger.tag.mp4.field.Mp4TagTextField;
import org.openaudible.Audible;
import org.openaudible.Directories;
import org.openaudible.books.Book;
import org.openaudible.books.BookElement;
import org.openaudible.convert.AAXParser;
import org.openaudible.progress.IProgressTask;
import org.openaudible.util.CopyWithProgress;
import org.openaudible.util.Util;
import org.openaudible.util.queues.IQueueJob;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DownloadJob implements IQueueJob
{
	private static final Log LOG = LogFactory.getLog(DownloadJob.class);
	/*

	GET /download?user_id=uuuu&product_id=BK_HACH_000001&codec=LC_64_22050_ster&awtype=AAX&cust_id=xxx HTTP/1.1
	User-Agent: Audible ADM 6.6.0.19;Windows Vis
	 *
	 */
	static HttpClientBuilder bld = HttpClients.custom();
	final Book b;
	final File destFile;
	volatile boolean quit = false;
	IProgressTask task;

	public DownloadJob(Book b, File destFile)
	{
		this.b = b;
		this.destFile = destFile;
		assert (!destFile.exists());
	}

	public void download() throws IOException
	{
		if (!b.has(BookElement.shortTitle) || !b.has(BookElement.genre))
		{
			try
			{
				Audible.instance.updateInfo(b);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		String codec = b.getCodec();
		if (codec.isEmpty())
		{
			codec = "LC_64_22050_stereo";
		}

        /*
        https://cds.audible.com.au/download?
        asin=B00FE5FDCQ&
        cust_id=oUpFQWz5Lk0ZxIJkS-Hb1T-RSexXK6yCj2Sfs-PX3nKeLxpDMlkNNxHI7DpU&
        codec=LC_64_22050_Stereo&
		source=Audible&
		type=AUDI
         */

		String url = "http://cds.audible.com.au/download";
		url += "?asin=" + b.getAsin();
		url += "&cust_id=" + b.getCust_id();
		url += "&codec=" + codec;
		url += "&source=" + b.getSource();
		url += "&type=" + b.getType();

		LOG.info("Download book: " + b + " url=" + url);

		File tmp = null;
		long start = System.currentTimeMillis();
		FileOutputStream fos = null;
		boolean success = false;
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
		//"Audible ADM 6.6.0.19;Windows Vista  Build 9200");

		CloseableHttpClient httpclient = null;
		CloseableHttpResponse response = null;

		try
		{

			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setSocketTimeout(30000)
					.build();

			bld.setDefaultRequestConfig(defaultRequestConfig);
			httpclient = bld.build();

			response = httpclient.execute(httpGet);

			int code = response.getStatusLine().getStatusCode();
			if (code != 200)
			{
				throw new IOException(response.getStatusLine().toString());
			}
			HttpEntity entity = response.getEntity();
			Header ctyp = entity.getContentType();
			if (ctyp != null)
			{
				if (!ctyp.getValue().contains("audio"))
				{
					String err = "Download error:";

					if (entity.getContentLength() < 256)
					{
						err += EntityUtils.toString(entity);
					}
					err += " for " + b;
					throw new IOException(err); //
				}
			}

			tmp = new File(Directories.getTmpDir(), destFile.getName() + ".part");

			if (tmp.exists())
			{
				boolean v = tmp.delete();
				assert (v);
			}

			fos = new FileOutputStream(tmp);

			CopyWithProgress.copyWithProgress(getByteReporter(), 500, entity.getContent(), fos);

			/// IO.copy(entity.getContent(), fos);

			if (quit)
			{
				success = false;
				throw new IOException("quit");
			}
			success = true;
		} finally
		{

			response.close();
			if (fos != null)
			{
				fos.close();
			}

			if (httpclient != null)
			{
				httpclient.close();
			}

			if (success)
			{
				if (tmp != null)
				{
					boolean ok = tmp.renameTo(destFile);
					if (!ok)
					{
						throw new IOException("failed to rename." + tmp.getAbsolutePath() + " to " + destFile.getAbsolutePath());
					}
				}

				long time = System.currentTimeMillis() - start;
				long bytes = destFile.length();
				double bps = bytes / (time / 1000.0);

				File aaxFile = new File(destFile.getPath());
				Mp4FileWriter writer = new Mp4FileWriter();
				Mp4FileReader reader = new Mp4FileReader();
				try
				{
					Mp4TagTextField field;
					AudioFile audiofile = reader.read(aaxFile);
					Mp4Tag tag = (Mp4Tag) audiofile.getTag();

					field = (Mp4TagTextField) tag.getFirstField("@sti");
					field.setContent(b.getShortTitle());
					tag.setField(field);

					field = (Mp4TagTextField) tag.getFirstField("©gen");
					field.setContent(b.getGenre());
					tag.setField(field);

					field = (Mp4TagTextField) tag.getFirstField("©cmt");
					field.setContent(b.getSummary());
					tag.setField(field);

					audiofile.setTag(tag);
					writer.write(audiofile);
				} catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException | CannotWriteException e)
				{
					e.printStackTrace();
				}

				LOG.info("Downloaded " + destFile.getName() + " bytes=" + bytes + " time=" + time + " Kbps=" + (int) (bps / 1024.0));
			}
			else
			{
				if (tmp != null)
				{
					tmp.delete();
				}
				destFile.delete();
			}
		}
	}

	private CopyWithProgress.ByteReporter getByteReporter()
	{

		CopyWithProgress.ByteReporter br = new CopyWithProgress.ByteReporter()
		{
			long startTime = System.currentTimeMillis();

			public void bytesCopied(long total) throws IOException
			{
				double seconds = (System.currentTimeMillis() - startTime) / 1000.0;
				double bps = total / seconds;
				String rate = Util.instance.byteCountToString((long) bps) + "/sec";
				String t = "Downloading " + b;
				String s = Util.instance.byteCountToString(total) + " at " + rate;

				if (task != null)
				{
					task.setTask(t, s);
				}

				// LOG.info(task+" "+ s);
				if (quit)
				{
					throw new IOException("quit");
				}
			}
		};

		return br;
	}

	public void quit()
	{
	}

	@Override
	public void processJob() throws Exception
	{
		download();
		// update book info, based on tags.
		AAXParser.instance.update(b);
	}

	@Override
	public void quitJob()
	{
		quit = true;
	}

	@Override
	public String toString()
	{
		return "Download " + b;
	}

	public void setProgress(IProgressTask task)
	{
		this.task = task;
	}
}
