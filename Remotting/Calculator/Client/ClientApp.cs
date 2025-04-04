using System;
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

            // Get the calculator service
            ICalculator calculator = (ICalculator)Activator.GetObject(
                typeof(ICalculator), "tcp://localhost:8080/CalculatorService");

            Console.WriteLine("Connected to the calculator service. Enter 'exit' to quit.");

            while (true)
            {
                // Prompt the user for input
                Console.Write("Enter operation (+, -, *, /) or 'exit': ");
                string operation = Console.ReadLine();

                if (operation.ToLower() == "exit")
                {
                    break; // Exit the loop
                }

                if (operation != "+" && operation != "-" && operation != "*" && operation != "/")
                {
                    Console.WriteLine("Invalid operation. Please try again.");
                    continue;
                }

                // Prompt for numbers
                Console.Write("Enter first number: ");
                int a = int.Parse(Console.ReadLine());

                Console.Write("Enter second number: ");
                int b = int.Parse(Console.ReadLine());

                // Perform the operation on the server
                int result = 0;
                switch (operation)
                {
                    case "+":
                        result = calculator.Add(a, b);
                        break;
                    case "-":
                        result = calculator.Subtract(a, b);
                        break;
                    case "*":
                        result = calculator.Multiply(a, b);
                        break;
                    case "/":
                        result = calculator.Divide(a, b);
                        break;
                }

               Console.WriteLine(string.Format("Result: {0} {1} {2} = {3}", a, operation, b, result));
            }

            Console.WriteLine("Calculator client exited.");
        }
    }
}