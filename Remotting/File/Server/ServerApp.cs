using System;
using System.IO;
using SharedServices;

namespace ServerApp
{
    public class FileTransferService : MarshalByRefObject, IFileTransfer
    {
        private readonly string serverFilesPath = "ServerFiles";

        public FileTransferService()
        {
            // Ensure the ServerFiles directory exists
            if (!Directory.Exists(serverFilesPath))
            {
                Directory.CreateDirectory(serverFilesPath);
            }
        }

        // List files on the server
        public string[] ListFiles()
        {
            return Directory.GetFiles(serverFilesPath);
        }

        // Download a file from the server
        public byte[] DownloadFile(string fileName)
        {
            string filePath = Path.Combine(serverFilesPath, fileName);
            if (File.Exists(filePath))
            {
                return File.ReadAllBytes(filePath);
            }
            throw new FileNotFoundException("File not found on server.");
        }

        // Upload a file to the server
        public void UploadFile(string fileName, byte[] fileData)
        {
            string filePath = Path.Combine(serverFilesPath, fileName);
            File.WriteAllBytes(filePath, fileData);
        }
    }

    class Program
    {
        static void Main(string[] args)
        {
            // Register the TCP channel
            System.Runtime.Remoting.Channels.ChannelServices.RegisterChannel(
                new System.Runtime.Remoting.Channels.Tcp.TcpChannel(8080), false);

           System.Runtime.Remoting.RemotingConfiguration.RegisterWellKnownServiceType(
    typeof(FileTransferService), 
    "FileTransferService", 
    System.Runtime.Remoting.WellKnownObjectMode.Singleton // or WellKnownObjectMode.SingleCall
);


            Console.WriteLine("File transfer server is running. Press Enter to exit...");
            Console.ReadLine();
        }
    }
}