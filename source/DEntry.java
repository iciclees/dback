import java.util.ArrayList;

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
