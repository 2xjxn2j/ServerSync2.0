package org.jacob.spigot.plugins.serversync2;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.N;
import sun.nio.cs.UTF_8;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;

public final class ServerSync2 extends JavaPlugin {

    public static HashMap<PacketContainer, Integer> sentPackets = new HashMap<PacketContainer, Integer>();

    public static Socket socket;

    private static ServerSync2 instance;
    public static ServerSync2 getInstance() {
        return instance;
    }

    private ObjectInputStream incoming;
    private ObjectOutputStream outgoing;

    public static PrintWriter os;
    public static BufferedReader is;

    public static ProtocolManager protocolManager;

    public static byte[] objectToBytes(Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static Object parseObject(byte[] data) {
        Object object;
        try {
            object = new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return object;
    }

    @Override
    public void onEnable() {
        instance = this;
        protocolManager = ProtocolLibrary.getProtocolManager();

        Thread a = new Thread() {
            @Override
            public void run() {
                listenForPackets();
            }

        };
        a.start();

        startConnection();
    }



    private void listenForPackets() {
        ServerSync2.protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {

                if(!sentPackets.containsValue(Integer.valueOf(event.getPacket().getId()))) ServerSync2.os.println(Base64.getEncoder().encodeToString(objectToBytes(event.getPacket())));

            }

        });

    }

    private void startConnection() {
        System.out.println("Attempting to connect");
        try {
            socket = new Socket("207.244.234.181", 1000);
            System.out.println("Connected!");
        } catch (IOException e) {
            System.out.println("Connection failed.");
        }

        try {
            if (socket != null) {

                is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                os = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("Streams created");
            }

        } catch (IOException e) {
            System.out.println("Connection failed");
        }


        Thread listener = new Thread() {

            @Override
            public void run() {

                while (true) {
                    try {
                        String text = is.readLine();

                        System.out.println("Received: " + text);
                        //do stuff here

                        PacketContainer container = (PacketContainer)parseObject(Base64.getDecoder().decode(text));

                        ServerSync2.protocolManager.broadcastServerPacket(container);

                        sentPackets.put(container, container.getId());


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        listener.start();

    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
