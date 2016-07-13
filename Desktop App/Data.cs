using System;
using System.Net;
using System.Text;
using System.Threading;
using System.Net.Sockets;
using System.Windows.Forms;
using System.Collections.Generic;

namespace Lugenium.Desktop
{
    public static class Data
    {
        public static bool turnRight = false;

        private static Socket serverSocket;
        private static byte[] buffer = new byte[1024];
        private static Dictionary<Device, Socket> devices = new Dictionary<Device, Socket>();
        private static List<string> logs = new List<string>();

        public static event EventHandler DeviceAdded;
        public static event EventHandler DeviceRemoved;

        public static Form1 form;

        public static void SetupServer(Form1 form1)
        {
            form = form1;
            serverSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            serverSocket.Bind(new IPEndPoint(IPAddress.Any, 12345));
            serverSocket.Listen(5);
            serverSocket.BeginAccept(AcceptCallback, null);
            Log("Server setup");
        }

        public static Device[] GetDevices()
        {
            Device[] ds = new Device[devices.Count];
            devices.Keys.CopyTo(ds, 0);
            return ds;
        }

        public static bool DeviceExists(Guid guid)
        {
            foreach (Device device in devices.Keys)
            {
                if (device.ID == guid)
                    return true;
            }
            return false;
        }

        public static Device FindDevice(Guid guid)
        {
            foreach (Device device in devices.Keys)
            {
                if (device.ID == guid)
                    return device;
            }
            return null;
        }

        private static void AcceptCallback(IAsyncResult ar)
        {
            Socket socket;

            try
            {
                socket = serverSocket.EndAccept(ar);
            }
            catch (ObjectDisposedException) // I cannot seem to avoid this (on exit when properly closing sockets)
            {
                return;
            }

            Device device = new Device();
            Log("Device created with Guid : " + device.ID);
            devices.Add(device, socket);
            OnDeviceAdded(device, EventArgs.Empty);
            socket.BeginReceive(buffer, 0, 1024, SocketFlags.None, ReceiveCallback, device);
            serverSocket.BeginAccept(AcceptCallback, null);
        }

        private static void ReceiveCallback(IAsyncResult ar)
        {
            Device current = (Device)ar.AsyncState;
            int received;

            try
            {
                received = devices[current].EndReceive(ar);
            }
            catch (SocketException)
            {
                devices[current].Close(); // Dont shutdown because the socket may be disposed and its disconnected anyway
                devices.Remove(current);
                return;
            }

            byte[] recBuf = new byte[received];
            Array.Copy(buffer, recBuf, received);
            string text = Encoding.ASCII.GetString(recBuf);

            Log(current.ID + " - \"" + text + "\"");
            text = text.TrimEnd('\n', '\r');

            if (text.ToLower() == "exit" || received == 0)
            {
                devices[current].Shutdown(SocketShutdown.Both);
                devices[current].Close();
                devices.Remove(current);
                Log("Connection with " + current.ID + " terminated");
                OnDeviceRemoved(current, EventArgs.Empty);
                return;
            }
            else
            {
                string[] commands = text.Split('.');
                if(commands[0] == "keyboard")
                {
                    if (commands[1] == "keydown")
                    {
                        InputManager.Keyboard.KeyDown(Key(commands[2]));
                    }
                    else if (commands[1] == "keyup")
                    {
                        InputManager.Keyboard.KeyUp(Key(commands[2]));
                    }
                }
                else if(commands[0] == "mouse")
                {
                    if(commands[1] == "move")
                    {
                        InputManager.Mouse.MoveRelative(int.Parse(commands[2]), int.Parse(commands[3]));
                    }
                }
                Console.WriteLine("\"" + text + "\"");
            }

            devices[current].BeginReceive(buffer, 0, 1024, SocketFlags.None, ReceiveCallback, current);
        }

        public static void Log(string message)
        {
            Console.WriteLine(message);
            logs.Add(message);
            form.listBox1.Invoke((MethodInvoker)delegate()
            {
                form.listBox1.Items.Add(message);
            });
        }

        private static Keys Key(string key)
        {
            switch(key.ToLower())
            {
                case "w":
                    return Keys.W;
                case "a":
                    return Keys.A;
                case "s":
                    return Keys.S;
                case "d":
                    return Keys.D;
                case "f":
                    return Keys.F;
                default:
                    return Keys.None;
            }
        }

        private static void OnDeviceAdded(object sender, EventArgs e)
        {
            if (DeviceAdded != null)
                DeviceAdded(null, e);
        }

        private static void OnDeviceRemoved(object sender, EventArgs e)
        {
            if (DeviceAdded != null)
                DeviceAdded(null, e);
        }
    }
}