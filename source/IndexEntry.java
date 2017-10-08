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
