using System;
using System.IO;
using SharedServices;

namespace ClientApp
{
    class Program
    {
        static void Main(string[] args)
        {
            // Register the TCP channel
            System.Runtime.Remoting.Channels.ChannelServices.RegisterChannel(
                new System.Runtime.Remoting.Channels.Tcp.TcpChannel(), false);

            // Get the file transfer service
            IFileTransfer fileTransfer = (IFileTransfer)Activator.GetObject(
                typeof(IFileTransfer), "tcp://localhost:8080/FileTransferService");

            Console.WriteLine("Connected to the file transfer server. Enter 'exit' to quit.");

            while (true)
            {
                Console.WriteLine("\nChoose an option:");
                Console.WriteLine("1. List files on the server");
                Console.WriteLine("2. Download a file from the server");
                Console.WriteLine("3. Upload a file to the server");
                Console.WriteLine("4. Exit");
                Console.Write("Enter your choice: ");
                string choice = Console.ReadLine();

                switch (choice)
                {
                    case "1": // List files
                        ListFiles(fileTransfer);
                        break;

                    case "2": // Download a file
                        DownloadFile(fileTransfer);
                        break;

                    case "3": // Upload a file
                        UploadFile(fileTransfer);
                        break;

                    case "4": // Exit
                        Console.WriteLine("Exiting...");
                        return;

                    default:
                        Console.WriteLine("Invalid choice. Please try again.");
                        break;
                }
            }
        }

        static void ListFiles(IFileTransfer fileTransfer)
        {
            try
            {
                string[] files = fileTransfer.ListFiles();
                Console.WriteLine("\nFiles available on the server:");
                foreach (string file in files)
                {
                    Console.WriteLine(Path.GetFileName(file));
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error: " + ex.Message);
            }
        }

        static void DownloadFile(IFileTransfer fileTransfer)
        {
            Console.Write("Enter the name of the file to download: ");
            string fileName = Console.ReadLine();

            try
            {
                byte[] fileData = fileTransfer.DownloadFile(fileName);
                File.WriteAllBytes(fileName, fileData);
                Console.WriteLine(string.Format("File '{0}' downloaded successfully.", fileName));
            }
            catch (FileNotFoundException ex)
            {
                Console.WriteLine("Error: " + ex.Message);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Error: " + ex.Message);
            }
        }

        static void UploadFile(IFileTransfer fileTransfer)
        {
            Console.Write("Enter the path of the file to upload: ");
            string filePath = Console.ReadLine();

            if (File.Exists(filePath))
            {
                try
                {
                    byte[] fileData = File.ReadAllBytes(filePath);
                    string fileName = Path.GetFileName(filePath);
                    fileTransfer.UploadFile(fileName, fileData);
                    Console.WriteLine(string.Format("File '{0}' uploaded successfully.", fileName));
                }
                catch (Exception ex)
                {
                    Console.WriteLine("Error: " + ex.Message);
                }
            }
            else
            {
                Console.WriteLine("File not found.");
            }
        }
    }
}