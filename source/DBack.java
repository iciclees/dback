import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.zip.DataFormatException;

public class DBack
{
	//private File mLocalIndexPath;
	private File mLocalPath;
	//private File mRemoteIndexPath;
	private File mRemotePath;
	private File mRemoteDbPath;
	private DEntry mLocalIndex;
	private DEntry mRemoteIndex;
	private HashSet<String> mRemoteDb;

	public DBack()
	{
	}

	public DEntry GetRoot()
	{
		return mLocalIndex;
	}

	public void LoadLocalPath(File localPath) throws IOException, DataFormatException
	{
		File path = localPath.getCanonicalFile();
		if (path.isDirectory())
		{
			mLocalPath = path;
			mLocalIndex = IndexUtils.BuildIndex(path);
			// mLocalIndexPath = null;
		}
		else
		{
			// mLocalIndexPath = path;
			mLocalIndex = IndexUtils.LoadIndex(path);
			mLocalPath = new File(path.getParent(), mLocalIndex.mName);
		}
		// mFileMap = new HashMap<String, FEntry>();
	}

	public void SaveLocalIndex() throws IOException
	{
		AssertLocalIndex();
		// if (mLocalIndexPath == null)
		// {
		// 	mLocalIndexPath = new File(mLocalPath.toString() + ".index");
		// }
		File localIndexPath = new File(mLocalPath.toString() + ".index");
		IndexUtils.SaveIndex(localIndexPath, mLocalIndex);
	}

	public void LoadRemoteIndex(File remoteIndexPath) throws IOException, DataFormatException
	{
		File path = remoteIndexPath.getCanonicalFile();
		mRemoteDb = new HashSet<String>();
		if (path.isDirectory())
		{
			mRemotePath = path;
			mRemoteDbPath = new File(path, "db");
		}
		else
		{
			// mRemoteIndexPath = path;
			mRemoteIndex = IndexUtils.LoadIndex(path);
			mRemotePath = path.getParentFile();
			mRemoteDbPath = new File(mRemotePath, "db");
		}
	}

	public void PerformBackup() throws IOException
	{
		AssertLocalIndex();
		BackupIndex();
		byte[] tempBuffer = new byte[16*1024];
		Backup(mLocalPath, mLocalIndex, tempBuffer);
	}

	private void BackupIndex() throws IOException
	{
		String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-AAAAAAAA"));
		File remoteIndexPath = new File(mRemotePath, "index." + time);
		IndexUtils.SaveIndex(remoteIndexPath, mLocalIndex);
	}

	private void Backup(File dir, DEntry dEntry, byte[] tempBuffer) throws IOException
	{
		System.out.printf("Backing up: %s\n", dir.toString());
		mRemoteDbPath.mkdirs();
		for (DEntry subDir: dEntry.mSubDirs)
		{
			Backup(new File(dir, subDir.mName), subDir, tempBuffer);
		}
		for (FEntry file: dEntry.mFiles)
		{
			Backup(new File(dir, file.mName), file, tempBuffer);
		}
	}

	private void Backup(File file, FEntry fEntry, byte[] tempBuffer) throws IOException
	{
		if (!mRemoteDb.contains(fEntry.mSha1))
		{
			System.out.printf("Backing up: %s\n", file.toString());
			FileInputStream fis = null;
			FileOutputStream fos = null;
			try
			{
				File dest = new File(mRemoteDbPath, fEntry.mSha1);
				fis = new FileInputStream(file);
				fos = new FileOutputStream(dest);
				int bytesRead = -1;
				while ((bytesRead = fis.read(tempBuffer)) != -1)
				{
					fos.write(tempBuffer, 0, bytesRead);
				}
				mRemoteDb.add(fEntry.mSha1);
			}
			finally
			{
				if (fis != null)
				{
					fis.close();
				}
				if (fos != null)
				{
					fos.close();
				}
			}
		}
	}

	private void AssertLocalIndex()
	{
		if (mLocalIndex == null)
		{
			throw new IllegalStateException("No local index has been created.");
		}
	}

	// private void AssertRemoteIndex()
	// {
	// 	if (mRemoteIndex == null)
	// 	{
	// 		throw new IllegalStateException("No remote index has been created.");
	// 	}
	// }
}
