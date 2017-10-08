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
