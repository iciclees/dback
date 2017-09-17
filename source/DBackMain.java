import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

public class DBackMain
{
	public static void main(String[] args)
	{
		File rootToArchive = null;
		File indexToLoad = null;
		boolean saveIndex = false;
		boolean error = false;

		try
		{
			for (int i = 0; i < args.length; ++i)
			{
				String arg = args[i];

				if (arg.equals("-s"))
				{
					saveIndex = true;
				}
				else if (arg.equals("-l"))
				{
					if (++i < args.length)
					{
						if (indexToLoad != null)
						{
							System.err.println("Only one occurence of -l is allowed.");
							error = true;
						}
						else if (rootToArchive != null)
						{
							System.err.println("Cannot specify both a directory and an index file with -l.");
							error = true;
						}
						else
						{
							indexToLoad = new File(args[i]);
							if (!indexToLoad.exists())
							{
								System.err.printf("'%s' does not exist.\n", arg);
								error = true;
							}
							indexToLoad = indexToLoad.getCanonicalFile();
						}
					}
					else
					{
						System.err.println("Missing argument to -i.");
						error = true;
					}
				}
				else
				{
					if (rootToArchive != null)
					{
						System.err.println("Only one directory may be specified.");
						error = true;
					}
					else if (indexToLoad != null)
					{
						System.err.println("Cannot specify both a directory and an index file with -l.");
						error = true;
					}
					else
					{
						rootToArchive = new File(arg);
						if (!rootToArchive.exists())
						{
							System.err.printf("'%s' does not exist.\n", arg);
							error = true;
						}
						else if (!rootToArchive.isDirectory())
						{
							System.err.printf("'%s' is not a directory.\n", arg);
							error = true;
						}
						rootToArchive = rootToArchive.getCanonicalFile();
					}
				}
			}

			if (error)
			{
				System.exit(1);
			}

			DBack dback = new DBack();

			if (rootToArchive != null)
			{
				dback.BuildIndex(rootToArchive);
			}
			else if (indexToLoad != null)
			{
				dback.LoadIndex(indexToLoad);
				rootToArchive = new File(indexToLoad.getParent(), dback.GetRoot().mName);
			}

			if ((rootToArchive != null) && saveIndex)
			{
				File indexFile = new File(rootToArchive + ".index");
				dback.SaveIndex(indexFile);
			}
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
		catch (DataFormatException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
}
