using System;
using SharedServices;

namespace ServerApp
{
    public class CalculatorService : MarshalByRefObject, ICalculator
    {
        public int Add(int a, int b) { return a + b; }
        public int Subtract(int a, int b) { return a - b; }
        public int Multiply(int a, int b) { return a * b; }
        public int Divide(int a, int b) { return a / b; }
    }

    class Program
    {
        static void Main(string[] args)
        {
            // Register the TCP channel
            System.Runtime.Remoting.Channels.ChannelServices.RegisterChannel(
                new System.Runtime.Remoting.Channels.Tcp.TcpChannel(8080), false);

            // Register the service
            System.Runtime.Remoting.RemotingConfiguration.RegisterWellKnownServiceType(
                typeof(CalculatorService), "CalculatorService", System.Runtime.Remoting.WellKnownObjectMode.Singleton);

            Console.WriteLine("Server is running. Press Enter to exit...");
            Console.ReadLine();
        }
    }
}