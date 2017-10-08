import java.io.File;

public class DBackMain
{
	public static void main(String[] args)
	{
		File localPath = null;
		File remoteIndexPath = null;
		boolean saveIndex = false;
		boolean backup = false;
		boolean error = false;

		try
		{
			for (int i = 0; i < args.length; ++i)
			{
				String arg = args[i];

				if (arg.equals("-b"))
				{
					backup = true;
				}
				else if (arg.equals("-l"))
				{
					if (++i < args.length)
					{
						if (localPath != null)
						{
							System.err.println("Only one occurence of -l is allowed.");
							error = true;
						}
						else
						{
							localPath = new File(args[i]);
						}
					}
					else
					{
						System.err.println("Missing argument to -l.");
						error = true;
					}
				}
				else if (arg.equals("-r"))
				{
					if (++i < args.length)
					{
						if (remoteIndexPath != null)
						{
							System.err.println("Only one occurence of -r is allowed.");
							error = true;
						}
						else
						{
							remoteIndexPath = new File(args[i]);
						}
					}
					else
					{
						System.err.println("Missing argument to -r.");
						error = true;
					}
				}
				else if (arg.equals("-s"))
				{
					saveIndex = true;
				}
				else
				{
					System.err.printf("Unknown option: %s\n", arg);
				}
			}

			if (error)
			{
				System.exit(1);
			}

			DBack dback = new DBack();

			if (localPath != null)
			{
				dback.LoadLocalPath(localPath);
			}
			if (saveIndex)
			{
				dback.SaveLocalIndex();
			}
			if (remoteIndexPath != null)
			{
				dback.LoadRemoteIndex(remoteIndexPath);
			}
			if (backup)
			{
				dback.PerformBackup();
			}
		}
		catch (Exception e)
		{
			System.err.println(e);
			System.err.printf("Error: %s\n", e.getMessage());
			System.exit(1);
		}
	}
}
