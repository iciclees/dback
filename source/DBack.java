import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.DataFormatException;

public class DBack
{
	public class IndexEntry
	{
		private static final int IS_READABLE = 4;
		private static final int IS_WRITABLE = 2;
		private static final int IS_EXECUTABLE = 1;

		public String mName;
		public int mPermissions;

		public IndexEntry(String name, boolean isReadable, boolean isWritable, boolean isExecutable)
		{
			mName = name;
			SetReadable(isReadable);
			SetWritable(isWritable);
			SetExecutable(isExecutable);
		}

		public IndexEntry(String name, int permissions)
		{
			mName = name;
			mPermissions = permissions;
		}

		public boolean IsReadable()
		{
			return (mPermissions & IS_READABLE) != 0;
		}

		public void SetReadable(boolean readable)
		{
			mPermissions = readable ? (mPermissions | IS_READABLE) : (mPermissions & ~IS_READABLE);
		}

		public boolean IsWritable()
		{
			return (mPermissions & IS_WRITABLE) != 0;
		}

		public void SetWritable(boolean writable)
		{
			mPermissions = writable ? (mPermissions | IS_WRITABLE) : (mPermissions & ~IS_WRITABLE);
		}

		public boolean IsExecutable()
		{
			return (mPermissions & IS_EXECUTABLE) != 0;
		}

		public void SetExecutable(boolean executable)
		{
			mPermissions = executable ? (mPermissions | IS_EXECUTABLE) : (mPermissions & ~IS_EXECUTABLE);
		}
	}

	public class DEntry extends IndexEntry
	{
		public ArrayList<DEntry> mSubDirs;
		public ArrayList<FEntry> mFiles;

		public DEntry()
		{
			this("", false, false, false);
		}

		public DEntry(String name, boolean isReadable, boolean isWritable, boolean isExecutable)
		{
			super(name, isReadable, isWritable, isExecutable);
			mSubDirs = new ArrayList<DEntry>();
			mFiles = new ArrayList<FEntry>();
		}

		public DEntry(String name, int permissions)
		{
			super(name, permissions);
			mSubDirs = new ArrayList<DEntry>();
			mFiles = new ArrayList<FEntry>();
		}
	}

	public class FEntry extends IndexEntry
	{
		public String mSha1;

		public FEntry(String name, String sha1, boolean isReadable, boolean isWritable, boolean isExecutable)
		{
			super(name, isReadable, isWritable, isExecutable);
			mSha1 = sha1;
		}

		public FEntry(String name, String sha1, int permissions)
		{
			super(name, permissions);
			mSha1 = sha1;
		}
	}

	private HashMap<String, FEntry> mFileMap;
	private DEntry mRoot;
	private byte[] mTempBuffer;

	public DBack()
	{
		mTempBuffer = new byte[16*1024];
	}

	public DEntry GetRoot()
	{
		return mRoot;
	}

	public void BuildIndex(File f) throws IOException
	{
		mRoot = null;
		mFileMap = new HashMap<String, FEntry>();
		DEntry tempRoot = new DEntry();
		BuildIndex(tempRoot, f);
		if (tempRoot.mSubDirs.size() > 0)
		{
			mRoot = tempRoot.mSubDirs.get(0);
		}
	}

	private void BuildIndex(DEntry parent, File f) throws IOException
	{
		if (f.exists())
		{
			if (f.isDirectory())
			{
				DEntry dEntry = new DEntry(f.getName(), f.canRead(), f.canWrite(), f.canExecute());
				parent.mSubDirs.add(dEntry);
				File[] list = f.listFiles();
				for (File child: list)
				{
					BuildIndex(dEntry, child);
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
					byte[] tempBuffer = mTempBuffer;
					while (dis.read(tempBuffer) != -1) {}
					String sha1 = Hex.encodeHexString(md.digest());
					FEntry fEntry = new FEntry(f.getName(), sha1, f.canRead(), f.canWrite(), f.canExecute());
					mFileMap.put(sha1, fEntry);
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
	}

	public void SaveIndex(File output) throws IOException
	{
		CSVPrinter printer = null;
		try
		{
			printer = CSVFormat.DEFAULT.print(output, Charset.forName("UTF-8"));
			SaveIndex(printer, mRoot);
		}
		finally
		{
			printer.close();
		}
	}

	private void SaveIndex(CSVPrinter printer, DEntry dEntry) throws IOException
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

	private void SaveIndex(CSVPrinter printer, FEntry fEntry) throws IOException
	{
		printer.printRecord('f', fEntry.mName, fEntry.mPermissions, fEntry.mSha1);
	}

	public void LoadIndex(File input) throws IOException, DataFormatException
	{
		CSVParser parser = null;
		try
		{
			mRoot = null;
			mFileMap = new HashMap<String, FEntry>();
			parser = CSVParser.parse(input, Charset.forName("UTF-8"), CSVFormat.DEFAULT);
			DEntry tempRoot = new DEntry();
			LoadIndex(parser.iterator(), tempRoot);
			if (tempRoot.mSubDirs.size() > 0)
			{
				mRoot = tempRoot.mSubDirs.get(0);
			}
		}
		finally
		{
			parser.close();
		}
	}

	private void LoadIndex(Iterator<CSVRecord> records, DEntry parent) throws DataFormatException
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
				mFileMap.put(fEntry.mSha1, fEntry);
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
