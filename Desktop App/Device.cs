using System;
using System.Windows.Forms;
using System.Collections.Generic;

namespace Lugenium.Desktop
{
    public class Device
    {
        public Guid ID { get; set; }
        public Vector3 Orientation { get; set; }
        public string Name { get; set; }
        public string Version { get; set; }
        public RecieveStage Stage { get; set; }
        public Dictionary<int, Keys> Keys { get; set; }

        public Device()
        {
            ID = Guid.NewGuid();
            Keys = new Dictionary<int, Keys>();
            Stage = RecieveStage.Name;
        }
    }

    public enum RecieveStage { Name, Version, Data, Testing, Ping }
}
