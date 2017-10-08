import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.zip.DataFormatException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

public class IndexUtils
{
	public static DEntry BuildIndex(File f) throws IOException
	{
		DEntry root = null;
		DEntry tempRoot = new DEntry();
		byte[] tempBuffer = new byte[16*1024];
		BuildIndex(tempRoot, f, tempBuffer);
		if (tempRoot.mSubDirs.size() > 0)
		{
			root = tempRoot.mSubDirs.get(0);
		}
		return root;
	}

	private static void BuildIndex(DEntry parent, File f, byte[] tempBuffer) throws IOException
	{
		if (f.isDirectory())
		{
			DEntry dEntry = new DEntry(f.getName(), f.canRead(), f.canWrite(), f.canExecute());
			parent.mSubDirs.add(dEntry);
			File[] list = f.listFiles();
			for (File child: list)
			{
				BuildIndex(dEntry, child, tempBuffer);
			}
		}
		else if (f.isFile())
		{
			FileInputStream fis = null;
			DigestInputStream dis = null;
			try
			{
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				fis = new FileInputStream(f);
				dis = new DigestInputStream(fis, md);
				while (dis.read(tempBuffer) != -1) {}
				String sha1 = Hex.encodeHexString(md.digest());
				FEntry fEntry = new FEntry(f.getName(), sha1, f.canRead(), f.canWrite(), f.canExecute());
				parent.mFiles.add(fEntry);
			}
			catch (NoSuchAlgorithmException e) {}
			finally
			{
				if (dis != null)
				{
					dis.close();
				}
				if (fis != null)
				{
					fis.close();
				}
			}
		}
	}

	public static void SaveIndex(File output, DEntry root) throws IOException
	{
		CSVPrinter printer = null;
		try
		{
			printer = CSVFormat.DEFAULT.print(output, Charset.forName("UTF-8"));
			SaveIndex(printer, root);
		}
		finally
		{
			printer.close();
		}
	}

	private static void SaveIndex(CSVPrinter printer, DEntry dEntry) throws IOException
	{
		printer.printRecord('d', dEntry.mName, dEntry.mPermissions, dEntry.mSubDirs.size() + dEntry.mFiles.size());
		for (DEntry subDir: dEntry.mSubDirs)
		{
			SaveIndex(printer, subDir);
		}
		for (FEntry file: dEntry.mFiles)
		{
			SaveIndex(printer, file);
		}
	}

	private static void SaveIndex(CSVPrinter printer, FEntry fEntry) throws IOException
	{
		printer.printRecord('f', fEntry.mName, fEntry.mPermissions, fEntry.mSha1);
	}

	public static DEntry LoadIndex(File input) throws IOException, DataFormatException
	{
		DEntry root = null;
		CSVParser parser = null;
		try
		{
			parser = CSVParser.parse(input, Charset.forName("UTF-8"), CSVFormat.DEFAULT);
			DEntry tempRoot = new DEntry();
			LoadIndex(parser.iterator(), tempRoot);
			if (tempRoot.mSubDirs.size() > 0)
			{
				root = tempRoot.mSubDirs.get(0);
			}
		}
		finally
		{
			if (parser != null)
			{
				parser.close();
			}
		}
		return root;
	}

	private static void LoadIndex(Iterator<CSVRecord> records, DEntry parent) throws DataFormatException
	{
		try
		{
			if (!records.hasNext())
			{
				throw new DataFormatException("Malformed index file.");
			}

			CSVRecord record = records.next();
			if (record.size() != 4)
			{
				throw new DataFormatException("Malformed index file.");
			}

			String entryType = record.get(0);
			String name = record.get(1);
			int permissions = Integer.decode(record.get(2));

			if (entryType.equals("d"))
			{
				DEntry dEntry = new DEntry(name, permissions);
				parent.mSubDirs.add(dEntry);
				int numChildren = Integer.decode(record.get(3));
				for (int i = 0; i < numChildren; ++i)
				{
					LoadIndex(records, dEntry);
				}
			}
			else if (entryType.equals("f"))
			{
				FEntry fEntry = new FEntry(name, record.get(3), permissions);
				parent.mFiles.add(fEntry);
			}
			else
			{
				throw new DataFormatException("Malformed index file.");
			}
		}
		catch (NumberFormatException e)
		{
			throw new DataFormatException("Malformed index file.");
		}
	}
}
