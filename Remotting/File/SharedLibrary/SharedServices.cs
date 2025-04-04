using System;

namespace SharedServices
{
    public interface IFileTransfer
    {
        string[] ListFiles(); // List files on the server
        byte[] DownloadFile(string fileName); // Download a file from the server
        void UploadFile(string fileName, byte[] fileData); // Upload a file to the server
    }
}