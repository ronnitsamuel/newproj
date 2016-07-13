using System;
using System.Drawing;
using MetroFramework;
using System.Threading;
using MetroFramework.Forms;
using System.Windows.Forms;
using System.Collections.Generic;

namespace Lugenium.Desktop
{
    public partial class Form1 : MetroForm
    {
        public Form1()
        {
            InitializeComponent();
            Load += (sender, e) =>
            {
                Data.SetupServer(this);
            };
        }
    }
}
