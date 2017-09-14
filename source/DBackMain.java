import java.io.File;
import java.io.IOException;

public class DBackMain
{
	public static void main(String[] args)
	{
		File rootToArchive = null;
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
				else
				{
					if (rootToArchive != null)
					{
						System.err.println("Only one directory may be specified.");
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
				if (rootToArchive.exists())
				{
					dback.BuildIndex(rootToArchive);
				}
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
	}
}
