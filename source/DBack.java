import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class DBack
{
	public class DEntry
	{
		public String mName;
		public ArrayList<DEntry> mSubDirs;
		public ArrayList<FEntry> mFiles;

		public DEntry(String name)
		{
			mName = name;
			mSubDirs = new ArrayList<DEntry>();
			mFiles = new ArrayList<FEntry>();
		}
	}

	public class FEntry
	{
		public String mName;
		public String mSha1;
		public boolean mCanRead;
		public boolean mCanWrite;
		public boolean mCanExecute;

		public FEntry(String name, String sha1, boolean canRead, boolean canWrite, boolean canExecute)
		{
			mName = name;
			mSha1 = sha1;
			mCanRead = canRead;
			mCanWrite = canWrite;
			mCanExecute = canExecute;
		}
	}

	private HashMap<String, FEntry> mFileMap;
	private DEntry mRoot;
	private byte[] mTempBuffer;

	public DBack()
	{
		mTempBuffer = new byte[16*1024];
	}

	public void BuildIndex(File f) throws IOException
	{
		mFileMap = new HashMap<String, FEntry>();
		mRoot = new DEntry("");
		Index(mRoot, f.getCanonicalFile());
	}

	private void Index(DEntry parent, File f) throws IOException
	{
		if (f.exists())
		{
			if (f.isDirectory())
			{
				DEntry dEntry = new DEntry(f.getName());
				parent.mSubDirs.add(dEntry);
				File[] list = f.listFiles();
				for (File child: list)
				{
					Index(dEntry, child);
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
		FileWriter fw = null;
		BufferedWriter bw = null;
		CSVPrinter printer = null;
		try
		{
			fw = new FileWriter(output);
			bw = new BufferedWriter(fw);
			printer = new CSVPrinter(bw, CSVFormat.DEFAULT);
			SaveIndex(printer, mRoot);
		}
		finally
		{
			if (printer != null)
			{
				printer.close();
			}
			if (bw != null)
			{
				bw.close();
			}
			if (fw != null)
			{
				fw.close();
			}
		}
	}

	private void SaveIndex(CSVPrinter printer, DEntry dEntry) throws IOException
	{
		printer.printRecord('d', dEntry.mName, dEntry.mSubDirs.size() + dEntry.mFiles.size());
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
		printer.printRecord('f', fEntry.mName, fEntry.mSha1);
	}
}
